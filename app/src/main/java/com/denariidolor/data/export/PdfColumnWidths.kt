/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
