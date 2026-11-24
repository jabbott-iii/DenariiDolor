# Roadmap

The rewrite of Denarii Dolor as a Flutter app with a Rust core, from the plan to an Android release
that replaces v1 on Google Play, then iOS. The milestone order is a draft of 2026-11-23 for the
maintainer to confirm.

## How to use this file

- Work from the top down. A task is unblocked when every task in its `Depends` line is ticked.
- Tick a task only after its `Accept` results hold and its `Verify` commands ran and passed, as the
  "Definition of done" in `CLAUDE.md` says.
- The maintainer checks each milestone's exit criteria.
- Within a milestone, work moves through the environments in this order: Linux with Rust, an
  Android emulator (API 26 and API 36), an Android phone with v1 installed and a fingerprint
  enrolled, then a Mac with Xcode.
- v1 means the Kotlin app at commit `6d57526`; read its source with `git show 6d57526:<path>`.

## M0 — The plan, and the domain rules in Rust

**Goal:** Agree on the rewrite's direction, write down what v1 leaves on phones, and port the pure
domain rules to Rust with their tests.

- [x] **M0.1 — claude: adopt the Doctrina rules**
  - Note: 2026-11-23: copied from the Doctrina repository with their "Project details" filled in.
    `flutter.md` points to `kotlin.md`, `android.md`, and `swift.md`, which this repository
    doesn't have yet; M3.1 and M7.1 add them.
  - Do:
    - Add `commits.md`, `flutter.md`, `roadmap-and-docs.md`, `rust.md`, and `writing.md` to
      `.claude/rules/`, and point `security.md` and `legal-docs.md` at the new layout.
  - Accept:
    - No rule's "Project details" still reads `[FILL IN: …]`.
  - Verify: `grep -n '^- \*\*.*\[FILL IN' .claude/rules/*.md` (expect no output)
- [x] **M0.2 — docs: spec what v1 stores on phones and in backups**
  - Do:
    - Write `docs/specs/android-v1-formats.md` from v1's source, citing `path:line` at `6d57526`.
  - Accept:
    - The spec covers file names, Keystore keys, the profile and key wrapping, the v1.0.x
      upgrade, SQLCipher, the schema, backups, exports, and the session, with invariants and the
      fixtures F1–F9 that later tasks build.
  - Verify: the maintainer reads it against the code (no command)
- [x] **M0.3 — docs: overview, architecture, dependency policy, decisions, and roadmap**
  - Do:
    - Write `docs/00-overview.md`, `docs/01-architecture.md`, `docs/02-dependency-policy.md`,
      `docs/decisions.md` (ADR-0001 to ADR-0008), and this file.
  - Accept:
    - ADR-0001, ADR-0002, ADR-0003, and ADR-0008 record the maintainer's decisions; the rest are
      `Proposed`.
  - Verify: the maintainer reviews them (no command)
- [x] **M0.4 — core: Rust workspace, lints, and CI job**
  - Note: 2026-11-23: Rust 1.100.0 pinned in `rust/rust-toolchain.toml`, edition 2024. No
    third-party crates.
  - Do:
    - Create `rust/Cargo.toml` with `[workspace.lints]`, `rust/clippy.toml`, and the
      `denarii-core` crate; add a Rust job to `.github/workflows/ci.yml`.
  - Accept:
    - The four Rust check commands in `.claude/rules/rust.md` pass.
  - Verify: `cd rust && cargo fmt --all --check && cargo clippy --workspace --all-targets --locked -- -D warnings`
    (expect success)
- [x] **M0.5 — core: money parsing and plain text**
  - Note: 2026-11-23: the decimal-digit and currency-sign tables cover only the Basic
    Multilingual Plane, from Unicode 15.0, because v1's Kotlin code read text one UTF-16 unit at a
    time (`docs/specs/core.md`, "Money").
  - Do:
    - Port `Money.parseToCents`, `normalizeDigits`, `toPlain`, and `toInput` to `core::money`.
  - Accept:
    - Every parsing case in v1's `MoneyTest.kt` gives the same result in Rust.
  - Verify: `cd rust && cargo test --locked -p denarii-core --test money` (expect all pass)
- [x] **M0.6 — core: transactions and balance changes**
  - Do:
    - Port `Transaction`, `Expense`, `Income`, `Transfer`, `TransactionType`, and
      `Ledger.balanceDeltas` to `core::transaction` and `core::ledger`, with checked arithmetic.
  - Accept:
    - Every case in v1's `LedgerTest.kt` and `TransactionModelTest.kt` passes in Rust.
  - Verify: `cd rust && cargo test --locked -p denarii-core --test ledger` (expect all pass)
- [x] **M0.7 — core: transaction validation and budget overage**
  - Do:
    - Port `Validators`, `DomainError`, and the field checks of `ValidateTransactionUseCase` to
      `core::validation` and `core::error`; port the seeded default IDs to `core::defaults`.
  - Accept:
    - Every case in v1's `ValidatorsTest.kt` passes, and validation reports errors in v1's order.
  - Verify: `cd rust && cargo test --locked -p denarii-core --test validation` (expect all pass)
- [x] **M0.8 — claude: skills for the new stack**
  - Note: 2026-11-23: `ci-gates` now runs the Rust checks. `new-feature`, `room-migration`, and
    `dependency-upgrade` were Kotlin-only and left with ADR-0003; their text is at `6d57526`.
  - Note: 2026-11-23: at the maintainer's request, the skills `next-task` and `fix-doc-drift`
    are copied unchanged from Doctrina commit `96b996b`. They take their values from the rules'
    "Project details" and name no project command, so the new stack needs no rewrite of them.
  - Note: 2026-11-23: the user's choice: this task writes `ci-gates` and `dependency-upgrade`
    only. The layers that `new-feature` and `schema-migration` walk through (a `store` use case
    and its migrations, the bridge, ARB strings, and backup formats) don't exist yet, so those
    skills move to M4.9 and M5.3.
  - Note: 2026-11-23: `ci-gates` now runs the Flutter checks with Flutter from `~/flutter/bin`
    and, for a change to `android/` or the packages, `flutter build apk --release` and the
    release APK's permissions from `aapt2` 36.0.0: only
    `com.denariidolor.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. `dependency-upgrade` covers
    crates, Dart packages, the Rust and Flutter pins, the Android build tools and SDK levels, and
    GitHub Actions. Every command in both skills ran once, including `flutter run --no-resident`
    on `Denarii_API_26` and the pin check, which ran in a scratch copy of `pubspec.yaml`.
  - Depends: M1.1
  - Do:
    - Extend `ci-gates` with the Flutter checks; write `new-feature`, `schema-migration`, and
      `dependency-upgrade` for Flutter and Rust, starting from v1's skills.
  - Accept:
    - Each skill names only commands and files that exist.
  - Verify: run each skill's commands once (expect success)

**Exit criteria (M0):** the maintainer has read the format spec, confirmed or changed the milestone
order, and accepted, changed, or rejected ADR-0004 to ADR-0007.

---

## M1 — A Flutter app on Android that calls Rust

**Goal:** A debug build that starts on Android, shows a placeholder screen in all seven languages,
and displays an amount that Rust parsed and Dart formatted.

- [x] **M1.1 — ui: create the Flutter project at the repository root**
  - Note: 2026-11-23: the maintainer chose Flutter 3.50.1 pinned in `pubspec.yaml` with iOS 15
    and later (ADR-0009), `ChangeNotifier` view models with no package (ADR-0010), and Flutter's
    `Navigator` with no package (ADR-0011). The maintainer also accepted ADR-0012: generated code
    is committed, and CI checks that it is current.
  - Note: 2026-11-23: Flutter 3.50.1 is installed on the maintainer's machine, in `~/flutter`. Its
    Android SDK has platform 37, build-tools 36.0.0, the command-line tools, NDK 28.2.13676358,
    and the emulators `Denarii_API_26` and `Denarii_API_36` (Google APIs x86_64 images), which
    both boot.
  - Note: 2026-11-23: Flutter 3.50 moved Material out of the SDK (`package:flutter/material.dart`
    is deprecated), so the maintainer approved `material_ui` 1.11.1, and `flutter_lints` 6.0.0 as a
    dev dependency; `cupertino_icons` is left out (the register in `docs/02-dependency-policy.md`).
    Import Material from `package:material_ui/material_ui.dart`.
  - Note: 2026-11-23: the maintainer chose to keep Flutter's `INTERNET` permission in the debug and
    profile manifests only (ADR-0013), and to keep v1's launcher name: `MainActivity` isn't
    exported, and an exported `activity-alias` named `.presentation.ui.auth.LoginActivity` carries
    the launcher intent filter.
  - Note: 2026-11-23: `flutter run --no-resident` started the app through the alias on
    `Denarii_API_26` and `Denarii_API_36`, and starting `MainActivity` directly from `adb` failed
    with "not exported". The pin was checked by changing it: Flutter 3.50.1's `flutter pub get`
    fails against `flutter: 3.50.0` and `flutter: 3.50.2`, and succeeds against `3.50.1`. The
    release APK asks for no permission except AndroidX's own
    `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
  - Note: 2026-11-23: the iOS bundle identifier is `com.denariidolor` and the deployment target
    15.0, set in the Xcode project but not built, because building needs a Mac (M7). The version is
    a placeholder, `0.0.0+1`, until M6.1.
  - Note: 2026-11-23: the app starts locked through `LockedSessionLock`, which has no way to
    unlock, so this build can't reach the signed-in placeholder. Tests unlock it with a fake.
  - Depends: M0.3
  - Do:
    - Run `flutter create` with the application ID `com.denariidolor`, set minSdk 26, targetSdk
      36, and compileSdk 37, set the iOS deployment target to 15.0, and pin the SDK in
      `pubspec.yaml` as ADR-0009 says.
    - Add `analysis_options.yaml` with the strict modes in the flutter rule.
    - Add the app scope, a root widget that switches between a placeholder sign-in screen and a
      placeholder signed-in tree, and one placeholder view model with its test (ADR-0010,
      ADR-0011).
  - Accept:
    - `flutter run` starts the app on an API 26 and an API 36 emulator.
    - `flutter pub get` fails on a Flutter release other than 3.50.1.
  - Verify: `dart format --output=none --set-exit-if-changed .`; `flutter analyze`; `flutter test`
    (expect no issues)
