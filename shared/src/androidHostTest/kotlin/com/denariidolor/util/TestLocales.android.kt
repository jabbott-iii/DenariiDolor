/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import java.util.Locale

actual fun localeOf(languageTag: String): PlatformLocale = Locale.forLanguageTag(languageTag)
