/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/app.dart';
import 'package:denarii_dolor/app/app_scope.dart';
import 'package:denarii_dolor/features/auth/ui/sign_in_placeholder_screen.dart';
import 'package:denarii_dolor/features/session/ui/signed_in_placeholder_screen.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import '../fakes/fake_money_parser.dart';
import '../fakes/fake_session_lock.dart';

void main() {
  /// Starts the app on a phone whose languages are [locales], in that order.
  Future<FakeSessionLock> pumpApp(
    WidgetTester tester,
    List<Locale> locales,
  ) async {
    tester.platformDispatcher.localesTestValue = locales;
    addTearDown(tester.platformDispatcher.clearLocalesTestValue);
    final sessionLock = FakeSessionLock();
    await tester.pumpWidget(
      DenariiDolorApp(
        services: AppServices(
          sessionLock: sessionLock,
          moneyParser: FakeMoneyParser(),
        ),
      ),
    );
    await tester.pumpAndSettle();
    return sessionLock;
  }

  testWidgets('Arabic lays out right to left, in Arabic', (tester) async {
    final sessionLock = await pumpApp(tester, const [Locale('ar', 'EG')]);

    final signIn = tester.element(find.byType(SignInPlaceholderScreen));
    expect(Localizations.localeOf(signIn), const Locale('ar', 'EG'));
    expect(Directionality.of(signIn), TextDirection.rtl);
    expect(find.widgetWithText(TextField, 'المبلغ'), findsOneWidget);

    sessionLock.unlock();
    await tester.pumpAndSettle();
    final signedIn = tester.element(find.byType(SignedInPlaceholderScreen));
    expect(Directionality.of(signedIn), TextDirection.rtl);
    expect(find.byTooltip('تسجيل الخروج'), findsOneWidget);
    // In right-to-left text, the app bar's actions sit on the left.
    expect(
      tester.getCenter(find.byType(IconButton)).dx,
      lessThan(tester.getCenter(find.text('Denarii Dolor')).dx),
    );
  });

  testWidgets('a phone language with no pack gives way to a later one', (
    tester,
  ) async {
    await pumpApp(tester, const [Locale('de', 'DE'), Locale('es', 'ES')]);

    final signIn = tester.element(find.byType(SignInPlaceholderScreen));
    expect(Localizations.localeOf(signIn), const Locale('es', 'ES'));
    expect(Directionality.of(signIn), TextDirection.ltr);
    expect(find.widgetWithText(TextField, 'Monto'), findsOneWidget);
  });

  testWidgets('Traditional Chinese shows English, as in v1', (tester) async {
    await pumpApp(tester, const [
      Locale.fromSubtags(
        languageCode: 'zh',
        scriptCode: 'Hant',
        countryCode: 'TW',
      ),
    ]);

    expect(find.widgetWithText(TextField, 'Amount'), findsOneWidget);
  });

  testWidgets('Simplified Chinese outside China shows Chinese', (tester) async {
    await pumpApp(tester, const [
      Locale.fromSubtags(
        languageCode: 'zh',
        scriptCode: 'Hans',
        countryCode: 'SG',
      ),
    ]);

    expect(find.widgetWithText(TextField, '金额'), findsOneWidget);
  });

  testWidgets('the window title is the app name', (tester) async {
    await pumpApp(tester, const [Locale('fr', 'FR')]);

    expect(tester.widget<Title>(find.byType(Title)).title, 'Denarii Dolor');
  });
}
