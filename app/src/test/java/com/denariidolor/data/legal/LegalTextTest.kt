/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.legal

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalTextTest {
    @Test
    fun markdownBecomesHeadingsParagraphsBulletsAndRows() {
        val markdown = """
            # Privacy policy

            **Effective:** 4 October 2026. See [LICENSE](LICENSE)
            and `THIRD_PARTY_NOTICES.md`.

            ## What stays on your phone
            - Transactions, **budgets** and accounts.
            - Settings.

            | Component | License |
            |---|:---:|
            | Room | Apache License 2.0 |
            | [SQLCipher](https://www.zetetic.net) | BSD 3-Clause |
        """.trimIndent()

        assertEquals(
            listOf(
                LegalBlock.Heading("Privacy policy", 1),
                LegalBlock.Paragraph("Effective: 4 October 2026. See LICENSE and THIRD_PARTY_NOTICES.md."),
                LegalBlock.Heading("What stays on your phone", 2),
                LegalBlock.Bullet("Transactions, budgets and accounts."),
                LegalBlock.Bullet("Settings."),
                LegalBlock.Row(listOf("Room", "Apache License 2.0")),
                LegalBlock.Row(listOf("SQLCipher", "BSD 3-Clause"))
            ),
            LegalText.parseMarkdown(markdown)
        )
    }

    @Test
    fun windowsLineBreaksAreAccepted() {
        assertEquals(
            listOf(LegalBlock.Heading("Title", 1), LegalBlock.Paragraph("One two")),
            LegalText.parseMarkdown("# Title\r\n\r\nOne\r\ntwo\r\n")
        )
    }

    @Test
    fun plainTextParagraphsAreReflowed() {
        val text = "   Apache License\n  Version 2.0\n\n   1. Definitions.\n\n      \"License\" shall mean\n      the terms.\n"

        assertEquals(
            listOf(
                LegalBlock.Paragraph("Apache License Version 2.0"),
                LegalBlock.Paragraph("1. Definitions."),
                LegalBlock.Paragraph("\"License\" shall mean the terms.")
            ),
            LegalText.parsePlainText(text)
        )
    }

    /** The app shows the repository's own files, so they must stay within the Markdown subset the parser supports. */
    @Test
    fun theShippedLegalFilesUseOnlySupportedMarkdown() {
        listOf("PRIVACY.md", "THIRD_PARTY_NOTICES.md").forEach { name ->
            val file = listOf(File("..", name), File(name)).first { it.exists() }
            val blocks = LegalText.parseMarkdown(file.readText())

            assertTrue(name, blocks.first() is LegalBlock.Heading)
            blocks.forEach { block ->
                val text = when (block) {
                    is LegalBlock.Heading -> block.text
                    is LegalBlock.Paragraph -> block.text
                    is LegalBlock.Bullet -> block.text
                    is LegalBlock.Row -> block.cells.joinToString("|")
                }
                listOf("**", "](", "`", "<").forEach { marker -> assertFalse("$name: '$marker' left in \"$text\"", marker in text) }
                listOf("* ", "+ ", "1. ").forEach { marker -> assertFalse("$name: list '$marker' in \"$text\"", text.startsWith(marker)) }
            }
        }
    }
}
