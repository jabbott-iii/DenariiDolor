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

// Android's side of PlatformFormat.kt, on java.util, java.text and java.time. Android callers keep passing Locale and
// Currency, because these are the same types.

actual typealias PlatformLocale = Locale

actual typealias PlatformCurrency = Currency

actual fun defaultFormatLocale(): PlatformLocale = Locale.getDefault(Locale.Category.FORMAT)

actual fun defaultLocale(): PlatformLocale = Locale.getDefault()

internal actual fun languageCode(locale: PlatformLocale): String = locale.language

internal actual fun currencyForCode(code: String): PlatformCurrency? = runCatching { Currency.getInstance(code) }.getOrNull()

internal actual fun currencyForRegion(locale: PlatformLocale): PlatformCurrency? = runCatching { Currency.getInstance(locale) }.getOrNull()

internal actual fun fractionDigits(currency: PlatformCurrency): Int = currency.defaultFractionDigits

internal actual fun localeNumberSymbols(locale: PlatformLocale, currency: PlatformCurrency): NumberSymbols = NumberSymbols(
    decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator,
    currencyMarks = listOf(currency.getSymbol(locale), currency.symbol, currency.currencyCode)
)

internal actual fun localeMoneyText(cents: Long, currency: PlatformCurrency, locale: PlatformLocale, showPlus: Boolean): String {
    val format = NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = currency
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    if (showPlus && cents > 0 && format is DecimalFormat) format.positivePrefix = "+" + format.positivePrefix
    return format.format(BigDecimal.valueOf(cents, 2))
}

internal actual fun localeMonthYearText(period: YearMonth, locale: PlatformLocale): String =
    period.toJavaYearMonth().format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))

internal actual fun zonedDateTimeText(epochMillis: Long, timeZone: TimeZone): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z", Locale.US).withZone(timeZone.toJavaZoneId())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
