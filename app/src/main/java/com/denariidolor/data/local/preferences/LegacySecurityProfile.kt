/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import java.security.MessageDigest
import java.util.Base64

/**
 * The v1.0.x profile, which kept PBKDF2 hashes of the PIN and the security answer (and the database key separately).
 * It is only read to verify the user once, so the install can be upgraded to [SecurityProfileService]; then it is deleted.
 */
class LegacySecurityProfile(private val store: SecurityProfileStore) {
    fun isConfigured(): Boolean = store.getBoolean(KEY_PROFILE_CONFIGURED, false) &&
        !store.getString(KEY_SECURITY_QUESTION).isNullOrBlank() &&
        hasHash(KEY_PIN_HASH, KEY_PIN_SALT, KEY_PIN_ITERATIONS) &&
        hasHash(KEY_SECURITY_ANSWER_HASH, KEY_SECURITY_ANSWER_SALT, KEY_SECURITY_ANSWER_ITERATIONS)

    fun securityQuestion(): String? = store.getString(KEY_SECURITY_QUESTION)

    fun verifyPin(pin: String): Boolean = verify(pin, KEY_PIN_HASH, KEY_PIN_SALT, KEY_PIN_ITERATIONS)

    /** v1 compared answers after trimming only, so they are case-sensitive here. */
    fun verifyAnswer(answer: String): Boolean = answer.isNotBlank() &&
        verify(answer.trim(), KEY_SECURITY_ANSWER_HASH, KEY_SECURITY_ANSWER_SALT, KEY_SECURITY_ANSWER_ITERATIONS)

    private fun hasHash(hashKey: String, saltKey: String, iterationsKey: String): Boolean = !store.getString(hashKey).isNullOrBlank() &&
        !store.getString(saltKey).isNullOrBlank() &&
        store.getInt(iterationsKey, 0) in SecurityProfileService.MIN_ITERATIONS..MAX_ITERATIONS

    private fun verify(secret: String, hashKey: String, saltKey: String, iterationsKey: String): Boolean {
        val expected = store.getString(hashKey)?.let(::decode)
        val salt = store.getString(saltKey)?.let(::decode)
        val iterations = store.getInt(iterationsKey, 0)
        if (expected == null || salt == null || iterations !in SecurityProfileService.MIN_ITERATIONS..MAX_ITERATIONS) return false
        return MessageDigest.isEqual(expected, pbkdf2(secret, salt, iterations))
    }

    private fun decode(value: String): ByteArray? = runCatching { Base64.getDecoder().decode(value) }.getOrNull()

    companion object {
        private const val MAX_ITERATIONS = 1_000_000

        const val KEY_PROFILE_CONFIGURED = "key_profile_configured"
        const val KEY_PIN_HASH = "key_pin_hash"
        const val KEY_PIN_SALT = "key_pin_salt"
        const val KEY_PIN_ITERATIONS = "key_pin_iterations"
        const val KEY_SECURITY_QUESTION = "key_security_question"
        const val KEY_SECURITY_ANSWER_HASH = "key_security_answer_hash"
        const val KEY_SECURITY_ANSWER_SALT = "key_security_answer_salt"
        const val KEY_SECURITY_ANSWER_ITERATIONS = "key_security_answer_iterations"
    }
}
