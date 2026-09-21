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
package com.denariidolor.presentation.ui.common

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denariidolor.data.local.preferences.ThemePreferences

private val LightNavigationScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkNavigationScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

/** The user's choice when set, otherwise the device setting. */
@Composable
fun ThemePreferences.isDarkTheme(): Boolean {
    val override by darkModeOverride.collectAsStateWithLifecycle()
    return override ?: isSystemInDarkTheme()
}

/** Sets Compose content wrapped in [DenariiDolorTheme], keeping system-bar icon colors in sync with the chosen theme. */
fun ComponentActivity.setThemedContent(themePreferences: ThemePreferences, content: @Composable () -> Unit) {
    enableEdgeToEdge()
    setContent {
        val darkTheme = themePreferences.isDarkTheme()
        LaunchedEffect(darkTheme) {
            enableEdgeToEdge(
                statusBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                },
                navigationBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(DarkNavigationScrim)
                } else {
                    SystemBarStyle.light(LightNavigationScrim, DarkNavigationScrim)
                }
            )
        }
        DenariiDolorTheme(darkTheme = darkTheme, content = content)
    }
}
