/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.denariidolor.util

import java.util.Currency
import java.util.Locale

/** The currencies amounts can be shown in. Amounts are stored as whole cents, so only currencies with 2 decimals fit. */
object Currencies {
    /** What every install showed before the currency setting existed, and the fallback for regions without a fitting currency. */
    val DEFAULT: Currency = Currency.getInstance("USD")

    // Offered in Settings, covering the regions of the app's languages; the saved currency is always offered too.
    private val COMMON = listOf(
        "USD", "EUR", "GBP", "CAD", "AUD", "CHF", "CNY", "HKD", "TWD", "SGD", "INR",
        "MXN", "ARS", "COP", "PEN", "BRL", "SAR", "AED", "EGP", "MAD", "QAR"
    )

    fun isSupported(currency: Currency): Boolean = currency.defaultFractionDigits == 2

    /** The currency of [locale]'s region (India → INR, Mexico → MXN), or [DEFAULT] when there is none or it doesn't have 2 decimals. */
    fun regionDefault(locale: Locale): Currency = runCatching { Currency.getInstance(locale) }.getOrNull()?.takeIf(::isSupported) ?: DEFAULT

    /** The ISO code if it names a supported currency, otherwise null. */
    fun fromCode(code: String?): Currency? = code?.let { runCatching { Currency.getInstance(it) }.getOrNull() }?.takeIf(::isSupported)

    /** The choices for Settings: the common currencies plus [current], in that order and without duplicates. */
    fun options(current: Currency): List<Currency> = (COMMON.map(Currency::getInstance) + current).distinct()
}
