/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import kotlinx.coroutines.flow.StateFlow

/** The part of [ThemePreferences] that Settings uses; an interface, so the Settings ViewModel can be tested. */
interface ThemeSettings {
    /** The user's choice, or null to follow the device setting. */
    val darkModeOverride: StateFlow<Boolean?>

    fun setDarkMode(enabled: Boolean)
}