- [x] **M1.2 — bridge: call Rust from Dart**
  - Note: 2026-11-23: the maintainer accepted ADR-0005 and chose ADR-0014: `flutter_rust_bridge`
    2.13.0 pinned exactly, with its default features off and only `dart-opaque`, `rust-async`, and
    `thread-pool` on (the least the generated code compiles with), built by Flutter's
    native-assets hook (`hook/build.dart`). By the user's choice, `x86` is dropped from the `Do`
    line below, because Flutter 3.50.1 builds only `android-arm`, `android-arm64`, and
    `android-x64`.
  - Note: 2026-11-23: `flutter_rust_bridge` 2.13.0 was tested against Flutter 3.44, so check it
    on Flutter 3.50.1 (ADR-0009) before anything else.
  - Note: 2026-11-23: NDK 28.2.13676358, the version Flutter 3.50's Gradle plugin defaults to, is
    installed on the maintainer's machine; build the Rust library with the same version.
  - Note: 2026-11-23: `flutter_rust_bridge` 2.13.0 works on Flutter 3.50.1. Pinned:
    `flutter_rust_bridge` 2.13.0 (crate, Dart package, and `flutter_rust_bridge_codegen`),
    `flutter_rust_bridge_hooks` 2.13.0, `native_toolchain_rust` 1.1.1, and `cargo-expand` 1.0.127
    for the codegen. `rust/rust-toolchain.toml` lists the three Android targets and
    `x86_64-unknown-linux-gnu`, and `rust/crates/bridge/rust-toolchain.toml` links to it.
  - Note: 2026-11-23: the bridge has one function, `parse_cents` (`docs/specs/bridge.md`). The
    sign-in placeholder got a view model, a route, and an amount field that shows the cents Rust
    returned, or the `AmountError` it gave. `flutter test integration_test` passed (3 tests) on
    `Denarii_API_26` and `Denarii_API_36`, and the app showed `123456 cents` for `1,234.56` on API
    36, in the light theme and in the dark theme at 200% text. The release APK holds `libdenarii_bridge.so` for the
    three ABIs and asks for no new permission. The first integration build downloaded Android SDK
    Platform 36, which `integration_test`'s Android module compiles against.
  - Depends: M1.1
  - Decide first: accept or change ADR-0005 (`flutter_rust_bridge`).
  - Do:
    - Add the `bridge` crate, generate the Dart bindings, and build the Rust library for
      `arm64-v8a`, `armeabi-v7a`, `x86_64`, and `x86`.
  - Accept:
    - The placeholder screen shows the cents that `core::parse_cents` returned for a typed amount.
  - Verify: `flutter test integration_test` on an emulator (expect pass)
- [ ] **M1.3 — ci: Flutter checks and the Android build in CI**
  - Note (from M1.2): every Flutter build and `flutter test` run compiles the bridge through
    `hook/build.dart`, so the Flutter job needs Rust 1.100.0 with the targets in
    `rust/rust-toolchain.toml`. The check that the bindings are current runs
    `flutter_rust_bridge_codegen generate` and fails on `git diff --exit-code`; it needs
    `flutter_rust_bridge_codegen` 2.13.0 and `cargo-expand` 1.0.127 installed with
    `cargo install --locked`. Keep `flutter_rust_bridge`'s crate and its two Dart packages in one
    Dependabot group, because they must move together (ADR-0014). The integration tests need an
    emulator: `flutter test integration_test -d <device>`.
  - Note: 2026-11-23: the maintainer chose the recommended answer to three questions, which change
    the M1.2 note above where they differ:
    - CI installs Flutter with a shallow clone of `github.com/flutter/flutter` at the `3.50.1` tag,
      and fails unless the tag points to commit `ae9b586f34a8443573f82ef57c3bf914a908cbf7`, the
      commit it pointed to that day. No third-party action installs it; the alternatives were
      Google's release archive checked against a SHA-256, and `subosito/flutter-action`.
    - CI caches nothing between runs for now; the backlog has the trigger. The alternative was
      `actions/cache`.
    - Dependabot groups `flutter_rust_bridge`'s two Dart packages in one `pub` pull request and
      sends the crate in a Cargo pull request of its own. A CI step fails while the crate, the two
      packages, and the codegen version in `ci.yml` differ, so either pull request stays red until
      someone finishes the upgrade with the `dependency-upgrade` skill. The alternatives were one
      multi-ecosystem group, whose effect on the other crates and packages GitHub's documentation
      doesn't state, and ignoring `flutter_rust_bridge` in Dependabot.
  - Note: 2026-11-23: `ci.yml` keeps "Rust checks" and adds two jobs, whose versions are pinned in
    the workflow's `env` block. "Flutter checks" installs the Rust toolchain and Flutter, then runs
    `flutter pub get --enforce-lockfile`, the three Flutter check commands, and
    `flutter build appbundle --debug`. Before the build, it points `ANDROID_NDK_HOME` at NDK
    28.2.13676358: the runner image points that variable at NDK 27.3, and Flutter hands the NDK it
    names to the bridge's build hook. "Generated code is current" checks that the crate, the two
    Dart packages, and `FRB_VERSION` agree, installs `cargo-expand` 1.0.127 and the codegen 2.13.0,
    reruns the codegen, and fails on any changed or new file. The localizations have no source
    until M1.4, which adds them to that job. `dependabot.yml` adds the `pub` ecosystem.
  - Note: 2026-11-23: a local script ran each new job's `run` steps in order on a fresh clone of
    this change, on the maintainer's Linux machine, and both jobs passed. It ran 23 Flutter tests,
    built a 77 MB debug app bundle with `libdenarii_bridge.so` for the three ABIs, each built with
    NDK 28.2.13676358, and regenerated bindings identical to the committed ones. The shallow clone
    reports `Flutter 3.50.1` on the channel `[user-branch]`, and `pub get` accepts it. Each check
    also failed when it should: on a wrong `FLUTTER_COMMIT`, a hand-edited binding, a
    `flutter_rust_bridge_hooks` 2.14.0 in `pubspec.lock`, and an `FRB_VERSION` of 2.14.0. The
    slowest local steps were the debug app bundle (239 s), compiling the codegen tools (103 s),
    and Flutter's first-run download (about 90 s). Times on GitHub's runners aren't measured yet.
  - Note (from M1.6): the "Generated code is current" job also regenerates
    `lib/l10n/currency_symbols.dart` with the runner's Java 25 (`JAVA_HOME_25_X64`, which GitHub's
    `ubuntu-24.04` release notes list as Temurin 25). Locally, Ubuntu's OpenJDK 25.0.5 writes the
    committed table; whether Temurin's build writes the same bytes is first checked by this pull
    request.
  - ⏳ needs GitHub: open a pull request with this change, and expect "Rust checks", "Flutter
    checks", and "Generated code is current" to pass. Then make the three jobs required status
    checks for `main`: on 2026-11-23 the repository's branch ruleset requires none, so a failing
    job doesn't block a merge.
  - Depends: M1.2
  - Do:
    - Add jobs to `ci.yml` for the Flutter check commands and a debug app bundle, with actions
      pinned to commit SHAs (CS-08); add the `pub` ecosystem to `dependabot.yml`.
    - Regenerate the bridge bindings and the localizations in CI, and fail on any difference from
      the committed files (ADR-0012).
  - Accept:
    - A pull request runs the Rust and Flutter jobs and blocks on either.
  - Verify: a pull request's checks (expect green)
