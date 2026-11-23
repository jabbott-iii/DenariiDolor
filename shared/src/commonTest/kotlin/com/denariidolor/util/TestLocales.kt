/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

// Tests name locales by BCP 47 tag and currencies by ISO 4217 code, so they need nothing from java.util and run on every
// platform against that platform's locale data.

/** The locale for [languageTag], such as "fr-FR" or "ar". */
expect fun localeOf(languageTag: String): PlatformLocale

/** The currency for an ISO 4217 [code] the platform must know. */
fun currencyOf(code: String): PlatformCurrency = checkNotNull(currencyForCode(code)) { "$code is ISO 4217" }
