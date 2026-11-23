/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.plusMonth
import kotlinx.datetime.toLocalDateTime

// A day starts at its first moment, which isn't midnight where a daylight-saving change skips midnight. Every zone is passed
// in, from the injected clock (rule 5); there is no system-zone default.
object DateUtils {
    fun monthRangeEpochMillis(year: Int, month: Int, timeZone: TimeZone): Pair<Long, Long> {
        val ym = YearMonth(year, month)
        val start = ym.firstDay.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val end = ym.plusMonth().firstDay.atStartOfDayIn(timeZone).toEpochMilliseconds() - 1
        return start to end
    }

    fun formatIso(epochMillis: Long): String = Instant.fromEpochMilliseconds(epochMillis).toString()

    fun formatLocalDate(epochMillis: Long, timeZone: TimeZone): String =
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).date.toString()

    /** A `YYYY-MM-DD` date, also when typed with Arabic-Indic, Devanagari or other non-ASCII digits. */
    fun parseIsoDate(value: String): LocalDate = LocalDate.parse(Money.normalizeDigits(value).trim())

    fun parseIsoDateToStartOfDayEpochMillis(value: String, timeZone: TimeZone): Long =
        parseIsoDate(value).atStartOfDayIn(timeZone).toEpochMilliseconds()

    fun parseIsoDateToEndOfDayEpochMillis(value: String, timeZone: TimeZone): Long =
        parseIsoDate(value).plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds() - 1
}
