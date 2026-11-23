/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.report

import com.denariidolor.util.PlatformLocale
import com.denariidolor.util.defaultLocale
import com.denariidolor.util.localeMonthYearText
import com.denariidolor.util.zonedDateTimeText
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth

object ReportText {
    const val TITLE = "Monthly Spending Report"

    fun periodLabel(period: YearMonth, locale: PlatformLocale = defaultLocale()): String = localeMonthYearText(period, locale)

    fun generatedLabel(epochMillis: Long, timeZone: TimeZone): String = zonedDateTimeText(epochMillis, timeZone)

    fun fileName(period: YearMonth, extension: String): String = "spending-report-$period.$extension"
}
