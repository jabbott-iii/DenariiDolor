/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/l10n/currency_symbols.dart';
import 'package:denarii_dolor/platform/money_parser.dart';
import 'package:flutter/widgets.dart';
import 'package:intl/date_symbol_data_local.dart' show dateTimeSymbolMap;
import 'package:intl/date_symbols.dart';
import 'package:intl/intl.dart';

// Money and month text, ported from v1's `PlatformFormat.kt` with `package:intl` (ADR-0016). Callers pass the locale
// that `Localizations.localeOf` gives, which `resolveAppLocale` picked (ADR-0015).

/// [cents] in the currency with ISO 4217 code [currencyCode], written as [locale] writes money:
/// `$1,234.50` (en-US), `₹12,34,567.89` (hi-IN), or, with no-break spaces and direction marks
/// among them, `1 234,50 €` (fr-FR) and `١٬٢٣٤٫٥٠ ر.س.` (ar-SA).
///
/// The amount always has two decimals, because the app allows only currencies with two. [showPlus]
/// puts a `+` in front of a positive amount, as v1's income rows had. Every `int` is written
/// exactly: no floating point touches the amount.
String formatMoney(
  int cents, {
  required String currencyCode,
  required Locale locale,
  bool showPlus = false,
}) {
  final dataLocale = _numberDataLocale(locale);
  final format = _currencyFormat(dataLocale, currencyCode);
  var text = _exactText(format, cents);
  final moneyDecimal = _moneyDecimalSeparators[dataLocale];
  if (moneyDecimal != null) {
    text = text.replaceFirst(format.symbols.DECIMAL_SEP, moneyDecimal);
  }
  text = text.replaceFirst(
    _symbolStandIn,
    currencySymbol(currencyCode, locale),
  );
  return showPlus && cents > 0 ? '+$text' : text;
}

/// The symbol [locale] writes for the currency with ISO 4217 code [currencyCode], from Unicode
/// CLDR (`currency_symbols.dart`): `US$` for `USD` in en-GB, `$US` in fr-FR, `USD` in es-MX. A
/// currency CLDR gives no symbol in that language is written as its code.
String currencySymbol(String currencyCode, Locale locale) {
  final language = locale.languageCode;
  final tag = _symbolTableTag(locale);
  return (tag == null ? null : currencySymbols[tag]?[currencyCode]) ??
      currencySymbols[language]?[currencyCode] ??
      currencyCode;
}

/// What the Rust core needs to read an amount the user types in [locale] and the currency with
/// ISO 4217 code [currencyCode]: the locale's decimal separator, then the currency's symbol in
/// [locale] and its code (`docs/specs/core.md`, "Money").
///
/// v1 also passed the currency's symbol in the device's default locale, which on a phone was the
/// same locale, so it added nothing.
NumberSymbols moneyParsingSymbols(String currencyCode, Locale locale) {
  final format = _currencyFormat(_numberDataLocale(locale), currencyCode);
  return NumberSymbols(
    decimalSeparator: format.symbols.DECIMAL_SEP,
    currencyMarks: [currencySymbol(currencyCode, locale), currencyCode],
  );
}

/// The [month] (1–12) of [year] (1–9999) as v1's monthly report named it: the month's name in
/// [locale], a space, and the year in four ASCII digits, such as `septiembre 2026`. v1 wrote it
/// with the `java.time` pattern `MMMM yyyy`, which writes ASCII digits in every language.
String formatMonthYear({
  required int year,
  required int month,
  required Locale locale,
}) {
  RangeError.checkValueInInterval(year, 1, 9999, 'year');
  RangeError.checkValueInInterval(month, 1, 12, 'month');
  final dataLocale = Intl.verifiedLocale(
    _intlLocaleName(locale),
    _dateSymbols.containsKey,
    onFailure: (_) => _fallbackLocale,
  );
  final symbols = _dateSymbols[dataLocale] ?? _dateSymbols[_fallbackLocale];
  if (symbols == null) {
    throw StateError('intl has no date data for $_fallbackLocale.');
  }
  return '${symbols.MONTHS[month - 1]} ${year.toString().padLeft(4, '0')}';
}

/// The locale whose data `intl` falls back to when it has none for a language.
const String _fallbackLocale = 'en';

/// Stands for the currency symbol until the number is written, so that changing the number's
/// marks can't touch the symbol. No locale writes the generic currency sign in a number.
const String _symbolStandIn = '¤';

/// The most cents that [MicroMoney], which counts millionths of a unit in a 64-bit `int`, holds:
/// `(2^63 - 1) ~/ 10000`.
const int _maxMicroMoneyCents = 922337203685477;

/// The Arabic regions that write money with Arabic-Indic digits (`١٬٢٣٤٫٥٠`), as `intl`'s Egyptian
/// data does. Checked against Android 16's ICU on the API 36 emulator on 2026-11-23. `intl` has no
/// data of its own for these regions, and its plain Arabic data writes Latin digits, as Android 16
/// does for plain Arabic, the United Arab Emirates, and Western Sahara.
const Set<String> _arabicIndicDigitRegions = {
  'BH',
  'DJ',
  'EG',
  'ER',
  'IL',
  'IQ',
  'JO',
  'KM',
  'KW',
  'LB',
  'MR',
  'OM',
  'PS',
  'QA',
  'SA',
  'SD',
  'SO',
  'SS',
  'SY',
  'TD',
  'YE',
};

