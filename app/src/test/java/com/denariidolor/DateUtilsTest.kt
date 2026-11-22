/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import com.denariidolor.util.DateUtils
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {
    private val zoneId = TimeZone.of("UTC")

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
        assertEquals(LocalDate(2024, 9, 1), DateUtils.parseIsoDate("٢٠٢٤-٠٩-٠١"))
        assertEquals(LocalDate(2024, 9, 1), DateUtils.parseIsoDate(" २०२४-०९-०१ "))
        assertEquals(1_725_148_800_000L, DateUtils.parseIsoDateToStartOfDayEpochMillis("٢٠٢٤-٠٩-٠١", zoneId))
    }

    @Test
    fun formatLocalDateUsesZone() {
        assertEquals("2024-09-01", DateUtils.formatLocalDate(1_725_148_800_000L, zoneId))
        assertEquals("2024-08-31", DateUtils.formatLocalDate(1_725_148_800_000L, TimeZone.of("America/Phoenix")))
    }

    @Test
    fun formatLocalDateUsesQuarterHourOffsets() {
        assertEquals("2024-09-01", DateUtils.formatLocalDate(1_725_128_100_000L, TimeZone.of("Asia/Kathmandu")))
        assertEquals("2024-08-31", DateUtils.formatLocalDate(1_725_128_099_999L, TimeZone.of("Asia/Kathmandu")))
    }

    /** São Paulo moved its clocks from 00:00 straight to 01:00 on 2018-11-04, so that day starts at 01:00. */
    @Test
    fun aDayWhoseMidnightWasSkippedStartsAtItsFirstMoment() {
        val saoPaulo = TimeZone.of("America/Sao_Paulo")

        assertEquals(1_541_300_400_000L, DateUtils.parseIsoDateToStartOfDayEpochMillis("2018-11-04", saoPaulo))
        assertEquals(1_541_300_399_999L, DateUtils.parseIsoDateToEndOfDayEpochMillis("2018-11-03", saoPaulo))
    }

    /** Samoa skipped 2011-12-30 when it moved across the date line, so its December had 30 days. */
    @Test
    fun aMonthWithASkippedDayIsThatMuchShorter() {
        val apia = TimeZone.of("Pacific/Apia")

        assertEquals(1_322_733_600_000L to 1_325_325_599_999L, DateUtils.monthRangeEpochMillis(2011, 12, apia))
        assertEquals("2011-12-31", DateUtils.formatLocalDate(1_325_239_200_000L, apia))
    }
}
