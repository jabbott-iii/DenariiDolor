/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.platform

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** The date fields on the transaction form and Search hold ISO text, and their picker opens on that date or on today. */
class DateFieldPickingTest {
    private val today = LocalDate(2026, 11, 22)

    @Test
    fun aBlankFieldOpensOnToday() {
        listOf("", " ", "\t").forEach { assertEquals("'$it'", today, dateFieldStart(it, today)) }
    }

    @Test
    fun aFieldWithADateOpensOnThatDate() {
        assertEquals(LocalDate(2025, 2, 28), dateFieldStart("2025-02-28", today))
        assertEquals(LocalDate(2024, 2, 29), dateFieldStart(" 2024-02-29 ", today))
        assertEquals(LocalDate(2025, 2, 28), dateFieldStart("٢٠٢٥-٠٢-٢٨", today))
    }

    @Test
    fun aFieldThatIsNotADateOpensOnToday() {
        listOf("2025-02-30", "2025-2-28", "28/02/2025", "2025-02-28T10:00", "today").forEach {
            assertEquals("'$it'", today, dateFieldStart(it, today))
        }
    }

    @Test
    fun theFieldOpensThePickerOnItsDateAndGetsTheChoiceBackAsIsoText() {
        val picker = FakeDatePicker(pick = LocalDate(2026, 1, 5))
        var field = "2025-12-31"

        picker.pickForField(field, today) { field = it }

        assertEquals(listOf(LocalDate(2025, 12, 31)), picker.openedOn)
        assertEquals("2026-01-05", field)
    }

    @Test
    fun aDismissedPickerLeavesTheFieldAlone() {
        val picker = FakeDatePicker(pick = null)
        var field = ""

        picker.pickForField(field, today) { field = it }

        assertEquals(listOf(today), picker.openedOn)
        assertEquals("", field)
    }

    /** Opens on the date it's given and, unless [pick] is null (the user dismissed it), answers with [pick]. */
    private class FakeDatePicker(private val pick: LocalDate?) : DatePicker {
        val openedOn = mutableListOf<LocalDate>()

        override fun pick(initial: LocalDate, onPicked: (LocalDate) -> Unit) {
            openedOn += initial
            pick?.let(onPicked)
        }
    }
}
