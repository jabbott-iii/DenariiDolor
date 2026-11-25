/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The vault's Android Keystore operations, which Rust calls through JNI (`rust/crates/keystore/src/jni.rs`, ADR-0022);
 * nothing in Kotlin or Dart calls them, so R8 keeps them by name (`proguard-rules.pro`). The keys never leave the
 * Keystore (docs/specs/android-v1-formats.md, sections 2 and 3): a storage key that seals the profile, and a device key
 * that every PIN and answer guess passes through. v1's `KeystoreSecrets` and `KeystoreProfileStore` did the same, with
 * the same aliases and parameters.
 *
 * Each method takes the alias prefix, `denarii_vault_` for the vault and one that starts with `denarii_test_` for the
 * integration test, so a test never touches a user's keys. Every failure the caller should handle is thrown as a
 * [KeystoreException]. Each method blocks its caller, a Rust worker thread, never Android's main thread; creating a key,
 * saving the profile, and deleting run one caller at a time.
 */
object VaultKeystore {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val AES_KEY_BITS = 256
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val GCM_TAG_BYTES = GCM_TAG_BITS / Byte.SIZE_BITS
    private const val PROFILE_ENTRY = "profile"
    private val PROFILE_ASSOCIATED_DATA = "denarii-dolor:profile:v2".toByteArray(Charsets.UTF_8)

    @Volatile
    private var appContext: Context? = null

    /** Called by [MainActivity] before Flutter starts Dart, so the profile methods can open preferences files. */
    fun attach(context: Context) {
        appContext = context.applicationContext
    }

    /** Creates the device key, an HMAC-SHA256 key, unless it exists. v1 did this before it wrote a new profile. */
    @JvmStatic
    @Synchronized
    fun ensureDeviceKey(aliasPrefix: String): Unit = keystoreCall {
        val alias = deviceAlias(aliasPrefix)
        if (secretKey(alias) == null) generateHmacKey(alias)
    }

