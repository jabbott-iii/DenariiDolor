package com.denariidolor.data.local.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedPreferencesManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = EncryptedPreferencesManager(context)

    @Before
    fun setUp() {
        context.deleteSharedPreferences(PREFS_NAME)
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(PREFS_NAME)
    }

    @Test
    fun isPinConfiguredFalseWhenPersistedStateIsPartial() {
        manager.createPinSecurity(pin = "1234", securityQuestion = "City?", securityAnswer = "Rome")
        assertTrue(manager.isPinConfigured())

        encryptedPrefs().edit()
            .remove(KEY_SECURITY_ANSWER_HASH)
            .commit()

        assertFalse(manager.isPinConfigured())
    }

    @Test
    fun createPinSecuritySupportsHappyPathVerification() {
        manager.createPinSecurity(pin = "1234", securityQuestion = "City?", securityAnswer = "Rome")

        assertTrue(manager.isPinConfigured())
        assertTrue(manager.verifyPin("1234"))
        assertTrue(manager.verifySecurityAnswer("Rome"))
    }

    @Test
    fun isPinConfiguredFalseWhenSecurityQuestionBlank() {
        manager.createPinSecurity(pin = "1234", securityQuestion = "City?", securityAnswer = "Rome")
        encryptedPrefs().edit()
            .putString(KEY_SECURITY_QUESTION, "")
            .commit()

        assertFalse(manager.isPinConfigured())
    }

    @Test
    fun isPinConfiguredFalseWhenPinHashBlank() {
        manager.createPinSecurity(pin = "1234", securityQuestion = "City?", securityAnswer = "Rome")
        encryptedPrefs().edit()
            .putString(KEY_PIN_HASH, "")
            .commit()

        assertFalse(manager.isPinConfigured())
    }

    @Test
    fun verifyPinFailsWhenPinSaltMissing() {
        manager.createPinSecurity(pin = "1234", securityQuestion = "City?", securityAnswer = "Rome")
        encryptedPrefs().edit()
            .remove(KEY_PIN_SALT)
            .commit()

        assertFalse(manager.verifyPin("1234"))
    }

    @Test
    fun verifyPinFailsWhenPinSaltMalformed() {
        manager.createPinSecurity(pin = "1234", securityQuestion = "City?", securityAnswer = "Rome")
        encryptedPrefs().edit()
            .putString(KEY_PIN_SALT, "not-a-valid-base64-value")
            .commit()

        assertFalse(manager.verifyPin("1234"))
    }

    @Test
    fun verifyPinFailsWhenPinHashMalformed() {
        manager.createPinSecurity(pin = "1234", securityQuestion = "City?", securityAnswer = "Rome")
        encryptedPrefs().edit()
            .putString(KEY_PIN_HASH, "not-a-valid-base64-value")
            .commit()

        assertFalse(manager.verifyPin("1234"))
    }

    @Test
    fun verifySecurityAnswerFailsWhenAnswerSaltMissing() {
        manager.createPinSecurity(pin = "1234", securityQuestion = "City?", securityAnswer = "Rome")
        encryptedPrefs().edit()
            .remove(KEY_SECURITY_ANSWER_SALT)
            .commit()

        assertFalse(manager.verifySecurityAnswer("Rome"))
    }

    @Test
    fun verifySecurityAnswerFailsWhenAnswerSaltMalformed() {
        manager.createPinSecurity(pin = "1234", securityQuestion = "City?", securityAnswer = "Rome")
        encryptedPrefs().edit()
            .putString(KEY_SECURITY_ANSWER_SALT, "not-a-valid-base64-value")
            .commit()

        assertFalse(manager.verifySecurityAnswer("Rome"))
    }

    @Test
    fun verifySecurityAnswerFailsWhenAnswerHashMalformed() {
        manager.createPinSecurity(pin = "1234", securityQuestion = "City?", securityAnswer = "Rome")
        encryptedPrefs().edit()
            .putString(KEY_SECURITY_ANSWER_HASH, "not-a-valid-base64-value")
            .commit()

        assertFalse(manager.verifySecurityAnswer("Rome"))
    }

    @Test
    fun resetPinFailsWhenPersistedStateIncomplete() {
        manager.createPinSecurity(pin = "1234", securityQuestion = "City?", securityAnswer = "Rome")
        encryptedPrefs().edit()
            .remove(KEY_SECURITY_QUESTION)
            .commit()

        assertFalse(manager.resetPin("9999"))
    }

    @Test
    fun resetPinUpdatesCredentialToNewPin() {
        manager.createPinSecurity(pin = "1234", securityQuestion = "City?", securityAnswer = "Rome")

        assertTrue(manager.resetPin("9999"))
        assertFalse(manager.verifyPin("1234"))
        assertTrue(manager.verifyPin("9999"))
    }

    private fun encryptedPrefs() = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_SECURITY_QUESTION = "key_security_question"
        private const val KEY_SECURITY_ANSWER_HASH = "key_security_answer_hash"
        private const val KEY_SECURITY_ANSWER_SALT = "key_security_answer_salt"
    }
}
