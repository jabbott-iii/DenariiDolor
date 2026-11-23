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
- [ ] **M0.8 — claude: skills for the new stack**
  - Note: 2026-11-23: `ci-gates` now runs the Rust checks. `new-feature`, `room-migration`, and
    `dependency-upgrade` were Kotlin-only and left with ADR-0003; their text is at `6d57526`.
  - Note: 2026-11-23: at the maintainer's request, the skills `next-task` and `fix-doc-drift`
    are copied unchanged from Doctrina commit `96b996b`. They take their values from the rules'
    "Project details" and name no project command, so the new stack needs no rewrite of them.
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

- [ ] **M1.1 — ui: create the Flutter project at the repository root**
  - Note: 2026-11-23: the maintainer chose Flutter 3.50.1 pinned in `pubspec.yaml` with iOS 15
    and later (ADR-0009), `ChangeNotifier` view models with no package (ADR-0010), and Flutter's
    `Navigator` with no package (ADR-0011). The maintainer also accepted ADR-0012: generated code
    is committed, and CI checks that it is current.
  - Note: 2026-11-23: Flutter 3.50.1 is installed on the maintainer's machine, in `~/flutter`. Its
    Android SDK has platform 37, build-tools 36.0.0, the command-line tools, NDK 28.2.13676358,
    and the emulators `Denarii_API_26` and `Denarii_API_36` (Google APIs x86_64 images), which
    both boot.
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
- [ ] **M1.2 — bridge: call Rust from Dart**
  - Note: 2026-11-23: `flutter_rust_bridge` 2.13.0 was tested against Flutter 3.44, so check it
    on Flutter 3.50.1 (ADR-0009) before anything else.
  - Note: 2026-11-23: NDK 28.2.13676358, the version Flutter 3.50's Gradle plugin defaults to, is
    installed on the maintainer's machine; build the Rust library with the same version.
  - Depends: M1.1
  - Decide first: accept or change ADR-0005 (`flutter_rust_bridge`).
  - Do:
    - Add the `bridge` crate, generate the Dart bindings, and build the Rust library for
      `arm64-v8a`, `armeabi-v7a`, `x86_64`, and `x86`.
  - Accept:
    - The placeholder screen shows the cents that `core::parse_cents` returned for a typed amount.
  - Verify: `flutter test integration_test` on an emulator (expect pass)
- [ ] **M1.3 — ci: Flutter checks and the Android build in CI**
  - Depends: M1.2
  - Do:
    - Add jobs to `ci.yml` for the Flutter check commands and a debug app bundle, with actions
      pinned to commit SHAs (CS-08); add the `pub` ecosystem to `dependabot.yml`.
    - Regenerate the bridge bindings and the localizations in CI, and fail on any difference from
      the committed files (ADR-0012).
  - Accept:
    - A pull request runs the Rust and Flutter jobs and blocks on either.
  - Verify: a pull request's checks (expect green)
- [ ] **M1.4 — i18n: port v1's strings to ARB files**
  - Depends: M1.1
  - Do:
    - Convert `app/src/main/res/values*/strings.xml` at `6d57526` into `lib/l10n/app_*.arb` for
      the seven languages, keeping each key, its plural forms, and a description for translators.
  - Accept:
    - Every v1 string and plural has an ARB entry in each language, and Arabic lays out right to
      left.
  - Verify: `flutter gen-l10n` (expect no missing translations)
- [ ] **M1.5 — ui: theme with v1's colors**
  - Depends: M1.1
  - Do:
    - Port the palette in v1's `ThemedContent.kt` and `VizColors.kt` to a Material 3 theme with
      light, dark, and system modes.
  - Accept:
    - The placeholder screen matches v1's colors in both themes.
  - Verify: a widget test per theme; a look on an emulator in both themes
- [ ] **M1.6 — ui: money and month text in Dart**
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

- [ ] **M2.1 — store: open SQLCipher from Rust**
  - Depends: M0.7
  - Decide first: accept or change ADR-0006 (`rusqlite` with bundled SQLCipher).
  - Do:
    - Open a SQLCipher 4 database with a raw key, as section 5 of
      `docs/specs/android-v1-formats.md` describes, on the host and for the Android targets.
  - Accept:
    - A database made by `sqlcipher-android` 4.19.1 opens and reads.
  - Verify: `cd rust && cargo test --locked -p denarii-store` (expect pass)
- [ ] **M2.2 — store: schema version 2 and forward migrations**
  - Depends: M2.1
  - Do:
    - Map the four tables in section 6 of the format spec, continue from `user_version` 2, and
      keep Room's bookkeeping table.
  - Accept:
    - A version 2 database opens with no row changed; a version 1 database migrates as v1 did.
  - Verify: `cd rust && cargo test --locked -p denarii-store migration` (expect pass)
- [ ] **M2.3 — store: a database fixture written by v1**
  - ⏳ needs an Android emulator to run the v1 app at `6d57526`
  - Depends: M2.2
  - Do:
    - Build fixtures F3 and F4 of the format spec with the v1 app, including a malformed transfer
      (BUG-06), and commit them under `rust/crates/store/tests/fixtures/`.
  - Accept:
    - Rust reproduces every stored balance from the fixture's transactions.
  - Verify: `cd rust && cargo test --locked -p denarii-store fixture` (expect pass)
- [ ] **M2.4 — store: transaction writes**
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
  - Accept:
    - A new database holds accounts 1–2 and categories 1–3 with v1's icons.
  - Verify: `cd rust && cargo test --locked -p denarii-store seed` (expect pass)
- [ ] **M2.7 — report: monthly report, search, and CSV**
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
  - Depends: M3.5
  - Do:
    - Keep CS-01, CS-02, CS-04, CS-16, and CS-17.
  - Accept:
    - The session ends after 5 minutes on the monotonic clock; wipe leaves what invariant 16 of the
      format spec says.
  - Verify: unit tests over a fake clock; an instrumented test of wipe
- [ ] **M3.8 — android: manifest controls**
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
  - Depends: M3.5
  - Decide first: the chart package.
- [ ] **M4.2 — transaction: the add and edit form**
  - Depends: M3.5
  - Note: keeps one write at a time (`isSaving`) and the over-budget confirmation.
- [ ] **M4.3 — search: filters and results**
  - Depends: M2.7, M3.5
- [ ] **M4.4 — category: manage categories, accounts, and budgets**
  - Depends: M2.5, M3.5
- [ ] **M4.5 — report: monthly report, CSV and PDF export, and sharing**
  - Depends: M2.7, M3.5
  - Decide first: how PDFs are rendered.
- [ ] **M4.6 — settings: currency, theme, and biometric sign-in**
  - Depends: M3.6
- [ ] **M4.7 — legal: help, legal documents, welcome tour, and rating prompt**
  - Depends: M3.5
  - Note: `PRIVACY.md`, `THIRD_PARTY_NOTICES.md`, and `licenses/` ship as app assets, as
    `.claude/rules/legal-docs.md` describes.
- [ ] **M4.8 — ui: accessibility, right-to-left, and large text**
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
  - Depends: M6.1
  - Do:
    - Rewrite `THIRD_PARTY_NOTICES.md` and `licenses/` for the new dependencies; review
      `PRIVACY.md`; tell the maintainer what the Play Data safety form needs.
- [ ] **M6.3 — build: obfuscation and size**
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
