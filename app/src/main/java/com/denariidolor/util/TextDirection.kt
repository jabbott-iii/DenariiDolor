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
