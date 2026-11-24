/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'dart:convert';
import 'dart:io';

import 'package:denarii_dolor/l10n/app_localizations.dart';
import 'package:flutter_test/flutter_test.dart';

/// The plural categories each language uses for whole numbers, from Unicode CLDR. v1's Android
/// packs had these forms, and Android chose among them by the same rules.
const _pluralCategories = {
  'en': {'one', 'other'},
  'zh': {'other'},
  'hi': {'one', 'other'},
  'es': {'one', 'many', 'other'},
  'ar': {'zero', 'one', 'two', 'few', 'many', 'other'},
  'fr': {'one', 'many', 'other'},
  'pt': {'one', 'many', 'other'},
};

/// The ARB files in lib/l10n/, by the locale in their `@@locale`.
Map<String, Map<String, Object?>> _readArbFiles() {
  final arbs = <String, Map<String, Object?>>{};
  final files = Directory('lib/l10n')
      .listSync()
      .whereType<File>()
      .where((file) => file.path.endsWith('.arb'));
  for (final file in files) {
    final arb = jsonDecode(file.readAsStringSync());
    if (arb
        case {'@@locale': final String locale} &&
            final Map<String, Object?> messages) {
      arbs[locale] = messages;
    } else {
      fail('${file.path} has no @@locale');
    }
  }
  return arbs;
}

/// The messages of [arb], without its `@@locale` and `@` metadata.
Map<String, String> _messages(Map<String, Object?> arb) => {
  for (final MapEntry(:key, :value) in arb.entries)
    if (!key.startsWith('@'))
      if (value case final String text) key: text,
};

/// The names of the placeholders that [message] fills in, such as `count` in
/// `{count, plural, one{{count} budget} other{{count} budgets}}`.
Set<String> _placeholders(String message) => {
  for (final match in RegExp(r'\{([A-Za-z]\w*)\}').allMatches(message))
    if (match.group(1) case final String name) name,
};

/// The plural forms of a plural [message], such as `one` and `other`.
Set<String> _pluralForms(String message) => {
  for (final match in RegExp(
    r'\b(zero|one|two|few|many|other)\{',
  ).allMatches(message))
    if (match.group(1) case final String form) form,
};

void main() {
  final arbs = _readArbFiles();
  final englishArb = arbs['en'] ?? const <String, Object?>{};
  final template = _messages(englishArb);
  final plurals = {
    for (final MapEntry(:key, :value) in template.entries)
      if (value.contains(', plural,')) key,
  };

  test('there is one ARB file for each of the seven languages', () {
    expect(arbs.keys, unorderedEquals(_pluralCategories.keys));
    expect(
      AppLocalizations.supportedLocales.map((locale) => locale.toString()),
      unorderedEquals(_pluralCategories.keys),
    );
    expect(AppLocalizations.supportedLocales.first.languageCode, 'en');
    expect(template, isNotEmpty);
    expect(plurals, isNotEmpty);
  });

  test('every template message has a description for translators', () {
    for (final key in template.keys) {
      final metadata = englishArb['@$key'];
      if (metadata case {'description': final String description}) {
        expect(description.trim(), isNotEmpty, reason: key);
      } else {
        fail('$key has no description');
      }
    }
  });

  for (final MapEntry(key: locale, value: arb) in arbs.entries) {
    group(locale, () {
      final messages = _messages(arb);

      test('has every message of the template and no other', () {
        expect(messages.keys, unorderedEquals(template.keys));
      });

      test('fills in the same placeholders as the template', () {
        for (final MapEntry(:key, value: english) in template.entries) {
          expect(
            _placeholders(messages[key] ?? ''),
            _placeholders(english),
            reason: key,
          );
        }
      });

      test('has every plural form its language uses', () {
        final categories = _pluralCategories[locale] ?? const <String>{};
        expect(categories, isNotEmpty, reason: 'no plural rules for $locale');
        for (final key in plurals) {
          expect(
            _pluralForms(messages[key] ?? ''),
            containsAll(categories),
            reason: key,
          );
        }
      });
    });
  }

  test('the confirmation words and the app name are never translated', () {
    for (final locale in AppLocalizations.supportedLocales) {
      final l10n = lookupAppLocalizations(locale);
      // The wipe check compares the typed text with this exact word (CS-16).
      expect(l10n.wipeDataConfirmationWord, 'WIPE', reason: '$locale');
      expect(l10n.backupRestoreConfirmationWord, 'RESTORE', reason: '$locale');
      expect(l10n.appName, 'Denarii Dolor', reason: '$locale');
    }
  });
}
