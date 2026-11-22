/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** [CryptoPrimitives] on the platform's JCA providers and java.text: what the profile and backups have always used on Android. */
class JcaCryptoPrimitives(private val random: SecureRandom = SecureRandom()) : CryptoPrimitives {
    override fun randomBytes(count: Int): ByteArray = ByteArray(count).also(random::nextBytes)

    override fun pbkdf2HmacSha256(secret: String, salt: ByteArray, iterations: Int): ByteArray = pbkdf2(secret, salt, iterations)

    override fun aesGcmSeal(key: ByteArray, nonce: ByteArray, associatedData: ByteArray, plaintext: ByteArray): ByteArray =
        gcm(Cipher.ENCRYPT_MODE, key, nonce, associatedData).doFinal(plaintext)

    override fun aesGcmOpen(key: ByteArray, nonce: ByteArray, associatedData: ByteArray, sealed: ByteArray): ByteArray? = try {
        gcm(Cipher.DECRYPT_MODE, key, nonce, associatedData).doFinal(sealed)
    } catch (_: AEADBadTagException) {
        null
    }

    override fun normalizeNfc(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFC)

    override fun normalizeNfkc(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)

    // The sizes are checked because a 16-byte key would quietly select AES-128, and the format promises AES-256.
    private fun gcm(mode: Int, key: ByteArray, nonce: ByteArray, associatedData: ByteArray): Cipher {
        require(key.size == KEY_BYTES) { "AES-256-GCM needs a $KEY_BYTES-byte key" }
        require(nonce.size == NONCE_BYTES) { "AES-256-GCM needs a $NONCE_BYTES-byte nonce" }
        return Cipher.getInstance(AES_GCM).apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(associatedData)
        }
    }

    private companion object {
        const val AES_GCM = "AES/GCM/NoPadding"
        const val KEY_BYTES = 32
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
    }
}
