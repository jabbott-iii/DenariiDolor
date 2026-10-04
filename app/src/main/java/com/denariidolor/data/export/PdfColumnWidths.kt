/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.export

/**
 * How one report column sizes itself, in PDF points. A fitted column ([weight] 0: dates, types, amounts) is as wide as its widest
 * cell, so it never wraps. A flexible column (names, descriptions) shares the remaining width by [weight] and wraps its text.
 * [minWidth] is the header's longest word, so a header wraps between words, never inside one.
 */
internal data class PdfColumnSpec(val minWidth: Float, val contentWidth: Float, val weight: Float = 0f) {
    val isFitted: Boolean get() = weight == 0f
}

internal object PdfColumnWidths {
    /** Column widths that add up to [available]. */
    fun compute(specs: List<PdfColumnSpec>, available: Float): List<Float> {
        val base = specs.map { if (it.isFitted) maxOf(it.contentWidth, it.minWidth) else it.minWidth }
        val total = base.sum()
        // Only when single words are wider than the page: every column shrinks alike, and text then wraps inside words.
        if (total > available) return base.map { it * available / total }
        val spare = available - total
        val totalWeight = specs.sumOf { it.weight.toDouble() }.toFloat()
        return if (totalWeight > 0f) {
            specs.mapIndexed { index, spec -> base[index] + spare * spec.weight / totalWeight }
        } else {
            base.map { it + spare / specs.size }
        }
    }
}
