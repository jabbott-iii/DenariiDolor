package com.denariidolor.data.local.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val securityProfileService = SecurityProfileService(
        object : SecurityProfileStore {
            override fun getString(key: String): String? = sharedPreferences.getString(key, null)

            override fun getInt(key: String, defaultValue: Int): Int = sharedPreferences.getInt(key, defaultValue)

            override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
                return sharedPreferences.getBoolean(key, defaultValue)
            }

            override fun edit(block: SecurityProfileStoreEditor.() -> Unit) {
                val editor = sharedPreferences.edit()
                val storeEditor = object : SecurityProfileStoreEditor {
                    override fun putString(key: String, value: String) {
                        editor.putString(key, value)
                    }

                    override fun putInt(key: String, value: Int) {
                        editor.putInt(key, value)
                    }

                    override fun putBoolean(key: String, value: Boolean) {
                        editor.putBoolean(key, value)
                    }

                    override fun remove(key: String) {
                        editor.remove(key)
                    }

                    override fun clear() {
                        editor.clear()
                    }
                }
                block(storeEditor)
                editor.apply()
            }
        }
    )

    fun getProfileState(): SecurityProfileState = securityProfileService.getProfileState()

    fun setupProfile(
        pin: String,
        pinConfirmation: String,
        securityQuestion: String,
        securityAnswer: String
    ): SetupProfileResult {
        return securityProfileService.setupProfile(
            pin = pin,
            pinConfirmation = pinConfirmation,
            securityQuestion = securityQuestion,
            securityAnswer = securityAnswer
        )
    }

    fun verifyPin(pin: String): Boolean = securityProfileService.verifyPin(pin)

    fun recoverPin(
        securityAnswer: String,
        newPin: String,
        pinConfirmation: String
    ): RecoverPinResult {
        return securityProfileService.recoverPin(
            securityAnswer = securityAnswer,
            newPin = newPin,
            pinConfirmation = pinConfirmation
        )
    }

    fun isProfileConfigured(): Boolean = securityProfileService.isProfileConfigured()

    fun getSecurityQuestion(): String? = getProfileState().securityQuestion

    fun clearAllSecurityData() {
        securityProfileService.wipeAll()
    }

    fun clearPinForSignInReset() {
        securityProfileService.clearPinForSignInReset()
    }
}
