package com.denariidolor.data.local.preferences

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

interface SecurityProfileStore {
    fun getString(key: String): String?
    fun getInt(key: String, defaultValue: Int): Int
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun edit(block: SecurityProfileStoreEditor.() -> Unit)
}

interface SecurityProfileStoreEditor {
    fun putString(key: String, value: String)
    fun putInt(key: String, value: Int)
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
    fun clear()
}

enum class ProfileMode {
    FIRST_TIME_SETUP,
    SIGN_IN
}

data class SecurityProfileState(
    val mode: ProfileMode,
    val securityQuestion: String?
)

enum class SetupProfileResult {
    SUCCESS,
    ALREADY_CONFIGURED,
    INVALID_PIN_FORMAT,
    PIN_MISMATCH,
    SECURITY_QUESTION_REQUIRED,
    SECURITY_ANSWER_REQUIRED,
    LEGACY_PIN_MISMATCH
}

enum class RecoverPinResult {
    SUCCESS,
    PROFILE_NOT_CONFIGURED,
    SECURITY_ANSWER_REQUIRED,
    INVALID_SECURITY_ANSWER,
    INVALID_PIN_FORMAT,
    PIN_MISMATCH
}

class SecurityProfileService(
    private val store: SecurityProfileStore,
    private val secureRandom: SecureRandom = SecureRandom()
) {
    fun getProfileState(): SecurityProfileState {
        return SecurityProfileState(
            mode = if (isProfileConfigured()) ProfileMode.SIGN_IN else ProfileMode.FIRST_TIME_SETUP,
            securityQuestion = if (isProfileConfigured()) store.getString(KEY_SECURITY_QUESTION) else null
        )
    }

    fun setupProfile(
        pin: String,
        pinConfirmation: String,
        securityQuestion: String,
        securityAnswer: String
    ): SetupProfileResult {
        if (isProfileConfigured()) {
            return SetupProfileResult.ALREADY_CONFIGURED
        }

        if (!isValidPin(pin)) {
            return SetupProfileResult.INVALID_PIN_FORMAT
        }
        if (pin != pinConfirmation) {
            return SetupProfileResult.PIN_MISMATCH
        }

        val trimmedQuestion = securityQuestion.trim()
        if (trimmedQuestion.isEmpty()) {
            return SetupProfileResult.SECURITY_QUESTION_REQUIRED
        }

        val trimmedAnswer = securityAnswer.trim()
        if (trimmedAnswer.isEmpty()) {
            return SetupProfileResult.SECURITY_ANSWER_REQUIRED
        }

        val legacyPin = store.getString(KEY_LEGACY_PIN)
        if (legacyPin != null && !constantTimeStringEquals(pin, legacyPin)) {
            return SetupProfileResult.LEGACY_PIN_MISMATCH
        }

        val pinHash = hashSecret(pin)
        val answerHash = hashSecret(trimmedAnswer)

        store.edit {
            putString(KEY_PIN_HASH, pinHash.hash)
            putString(KEY_PIN_SALT, pinHash.salt)
            putInt(KEY_PIN_ITERATIONS, pinHash.iterations)
            putString(KEY_SECURITY_QUESTION, trimmedQuestion)
            putString(KEY_SECURITY_ANSWER_HASH, answerHash.hash)
            putString(KEY_SECURITY_ANSWER_SALT, answerHash.salt)
            putInt(KEY_SECURITY_ANSWER_ITERATIONS, answerHash.iterations)
            putBoolean(KEY_PROFILE_CONFIGURED, true)
            remove(KEY_LEGACY_PIN)
        }

        return SetupProfileResult.SUCCESS
    }

    fun verifyPin(pin: String): Boolean {
        if (isProfileConfigured()) {
            return verifyHashedSecret(
                secret = pin,
                hash = store.getString(KEY_PIN_HASH),
                salt = store.getString(KEY_PIN_SALT),
                iterations = store.getInt(KEY_PIN_ITERATIONS, DEFAULT_ITERATIONS)
            )
        }

        val legacyPin = store.getString(KEY_LEGACY_PIN) ?: return false
        return constantTimeStringEquals(pin, legacyPin)
    }

    fun recoverPin(
        securityAnswer: String,
        newPin: String,
        pinConfirmation: String
    ): RecoverPinResult {
        if (!isProfileConfigured()) {
            return RecoverPinResult.PROFILE_NOT_CONFIGURED
        }

        if (!isValidPin(newPin)) {
            return RecoverPinResult.INVALID_PIN_FORMAT
        }

        if (newPin != pinConfirmation) {
            return RecoverPinResult.PIN_MISMATCH
        }

        val trimmedAnswer = securityAnswer.trim()
        if (trimmedAnswer.isEmpty()) {
            return RecoverPinResult.SECURITY_ANSWER_REQUIRED
        }

        val answerMatches = verifyHashedSecret(
            secret = trimmedAnswer,
            hash = store.getString(KEY_SECURITY_ANSWER_HASH),
            salt = store.getString(KEY_SECURITY_ANSWER_SALT),
            iterations = store.getInt(KEY_SECURITY_ANSWER_ITERATIONS, DEFAULT_ITERATIONS)
        )
        if (!answerMatches) {
            return RecoverPinResult.INVALID_SECURITY_ANSWER
        }

        val pinHash = hashSecret(newPin)
        store.edit {
            putString(KEY_PIN_HASH, pinHash.hash)
            putString(KEY_PIN_SALT, pinHash.salt)
            putInt(KEY_PIN_ITERATIONS, pinHash.iterations)
            remove(KEY_LEGACY_PIN)
        }

        return RecoverPinResult.SUCCESS
    }

    fun isProfileConfigured(): Boolean {
        val pinIterations = store.getInt(KEY_PIN_ITERATIONS, 0)
        val answerIterations = store.getInt(KEY_SECURITY_ANSWER_ITERATIONS, 0)
        return store.getBoolean(KEY_PROFILE_CONFIGURED, false) &&
            !store.getString(KEY_PIN_HASH).isNullOrBlank() &&
            !store.getString(KEY_PIN_SALT).isNullOrBlank() &&
            !store.getString(KEY_SECURITY_QUESTION).isNullOrBlank() &&
            !store.getString(KEY_SECURITY_ANSWER_HASH).isNullOrBlank() &&
            !store.getString(KEY_SECURITY_ANSWER_SALT).isNullOrBlank() &&
            pinIterations in MIN_ITERATIONS..MAX_ITERATIONS &&
            answerIterations in MIN_ITERATIONS..MAX_ITERATIONS
    }

    fun wipeAll() {
        store.edit { clear() }
    }

    private fun hashSecret(secret: String): HashedSecret {
        val saltBytes = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val hashBytes = derivePbkdf2(secret, saltBytes, DEFAULT_ITERATIONS)
        return HashedSecret(
            hash = Base64.getEncoder().encodeToString(hashBytes),
            salt = Base64.getEncoder().encodeToString(saltBytes),
            iterations = DEFAULT_ITERATIONS
        )
    }

    private fun verifyHashedSecret(secret: String, hash: String?, salt: String?, iterations: Int): Boolean {
        if (hash.isNullOrBlank() || salt.isNullOrBlank()) {
            return false
        }
        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
            return false
        }

        val expectedHash = runCatching { Base64.getDecoder().decode(hash) }.getOrNull() ?: return false
        val saltBytes = runCatching { Base64.getDecoder().decode(salt) }.getOrNull() ?: return false
        val candidateHash = derivePbkdf2(secret, saltBytes, iterations)
        return MessageDigest.isEqual(expectedHash, candidateHash)
    }

    private fun derivePbkdf2(secret: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, iterations, DERIVED_KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun isValidPin(pin: String): Boolean = PIN_REGEX.matches(pin)

    private fun constantTimeStringEquals(left: String, right: String): Boolean {
        return MessageDigest.isEqual(left.toByteArray(Charsets.UTF_8), right.toByteArray(Charsets.UTF_8))
    }

    private data class HashedSecret(
        val hash: String,
        val salt: String,
        val iterations: Int
    )

    companion object {
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val DEFAULT_ITERATIONS = 210_000
        private const val MIN_ITERATIONS = 10_000
        private const val MAX_ITERATIONS = 1_000_000
        private const val DERIVED_KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16
        private val PIN_REGEX = Regex("^[0-9]{4,12}$")

        const val KEY_PROFILE_CONFIGURED = "key_profile_configured"
        const val KEY_PIN_HASH = "key_pin_hash"
        const val KEY_PIN_SALT = "key_pin_salt"
        const val KEY_PIN_ITERATIONS = "key_pin_iterations"
        const val KEY_SECURITY_QUESTION = "key_security_question"
        const val KEY_SECURITY_ANSWER_HASH = "key_security_answer_hash"
        const val KEY_SECURITY_ANSWER_SALT = "key_security_answer_salt"
        const val KEY_SECURITY_ANSWER_ITERATIONS = "key_security_answer_iterations"
        const val KEY_LEGACY_PIN = "key_pin"
    }
}
