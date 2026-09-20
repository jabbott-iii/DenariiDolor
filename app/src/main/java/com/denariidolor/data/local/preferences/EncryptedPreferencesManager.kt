package com.denariidolor.data.local.preferences

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.denariidolor.domain.auth.PinSecurityStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) : PinSecurityStore {
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun isPinConfigured(): Boolean {
        return sharedPreferences.getString(KEY_PIN_HASH, null) != null &&
            sharedPreferences.getString(KEY_PIN_SALT, null) != null &&
            sharedPreferences.getString(KEY_SECURITY_QUESTION, null) != null &&
            sharedPreferences.getString(KEY_SECURITY_ANSWER_HASH, null) != null &&
            sharedPreferences.getString(KEY_SECURITY_ANSWER_SALT, null) != null
    }

    override fun createPinSecurity(pin: String, securityQuestion: String, securityAnswer: String): Boolean {
        if (isPinConfigured()) return false
        val pinSalt = generateSalt()
        val answerSalt = generateSalt()
        sharedPreferences.edit()
            .putString(KEY_PIN_HASH, hashSecret(pin, pinSalt))
            .putString(KEY_PIN_SALT, Base64.encodeToString(pinSalt, Base64.NO_WRAP))
            .putString(KEY_SECURITY_QUESTION, securityQuestion)
            .putString(KEY_SECURITY_ANSWER_HASH, hashSecret(securityAnswer, answerSalt))
            .putString(KEY_SECURITY_ANSWER_SALT, Base64.encodeToString(answerSalt, Base64.NO_WRAP))
            .apply()
        return true
    }

    override fun verifyPin(pin: String): Boolean {
        val storedHash = sharedPreferences.getString(KEY_PIN_HASH, null) ?: return false
        val salt = sharedPreferences.getString(KEY_PIN_SALT, null)?.let(::decodeSalt) ?: return false
        return constantTimeEquals(storedHash, hashSecret(pin, salt))
    }

    override fun getSecurityQuestion(): String? = sharedPreferences.getString(KEY_SECURITY_QUESTION, null)

    override fun verifySecurityAnswer(securityAnswer: String): Boolean {
        val storedHash = sharedPreferences.getString(KEY_SECURITY_ANSWER_HASH, null) ?: return false
        val salt = sharedPreferences.getString(KEY_SECURITY_ANSWER_SALT, null)?.let(::decodeSalt) ?: return false
        return constantTimeEquals(storedHash, hashSecret(securityAnswer, salt))
    }

    override fun resetPin(pin: String): Boolean {
        if (!isPinConfigured()) return false
        val pinSalt = generateSalt()
        sharedPreferences.edit()
            .putString(KEY_PIN_HASH, hashSecret(pin, pinSalt))
            .putString(KEY_PIN_SALT, Base64.encodeToString(pinSalt, Base64.NO_WRAP))
            .apply()
        return true
    }

    override fun clearPin() {
        sharedPreferences.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_SECURITY_QUESTION)
            .remove(KEY_SECURITY_ANSWER_HASH)
            .remove(KEY_SECURITY_ANSWER_SALT)
            .apply()
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    private fun generateSalt(): ByteArray = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }

    private fun decodeSalt(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    private fun hashSecret(secret: String, salt: ByteArray): String {
        val keySpec = PBEKeySpec(secret.toCharArray(), salt, HASH_ITERATIONS, KEY_LENGTH_BITS)
        val hash = secretKeyFactory.generateSecret(keySpec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private fun constantTimeEquals(expected: String, actual: String): Boolean {
        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.UTF_8),
            actual.toByteArray(Charsets.UTF_8)
        )
    }

    companion object {
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
        private const val KEY_SECURITY_QUESTION = "key_security_question"
        private const val KEY_SECURITY_ANSWER_HASH = "key_security_answer_hash"
        private const val KEY_SECURITY_ANSWER_SALT = "key_security_answer_salt"
        private const val SALT_BYTES = 16
        private const val HASH_ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
        private val secureRandom = SecureRandom()
        private val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    }
}
