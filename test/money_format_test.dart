/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'dart:math';

import 'package:denarii_dolor/l10n/money_format.dart';
import 'package:denarii_dolor/platform/money_parser.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';

/// Ports v1's `MoneySnapshotTest.kt` at 6d57526 (ADR-0016). v1's tables are copied unchanged in
/// [_v1Money] and [_v1Months]; [_differences] and [_monthDifferences] list where the app writes
/// other text, and why. Invisible characters are escaped: U+00A0 and U+202F are no-break spaces,
/// U+200E and U+200F direction marks, and U+061C the Arabic letter mark.
///
/// v1's tables hold what JDK 17 wrote. On phones, v1 wrote what each Android release's ICU data
/// gave, which differed from these tables in 19 rows on API 26 and 11 on API 36. Every difference
/// listed below is also what Android 16 (API 36) wrote, except where its comment says otherwise.
void main() {
  group("v1's snapshot", () {
    test('money is written as each language writes it', () {
      for (final MapEntry(key: row, value: v1Texts) in _v1Money.entries) {
        final [tag, currencyCode] = row.split(' ');
        final locale = _locale(tag);
        String money(int cents, {bool showPlus = false}) => formatMoney(
          cents,
          currencyCode: currencyCode,
          locale: locale,
          showPlus: showPlus,
        );
        final actual = [
          for (final cents in _amounts) money(cents),
          money(_plusAmount, showPlus: true),
          // v1's formatSignedAmount: an expense negative, an income with a plus, a transfer as is.
          money(-_signedAmount),
          money(_signedAmount, showPlus: true),
          money(_signedAmount),
        ];
        final expected = [
          for (final (index, text) in v1Texts.indexed)
            _differences[row]?[index] ?? text,
        ];

        expect(actual, expected, reason: row);
      }
    });

    test('month labels are written as each language writes them', () {
      for (final MapEntry(key: tag, value: v1Labels) in _v1Months.entries) {
        final locale = _locale(tag);
        final actual = [
          formatMonthYear(year: 2026, month: 5, locale: locale),
          formatMonthYear(year: 2026, month: 9, locale: locale),
        ];

        expect(actual, _monthDifferences[tag] ?? v1Labels, reason: tag);
      }
    });

    test('every listed difference names a row and differs from it', () {
      for (final MapEntry(key: row, value: texts) in _differences.entries) {
        final v1Texts = _v1Money[row];
        expect(v1Texts, isNotNull, reason: row);
        for (final MapEntry(key: index, value: text) in texts.entries) {
          expect(text, isNot(v1Texts?[index]), reason: '$row [$index]');
        }
      }
      for (final MapEntry(key: tag, value: labels)
          in _monthDifferences.entries) {
        expect(labels, isNot(_v1Months[tag]), reason: tag);
      }
    });
  });

  group('exact amounts', () {
    test('every int is written exactly', () {
      final random = Random(_seed);
      final cents = [
        ...[for (var value = -1000; value <= 1000; value++) value],
        // MicroMoney holds up to 922337203685477 cents; past it the whole units are written alone.
        for (final edge in [922337203685477, 922337203685478]) ...[edge, -edge],
        9223372036854775807,
        -9223372036854775808,
        for (var i = 0; i < 10000; i++)
          (random.nextInt(1 << 32) << 32) | random.nextInt(1 << 32),
        for (var i = 0; i < 10000; i++) random.nextInt(20000000) - 10000000,
      ];
      const usEnglish = Locale('en', 'US');

      for (final value in cents) {
        expect(
          formatMoney(value, currencyCode: 'USD', locale: usEnglish),
          _usDollars(value),
          reason: '$value',
        );
      }
    });

    test('the largest amounts keep their digits and marks', () {
      const saudi = Locale('ar', 'SA');
      const swissFrench = Locale('fr', 'CH');

      expect(
        formatMoney(9223372036854775807, currencyCode: 'SAR', locale: saudi),
        '\u{200F}٩٢٬٢٣٣٬٧٢٠٬٣٦٨٬٥٤٧٬٧٥٨٫٠٧\u{A0}ر.س.\u{200F}',
      );
      expect(
        formatMoney(-9223372036854775808, currencyCode: 'SAR', locale: saudi),
        '\u{61C}-\u{200F}٩٢٬٢٣٣٬٧٢٠٬٣٦٨٬٥٤٧٬٧٥٨٫٠٨\u{A0}ر.س.\u{200F}',
      );
      expect(
        formatMoney(
          9223372036854775807,
          currencyCode: 'CHF',
          locale: swissFrench,
        ),
        '92\u{202F}233\u{202F}720\u{202F}368\u{202F}547\u{202F}758.07\u{A0}CHF',
      );
      expect(
        formatMoney(
          -9223372036854775808,
          currencyCode: 'CHF',
          locale: swissFrench,
        ),
        '-92\u{202F}233\u{202F}720\u{202F}368\u{202F}547\u{202F}758.08\u{A0}CHF',
      );
    });

    test('only a positive amount gets a plus sign', () {
      const usEnglish = Locale('en', 'US');

      expect(
        formatMoney(0, currencyCode: 'USD', locale: usEnglish, showPlus: true),
        '\$0.00',
      );
      expect(
        formatMoney(-5, currencyCode: 'USD', locale: usEnglish, showPlus: true),
        '-\$0.05',
      );
      expect(
        formatMoney(1, currencyCode: 'USD', locale: usEnglish, showPlus: true),
        '+\$0.01',
      );
    });
  });

  group('currency symbols', () {
    test("each locale writes the currency CLDR's way", () {
      final cases = {
        ('en-US', 'USD'): '\$',
        ('en-GB', 'USD'): 'US\$',
        ('en-AU', 'USD'): 'USD',
        ('en-AU', 'AUD'): '\$',
        ('es-MX', 'USD'): 'USD',
        ('es-MX', 'MXN'): '\$',
        ('fr-FR', 'USD'): '\$US',
        ('fr-CA', 'USD'): '\$\u{A0}US',
        ('zh-CN', 'CNY'): '¥',
        ('ar-SA', 'SAR'): 'ر.س.\u{200F}',
        ('hi-IN', 'INR'): '₹',
        ('pt-BR', 'BRL'): 'R\$',
      };

      for (final MapEntry(key: (tag, code), value: symbol) in cases.entries) {
        expect(
          currencySymbol(code, _locale(tag)),
          symbol,
          reason: '$tag $code',
        );
      }
    });

    test("a region CLDR doesn't list reads its language's symbols", () {
      expect(currencySymbol('USD', const Locale('es', 'ZZ')), 'US\$');
      expect(currencySymbol('USD', const Locale('es')), 'US\$');
    });

    test('a currency or a language the table lacks is written as its code', () {
      expect(currencySymbol('XYZ', const Locale('en', 'US')), 'XYZ');
      expect(currencySymbol('USD', const Locale('de', 'DE')), 'USD');
    });

    test('a Chinese script keeps the lookup in that script', () {
      const simplifiedInTaiwan = Locale.fromSubtags(
        languageCode: 'zh',
        scriptCode: 'Hans',
        countryCode: 'TW',
      );
      const traditionalInTaiwan = Locale.fromSubtags(
        languageCode: 'zh',
        scriptCode: 'Hant',
        countryCode: 'TW',
      );
      const simplifiedInSingapore = Locale.fromSubtags(
        languageCode: 'zh',
        scriptCode: 'Hans',
        countryCode: 'SG',
      );

      expect(currencySymbol('TWD', simplifiedInTaiwan), 'NT\$');
      expect(currencySymbol('TWD', traditionalInTaiwan), '\$');
      expect(currencySymbol('SGD', simplifiedInSingapore), '\$');
    });

    test("a language's only script is ignored", () {
      const portugueseInBrazil = Locale.fromSubtags(
        languageCode: 'pt',
        scriptCode: 'Latn',
        countryCode: 'BR',
      );

      expect(currencySymbol('BRL', portugueseInBrazil), 'R\$');
    });
  });

  group('Arabic regions', () {
    // Each set is what Android 16's ICU wrote on the API 36 emulator on 2026-11-23.
    const arabicIndicDigits = [
      'BH', 'DJ', 'EG', 'ER', 'IL', 'IQ', 'JO', 'KM', 'KW', 'LB', 'MR', //
      'OM', 'PS', 'QA', 'SA', 'SD', 'SO', 'SS', 'SY', 'TD', 'YE',
    ];
    const decimalComma = ['DZ', 'LY', 'MA', 'TN'];
    const latinDigits = ['AE', 'EH', null];

    String money(String? region) =>
        formatMoney(123450, currencyCode: 'USD', locale: Locale('ar', region));

    test('most write Arabic-Indic digits', () {
      for (final region in arabicIndicDigits) {
        expect(money(region), '\u{200F}١٬٢٣٤٫٥٠\u{A0}US\$', reason: region);
      }
    });

    test('Algeria, Libya, Morocco, and Tunisia write a decimal comma', () {
      for (final region in decimalComma) {
        expect(money(region), '\u{200F}1.234,50\u{A0}US\$', reason: region);
      }
    });

    test(
      'plain Arabic, the Emirates, and Western Sahara write Latin digits',
      () {
        for (final region in latinDigits) {
          expect(money(region), '\u{200F}1,234.50\u{A0}US\$', reason: region);
        }
      },
    );
  });

  group('symbols for parsing', () {
    test("each locale's decimal separator and the currency's marks", () {
      final cases = {
        ('en-US', 'USD'): const NumberSymbols(
          decimalSeparator: '.',
          currencyMarks: ['\$', 'USD'],
        ),
        ('fr-FR', 'EUR'): const NumberSymbols(
          decimalSeparator: ',',
          currencyMarks: ['€', 'EUR'],
        ),
        ('ar-SA', 'SAR'): const NumberSymbols(
          decimalSeparator: '٫',
          currencyMarks: ['ر.س.\u{200F}', 'SAR'],
        ),
        ('ar-MA', 'MAD'): const NumberSymbols(
          decimalSeparator: ',',
          currencyMarks: ['د.م.\u{200F}', 'MAD'],
        ),
        // Money writes `.` in Swiss French, but v1 read typed amounts with the separator of
        // other numbers, which is `,`. A single `.` before two digits is a decimal mark anyway.
        ('fr-CH', 'CHF'): const NumberSymbols(
          decimalSeparator: ',',
          currencyMarks: ['CHF', 'CHF'],
        ),
      };

      for (final MapEntry(key: (tag, code), value: symbols) in cases.entries) {
        final actual = moneyParsingSymbols(code, _locale(tag));
        // The generated NumberSymbols compares its list by identity, so compare the fields.
        expect(
          actual.decimalSeparator,
          symbols.decimalSeparator,
          reason: '$tag $code',
        );
        expect(
          actual.currencyMarks,
          symbols.currencyMarks,
          reason: '$tag $code',
        );
      }
    });
  });

  group('month labels', () {
    test('the year has four digits', () {
      expect(
        formatMonthYear(year: 999, month: 5, locale: const Locale('en', 'US')),
        'May 0999',
      );
    });

    test('a language intl lacks gets English month names', () {
      expect(
        formatMonthYear(year: 2026, month: 9, locale: const Locale('xx')),
        'September 2026',
      );
    });

    test('a month or year out of range is a programming error', () {
      const english = Locale('en');

      expect(
        () => formatMonthYear(year: 2026, month: 0, locale: english),
        throwsRangeError,
      );
      expect(
        () => formatMonthYear(year: 2026, month: 13, locale: english),
        throwsRangeError,
      );
      expect(
        () => formatMonthYear(year: 0, month: 1, locale: english),
        throwsRangeError,
      );
      expect(
        () => formatMonthYear(year: 10000, month: 1, locale: english),
        throwsRangeError,
      );
    });
  });
}

