/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
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
