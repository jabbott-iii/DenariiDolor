/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/viz_colors.dart';
import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

/// The light theme: v1's colors (`Composables.kt` at 6d57526), its chart colors, and its system bars.
final ThemeData lightTheme = _theme(_lightColors, _lightViz);

/// The dark theme: v1's colors (`Composables.kt` at 6d57526), its chart colors, and its system bars.
final ThemeData darkTheme = _theme(_darkColors, _darkViz);

/// The system bars over a theme of [brightness], as v1's `ThemedContent.kt` styled them: a
/// transparent status bar, and a translucent scrim behind the navigation bar, each with icons that
/// contrast with the theme. Android adds no contrast scrim of its own to either bar.
///
/// Android 15 and later ignore the navigation bar's color, so the scrim shows only on Android 8–14.
/// Where the app isn't drawn behind the navigation bar, use [withOpaqueNavigationBar].
SystemUiOverlayStyle systemBarsStyle(Brightness brightness) =>
    switch (brightness) {
      Brightness.light => _lightSystemBars,
      Brightness.dark => _darkSystemBars,
    };

/// [style] with its navigation bar scrim painted already blended over [surface].
///
/// Flutter 3.50.1 draws the app behind the system bars (`SystemUiMode.edgeToEdge`) only from
/// Android 10. On Android 8 and 9 a translucent scrim would blend with the window's own background,
/// which is white or black and doesn't follow a change of theme, rather than with the app as v1's
/// did. Blending it over the app's surface first shows the color v1 showed.
SystemUiOverlayStyle withOpaqueNavigationBar(
  SystemUiOverlayStyle style,
  Color surface,
) {
  final scrim = style.systemNavigationBarColor;
  return scrim == null
      ? style
      : style.copyWith(
          systemNavigationBarColor: Color.alphaBlend(scrim, surface),
        );
}

ThemeData _theme(ColorScheme colors, VizColors vizColors) => ThemeData(
  colorScheme: colors,
  extensions: [vizColors],
  // An app bar styles the status bar behind it, by its own background unless told otherwise. v1
  // kept one style for the whole screen.
  appBarTheme: AppBarTheme(
    systemOverlayStyle: systemBarsStyle(colors.brightness),
  ),
);

// v1 set nine roles, and Compose Material3 1.4.0's `lightColorScheme` and `darkColorScheme` filled
// the rest from Material 3's baseline tokens. Flutter's `ColorScheme` would derive a missing role
// from the roles that are set instead, so every role is written out here. Three of Compose's roles
// aren't, because Flutter deprecated them: `background` and `onBackground`, which equal `surface`
// and `onSurface` in v1 and which Flutter fills the same way, and `surfaceVariant`, which falls back
// to `surfaceContainerHighest` (light `0xFFE7E0EC` became `0xFFE6E0E9`, dark `0xFF49454F` became
// `0xFF36343B`). No widget in material_ui 1.11.1 reads `surfaceVariant`.

const _lightColors = ColorScheme(
  brightness: Brightness.light,
  // v1's own.
  primary: Color(0xFF256ABF),
  onPrimary: Color(0xFFFFFFFF),
  primaryContainer: Color(0xFFCDE2FB),
  onPrimaryContainer: Color(0xFF0D366B),
  surface: Color(0xFFFCFCFB),
  onSurface: Color(0xFF0B0B0B),
  onSurfaceVariant: Color(0xFF52514E),
  // Compose's default: the primary color.
  surfaceTint: Color(0xFF256ABF),
  // Material 3's baseline.
  inversePrimary: Color(0xFFD0BCFF),
  primaryFixed: Color(0xFFEADDFF),
  primaryFixedDim: Color(0xFFD0BCFF),
  onPrimaryFixed: Color(0xFF21005D),
  onPrimaryFixedVariant: Color(0xFF4F378B),
  secondary: Color(0xFF625B71),
  onSecondary: Color(0xFFFFFFFF),
  secondaryContainer: Color(0xFFE8DEF8),
  onSecondaryContainer: Color(0xFF1D192B),
  secondaryFixed: Color(0xFFE8DEF8),
  secondaryFixedDim: Color(0xFFCCC2DC),
  onSecondaryFixed: Color(0xFF1D192B),
  onSecondaryFixedVariant: Color(0xFF4A4458),
  tertiary: Color(0xFF7D5260),
  onTertiary: Color(0xFFFFFFFF),
  tertiaryContainer: Color(0xFFFFD8E4),
  onTertiaryContainer: Color(0xFF31111D),
  tertiaryFixed: Color(0xFFFFD8E4),
  tertiaryFixedDim: Color(0xFFEFB8C8),
  onTertiaryFixed: Color(0xFF31111D),
  onTertiaryFixedVariant: Color(0xFF633B48),
  error: Color(0xFFB3261E),
  onError: Color(0xFFFFFFFF),
  errorContainer: Color(0xFFF9DEDC),
  onErrorContainer: Color(0xFF410E0B),
  surfaceDim: Color(0xFFDED8E1),
  surfaceBright: Color(0xFFFEF7FF),
  surfaceContainerLowest: Color(0xFFFFFFFF),
  surfaceContainerLow: Color(0xFFF7F2FA),
  surfaceContainer: Color(0xFFF3EDF7),
  surfaceContainerHigh: Color(0xFFECE6F0),
  surfaceContainerHighest: Color(0xFFE6E0E9),
  outline: Color(0xFF79747E),
  outlineVariant: Color(0xFFCAC4D0),
  scrim: Color(0xFF000000),
  inverseSurface: Color(0xFF322F35),
  onInverseSurface: Color(0xFFF5EFF7),
);

