/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
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
