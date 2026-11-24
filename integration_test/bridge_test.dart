/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/app.dart';
import 'package:denarii_dolor/app/app_scope.dart';
import 'package:denarii_dolor/app/session_lock.dart';
import 'package:denarii_dolor/l10n/app_localizations.dart';
import 'package:denarii_dolor/l10n/locale_resolution.dart';
import 'package:denarii_dolor/l10n/money_format.dart';
import 'package:denarii_dolor/platform/money_parser.dart';
import 'package:denarii_dolor/platform/rust/frb_generated.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:material_ui/material_ui.dart';

/// Checks on a device that Dart reaches the Rust library that the build hook packaged (M1.2).
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  setUpAll(RustLib.init);

  const english = NumberSymbols(
    decimalSeparator: '.',
    currencyMarks: ['\$', '\$', 'USD'],
  );

  test('the Rust core parses an amount into cents', () async {
    const parser = RustMoneyParser();

    expect(
      await parser.parseCents('\$1,234.56', english),
      const AmountInCents(123456),
    );
    expect(
      await parser.parseCents('-25.50', english),
      const AmountInCents(-2550),
    );
  });

  test('the Rust core names each rejection', () async {
    const parser = RustMoneyParser();

    expect(
      await parser.parseCents('', english),
      const RejectedAmount(AmountError.empty),
    );
    expect(
      await parser.parseCents('abc', english),
      const RejectedAmount(AmountError.malformed),
    );
    expect(
      await parser.parseCents('1.005', english),
      const RejectedAmount(AmountError.tooManyDecimals),
    );
    expect(
      await parser.parseCents('99999999999', english),
      const RejectedAmount(AmountError.tooLarge),
    );
    expect(
      await parser.parseCents(
        '12',
        const NumberSymbols(decimalSeparator: '', currencyMarks: []),
      ),
      const RejectedAmount(AmountError.invalidDecimalSeparator),
    );
  });

  testWidgets('the placeholder screen shows the cents Rust returned', (
    tester,
  ) async {
    await tester.pumpWidget(
      DenariiDolorApp(
        services: AppServices(
          sessionLock: LockedSessionLock(),
          moneyParser: const RustMoneyParser(),
        ),
      ),
    );

    await tester.enterText(find.byType(TextField), '1,234.56');
    // The answer comes back from a Rust worker thread, so wait for it rather than for a frame.
    await _pumpUntilFound(tester, find.text('123456 cents'));
    // Dart writes the same cents as money (M1.6), in the language the device's list picked.
    final locale = resolveAppLocale(
      tester.platformDispatcher.locales,
      AppLocalizations.supportedLocales,
    );
    expect(
      find.text(formatMoney(123456, currencyCode: 'USD', locale: locale)),
      findsOneWidget,
    );

    await tester.enterText(find.byType(TextField), '12,345,67');
    await _pumpUntilFound(tester, find.text('Rejected: malformed'));
  });

  // v1's MoneyCompatibilityTest.kt read shown amounts back in these locales and currencies.
  test('the Rust core reads back the amounts Dart writes', () async {
    const parser = RustMoneyParser();
    const amounts = [0, 1, 5, 99, 100, 1250, 123450, 123456789, 999999999999];
    const cases = {
      'en-US': 'USD', 'en-IN': 'INR', 'zh-CN': 'CNY', 'hi-IN': 'INR', //
      'es-ES': 'EUR', 'es-MX': 'MXN', 'ar-SA': 'SAR', 'ar-EG': 'EGP',
      'ar-MA': 'MAD', 'fr-FR': 'EUR', 'fr-CA': 'CAD', 'fr-CH': 'CHF',
      'de-DE': 'EUR', 'de-CH': 'CHF', 'pt-BR': 'BRL', 'pt-PT': 'EUR',
    };

    for (final MapEntry(key: tag, value: regionCurrency) in cases.entries) {
      final [language, region] = tag.split('-');
      final locale = Locale(language, region);
      for (final currencyCode in {regionCurrency, 'USD'}) {
        final symbols = moneyParsingSymbols(currencyCode, locale);
        for (final cents in [
          for (final amount in amounts) ...[amount, -amount],
        ]) {
          final text = formatMoney(
            cents,
            currencyCode: currencyCode,
            locale: locale,
          );
          // The core, like v1's parser, reads only groups of three digits, so Indian grouping of a
          // lakh (1,00,000) or more doesn't read back. v1 on Android grouped the same way.
          final indianGrouping = region == 'IN' && cents.abs() >= 10000000;
          expect(
            await parser.parseCents(text, symbols),
            indianGrouping
                ? const RejectedAmount(AmountError.malformed)
                : AmountInCents(cents),
            reason: '$tag $currencyCode $text',
          );
        }
      }
    }
  });
}

/// Pumps frames until [finder] finds a widget, failing after about 10 seconds.
Future<void> _pumpUntilFound(WidgetTester tester, Finder finder) async {
  for (var frame = 0; frame < 100; frame++) {
    if (finder.evaluate().isNotEmpty) {
      return;
    }
    await tester.pump(const Duration(milliseconds: 100));
  }
  expect(finder, findsOneWidget);
}
