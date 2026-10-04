/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.vault

import android.content.Context
import com.denariidolor.data.local.preferences.SecureStorageException
import com.denariidolor.data.local.preferences.SecurityProfileStore
import com.denariidolor.data.local.preferences.SecurityProfileStoreEditor
import java.util.Base64
import org.json.JSONException
import org.json.JSONObject

/**
 * [SecurityProfileStore] kept as a single AES-256-GCM blob in app-private SharedPreferences, encrypted with a
 * non-exportable Android Keystore key. Replaces the deprecated `EncryptedSharedPreferences` (CS-09).
 */
class KeystoreProfileStore(context: Context, private val prefsName: String, private val secrets: KeystoreSecrets) :
    SecurityProfileStore {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private var cache: JSONObject? = null

    override fun getString(key: String): String? = values().let { if (it.has(key)) it.getString(key) else null }

    override fun getInt(key: String, defaultValue: Int): Int = values().optInt(key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Long = values().optLong(key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = values().optBoolean(key, defaultValue)

    @Synchronized
    override fun edit(block: SecurityProfileStoreEditor.() -> Unit) {
        val editor = JsonEditor(JSONObject(values().toString()))
        editor.block()
        val blob = secrets.encrypt(editor.values.toString().toByteArray(Charsets.UTF_8), ASSOCIATED_DATA)
        val saved = preferences.edit().putString(KEY_BLOB, Base64.getEncoder().encodeToString(blob)).commit()
        if (!saved) throw SecureStorageException("Unable to save the security profile")
        cache = editor.values
    }

    /** Deletes the stored profile; the caller deletes the Keystore keys. */
    @Synchronized
    fun delete() {
        cache = null
        preferences.edit().clear().commit()
        appContext.deleteSharedPreferences(prefsName)
    }

    @Synchronized
    private fun values(): JSONObject = cache ?: load().also { cache = it }

    private fun load(): JSONObject {
        val blob = preferences.getString(KEY_BLOB, null) ?: return JSONObject()
        val encrypted = try {
            Base64.getDecoder().decode(blob)
        } catch (e: IllegalArgumentException) {
            throw SecureStorageException("The security profile is unreadable", e)
        }
        val plaintext = secrets.decrypt(encrypted, ASSOCIATED_DATA)
        return try {
            JSONObject(String(plaintext, Charsets.UTF_8))
        } catch (e: JSONException) {
            throw SecureStorageException("The security profile is unreadable", e)
        } finally {
            plaintext.fill(0)
        }
    }

    private class JsonEditor(var values: JSONObject) : SecurityProfileStoreEditor {
        override fun putString(key: String, value: String) {
            values.put(key, value)
        }

        override fun putInt(key: String, value: Int) {
            values.put(key, value)
        }

        override fun putLong(key: String, value: Long) {
            values.put(key, value)
        }

        override fun putBoolean(key: String, value: Boolean) {
            values.put(key, value)
        }

        override fun remove(key: String) {
            values.remove(key)
        }

        override fun clear() {
            values = JSONObject()
        }
    }

    private companion object {
        const val KEY_BLOB = "profile"
        val ASSOCIATED_DATA = "denarii-dolor:profile:v2".toByteArray(Charsets.UTF_8)
    }
}
