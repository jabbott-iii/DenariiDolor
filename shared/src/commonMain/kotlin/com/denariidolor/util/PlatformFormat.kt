/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth

// The device's own rules for writing money, month names and zone names, which each platform keeps in its own locale data.
// Common code only calls these functions, with the types from PlatformLocale.kt and PlatformCurrency.kt.
// PlatformFormat.android.kt implements them on java.text and java.time; an iOS version must produce what MoneySnapshotTest
// pins.

/** What [Money.parseToCents] needs from [locale]: its decimal separator and the ways it writes [currency]. */
internal expect fun localeNumberSymbols(locale: PlatformLocale, currency: PlatformCurrency): NumberSymbols

/** [cents] in [currency] as [locale] writes money; [showPlus] adds a `+` to positive amounts. */
internal expect fun localeMoneyText(cents: Long, currency: PlatformCurrency, locale: PlatformLocale, showPlus: Boolean): String

/** The month and year as [locale] names them, such as `septiembre 2026`. */
internal expect fun localeMonthYearText(period: YearMonth, locale: PlatformLocale): String

/** `2026-09-21 12:00 EDT`: digits in every locale, with the zone's short name. */
internal expect fun zonedDateTimeText(epochMillis: Long, timeZone: TimeZone): String
