/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import java.security.SecureRandom
import java.text.Normalizer
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface SecurityProfileStore {
    fun getString(key: String): String?
    fun getInt(key: String, defaultValue: Int): Int
    fun getLong(key: String, defaultValue: Long): Long
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun edit(block: SecurityProfileStoreEditor.() -> Unit)
}

interface SecurityProfileStoreEditor {
    fun putString(key: String, value: String)
    fun putInt(key: String, value: Int)
    fun putLong(key: String, value: Long)
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
    fun clear()
}

/**
 * Mixes a stretched secret with a key that never leaves this device (an Android Keystore HMAC key in production),
 * so PIN and answer guesses can only be checked on the device itself.
 */
interface DeviceKeyMixer {
    /** Creates the device key if it doesn't exist yet. Called only before a new profile is written. */
    fun prepare()

    /** HMAC of [input] under the device key; fails with [SecureStorageException] if the key is missing. */
    fun mix(input: ByteArray): ByteArray
}

/** Time that the user can't change, and the device's boot count (`null` when unknown). */
interface MonotonicClock {
    fun elapsedRealtimeMillis(): Long
    fun bootCount(): Int?
}

enum class ProfileMode {
    FIRST_TIME_SETUP,
    SIGN_IN
}

data class SecurityProfileState(val mode: ProfileMode, val securityQuestion: String?)

enum class SetupProfileResult {
    SUCCESS,
    ALREADY_CONFIGURED,
    INVALID_PIN_FORMAT,
    PIN_MISMATCH,
    SECURITY_QUESTION_REQUIRED,
    SECURITY_QUESTION_TOO_LONG,
    SECURITY_ANSWER_REQUIRED,
    SECURITY_ANSWER_TOO_SHORT,
    SECURITY_ANSWER_TOO_LONG,
    SECURITY_ANSWER_IN_QUESTION,
    SECURITY_ANSWER_MATCHES_PIN
}

enum class RecoverPinResult {
    SUCCESS,
    PROFILE_NOT_CONFIGURED,
    SECURITY_ANSWER_REQUIRED,
    INVALID_SECURITY_ANSWER,
    INVALID_PIN_FORMAT,
    PIN_MISMATCH,
    LOCKED_OUT
}

/** Outcome of a PIN or security-answer check that runs under the shared lockout. */
sealed interface AttemptResult<out T> {
    class Accepted<T>(val value: T) : AttemptResult<T>
    data class Rejected(val attemptsBeforeLockout: Int) : AttemptResult<Nothing>
    data class LockedOut(val remainingMillis: Long) : AttemptResult<Nothing>
}

/**
 * The single-user security profile (CS-15). A random database key is stored only in wrapped form:
 * the PIN and the normalized security answer each wrap it with AES-256-GCM, under a key-encryption key made from
 * PBKDF2-HMAC-SHA256 mixed with a device-bound key ([DeviceKeyMixer]).
 *
 * Failed PIN and answer checks share an escalating lockout timed on a [MonotonicClock], so changing the device's
 * date can't shorten it (CS-13). Public methods are synchronized, so checks can't run in parallel to beat the lockout.
 */
