/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.export

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfColumnWidthsTest {
    private fun assertSums(expected: Float, widths: List<Float>) = assertEquals(expected, widths.sum(), 0.01f)

    @Test
    fun fittedColumnsTakeTheirWidestValueAndFlexibleColumnsShareTheRest() {
        val widths = PdfColumnWidths.compute(
            listOf(
                PdfColumnSpec(minWidth = 30f, contentWidth = 60f), // date
                PdfColumnSpec(minWidth = 40f, contentWidth = 200f, weight = 1f), // category
                PdfColumnSpec(minWidth = 50f, contentWidth = 900f, weight = 3f), // description
                PdfColumnSpec(minWidth = 45f, contentWidth = 70f) // amount
            ),
            available = 540f
        )

        assertEquals(60f, widths[0], 0.01f)
        assertEquals(70f, widths[3], 0.01f)
        // 540 - 60 - 70 - 40 - 50 = 320 spare, shared 1:3.
        assertEquals(40f + 80f, widths[1], 0.01f)
        assertEquals(50f + 240f, widths[2], 0.01f)
        assertSums(540f, widths)
    }

    @Test
    fun aLongHeaderWordWidensAFittedColumn() {
        // A long translated header (e.g. "Transferencia") is wider than every value under it, so it sets the width.
        val widths = PdfColumnWidths.compute(
            listOf(PdfColumnSpec(minWidth = 90f, contentWidth = 50f), PdfColumnSpec(minWidth = 10f, contentWidth = 10f, weight = 1f)),
            available = 300f
        )

        assertEquals(90f, widths[0], 0.01f)
        assertSums(300f, widths)
    }

    @Test
    fun columnsShrinkTogetherOnlyWhenTheirMinimumsCannotFit() {
        val widths = PdfColumnWidths.compute(
            listOf(PdfColumnSpec(minWidth = 300f, contentWidth = 300f), PdfColumnSpec(minWidth = 300f, contentWidth = 900f, weight = 1f)),
            available = 400f
        )

        assertEquals(200f, widths[0], 0.01f)
        assertEquals(200f, widths[1], 0.01f)
    }

    @Test
    fun withoutFlexibleColumnsTheSpareWidthIsSplitEvenly() {
        val widths = PdfColumnWidths.compute(
            listOf(PdfColumnSpec(minWidth = 10f, contentWidth = 100f), PdfColumnSpec(minWidth = 10f, contentWidth = 100f)),
            available = 300f
        )

        assertEquals(listOf(150f, 150f), widths)
    }
}
