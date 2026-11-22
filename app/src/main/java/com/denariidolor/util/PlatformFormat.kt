/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toJavaYearMonth
import kotlinx.datetime.toJavaZoneId

// The device's own rules for writing money, month names and zone names, which each platform keeps in its own locale data. The
// code that calls these is plain Kotlin. This is the Android version, on java.text and java.time; once there is a shared module
// they become expect/actual functions, and the iOS version must produce what MoneySnapshotTest pins.

/** What [Money.parseToCents] needs from [locale]: its decimal separator and the ways it writes [currency]. */
internal fun localeNumberSymbols(locale: Locale, currency: Currency): NumberSymbols = NumberSymbols(
    decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator,
    currencyMarks = listOf(currency.getSymbol(locale), currency.symbol, currency.currencyCode)
)

/** [cents] in [currency] as [locale] writes money; [showPlus] adds a `+` to positive amounts. */
internal fun localeMoneyText(cents: Long, currency: Currency, locale: Locale, showPlus: Boolean): String {
    val format = NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = currency
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    if (showPlus && cents > 0 && format is DecimalFormat) format.positivePrefix = "+" + format.positivePrefix
    return format.format(BigDecimal.valueOf(cents, 2))
}

/** The month and year as [locale] names them, such as `septiembre 2026`. */
internal fun localeMonthYearText(period: YearMonth, locale: Locale): String =
    period.toJavaYearMonth().format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))

/** `2026-09-21 12:00 EDT`: digits in every locale, with the zone's short name. */
internal fun zonedDateTimeText(epochMillis: Long, timeZone: TimeZone): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z", Locale.US).withZone(timeZone.toJavaZoneId())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
