/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/l10n/app_localizations.dart';
import 'package:denarii_dolor/l10n/locale_resolution.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';

/// Parses a BCP 47 tag such as `zh-Hant-TW` into a [Locale].
Locale _tag(String tag) {
  final parts = tag.split('-');
  String? script;
  String? country;
  for (final part in parts.skip(1)) {
    if (part.length == 4) {
      script = part;
    } else {
      country = part;
    }
  }
  return Locale.fromSubtags(
    languageCode: parts.first,
    scriptCode: script,
    countryCode: country,
  );
}

/// What the app resolves for a phone set to [tags], in that order.
Locale _resolve(List<String> tags) => resolveAppLocale(
  tags.map(_tag).toList(),
  AppLocalizations.supportedLocales,
);

void main() {
  test('each pack resolves to itself', () {
    for (final locale in AppLocalizations.supportedLocales) {
      expect(
        resolveAppLocale([locale], AppLocalizations.supportedLocales),
        locale,
      );
    }
  });

  test('a phone language with a region keeps its region', () {
    for (final tag in ['en-GB', 'es-MX', 'ar-EG', 'hi-IN', 'fr-CA', 'pt-PT']) {
      expect(_resolve([tag]), _tag(tag), reason: tag);
    }
  });

  group('Chinese matches by script, as Android does', () {
    test('Simplified Chinese reads the zh pack, in any region', () {
      for (final tag in [
        'zh',
        'zh-CN',
        'zh-SG',
        'zh-Hans',
        'zh-Hans-SG',
        'zh-Hans-TW',
      ]) {
        expect(_resolve([tag]), _tag(tag), reason: tag);
      }
    });

    test('Traditional Chinese falls back to English', () {
      for (final tag in [
        'zh-TW',
        'zh-HK',
        'zh-MO',
        'zh-US',
        'zh-Hant',
        'zh-Hant-TW',
        'zh-Hant-CN',
      ]) {
        expect(_resolve([tag]), const Locale('en'), reason: tag);
      }
    });

    test('a later Simplified Chinese wins over an earlier Traditional one', () {
      expect(_resolve(['zh-Hant-TW', 'zh-Hans-CN']), _tag('zh-Hans-CN'));
    });
  });

  group('the whole language list is walked (ADR-0015)', () {
    test('a language with no pack is skipped for a later one', () {
      expect(_resolve(['de-DE', 'es-ES']), _tag('es-ES'));
      expect(_resolve(['ja-JP', 'de-DE', 'ar-SA']), _tag('ar-SA'));
    });

    test('the earliest match wins', () {
      expect(_resolve(['hi-IN', 'en-US']), _tag('hi-IN'));
      expect(_resolve(['de-DE', 'en-GB', 'es-ES']), _tag('en-GB'));
    });

    test('no match, an empty list, or no list falls back to English', () {
      expect(_resolve(['de-DE', 'ja-JP']), const Locale('en'));
      expect(_resolve([]), const Locale('en'));
      expect(
        resolveAppLocale(null, AppLocalizations.supportedLocales),
        const Locale('en'),
      );
    });
  });

  test('a language with no pack never takes a pack from its country', () {
    // Flutter's basicLocaleListResolution gives Uyghur in China the Chinese pack.
    expect(_resolve(['ug-CN']), const Locale('en'));
    expect(_resolve(['ca-ES']), const Locale('en'));
  });

  test('a pack language in another script falls back to English', () {
    expect(_resolve(['hi-Latn-IN']), const Locale('en'));
  });

  test('text outside widgets comes from the resolved locale, synchronously', () {
    // Setup seeds the default accounts' names before any screen exists (M2.6).
    expect(
      lookupAppLocalizations(_resolve(['zh-Hans-SG'])).defaultAccountCash,
      '现金',
    );
    expect(
      lookupAppLocalizations(_resolve(['zh-TW'])).defaultAccountCash,
      'Cash',
    );
  });
}
