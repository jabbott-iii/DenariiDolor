/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.legal

/** One block of a legal document, as the app shows it. */
sealed interface LegalBlock {
    data class Heading(val text: String, val level: Int) : LegalBlock

    data class Paragraph(val text: String) : LegalBlock

    data class Bullet(val text: String) : LegalBlock

    /** A table row. Header rows are left out: each row is shown on its own, its first cell as the title. */
    data class Row(val cells: List<String>) : LegalBlock
}

/**
 * Turns the repository's legal files into [LegalBlock]s. The Markdown support covers what `PRIVACY.md` and
 * `THIRD_PARTY_NOTICES.md` use: `#` headings, paragraphs, `-` lists and pipe tables, with `**bold**`, `` `code` `` and links
 * reduced to their text. Keep those files within that subset.
 */
object LegalText {
    private val LINK = Regex("""\[([^\]]*)]\([^)]*\)""")
    private val BOLD = Regex("""\*\*(.+?)\*\*""")
    private val CODE = Regex("""`([^`]*)`""")
    private val TABLE_SEPARATOR = Regex(""":?-{3,}:?""")
    private val BLANK_LINE = Regex("""\n[ \t]*\n""")
    private val BLOCK_MARKERS = listOf("#", "- ", "|")

    fun parseMarkdown(markdown: String): List<LegalBlock> {
        val blocks = mutableListOf<LegalBlock>()
        val paragraph = mutableListOf<String>()
        fun endParagraph() {
            if (paragraph.isNotEmpty()) blocks += LegalBlock.Paragraph(inline(paragraph.joinToString(" ")))
            paragraph.clear()
        }
        normalizeLineBreaks(markdown).lineSequence().map(String::trim).forEach { line ->
            if (line.isEmpty() || BLOCK_MARKERS.any(line::startsWith)) endParagraph()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#") -> blocks += heading(line)
                line.startsWith("- ") -> blocks += LegalBlock.Bullet(inline(line.removePrefix("- ").trim()))
                line.startsWith("|") -> addTableRow(blocks, line)
                else -> paragraph += line
            }
        }
        endParagraph()
        return blocks
    }

    /** Plain-text license files: blank lines separate paragraphs, and the hard line breaks inside a paragraph are joined. */
    fun parsePlainText(text: String): List<LegalBlock> = normalizeLineBreaks(text).split(BLANK_LINE)
        .map { block -> block.lines().joinToString(" ") { it.trim() }.trim() }
        .filter(String::isNotEmpty)
        .map(LegalBlock::Paragraph)

    private fun heading(line: String) = LegalBlock.Heading(inline(line.trimStart('#').trim()), level = line.takeWhile { it == '#' }.length)

    private fun addTableRow(blocks: MutableList<LegalBlock>, line: String) {
        val cells = line.trim('|').split('|').map { inline(it.trim()) }
        if (cells.all { it.matches(TABLE_SEPARATOR) }) {
            // The row above a separator is the table's header.
            if (blocks.lastOrNull() is LegalBlock.Row) blocks.removeAt(blocks.lastIndex)
        } else {
            blocks += LegalBlock.Row(cells)
        }
    }

    private fun inline(text: String): String = text.replace(LINK, "$1").replace(BOLD, "$1").replace(CODE, "$1")

    private fun normalizeLineBreaks(text: String): String = text.replace("\r\n", "\n")
}
