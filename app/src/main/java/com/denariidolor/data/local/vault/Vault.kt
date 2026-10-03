/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.denariidolor.data.local.vault

import android.content.Context
import com.denariidolor.data.local.db.DatabaseHolder
import com.denariidolor.data.local.db.DefaultDataInitializer
import com.denariidolor.data.local.preferences.AttemptResult
import com.denariidolor.data.local.preferences.LegacySecurityProfile
import com.denariidolor.data.local.preferences.ProfileMode
import com.denariidolor.data.local.preferences.RecoverPinResult
import com.denariidolor.data.local.preferences.SecureStorageException
import com.denariidolor.data.local.preferences.SecurityProfileService
import com.denariidolor.data.local.preferences.SetupProfileResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Guards the database key (CS-15). On disk the key exists only wrapped by the PIN, by the security answer and, when
 * turned on, by a biometric-bound Keystore key (CS-07); the database opens only after one of them unwraps it.
 * Storage and Keystore failures surface as [SecureStorageException] (BUG-05).
 */
@Suppress("TooManyFunctions") // The vault is the only holder of the unwrapped key; splitting it would spread that key across classes.
@Singleton
class Vault @Inject constructor(
    @ApplicationContext context: Context,
    config: VaultConfig,
    private val databaseHolder: DatabaseHolder,
    private val defaultDataInitializer: DefaultDataInitializer
) {
    private val appContext = context.applicationContext
    private val secrets = KeystoreSecrets(config.keyAliasPrefix)
    private val profileStore = KeystoreProfileStore(appContext, config.profilePrefsName, secrets)
    private val profile = SecurityProfileService(profileStore, KeystoreDeviceKeyMixer(secrets), AndroidMonotonicClock(appContext))
    private val legacy = LegacyProfileStorage(appContext, config, secrets)

    private class PendingUpgrade(val pin: String, val databaseKey: ByteArray)

    // Guarded by this: the key while unlocked (to turn on biometric sign-in) and a v1 upgrade waiting for its question.
    private var unlockedKey: ByteArray? = null
    private var pendingUpgrade: PendingUpgrade? = null

    val isUnlocked: Boolean get() = databaseHolder.isOpen

    suspend fun state(): VaultState = background {
        val current = profile.getProfileState()
        val legacyProfile = if (current.mode == ProfileMode.SIGN_IN) null else legacy.profile()
        when {
            current.mode == ProfileMode.SIGN_IN -> {
                if (legacy.hasFiles()) legacy.delete() // Left over from an upgrade that was interrupted after it was saved.
                VaultState.Configured(current.securityQuestion, profile.biometricWrap() != null, upgradeStarted = false)
            }
            legacyProfile != null -> VaultState.Configured(
                securityQuestion = legacyProfile.securityQuestion(),
                biometricEnrolled = false,
                upgradeStarted = synchronized(this) { pendingUpgrade != null }
            )
            else -> VaultState.NeedsSetup
        }
    }

    suspend fun setUp(pin: String, pinConfirmation: String, question: String, answer: String): SetupProfileResult = background {
        val problem = SecurityProfileService.validateSetup(pin, pinConfirmation, question, answer)
        when {
            profile.getProfileState().mode == ProfileMode.SIGN_IN || legacy.profile() != null -> SetupProfileResult.ALREADY_CONFIGURED
            problem != SetupProfileResult.SUCCESS -> problem
            else -> {
                // With no profile, nothing can decrypt an existing database any more: it is an orphan.
                databaseHolder.deleteDatabaseFiles()
                legacy.delete()
                profile.setupProfile(pin, pinConfirmation, question, answer)
            }
        }
    }

    suspend fun signIn(pin: String): SignInResult = background {
        if (profile.getProfileState().mode == ProfileMode.SIGN_IN) {
            profile.unlockWithPin(pin).toSignInResult { databaseKey ->
                unlock(databaseKey)
                SignInResult.Unlocked
            }
        } else {
            val legacyProfile = checkNotNull(legacy.profile()) { "No security profile" }
            profile.attempt { pin.takeIf(legacyProfile::verifyPin) }.toSignInResult { verifiedPin ->
                startUpgrade(verifiedPin)
                SignInResult.UpgradeRequired
            }
        }
    }

    suspend fun recoverPin(answer: String, newPin: String, pinConfirmation: String): RecoveryResult = background {
        val result = if (profile.getProfileState().mode == ProfileMode.SIGN_IN) {
            RecoveryResult.Finished(profile.recoverPin(answer, newPin, pinConfirmation))
        } else {
            recoverLegacy(checkNotNull(legacy.profile()) { "No security profile" }, answer, newPin, pinConfirmation)
        }
        if (result is RecoveryResult.Finished && result.result == RecoverPinResult.LOCKED_OUT) {
            result.copy(lockoutRemainingMillis = profile.lockoutRemainingMillis())
        } else {
            result
        }
    }

    /** Finishes a v1 upgrade: the new profile wraps the existing database key, and the database opens. */
    suspend fun completeUpgrade(question: String, answer: String): SetupProfileResult = background {
        val pending = checkNotNull(synchronized(this) { pendingUpgrade }) { "No upgrade in progress" }
        val result = profile.migrateProfile(pending.pin, question, answer, pending.databaseKey)
        if (result == SetupProfileResult.SUCCESS) {
            legacy.delete()
            synchronized(this) { pendingUpgrade = null }
            unlock(pending.databaseKey)
        }
        result
    }

    suspend fun prepareBiometricSignIn(): BiometricSignIn = background {
        val wrapped = profile.biometricWrap()
        val cipher = wrapped?.takeIf { it.size > KeystoreSecrets.GCM_IV_BYTES }
            ?.let { secrets.biometricDecryptCipher(it.copyOfRange(0, KeystoreSecrets.GCM_IV_BYTES)) }
        when {
            wrapped == null -> BiometricSignIn.NotEnrolled
            cipher == null -> {
                turnOffBiometric()
                BiometricSignIn.Invalidated
            }
            else -> BiometricSignIn.Ready(cipher)
        }
    }

    /** Finishes biometric sign-in with the cipher that `BiometricPrompt` authorized. */
    suspend fun signInWithBiometric(cipher: Cipher): SignInResult = background {
        val databaseKey = profile.biometricWrap()?.let { wrapped ->
            try {
                cipher.doFinal(wrapped, KeystoreSecrets.GCM_IV_BYTES, wrapped.size - KeystoreSecrets.GCM_IV_BYTES)
            } catch (_: GeneralSecurityException) {
                null
            }
        }
        if (databaseKey == null) {
            turnOffBiometric()
            SignInResult.BiometricFailed
        } else {
            profile.recordSuccessfulAuthentication()
            unlock(databaseKey)
            SignInResult.Unlocked
        }
    }

    /** A cipher for `BiometricPrompt` that wraps the database key with a new biometric-bound key. Needs an unlocked vault. */
    suspend fun prepareBiometricEnrollment(): Cipher = background {
        check(isUnlocked) { "The vault is locked" }
        secrets.biometricEncryptCipher()
    }

    /** Stores the database key wrapped by the cipher `BiometricPrompt` authorized; false if the vault locked meanwhile. */
    suspend fun completeBiometricEnrollment(cipher: Cipher): Boolean = background {
        val databaseKey = synchronized(this) { unlockedKey?.copyOf() }
        if (databaseKey == null) {
            false
        } else {
            try {
                profile.setBiometricWrap(cipher.iv + cipher.doFinal(databaseKey))
                true
            } catch (e: GeneralSecurityException) {
                throw SecureStorageException("Couldn't wrap the key for biometric sign-in", e)
            } finally {
                databaseKey.fill(0)
            }
        }
    }

    suspend fun disableBiometric() = background { turnOffBiometric() }

    /** Closes the database and forgets every unwrapped key: sign-out, session timeout, or leaving an upgrade. */
    fun lock() {
        synchronized(this) {
            unlockedKey?.fill(0)
            unlockedKey = null
            pendingUpgrade?.databaseKey?.fill(0)
            pendingUpgrade = null
        }
        databaseHolder.close()
    }

    /** Crypto-erase (CS-17): deletes the database files, both profile formats and every vault key. */
    suspend fun wipe(): Boolean = background {
        lock()
        listOf(
            databaseHolder::deleteDatabaseFiles,
            {
                profileStore.delete()
                true
            },
            {
                legacy.delete()
                true
            },
            {
                secrets.deleteAll()
                true
            },
            ::resetCacheDir
        ).map { step -> runCatching(step).getOrDefault(false) }.all { it }
    }

    private fun recoverLegacy(
        legacyProfile: LegacySecurityProfile,
        answer: String,
        newPin: String,
        pinConfirmation: String
    ): RecoveryResult {
        val problem = when {
            !SecurityProfileService.isValidPin(newPin) -> RecoverPinResult.INVALID_PIN_FORMAT
            newPin != pinConfirmation -> RecoverPinResult.PIN_MISMATCH
            answer.isBlank() -> RecoverPinResult.SECURITY_ANSWER_REQUIRED
            else -> null
        }
        if (problem != null) return RecoveryResult.Finished(problem)
        return when (profile.attempt { Unit.takeIf { legacyProfile.verifyAnswer(answer) } }) {
            is AttemptResult.Accepted -> {
                startUpgrade(newPin)
                RecoveryResult.UpgradeRequired
            }
            is AttemptResult.Rejected -> RecoveryResult.Finished(RecoverPinResult.INVALID_SECURITY_ANSWER)
            is AttemptResult.LockedOut -> RecoveryResult.Finished(RecoverPinResult.LOCKED_OUT)
        }
    }

    private fun startUpgrade(pin: String) {
        // v1 deleted the database whenever its key went missing; do the same rather than keep an unreadable file.
        val databaseKey = legacy.databaseKey() ?: ByteArray(SecurityProfileService.DATABASE_KEY_BYTES).also {
            databaseHolder.deleteDatabaseFiles()
            SecureRandom().nextBytes(it)
        }
        synchronized(this) {
            pendingUpgrade?.databaseKey?.fill(0)
            pendingUpgrade = PendingUpgrade(pin, databaseKey)
        }
    }

    private suspend fun unlock(databaseKey: ByteArray) {
        try {
            if (databaseHolder.isPlaintext()) databaseHolder.deleteDatabaseFiles() // From a build before SQLCipher.
            databaseHolder.open(databaseKey)
            defaultDataInitializer.seedDefaults()
            synchronized(this) {
                unlockedKey?.fill(0)
                unlockedKey = databaseKey.copyOf()
            }
        } finally {
            databaseKey.fill(0)
        }
    }

    private fun turnOffBiometric() {
        profile.setBiometricWrap(null)
        secrets.deleteBiometricKey()
    }

    private fun resetCacheDir(): Boolean {
        val cacheDir = appContext.cacheDir
        val cleared = !cacheDir.exists() || cacheDir.deleteRecursively()
        return cleared && (cacheDir.exists() || cacheDir.mkdirs())
    }

    private inline fun <T : Any> AttemptResult<T>.toSignInResult(onAccepted: (T) -> SignInResult): SignInResult = when (this) {
        is AttemptResult.Accepted -> onAccepted(value)
        is AttemptResult.Rejected -> SignInResult.InvalidPin(attemptsBeforeLockout)
        is AttemptResult.LockedOut -> SignInResult.LockedOut(remainingMillis)
    }

    private suspend fun <T> background(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
}