    /**
     * HMAC-SHA256 of [message] under the device key. It never creates the key.
     *
     * @throws KeystoreException with [KeystoreFailure.KEY_MISSING] when the device key doesn't exist, or
     *   [KeystoreFailure.FAILED].
     */
    @JvmStatic
    fun deviceHmac(aliasPrefix: String, message: ByteArray): ByteArray = keystoreCall {
        val key = secretKey(deviceAlias(aliasPrefix)) ?: throw KeystoreException(KeystoreFailure.KEY_MISSING)
        Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256).run {
            init(key)
            doFinal(message)
        }
    }

    /**
     * The profile opened from the `profile` entry of the preferences file [prefsName], or null when the file has no
     * entry. The entry is Base64 of the IV, the ciphertext, and the tag, sealed with AES-256-GCM under the storage key.
     *
     * @throws KeystoreException with [KeystoreFailure.KEY_MISSING] when there's an entry but no storage key,
     *   [KeystoreFailure.UNREADABLE] when the entry isn't Base64, is too short, or fails AES-GCM's check, or
     *   [KeystoreFailure.FAILED].
     */
    @JvmStatic
    fun readProfile(aliasPrefix: String, prefsName: String): ByteArray? = keystoreCall {
        val entry = preferences(prefsName).getString(PROFILE_ENTRY, null) ?: return null
        val blob = decodeBlob(entry)
        val key = secretKey(storageAlias(aliasPrefix)) ?: throw KeystoreException(KeystoreFailure.KEY_MISSING)
        try {
            Cipher.getInstance(AES_GCM).run {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, blob, 0, GCM_IV_BYTES))
                updateAAD(PROFILE_ASSOCIATED_DATA)
                doFinal(blob, GCM_IV_BYTES, blob.size - GCM_IV_BYTES)
            }
        } catch (e: AEADBadTagException) {
            throw KeystoreException(KeystoreFailure.UNREADABLE, e)
        }
    }

    /**
     * Seals [plaintext] under the storage key with a new IV, and saves it as the `profile` entry of [prefsName] with a
     * synchronous `commit()`. It creates the storage key only when the file has no entry: a stored profile without its
     * key can't be opened, and a new key would hide that (never re-create a key while a profile exists).
     *
     * @throws KeystoreException with [KeystoreFailure.KEY_MISSING] when there's an entry but no storage key, which
     *   leaves the entry as it was, or [KeystoreFailure.FAILED] when the Keystore fails or the file isn't saved.
     */
    @JvmStatic
    @Synchronized
    fun writeProfile(aliasPrefix: String, prefsName: String, plaintext: ByteArray): Unit = keystoreCall {
        val preferences = preferences(prefsName)
        val alias = storageAlias(aliasPrefix)
        val key = secretKey(alias)
            ?: if (preferences.contains(PROFILE_ENTRY)) {
                throw KeystoreException(KeystoreFailure.KEY_MISSING)
            } else {
                generateAesKey(alias)
            }
        val cipher = Cipher.getInstance(AES_GCM).apply {
            init(Cipher.ENCRYPT_MODE, key)
            updateAAD(PROFILE_ASSOCIATED_DATA)
        }
        val iv = cipher.iv
        check(iv.size == GCM_IV_BYTES) { "The Keystore chose a ${iv.size}-byte IV" }
        val blob = iv + cipher.doFinal(plaintext)
        val saved = preferences.edit().putString(PROFILE_ENTRY, Base64.getEncoder().encodeToString(blob)).commit()
        if (!saved) throw KeystoreException(KeystoreFailure.FAILED)
    }

    /**
     * Empties the preferences file [prefsName] and deletes it, as v1 did. Emptying it first matters, because Android keeps
     * the file's contents in memory after it's deleted. A file that doesn't exist counts as deleted.
     *
     * @throws KeystoreException with [KeystoreFailure.FAILED] when the file can't be emptied or deleted.
     */
    @JvmStatic
    @Synchronized
    fun deleteProfile(prefsName: String): Unit = keystoreCall {
        val cleared = preferences(prefsName).edit().clear().commit()
        val deleted = context().deleteSharedPreferences(prefsName)
        if (!cleared || !deleted) throw KeystoreException(KeystoreFailure.FAILED)
    }

    /**
     * Deletes the storage, device, and biometric keys. It tries all three even when one fails, because wipe deletes every
     * key (CS-17). A key that doesn't exist counts as deleted.
     *
     * @throws KeystoreException with [KeystoreFailure.FAILED] when any of them can't be deleted.
     */
    @JvmStatic
    @Synchronized
    fun deleteKeys(aliasPrefix: String): Unit = keystoreCall {
        val keyStore = keyStore()
        val aliases = listOf(storageAlias(aliasPrefix), deviceAlias(aliasPrefix), biometricAlias(aliasPrefix))
        val failures = aliases.count { alias -> !deleteEntry(keyStore, alias) }
        if (failures > 0) throw KeystoreException(KeystoreFailure.FAILED)
    }

    private fun storageAlias(aliasPrefix: String) = "${aliasPrefix}storage"

    private fun deviceAlias(aliasPrefix: String) = "${aliasPrefix}device"

    // M3.6 creates this key; deleteKeys deletes it already, as v1's wipe did.
    private fun biometricAlias(aliasPrefix: String) = "${aliasPrefix}biometric"

    private fun context(): Context = checkNotNull(appContext) { "MainActivity hasn't attached a context" }

    private fun preferences(prefsName: String): SharedPreferences = context().getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private fun decodeBlob(entry: String): ByteArray {
        val blob = try {
            Base64.getDecoder().decode(entry)
        } catch (e: IllegalArgumentException) {
            throw KeystoreException(KeystoreFailure.UNREADABLE, e)
        }
        if (blob.size < GCM_IV_BYTES + GCM_TAG_BYTES) throw KeystoreException(KeystoreFailure.UNREADABLE)
        return blob
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun secretKey(alias: String): SecretKey? = keyStore().getKey(alias, null) as SecretKey?

    /** Whether [alias] is gone afterward. */
    private fun deleteEntry(keyStore: KeyStore, alias: String): Boolean = try {
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
        true
    } catch (_: GeneralSecurityException) {
        false
    }

    // Called only with the object's lock held, from ensureDeviceKey and writeProfile, so two callers can't both create
    // the key, and the second overwrite the first.
    private fun generateAesKey(alias: String): SecretKey {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_BITS)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(spec)
            generateKey()
        }
    }

    private fun generateHmacKey(alias: String): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE).run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN).build())
            generateKey()
        }

    /** Runs [block], and throws [KeystoreFailure.FAILED] for every Keystore and storage failure, as v1 did. */
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
        throw KeystoreException(KeystoreFailure.FAILED, failure)
    }
}

/**
 * A failed [VaultKeystore] operation. Rust reads [code] through JNI and maps it to its `KeystoreError`; R8 keeps the
 * class's name and the field's (`proguard-rules.pro`). The message names the failure only, never a secret.
 */
class KeystoreException(failure: KeystoreFailure, cause: Throwable? = null) : Exception(failure.name, cause) {
    @JvmField
    val code: Int = failure.code
}

/** Why a [VaultKeystore] operation failed, as the code Rust maps (`rust/crates/keystore/src/jni.rs`). Keep both lists the same. */
enum class KeystoreFailure(val code: Int) {
    /** The key the operation needs doesn't exist. */
    KEY_MISSING(1),

    /** The stored profile isn't Base64, is too short to hold an IV and a tag, or fails AES-GCM's check. */
    UNREADABLE(2),

    /** The Keystore failed, or the preferences file couldn't be saved or deleted. */
    FAILED(3),
}
