---
paths:
  - "app/src/main/res/**"
  - "app/src/main/java/com/denariidolor/util/Money.kt"
  - "app/src/main/java/com/denariidolor/util/Currencies.kt"
  - "app/src/main/java/com/denariidolor/util/DateUtils.kt"
  - "app/src/main/java/com/denariidolor/util/TextDirection.kt"
  - "app/src/main/java/com/denariidolor/util/TransactionTypeLabels.kt"
  - "app/src/main/java/com/denariidolor/data/local/preferences/CurrencyPreferences.kt"
---

# Strings, languages, currency and resources

## Strings and the six language packs

- English is the source, in `res/values/strings.xml`. Six packs follow it:

  | Folder | Language |
  |---|---|
  | `values-zh-rCN` | Simplified Chinese |
  | `values-hi` | Hindi |
  | `values-es` | Latin American Spanish |
  | `values-ar` | Modern Standard Arabic (right to left) |
  | `values-fr` | French (France) |
  | `values-pt` | Brazilian Portuguese |

- Every translatable string is in all six packs; `lintDebug` fails on a missing translation. Mark a string that must not be
  translated with `translatable="false"`.
- Keep the format arguments identical in every pack (`%1$s`, `%d`, `%%`), and use positional arguments when there's more
  than one. Escape `'` and `"` (`\'`, `\"`).
- **Plurals:** use `<plurals>` with every quantity the language needs:
  - Chinese: `other`.
  - Hindi: `one`, `other`.
  - Spanish, French and Portuguese: `one`, `many`, `other`.
  - Arabic: `zero`, `one`, `two`, `few`, `many`, `other`.

  Read them with `pluralStringResource(id, count, count)` or `getQuantityString(id, count, count)`.
- Never build sentences from separately translated fragments. Use one string with arguments.
- Name keys in snake_case with the feature's prefix, matching their neighbours, and put a new key next to its feature group in
  every file.
- Translations you write are drafts. List the new keys in your summary as needing review by a native speaker.

## Currency, numbers and dates

- **Currency** is a saved setting (an ISO code in `CurrencyPreferences`), not part of a language pack.
  - Only currencies with 2 decimals are allowed, and amounts are never converted.
  - Never write `$`, `€` or any other symbol into strings or code. `formatMoney` formats through `NumberFormat` for the
    device's locale.
- **Parsing:** `Money.parseToCents` and `DateUtils.parseIsoDate` accept the currency's symbol or code and digits from any
  Unicode script (`Money.normalizeDigits`). Keep that when you change either one, and add `MoneyTest` or `DateUtilsTest`
  cases for the scripts involved.
- **Seeded names:** the default accounts and categories are named from string resources when they're seeded, and after that
  they're the user's data. Never translate stored data again.

## Right to left

- The manifest sets `android:supportsRtl="true"`.
- Use `start` and `end`, and mirrored icons.
- Transfer arrows come from `transferRoute`, which follows the language's direction.
- A known limit: Arabic with account names in a left-to-right script can point the arrow the wrong way.

## Other resources

- **Themes:** `values/themes.xml` and `values-night/themes.xml`; colors in `values/colors.xml`. Compose screens take colors
  from `MaterialTheme` and `VizColors`, not from raw resources.
- **Icons:** the launcher icons are adaptive (`mipmap-anydpi`). `src/main/ic_launcher-playstore.png` and
  `feature-graphic-playstore.png` are store assets; don't regenerate them unless asked.
- **Security controls:** `res/xml/backup_rules.xml`, `data_extraction_rules.xml` and `file_paths.xml` are controls; see
  `security.md`.
