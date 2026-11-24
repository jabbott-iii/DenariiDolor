/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/app.dart';
import 'package:denarii_dolor/app/app_scope.dart';
import 'package:denarii_dolor/app/session_lock.dart';
import 'package:denarii_dolor/app/theme.dart';
import 'package:denarii_dolor/app/viz_colors.dart';
import 'package:denarii_dolor/features/auth/ui/sign_in_placeholder_screen.dart';
import 'package:denarii_dolor/features/session/ui/signed_in_placeholder_screen.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import '../fakes/fake_money_parser.dart';
import '../fakes/fake_session_lock.dart';

/// Every color role, light then dark, as v1 rendered it: v1's own nine roles (and `surfaceTint`,
/// which Compose took from `primary`) in `Composables.kt` at 6d57526, and the rest from Compose
/// Material3 1.4.0's `lightColorScheme` and `darkColorScheme` defaults, which v1 used.
final _roles = <(String, Color Function(ColorScheme), int, int)>[
  ('primary', (c) => c.primary, 0xFF256ABF, 0xFF86B6EF),
  ('onPrimary', (c) => c.onPrimary, 0xFFFFFFFF, 0xFF0D366B),
  ('primaryContainer', (c) => c.primaryContainer, 0xFFCDE2FB, 0xFF184F95),
  ('onPrimaryContainer', (c) => c.onPrimaryContainer, 0xFF0D366B, 0xFFCDE2FB),
  ('surface', (c) => c.surface, 0xFFFCFCFB, 0xFF1A1A19),
  ('onSurface', (c) => c.onSurface, 0xFF0B0B0B, 0xFFFFFFFF),
  ('onSurfaceVariant', (c) => c.onSurfaceVariant, 0xFF52514E, 0xFFC3C2B7),
  ('surfaceTint', (c) => c.surfaceTint, 0xFF256ABF, 0xFF86B6EF),
  ('inversePrimary', (c) => c.inversePrimary, 0xFFD0BCFF, 0xFF6750A4),
  ('primaryFixed', (c) => c.primaryFixed, 0xFFEADDFF, 0xFFEADDFF),
  ('primaryFixedDim', (c) => c.primaryFixedDim, 0xFFD0BCFF, 0xFFD0BCFF),
  ('onPrimaryFixed', (c) => c.onPrimaryFixed, 0xFF21005D, 0xFF21005D),
  (
    'onPrimaryFixedVariant',
    (c) => c.onPrimaryFixedVariant,
    0xFF4F378B,
    0xFF4F378B,
  ),
  ('secondary', (c) => c.secondary, 0xFF625B71, 0xFFCCC2DC),
  ('onSecondary', (c) => c.onSecondary, 0xFFFFFFFF, 0xFF332D41),
  ('secondaryContainer', (c) => c.secondaryContainer, 0xFFE8DEF8, 0xFF4A4458),
  (
    'onSecondaryContainer',
    (c) => c.onSecondaryContainer,
    0xFF1D192B,
    0xFFE8DEF8,
  ),
  ('secondaryFixed', (c) => c.secondaryFixed, 0xFFE8DEF8, 0xFFE8DEF8),
  ('secondaryFixedDim', (c) => c.secondaryFixedDim, 0xFFCCC2DC, 0xFFCCC2DC),
  ('onSecondaryFixed', (c) => c.onSecondaryFixed, 0xFF1D192B, 0xFF1D192B),
  (
    'onSecondaryFixedVariant',
    (c) => c.onSecondaryFixedVariant,
    0xFF4A4458,
    0xFF4A4458,
  ),
  ('tertiary', (c) => c.tertiary, 0xFF7D5260, 0xFFEFB8C8),
  ('onTertiary', (c) => c.onTertiary, 0xFFFFFFFF, 0xFF492532),
  ('tertiaryContainer', (c) => c.tertiaryContainer, 0xFFFFD8E4, 0xFF633B48),
  ('onTertiaryContainer', (c) => c.onTertiaryContainer, 0xFF31111D, 0xFFFFD8E4),
  ('tertiaryFixed', (c) => c.tertiaryFixed, 0xFFFFD8E4, 0xFFFFD8E4),
  ('tertiaryFixedDim', (c) => c.tertiaryFixedDim, 0xFFEFB8C8, 0xFFEFB8C8),
  ('onTertiaryFixed', (c) => c.onTertiaryFixed, 0xFF31111D, 0xFF31111D),
  (
    'onTertiaryFixedVariant',
    (c) => c.onTertiaryFixedVariant,
    0xFF633B48,
    0xFF633B48,
  ),
  ('error', (c) => c.error, 0xFFB3261E, 0xFFF2B8B5),
  ('onError', (c) => c.onError, 0xFFFFFFFF, 0xFF601410),
  ('errorContainer', (c) => c.errorContainer, 0xFFF9DEDC, 0xFF8C1D18),
  ('onErrorContainer', (c) => c.onErrorContainer, 0xFF410E0B, 0xFFF9DEDC),
  ('surfaceDim', (c) => c.surfaceDim, 0xFFDED8E1, 0xFF141218),
  ('surfaceBright', (c) => c.surfaceBright, 0xFFFEF7FF, 0xFF3B383E),
  (
    'surfaceContainerLowest',
    (c) => c.surfaceContainerLowest,
    0xFFFFFFFF,
    0xFF0F0D13,
  ),
  ('surfaceContainerLow', (c) => c.surfaceContainerLow, 0xFFF7F2FA, 0xFF1D1B20),
  ('surfaceContainer', (c) => c.surfaceContainer, 0xFFF3EDF7, 0xFF211F26),
  (
    'surfaceContainerHigh',
    (c) => c.surfaceContainerHigh,
    0xFFECE6F0,
    0xFF2B2930,
  ),
  (
    'surfaceContainerHighest',
    (c) => c.surfaceContainerHighest,
    0xFFE6E0E9,
    0xFF36343B,
  ),
  ('outline', (c) => c.outline, 0xFF79747E, 0xFF938F99),
  ('outlineVariant', (c) => c.outlineVariant, 0xFFCAC4D0, 0xFF49454F),
  ('scrim', (c) => c.scrim, 0xFF000000, 0xFF000000),
  ('inverseSurface', (c) => c.inverseSurface, 0xFF322F35, 0xFFE6E0E9),
  ('onInverseSurface', (c) => c.onInverseSurface, 0xFFF5EFF7, 0xFF322F35),
];

