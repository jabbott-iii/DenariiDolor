package com.denariidolor.util

import java.time.Instant
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
}
