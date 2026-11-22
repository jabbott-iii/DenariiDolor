/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

/**
 * The cryptography the security profile (CS-15) and backups (CS-28) are built on, so their code stays plain Kotlin and each
 * platform brings its own implementation. Profiles and backups already on phones depend on these exact bytes: an implementation
 * must pass `CryptoKnownAnswerTest`, and on Android `CryptoPrimitivesDeviceTest` as well. Callers own every array they get back
 * and zero the secret ones.
 */
interface CryptoPrimitives {
    /** [count] bytes from a cryptographically secure random source. */
    fun randomBytes(count: Int): ByteArray

    /** PBKDF2-HMAC-SHA256 over the UTF-8 bytes of [secret], 32 bytes long. */
    fun pbkdf2HmacSha256(secret: String, salt: ByteArray, iterations: Int): ByteArray

    /** AES-256-GCM with a 32-byte [key], a 12-byte [nonce] and a 128-bit tag: the ciphertext followed by the tag. */
    fun aesGcmSeal(key: ByteArray, nonce: ByteArray, associatedData: ByteArray, plaintext: ByteArray): ByteArray

    /**
     * The plaintext [aesGcmSeal] sealed, or null when the tag doesn't verify: a wrong key, or a changed nonce, associated data or
     * ciphertext. Any other failure throws.
     */
    fun aesGcmOpen(key: ByteArray, nonce: ByteArray, associatedData: ByteArray, sealed: ByteArray): ByteArray?

    /** Unicode Normalization Form C, in which backup passphrases are compared. */
    fun normalizeNfc(text: String): String

    /** Unicode Normalization Form KC, in which security answers are compared. */
    fun normalizeNfkc(text: String): String
}
