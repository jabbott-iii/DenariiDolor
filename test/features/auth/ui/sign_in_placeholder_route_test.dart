/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/app.dart';
import 'package:denarii_dolor/app/app_scope.dart';
import 'package:denarii_dolor/app/session_lock.dart';
import 'package:denarii_dolor/platform/money_parser.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import '../../../fakes/fake_money_parser.dart';

void main() {
  late FakeMoneyParser moneyParser;

  Future<void> pumpApp(WidgetTester tester) async {
    moneyParser = FakeMoneyParser();
    await tester.pumpWidget(
      DenariiDolorApp(
        services: AppServices(
          sessionLock: LockedSessionLock(),
          moneyParser: moneyParser,
        ),
      ),
    );
  }

  testWidgets('shows nothing under the field before anything is typed', (
    tester,
  ) async {
    await pumpApp(tester);

    expect(find.widgetWithText(TextField, 'Amount'), findsOneWidget);
    expect(find.textContaining('cents'), findsNothing);
    expect(find.textContaining('Rejected'), findsNothing);
  });

  testWidgets('shows the cents the parser returned for the typed amount', (
    tester,
  ) async {
    await pumpApp(tester);
    moneyParser.answers['1,234.56'] = const AmountInCents(123456);

    await tester.enterText(find.byType(TextField), '1,234.56');
    await tester.pump();

    expect(find.text('123456 cents'), findsOneWidget);
    expect(moneyParser.calls.map((call) => call.$1), ['1,234.56']);
  });

  testWidgets('shows why the parser rejected the typed amount', (tester) async {
    await pumpApp(tester);
    moneyParser.answers['1.005'] = const RejectedAmount(
      AmountError.tooManyDecimals,
    );

    await tester.enterText(find.byType(TextField), '1.005');
    await tester.pump();

    expect(find.text('Rejected: tooManyDecimals'), findsOneWidget);
  });

  testWidgets('announces the result as a live region', (tester) async {
    final handle = tester.ensureSemantics();
    await pumpApp(tester);
    moneyParser.answers['12'] = const AmountInCents(1200);

    await tester.enterText(find.byType(TextField), '12');
    await tester.pump();

    expect(
      tester.getSemantics(find.text('1200 cents')),
      matchesSemantics(label: '1200 cents', isLiveRegion: true),
    );
    handle.dispose();
  });
}
