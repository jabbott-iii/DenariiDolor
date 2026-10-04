/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object DateUtils {
    fun monthRangeEpochMillis(year: Int, month: Int, zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val ym = YearMonth.of(year, month)
        val start = ym.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = ym.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        return start to end
    }

    fun formatIso(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).toString()

    fun formatLocalDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().toString()

    /** A `YYYY-MM-DD` date, also when typed with Arabic-Indic, Devanagari or other non-ASCII digits. */
    fun parseIsoDate(value: String): LocalDate = LocalDate.parse(Money.normalizeDigits(value).trim())

    fun parseIsoDateToStartOfDayEpochMillis(value: String, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        parseIsoDate(value).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun parseIsoDateToEndOfDayEpochMillis(value: String, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        parseIsoDate(value).plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
}
