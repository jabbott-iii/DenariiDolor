/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.db

import com.denariidolor.data.local.db.security.DatabaseKeys
import java.security.SecureRandom
import org.junit.Assert.assertArrayEquals
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
    fun rawKeysRoundTripThroughHex() {
        val key = ByteArray(32) { (it * 9 - 128).toByte() }
        val hex = DatabaseKeys.toHex(key)

        assertTrue(DatabaseKeys.isValidHexKey(hex))
        assertArrayEquals(key, DatabaseKeys.fromHex(hex))
    }
}
