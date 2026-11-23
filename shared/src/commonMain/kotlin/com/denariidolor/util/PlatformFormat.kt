/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth

// The device's own rules for writing money, month names and zone names, which each platform keeps in its own locale data.
// Common code only passes these types along and calls these functions. PlatformFormat.android.kt implements them on
// java.util, java.text and java.time; an iOS version must produce what MoneySnapshotTest pins.

/** A locale as the platform knows it: `java.util.Locale` on Android. */
expect class PlatformLocale

/** A currency as the platform knows it: `java.util.Currency` on Android. */
expect class PlatformCurrency

/** The locale the device writes numbers and money in. A device can set it apart from its language. */
expect fun defaultFormatLocale(): PlatformLocale

/** The device's language and region. */
expect fun defaultLocale(): PlatformLocale

/** [locale]'s language code, such as `ar`. */
internal expect fun languageCode(locale: PlatformLocale): String

/** The currency with ISO 4217 [code], or null when the platform knows none. */
internal expect fun currencyForCode(code: String): PlatformCurrency?

/** The currency of [locale]'s region, or null when it names no region or the region has no currency of its own. */
internal expect fun currencyForRegion(locale: PlatformLocale): PlatformCurrency?

/** How many decimals [currency] has: 2 for euros, 0 for yen. */
internal expect fun fractionDigits(currency: PlatformCurrency): Int

/** What [Money.parseToCents] needs from [locale]: its decimal separator and the ways it writes [currency]. */
internal expect fun localeNumberSymbols(locale: PlatformLocale, currency: PlatformCurrency): NumberSymbols

/** [cents] in [currency] as [locale] writes money; [showPlus] adds a `+` to positive amounts. */
internal expect fun localeMoneyText(cents: Long, currency: PlatformCurrency, locale: PlatformLocale, showPlus: Boolean): String

/** The month and year as [locale] names them, such as `septiembre 2026`. */
internal expect fun localeMonthYearText(period: YearMonth, locale: PlatformLocale): String

/** `2026-09-21 12:00 EDT`: digits in every locale, with the zone's short name. */
internal expect fun zonedDateTimeText(epochMillis: Long, timeZone: TimeZone): String
