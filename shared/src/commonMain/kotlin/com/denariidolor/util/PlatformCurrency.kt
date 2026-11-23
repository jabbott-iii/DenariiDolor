/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

// Which currency a code or a region means, from the platform's own currency data. PlatformCurrency.android.kt implements it
// on java.util.Currency.

/** A currency as the platform knows it: `java.util.Currency` on Android. */
expect class PlatformCurrency

/** The currency with ISO 4217 [code], or null when the platform knows none. */
internal expect fun currencyForCode(code: String): PlatformCurrency?

/** [currency]'s ISO 4217 code, such as `EUR`. */
internal expect fun currencyCode(currency: PlatformCurrency): String

/** The currency of [locale]'s region, or null when it names no region or the region has no currency of its own. */
internal expect fun currencyForRegion(locale: PlatformLocale): PlatformCurrency?

/** How many decimals [currency] has: 2 for euros, 0 for yen. */
internal expect fun fractionDigits(currency: PlatformCurrency): Int
