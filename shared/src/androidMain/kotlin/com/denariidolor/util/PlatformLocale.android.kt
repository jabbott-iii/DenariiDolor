/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import java.util.Locale

actual typealias PlatformLocale = Locale

actual fun defaultFormatLocale(): PlatformLocale = Locale.getDefault(Locale.Category.FORMAT)

actual fun defaultLocale(): PlatformLocale = Locale.getDefault()

internal actual fun languageCode(locale: PlatformLocale): String = locale.language
