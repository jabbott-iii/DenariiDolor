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
package com.denariidolor.data.local.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Appearance preference. Not sensitive, so it lives in plain app-private prefs (still excluded from backups).
 * `null` means "follow the device setting" until the user flips the switch.
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _darkModeOverride = MutableStateFlow(readOverride())
    val darkModeOverride: StateFlow<Boolean?> = _darkModeOverride.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _darkModeOverride.value = enabled
    }

    private fun readOverride(): Boolean? =
        if (preferences.contains(KEY_DARK_MODE)) preferences.getBoolean(KEY_DARK_MODE, false) else null

    private companion object {
        const val PREFS_NAME = "ui_prefs"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
