package com.denariidolor.data.local.preferences

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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
    PIN_MISMATCH,
    LOCKED_OUT
}

sealed interface PinAttemptResult {
    data object Success : PinAttemptResult
    data class Invalid(val attemptsBeforeLockout: Int) : PinAttemptResult
    data class LockedOut(val remainingMillis: Long) : PinAttemptResult
}

class SecurityProfileService(
    private val store: SecurityProfileStore,
    private val secureRandom: SecureRandom = SecureRandom(),
    private val clock: () -> Long = System::currentTimeMillis
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

    fun attemptPin(pin: String): PinAttemptResult {
        lockoutRemainingMillis().takeIf { it > 0 }?.let { return PinAttemptResult.LockedOut(it) }
        if (verifyPin(pin)) {
            resetFailedAttempts()
            return PinAttemptResult.Success
        }
        val failures = registerFailedAttempt()
        val remaining = lockoutRemainingMillis()
        return if (remaining > 0) {
            PinAttemptResult.LockedOut(remaining)
        } else {
            PinAttemptResult.Invalid(attemptsBeforeLockout = MAX_FREE_ATTEMPTS - failures)
        }
    }

    fun recordSuccessfulAuthentication() = resetFailedAttempts()

    fun lockoutRemainingMillis(): Long {
        val lockedUntil = store.getLong(KEY_LOCKED_UNTIL, 0L)
        if (lockedUntil <= 0L) return 0L
        val lastFailureAt = store.getLong(KEY_LAST_FAILURE_AT, 0L)
        // A clock moved backwards must not shorten the lockout.
        val now = maxOf(clock(), lastFailureAt)
        return (lockedUntil - now).coerceAtLeast(0L)
    }

    internal fun verifyPin(pin: String): Boolean {
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

        if (lockoutRemainingMillis() > 0) {
            return RecoverPinResult.LOCKED_OUT
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
            registerFailedAttempt()
            return if (lockoutRemainingMillis() > 0) RecoverPinResult.LOCKED_OUT else RecoverPinResult.INVALID_SECURITY_ANSWER
        }

        val pinHash = hashSecret(newPin)
        store.edit {
            putString(KEY_PIN_HASH, pinHash.hash)
            putString(KEY_PIN_SALT, pinHash.salt)
            putInt(KEY_PIN_ITERATIONS, pinHash.iterations)
            remove(KEY_LEGACY_PIN)
            removeLockoutState()
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

    private fun registerFailedAttempt(): Int {
        val failures = store.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val now = clock()
        store.edit {
            putInt(KEY_FAILED_ATTEMPTS, failures)
            putLong(KEY_LAST_FAILURE_AT, now)
            if (failures >= MAX_FREE_ATTEMPTS) {
                putLong(KEY_LOCKED_UNTIL, now + lockoutDurationMillis(failures))
            }
        }
        return failures
    }

    private fun resetFailedAttempts() {
        store.edit { removeLockoutState() }
    }

    private fun SecurityProfileStoreEditor.removeLockoutState() {
        remove(KEY_FAILED_ATTEMPTS)
        remove(KEY_LAST_FAILURE_AT)
        remove(KEY_LOCKED_UNTIL)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun hashSecret(secret: String): HashedSecret {
        val saltBytes = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val hashBytes = derivePbkdf2(secret, saltBytes, DEFAULT_ITERATIONS)
        return HashedSecret(
            hash = Base64.Default.encode(hashBytes),
            salt = Base64.Default.encode(saltBytes),
            iterations = DEFAULT_ITERATIONS
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun verifyHashedSecret(secret: String, hash: String?, salt: String?, iterations: Int): Boolean {
        if (hash.isNullOrBlank() || salt.isNullOrBlank()) {
            return false
        }
        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
            return false
        }

        val expectedHash = runCatching { Base64.Default.decode(hash) }.getOrNull() ?: return false
        val saltBytes = runCatching { Base64.Default.decode(salt) }.getOrNull() ?: return false
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
        private const val MAX_DOUBLINGS = 5

        const val MAX_FREE_ATTEMPTS = 5
        const val BASE_LOCKOUT_MILLIS = 30_000L
        const val MAX_LOCKOUT_MILLIS = 15 * 60_000L
        const val KEY_FAILED_ATTEMPTS = "key_failed_attempts"
        const val KEY_LAST_FAILURE_AT = "key_last_failure_at"
        const val KEY_LOCKED_UNTIL = "key_locked_until"

        /** 5th failure locks for 30s; each further failure doubles it, capped at 15 minutes. */
        fun lockoutDurationMillis(failures: Int): Long {
            if (failures < MAX_FREE_ATTEMPTS) return 0L
            val doublings = (failures - MAX_FREE_ATTEMPTS).coerceAtMost(MAX_DOUBLINGS)
            return (BASE_LOCKOUT_MILLIS shl doublings).coerceAtMost(MAX_LOCKOUT_MILLIS)
        }

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
