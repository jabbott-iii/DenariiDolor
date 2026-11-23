/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import java.util.Currency

actual typealias PlatformCurrency = Currency

internal actual fun currencyForCode(code: String): PlatformCurrency? = runCatching { Currency.getInstance(code) }.getOrNull()

internal actual fun currencyCode(currency: PlatformCurrency): String = currency.currencyCode

internal actual fun currencyForRegion(locale: PlatformLocale): PlatformCurrency? = runCatching { Currency.getInstance(locale) }.getOrNull()

internal actual fun fractionDigits(currency: PlatformCurrency): Int = currency.defaultFractionDigits
