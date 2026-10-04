/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import com.denariidolor.util.DateUtils
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun parseIsoDateToStartOfDayEpochMillisUsesInclusiveStart() {
        assertEquals(1_725_148_800_000L, DateUtils.parseIsoDateToStartOfDayEpochMillis("2024-09-01", zoneId))
    }

    @Test
    fun parseIsoDateToEndOfDayEpochMillisUsesInclusiveEnd() {
        assertEquals(1_725_235_199_999L, DateUtils.parseIsoDateToEndOfDayEpochMillis("2024-09-01", zoneId))
    }

    @Test
    fun parsesDatesTypedWithArabicIndicOrDevanagariDigits() {
        assertEquals(LocalDate.of(2024, 9, 1), DateUtils.parseIsoDate("٢٠٢٤-٠٩-٠١"))
        assertEquals(LocalDate.of(2024, 9, 1), DateUtils.parseIsoDate(" २०२४-०९-०१ "))
        assertEquals(1_725_148_800_000L, DateUtils.parseIsoDateToStartOfDayEpochMillis("٢٠٢٤-٠٩-٠١", zoneId))
    }

    @Test
    fun formatLocalDateUsesZone() {
        assertEquals("2024-09-01", DateUtils.formatLocalDate(1_725_148_800_000L, zoneId))
        assertEquals("2024-08-31", DateUtils.formatLocalDate(1_725_148_800_000L, ZoneId.of("America/Phoenix")))
    }
}
