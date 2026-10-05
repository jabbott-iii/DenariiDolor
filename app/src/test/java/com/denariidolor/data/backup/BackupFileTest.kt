/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.backup

import com.denariidolor.data.local.preferences.pbkdf2
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFileTest {
    private fun write(snapshot: BackupSnapshot = sampleSnapshot(), passphrase: String = PASSPHRASE) =
        BackupFile.write(snapshot, passphrase, iterations = FAST_ITERATIONS)

    private fun failure(file: ByteArray, passphrase: String = PASSPHRASE): BackupError =
        assertThrows(BackupException::class.java) { BackupFile.read(file, passphrase) }.error

    @Test
    fun everyFieldSurvivesARoundTrip() {
        assertEquals(sampleSnapshot(), BackupFile.read(write(), PASSPHRASE))
    }

    @Test
    fun anEmptySnapshotSurvivesARoundTrip() {
        val empty = BackupSnapshot(0L, "USD", emptyList(), emptyList(), emptyList(), emptyList())

        assertEquals(empty, BackupFile.read(write(empty), PASSPHRASE))
    }

    @Test
    fun newFilesUseTheRecommendedIterationCount() {
        val file = BackupFile.write(sampleSnapshot(), PASSPHRASE)

        assertEquals(BackupFile.DEFAULT_ITERATIONS, ByteBuffer.wrap(file).getInt(ITERATIONS_OFFSET))
        assertEquals(sampleSnapshot(), BackupFile.read(file, PASSPHRASE))
    }

    @Test
    fun eachFileHasItsOwnSaltAndNonce() {
        val first = write()
        val second = write()

        assertFalse(first.copyOfRange(SALT_OFFSET, HEADER_BYTES).contentEquals(second.copyOfRange(SALT_OFFSET, HEADER_BYTES)))
        assertFalse(first.contentEquals(second))
    }

    @Test
    fun aWrongPassphraseIsRejected() {
        assertEquals(BackupError.WRONG_PASSPHRASE, failure(write(), "correct horse battery stapled"))
    }

    @Test
    fun thePassphraseIsComparedInUnicodeNormalForm() {
        val composed = "café au lait chaud"
        val decomposed = "café au lait chaud"

        assertEquals(sampleSnapshot(), BackupFile.read(write(passphrase = composed), decomposed))
    }

    @Test
    fun anyChangedByteFailsAuthentication() {
        val file = write()
        listOf(ITERATIONS_OFFSET + 3, SALT_OFFSET, NONCE_OFFSET, HEADER_BYTES, file.size - 1).forEach { index ->
            val changed = file.copyOf().also { it[index] = (it[index].toInt() xor 1).toByte() }
            assertEquals("byte $index", BackupError.WRONG_PASSPHRASE, failure(changed))
        }
    }

    /** Every header field already changes the key or nonce, or is checked; the AAD keeps it so as the format grows. */
    @Test
    fun theWholeHeaderIsAuthenticatedData() {
        val file = write()
        val header = file.copyOfRange(0, HEADER_BYTES)

        assertEquals(
            sampleSnapshot(),
            BackupCodec.decode(gcm(Cipher.DECRYPT_MODE, header, aad = true, file.copyOfRange(HEADER_BYTES, file.size)))
        )
        assertThrows(AEADBadTagException::class.java) {
            gcm(Cipher.DECRYPT_MODE, header, aad = false, file.copyOfRange(HEADER_BYTES, file.size))
        }

        val sealedWithoutAad = header + gcm(Cipher.ENCRYPT_MODE, header, aad = false, BackupCodec.encode(sampleSnapshot()))
        assertEquals(BackupError.WRONG_PASSPHRASE, failure(sealedWithoutAad))
    }

    private fun gcm(mode: Int, header: ByteArray, aad: Boolean, input: ByteArray): ByteArray {
        val key = pbkdf2(PASSPHRASE, header.copyOfRange(SALT_OFFSET, NONCE_OFFSET), FAST_ITERATIONS)
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, header, NONCE_OFFSET, HEADER_BYTES - NONCE_OFFSET))
            if (aad) updateAAD(header)
            doFinal(input)
        }
    }

    @Test
    fun aBackupIsReadWholeFromAStreamThatReturnsLittleAtATime() {
        val file = write()

        assertArrayEquals(file, BackupFile.readFrom(TrickleStream(ByteArrayInputStream(file))))
    }

    @Test
    fun anotherKindOfFileIsRefusedAfterItsHeader() {
        val video = CountingStream(size = 70L * 1024 * 1024, first = "\u0000\u0000\u0000\u0020ftypmp42".toByteArray())

        assertEquals(BackupError.NOT_A_BACKUP, assertThrows(BackupException::class.java) { BackupFile.readFrom(video) }.error)
        assertTrue("read ${video.bytesRead} bytes", video.bytesRead <= HEADER_BYTES)
    }

    @Test
    fun aFileLargerThanAnyBackupIsRefused() {
        val tooLarge = CountingStream(size = BackupFile.MAX_FILE_BYTES + 1L, first = write().copyOf(HEADER_BYTES))
        val largest = CountingStream(size = BackupFile.MAX_FILE_BYTES.toLong(), first = write().copyOf(HEADER_BYTES))

        assertEquals(BackupError.NOT_A_BACKUP, assertThrows(BackupException::class.java) { BackupFile.readFrom(tooLarge) }.error)
        assertEquals(BackupFile.MAX_FILE_BYTES, BackupFile.readFrom(largest).size)
    }

    @Test
    fun otherFilesAreNotBackups() {
        listOf(
            ByteArray(0),
            "%PDF-1.7".toByteArray(),
            "Date,Type,Category\n".toByteArray(),
            ByteArray(HEADER_BYTES - 1),
            ByteArray(100)
        ).forEach {
            assertEquals(BackupError.NOT_A_BACKUP, failure(it))
        }
        val truncatedHeader = write().copyOf(HEADER_BYTES - 1)
        assertEquals(BackupError.NOT_A_BACKUP, failure(truncatedHeader))
    }

    @Test
    fun aNewerFormatIsReportedBeforeAnyDecryption() {
        val file = write().also { ByteBuffer.wrap(it).putInt(VERSION_OFFSET, BackupFile.FORMAT_VERSION + 1) }

        assertEquals(BackupError.NEWER_VERSION, failure(file, "any passphrase at all"))
    }

    @Test
    fun aDamagedHeaderOrMissingCiphertextIsReported() {
        val zeroVersion = write().also { ByteBuffer.wrap(it).putInt(VERSION_OFFSET, 0) }
        val noIterations = write().also { ByteBuffer.wrap(it).putInt(ITERATIONS_OFFSET, 0) }
        val tooManyIterations = write().also { ByteBuffer.wrap(it).putInt(ITERATIONS_OFFSET, Int.MAX_VALUE) }
        val headerOnly = write().copyOf(HEADER_BYTES)

        listOf(zeroVersion, noIterations, tooManyIterations, headerOnly).forEach { assertEquals(BackupError.DAMAGED, failure(it)) }
    }

    @Test
    fun passphrasesNeedTwelveToTwoHundredFiftySixCharacters() {
        assertEquals(BackupError.PASSPHRASE_LENGTH, BackupFile.checkPassphrase("a".repeat(11)))
        assertNull(BackupFile.checkPassphrase("a".repeat(12)))
        assertNull(BackupFile.checkPassphrase("a".repeat(256)))
        assertEquals(BackupError.PASSPHRASE_LENGTH, BackupFile.checkPassphrase("a".repeat(257)))
        assertEquals(BackupError.PASSPHRASE_LENGTH, BackupFile.checkPassphrase(" ".repeat(12)))
        // Characters, not UTF-16 units: each emoji is two units.
        assertNull(BackupFile.checkPassphrase("🔒".repeat(12)))
        assertEquals(BackupError.PASSPHRASE_LENGTH, BackupFile.checkPassphrase("🔒".repeat(11)))
    }

    @Test
    fun aNewPassphraseMustMatchItsConfirmation() {
        assertEquals(BackupError.PASSPHRASE_MISMATCH, BackupFile.checkPassphrase(PASSPHRASE, "$PASSPHRASE "))
        assertNull(BackupFile.checkPassphrase(PASSPHRASE, PASSPHRASE))
        assertEquals(BackupError.PASSPHRASE_LENGTH, BackupFile.checkPassphrase("short", "short"))
    }

    @Test
    fun aShortPassphraseCannotProtectABackup() {
        val error = assertThrows(BackupException::class.java) { BackupFile.write(sampleSnapshot(), "too short") }.error

        assertEquals(BackupError.PASSPHRASE_LENGTH, error)
    }

    @Test
    fun aPayloadWithExtraOrMissingBytesIsDamaged() {
        val payload = BackupCodec.encode(sampleSnapshot())

        assertEquals(sampleSnapshot(), BackupCodec.decode(payload))
        assertEquals(BackupError.DAMAGED, assertThrows(BackupException::class.java) { BackupCodec.decode(payload + 0) }.error)
        assertEquals(
            BackupError.DAMAGED,
            assertThrows(BackupException::class.java) { BackupCodec.decode(payload.copyOf(payload.size - 1)) }.error
        )
    }

    @Test
    fun aNegativeListLengthIsDamaged() {
        val payload = BackupCodec.encode(BackupSnapshot(0L, "USD", emptyList(), emptyList(), emptyList(), emptyList()))
        // The account count follows the creation time (8 bytes) and the currency code (2-byte length + 3 bytes).
        ByteBuffer.wrap(payload).putInt(ACCOUNT_COUNT_OFFSET, -1)

        assertEquals(BackupError.DAMAGED, assertThrows(BackupException::class.java) { BackupCodec.decode(payload) }.error)
    }

    /** Returns at most 7 bytes per read, like a slow provider. */
    private class TrickleStream(private val source: InputStream) : InputStream() {
        override fun read(): Int = source.read()

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = source.read(buffer, offset, minOf(length, 7))
    }

    /** [size] bytes, starting with [first] and then zeros, generated as they are read; counts what was read. */
    private class CountingStream(private val size: Long, private val first: ByteArray) : InputStream() {
        var bytesRead = 0L
            private set

        override fun read(): Int = if (bytesRead >= size) -1 else first.getOrElse(bytesRead++.toInt()) { 0 }.toInt() and 0xFF

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (bytesRead >= size) return -1
            val count = minOf(length.toLong(), size - bytesRead).toInt()
            for (i in 0 until count) buffer[offset + i] = first.getOrElse((bytesRead + i).toInt()) { 0 }
            bytesRead += count
            return count
        }
    }

    private companion object {
        const val PASSPHRASE = "correct horse battery staple"
        const val FAST_ITERATIONS = 100_000
        const val VERSION_OFFSET = 4
        const val ITERATIONS_OFFSET = 8
        const val SALT_OFFSET = 12
        const val NONCE_OFFSET = 28
        const val HEADER_BYTES = 40
        const val ACCOUNT_COUNT_OFFSET = 13
    }
}