/// v1's seed for generated amounts (`MoneyCompatibilityTest.kt`).
const int _seed = 20261122;

/// v1's `AMOUNTS`, `PLUS_AMOUNT`, and `SIGNED_AMOUNT`, in cents.
const List<int> _amounts = [5, -100, 123450, -123456789];
const int _plusAmount = 1200;
const int _signedAmount = 120000;

Locale _locale(String tag) {
  final [language, region] = tag.split('-');
  return Locale(language, region);
}

/// [cents] in US dollars as en-US writes them, without `intl`, to check it against.
String _usDollars(int cents) {
  final magnitude = BigInt.from(cents).abs();
  final hundred = BigInt.from(100);
  final units = (magnitude ~/ hundred).toString();
  final fraction = (magnitude % hundred).toString().padLeft(2, '0');
  final grouped = units.replaceAllMapped(
    RegExp(r'\B(?=(\d{3})+(?!\d))'),
    (_) => ',',
  );
  return '${cents < 0 ? '-' : ''}\$$grouped.$fraction';
}

Map<int, String> _wholeRow(List<String> texts) => texts.asMap();

/// Where the app writes other text than [_v1Money], by row and index.
final Map<String, Map<int, String>> _differences = {
  // `intl` groups lakhs and crores (12,34,567.89), as Android 16 does. JDK 17 grouped by thousands.
  'en-IN INR': {3: '-₹12,34,567.89'},
  'en-IN USD': {3: '-\$12,34,567.89'},
  'hi-IN INR': {3: '-₹12,34,567.89'},
  'hi-IN USD': {3: '-\$12,34,567.89'},
  // CLDR 48 starts Arabic amounts with a right-to-left mark, as Android 16 does. Before a minus,
  // the app keeps the Arabic letter mark that v1 wrote; Android 16 drops it, because its
  // `java.text` minus sign is a single character.
  'ar-SA SAR': _wholeRow([
    '\u{200F}٠٫٠٥\u{A0}ر.س.\u{200F}',
    '\u{61C}-\u{200F}١٫٠٠\u{A0}ر.س.\u{200F}',
    '\u{200F}١٬٢٣٤٫٥٠\u{A0}ر.س.\u{200F}',
    '\u{61C}-\u{200F}١٬٢٣٤٬٥٦٧٫٨٩\u{A0}ر.س.\u{200F}',
    '+\u{200F}١٢٫٠٠\u{A0}ر.س.\u{200F}',
    '\u{61C}-\u{200F}١٬٢٠٠٫٠٠\u{A0}ر.س.\u{200F}',
    '+\u{200F}١٬٢٠٠٫٠٠\u{A0}ر.س.\u{200F}',
    '\u{200F}١٬٢٠٠٫٠٠\u{A0}ر.س.\u{200F}',
  ]),
  'ar-SA USD': _wholeRow([
    '\u{200F}٠٫٠٥\u{A0}US\$',
    '\u{61C}-\u{200F}١٫٠٠\u{A0}US\$',
    '\u{200F}١٬٢٣٤٫٥٠\u{A0}US\$',
    '\u{61C}-\u{200F}١٬٢٣٤٬٥٦٧٫٨٩\u{A0}US\$',
    '+\u{200F}١٢٫٠٠\u{A0}US\$',
    '\u{61C}-\u{200F}١٬٢٠٠٫٠٠\u{A0}US\$',
    '+\u{200F}١٬٢٠٠٫٠٠\u{A0}US\$',
    '\u{200F}١٬٢٠٠٫٠٠\u{A0}US\$',
  ]),
  'ar-EG EGP': _wholeRow([
    '\u{200F}٠٫٠٥\u{A0}ج.م.\u{200F}',
    '\u{61C}-\u{200F}١٫٠٠\u{A0}ج.م.\u{200F}',
    '\u{200F}١٬٢٣٤٫٥٠\u{A0}ج.م.\u{200F}',
    '\u{61C}-\u{200F}١٬٢٣٤٬٥٦٧٫٨٩\u{A0}ج.م.\u{200F}',
    '+\u{200F}١٢٫٠٠\u{A0}ج.م.\u{200F}',
    '\u{61C}-\u{200F}١٬٢٠٠٫٠٠\u{A0}ج.م.\u{200F}',
    '+\u{200F}١٬٢٠٠٫٠٠\u{A0}ج.م.\u{200F}',
    '\u{200F}١٬٢٠٠٫٠٠\u{A0}ج.م.\u{200F}',
  ]),
  'ar-EG USD': _wholeRow([
    '\u{200F}٠٫٠٥\u{A0}US\$',
    '\u{61C}-\u{200F}١٫٠٠\u{A0}US\$',
    '\u{200F}١٬٢٣٤٫٥٠\u{A0}US\$',
    '\u{61C}-\u{200F}١٬٢٣٤٬٥٦٧٫٨٩\u{A0}US\$',
    '+\u{200F}١٢٫٠٠\u{A0}US\$',
    '\u{61C}-\u{200F}١٬٢٠٠٫٠٠\u{A0}US\$',
    '+\u{200F}١٬٢٠٠٫٠٠\u{A0}US\$',
    '\u{200F}١٬٢٠٠٫٠٠\u{A0}US\$',
  ]),
  // Morocco writes the symbol after the number, behind a right-to-left mark, as Android 16 does.
  // JDK 17 wrote it first.
  'ar-MA MAD': _wholeRow([
    '\u{200F}0,05\u{A0}د.م.\u{200F}',
    '\u{200F}-1,00\u{A0}د.م.\u{200F}',
    '\u{200F}1.234,50\u{A0}د.م.\u{200F}',
    '\u{200F}-1.234.567,89\u{A0}د.م.\u{200F}',
    '+\u{200F}12,00\u{A0}د.م.\u{200F}',
    '\u{200F}-1.200,00\u{A0}د.م.\u{200F}',
    '+\u{200F}1.200,00\u{A0}د.م.\u{200F}',
    '\u{200F}1.200,00\u{A0}د.م.\u{200F}',
  ]),
  'ar-MA USD': _wholeRow([
    '\u{200F}0,05\u{A0}US\$',
    '\u{200F}-1,00\u{A0}US\$',
    '\u{200F}1.234,50\u{A0}US\$',
    '\u{200F}-1.234.567,89\u{A0}US\$',
    '+\u{200F}12,00\u{A0}US\$',
    '\u{200F}-1.200,00\u{A0}US\$',
    '+\u{200F}1.200,00\u{A0}US\$',
    '\u{200F}1.200,00\u{A0}US\$',
  ]),
  // CLDR 47 writes Canadian dollars as `$` in Canadian French, as Android 16 does. JDK 17's
  // CLDR 39 wrote `$ CA`.
  'fr-CA CAD': _wholeRow([
    '0,05\u{A0}\$',
    '-1,00\u{A0}\$',
    '1\u{A0}234,50\u{A0}\$',
    '-1\u{A0}234\u{A0}567,89\u{A0}\$',
    '+12,00\u{A0}\$',
    '-1\u{A0}200,00\u{A0}\$',
    '+1\u{A0}200,00\u{A0}\$',
    '1\u{A0}200,00\u{A0}\$',
  ]),
};