/// v1's chart colors, light then dark (`Composables.kt` at 6d57526).
final _vizRoles = <(String, Color Function(VizColors), int, int)>[
  ('series1', (v) => v.series1, 0xFF2A78D6, 0xFF3987E5),
  ('meterTrack', (v) => v.meterTrack, 0xFFCDE2FB, 0xFF184F95),
  ('statusGood', (v) => v.statusGood, 0xFF0CA30C, 0xFF0CA30C),
  ('statusWarning', (v) => v.statusWarning, 0xFFFAB219, 0xFFFAB219),
  ('statusCritical', (v) => v.statusCritical, 0xFFD03B3B, 0xFFD03B3B),
];

/// The roles whose color differs from [expected], as `role: actual, expected` lines.
List<String> _mismatches<T>(
  T colors,
  List<(String, Color Function(T), int, int)> roles,
  Brightness brightness,
) => [
  for (final (name, read, light, dark) in roles)
    if (read(colors) != Color(brightness == Brightness.light ? light : dark))
      '$name: ${read(colors)}, expected '
          '${Color(brightness == Brightness.light ? light : dark)}',
];

ThemeData _themeFor(Brightness brightness) => switch (brightness) {
  Brightness.light => lightTheme,
  Brightness.dark => darkTheme,
};

Brightness _other(Brightness brightness) => switch (brightness) {
  Brightness.light => Brightness.dark,
  Brightness.dark => Brightness.light,
};

