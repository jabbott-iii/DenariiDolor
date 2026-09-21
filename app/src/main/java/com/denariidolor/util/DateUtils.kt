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
