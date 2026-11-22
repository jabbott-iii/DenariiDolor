/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.backup

// The layout of java.io.DataOutput, written out in plain Kotlin so the backup format doesn't depend on java.io and reads the
// same on every platform. Backup format version 1 was written with DataOutputStream, so these must match it byte for byte.
//
// - Int and Long: 4 and 8 bytes, big-endian, two's complement.
// - Boolean: one byte, 1 or 0. Any byte other than 0 reads as true, as DataInput.readBoolean does.
// - Text: a 2-byte unsigned length, then that many bytes of Java's modified UTF-8. Each UTF-16 unit is encoded on its own, so a
//   character outside the BMP takes two 3-byte sequences rather than one 4-byte sequence, and U+0000 takes two bytes.

private const val BYTE_MASK = 0xFF
private const val INT_MASK = 0xFFFF_FFFFL
private const val MAX_TEXT_BYTES = 0xFFFF
private const val ONE_BYTE_MAX = 0x7F
private const val TWO_BYTE_MAX = 0x7FF
private const val MAX_UNIT_BYTES = 3
private const val CONTINUATION = 0x80
private const val CONTINUATION_MASK = 0xC0
private const val CONTINUATION_BITS = 0x3F
private const val TWO_BYTE_LEAD = 0xC0
private const val TWO_BYTE_LEAD_MASK = 0xE0
private const val TWO_BYTE_LEAD_BITS = 0x1F
private const val THREE_BYTE_LEAD = 0xE0
private const val THREE_BYTE_LEAD_MASK = 0xF0
private const val THREE_BYTE_LEAD_BITS = 0x0F
private const val BITS_PER_CONTINUATION = 6
private const val INITIAL_CAPACITY = 1024

/** Writes values in [java.io.DataOutput]'s layout. [finish] hands over the bytes; the writer keeps no copy of them. */
internal class BigEndianWriter {
    private var buffer = ByteArray(INITIAL_CAPACITY)
    private var size = 0

    fun writeInt(value: Int) {
        reserve(Int.SIZE_BYTES)
        for (shift in Int.SIZE_BITS - Byte.SIZE_BITS downTo 0 step Byte.SIZE_BITS) put(value ushr shift)
    }

    fun writeLong(value: Long) {
        writeInt((value ushr Int.SIZE_BITS).toInt())
        writeInt(value.toInt())
    }

    fun writeBoolean(value: Boolean) {
        reserve(1)
        put(if (value) 1 else 0)
    }

    /** Throws [IllegalArgumentException] when [value] takes more than 65,535 bytes, as `DataOutput.writeUTF` refuses it. */
    fun writeUtf(value: String) {
        val length = value.sumOf(::encodedLength)
        require(length <= MAX_TEXT_BYTES) { "Text too long for a backup: $length bytes" }
        reserve(Short.SIZE_BYTES + length)
        put(length ushr Byte.SIZE_BITS)
        put(length)
        value.forEach { char ->
            val unit = char.code
            when (unit) {
                in 1..ONE_BYTE_MAX -> put(unit)
                in 0..TWO_BYTE_MAX -> {
                    put(TWO_BYTE_LEAD or ((unit ushr BITS_PER_CONTINUATION) and TWO_BYTE_LEAD_BITS))
                    put(CONTINUATION or (unit and CONTINUATION_BITS))
                }
                else -> {
                    put(THREE_BYTE_LEAD or ((unit ushr (2 * BITS_PER_CONTINUATION)) and THREE_BYTE_LEAD_BITS))
                    put(CONTINUATION or ((unit ushr BITS_PER_CONTINUATION) and CONTINUATION_BITS))
                    put(CONTINUATION or (unit and CONTINUATION_BITS))
                }
            }
        }
    }

    /** Returns everything written, then zeroes the writer's buffer and empties it. */
    fun finish(): ByteArray = buffer.copyOf(size).also {
        buffer.fill(0)
        size = 0
    }

    private fun put(value: Int) {
        buffer[size++] = value.toByte()
    }

    // A payload holds the whole ledger, so an outgrown buffer is zeroed rather than left behind with a copy of it.
    private fun reserve(count: Int) {
        if (count <= buffer.size - size) return
        val grown = buffer.copyOf(maxOf(buffer.size * 2, size + count))
        buffer.fill(0)
        buffer = grown
    }

    private fun encodedLength(char: Char): Int = when (char.code) {
        in 1..ONE_BYTE_MAX -> 1
        in 0..TWO_BYTE_MAX -> 2
        else -> MAX_UNIT_BYTES
    }
}

/**
 * Reads values in [java.io.DataInput]'s layout from [bytes]. Running out of bytes, or text that `DataInput.readUTF` would
 * reject, throws [BackupException] with [BackupError.DAMAGED].
 */
internal class BigEndianReader(private val bytes: ByteArray) {
    private var position = 0

    /** True once every byte has been read. */
    val isExhausted: Boolean
        get() = position == bytes.size

    fun readInt(): Int {
        need(Int.SIZE_BYTES)
        var value = 0
        repeat(Int.SIZE_BYTES) { value = (value shl Byte.SIZE_BITS) or next() }
        return value
    }

    fun readLong(): Long {
        val high = readInt().toLong()
        val low = readInt().toLong() and INT_MASK
        return (high shl Int.SIZE_BITS) or low
    }

    fun readBoolean(): Boolean {
        need(1)
        return next() != 0
    }

    fun readUtf(): String {
        need(Short.SIZE_BYTES)
        val high = next()
        val length = (high shl Byte.SIZE_BITS) or next()
        need(length)
        val end = position + length
        val text = StringBuilder(length)
        while (position < end) text.append(readUtfUnit(end))
        return text.toString()
    }

    // The checks of DataInput.readUTF and no more: a sequence longer than it needs to be (such as C0 80 for U+0000) and an
    // unpaired surrogate both read as they do in Java, and so does a lead byte the writer never produces but Java accepts.
    private fun readUtfUnit(end: Int): Char {
        val lead = next()
        val code = when {
            lead <= ONE_BYTE_MAX -> lead
            (lead and TWO_BYTE_LEAD_MASK) == TWO_BYTE_LEAD -> {
                if (end - position < 1) damaged()
                ((lead and TWO_BYTE_LEAD_BITS) shl BITS_PER_CONTINUATION) or continuation()
            }
            (lead and THREE_BYTE_LEAD_MASK) == THREE_BYTE_LEAD -> {
                if (end - position < 2) damaged()
                val middle = continuation()
                ((lead and THREE_BYTE_LEAD_BITS) shl (2 * BITS_PER_CONTINUATION)) or (middle shl BITS_PER_CONTINUATION) or continuation()
            }
            else -> damaged()
        }
        return code.toChar()
    }

    private fun continuation(): Int {
        val byte = next()
        if ((byte and CONTINUATION_MASK) != CONTINUATION) damaged()
        return byte and CONTINUATION_BITS
    }

    private fun need(count: Int) {
        if (count > bytes.size - position) damaged()
    }

    private fun next(): Int = bytes[position++].toInt() and BYTE_MASK

    private fun damaged(): Nothing = throw BackupException(BackupError.DAMAGED)
}