const _darkColors = ColorScheme(
  brightness: Brightness.dark,
  // v1's own.
  primary: Color(0xFF86B6EF),
  onPrimary: Color(0xFF0D366B),
  primaryContainer: Color(0xFF184F95),
  onPrimaryContainer: Color(0xFFCDE2FB),
  surface: Color(0xFF1A1A19),
  onSurface: Color(0xFFFFFFFF),
  onSurfaceVariant: Color(0xFFC3C2B7),
  // Compose's default: the primary color.
  surfaceTint: Color(0xFF86B6EF),
  // Material 3's baseline.
  inversePrimary: Color(0xFF6750A4),
  primaryFixed: Color(0xFFEADDFF),
  primaryFixedDim: Color(0xFFD0BCFF),
  onPrimaryFixed: Color(0xFF21005D),
  onPrimaryFixedVariant: Color(0xFF4F378B),
  secondary: Color(0xFFCCC2DC),
  onSecondary: Color(0xFF332D41),
  secondaryContainer: Color(0xFF4A4458),
  onSecondaryContainer: Color(0xFFE8DEF8),
  secondaryFixed: Color(0xFFE8DEF8),
  secondaryFixedDim: Color(0xFFCCC2DC),
  onSecondaryFixed: Color(0xFF1D192B),
  onSecondaryFixedVariant: Color(0xFF4A4458),
  tertiary: Color(0xFFEFB8C8),
  onTertiary: Color(0xFF492532),
  tertiaryContainer: Color(0xFF633B48),
  onTertiaryContainer: Color(0xFFFFD8E4),
  tertiaryFixed: Color(0xFFFFD8E4),
  tertiaryFixedDim: Color(0xFFEFB8C8),
  onTertiaryFixed: Color(0xFF31111D),
  onTertiaryFixedVariant: Color(0xFF633B48),
  error: Color(0xFFF2B8B5),
  onError: Color(0xFF601410),
  errorContainer: Color(0xFF8C1D18),
  onErrorContainer: Color(0xFFF9DEDC),
  surfaceDim: Color(0xFF141218),
  surfaceBright: Color(0xFF3B383E),
  surfaceContainerLowest: Color(0xFF0F0D13),
  surfaceContainerLow: Color(0xFF1D1B20),
  surfaceContainer: Color(0xFF211F26),
  surfaceContainerHigh: Color(0xFF2B2930),
  surfaceContainerHighest: Color(0xFF36343B),
  outline: Color(0xFF938F99),
  outlineVariant: Color(0xFF49454F),
  scrim: Color(0xFF000000),
  inverseSurface: Color(0xFFE6E0E9),
  onInverseSurface: Color(0xFF322F35),
);

// v1's chart colors.
const _lightViz = VizColors(
  series1: Color(0xFF2A78D6),
  meterTrack: Color(0xFFCDE2FB),
  statusGood: Color(0xFF0CA30C),
  statusWarning: Color(0xFFFAB219),
  statusCritical: Color(0xFFD03B3B),
);

const _darkViz = VizColors(
  series1: Color(0xFF3987E5),
  meterTrack: Color(0xFF184F95),
  statusGood: Color(0xFF0CA30C),
  statusWarning: Color(0xFFFAB219),
  statusCritical: Color(0xFFD03B3B),
);

// v1's `SystemBarStyle.light` and `SystemBarStyle.dark` for each bar. `statusBarBrightness` is
// iOS's name for the background under the status bar's text.
const _lightSystemBars = SystemUiOverlayStyle(
  statusBarColor: Color(0x00000000),
  statusBarIconBrightness: Brightness.dark,
  statusBarBrightness: Brightness.light,
  systemStatusBarContrastEnforced: false,
  systemNavigationBarColor: Color(0xE6FFFFFF),
  systemNavigationBarIconBrightness: Brightness.dark,
  systemNavigationBarContrastEnforced: false,
);

const _darkSystemBars = SystemUiOverlayStyle(
  statusBarColor: Color(0x00000000),
  statusBarIconBrightness: Brightness.light,
  statusBarBrightness: Brightness.dark,
  systemStatusBarContrastEnforced: false,
  systemNavigationBarColor: Color(0x801B1B1B),
  systemNavigationBarIconBrightness: Brightness.light,
  systemNavigationBarContrastEnforced: false,
);
