/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.data.legal.LegalBlock
import com.denariidolor.data.legal.LegalDocument
import com.denariidolor.data.legal.LegalDocuments
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The build copies PRIVACY.md, THIRD_PARTY_NOTICES.md and licenses/ into the APK; these read them back from its assets. */
@RunWith(AndroidJUnit4::class)
class LegalDocumentsTest {
    private val documents = LegalDocuments(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun thePrivacyPolicyShipsInTheApp() = runBlocking<Unit> {
        val blocks = documents.load(LegalDocument.PRIVACY_POLICY)

        assertEquals(LegalBlock.Heading("Denarii Dolor Privacy Policy", 1), blocks.first())
        assertTrue(blocks.any { it is LegalBlock.Paragraph && "jabbottpublicsupport@gmail.com" in it.text })
    }

    @Test
    fun theNoticesAndEveryLicenseTextShipInTheApp() = runBlocking<Unit> {
        val blocks = documents.load(LegalDocument.OPEN_SOURCE_LICENSES)

        assertEquals(LegalBlock.Heading("Third-party notices", 1), blocks.first())
        assertTrue(blocks.any { it is LegalBlock.Row && it.cells.first().startsWith("SQLCipher for Android") })
        val headings = blocks.filterIsInstance<LegalBlock.Heading>().map { it.text }
        assertTrue(headings.toString(), headings.containsAll(listOf("Apache-2.0", "SQLCipher-BSD-3-Clause")))
        assertTrue(blocks.any { it is LegalBlock.Paragraph && it.text.startsWith("Apache License Version 2.0") })
    }
}
