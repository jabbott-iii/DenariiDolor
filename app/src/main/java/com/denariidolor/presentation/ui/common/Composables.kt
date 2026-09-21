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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Data-visualization roles. Values follow the validated reference palette; light and dark are selected, not flipped. */
@Immutable
data class VizColors(
    val series1: Color,
    val meterTrack: Color,
    val statusGood: Color,
    val statusWarning: Color,
    val statusCritical: Color
)

private val LightViz = VizColors(
    series1 = Color(0xFF2A78D6),
    meterTrack = Color(0xFFCDE2FB),
    statusGood = Color(0xFF0CA30C),
    statusWarning = Color(0xFFFAB219),
    statusCritical = Color(0xFFD03B3B)
)

private val DarkViz = VizColors(
    series1 = Color(0xFF3987E5),
    meterTrack = Color(0xFF184F95),
    statusGood = Color(0xFF0CA30C),
    statusWarning = Color(0xFFFAB219),
    statusCritical = Color(0xFFD03B3B)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF256ABF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE2FB),
    onPrimaryContainer = Color(0xFF0D366B),
    background = Color(0xFFFCFCFB),
    surface = Color(0xFFFCFCFB),
    onBackground = Color(0xFF0B0B0B),
    onSurface = Color(0xFF0B0B0B),
    onSurfaceVariant = Color(0xFF52514E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF86B6EF),
    onPrimary = Color(0xFF0D366B),
    primaryContainer = Color(0xFF184F95),
    onPrimaryContainer = Color(0xFFCDE2FB),
    background = Color(0xFF1A1A19),
    surface = Color(0xFF1A1A19),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFC3C2B7)
)

val LocalVizColors = staticCompositionLocalOf { LightViz }

@Composable
fun DenariiDolorTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVizColors provides if (darkTheme) DarkViz else LightViz) {
        MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors) {
            Surface(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}