/// Where the app writes other month labels than [_v1Months].
const Map<String, List<String>> _monthDifferences = {
  // `intl` has no Moroccan month names, so Morocco gets standard Arabic ones. JDK 17 and Android 16
  // wrote ماي and شتنبر.
  'ar-MA': ['مايو 2026', 'سبتمبر 2026'],
};

/// v1's `MONEY`: for each language and currency, [_amounts], then [_plusAmount] with a plus sign,
/// then [_signedAmount] as an expense, an income, and a transfer.
const Map<String, List<String>> _v1Money = {
  'en-US USD': [
    '\$0.05',
    '-\$1.00',
    '\$1,234.50',
    '-\$1,234,567.89',
    '+\$12.00',
    '-\$1,200.00',
    '+\$1,200.00',
    '\$1,200.00',
  ],
  'en-GB GBP': [
    '£0.05',
    '-£1.00',
    '£1,234.50',
    '-£1,234,567.89',
    '+£12.00',
    '-£1,200.00',
    '+£1,200.00',
    '£1,200.00',
  ],
  'en-GB USD': [
    'US\$0.05',
    '-US\$1.00',
    'US\$1,234.50',
    '-US\$1,234,567.89',
    '+US\$12.00',
    '-US\$1,200.00',
    '+US\$1,200.00',
    'US\$1,200.00',
  ],
  'en-IN INR': [
    '₹0.05',
    '-₹1.00',
    '₹1,234.50',
    '-₹1,234,567.89',
    '+₹12.00',
    '-₹1,200.00',
    '+₹1,200.00',
    '₹1,200.00',
  ],
  'en-IN USD': [
    '\$0.05',
    '-\$1.00',
    '\$1,234.50',
    '-\$1,234,567.89',
    '+\$12.00',
    '-\$1,200.00',
    '+\$1,200.00',
    '\$1,200.00',
  ],
  'zh-CN CNY': [
    '¥0.05',
    '-¥1.00',
    '¥1,234.50',
    '-¥1,234,567.89',
    '+¥12.00',
    '-¥1,200.00',
    '+¥1,200.00',
    '¥1,200.00',
  ],
  'zh-CN USD': [
    'US\$0.05',
    '-US\$1.00',
    'US\$1,234.50',
    '-US\$1,234,567.89',
    '+US\$12.00',
    '-US\$1,200.00',
    '+US\$1,200.00',
    'US\$1,200.00',
  ],
  'hi-IN INR': [
    '₹0.05',
    '-₹1.00',
    '₹1,234.50',
    '-₹1,234,567.89',
    '+₹12.00',
    '-₹1,200.00',
    '+₹1,200.00',
    '₹1,200.00',
  ],
  'hi-IN USD': [
    '\$0.05',
    '-\$1.00',
    '\$1,234.50',
    '-\$1,234,567.89',
    '+\$12.00',
    '-\$1,200.00',
    '+\$1,200.00',
    '\$1,200.00',
  ],
  'es-ES EUR': [
    '0,05\u{A0}€',
    '-1,00\u{A0}€',
    '1.234,50\u{A0}€',
    '-1.234.567,89\u{A0}€',
    '+12,00\u{A0}€',
    '-1.200,00\u{A0}€',
    '+1.200,00\u{A0}€',
    '1.200,00\u{A0}€',
  ],
  'es-ES USD': [
    '0,05\u{A0}US\$',
    '-1,00\u{A0}US\$',
    '1.234,50\u{A0}US\$',
    '-1.234.567,89\u{A0}US\$',
    '+12,00\u{A0}US\$',
    '-1.200,00\u{A0}US\$',
    '+1.200,00\u{A0}US\$',
    '1.200,00\u{A0}US\$',
  ],
  'es-MX MXN': [
    '\$0.05',
    '-\$1.00',
    '\$1,234.50',
    '-\$1,234,567.89',
    '+\$12.00',
    '-\$1,200.00',
    '+\$1,200.00',
    '\$1,200.00',
  ],
  'es-MX USD': [
    'USD0.05',
    '-USD1.00',
    'USD1,234.50',
    '-USD1,234,567.89',
    '+USD12.00',
    '-USD1,200.00',
    '+USD1,200.00',
    'USD1,200.00',
  ],
  'ar-SA SAR': [
    '٠٫٠٥\u{A0}ر.س.\u{200F}',
    '\u{61C}-١٫٠٠\u{A0}ر.س.\u{200F}',
    '١٬٢٣٤٫٥٠\u{A0}ر.س.\u{200F}',
    '\u{61C}-١٬٢٣٤٬٥٦٧٫٨٩\u{A0}ر.س.\u{200F}',
    '+١٢٫٠٠\u{A0}ر.س.\u{200F}',
    '\u{61C}-١٬٢٠٠٫٠٠\u{A0}ر.س.\u{200F}',
    '+١٬٢٠٠٫٠٠\u{A0}ر.س.\u{200F}',
    '١٬٢٠٠٫٠٠\u{A0}ر.س.\u{200F}',
  ],
  'ar-SA USD': [
    '٠٫٠٥\u{A0}US\$',
    '\u{61C}-١٫٠٠\u{A0}US\$',
    '١٬٢٣٤٫٥٠\u{A0}US\$',
    '\u{61C}-١٬٢٣٤٬٥٦٧٫٨٩\u{A0}US\$',
    '+١٢٫٠٠\u{A0}US\$',
    '\u{61C}-١٬٢٠٠٫٠٠\u{A0}US\$',
    '+١٬٢٠٠٫٠٠\u{A0}US\$',
    '١٬٢٠٠٫٠٠\u{A0}US\$',
  ],
  'ar-EG EGP': [
    '٠٫٠٥\u{A0}ج.م.\u{200F}',
    '\u{61C}-١٫٠٠\u{A0}ج.م.\u{200F}',
    '١٬٢٣٤٫٥٠\u{A0}ج.م.\u{200F}',
    '\u{61C}-١٬٢٣٤٬٥٦٧٫٨٩\u{A0}ج.م.\u{200F}',
    '+١٢٫٠٠\u{A0}ج.م.\u{200F}',
    '\u{61C}-١٬٢٠٠٫٠٠\u{A0}ج.م.\u{200F}',
    '+١٬٢٠٠٫٠٠\u{A0}ج.م.\u{200F}',
    '١٬٢٠٠٫٠٠\u{A0}ج.م.\u{200F}',
  ],
  'ar-EG USD': [
    '٠٫٠٥\u{A0}US\$',
    '\u{61C}-١٫٠٠\u{A0}US\$',
    '١٬٢٣٤٫٥٠\u{A0}US\$',
    '\u{61C}-١٬٢٣٤٬٥٦٧٫٨٩\u{A0}US\$',
    '+١٢٫٠٠\u{A0}US\$',
    '\u{61C}-١٬٢٠٠٫٠٠\u{A0}US\$',
    '+١٬٢٠٠٫٠٠\u{A0}US\$',
    '١٬٢٠٠٫٠٠\u{A0}US\$',
  ],
  'ar-MA MAD': [
    'د.م.\u{200F}\u{A0}0,05',
    '\u{200E}-د.م.\u{200F}\u{A0}1,00',
    'د.م.\u{200F}\u{A0}1.234,50',
    '\u{200E}-د.م.\u{200F}\u{A0}1.234.567,89',
    '+د.م.\u{200F}\u{A0}12,00',
    '\u{200E}-د.م.\u{200F}\u{A0}1.200,00',
    '+د.م.\u{200F}\u{A0}1.200,00',
    'د.م.\u{200F}\u{A0}1.200,00',
  ],
  'ar-MA USD': [
    'US\$\u{A0}0,05',
    '\u{200E}-US\$\u{A0}1,00',
    'US\$\u{A0}1.234,50',
    '\u{200E}-US\$\u{A0}1.234.567,89',
    '+US\$\u{A0}12,00',
    '\u{200E}-US\$\u{A0}1.200,00',
    '+US\$\u{A0}1.200,00',
    'US\$\u{A0}1.200,00',
  ],
  'fr-FR EUR': [
    '0,05\u{A0}€',
    '-1,00\u{A0}€',
    '1\u{202F}234,50\u{A0}€',
    '-1\u{202F}234\u{202F}567,89\u{A0}€',
    '+12,00\u{A0}€',
    '-1\u{202F}200,00\u{A0}€',
    '+1\u{202F}200,00\u{A0}€',
    '1\u{202F}200,00\u{A0}€',
  ],
  'fr-FR USD': [
    '0,05\u{A0}\$US',
    '-1,00\u{A0}\$US',
    '1\u{202F}234,50\u{A0}\$US',
    '-1\u{202F}234\u{202F}567,89\u{A0}\$US',
    '+12,00\u{A0}\$US',
    '-1\u{202F}200,00\u{A0}\$US',
    '+1\u{202F}200,00\u{A0}\$US',
    '1\u{202F}200,00\u{A0}\$US',
  ],
  'fr-CA CAD': [
    '0,05\u{A0}\$\u{A0}CA',
    '-1,00\u{A0}\$\u{A0}CA',
    '1\u{A0}234,50\u{A0}\$\u{A0}CA',
    '-1\u{A0}234\u{A0}567,89\u{A0}\$\u{A0}CA',
    '+12,00\u{A0}\$\u{A0}CA',
    '-1\u{A0}200,00\u{A0}\$\u{A0}CA',
    '+1\u{A0}200,00\u{A0}\$\u{A0}CA',
    '1\u{A0}200,00\u{A0}\$\u{A0}CA',
  ],
  'fr-CA USD': [
    '0,05\u{A0}\$\u{A0}US',
    '-1,00\u{A0}\$\u{A0}US',
    '1\u{A0}234,50\u{A0}\$\u{A0}US',
    '-1\u{A0}234\u{A0}567,89\u{A0}\$\u{A0}US',
    '+12,00\u{A0}\$\u{A0}US',
    '-1\u{A0}200,00\u{A0}\$\u{A0}US',
    '+1\u{A0}200,00\u{A0}\$\u{A0}US',
    '1\u{A0}200,00\u{A0}\$\u{A0}US',
  ],
  'fr-CH CHF': [
    '0.05\u{A0}CHF',
    '-1.00\u{A0}CHF',
    '1\u{202F}234.50\u{A0}CHF',
    '-1\u{202F}234\u{202F}567.89\u{A0}CHF',
    '+12.00\u{A0}CHF',
    '-1\u{202F}200.00\u{A0}CHF',
    '+1\u{202F}200.00\u{A0}CHF',
    '1\u{202F}200.00\u{A0}CHF',
  ],
  'fr-CH USD': [
    '0.05\u{A0}\$US',
    '-1.00\u{A0}\$US',
    '1\u{202F}234.50\u{A0}\$US',
    '-1\u{202F}234\u{202F}567.89\u{A0}\$US',
    '+12.00\u{A0}\$US',
    '-1\u{202F}200.00\u{A0}\$US',
    '+1\u{202F}200.00\u{A0}\$US',
    '1\u{202F}200.00\u{A0}\$US',
  ],
  'pt-BR BRL': [
    'R\$\u{A0}0,05',
    '-R\$\u{A0}1,00',
    'R\$\u{A0}1.234,50',
    '-R\$\u{A0}1.234.567,89',
    '+R\$\u{A0}12,00',
    '-R\$\u{A0}1.200,00',
    '+R\$\u{A0}1.200,00',
    'R\$\u{A0}1.200,00',
  ],
  'pt-BR USD': [
    'US\$\u{A0}0,05',
    '-US\$\u{A0}1,00',
    'US\$\u{A0}1.234,50',
    '-US\$\u{A0}1.234.567,89',
    '+US\$\u{A0}12,00',
    '-US\$\u{A0}1.200,00',
    '+US\$\u{A0}1.200,00',
    'US\$\u{A0}1.200,00',
  ],
  'pt-PT EUR': [
    '0,05\u{A0}€',
    '-1,00\u{A0}€',
    '1\u{A0}234,50\u{A0}€',
    '-1\u{A0}234\u{A0}567,89\u{A0}€',
    '+12,00\u{A0}€',
    '-1\u{A0}200,00\u{A0}€',
    '+1\u{A0}200,00\u{A0}€',
    '1\u{A0}200,00\u{A0}€',
  ],
  'pt-PT USD': [
    '0,05\u{A0}US\$',
    '-1,00\u{A0}US\$',
    '1\u{A0}234,50\u{A0}US\$',
    '-1\u{A0}234\u{A0}567,89\u{A0}US\$',
    '+12,00\u{A0}US\$',
    '-1\u{A0}200,00\u{A0}US\$',
    '+1\u{A0}200,00\u{A0}US\$',
    '1\u{A0}200,00\u{A0}US\$',
  ],
};

/// v1's `PERIODS`: May and September 2026.
const Map<String, List<String>> _v1Months = {
  'en-US': ['May 2026', 'September 2026'],
  'en-GB': ['May 2026', 'September 2026'],
  'en-IN': ['May 2026', 'September 2026'],
  'zh-CN': ['五月 2026', '九月 2026'],
  'hi-IN': ['मई 2026', 'सितंबर 2026'],
  'es-ES': ['mayo 2026', 'septiembre 2026'],
  'es-MX': ['mayo 2026', 'septiembre 2026'],
  'ar-SA': ['مايو 2026', 'سبتمبر 2026'],
  'ar-EG': ['مايو 2026', 'سبتمبر 2026'],
  'ar-MA': ['ماي 2026', 'شتنبر 2026'],
  'fr-FR': ['mai 2026', 'septembre 2026'],
  'fr-CA': ['mai 2026', 'septembre 2026'],
  'fr-CH': ['mai 2026', 'septembre 2026'],
  'pt-BR': ['maio 2026', 'setembro 2026'],
  'pt-PT': ['maio 2026', 'setembro 2026'],
};
