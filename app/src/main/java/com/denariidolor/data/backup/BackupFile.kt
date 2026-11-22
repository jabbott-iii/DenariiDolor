/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.backup

import com.denariidolor.data.local.preferences.CryptoPrimitives
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer

/** Why a backup couldn't be made or opened. The UI maps each one to a message. */
enum class BackupError {
    PASSPHRASE_LENGTH,
    PASSPHRASE_MISMATCH,
    NOT_A_BACKUP,
    NEWER_VERSION,
    WRONG_PASSPHRASE,
    DAMAGED,
    READ_FAILED
}

class BackupException(val error: BackupError, cause: Throwable? = null) : Exception(error.name, cause)

/**
 * A backup file (CS-28): a 40-byte header, then the [BackupCodec] payload encrypted with AES-256-GCM.
 *
 * | Bytes | Field |
 * |---|---|
 * | 4 | Magic `DDBK` |
 * | 4 | Format version |
 * | 4 | PBKDF2 iterations |
 * | 16 | Salt |
 * | 12 | GCM nonce |
 *
 * The key is PBKDF2-HMAC-SHA256 of the NFC-normalized passphrase, and the whole header is the GCM associated data, so a changed
 * header fails like a wrong passphrase. Unlike the vault's keys, this key isn't tied to the device, because the file has to open
 * on a new phone: the passphrase alone stands against offline guessing, hence its minimum length and the iteration count.
 */
object BackupFile {
    const val FORMAT_VERSION = 1
    const val MIME_TYPE = "application/octet-stream"
    const val FILE_EXTENSION = "ddbackup"
    const val MIN_PASSPHRASE_LENGTH = 12
    const val MAX_PASSPHRASE_LENGTH = 256

    /** The largest backup [readFrom] accepts, so also the largest one worth saving. Ten years of daily entries take a few megabytes. */
    const val MAX_FILE_BYTES = 32 * 1024 * 1024

    /** OWASP's recommendation for PBKDF2-HMAC-SHA256 (2023). */
    const val DEFAULT_ITERATIONS = 600_000

    // A file may ask for more iterations than this build uses, within bounds that keep a damaged or crafted header from stalling
    // the phone: Android derives the key in Java, at a few hundred thousand iterations a second on older phones.
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 2_000_000

    private val MAGIC = "DDBK".toByteArray(Charsets.US_ASCII)
    private const val VERSION_OFFSET = 4
    private const val ITERATIONS_OFFSET = 8
    private const val SALT_OFFSET = 12
    private const val SALT_BYTES = 16
    private const val NONCE_OFFSET = SALT_OFFSET + SALT_BYTES
    private const val NONCE_BYTES = 12
    private const val HEADER_BYTES = NONCE_OFFSET + NONCE_BYTES
    private const val TAG_BYTES = 16
    private const val CHUNK_BYTES = 64 * 1024

    /** What is wrong with [passphrase], or null if it can protect a backup. Pass [confirmation] when the passphrase is new. */
    fun checkPassphrase(crypto: CryptoPrimitives, passphrase: String, confirmation: String? = null): BackupError? {
        val normalized = crypto.normalizeNfc(passphrase)
        val length = normalized.codePointCount(0, normalized.length)
        return when {
            normalized.isBlank() || length !in MIN_PASSPHRASE_LENGTH..MAX_PASSPHRASE_LENGTH -> BackupError.PASSPHRASE_LENGTH
            confirmation != null && crypto.normalizeNfc(confirmation) != normalized -> BackupError.PASSPHRASE_MISMATCH
            else -> null
        }
    }