/// The Arabic regions that write money with Latin digits and a decimal comma (`1.234,50`), as
/// `intl`'s Algerian data does. Checked against Android 16's ICU on the API 36 emulator on
/// 2026-11-23.
const Set<String> _decimalCommaArabicRegions = {'DZ', 'LY', 'MA', 'TN'};

/// The decimal mark of money in the locales where CLDR sets one apart from the mark of other
/// numbers, which `intl` ignores, keyed by `intl`'s locale name. Only Swiss French is listed:
/// Android 16, JDK 17, and JDK 25 all write `1 234.50 CHF` there, and they disagree on the others
/// (English in Germany, for one).
const Map<String, String> _moneyDecimalSeparators = {'fr_CH': '.'};

/// `intl`'s date data for every locale, read once. `DateFormat` would read the same data from a
/// global table that Material's localizations also fill, so the text could depend on which wrote
/// it last.
final Map<String, DateSymbols> _dateSymbols = dateTimeSymbolMap();

/// One currency format per `intl` locale and currency, because building one parses its pattern.
final Map<(String, String), NumberFormat> _currencyFormats = {};

NumberFormat _currencyFormat(String dataLocale, String currencyCode) =>
    _currencyFormats.putIfAbsent(
      (dataLocale, currencyCode),
      () => NumberFormat.currency(
        locale: dataLocale,
        name: currencyCode,
        symbol: _symbolStandIn,
        decimalDigits: 2,
      ),
    );

/// The `intl` locale whose number data writes money for [locale] (ADR-0016).
String _numberDataLocale(Locale locale) {
  if (locale.languageCode == 'ar') {
    final region = locale.countryCode;
    if (_arabicIndicDigitRegions.contains(region)) {
      return 'ar_EG';
    }
    if (_decimalCommaArabicRegions.contains(region)) {
      return 'ar_DZ';
    }
    return 'ar';
  }
  return Intl.verifiedLocale(
        _intlLocaleName(locale),
        NumberFormat.localeExists,
        onFailure: (_) => _fallbackLocale,
      ) ??
      _fallbackLocale;
}

/// [locale]'s language and region the way `intl` names locales, such as `pt_BR`. The script is
/// left out: no locale of the app's seven languages that `intl` has data for differs by script.
String _intlLocaleName(Locale locale) {
  final region = locale.countryCode;
  return region == null
      ? locale.languageCode
      : '${locale.languageCode}_$region';
}

/// The most specific tag of `currency_symbols.dart` that describes [locale], or null when only
/// its language does. A script that the table knows for the language (`zh-Hans`, `zh-Hant`) keeps
/// the lookup in that script, so Simplified Chinese in Taiwan doesn't read Traditional Chinese's
/// data; any other script is the language's only one, and is ignored.
String? _symbolTableTag(Locale locale) {
  final language = locale.languageCode;
  final script = locale.scriptCode;
  final region = locale.countryCode;
  final scriptIsKnown =
      script != null && currencySymbols.containsKey('$language-$script');
  final candidates = [
    if (scriptIsKnown && region != null) '$language-$script-$region',
    if (scriptIsKnown) '$language-$script',
    if (!scriptIsKnown && region != null) '$language-$region',
  ];
  for (final candidate in candidates) {
    if (currencySymbols.containsKey(candidate)) {
      return candidate;
    }
  }
  return null;
}

/// [cents] written by [format], exactly.
///
/// [MicroMoney] is `intl`'s exact form of an amount, but it holds at most [_maxMicroMoneyCents].
/// Further from zero, the whole units can't be 0 and carry the sign, so they're written as an
/// `int`, which `intl` writes exactly, and the cents replace the two zeros after its decimal mark.
String _exactText(NumberFormat format, int cents) {
  if (cents >= -_maxMicroMoneyCents && cents <= _maxMicroMoneyCents) {
    return format.format(MicroMoney(cents * 10000));
  }
  final text = format.format(cents ~/ 100);
  final zero = format.symbols.ZERO_DIGIT.codeUnitAt(0);
  bool isDigit(int codeUnit) => codeUnit >= zero && codeUnit <= zero + 9;
  var last = text.length - 1;
  while (last >= 0 && !isDigit(text.codeUnitAt(last))) {
    last--;
  }
  if (last < 1 ||
      text.codeUnitAt(last) != zero ||
      text.codeUnitAt(last - 1) != zero) {
    throw StateError('intl wrote $cents ~/ 100 without two zero decimals.');
  }
  final fraction = cents.remainder(100).abs();
  final fractionDigits = String.fromCharCodes([
    zero + fraction ~/ 10,
    zero + fraction % 10,
  ]);
  return text.replaceRange(last - 1, last + 1, fractionDigits);
}
