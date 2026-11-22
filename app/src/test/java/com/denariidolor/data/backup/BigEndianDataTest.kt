/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.UTFDataFormatException
import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Backup format version 1 was written with `DataOutputStream`, so the reader and writer are checked against java.io itself. */
class BigEndianDataTest {
    @Test
    fun numbersAndFlagsAreWrittenAsDataOutputWritesThem() {
        val random = Random(SEED)
        val ints = listOf(0, 1, -1, Int.MIN_VALUE, Int.MAX_VALUE) + List(100) { random.nextInt() }
        val longs = listOf(0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE) + List(100) { random.nextLong() }

        assertArrayEquals(
            java {
                ints.forEach(::writeInt)
                longs.forEach(::writeLong)
                writeBoolean(true)
                writeBoolean(false)
            },
            ours {
                ints.forEach(::writeInt)
                longs.forEach(::writeLong)
                writeBoolean(true)
                writeBoolean(false)
            }
        )
    }

    @Test
    fun numbersReadBackAsDataInputReadsThem() {
        val bytes = Random(SEED).nextBytes(Long.SIZE_BYTES * 50)
        val java = DataInputStream(ByteArrayInputStream(bytes))
        val ours = BigEndianReader(bytes)

        repeat(25) {
            assertEquals(java.readInt(), ours.readInt())
            assertEquals(java.readLong(), ours.readLong())
            assertEquals(java.readInt(), ours.readInt())
        }
        assertTrue(ours.isExhausted)
    }

    @Test
    fun aFlagByteOtherThanZeroReadsAsTrueAsInJava() {
        listOf(0, 1, 2, 0x80, 0xFF).forEach { byte ->
            val bytes = byteArrayOf(byte.toByte())

            assertEquals("byte $byte", DataInputStream(ByteArrayInputStream(bytes)).readBoolean(), BigEndianReader(bytes).readBoolean())
        }
    }

    @Test
    fun textIsWrittenInJavasModifiedUtf8() {
        val random = Random(SEED)
        val randomUnits = List(200) { String(CharArray(random.nextInt(0, 40)) { random.nextInt(0, 0x10000).toChar() }) }

        (SAMPLE_TEXT + randomUnits).forEach { text ->
            assertArrayEquals(text, java { writeUTF(text) }, ours { writeUtf(text) })
        }
    }

    @Test
    fun textUpToTheLengthLimitIsWrittenAndReadBack() {
        listOf("a".repeat(MAX_TEXT_BYTES), "ࠀ".repeat(MAX_TEXT_BYTES / 3), "\u0000".repeat(MAX_TEXT_BYTES / 2)).forEach { text ->
            val bytes = ours { writeUtf(text) }

            assertArrayEquals(java { writeUTF(text) }, bytes)
            assertEquals(text, BigEndianReader(bytes).readUtf())
        }
    }

    @Test
    fun textLongerThanDataOutputAllowsIsRefused() {
        listOf("a".repeat(MAX_TEXT_BYTES + 1), "\u0000".repeat(MAX_TEXT_BYTES / 2 + 1), "ࠀ".repeat(MAX_TEXT_BYTES / 3 + 1)).forEach {
            assertThrows(UTFDataFormatException::class.java) { java { writeUTF(it) } }
            assertThrows(IllegalArgumentException::class.java) { ours { writeUtf(it) } }
        }
    }

    @Test
    fun textReadsBackAsWritten() {
        val bytes = ours { SAMPLE_TEXT.forEach(::writeUtf) }
        val reader = BigEndianReader(bytes)

        SAMPLE_TEXT.forEach { assertEquals(it, reader.readUtf()) }
        assertTrue(reader.isExhausted)
    }

    /** Java accepts some sequences the writer never produces, so an older or hand-made file reads the same either way. */
    @Test
    fun textIsAcceptedOrRejectedExactlyAsReadUtfDoes() {
        listOf(
            "c080", "c1bf", "e08080", "eda080", "edb080", "efbfbf", "00", "7f",
            "80", "bf", "c3", "e282", "c328", "e228a1", "e28228", "f09f8d9c", "f08080", "f88080", "ff8080", "fe", "ff"
        ).forEach { hex ->
            val body = hexBytes(hex)
            assertSameUtfOutcome(byteArrayOf(0, body.size.toByte()) + body)
        }
        listOf("", "00", "0005414243", "000141", "ffff").forEach { assertSameUtfOutcome(hexBytes(it)) }
    }

