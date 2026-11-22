/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.backup

import com.denariidolor.data.backup.BackupGoldenFiles.V1_FILE_HEX
import com.denariidolor.data.backup.BackupGoldenFiles.V1_ITERATIONS
import com.denariidolor.data.backup.BackupGoldenFiles.V1_PASSPHRASE
import com.denariidolor.data.backup.BackupGoldenFiles.V1_PAYLOAD_HEX
import com.denariidolor.data.local.preferences.JcaCryptoPrimitives
import java.security.SecureRandom
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pins the stored format byte for byte, so it can move to other platforms and libraries without a silent change (CS-28). */
class BackupGoldenFileTest {
    @Test
    fun aVersion1PayloadDecodesToItsSnapshot() {
        assertEquals(sampleSnapshot(), BackupCodec.decode(hexBytes(V1_PAYLOAD_HEX)))
    }

    @Test
    fun aVersion1FileStillRestores() {
        assertEquals(sampleSnapshot(), BackupFile.read(JcaCryptoPrimitives(), hexBytes(V1_FILE_HEX), V1_PASSPHRASE))
    }

    @Test
    fun aVersion1FileOpensWithThePassphraseTypedInAnotherUnicodeForm() {
        assertEquals(sampleSnapshot(), BackupFile.read(JcaCryptoPrimitives(), hexBytes(V1_FILE_HEX), "cafe\u0301 au lait chaud"))
    }

    /** These two follow the current version. When [BackupFile.FORMAT_VERSION] moves on, they move to that version's fixtures. */
    @Test
    fun theCurrentFormatEncodesTheVersion1Payload() {
        assertEquals(1, BackupFile.FORMAT_VERSION)
        assertArrayEquals(hexBytes(V1_PAYLOAD_HEX), BackupCodec.encode(sampleSnapshot()))
    }

    @Test
    fun theCurrentFormatWritesTheVersion1FileGivenTheSameSaltAndNonce() {
        val file = BackupFile.write(JcaCryptoPrimitives(CountingRandom()), sampleSnapshot(), V1_PASSPHRASE, V1_ITERATIONS)

        assertEquals(1, BackupFile.FORMAT_VERSION)
        assertArrayEquals(hexBytes(V1_FILE_HEX), file)
    }

    /** Hands out the bytes 00, 01, 02 … in order, so a written file's salt and nonce are known. */
    private class CountingRandom : SecureRandom() {
        private var next = 0

        override fun nextBytes(bytes: ByteArray) {
            for (i in bytes.indices) bytes[i] = next++.toByte()
        }
    }
}