- [x] **M1.4 — i18n: port v1's strings to ARB files**
  - Note (from M1.1): the placeholder sign-out button's tooltip is v1's English `sign_out` string,
    hard-coded in `lib/features/session/ui/signed_in_placeholder_screen.dart`; move it to the ARB
    files.
  - Note (from M1.2): the sign-in placeholder's `Amount` label and its result line
    (`<cents> cents` and `Rejected: <error>`) are hard-coded English in
    `lib/features/auth/ui/sign_in_placeholder_screen.dart`; move them to the ARB files, or drop
    them if M3.5 has removed the amount field by then.
  - Note (from M1.3): add `flutter gen-l10n` to the `generated` job in `.github/workflows/ci.yml`,
    before its "Compare with the committed files" step, so CI fails when the committed
    localizations differ from the ARB files (ADR-0012).
  - Note: 2026-11-23: the maintainer approved `flutter_localizations` and `intl` 0.20.3 as direct
    dependencies (both already came with `material_ui`), chose lowerCamelCase keys (v1's
    `sign_out` is `signOut`), and chose ADR-0015: the app walks the phone's whole language list
    and matches each language by script, as Android does, so zh-TW shows English and a phone set
    to German then Spanish shows Spanish (v1 showed English there).
  - Note: 2026-11-23: a one-off script converted v1's 325 strings, three of them plurals, into
    `lib/l10n/app_{en,zh,hi,es,ar,fr,pt}.arb` in v1's order, and checked every pack against
    English. It undid Android's escapes (`\'`, `\"`, `\n`, `&amp;`, `%%`), named each `%1$s` or
    `%d` placeholder (`%d` is an `int`; amounts and dates stay preformatted `String`s), and wrote
    each plural with the forms its pack had (Arabic has all six). The English file holds a
    description for every message. `appName`, `wipeDataConfirmationWord` (`WIPE`, CS-16), and
    `backupRestoreConfirmationWord` (`RESTORE`) aren't translated and are the same in every pack.
    The Simplified Chinese pack is `app_zh.arb`, because gen-l10n needs a language-only pack.
  - Note: 2026-11-23: the placeholders' text moved to the ARB files: the amount label reuses
    `transactionAmountHint`, the sign-out tooltip is `signOut`, and `lib/app/app_name.dart` gave
    way to `appName`. The result line got two new strings, `placeholderAmountInCents` and
    `placeholderAmountRejected`, with drafted translations. The app uses `material_ui`'s
    `GlobalMaterialLocalizations.delegates`, not the SDK's in
    `AppLocalizations.localizationsDelegates`, because Material's widgets now look up
    `material_ui`'s types. `ios/Runner/Info.plist` lists the seven languages in
    `CFBundleLocalizations`, unbuilt until M7.
  - Note: 2026-11-23: `flutter gen-l10n` reported no untranslated message, and reported one when a
    French message was removed for the test. `test/l10n/` (40 tests) checks each pack's messages,
    placeholders, and plural forms, the untranslated words, the language choice, and Arabic's
    right-to-left layout. On `Denarii_API_36`, with the app's languages set by
    `adb shell cmd locale set-app-locales`, ar-EG laid out right to left in Arabic, de-DE then
    es-ES showed Spanish, zh-Hant-TW showed English, and zh-Hans-SG showed Chinese.
    `flutter test integration_test` passed (3 tests) on `Denarii_API_26` and `Denarii_API_36`. The
    translations are still machine drafts for a native speaker to review before M6.
  - Depends: M1.1
  - Do:
    - Convert `app/src/main/res/values*/strings.xml` at `6d57526` into `lib/l10n/app_*.arb` for
      the seven languages, keeping each key, its plural forms, and a description for translators.
  - Accept:
    - Every v1 string and plural has an ARB entry in each language, and Arabic lays out right to
      left.
  - Verify: `flutter gen-l10n` (expect no missing translations)