class SecurityProfileService(
    private val store: SecurityProfileStore,
    private val deviceKeyMixer: DeviceKeyMixer,
    private val clock: MonotonicClock,
    private val secureRandom: SecureRandom = SecureRandom(),
    private val iterations: Int = DEFAULT_ITERATIONS
) {
    private enum class Slot(id: String) {
        PIN("pin"),
        ANSWER("answer");

        val wrapKey = "${id}_wrap"
        val saltKey = "${id}_salt"
        val iterationsKey = "${id}_iterations"
        val context = "denarii-dolor:$id:v2".toByteArray(Charsets.UTF_8)
    }

    private class SealedKey(val slot: Slot, val salt: ByteArray, val iterations: Int, val wrapped: ByteArray)

    private fun isProfileConfigured(): Boolean = store.getInt(KEY_PROFILE_VERSION, 0) == PROFILE_VERSION &&
        !store.getString(KEY_SECURITY_QUESTION).isNullOrBlank() &&
        Slot.entries.all { slot ->
            !store.getString(slot.wrapKey).isNullOrBlank() &&
                !store.getString(slot.saltKey).isNullOrBlank() &&
                store.getInt(slot.iterationsKey, 0) in MIN_ITERATIONS..MAX_ITERATIONS
        }

    @Synchronized
    fun getProfileState(): SecurityProfileState = if (isProfileConfigured()) {
        SecurityProfileState(ProfileMode.SIGN_IN, store.getString(KEY_SECURITY_QUESTION))
    } else {
        SecurityProfileState(ProfileMode.FIRST_TIME_SETUP, null)
    }

    /** Creates the profile around a new random database key; the database itself is created at the first sign-in. */
    @Synchronized
    fun setupProfile(pin: String, pinConfirmation: String, securityQuestion: String, securityAnswer: String): SetupProfileResult {
        if (isProfileConfigured()) return SetupProfileResult.ALREADY_CONFIGURED
        val problem = validateSetup(pin, pinConfirmation, securityQuestion, securityAnswer)
        if (problem != SetupProfileResult.SUCCESS) return problem
        val databaseKey = ByteArray(DATABASE_KEY_BYTES).also(secureRandom::nextBytes)
        try {
            writeProfile(pin, securityQuestion, securityAnswer, databaseKey)
        } finally {
            databaseKey.fill(0)
        }
        return SetupProfileResult.SUCCESS
    }

    /** Upgrades a v1 (hash-only) profile: wraps its existing [databaseKey] with the already verified [pin] and a new answer. */
    @Synchronized
    fun migrateProfile(pin: String, securityQuestion: String, securityAnswer: String, databaseKey: ByteArray): SetupProfileResult {
        if (isProfileConfigured()) return SetupProfileResult.ALREADY_CONFIGURED
        val problem = validateSetup(pin, pin, securityQuestion, securityAnswer)
        if (problem == SetupProfileResult.SUCCESS) writeProfile(pin, securityQuestion, securityAnswer, databaseKey)
        return problem
    }

    /** Unwraps the database key with [pin]; the caller owns the returned key and must zero it when done. */
    @Synchronized
    fun unlockWithPin(pin: String): AttemptResult<ByteArray> {
        check(isProfileConfigured()) { "No security profile" }
        return attempt { open(Slot.PIN, pin) }
    }

    @Synchronized
    fun recoverPin(securityAnswer: String, newPin: String, pinConfirmation: String): RecoverPinResult {
        val normalizedAnswer = normalizeAnswer(securityAnswer)
        val problem = when {
            !isProfileConfigured() -> RecoverPinResult.PROFILE_NOT_CONFIGURED
            lockoutRemainingMillis() > 0 -> RecoverPinResult.LOCKED_OUT
            !isValidPin(newPin) -> RecoverPinResult.INVALID_PIN_FORMAT
            newPin != pinConfirmation -> RecoverPinResult.PIN_MISMATCH
            normalizedAnswer.isEmpty() -> RecoverPinResult.SECURITY_ANSWER_REQUIRED
            else -> null
        }
        if (problem != null) return problem
        return when (val result = attempt { open(Slot.ANSWER, normalizedAnswer) }) {
            is AttemptResult.Accepted -> {
                val databaseKey = result.value
                try {
                    val pinKey = seal(Slot.PIN, newPin, databaseKey)
                    store.edit { putSealed(pinKey) }
                } finally {
                    databaseKey.fill(0)
                }
                RecoverPinResult.SUCCESS
            }
            is AttemptResult.Rejected -> RecoverPinResult.INVALID_SECURITY_ANSWER
            is AttemptResult.LockedOut -> RecoverPinResult.LOCKED_OUT
        }
    }

    /** Runs [verify] under the shared lockout: a null result is a failed attempt, anything else resets the counter. */
    @Synchronized
    fun <T : Any> attempt(verify: () -> T?): AttemptResult<T> {
        lockoutRemainingMillis().takeIf { it > 0 }?.let { return AttemptResult.LockedOut(it) }
        val value = verify()
        if (value != null) {
            resetFailedAttempts()
            return AttemptResult.Accepted(value)
        }
        val failures = registerFailedAttempt()
        val remaining = lockoutRemainingMillis()
        return if (remaining > 0) AttemptResult.LockedOut(remaining) else AttemptResult.Rejected(MAX_FREE_ATTEMPTS - failures)
    }

    /** A successful biometric sign-in also clears the lockout (accepted risk CS-A3). */
    @Synchronized
    fun recordSuccessfulAuthentication() = resetFailedAttempts()

    @Synchronized
    fun lockoutRemainingMillis(): Long {
        val duration = store.getLong(KEY_LOCK_DURATION, 0L)
        if (duration <= 0L) return 0L
        val startedAt = store.getLong(KEY_LOCK_STARTED_AT, 0L)
        val storedBoot = store.getInt(KEY_LOCK_BOOT_COUNT, UNKNOWN_BOOT)
        val now = clock.elapsedRealtimeMillis()
        val boot = clock.bootCount() ?: UNKNOWN_BOOT
        val bootChanged = boot != UNKNOWN_BOOT && storedBoot != UNKNOWN_BOOT && boot != storedBoot
        if (now < startedAt || bootChanged) {
            // Elapsed time restarts at boot, so the time already served is unknown: serve the whole lockout again.
            store.edit {
                putLong(KEY_LOCK_STARTED_AT, now)
                putInt(KEY_LOCK_BOOT_COUNT, boot)
            }
            return duration
        }
        val remaining = startedAt + duration - now
        if (remaining <= 0L) store.edit { removeLockTimer() }
        return remaining.coerceAtLeast(0L)
    }

    /** The database key as wrapped by the biometric-bound Keystore key (IV followed by ciphertext), or null if not enrolled. */
    @Synchronized
    fun biometricWrap(): ByteArray? = store.getString(KEY_BIOMETRIC_WRAP)?.let(::decode)

    /** Stores the biometric wrap, or turns biometric sign-in off when [wrapped] is null. */
    @Synchronized
    fun setBiometricWrap(wrapped: ByteArray?) = store.edit {
        if (wrapped == null) remove(KEY_BIOMETRIC_WRAP) else putString(KEY_BIOMETRIC_WRAP, encode(wrapped))
    }

    private fun writeProfile(pin: String, securityQuestion: String, securityAnswer: String, databaseKey: ByteArray) {
        deviceKeyMixer.prepare()
        val pinKey = seal(Slot.PIN, pin, databaseKey)
        val answerKey = seal(Slot.ANSWER, normalizeAnswer(securityAnswer), databaseKey)
        store.edit {
            clear()
            putInt(KEY_PROFILE_VERSION, PROFILE_VERSION)
            putString(KEY_SECURITY_QUESTION, securityQuestion.trim())
            putSealed(pinKey)
            putSealed(answerKey)
        }
    }

    private fun seal(slot: Slot, secret: String, databaseKey: ByteArray): SealedKey {
        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val iv = ByteArray(GCM_IV_BYTES).also(secureRandom::nextBytes)
        val kek = keyEncryptionKey(slot, secret, salt, iterations)
        try {
            val cipher = Cipher.getInstance(AES_GCM).apply {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(kek, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
                updateAAD(slot.context)
            }
            return SealedKey(slot, salt, iterations, iv + cipher.doFinal(databaseKey))
        } finally {
            kek.fill(0)
        }
    }

    /** The database key, or null when [secret] is wrong. */
    private fun open(slot: Slot, secret: String): ByteArray? {
        val wrapped = requireStored(store.getString(slot.wrapKey)?.let(::decode))
        val salt = requireStored(store.getString(slot.saltKey)?.let(::decode))
        val storedIterations = store.getInt(slot.iterationsKey, 0)
        if (storedIterations !in MIN_ITERATIONS..MAX_ITERATIONS || wrapped.size <= GCM_IV_BYTES) {
            throw SecureStorageException("Security profile is corrupt")
        }
        val kek = keyEncryptionKey(slot, secret, salt, storedIterations)
        return try {
            Cipher.getInstance(AES_GCM).run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(kek, "AES"), GCMParameterSpec(GCM_TAG_BITS, wrapped, 0, GCM_IV_BYTES))
                updateAAD(slot.context)
                doFinal(wrapped, GCM_IV_BYTES, wrapped.size - GCM_IV_BYTES)
            }
        } catch (_: AEADBadTagException) {
            null
        } finally {
            kek.fill(0)
        }
    }

    private fun keyEncryptionKey(slot: Slot, secret: String, salt: ByteArray, iterations: Int): ByteArray {
        val stretched = pbkdf2(secret, salt, iterations)
        return try {
            deviceKeyMixer.mix(slot.context + stretched)
        } finally {
            stretched.fill(0)
        }
    }

    private fun registerFailedAttempt(): Int {
        val failures = store.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val duration = lockoutDurationMillis(failures)
        val now = clock.elapsedRealtimeMillis()
        val boot = clock.bootCount() ?: UNKNOWN_BOOT
        store.edit {
            putInt(KEY_FAILED_ATTEMPTS, failures)
            if (duration > 0L) {
                putLong(KEY_LOCK_DURATION, duration)
                putLong(KEY_LOCK_STARTED_AT, now)
                putInt(KEY_LOCK_BOOT_COUNT, boot)
            }
        }
        return failures
    }

    private fun resetFailedAttempts() {
        store.edit {
            remove(KEY_FAILED_ATTEMPTS)
            removeLockTimer()
        }
    }

    private fun SecurityProfileStoreEditor.removeLockTimer() {
        remove(KEY_LOCK_DURATION)
        remove(KEY_LOCK_STARTED_AT)
        remove(KEY_LOCK_BOOT_COUNT)
    }

    private fun SecurityProfileStoreEditor.putSealed(sealed: SealedKey) {
        putString(sealed.slot.wrapKey, encode(sealed.wrapped))
        putString(sealed.slot.saltKey, encode(sealed.salt))
        putInt(sealed.slot.iterationsKey, sealed.iterations)
    }

    private fun <T> requireStored(value: T?): T = value ?: throw SecureStorageException("Security profile is incomplete")

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray? = runCatching { Base64.getDecoder().decode(value) }.getOrNull()

    companion object {
        private const val PROFILE_VERSION = 2
        private const val AES_GCM = "AES/GCM/NoPadding"
        private const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val SALT_BYTES = 16
        private const val MAX_ITERATIONS = 1_000_000
        private const val MAX_DOUBLINGS = 5
        private const val UNKNOWN_BOOT = -1
        private val PIN_REGEX = Regex("^[0-9]{6,12}$") // NIST SP 800-63B: numeric secrets need at least 6 digits
        private val WHITESPACE = Regex("\\s+")

        const val DEFAULT_ITERATIONS = 210_000
        const val MIN_ITERATIONS = 10_000
        const val DATABASE_KEY_BYTES = 32
        const val MIN_ANSWER_LENGTH = 6
        const val MAX_ANSWER_LENGTH = 100
        const val MAX_QUESTION_LENGTH = 200
        const val MAX_FREE_ATTEMPTS = 5
        const val BASE_LOCKOUT_MILLIS = 30_000L
        const val MAX_LOCKOUT_MILLIS = 15 * 60_000L

        const val KEY_PROFILE_VERSION = "profile_version"
        const val KEY_SECURITY_QUESTION = "security_question"
        const val KEY_BIOMETRIC_WRAP = "biometric_wrap"
        const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        const val KEY_LOCK_DURATION = "lock_duration_millis"
        const val KEY_LOCK_STARTED_AT = "lock_started_at_elapsed_millis"
        const val KEY_LOCK_BOOT_COUNT = "lock_boot_count"

        fun isValidPin(pin: String): Boolean = PIN_REGEX.matches(pin)

        /** Answers are compared ignoring case, spacing and Unicode form (NFKC), so users aren't pushed to trivial answers. */
        fun normalizeAnswer(answer: String): String =
            Normalizer.normalize(answer, Normalizer.Form.NFKC).trim().replace(WHITESPACE, " ").lowercase()

        /** [SetupProfileResult.SUCCESS] when the setup input meets the PIN rules and the recovery rules (CS-14). */
        fun validateSetup(pin: String, pinConfirmation: String, securityQuestion: String, securityAnswer: String): SetupProfileResult {
            val question = securityQuestion.trim()
            val answer = normalizeAnswer(securityAnswer)
            return when {
                !isValidPin(pin) -> SetupProfileResult.INVALID_PIN_FORMAT
                pin != pinConfirmation -> SetupProfileResult.PIN_MISMATCH
                question.isEmpty() -> SetupProfileResult.SECURITY_QUESTION_REQUIRED
                question.length > MAX_QUESTION_LENGTH -> SetupProfileResult.SECURITY_QUESTION_TOO_LONG
                answer.isEmpty() -> SetupProfileResult.SECURITY_ANSWER_REQUIRED
                answer.codePointCount(0, answer.length) < MIN_ANSWER_LENGTH -> SetupProfileResult.SECURITY_ANSWER_TOO_SHORT
                answer.length > MAX_ANSWER_LENGTH -> SetupProfileResult.SECURITY_ANSWER_TOO_LONG
                normalizeAnswer(question).contains(answer) -> SetupProfileResult.SECURITY_ANSWER_IN_QUESTION
                answer == pin -> SetupProfileResult.SECURITY_ANSWER_MATCHES_PIN
                else -> SetupProfileResult.SUCCESS
            }
        }

        /** 5th failure locks for 30s; each further failure doubles it, capped at 15 minutes. */
        fun lockoutDurationMillis(failures: Int): Long {
            if (failures < MAX_FREE_ATTEMPTS) return 0L
            val doublings = (failures - MAX_FREE_ATTEMPTS).coerceAtMost(MAX_DOUBLINGS)
            return (BASE_LOCKOUT_MILLIS shl doublings).coerceAtMost(MAX_LOCKOUT_MILLIS)
        }
    }
}