    /** Encrypts [snapshot] under [passphrase]. Throws [BackupException] when the passphrase is too short or too long. */
    fun write(crypto: CryptoPrimitives, snapshot: BackupSnapshot, passphrase: String, iterations: Int = DEFAULT_ITERATIONS): ByteArray {
        checkPassphrase(crypto, passphrase)?.let { throw BackupException(it) }
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Unsupported iteration count" }
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC)
            .putInt(FORMAT_VERSION)
            .putInt(iterations)
            .put(crypto.randomBytes(SALT_BYTES))
            .put(crypto.randomBytes(NONCE_BYTES))
            .array()
        val payload = BackupCodec.encode(snapshot)
        return try {
            header + withKey(crypto, passphrase, header) { key, nonce -> crypto.aesGcmSeal(key, nonce, header, payload) }
        } finally {
            payload.fill(0)
        }
    }

    /**
     * Reads a backup file from [input]: the header first, so another kind of file (a video picked by mistake) is refused after
     * 40 bytes, then the rest, up to [MAX_FILE_BYTES]. Throws [BackupException] with [BackupError.NOT_A_BACKUP] for a file that
     * doesn't start like a backup or is too large to be one. I/O errors propagate.
     */
    fun readFrom(input: InputStream): ByteArray {
        val bytes = ByteArrayOutputStream()
        input.copyUpTo(bytes, HEADER_BYTES)
        val start = bytes.toByteArray()
        if (start.size < HEADER_BYTES || !start.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw BackupException(BackupError.NOT_A_BACKUP)
        }
        input.copyUpTo(bytes, MAX_FILE_BYTES - HEADER_BYTES + 1)
        if (bytes.size() > MAX_FILE_BYTES) throw BackupException(BackupError.NOT_A_BACKUP)
        return bytes.toByteArray()
    }

    /**
     * Decrypts and decodes [file]. Throws [BackupException]: [BackupError.NOT_A_BACKUP] for another kind of file,
     * [BackupError.NEWER_VERSION] for a format this build can't read, [BackupError.WRONG_PASSPHRASE] when authentication fails
     * (a wrong passphrase and a changed file can't be told apart), and [BackupError.DAMAGED] for anything else.
     */
    fun read(crypto: CryptoPrimitives, file: ByteArray, passphrase: String): BackupSnapshot {
        headerProblem(file)?.let { throw BackupException(it) }
        val header = file.copyOfRange(0, HEADER_BYTES)
        val sealed = file.copyOfRange(HEADER_BYTES, file.size)
        val payload = withKey(crypto, passphrase, header) { key, nonce -> crypto.aesGcmOpen(key, nonce, header, sealed) }
            ?: throw BackupException(BackupError.WRONG_PASSPHRASE)
        return try {
            BackupCodec.decode(payload)
        } finally {
            payload.fill(0)
        }
    }

    private fun headerProblem(file: ByteArray): BackupError? {
        if (file.size < HEADER_BYTES || !file.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) return BackupError.NOT_A_BACKUP
        val header = ByteBuffer.wrap(file)
        val version = header.getInt(VERSION_OFFSET)
        return when {
            version > FORMAT_VERSION -> BackupError.NEWER_VERSION
            version < 1 -> BackupError.DAMAGED
            header.getInt(ITERATIONS_OFFSET) !in MIN_ITERATIONS..MAX_ITERATIONS -> BackupError.DAMAGED
            file.size < HEADER_BYTES + TAG_BYTES -> BackupError.DAMAGED
            else -> null
        }
    }

    /** Runs [use] with the key [header] calls for and its nonce, then zeroes the key. */
    private inline fun <T> withKey(
        crypto: CryptoPrimitives,
        passphrase: String,
        header: ByteArray,
        use: (key: ByteArray, nonce: ByteArray) -> T
    ): T {
        val iterations = ByteBuffer.wrap(header).getInt(ITERATIONS_OFFSET)
        val salt = header.copyOfRange(SALT_OFFSET, SALT_OFFSET + SALT_BYTES)
        val nonce = header.copyOfRange(NONCE_OFFSET, NONCE_OFFSET + NONCE_BYTES)
        // NFC, so the same passphrase typed on two keyboards that compose accents differently still opens the file.
        val key = crypto.pbkdf2HmacSha256(crypto.normalizeNfc(passphrase), salt, iterations)
        return try {
            use(key, nonce)
        } finally {
            key.fill(0)
        }
    }

    private fun InputStream.copyUpTo(out: ByteArrayOutputStream, count: Int) {
        val chunk = ByteArray(minOf(count, CHUNK_BYTES))
        var copied = 0
        while (copied < count) {
            val read = read(chunk, 0, minOf(chunk.size, count - copied))
            if (read < 0) break
            out.write(chunk, 0, read)
            copied += read
        }
    }
}
