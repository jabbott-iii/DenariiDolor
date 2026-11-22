/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.denariidolor.data.local.preferences.CryptoPrimitives
import com.denariidolor.data.local.preferences.JcaCryptoPrimitives
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CryptoKnownAnswerTest's vectors on the device's own JCA providers (Conscrypt and Bouncy Castle), which the JVM tests don't
 * reach. Profiles and backups on phones were made by these providers, so a difference here, such as how a non-ASCII secret
 * becomes bytes, means the shared code's contract doesn't hold on Android.
 */
@RunWith(AndroidJUnit4::class)
class CryptoPrimitivesDeviceTest {
    private val crypto: CryptoPrimitives = JcaCryptoPrimitives()

    @Test
    fun pbkdf2GivesTheKnownAnswers() {
        PBKDF2.forEach { vector ->
            val derived = crypto.pbkdf2HmacSha256(vector.secret, bytes(vector.salt), vector.iterations)
            assertEquals("'${vector.secret}' ${vector.iterations}", vector.expected, hex(derived))
        }
    }

    @Test
    fun aesGcmSealsAndOpensTheKnownAnswers() {
        GCM.forEach { vector ->
            val (key, nonce, associatedData) = Triple(bytes(vector.key), bytes(vector.nonce), bytes(vector.associatedData))
            val sealed = crypto.aesGcmSeal(key, nonce, associatedData, bytes(vector.plaintext))
            assertEquals(vector.sealed, hex(sealed))
            assertEquals(vector.plaintext, crypto.aesGcmOpen(key, nonce, associatedData, sealed)?.let(::hex))
        }
        val vector = GCM.first()
        val changed = bytes(vector.sealed).also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }
        assertNull(crypto.aesGcmOpen(bytes(vector.key), bytes(vector.nonce), bytes(vector.associatedData), changed))
    }

    @Test
    fun normalizationGivesTheKnownAnswers() {
        assertEquals("caf\u00E9", crypto.normalizeNfc("cafe\u0301"))
        assertEquals("fi", crypto.normalizeNfkc("\uFB01"))
        assertEquals("A1", crypto.normalizeNfkc("\uFF21\u2460"))
    }

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    private fun bytes(hex: String): ByteArray = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private data class Pbkdf2Vector(val secret: String, val salt: String, val iterations: Int, val expected: String)

    private data class GcmVector(val key: String, val nonce: String, val associatedData: String, val plaintext: String, val sealed: String)

    // The same vectors as CryptoKnownAnswerTest; instrumented tests can't share JVM test sources.
    private companion object {
        const val SALT_A = "101112131415161718191a1b1c1d1e1f"

        val PBKDF2 = listOf(
            Pbkdf2Vector("passwd", "73616c74", 1, "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc"),
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
            GcmVector(
                key = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f",
                nonce = "6465666768696a6b6c6d6e6f",
                associatedData = "64656e617269692d646f6c6f723a70696e3a7632",
                plaintext = "c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7",
                sealed = "80d214adb5249851eeb38d3b0eb0bc2a9a1bdcd157b12dad47304eab1f4643af6a868555997136136d4d3486340707b9"
            ),
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
