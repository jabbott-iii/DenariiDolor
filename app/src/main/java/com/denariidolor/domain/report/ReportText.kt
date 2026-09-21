package com.denariidolor.domain.report

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReportText {
    const val TITLE = "Monthly Spending Report"

    fun periodLabel(period: YearMonth, locale: Locale = Locale.getDefault()): String =
        period.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))

    fun generatedLabel(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z", Locale.US).withZone(zoneId).format(Instant.ofEpochMilli(epochMillis))

    fun fileName(period: YearMonth, extension: String): String = "spending-report-$period.$extension"
}
