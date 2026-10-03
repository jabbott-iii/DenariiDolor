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

import java.io.File
import java.security.SecureRandom

object DatabaseKeys {
    private const val KEY_BYTES = 32
    private const val HEX_RADIX = 16
    private val HEX_KEY_REGEX = Regex("^[0-9a-f]{${KEY_BYTES * 2}}$")
    private val SQLITE_PLAINTEXT_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    fun generateHexKey(random: SecureRandom): String {
        val bytes = ByteArray(KEY_BYTES).also(random::nextBytes)
        return try {
            bytes.joinToString("") { "%02x".format(it) }
        } finally {
            bytes.fill(0)
        }
    }

    fun isValidHexKey(value: String): Boolean = HEX_KEY_REGEX.matches(value)

    fun toHex(key: ByteArray): String = key.joinToString("") { "%02x".format(it) }

    fun fromHex(hexKey: String): ByteArray = ByteArray(hexKey.length / 2) { index ->
        hexKey.substring(index * 2, index * 2 + 2).toInt(HEX_RADIX).toByte()
    }

    /** SQLCipher raw-key syntax: skips PBKDF2 because the key is already 256 bits of randomness. */
    fun toRawKeyPassphrase(hexKey: String): ByteArray = "x'$hexKey'".toByteArray(Charsets.US_ASCII)

    fun hasPlaintextHeader(header: ByteArray): Boolean = header.size >= SQLITE_PLAINTEXT_HEADER.size &&
        header.copyOf(SQLITE_PLAINTEXT_HEADER.size).contentEquals(SQLITE_PLAINTEXT_HEADER)

    fun isPlaintextDatabase(file: File): Boolean {
        if (!file.isFile || file.length() < SQLITE_PLAINTEXT_HEADER.size) return false
        val header = ByteArray(SQLITE_PLAINTEXT_HEADER.size)
        val read = file.inputStream().use { it.read(header) }
        return read == header.size && hasPlaintextHeader(header)
    }
}