/// The system bar style that Flutter's `RenderView` would send to Android: the status bar's half
/// from the style painted at the middle of the status bar, and the navigation bar's half from the
/// style at the middle of the navigation bar.
SystemUiOverlayStyle? _sentStyle(WidgetTester tester) {
  final view = tester.view;
  final size = view.physicalSize;
  final layer = tester.layers.first;
  final top = layer.find<SystemUiOverlayStyle>(
    Offset(size.width / 2, view.padding.top / 2),
  );
  final bottom = layer.find<SystemUiOverlayStyle>(
    Offset(size.width / 2, size.height - 1 - view.padding.bottom / 2),
  );
  if (top == null || bottom == null) {
    return null;
  }
  return SystemUiOverlayStyle(
    statusBarColor: top.statusBarColor,
    statusBarIconBrightness: top.statusBarIconBrightness,
    statusBarBrightness: top.statusBarBrightness,
    systemStatusBarContrastEnforced: top.systemStatusBarContrastEnforced,
    systemNavigationBarColor: bottom.systemNavigationBarColor,
    systemNavigationBarDividerColor: bottom.systemNavigationBarDividerColor,
    systemNavigationBarIconBrightness: bottom.systemNavigationBarIconBrightness,
    systemNavigationBarContrastEnforced:
        bottom.systemNavigationBarContrastEnforced,
  );
}

/// Gives the test view the padding of a phone that draws the app behind its system bars, as
/// Android 10 and later do. The test view has no padding otherwise, like Android 8 and 9.
void _drawBehindSystemBars(WidgetTester tester) {
  const bars = FakeViewPadding(top: 72, bottom: 63);
  tester.view
    ..padding = bars
    ..viewPadding = bars;
  addTearDown(tester.view.resetPadding);
  addTearDown(tester.view.resetViewPadding);
}