- [x] **M1.5 — ui: theme with v1's colors**
  - Note: 2026-11-23: v1's palette is in `DenariiDolorTheme` in `Composables.kt`, besides the two
    files the `Do` line names. v1 set nine color roles for each theme, and Compose Material3 1.4.0
    (the version v1's Compose BOM 2026.09.00 picked, read from its AAR in the Gradle cache) filled
    the rest from Material 3's baseline tokens. `lib/app/theme.dart` writes out all 45 of Flutter's
    color roles for each theme, because Flutter's `ColorScheme` would derive the missing ones from
    the roles that are set. Compose's `surfaceVariant` isn't ported, because Flutter deprecated it:
    it falls back to `surfaceContainerHighest`, which differs slightly and which no widget in
    `material_ui` 1.11.1 reads.
  - Note: 2026-11-23: the chart colors are `VizColors`, a `ThemeExtension` in
    `lib/app/viz_colors.dart`. `DenariiDolorApp.themeMode` picks the light theme, the dark theme,
    or the device's choice, which is the default until M4.6 passes the user's setting.
  - Note: 2026-11-23: `ThemedContent.kt`'s colors style the system bars: a transparent status bar,
    and a navigation bar behind a scrim of `0xE6FFFFFF` (light) or `0x801B1B1B` (dark), each with
    contrasting icons and without Android's contrast scrim. `main` turns on Flutter's edge-to-edge
    mode, as v1's `enableEdgeToEdge()` did, and `systemBarsStyle` styles the bars over every
    screen and behind every app bar. Flutter 3.50.1 draws behind the bars only from Android 10,
    so on Android 8 and 9 the scrim is painted already blended over the surface
    (`withOpaqueNavigationBar`). Without that, the dark theme's navigation bar on API 26 showed
    `#0E0E0E` at a cold start and `#8D8D8D` after the device switched from light, where v1 showed
    about `#1A1A1A`.
  - Note: 2026-11-23: `test/app/theme_test.dart` (12 tests) checks every color role and chart
    color against v1's values, both placeholders in each theme while the device asks for the
    other, the system mode as the device changes, and the bar styles Flutter sends with and
    without the app behind the navigation bar. On `Denarii_API_26` (three-button navigation) and
    `Denarii_API_36` (gesture navigation), screenshots of the sign-in placeholder in each theme, at
    a cold start and after the device switched themes, showed `#FCFCFB` or `#1A1A19` behind the
    content and under both bars, except API 26's navigation bar, which showed `#FFFFFF` or
    `#1B1B1A`: v1's scrims over its surface. Icons were dark in the light theme and light in the
    dark theme. Arabic at 200% text on API 36 laid out right to left in the dark theme. The colors
    were checked against v1's source, not against a v1 build side by side.
  - Depends: M1.1
  - Do:
    - Port the palette in v1's `ThemedContent.kt` and `VizColors.kt` to a Material 3 theme with
      light, dark, and system modes.
  - Accept:
    - The placeholder screen matches v1's colors in both themes.
  - Verify: a widget test per theme; a look on an emulator in both themes
- [x] **M1.6 — ui: money and month text in Dart**
  - Note (from M1.2): `SignInPlaceholderViewModel.placeholderSymbols` sends `.` and no currency
    marks to the bridge's `parse_cents`; build the `NumberSymbols` from the user's locale with
    `package:intl`, as `docs/specs/core.md` ("Money") describes.
  - Note (from M1.4): the ARB files' `int` placeholders have no number format, so Dart writes
    their digits as ASCII in every language (Arabic shows `123456 سنت`). v1 filled `%d` with
    Android's `String.format` and the app's locale, and Java's `Formatter` writes the locale's own
    digits. Check Arabic and Hindi against v1, and give those placeholders a `format` where they
    differ. Amounts and dates reach the ARB messages as preformatted `String`s.
  - Note: 2026-11-23: the maintainer chose the recommended answer to three questions, recorded in
    ADR-0016: currency symbols come from a table that `tool/CurrencySymbols.java` generates from
    Java 25's CLDR 47 data (`intl` has one symbol per currency for every language); money and
    months are written in the locale `resolveAppLocale` picked (`Localizations.localeOf`), not the
    phone's first language as v1 did; and the Arabic regions and Swiss French money that `intl`
    lacks data for are corrected.
  - Note: 2026-11-23: `lib/l10n/money_format.dart` has `formatMoney`, `currencySymbol`,
    `moneyParsingSymbols`, and `formatMonthYear`. The generated `lib/l10n/currency_symbols.dart`
    holds 476 symbols for Java's 266 locales in the seven languages; the "Generated code is
    current" job reruns the tool with the runner's `JAVA_HOME_25_X64`, and the tool refuses any
    other Java release. Amounts reach `intl` as `MicroMoney`, which holds up to
    922,337,203,685,477 cents; past that the whole units are written as an `int`, so every `int`
    is written exactly. Months come from `intl`'s date data (`dateTimeSymbolMap`), not
    `DateFormat`, whose global table Material's localizations also fill.
  - Note: 2026-11-23: JDK 17.0.20.1, v1's test toolchain, reproduces every row of v1's snapshot.
    v1's same code on phones differed from it in 19 of the 29 money rows on `Denarii_API_26` and in
    11 on `Denarii_API_36`. The app writes 172 of the snapshot's 232 money strings and 14 of its 15
    month rows; `test/money_format_test.dart` lists the other 60 strings and the Moroccan months,
    with why. 220 of the 232 match Android 16; the other 12 are negative Arabic-Indic amounts,
    where the app keeps the Arabic letter mark before the minus that v1 wrote and Android 16 drops.
  - Note: 2026-11-23: the sign-in placeholder now sends `moneyParsingSymbols('USD', locale)` to the
    bridge (the note from M1.2) and shows the parsed cents as money above the cents line. On the
    note from M1.4: v1's `%d` wrote ASCII digits in Hindi on JDK 17, API 26, and API 36, as the app
    does. In Arabic, v1 wrote Arabic-Indic digits for ar-SA and ar-EG everywhere (and for plain
    Arabic and ar-AE on API 26), where the app writes ASCII. An ARB `format` can't fix that,
    because the generated Arabic class formats with the pack's locale, `ar`, whose `intl` data
    writes Latin digits; the note under M4.8 carries it.
  - Note: 2026-11-23: `flutter test test/money_format_test.dart` passed (18 tests), and
    `flutter test integration_test` passed (4 tests) on `Denarii_API_26` and `Denarii_API_36`. The
    new integration test reads back, through the Rust core, the amounts Dart writes in v1's 16
    compatibility locales, each in its region's currency and US dollars. Indian grouping of a lakh
    (`1,00,000`) or more doesn't read back, because the core, like v1's parser, accepts only groups
    of three digits. On `Denarii_API_36`, in Arabic (ar-SA) with the dark theme and 200% text,
    typing `-1234.5` showed the amount right to left in Arabic-Indic digits, with its minus and
    `US$`, above the cents line.
  - Depends: M1.1
  - Do:
    - Format amounts and month names with `package:intl` so they match the snapshot tables in v1's
      `MoneySnapshotTest.kt`.
  - Accept:
    - Every row of v1's snapshot tables matches, or a documented difference in `intl`'s locale data
      is listed in the test.
  - Verify: `flutter test test/money_format_test.dart` (expect pass)

**Exit criteria (M1):** the debug build runs on API 26 and API 36 emulators, in English and Arabic,
and shows an amount parsed by Rust.

---

## M2 — The v1 database opens and takes writes in Rust

**Goal:** Rust reads and writes a database that v1 created, with the same balances and the same
rules.

- [x] **M2.1 — store: open SQLCipher from Rust**
  - Note (from M1.2): the bridge runs each call on a pool of worker threads, so two calls can reach
    `store` at the same time; decide how `store` serializes them on its one connection.
  - Note: 2026-11-23: the maintainer accepted ADR-0006 and chose the recommended answer to three
    questions, recorded in ADR-0017: `rusqlite` 0.43.0 pinned exactly, with OpenSSL compiled from
    source on every target (`bundled-sqlcipher-vendored-openssl`), the host included; a fixture
    written by v1's own code with `sqlcipher-android` 4.19.1, from a throwaway instrumented test in
    a scratch copy of v1; and one connection behind a `Mutex` inside `Store`, which answers the
    note from M1.2.
  - Note: 2026-11-23: `denarii-store` has `Store::open`, `Store::user_version`, `Store::close`,
    `DatabaseKey`, and `StoreError` (`docs/specs/store.md`). `rusqlite` 0.43.0 compiles SQLCipher
    4.20.0 over SQLite 3.54.1 and OpenSSL 3.6.3. Opening pins SQLCipher 4's settings with
    `PRAGMA cipher_compatibility = 4`, checks the key against the first page, and turns on foreign
    keys. `rust/clippy.toml` now lists `SQLCipher` in `doc-valid-idents`, so doc comments write
    the name as prose (`.claude/rules/writing.md`) and not as code.
  - Note: 2026-11-23: the fixture is `rust/crates/store/tests/fixtures/v1-sqlcipher-android-4.19.1.db`,
    written on `Denarii_API_36` by a throwaway instrumented test in a scratch copy of v1, whose
    steps and source are in the fixtures folder's `README.md`. `sqlcipher-android` 4.19.1 reports
    SQLCipher 4.19.0; the file is in write-ahead-log mode, at `user_version` 2, with two accounts,
    and has no `android_metadata` table. Installing v1's debug build replaced the Flutter debug
    app on that emulator, and both were uninstalled afterward.
  - Note: 2026-11-23: two findings changed the code. SQLCipher logs warnings and errors to logcat,
    the Apple system log, or stderr once it starts, so `store` turns its log off. And SQLCipher
    4.20.0's process-wide setup has two races, a "sqlcipher not initialized" error and an
    `fclose(NULL)` crash, so `store` sets SQLCipher up once per process under a lock, before it
    opens any file (`docs/specs/store.md`, "Opening"; ADR-0017). Before the lock, the unit tests
    crashed in 10 of 300 runs; with only the log pragmas guarded, `tests/concurrent_open.rs` (64
    stores opened at once) failed in 4 of 877. With the lock, that file passed 3,000 runs in a
    row, and the unit tests and `tests/open.rs` 1,000 each. Neither race is reported upstream yet;
    that is the maintainer's call.
  - Note: 2026-11-23: `cargo test --locked -p denarii-store` passed (7 unit tests, 11 integration
    tests in two files, and 1 doc test). `cargo build --locked --release -p denarii-store` passed
    for `aarch64-linux-android`, `armv7-linux-androideabi`, and `x86_64-linux-android` with NDK
    28.2.13676358, in 54–60 s each from an empty target folder. On the maintainer's machine
    (16 cores), `cargo test --workspace --locked` from an empty target folder took 86.8 s, against
    17.0 s at `d63e806`, and 0.8 s with nothing to rebuild. The 60 s budget in
    `docs/00-overview.md` doesn't say which of these it means; it is unchanged, for the maintainer
    to settle.
  - Depends: M0.7
  - Decide first: accept or change ADR-0006 (`rusqlite` with bundled SQLCipher).
  - Do:
    - Open a SQLCipher 4 database with a raw key, as section 5 of
      `docs/specs/android-v1-formats.md` describes, on the host and for the Android targets.
  - Accept:
    - A database made by `sqlcipher-android` 4.19.1 opens and reads.
  - Verify: `cd rust && cargo test --locked -p denarii-store` (expect pass)
- [x] **M2.2 — store: schema version 2 and forward migrations**
  - Note (from M2.1): `user_version` 0 doesn't prove a new install. An absent or empty file opens
    as an empty database, and so does a 1-byte file, which SQLite reads that way
    (`tests/open.rs`). Decide which files may get a new schema before creating one, and never
    write over a file that isn't empty. v1's file has the tables `accounts`, `budgets`,
    `categories`, `room_master_table`, `sqlite_sequence`, and `transactions`, with no
    `android_metadata`, and is in write-ahead-log mode, which `Store::open` leaves as it is.
  - Note: 2026-11-23: the maintainer chose the recommended answer to three questions, recorded in
    ADR-0018. `Store::open` writes a new schema only into a file that was absent or held 0 bytes
    before it opened; any other file at `user_version` 0, the 1-byte file included, is refused
    and left unchanged. A schema version above 2, a negative one, and a version 2 file whose
    `room_master_table` is missing or holds another identity hash are refused too, and nothing is
    written. A file the store creates switches to write-ahead logging, as Room's files did. The
    alternatives were Room's rule (any version 0 file with no tables gets the schema), a separate
    `Store::create` for setup, checking a version 2 file without `room_master_table` column by
    column, as Room did, and leaving the journal mode to M2.4.
  - Note: 2026-11-23: `Store::open` now brings each file to version 2 before it turns foreign keys
    on (`docs/specs/store.md`, "Schema version 2"). `src/schema.rs` holds Room's version 2
    statements and identity hash, copied from v1's exported schema, and `src/migration.rs`
    chooses the step and runs v1's `MIGRATION_1_2` statements unchanged. `StoreError` gains
    `NewerSchema` and `UnrecognizedSchema`. "Map the four tables" is read as the schema; Rust
    types for the rows come with the use cases that read them (M2.4–M2.6). A new file's
    `sqlite_master` matches the M2.1 fixture's row for row, and a migrated version 1 file has the
    fixture's columns, foreign keys, and indexes. Until M2.3, the version 1 file in the tests is
    one that Rust writes with Room's version 1 statements and the rows of v1's `MigrationTest`.
  - Note: 2026-11-23: three findings. The migration needs foreign keys off, because this build
    turns them on by default and dropping `accounts` then fails on the `RESTRICT` key from
    `transactions`. It resets each `AUTOINCREMENT` counter to the highest ID copied, as v1's did,
    which the format spec now says. And SQLite 3.54.1 turns 19.995 dollars into 2,000 cents,
    because 19.995 times 100 comes out at exactly 1999.5 in binary floating point, while 1.005
    dollars becomes 100.
  - Note: 2026-11-23: `cargo test --locked -p denarii-store migration` passed (12 tests). The
    crate's 19 unit tests, 12 integration tests in two files, and doc test passed 100 runs in a
    row. Each of five deliberate breakages failed a test: a non-unique index after the migration,
    one missing space in the new schema, foreign keys left on, any version 0 file taken for a new
    one, and no switch to write-ahead logging.
  - Depends: M2.1
  - Do:
    - Map the four tables in section 6 of the format spec, continue from `user_version` 2, and
      keep Room's bookkeeping table.
  - Accept:
    - A version 2 database opens with no row changed; a version 1 database migrates as v1 did.
  - Verify: `cd rust && cargo test --locked -p denarii-store migration` (expect pass)
- [x] **M2.3 — store: a database fixture written by v1**
  - Note (from M2.1): `rust/crates/store/tests/fixtures/README.md` has the steps that wrote M2.1's
    fixture: a throwaway instrumented test in a scratch copy of v1 at `6d57526`, which builds
    offline with the maintainer's Gradle cache, run on `Denarii_API_36` and copied out with
    `adb exec-out run-as`. F3 can be made the same way, through v1's DAOs and use cases.
  - Note (from M2.2): give F4 balances of 0.005, 1.005, 19.995, and -12.345 dollars, which Rust's
    SQLite 3.54.1 migrates to 1, 100, 2,000, and -1,235 cents (`docs/specs/store.md`, "Migrating
    from version 1"). Keep two copies of the file: one at version 1, and one that v1's own
    `MIGRATION_1_2` migrated on the emulator. Then a test can check that `Store::open` migrates
    the first to the same rows as the second, which shows whether v1's SQLite rounded the same
    way. The tests in `src/migration.rs` write a version 1 file from Room's statements, and show
    how.
  - Note: 2026-11-23: the fixtures are `v1-f3-ledger.db`, `v1-f4-version-1.db`, and
    `v1-f4-migrated-by-v1.db` in `rust/crates/store/tests/fixtures/`, written on
    `Denarii_API_36` by a throwaway instrumented test in a scratch copy of v1. The folder's
    `README.md` has every row, the writer's source, and the steps. F3 went through v1's own
    `DatabaseHolder.open`, `DefaultDataInitializer`, use cases, and repositories, with the budget
    check's clock fixed at 2026-11-23T12:00:00Z in UTC. It holds 10 transactions of every type,
    two of them edited and one an over-budget expense saved with `allowOverBudget`, the gap of an
    eleventh that was deleted, two budgets, non-ASCII names, emoji, and Arabic text, and a
    malformed transfer as row 12. The malformed transfer went through
    `TransactionDao.insert`, as v1's `TransactionRepositoryImplTest` writes one, because no use
    case writes one. F4's version 1 file came from Room's `MigrationTestHelper` with write-ahead
    logging on, and its migrated copy from `DatabaseHolder.open`. The emulator ran with
    `-read-only`, so it kept nothing, and both apps were uninstalled afterward.
  - Note: 2026-11-23: v1's own migration on the emulator gave the same cents as this build's
    SQLite 3.54.1 for all 12 money values in F4, among them 1.005 dollars to 100 cents, 19.995 to
    2,000, and 2.675 to 268. F4 is kept as two files, as the note from M2.2 asked, so the tests
    compare the store's migration with v1's row for row. `denarii-store` gains `denarii-core` as a
    dev-dependency for `Transaction::account_impacts`. The tests map rows to `core::Transaction`
    themselves, because the store's row types come with the use cases (M2.4).
  - Note: 2026-11-23: `cargo test --locked -p denarii-store fixture` passed: 5 unit tests and 2
    integration tests (`src/store.rs`, `src/migration.rs`, `tests/open.rs`). Each of three
    deliberate breakages failed a test: a transfer that doesn't debit its source account in
    `core`, a malformed transfer read as a transfer to account 1, and a migration that truncates
    amounts instead of rounding them.
  - Depends: M2.2
  - Do:
    - Build fixtures F3 and F4 of the format spec with the v1 app, including a malformed transfer
      (BUG-06), and commit them under `rust/crates/store/tests/fixtures/`.
  - Accept:
    - Rust reproduces every stored balance from the fixture's transactions.
  - Verify: `cd rust && cargo test --locked -p denarii-store fixture` (expect pass)
- [x] **M2.4 — store: transaction writes**
  - Note (from M2.3): fixture F3 (`rust/crates/store/tests/fixtures/v1-f3-ledger.db`) is a ready
    budget case. Category 4 has a 40,000-cent budget, and November 2026 already holds 42,345 cents
    of its expenses in UTC, so any further expense there fails with `BUDGET_EXCEEDED` unless the
    caller allows it. October's 35,000 cents fall outside that month. The F3 tests in
    `src/store.rs` map rows to `core::Transaction` with `TransactionKind::from_stored`; the use
    cases' row mapping can replace that helper.
  - Note: 2026-11-24: the maintainer chose the recommended answer, recorded in ADR-0019: Dart
    passes each month's first moment and the next month's, and Rust gets no time-zone crate. The
    alternative was a crate such as `jiff` with a time-zone database, which also needs the
    zone's IANA name, which Dart's core libraries don't give.
  - Note: 2026-11-24: `denarii-store` has `Store::add_transaction`, `Store::update_transaction`,
    `Store::delete_transaction`, and `OverBudget` in `src/transactions.rs`
    (`docs/specs/store.md`, "Transaction writes"). `denarii-core` gains `MonthBounds`, which
    accepts a span of 27 to 32 days, and `Transaction::budgeted_amount`. `StoreError` gains
    `Domain`, `TransactionNotFound`, `DateOutsideMonth`, `Overflow`, and `MissingAccount`.
    `denarii-core` moves from the store's dev-dependencies to its dependencies, and `Cargo.lock`
    doesn't change. v1's checks now run inside the write's `BEGIN IMMEDIATE` transaction rather
    than before it, and each balance is added to in Rust with checked arithmetic rather than in
    SQL. The spec lists five differences from v1: three affect only a request v1's screens never
    sent or a file v1's checks never left, and two change only where the work happens. The F3 helper in `src/store.rs` is unchanged; the new tests
    read rows with the use cases' own mapping.
  - Note: 2026-11-24: the month limits come from tzdata 2026e, through Python's `zoneinfo`: over
    599 zones and every month from 1970 to 2100, the shortest month lasts 27.96 days
    (`America/Adak`, February 1975) and the longest 31.29 days (`Antarctica/Vostok`, January
    1994). Dart's `DateTime(2023, 10, 1)` under `TZ=America/Asuncion` is 2023-10-01T04:00:00Z,
    the first moment of a day whose midnight was skipped, as v1's `atStartOfDayIn` gives.
  - Note: 2026-11-24: `cargo test --locked -p denarii-store` passed: 57 unit tests, 33 of them new
    in `src/transactions.rs`, 13 integration tests in two files, and 1 doc test. The new tests port
    every case of v1's `TransactionCrudUseCaseTest` and `TransactionRepositoryImplTest`, and every
    case of `ValidateTransactionUseCaseTest` but `repositoryErrorBecomesFailureInsteadOfCrash`,
    whose throwing fake repository has no Rust counterpart. They add F3's budget in November and
    October, edits and deletes of F3's rows with its balances recomputed after each, rollback
    after the row is written, and eight threads writing at once. Each of eight deliberate
    breakages failed a test: no budget check, the next month's first moment counted, an edit
    counting its own amount, no balance change, a commit between the row and its balance change,
    a malformed transfer read as a transfer to account 1, type names read without regard to
    case, and no check that the month holds the date.
  - Depends: M2.2
  - Decide first: whether Dart passes each month's start and end, or Rust gets a time-zone crate.
  - Do:
    - Port `AddTransactionUseCase`, `UpdateTransactionUseCase`, `DeleteTransactionUseCase`, and the
      budget check of `ValidateTransactionUseCase`, applying `core::balance_deltas` in the same
      database transaction as the row.
  - Accept:
    - A failed write changes nothing; an expense over budget fails with `BUDGET_EXCEEDED` unless
      the caller allows it.
  - Verify: `cd rust && cargo test --locked -p denarii-store` (expect pass)
- [ ] **M2.5 — store: categories, accounts, and budgets**
  - Depends: M2.4
  - Do:
    - Port `CategoryUseCases`, `AccountUseCases`, and `BudgetUseCases`: name rules, duplicates, the
      protected defaults in `core::defaults`, and the in-use checks.
  - Accept:
    - Each `DomainError` those use cases raised in v1 is raised in the same case.
  - Verify: `cd rust && cargo test --locked -p denarii-store` (expect pass)
- [ ] **M2.6 — store: seed the defaults at setup**
  - Depends: M2.5
  - Do:
    - Port `DefaultDataInitializer`, taking the default names in the user's language from Dart.
  - Note (from M1.4): the names are `defaultAccountCash`, `defaultAccountSavings`, and the three
    `defaultCategory…` messages. Outside a widget, get them with
    `lookupAppLocalizations(resolveAppLocale(PlatformDispatcher.instance.locales, AppLocalizations.supportedLocales))`,
    which is synchronous.
  - Accept:
    - A new database holds accounts 1–2 and categories 1–3 with v1's icons.
  - Verify: `cd rust && cargo test --locked -p denarii-store seed` (expect pass)
- [ ] **M2.7 — report: monthly report, search, and CSV**
  - Note (from M2.4): take a report's month as `core::MonthBounds`, which Dart computes
    (ADR-0019), and select its rows from `start` inclusive to `next_start` exclusive, as the
    budget check in `rust/crates/store/src/transactions.rs` does. v1's search turned a typed
    `YYYY-MM-DD` into the first moment of that day, and its end date into one millisecond before
    the next day's first moment, in the phone's time zone (`DateUtils.parseIsoDateToStartOfDayEpochMillis`
    and `parseIsoDateToEndOfDayEpochMillis`), so those moments come from Dart too.
  - Depends: M2.4
  - Do:
    - Port `GenerateReportUseCase`, `SearchTransactionUseCase`, `SearchFilterParser`, and
      `ReportCsvFormatter`, keeping the CSV format and the formula guard (CS-20).
  - Accept:
    - CSV output matches fixture F8 byte for byte.
  - Verify: `cd rust && cargo test --locked -p denarii-report` (expect pass)

**Exit criteria (M2):** Rust tests open the v1 fixture database, reproduce its balances, and
reject each invalid write with v1's error.

---

## M3 — Existing installs sign in

**Goal:** A phone with v1.x data updates to the new build and signs in with the same PIN, answer,
and fingerprint, and sees the same balances.

- [ ] **M3.1 — vault: decide where hardware-key work runs**
  - Depends: M1.2
  - Decide first: accept or change ADR-0007, after a spike that calls the Keystore from Rust on
    API 26.
  - Do:
    - Build the spike, record its findings in ADR-0007, and add `kotlin.md` and `android.md` rules
      for the code in `android/`.
  - Accept:
    - ADR-0007 is `Accepted` or superseded.
  - Verify: the maintainer's decision (no command)
- [ ] **M3.2 — android: Keystore and biometric code**
  - ⏳ needs an Android phone with v1 installed
  - Note (from M1.4): once the APK carries translated Android resources (`androidx.biometric` has
    some), Android moves the first phone language that they or English match to the front before
    Flutter sees the list (ADR-0015). Check with `aapt2 dump configurations` that each of the six
    pack languages has Android resources too; otherwise a phone set to Hindi then English opens in
    English.
  - Depends: M3.1
  - Do:
    - Use the aliases and parameters in section 2 of the format spec, never re-creating a key while
      a profile exists.
  - Accept:
    - On a phone with v1 data, the new build opens the profile with v1's storage key.
  - Verify: an instrumented test with test-only aliases; a manual check on the phone
- [ ] **M3.3 — vault: profile, key wrapping, and lockout**
  - Depends: M3.2
  - Decide first: the cryptography crates (`docs/02-dependency-policy.md`).
  - Do:
    - Port `SecurityProfileService`, keeping CS-10, CS-13, CS-14, and CS-15, with golden tests from
      v1's `CryptoKnownAnswerTest` and `SecurityProfileGoldenTest` (fixtures F1 and F5).
  - Accept:
    - v1's golden values pass unchanged.
  - Verify: `cd rust && cargo test --locked -p denarii-vault` (expect pass)
- [ ] **M3.4 — vault: upgrade v1.0.x profiles**
  - Depends: M3.3
  - Decide first: keep the v1.0.x upgrade (which needs `security-crypto` in Kotlin), or drop it;
    and how to treat 4- and 5-digit PINs from before 2026-09-21 (section 4 of the format spec).
  - Do:
    - Port `LegacyProfileStorage` and `LegacySecurityProfile` as decided.
  - Accept:
    - Fixture F9 upgrades and signs in.
  - Verify: an instrumented test on an emulator
- [ ] **M3.5 — auth: setup, sign-in, and recovery screens**
  - Note (from M2.1): once the bridge depends on `denarii-store`, every Flutter build compiles
    SQLCipher and OpenSSL for each ABI through `hook/build.dart`, which takes about a minute per
    ABI from an empty target folder (ADR-0017). Check `flutter build apk` and the CI job's time
    then, and map each `StoreError` variant to a code for the screens.
  - Note (from M2.2): `Store::open` can also return `NewerSchema` and `UnrecognizedSchema`
    (ADR-0018). Both leave the file as it was, and neither may lead the app to delete it.
  - Note (from M1.2): the sign-in placeholder now has a route (`SignInPlaceholderRoute`) and a view
    model with M1.2's amount field. Remove the field with the placeholder, and point
    `integration_test/bridge_test.dart`'s screen check at a screen that still calls the bridge.
  - Note (from M1.4): remove `placeholderAmountInCents` and `placeholderAmountRejected` from the
    seven ARB files with the placeholder, and regenerate the localizations.
  - Note (from M1.1): replace `LockedSessionLock` in `lib/main.dart` with a `SessionLock` over the
    vault; wrap `SignedInTree` in the session scope (ADR-0010); and push the sign-in screens on a
    `Navigator` inside the sign-in flow, not the app's root `Navigator`, so unlocking removes them.
    The sign-in placeholder is `lib/features/auth/ui/sign_in_placeholder_screen.dart`.
  - Depends: M3.3, M1.4
  - Do:
    - Port `LoginScreen`, `LoginViewModel`, and the overlay guard (CS-11).
  - Accept:
    - Setup, sign-in, wrong PIN with lockout, and recovery work on an emulator.
  - Verify: `flutter test`; `flutter test integration_test` on an emulator
- [ ] **M3.6 — auth: biometric sign-in through a `CryptoObject`**
  - ⏳ needs an Android phone with an enrolled fingerprint
  - Depends: M3.5
  - Do:
    - Keep CS-07: enrolling a new fingerprint invalidates the wrap, and the PIN still works.
  - Accept:
    - Biometric sign-in works, and stops working after a new enrollment.
  - Verify: a manual check on the phone
- [ ] **M3.7 — session: timeout, lock, and wipe**
  - Note (from M2.3): after Room migrated a database on an emulator, an empty `<name>.lck` file
    was left beside it, and `Context.deleteDatabase` didn't remove it. A database Room only
    created had none. A phone whose database v1 migrated may hold an empty
    `databases/denarii_dolor.db.lck`, which holds no data; decide whether wipe deletes it
    (`rust/crates/store/tests/fixtures/README.md`, fixture F4).
  - Depends: M3.5
  - Do:
    - Keep CS-01, CS-02, CS-04, CS-16, and CS-17.
  - Accept:
    - The session ends after 5 minutes on the monotonic clock; wipe leaves what invariant 16 of the
      format spec says.
  - Verify: unit tests over a fake clock; an instrumented test of wipe
- [ ] **M3.8 — android: manifest controls**
  - Note (from M1.1): the release manifest already exports only the launcher `activity-alias`
    among activities, and has no `INTERNET` permission (ADR-0013 keeps it in debug and profile
    builds). `allowBackup`, the backup and data-extraction rules, and `HIDE_OVERLAY_WINDOWS` aren't
    set yet. AndroidX merges in `androidx.startup.InitializationProvider` (not exported) and
    `androidx.profileinstaller.ProfileInstallReceiver` (exported, but only to callers that hold
    `android.permission.DUMP`); decide whether to keep them.
  - Depends: M1.1
  - Do:
    - Keep `allowBackup="false"`, the data-extraction rules, `taskAffinity=""` (CS-21), one
      exported activity, and no `INTERNET` permission (ADR-0008).
  - Accept:
    - The merged release manifest shows each control.
  - Verify: inspect `build/app/intermediates/merged_manifest/release/` (expect each control)

**Exit criteria (M3):** on a phone with v1.x and real-looking data, installing the new debug build
over it keeps the data, and the same PIN, answer, and fingerprint sign in.

---

## M4 — Every v1 screen works on Android

**Goal:** Each flow in the README's user guide works in the new app as it does in v1.

- [ ] **M4.1 — dashboard: balances and the spending chart**
  - Note (from M1.6): v1's rows wrote an amount with `formatSignedAmount`: an expense as
    `formatMoney(-cents)`, an income with `showPlus: true`, and a transfer as is. That sign is only
    display; totals and balances still come from `Transaction::balance_impact` in Rust.
  - Note (from M1.5): take the chart's and the budgets' colors from
    `Theme.of(context).extension<VizColors>()` as v1's dashboard did: `series1` for the chart's
    columns and a budget within its limit, `statusWarning` for one near it, `statusCritical` for
    one over it, and `meterTrack` for the progress bar's track.
  - Note (from M1.5): two edge-to-edge details of v1's AndroidX 1.13.0 aren't ported. From Android
    15, Android ignores the navigation bar's color, so AndroidX painted v1's scrim as a view behind
    the bar; the Flutter app paints none, so content that scrolls behind the bar shows through
    without a scrim. On Android 9–14, v1 also let content reach into a display cutout, which
    Flutter's default keeps clear in landscape. Match them, or record the difference, on the first
    screen that scrolls behind the navigation bar.
  - Depends: M3.5
  - Decide first: the chart package.
- [ ] **M4.2 — transaction: the add and edit form**
  - Depends: M3.5
  - Note: keeps one write at a time (`isSaving`) and the over-budget confirmation.
  - Note (from M1.6): fill the edit field with v1's plain input text (`Cents::to_input`), not
    `formatMoney`: the core doesn't read back Indian grouping of a lakh or more.
  - Note (from M2.4): every add and edit passes the date's month as `core::MonthBounds`
    (ADR-0019). Write one Dart helper that takes the date's local year and month and returns
    `DateTime(year, month, 1)` and `DateTime(year, month + 1, 1)` as epoch milliseconds, and test
    it under `TZ=America/Asuncion`, where October 2023 must start at 2023-10-01T04:00:00Z. Pass
    the time now as an add's `created_at`, and as the date of a transaction whose date field was
    left empty, as v1 did. Map the store's errors as v1's `UiMessage.fromError` did:
    `StoreError::Domain` by its `DomainError::code`, `TransactionNotFound` to
    `errorItemNotFound`, and the rest to `genericError`. Save with `OverBudget::Refuse` first, and
    with `OverBudget::Allow` once the user confirms.
- [ ] **M4.9 — claude: the `new-feature` skill**
  - Note: 2026-11-23: split from M0.8 by the user's choice, because the layers the skill walks
    through (a `store` use case, the bridge, a view model, a screen, and ARB strings) don't all
    exist until M4.2. It's listed here so that M4.3–M4.7 can use it.
  - Depends: M4.2
  - Do:
    - Write `.claude/skills/new-feature/SKILL.md` for Flutter and Rust, starting from v1's skill
      (`git show 6d57526:.claude/skills/new-feature/SKILL.md`), with M4.2's transaction form as
      the example to copy, and add it to the skills table in `CLAUDE.md`.
  - Accept:
    - The skill names only commands and files that exist.
  - Verify: run the skill's commands once (expect success)
- [ ] **M4.3 — search: filters and results**
  - Depends: M2.7, M3.5
- [ ] **M4.4 — category: manage categories, accounts, and budgets**
  - Depends: M2.5, M3.5
- [ ] **M4.5 — report: monthly report, CSV and PDF export, and sharing**
  - Depends: M2.7, M3.5
  - Decide first: how PDFs are rendered.
- [ ] **M4.6 — settings: currency, theme, and biometric sign-in**
  - Note (from M1.5): pass the theme setting to `DenariiDolorApp.themeMode`. v1's `dark_mode` in
    `ui_prefs` means the dark theme when true, the light theme when false, and the device's choice
    when missing. v1 switched themes at once, while `MaterialApp` animates the change over 200 ms
    (`themeAnimationDuration`).
  - Note (from M1.6): pass the saved currency to `formatMoney` and `moneyParsingSymbols`. v1 kept
    it as `currency` in `ui_prefs`; an install with none showed US dollars, and setup saved the
    region's currency (`Currencies.regionDefault`). The sign-in placeholder uses
    `SignInPlaceholderViewModel.currencyCode`, US dollars, until M3.5 replaces it.
  - Depends: M3.6
- [ ] **M4.7 — legal: help, legal documents, welcome tour, and rating prompt**
  - Depends: M3.5
  - Note: `PRIVACY.md`, `THIRD_PARTY_NOTICES.md`, and `licenses/` ship as app assets, as
    `.claude/rules/legal-docs.md` describes.
- [ ] **M4.8 — ui: accessibility, right-to-left, and large text**
  - Note (from M1.6): the ARB messages' `int` placeholders write ASCII digits in Arabic, where v1
    wrote Arabic-Indic digits for ar-SA and ar-EG. An ARB `format` uses the pack's locale (`ar`,
    Latin digits), so the region's digits need the number passed already written, for example
    with `intl`'s data for `ar_EG`. Decide whether to match v1 before this check.
  - Depends: M4.1–M4.7
  - Do:
    - Check every screen with TalkBack, in Arabic, and at 200% text size, as the flutter rule's
      "Before finishing" lists.

Each M4 task gets its `Do`, `Accept`, and `Verify` lines when it starts, from v1's screen of the same
name at `6d57526`.

**Exit criteria (M4):** side by side with v1 on the same emulator, each user-guide flow in
`README.md` gives the same result.

---

## M5 — Backups restore every v1 format

**Goal:** Every backup file v1 wrote restores in the new app, and new backups follow the same
rules (CS-28).

- [ ] **M5.1 — backup: read and write format version 1**
  - Depends: M3.3
  - Do:
    - Port `BackupFile`, `BackupCodec`, and `BigEndianData`, with fixture F2 and the error cases
      in invariant 13 of the format spec.
  - Accept:
    - v1's golden backup files restore; damaged and truncated ones fail with v1's errors.
  - Verify: `cd rust && cargo test --locked -p denarii-backup` (expect pass)
- [ ] **M5.2 — backup: create and restore screens**
  - Depends: M5.1, M4.6
  - Do:
    - Port `BackupScreen` and `BackupViewModel`, with an atomic restore.
  - Accept:
    - A failed restore leaves the database unchanged.
  - Verify: an integration test on an emulator
- [ ] **M5.3 — claude: the `schema-migration` skill**
  - Note: 2026-11-23: split from M0.8 by the user's choice, because the store's migrations (M2.2)
    and the backup formats (M5.1) that a schema change must update don't exist yet.
  - Note (from M2.2): `rust/crates/store/src/migration.rs` chooses the step from `user_version`
    and runs each migration in one immediate transaction, with foreign keys off. The first schema
    version after 2 must also decide what `room_master_table` holds from then on (ADR-0018).
  - Depends: M2.2, M5.1
  - Do:
    - Write `.claude/skills/schema-migration/SKILL.md`, starting from v1's `room-migration` skill
      (`git show 6d57526:.claude/skills/room-migration/SKILL.md`): a forward-only migration from
      `user_version` 2 with its fixture and test, and a new backup `FORMAT_VERSION` that still
      restores every older one (CS-28). Add it to the skills table in `CLAUDE.md`.
  - Accept:
    - The skill names only commands and files that exist.
  - Verify: run the skill's commands once (expect success)

**Exit criteria (M5):** a backup made by v1 restores in the new app, and one made by the new app
restores in the new app on another phone.

---

## M6 — The Android release replaces v1 on Google Play

**Goal:** The new app ships as an update to v1 on Google Play.

- [ ] **M6.1 — ci: signed release pipeline**
  - Depends: M4.8, M5.2
  - Do:
    - Rebuild v1's tag-driven pipeline for Flutter, split into `ci-gate`, `build`, and `publish`
      jobs (CS-19), with a build number above v1's highest `versionCode`.
- [ ] **M6.2 — legal: notices, privacy policy, and Data safety**
  - Note (from M2.1): the store ships SQLCipher 4.20.0 (BSD-3-Clause), SQLite 3.54.1 (public
    domain), and OpenSSL 3.6.3's `libcrypto` (Apache-2.0), with the seven crates in the register in
    `docs/02-dependency-policy.md`. Compare `sqlcipher-src`'s amalgamation with one built from
    SQLCipher's signed v4.20.0 tag (ADR-0017).
  - Note (from M1.1): the app ships `material_ui`, `cupertino_ui`, `intl`, `clock` (Apache-2.0),
    and `path`, besides Flutter's own packages; see the register in `docs/02-dependency-policy.md`.
  - Note (from M1.2): `libdenarii_bridge.so` ships 30 third-party crates, listed with their
    licenses in ADR-0014; `allo-isolate` is Apache-2.0 only, and `dart-sys` compiles the Dart SDK's
    `dart_api_dl.c` (BSD-3-Clause). The app also ships the `flutter_rust_bridge` Dart package (MIT)
    and the packages its runtime imports (`logging`, `async`, `args`, and `web` from the Dart team,
    BSD-3-Clause, and `build_cli_annotations`, MIT); check which the release build keeps. The build
    hook's packages run only during the build and don't ship.
  - Note (from M1.6): `lib/l10n/currency_symbols.dart` holds data from Unicode CLDR 47, which
    `tool/CurrencySymbols.java` reads from Java 25, under the Unicode License V3 (the JDK's
    `legal/java.base/cldr.md` has its text). `intl`'s own number and date data come from CLDR 48.
  - Depends: M6.1
  - Do:
    - Rewrite `THIRD_PARTY_NOTICES.md` and `licenses/` for the new dependencies; review
      `PRIVACY.md`; tell the maintainer what the Play Data safety form needs.
- [ ] **M6.3 — build: obfuscation and size**
  - Note (from M2.1): a scratch library holding only `rusqlite` with SQLCipher and OpenSSL was
    6.2 MB stripped for `arm64-v8a`, against 2.1 MB for v1's `libsqlcipher.so` (ADR-0017).
  - Note (from M1.2): `libdenarii_bridge.so` is 661 KB (`arm64-v8a`), 444 KB (`armeabi-v7a`), and
    695 KB (`x86_64`) in M1.2's release APK. It includes backtrace symbolization (`backtrace`,
    `gimli`, `object`, `addr2line`), which `flutter_rust_bridge` turns on in `allo-isolate`.
  - Depends: M6.1
  - Do:
    - Build with `--obfuscate --split-debug-info`, keep the symbols private, and record the size
      against v1's.
