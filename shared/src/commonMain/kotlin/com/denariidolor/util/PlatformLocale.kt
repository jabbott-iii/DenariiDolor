/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

// The device's locales as the platform knows them. PlatformLocale.android.kt implements this on java.util.Locale.

/** A locale as the platform knows it: `java.util.Locale` on Android. */
expect class PlatformLocale

/** The locale the device writes numbers and money in. A device can set it apart from its language. */
expect fun defaultFormatLocale(): PlatformLocale

/** The device's language and region. */
expect fun defaultLocale(): PlatformLocale

/** [locale]'s language code, such as `ar`. */
internal expect fun languageCode(locale: PlatformLocale): String
