/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class InMemorySecurityProfileStore : SecurityProfileStore {
    val values = mutableMapOf<String, Any>()

    override fun getString(key: String): String? = values[key] as String?

    override fun getInt(key: String, defaultValue: Int): Int = values[key] as Int? ?: defaultValue

    override fun getLong(key: String, defaultValue: Long): Long = values[key] as Long? ?: defaultValue

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key] as Boolean? ?: defaultValue

    @Synchronized
    override fun edit(block: SecurityProfileStoreEditor.() -> Unit) {
        val editor = object : SecurityProfileStoreEditor {
            override fun putString(key: String, value: String) {
                values[key] = value
            }

            override fun putInt(key: String, value: Int) {
                values[key] = value
            }

            override fun putLong(key: String, value: Long) {
                values[key] = value
            }

            override fun putBoolean(key: String, value: Boolean) {
                values[key] = value
            }

            override fun remove(key: String) {
                values.remove(key)
            }

            override fun clear() {
                values.clear()
            }
        }
        block(editor)
    }
}

/** Elapsed time and boot count under test control; nothing here follows the wall clock. */
internal class FakeMonotonicClock(var elapsed: Long = 1_000_000L, var boot: Int? = 7) : MonotonicClock {
    override fun elapsedRealtimeMillis(): Long = elapsed

    override fun bootCount(): Int? = boot
}

/** A software HMAC standing in for the Keystore device key; [available] = false simulates a lost key. */
internal class FakeDeviceKeyMixer(private val key: ByteArray = ByteArray(32) { 1 }) : DeviceKeyMixer {
    var available = true

    override fun prepare() = Unit

    override fun mix(input: ByteArray): ByteArray {
        if (!available) throw SecureStorageException("Device key is missing")
        return Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(key, "HmacSHA256"))
            doFinal(input)
        }
    }
}
