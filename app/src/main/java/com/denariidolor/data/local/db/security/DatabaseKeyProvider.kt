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

package com.denariidolor.data.local.db.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

class DatabaseKey(val passphrase: ByteArray, val newlyCreated: Boolean)

@Singleton
class DatabaseKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val preferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Synchronized
    fun getOrCreate(): DatabaseKey {
        preferences.getString(KEY_DB_KEY_HEX, null)
            ?.takeIf { DatabaseKeys.isValidHexKey(it) }
            ?.let { return DatabaseKey(DatabaseKeys.toRawKeyPassphrase(it), newlyCreated = false) }

        val hexKey = DatabaseKeys.generateHexKey(SecureRandom())
        check(preferences.edit().putString(KEY_DB_KEY_HEX, hexKey).commit()) { "Unable to persist database key" }
        return DatabaseKey(DatabaseKeys.toRawKeyPassphrase(hexKey), newlyCreated = true)
    }

    private companion object {
        const val PREFS_NAME = "db_key_prefs"
        const val KEY_DB_KEY_HEX = "db_key_hex"
    }
}
