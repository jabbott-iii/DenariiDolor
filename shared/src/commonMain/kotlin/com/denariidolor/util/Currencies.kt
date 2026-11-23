/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

/** The currencies amounts can be shown in. Amounts are stored as whole cents, so only currencies with 2 decimals fit. */
object Currencies {
    /** What every install showed before the currency setting existed, and the fallback for regions without a fitting currency. */
    val DEFAULT: PlatformCurrency = checkNotNull(currencyForCode("USD")) { "USD is ISO 4217" }

    // Offered in Settings, covering the regions of the app's languages; the saved currency is always offered too.
    private val COMMON = listOf(
        "USD", "EUR", "GBP", "CAD", "AUD", "CHF", "CNY", "HKD", "TWD", "SGD", "INR",
        "MXN", "ARS", "COP", "PEN", "BRL", "SAR", "AED", "EGP", "MAD", "QAR"
    )

    fun isSupported(currency: PlatformCurrency): Boolean = fractionDigits(currency) == 2

    /** The currency of [locale]'s region (India → INR, Mexico → MXN), or [DEFAULT] when there is none or it doesn't have 2 decimals. */
    fun regionDefault(locale: PlatformLocale): PlatformCurrency = currencyForRegion(locale)?.takeIf(::isSupported) ?: DEFAULT

    /** The ISO code if it names a supported currency, otherwise null. */
    fun fromCode(code: String?): PlatformCurrency? = code?.let(::currencyForCode)?.takeIf(::isSupported)

    /** The choices for Settings: the common currencies plus [current], in that order and without duplicates. */
    fun options(current: PlatformCurrency): List<PlatformCurrency> =
        (COMMON.map { code -> checkNotNull(currencyForCode(code)) { "$code is ISO 4217" } } + current).distinct()
}
