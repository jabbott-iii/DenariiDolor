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

package com.denariidolor.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth

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

    fun parseIsoDateToStartOfDayEpochMillis(
        value: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long = LocalDate.parse(value).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun parseIsoDateToEndOfDayEpochMillis(
        value: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long = LocalDate.parse(value).plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
}
