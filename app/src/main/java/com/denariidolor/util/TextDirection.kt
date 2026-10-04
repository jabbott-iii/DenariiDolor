/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import java.util.Locale

// "iw" is the code older Java versions use for Hebrew.
private val RIGHT_TO_LEFT_LANGUAGES = setOf("ar", "ckb", "dv", "fa", "he", "iw", "ps", "sd", "ug", "ur", "yi")

/** True for languages written right to left, such as Arabic. */
fun isRightToLeft(locale: Locale): Boolean = locale.language in RIGHT_TO_LEFT_LANGUAGES

/**
 * A transfer's accounts, source first: `Cash → Savings`. In a right-to-left language the source is read first on the right,
 * so the arrow points left toward the destination.
 */
fun transferRoute(source: String, destination: String, locale: Locale = Locale.getDefault()): String =
    if (isRightToLeft(locale)) "$source ← $destination" else "$source → $destination"
