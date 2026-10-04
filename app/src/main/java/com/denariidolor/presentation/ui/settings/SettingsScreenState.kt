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

package com.denariidolor.presentation.ui.settings

import com.denariidolor.util.Currencies

data class SettingsScreenState(
    val sessionTimeoutMinutes: Long,
    val pinConfigured: Boolean,
    val darkMode: Boolean,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
    /** ISO 4217 code of the saved currency, and the codes Settings offers. */
    val currencyCode: String = Currencies.DEFAULT.currencyCode,
    val currencyOptions: List<String> = emptyList()
)
