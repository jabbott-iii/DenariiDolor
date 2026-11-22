/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Known answers for [CryptoPrimitives], computed outside the app with Python's hashlib and OpenSSL, which also reproduce the
 * RFC 7914 PBKDF2 vectors and test case 14 of the GCM specification. Every implementation must give these exact bytes, or the
 * profiles and backups already on phones won't open. CryptoPrimitivesDeviceTest runs the same vectors on Android's providers.
 */
class CryptoKnownAnswerTest {
    private val crypto: CryptoPrimitives = JcaCryptoPrimitives()

    @Test
    fun pbkdf2GivesItsKnownAnswers() {
        PBKDF2.forEach { vector ->
            val derived = crypto.pbkdf2HmacSha256(vector.secret, bytes(vector.salt), vector.iterations)
            assertEquals("'${vector.secret}' ${vector.iterations}", vector.expected, hex(derived))
        }
    }

    @Test
    fun aesGcmSealsToItsKnownAnswers() {
        GCM.forEach { vector -> assertEquals(vector.sealed, hex(seal(vector))) }
    }

    @Test
    fun aesGcmOpensWhatItSealed() {
        GCM.forEach { vector -> assertEquals(vector.plaintext, open(vector)?.let(::hex)) }
    }

    /** A wrong key and a changed nonce, associated data, ciphertext or tag all fail the same way: null, never a plaintext. */
    @Test
    fun aesGcmOpensNothingThatChanged() {
        val vector = GCM.first()
        val parts = listOf(bytes(vector.key), bytes(vector.nonce), bytes(vector.associatedData), bytes(vector.sealed))
        parts.indices.forEach { part ->
            listOf(0, parts[part].size - 1).forEach { index ->
                val changed = parts.mapIndexed { i, bytes -> if (i == part) flip(bytes, index) else bytes }
                assertNull("part $part byte $index", crypto.aesGcmOpen(changed[0], changed[1], changed[2], changed[3]))
            }
        }
    }

    @Test
    fun aesGcmRefusesOtherKeyAndNonceSizes() {
        val vector = GCM.first()
        assertThrows(IllegalArgumentException::class.java) { seal(vector.copy(key = vector.key.take(32))) }
        assertThrows(IllegalArgumentException::class.java) { seal(vector.copy(nonce = vector.nonce + "0000000000000000")) }
    }

    @Test
    fun normalizationGivesItsKnownAnswers() {
        assertEquals("caf\u00E9", crypto.normalizeNfc("cafe\u0301"))
        assertEquals("\u00C5", crypto.normalizeNfc("\u212B"))
        assertEquals("\uFB01", crypto.normalizeNfc("\uFB01"))
        assertEquals("fi", crypto.normalizeNfkc("\uFB01"))
        assertEquals("A1", crypto.normalizeNfkc("\uFF21\u2460"))
        assertEquals("\u30AC", crypto.normalizeNfkc("\uFF76\uFF9E"))
    }

    @Test
    fun randomBytesComeFromTheSourceItWasGiven() {
        assertArrayEquals(bytes("00010203"), JcaCryptoPrimitives(CountingSecureRandom()).randomBytes(4))
        assertEquals(32, crypto.randomBytes(32).size)
        assertFalse(crypto.randomBytes(32).contentEquals(crypto.randomBytes(32)))
    }

    private fun seal(vector: GcmVector): ByteArray =
        crypto.aesGcmSeal(bytes(vector.key), bytes(vector.nonce), bytes(vector.associatedData), bytes(vector.plaintext))

    private fun open(vector: GcmVector): ByteArray? =
        crypto.aesGcmOpen(bytes(vector.key), bytes(vector.nonce), bytes(vector.associatedData), bytes(vector.sealed))

    private fun flip(bytes: ByteArray, index: Int) = bytes.copyOf().also { it[index] = (it[index].toInt() xor 1).toByte() }

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    private fun bytes(hex: String): ByteArray = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private data class Pbkdf2Vector(val secret: String, val salt: String, val iterations: Int, val expected: String)

    private data class GcmVector(val key: String, val nonce: String, val associatedData: String, val plaintext: String, val sealed: String)

    private companion object {
        const val SALT_A = "101112131415161718191a1b1c1d1e1f"

        val PBKDF2 = listOf(
            // RFC 7914, section 11 (the first 32 bytes).
            Pbkdf2Vector("passwd", "73616c74", 1, "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc"),
            Pbkdf2Vector("Password", "4e61436c", 80_000, "4ddcd8f60b98be21830cee5ef22701f9641a4418d04c0414aeff08876b34ab56"),
            // A PIN at the profile's iteration count, then answers in the app's scripts, then a backup passphrase.
            Pbkdf2Vector(
                "135790",
                "000102030405060708090a0b0c0d0e0f",
                210_000,
                "cf62abfb966669173e02e9ab9fc17779418759795964dc97fab43a4f4c5611c1"
            ),
            Pbkdf2Vector("café müller", SALT_A, 10_000, "8dba8f749cf51925cff988ab7d988d121e9b4de1d21fe0ccb548d1a42e5f22dc"),
            Pbkdf2Vector("मेरा पहला स्कूल", SALT_A, 10_000, "8f680ec12b07ee92c17aaf510797641f9d1cbc8cbab2c0627899ac01754fdb39"),
            Pbkdf2Vector("我的第一所学校", SALT_A, 10_000, "bdefaf22acba2d1bdd9e16f1d8936acfb34430647feb1a7f8a7336d74d43ae55"),
            Pbkdf2Vector("مدرستي الأولى", SALT_A, 10_000, "7b9f34d40d08f7208db2bbd229417891c1b5cca9d0e73de498388b1a31859cc8"),
            Pbkdf2Vector(
                "café au lait chaud",
                "202122232425262728292a2b2c2d2e2f",
                100_000,
                "09fffbd097fe4966169d167b7a17a5322cbbdb6bef60477aeb1b6c01732b43e7"
            )
        )

        val GCM = listOf(
            // Wrapping a database key under the PIN slot's associated data, as the profile does.
            GcmVector(
                key = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f",
                nonce = "6465666768696a6b6c6d6e6f",
                associatedData = "64656e617269692d646f6c6f723a70696e3a7632",
                plaintext = "c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7",
                sealed = "80d214adb5249851eeb38d3b0eb0bc2a9a1bdcd157b12dad47304eab1f4643af6a868555997136136d4d3486340707b9"
            ),
            GcmVector(
                key = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f",
                nonce = "6465666768696a6b6c6d6e6f",
                associatedData = "",
                plaintext = "",
                sealed = "609bd50f6ad76df665f048ba8c5fd706"
            ),
            // The GCM specification's test case 14.
            GcmVector(
                key = "0000000000000000000000000000000000000000000000000000000000000000",
                nonce = "000000000000000000000000",
                associatedData = "",
                plaintext = "00000000000000000000000000000000",
                sealed = "cea7403d4d606b6e074ec5d3baf39d18d0d1c8a799996bf0265b98b5d48ab919"
            )
        )
    }
}