void main() {
  Future<void> pumpApp(
    WidgetTester tester,
    SessionLock sessionLock, {
    ThemeMode themeMode = ThemeMode.system,
  }) async {
    await tester.pumpWidget(
      DenariiDolorApp(
        services: AppServices(
          sessionLock: sessionLock,
          moneyParser: FakeMoneyParser(),
        ),
        themeMode: themeMode,
      ),
    );
  }

  for (final brightness in Brightness.values) {
    test('the ${brightness.name} theme has every color role v1 had', () {
      final theme = _themeFor(brightness);

      expect(theme.colorScheme.brightness, brightness);
      expect(_mismatches(theme.colorScheme, _roles, brightness), isEmpty);
      expect(theme.scaffoldBackgroundColor, theme.colorScheme.surface);
    });

    test('the ${brightness.name} theme has v1\'s chart colors', () {
      final vizColors = _themeFor(brightness).extension<VizColors>();

      expect(vizColors, isNotNull);
      if (vizColors != null) {
        expect(_mismatches(vizColors, _vizRoles, brightness), isEmpty);
      }
    });

    testWidgets('the placeholders show v1\'s ${brightness.name} colors, '
        'whatever the device asks for', (tester) async {
      tester.platformDispatcher.platformBrightnessTestValue = _other(
        brightness,
      );
      addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
      _drawBehindSystemBars(tester);
      final expected = _themeFor(brightness);
      final bars = systemBarsStyle(brightness);
      final sessionLock = FakeSessionLock();
      await pumpApp(
        tester,
        sessionLock,
        themeMode: brightness == Brightness.light
            ? ThemeMode.light
            : ThemeMode.dark,
      );

      final signIn = find.byType(SignInPlaceholderScreen);
      final theme = Theme.of(tester.element(signIn));
      expect(theme.colorScheme, expected.colorScheme);
      expect(theme.extension<VizColors>(), expected.extension<VizColors>());
      final background = tester.widget<Material>(
        find.descendant(of: signIn, matching: find.byType(Material)).first,
      );
      expect(background.color, expected.colorScheme.surface);
      final title = tester.renderObject<RenderParagraph>(
        find.text('Denarii Dolor'),
      );
      expect(title.text.style?.color, expected.colorScheme.onSurface);
      expect(_sentStyle(tester), bars);

      sessionLock.unlock();
      await tester.pumpAndSettle();
      expect(find.byType(SignedInPlaceholderScreen), findsOneWidget);
      // The app bar's own Material is the outermost; its icon button has another.
      final appBar = tester.widget<Material>(
        find
            .descendant(
              of: find.byType(AppBar),
              matching: find.byType(Material),
            )
            .first,
      );
      expect(appBar.color, expected.colorScheme.surface);
      expect(_sentStyle(tester), bars);
    });

    testWidgets('where the app isn\'t drawn behind the navigation bar, the '
        '${brightness.name} bar shows v1\'s scrim over the surface', (
      tester,
    ) async {
      final bars = systemBarsStyle(brightness);
      await pumpApp(
        tester,
        FakeSessionLock(),
        themeMode: brightness == Brightness.light
            ? ThemeMode.light
            : ThemeMode.dark,
      );

      final sent = _sentStyle(tester);
      expect(
        sent,
        withOpaqueNavigationBar(
          bars,
          _themeFor(brightness).colorScheme.surface,
        ),
      );
      // 0xE6FFFFFF over 0xFFFCFCFB, and 0x801B1B1B over 0xFF1A1A19.
      expect(
        sent?.systemNavigationBarColor?.toARGB32(),
        brightness == Brightness.light ? 0xFFFFFFFF : 0xFF1B1B1A,
      );
      expect(sent?.statusBarColor, bars.statusBarColor);
      expect(sent?.statusBarIconBrightness, bars.statusBarIconBrightness);
    });
  }

  test('a style with no navigation bar color stays as it is', () {
    const style = SystemUiOverlayStyle(statusBarColor: Color(0x00000000));

    expect(withOpaqueNavigationBar(style, const Color(0xFF1A1A19)), style);
  });

  testWidgets('the system mode follows the device as it changes', (
    tester,
  ) async {
    addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
    tester.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
    _drawBehindSystemBars(tester);
    await pumpApp(tester, FakeSessionLock());

    ColorScheme shown() =>
        Theme.of(tester.element(find.byType(SignInPlaceholderScreen)))
            .colorScheme;
    expect(shown(), darkTheme.colorScheme);
    expect(_sentStyle(tester), systemBarsStyle(Brightness.dark));

    tester.platformDispatcher.platformBrightnessTestValue = Brightness.light;
    await tester.pumpAndSettle();
    expect(shown(), lightTheme.colorScheme);
    expect(_sentStyle(tester), systemBarsStyle(Brightness.light));
  });

  group('VizColors', () {
    const a = VizColors(
      series1: Color(0xFF000000),
      meterTrack: Color(0xFF000000),
      statusGood: Color(0xFF000000),
      statusWarning: Color(0xFF000000),
      statusCritical: Color(0xFF000000),
    );
    const b = VizColors(
      series1: Color(0xFFFFFFFF),
      meterTrack: Color(0xFF0000FF),
      statusGood: Color(0xFF00FF00),
      statusWarning: Color(0xFFFF0000),
      statusCritical: Color(0xFFFFFF00),
    );

    test('blends each color toward the other theme\'s', () {
      expect(a.lerp(b, 0), a);
      expect(a.lerp(b, 1), b);
      expect(a.lerp(null, 0.5), a);
      final halfway = a.lerp(b, 0.5);
      expect(halfway.series1, Color.lerp(a.series1, b.series1, 0.5));
      expect(halfway.meterTrack, Color.lerp(a.meterTrack, b.meterTrack, 0.5));
      expect(halfway.statusGood, Color.lerp(a.statusGood, b.statusGood, 0.5));
      expect(
        halfway.statusWarning,
        Color.lerp(a.statusWarning, b.statusWarning, 0.5),
      );
      expect(
        halfway.statusCritical,
        Color.lerp(a.statusCritical, b.statusCritical, 0.5),
      );
    });

    test('copyWith replaces only the colors it is given', () {
      final copy = a.copyWith(statusWarning: b.statusWarning);

      expect(copy.statusWarning, b.statusWarning);
      expect(copy, isNot(a));
      expect(copy.copyWith(statusWarning: a.statusWarning), a);
      expect(a.copyWith(), a);
      expect(a.copyWith().hashCode, a.hashCode);
    });
  });
}
