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
