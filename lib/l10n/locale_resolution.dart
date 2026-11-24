/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:flutter/widgets.dart';

/// Picks the app's language from the phone's language list (ADR-0015).
///
/// A phone language matches a language pack when the two have the same language and the same
/// script, written in the tag or likely for it. So zh-Hans-SG and zh-CN read the Simplified Chinese
/// pack, but zh-TW and zh-Hant-HK don't, because they write Traditional characters. This is the rule
/// of Android's `LocaleList.matchesLanguageAndScript`, which v1's string resources followed.
///
/// The first phone language, in the user's order, that matches a pack wins. The phone language
/// itself is returned rather than the pack's, so widgets keep its region (en-GB dates, for
/// example). When no phone language matches, the result is the first of [supportedLocales],
/// English.
///
/// Flutter's default, `basicLocaleListResolution`, differs in two ways the app avoids: it gives
/// zh-TW the Simplified pack, and it falls back to a pack from the same country, such as
/// Simplified Chinese for Uyghur in China (ug-CN).
Locale resolveAppLocale(
  List<Locale>? preferredLocales,
  Iterable<Locale> supportedLocales,
) {
  for (final preferred in preferredLocales ?? const <Locale>[]) {
    final script = _likelyScript(preferred);
    if (script == null) {
      continue;
    }
    for (final supported in supportedLocales) {
      if (supported.languageCode == preferred.languageCode &&
          _likelyScript(supported) == script) {
        return preferred;
      }
    }
  }
  return supportedLocales.first;
}

/// The regions where Chinese is written in Traditional characters unless the tag says otherwise,
/// from Unicode CLDR's likely subtags, which Android's ICU uses. Checked against ICU 74 (CLDR 44)
/// on 2026-11-23. Chinese in every other region, or with no region, is Simplified.
const Set<String> _traditionalChineseRegions = {
  'AU',
  'BN',
  'GB',
  'GF',
  'HK',
  'ID',
  'MO',
  'PA',
  'PF',
  'PH',
  'SR',
  'TH',
  'TW',
  'US',
  'VN',
};

/// The script [locale] is written in: its own script subtag, or the likely one for its language
/// and region. Null for a language the app has no pack for, which never matches.
String? _likelyScript(Locale locale) {
  final written = locale.scriptCode;
  if (written != null) {
    return written.toLowerCase();
  }
  return switch (locale.languageCode) {
    'zh' =>
      _traditionalChineseRegions.contains(locale.countryCode) ? 'hant' : 'hans',
    'en' || 'es' || 'fr' || 'pt' => 'latn',
    'hi' => 'deva',
    'ar' => 'arab',
    _ => null,
  };
}
