/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */
package com.denariidolor.data.local.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Appearance preference. Not sensitive, so it lives in plain app-private prefs (still excluded from backups).
 * `null` means "follow the device setting" until the user flips the switch.
 */
@Singleton
class ThemePreferences @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _darkModeOverride = MutableStateFlow(readOverride())
    val darkModeOverride: StateFlow<Boolean?> = _darkModeOverride.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _darkModeOverride.value = enabled
    }

    private fun readOverride(): Boolean? = if (preferences.contains(KEY_DARK_MODE)) preferences.getBoolean(KEY_DARK_MODE, false) else null

    private companion object {
        const val PREFS_NAME = "ui_prefs"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