- [ ] **M6.4 — release: upgrade test from v1.x**
  - Depends: M6.1
  - Do:
    - Install each v1.x release on API 26 and API 36, add data, update to the release build, and
      sign in.
- [ ] **M6.5 — docs: platforms, quality, and release documents**
  - Depends: M6.1
  - Do:
    - Write `docs/03-platforms.md`, `docs/04-quality.md`, and `docs/05-release.md`.

**Exit criteria (M6):** the upgrade from v1.x passes on the closed testing track, and the
maintainer approves a staged rollout to production.

---

## M7 — iOS

**Goal:** The app ships on the App Store with the same features and protections.

- [ ] **M7.1 — ios: Keychain and Secure Enclave design, and the Swift code**
  - Note (from M2.1): `bundled-sqlcipher-vendored-openssl` compiles OpenSSL for Apple targets too,
    instead of using CommonCrypto; check the iOS build of `denarii-store` (ADR-0017).
  - Note (from M1.2): add the iOS targets to `rust/rust-toolchain.toml`, add `staticlib` to the
    bridge's `crate-type` if the iOS build links it statically, and check `hook/build.dart` on iOS.
  - Depends: M6.4
  - Decide first: how the device key, the storage key, and the biometric key map to the Keychain
    and the Secure Enclave, in a new decision record; add a `swift.md` rule.
- [ ] **M7.2 — auth: Face ID and Touch ID sign-in**
  - Depends: M7.1
- [ ] **M7.3 — release: privacy manifest, App Store listing, and TestFlight**
  - Depends: M7.2

**Exit criteria (M7):** a TestFlight build passes every flow in the README's user guide.

---

## Backlog (each needs a trigger or a decision)

| Item | Trigger / reason |
| :- | :- |
| A v1.x hotfix branch cut at `6d57526`, with the Kotlin build and `cd.yml` restored on it | A v1.x bug on Google Play that can't wait for M6 |
| Moving a backup between Android and iOS | M7 ships and a user asks to move their records between platforms |
| A feature list file tied to roadmap tasks | Parity tracking in M4 outgrows the roadmap's task list |
| Desktop or web builds | The maintainer decides to sell beyond phones, in a new decision record |
| Caching Flutter, packages, Gradle, and the codegen between CI runs with `actions/cache`, which M1.3 left out by the maintainer's choice | A `ci.yml` run on a pull request takes longer than 30 minutes |
