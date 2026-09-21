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

package com.denariidolor.data.local.db

import com.denariidolor.data.local.db.security.DatabaseKeys
import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DatabaseKeysTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun generatedKeysAre256BitHexAndUnique() {
        val random = SecureRandom()
        val first = DatabaseKeys.generateHexKey(random)
        val second = DatabaseKeys.generateHexKey(random)

        assertEquals(64, first.length)
        assertTrue(DatabaseKeys.isValidHexKey(first))
        assertNotEquals(first, second)
    }

    @Test
    fun invalidStoredKeysAreRejected() {
        assertFalse(DatabaseKeys.isValidHexKey(""))
        assertFalse(DatabaseKeys.isValidHexKey("zz".repeat(32)))
        assertFalse(DatabaseKeys.isValidHexKey("ab".repeat(31)))
    }

    @Test
    fun passphraseUsesSqlCipherRawKeySyntax() {
        val hex = "ab".repeat(32)

        assertEquals("x'$hex'", String(DatabaseKeys.toRawKeyPassphrase(hex), Charsets.US_ASCII))
    }

    @Test
    fun detectsPlaintextSqliteHeader() {
        val plaintext = temp.newFile("plain.db").apply { writeBytes("SQLite format 3\u0000".toByteArray() + ByteArray(100)) }
        val encrypted = temp.newFile("enc.db").apply { writeBytes(ByteArray(116) { (it * 7).toByte() }) }
        val tiny = temp.newFile("tiny.db").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        assertTrue(DatabaseKeys.isPlaintextDatabase(plaintext))
        assertFalse(DatabaseKeys.isPlaintextDatabase(encrypted))
        assertFalse(DatabaseKeys.isPlaintextDatabase(tiny))
    }

    @Test
    fun discardsPlaintextOrOrphanedDatabasesOnly() {
        val plaintext = temp.newFile("plain.db").apply { writeBytes("SQLite format 3\u0000".toByteArray() + ByteArray(100)) }
        val encrypted = temp.newFile("enc.db").apply { writeBytes(ByteArray(116) { 1 }) }
        val missing = temp.root.resolve("missing.db")

        assertTrue(DatabaseKeys.shouldDiscard(plaintext, keyNewlyCreated = false))
        assertTrue(DatabaseKeys.shouldDiscard(encrypted, keyNewlyCreated = true))
        assertFalse(DatabaseKeys.shouldDiscard(encrypted, keyNewlyCreated = false))
        assertFalse(DatabaseKeys.shouldDiscard(missing, keyNewlyCreated = true))
    }
}
