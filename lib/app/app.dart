/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/app_scope.dart';
import 'package:denarii_dolor/app/root_switch.dart';
import 'package:denarii_dolor/app/theme.dart';
import 'package:denarii_dolor/l10n/app_localizations.dart';
import 'package:denarii_dolor/l10n/locale_resolution.dart';
import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

/// The app: the app scope above a [MaterialApp] whose home is the [RootSwitch].
class DenariiDolorApp extends StatelessWidget {
  const DenariiDolorApp({
    super.key,
    required this.services,
    this.themeMode = ThemeMode.system,
  });

  final AppServices services;

  /// The light or the dark theme, or the device's choice between them. M4.6 passes the user's
  /// setting, which v1 kept as `dark_mode` in `ui_prefs`: missing means the device's choice.
  final ThemeMode themeMode;

  @override
  Widget build(BuildContext context) {
    return AppScope(
      services: services,
      child: MaterialApp(
        onGenerateTitle: (context) => AppLocalizations.of(context).appName,
        // material_ui's own delegates, not the SDK's in AppLocalizations.localizationsDelegates:
        // Flutter 3.50 moved Material and Cupertino out of the SDK, and their widgets look up the
        // localizations of material_ui and cupertino_ui.
        localizationsDelegates: const [
          AppLocalizations.delegate,
          ...GlobalMaterialLocalizations.delegates,
        ],
        supportedLocales: AppLocalizations.supportedLocales,
        localeListResolutionCallback: resolveAppLocale,
        theme: lightTheme,
        darkTheme: darkTheme,
        themeMode: themeMode,
        builder: (context, child) =>
            _SystemBars(child: child ?? const SizedBox.shrink()),
        home: const RootSwitch(),
      ),
    );
  }
}

/// Styles the system bars for the theme over every screen, as v1 did. An app bar styles the status
/// bar behind it the same way, through the theme's `appBarTheme`.
class _SystemBars extends StatelessWidget {
  const _SystemBars({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final style = systemBarsStyle(theme.brightness);
    // The navigation bar adds padding at the bottom, or at one side in landscape, only when the app
    // is drawn behind it, which Flutter does from Android 10.
    final padding = MediaQuery.viewPaddingOf(context);
    final isBehindNavigationBar =
        padding.bottom > 0 || padding.left > 0 || padding.right > 0;
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: isBehindNavigationBar
          ? style
          : withOpaqueNavigationBar(style, theme.colorScheme.surface),
      child: child,
    );
  }
}
