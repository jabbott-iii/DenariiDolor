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

package com.denariidolor.data.local.vault

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.denariidolor.data.local.preferences.SecureStorageException
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The vault's Android Keystore keys, none of which can be exported from the device:
 * a storage key for the encrypted profile, a device key that every PIN and answer guess must pass through, and an
 * optional biometric-bound key. Every Keystore failure surfaces as [SecureStorageException].
 */
class KeystoreSecrets(aliasPrefix: String) {
    private val storageAlias = "${aliasPrefix}storage"
    private val deviceAlias = "${aliasPrefix}device"
    private val biometricAlias = "${aliasPrefix}biometric"

    /** AES-256-GCM under the storage key, created on first use. Returns the IV followed by the ciphertext. */
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray = keystoreCall {
        val key = secretKey(storageAlias) ?: generateAesKey(storageAlias, biometricBound = false)
        val cipher = Cipher.getInstance(AES_GCM).apply {
            init(Cipher.ENCRYPT_MODE, key)
            updateAAD(associatedData)
        }
        cipher.iv + cipher.doFinal(plaintext)
    }

    fun decrypt(blob: ByteArray, associatedData: ByteArray): ByteArray = keystoreCall {
        val key = secretKey(storageAlias) ?: throw SecureStorageException("The storage key is missing")
        Cipher.getInstance(AES_GCM).run {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, blob, 0, GCM_IV_BYTES))
            updateAAD(associatedData)
            doFinal(blob, GCM_IV_BYTES, blob.size - GCM_IV_BYTES)
        }
    }

    fun ensureDeviceKey() = keystoreCall {
        if (secretKey(deviceAlias) == null) generateHmacKey(deviceAlias)
    }

    fun hmacWithDeviceKey(input: ByteArray): ByteArray = keystoreCall {
        val key = secretKey(deviceAlias) ?: throw SecureStorageException("The device key is missing")
        Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256).run {
            init(key)
            doFinal(input)
        }
    }

    /** A cipher that encrypts with a new biometric-bound key once `BiometricPrompt` authorizes it. */
    fun biometricEncryptCipher(): Cipher = keystoreCall {
        deleteAlias(biometricAlias)
        val key = generateAesKey(biometricAlias, biometricBound = true)
        Cipher.getInstance(AES_GCM).apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    /** A cipher for `BiometricPrompt` to authorize, or null when the key is gone or the enrolled biometrics changed. */
    fun biometricDecryptCipher(iv: ByteArray): Cipher? = keystoreCall {
        val key = secretKey(biometricAlias)
        try {
            key?.let { Cipher.getInstance(AES_GCM).apply { init(Cipher.DECRYPT_MODE, it, GCMParameterSpec(GCM_TAG_BITS, iv)) } }
        } catch (_: KeyPermanentlyInvalidatedException) {
            null
        }
    }

    fun deleteBiometricKey() = keystoreCall { deleteAlias(biometricAlias) }

    /** Deletes every vault key, which makes any surviving copy of the wrapped database key useless (CS-17). */
    fun deleteAll() = keystoreCall { listOf(storageAlias, deviceAlias, biometricAlias).forEach(::deleteAlias) }

    fun deleteAlias(alias: String) = keystoreCall {
        val keyStore = keyStore()
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun secretKey(alias: String): SecretKey? = keyStore().getKey(alias, null) as SecretKey?

    private fun generateAesKey(alias: String, biometricBound: Boolean): SecretKey {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_BITS)
        if (biometricBound) {
            spec.setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION") // Before API 30, -1 is the only way to require a biometric for every use.
                spec.setUserAuthenticationValidityDurationSeconds(-1)
            }
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(spec.build())
            generateKey()
        }
    }

    private fun generateHmacKey(alias: String): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE).run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN).build())
            generateKey()
        }

    private inline fun <T> keystoreCall(block: () -> T): T {
        val failure = try {
            return block()
        } catch (e: GeneralSecurityException) {
            e
        } catch (e: ProviderException) {
            e
        } catch (e: IOException) {
            e
        } catch (e: IllegalStateException) {
            e
        }
        throw SecureStorageException("Android Keystore operation failed", failure)
    }

    companion object {
        const val GCM_IV_BYTES = 12
        private const val GCM_TAG_BITS = 128
        private const val AES_KEY_BITS = 256
        private const val AES_GCM = "AES/GCM/NoPadding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }
}
