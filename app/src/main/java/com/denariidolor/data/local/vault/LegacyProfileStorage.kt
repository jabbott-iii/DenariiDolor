/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.vault

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.denariidolor.data.local.db.security.DatabaseKeys
import com.denariidolor.data.local.preferences.LegacySecurityProfile
import com.denariidolor.data.local.preferences.SecureStorageException
import com.denariidolor.data.local.preferences.SecurityProfileStore
import com.denariidolor.data.local.preferences.SecurityProfileStoreEditor
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Read-only access to the v1.0.x `EncryptedSharedPreferences` files, kept only so existing installs can be upgraded
 * at their next sign-in. This is the app's last use of the deprecated `androidx.security:security-crypto` (CS-09).
 */
class LegacyProfileStorage(context: Context, private val config: VaultConfig, private val secrets: KeystoreSecrets) {
    private val appContext = context.applicationContext

    fun hasFiles(): Boolean = listOf(config.legacySecurePrefsName, config.legacyDatabaseKeyPrefsName).any { prefsFile(it).exists() }

    /** The v1 profile, or null when there is none to upgrade. */
    fun profile(): LegacySecurityProfile? = openIfPresent(config.legacySecurePrefsName)
        ?.let { LegacySecurityProfile(ReadOnlyStore(it)) }
        ?.takeIf { legacyCall { it.isConfigured() } }

    /** The v1 database key, or null when it is missing or malformed. */
    fun databaseKey(): ByteArray? = openIfPresent(config.legacyDatabaseKeyPrefsName)
        ?.let { legacyCall { it.getString(KEY_DATABASE_KEY_HEX, null) } }
        ?.takeIf(DatabaseKeys::isValidHexKey)
        ?.let(DatabaseKeys::fromHex)

    /** Deletes both v1 files and their master key, so leftover copies can no longer be decrypted. */
    fun delete() {
        appContext.deleteSharedPreferences(config.legacySecurePrefsName)
        appContext.deleteSharedPreferences(config.legacyDatabaseKeyPrefsName)
        secrets.deleteAlias(config.legacyMasterKeyAlias)
    }

    private fun prefsFile(name: String) = File(appContext.dataDir, "shared_prefs/$name.xml")

    // Opening a missing file would create it, so only existing v1 files are opened.
    private fun openIfPresent(name: String): SharedPreferences? = if (!prefsFile(name).exists()) {
        null
    } else {
        legacyCall {
            val masterKey = MasterKey.Builder(appContext, config.legacyMasterKeyAlias)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                name,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    private class ReadOnlyStore(private val preferences: SharedPreferences) : SecurityProfileStore {
        override fun getString(key: String): String? = legacyCall { preferences.getString(key, null) }

        override fun getInt(key: String, defaultValue: Int): Int = legacyCall { preferences.getInt(key, defaultValue) }

        override fun getLong(key: String, defaultValue: Long): Long = legacyCall { preferences.getLong(key, defaultValue) }

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = legacyCall { preferences.getBoolean(key, defaultValue) }

        override fun edit(block: SecurityProfileStoreEditor.() -> Unit) = throw UnsupportedOperationException("The v1 profile is read-only")
    }

    private companion object {
        const val KEY_DATABASE_KEY_HEX = "db_key_hex"

        // EncryptedSharedPreferences reports unreadable values as java.lang.SecurityException.
        inline fun <T> legacyCall(block: () -> T): T {
            val failure = try {
                return block()
            } catch (e: GeneralSecurityException) {
                e
            } catch (e: IOException) {
                e
            } catch (e: SecurityException) {
                e
            } catch (e: IllegalStateException) {
                e
            }
            throw SecureStorageException("The v1 security profile is unreadable", failure)
        }
    }
}
