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

    private fun encryptedPrefs() = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private const val KEY_SECURITY_ANSWER_HASH = "key_security_answer_hash"
    }
}
