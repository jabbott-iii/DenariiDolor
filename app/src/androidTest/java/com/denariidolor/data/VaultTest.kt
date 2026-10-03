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

package com.denariidolor.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.DatabaseHolder
import com.denariidolor.data.local.db.DefaultDataInitializer
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.security.DatabaseKeys
import com.denariidolor.data.local.preferences.LegacySecurityProfile
import com.denariidolor.data.local.preferences.RecoverPinResult
import com.denariidolor.data.local.preferences.SecureStorageException
import com.denariidolor.data.local.preferences.SecurityProfileService
import com.denariidolor.data.local.preferences.SetupProfileResult
import com.denariidolor.data.local.vault.BiometricSignIn
import com.denariidolor.data.local.vault.RecoveryResult
import com.denariidolor.data.local.vault.SignInResult
import com.denariidolor.data.local.vault.Vault
import com.denariidolor.data.local.vault.VaultConfig
import com.denariidolor.data.local.vault.VaultState
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The vault on a real Keystore and SQLCipher, under test-only names so the app's own data is never touched. */
@RunWith(AndroidJUnit4::class)
class VaultTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val config = VaultConfig(
        databaseName = "vault_test.db",
        profilePrefsName = "vault_test_profile",
        keyAliasPrefix = "vault_test_",
        legacySecurePrefsName = "vault_test_secure_prefs",
        legacyDatabaseKeyPrefsName = "vault_test_db_key_prefs",
        legacyMasterKeyAlias = "vault_test_legacy_master_key"
    )
    private val holder = DatabaseHolder(context, config)
    private var vault = newVault()

    private fun newVault() = Vault(context, config, holder, DefaultDataInitializer(holder))

    @Before
    fun setUp() = runBlocking<Unit> { vault.wipe() }

    @After
    fun tearDown() = runBlocking<Unit> { vault.wipe() }

    private suspend fun setUpAndSignIn() {
        assertEquals(SetupProfileResult.SUCCESS, vault.setUp(PIN, PIN, QUESTION, ANSWER))
        assertEquals(SignInResult.Unlocked, vault.signIn(PIN))
    }

    private suspend fun accountNames(): List<String> = holder.database.accountDao().getAll().first().map { it.name }

    private suspend fun addTravelAccount() = holder.database.accountDao().insert(AccountEntity(name = "Travel", balanceCents = 500))

    private fun prefsFile(name: String) = File(context.dataDir, "shared_prefs/$name.xml")

    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun keyAliases(): List<String> = keyStore().aliases().toList()

    @Test
    fun databaseOpensOnlyAfterSignIn() = runBlocking<Unit> {
        assertEquals(VaultState.NeedsSetup, vault.state())
        assertEquals(SetupProfileResult.SUCCESS, vault.setUp(PIN, PIN, QUESTION, ANSWER))

        assertFalse(vault.isUnlocked)
        assertThrows(IllegalStateException::class.java) { holder.database }
        assertEquals(VaultState.Configured(QUESTION, biometricEnrolled = false, upgradeStarted = false), vault.state())

        assertEquals(SignInResult.Unlocked, vault.signIn(PIN))
        assertTrue(vault.isUnlocked)
        assertEquals(listOf("Cash", "Savings"), accountNames())
    }

    @Test
    fun wrongPinKeepsTheDatabaseClosedAndDataSurvivesLocking() = runBlocking<Unit> {
        setUpAndSignIn()
        addTravelAccount()
        vault.lock()

        assertTrue(vault.signIn("000000") is SignInResult.InvalidPin)
        assertFalse(vault.isUnlocked)
        assertEquals(SignInResult.Unlocked, vault.signIn(PIN))
        assertTrue("Travel" in accountNames())
    }

    @Test
    fun recoveryAnswerReplacesThePinAndKeepsTheData() = runBlocking<Unit> {
        setUpAndSignIn()
        addTravelAccount()
        vault.lock()

        assertEquals(RecoveryResult.Finished(RecoverPinResult.SUCCESS), vault.recoverPin("  LINCOLN   elementary ", NEW_PIN, NEW_PIN))
        assertTrue(vault.signIn(PIN) is SignInResult.InvalidPin)
        assertEquals(SignInResult.Unlocked, vault.signIn(NEW_PIN))
        assertTrue("Travel" in accountNames())
    }

    @Test
    fun storedProfileRevealsNoSecrets() = runBlocking<Unit> {
        setUpAndSignIn()

        val stored = prefsFile(config.profilePrefsName).readText()

        listOf(PIN, QUESTION, ANSWER, SecurityProfileService.normalizeAnswer(ANSWER)).forEach { assertFalse(stored.contains(it)) }
    }

    @Test
    fun biometricSignInStartsTurnedOffAndCannotBeEnrolledWhileLocked() = runBlocking<Unit> {
        assertEquals(SetupProfileResult.SUCCESS, vault.setUp(PIN, PIN, QUESTION, ANSWER))

        assertEquals(BiometricSignIn.NotEnrolled, vault.prepareBiometricSignIn())
        assertThrows(IllegalStateException::class.java) { runBlocking { vault.prepareBiometricEnrollment() } }
    }

    @Test
    fun wipeErasesTheDatabaseTheProfileAndEveryKey() = runBlocking<Unit> {
        setUpAndSignIn()
        addTravelAccount()
        val databaseFile = context.getDatabasePath(config.databaseName)
        assertTrue(databaseFile.exists())

        assertTrue(vault.wipe())

        assertFalse(databaseFile.exists())
        assertFalse(prefsFile(config.profilePrefsName).exists())
        assertTrue(keyAliases().none { it.startsWith(config.keyAliasPrefix) })
        assertEquals(VaultState.NeedsSetup, vault.state())
        setUpAndSignIn()
        assertEquals(listOf("Cash", "Savings"), accountNames())
    }

    @Test
    fun lostStorageKeyIsAStorageFailure() = runBlocking<Unit> {
        assertEquals(SetupProfileResult.SUCCESS, vault.setUp(PIN, PIN, QUESTION, ANSWER))
        keyStore().deleteEntry("${config.keyAliasPrefix}storage")
        vault = newVault()

        assertThrows(SecureStorageException::class.java) { runBlocking { vault.state() } }
    }

    @Test
    fun lostDeviceKeyIsAStorageFailureNotAWrongPin() = runBlocking<Unit> {
        assertEquals(SetupProfileResult.SUCCESS, vault.setUp(PIN, PIN, QUESTION, ANSWER))
        keyStore().deleteEntry("${config.keyAliasPrefix}device")

        assertThrows(SecureStorageException::class.java) { runBlocking { vault.signIn(PIN) } }
    }

    @Test
    fun orphanedDatabaseIsReplacedAtSetup() = runBlocking<Unit> {
        context.getDatabasePath(config.databaseName).apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(4_096) { 7 })
        }

        setUpAndSignIn()

        assertEquals(listOf("Cash", "Savings"), accountNames())
    }

    @Test
    fun v1InstallUpgradesAtSignInAndKeepsItsData() = runBlocking<Unit> {
        writeV1Install(pin = V1_PIN, question = "Favorite color?", answer = "Blue")
        vault = newVault()

        assertEquals(VaultState.Configured("Favorite color?", biometricEnrolled = false, upgradeStarted = false), vault.state())
        assertEquals(SignInResult.UpgradeRequired, vault.signIn(V1_PIN))
        assertEquals(SetupProfileResult.SECURITY_ANSWER_TOO_SHORT, vault.completeUpgrade("Favorite color?", "Blue"))
        assertEquals(SetupProfileResult.SUCCESS, vault.completeUpgrade(QUESTION, ANSWER))

        assertTrue(vault.isUnlocked)
        assertTrue("Legacy savings" in accountNames())
        assertFalse(prefsFile(config.legacySecurePrefsName).exists())
        assertFalse(prefsFile(config.legacyDatabaseKeyPrefsName).exists())
        assertFalse(config.legacyMasterKeyAlias in keyAliases())

        vault.lock()
        assertEquals(SignInResult.Unlocked, vault.signIn(V1_PIN))
        assertTrue("Legacy savings" in accountNames())
    }

    @Test
    fun v1RecoveryAlsoUpgrades() = runBlocking<Unit> {
        writeV1Install(pin = V1_PIN, question = "Favorite color?", answer = "Blue")
        vault = newVault()

        assertEquals(RecoveryResult.Finished(RecoverPinResult.INVALID_SECURITY_ANSWER), vault.recoverPin("blue", NEW_PIN, NEW_PIN))
        assertEquals(RecoveryResult.UpgradeRequired, vault.recoverPin("Blue", NEW_PIN, NEW_PIN))
        assertEquals(SetupProfileResult.SUCCESS, vault.completeUpgrade(QUESTION, ANSWER))
        assertTrue("Legacy savings" in accountNames())

        vault.lock()
        assertEquals(SignInResult.Unlocked, vault.signIn(NEW_PIN))
    }

    /** Recreates what v1.0.x stored: PBKDF2 hashes in one EncryptedSharedPreferences file, the database key in another. */
    private fun writeV1Install(pin: String, question: String, answer: String) {
        val masterKey = MasterKey.Builder(context, config.legacyMasterKeyAlias).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        fun open(name: String) = EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        open(config.legacySecurePrefsName).edit()
            .putBoolean(LegacySecurityProfile.KEY_PROFILE_CONFIGURED, true)
            .putString(LegacySecurityProfile.KEY_SECURITY_QUESTION, question)
            .putHash(pin, LegacySecurityProfile.KEY_PIN_HASH, LegacySecurityProfile.KEY_PIN_SALT, LegacySecurityProfile.KEY_PIN_ITERATIONS)
            .putHash(
                answer,
                LegacySecurityProfile.KEY_SECURITY_ANSWER_HASH,
                LegacySecurityProfile.KEY_SECURITY_ANSWER_SALT,
                LegacySecurityProfile.KEY_SECURITY_ANSWER_ITERATIONS
            )
            .commit()
        val hexKey = DatabaseKeys.generateHexKey(SecureRandom())
        open(config.legacyDatabaseKeyPrefsName).edit().putString("db_key_hex", hexKey).commit()

        System.loadLibrary("sqlcipher")
        val database = Room.databaseBuilder(context, AppDatabase::class.java, config.databaseName)
            .openHelperFactory(SupportOpenHelperFactory(DatabaseKeys.toRawKeyPassphrase(hexKey)))
            .build()
        runBlocking { database.accountDao().insert(AccountEntity(name = "Legacy savings", balanceCents = 12_345)) }
        database.close()
    }

    private fun SharedPreferences.Editor.putHash(
        secret: String,
        hashKey: String,
        saltKey: String,
        iterationsKey: String
    ): SharedPreferences.Editor {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(secret.toCharArray(), salt, V1_ITERATIONS, 256))
            .encoded
        return putString(hashKey, Base64.getEncoder().encodeToString(hash))
            .putString(saltKey, Base64.getEncoder().encodeToString(salt))
            .putInt(iterationsKey, V1_ITERATIONS)
    }

    private companion object {
        const val PIN = "482915"
        const val NEW_PIN = "730264"
        const val V1_PIN = "246801"
        const val V1_ITERATIONS = 210_000
        const val QUESTION = "Name of my first school?"
        const val ANSWER = "Lincoln Elementary"
    }
}