    @Test
    fun randomBytesReadAsTextAsReadUtfReadsThem() {
        val random = Random(SEED)
        val interesting = intArrayOf(0x00, 0x41, 0x7F, 0x80, 0xBF, 0xC0, 0xC3, 0xDF, 0xE0, 0xED, 0xEF, 0xF0, 0xF8, 0xFF)
        repeat(20_000) {
            val body = ByteArray(random.nextInt(0, 10)) {
                (if (random.nextBoolean()) interesting[random.nextInt(interesting.size)] else random.nextInt(0, 0x100)).toByte()
            }
            val declared = (body.size + random.nextInt(-2, 3)).coerceAtLeast(0)
            assertSameUtfOutcome(byteArrayOf((declared shr 8).toByte(), declared.toByte()) + body + byteArrayOf(0x12, 0x34, 0x56, 0x78))
        }
    }

    @Test
    fun runningOutOfBytesIsDamaged() {
        listOf<BigEndianReader.() -> Unit>(
            { readInt() },
            { readLong() },
            { readBoolean() },
            { readUtf() }
        ).forEach { read ->
            assertDamaged { BigEndianReader(ByteArray(0)).read() }
        }
        assertDamaged { BigEndianReader(ByteArray(Int.SIZE_BYTES - 1)).readInt() }
        assertDamaged { BigEndianReader(ByteArray(Long.SIZE_BYTES - 1)).readLong() }
        assertDamaged { BigEndianReader(byteArrayOf(0)).readUtf() }
        assertDamaged { BigEndianReader(hexBytes("0005414243")).readUtf() }
    }

    @Test
    fun aWriterGrowsPastItsFirstBufferWithoutLosingAnything() {
        val values = List(10_000) { it * 0x0101_0101_0101L }

        assertArrayEquals(java { values.forEach(::writeLong) }, ours { values.forEach(::writeLong) })
    }

    @Test
    fun finishEmptiesTheWriter() {
        val writer = BigEndianWriter()
        writer.writeLong(Long.MAX_VALUE)

        assertEquals(Long.SIZE_BYTES, writer.finish().size)
        assertEquals(0, writer.finish().size)
    }

    private fun java(write: DataOutputStream.() -> Unit): ByteArray =
        ByteArrayOutputStream().also { bytes -> DataOutputStream(bytes).use(write) }.toByteArray()

    private fun ours(write: BigEndianWriter.() -> Unit): ByteArray = BigEndianWriter().apply(write).finish()

    /** Both read the same text from [bytes] and stop at the same place, or both refuse it. */
    private fun assertSameUtfOutcome(bytes: ByteArray) {
        val javaInput = DataInputStream(ByteArrayInputStream(bytes))
        val expected = runCatching { javaInput.readUTF() }
        val ours = BigEndianReader(bytes)
        val actual = runCatching { ours.readUtf() }
        val label = bytes.joinToString("") { "%02x".format(it) }

        assertEquals(label, expected.getOrNull(), actual.getOrNull())
        assertEquals(label, expected.isSuccess, actual.isSuccess)
        if (expected.isSuccess) {
            assertEquals(label, javaInput.available() == 0, ours.isExhausted)
            if (javaInput.available() >= Int.SIZE_BYTES) assertEquals(label, javaInput.readInt(), ours.readInt())
        } else {
            assertEquals(label, BackupError.DAMAGED, (actual.exceptionOrNull() as BackupException).error)
        }
    }

    private fun assertDamaged(read: () -> Unit) {
        assertEquals(BackupError.DAMAGED, assertThrows(BackupException::class.java) { read() }.error)
    }

    private companion object {
        const val SEED = 20_261_121
        const val MAX_TEXT_BYTES = 65_535

        val SAMPLE_TEXT = listOf(
            "",
            "A",
            "\u0000",
            "\u007F",
            "\u0080",
            "é",
            "߿",
            "ࠀ",
            "￿",
            "🍜",
            "\uD83C",
            "\uDF5C",
            "\uDF5C\uD83C",
            "Carte de crédit",
            "مطاعم 🍜",
            "工资",
            "खाना",
            "a\u0000b"
        )
    }
}
