/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.report

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toJavaYearMonth
import kotlinx.datetime.toJavaZoneId

// kotlinx-datetime has no localized month names or zone abbreviations, so the two labels still format through java.time. They
// move to a platform formatter along with the money formatting, which needs the device locale too.
object ReportText {
    const val TITLE = "Monthly Spending Report"

    fun periodLabel(period: YearMonth, locale: Locale = Locale.getDefault()): String =
        period.toJavaYearMonth().format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))

    fun generatedLabel(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z", Locale.US).withZone(timeZone.toJavaZoneId())
        return formatter.format(Instant.ofEpochMilli(epochMillis))
    }

    fun fileName(period: YearMonth, extension: String): String = "spending-report-$period.$extension"
}
