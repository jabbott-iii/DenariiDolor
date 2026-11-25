# CLAUDE.md — Denarii Dolor

Denarii Dolor is a **production, proprietary app** sold on Google Play: a single-user, fully offline personal-finance
ledger (expenses, income, transfers, budgets, monthly reports, encrypted backups). Each user's financial records exist only on
their device, encrypted. Every change ships to paying users whose data can't be repaired by hand, so data integrity and
security come before speed, novelty or elegance.

The app is being rewritten as a Flutter app over a Rust core, for Android and then iOS (ADR-0001 in `docs/decisions.md`).
v1, the Kotlin Android app that Google Play serves today, left the working tree on 2026-11-23 (ADR-0003). Read any of its
files with `git show 6d57526:<path>`. The new app must open what v1 left on users' phones, as it is (ADR-0002).

## Status

As of 2026-11-25, all of M0's tasks, M1.1, M1.2, M1.4, M1.5, M1.6, all of M2's tasks, M3.1, M3.2, and M3.8 are done, and M1.3's Flutter jobs are in
`ci.yml` but haven't run on a pull request yet. M3.2's check against the profile v1 wrote on a phone moved to M3.5.
M0's exit criteria are the maintainer's to check, and they
include deciding ADR-0004, which is still Proposed; the maintainer accepted ADR-0005 in M1.2, ADR-0006 in M2.1, and
ADR-0007 in M3.1, after a throwaway spike in which Rust reached the Android Keystore through Kotlin, and decided
ADR-0018 in M2.2, ADR-0019 in M2.4, ADR-0020, the coverage floors, on 2026-11-24, ADR-0021 in M2.7, and ADR-0022 in
M3.2.
Built: the Doctrina rules, the `ci-gates` and `dependency-upgrade` skills, the planning documents in `docs/`, the spec of
v1's stored formats, the `denarii-core` crate (money, transactions, balance changes and validation) with its tests, the
Flutter project (M1.1): the Android and iOS host apps, the app scope, and a root switch between placeholder sign-in and
signed-in screens, with its tests, and the `denarii-bridge` crate (M1.2), which Dart calls through `flutter_rust_bridge`,
built by a native-assets hook, with an integration test that the sign-in placeholder shows the cents Rust parsed, and
v1's strings in seven ARB files (M1.4), with a locale resolver that walks the phone's language list (ADR-0015), and
v1's colors as Material 3 light and dark themes (M1.5), with its chart colors and system bars, and money and month
text in Dart (M1.6), with currency symbols generated from Java 25's CLDR data (ADR-0016), and the `denarii-store` crate
(M2.1), which opens v1's SQLCipher database with SQLCipher and OpenSSL compiled from source (ADR-0017), with tests
against a database that v1's own code wrote, and brings its schema to version 2 as v1 did (M2.2, ADR-0018), with
fixtures that v1's use cases and migration wrote, from which Rust reproduces every stored balance and v1's migrated cents
(M2.3), and adds, edits and deletes transactions with v1's checks, budget included, and their balance changes in one database
transaction, taking each month's bounds from Dart (M2.4, ADR-0019), and categories, accounts and budgets with v1's rules
for names, the seeded defaults and rows in use (M2.5), and seeds the default accounts and categories into empty tables
with the names Dart passes in the user's language (M2.6), and reads a month's transactions, v1's search, and every
category and account (M2.7), and the `denarii-report` crate (M2.7), which builds the monthly report, its CSV text and
the search filters from those rows, with each day's first moment from Dart (ADR-0021), and matches fixture F8, which
v1's own code wrote, byte for byte, and the Android manifest's controls (M3.8): no backup or device-to-device transfer of
any app data, v1's permission to hide overlays, and one exported activity, and the `denarii-keystore` crate (M3.2,
ADR-0022), which calls the Kotlin `VaultKeystore` through JNI for v1's device-key HMAC and its sealed profile, with an
integration test that reaches it from Dart under test-only aliases.
Planned: sign-in (M3), the screens (M4), backups (M5), the Android release (M6) and iOS (M7). Nothing from the rewrite has shipped. `docs/roadmap.md` has the
details.

## Working agreement

- Leave changes uncommitted for the maintainer to review. Don't commit, amend, rebase, force-push or tag unless asked in the
  current conversation. When the work is done, draft a commit message for the maintainer instead (see "Commit messages").
- Keep each change scoped to its task. No drive-by refactors, reformatting, renames or dependency bumps.
- Before a multi-file change, outline the plan (which files, and why). If a requirement is ambiguous or touches architecture,
  security, stored formats or UX policy, ask up to three focused questions instead of guessing.
- Copy the pattern in the nearest comparable file before you introduce a new abstraction, library or style. Where the new app
  has no such file yet, copy v1's behavior from `git show 6d57526:<path>` and its design from `docs/`.
- Keep user-visible behavior, stored data and backup formats as v1 has them unless a task and a decision record change them.
- Report honestly. List the commands you ran with their results, and what you couldn't run: Flutter checks need Flutter
  3.50.1 (in `~/flutter` on the maintainer's Linux machine, not on `PATH`, as of 2026-11-23) and the Rust toolchain with
  the targets in `rust/rust-toolchain.toml`, because every Flutter build compiles the bridge; integration tests need an
  emulator or device; upgrade checks need a phone with v1 installed; and biometric flows need a phone with an enrolled
  fingerprint.

## Read before working

| Before you touch | Read |
|---|---|
| Anything | `docs/00-overview.md`, `docs/01-architecture.md`, and the task in `docs/roadmap.md` |
| `rust/` | `.claude/rules/rust.md`, and the crate's spec in `docs/specs/` (`core.md`, `bridge.md`, `store.md`, `report.md` and `keystore.md` today) |
| `lib/`, `test/`, `pubspec.yaml`, ARB files (from M1.1) | `.claude/rules/flutter.md` |
| Kotlin, Gradle files, manifests or resources in `android/` | `.claude/rules/android.md` |
| A test, a test fixture or a CI check | `.claude/rules/testing.md` |
| Stored data, keys, sign-in, the session, backups, exports, `android/`, `ios/`, `.github/` | `.claude/rules/security.md` and `docs/specs/android-v1-formats.md` |
| `docs/`, `README.md`, `CONTRIBUTING.md`, `SECURITY.md` | `.claude/rules/roadmap-and-docs.md` and `.claude/rules/writing.md` |
| A dependency, a plugin or a GitHub Action | `docs/02-dependency-policy.md`, and the `dependency-upgrade` skill |
| A significant or hard-to-reverse choice | `docs/decisions.md` |
| `PRIVACY.md`, `THIRD_PARTY_NOTICES.md`, `licenses/`, `LICENSE` | `.claude/rules/legal-docs.md` |
| A commit message or pull request | `.claude/rules/commits.md` and "Commit messages" below |

## Stack

- **Rust:** a workspace in `rust/`, edition 2024, Rust 1.100.0 pinned in `rust/rust-toolchain.toml`. `crates/core`
  (`denarii-core`) is built and uses `std` only. `crates/bridge` (`denarii-bridge`) is built: the API Dart calls through
  `flutter_rust_bridge` 2.13.0 (ADR-0005), with only the `dart-opaque`, `rust-async` and `thread-pool` features, compiled by
  Flutter's native-assets hook in `hook/build.dart` (ADR-0014). `crates/store` (`denarii-store`) is built: it opens the
  SQLCipher database with `rusqlite` 0.43.0, compiling SQLCipher and OpenSSL's `libcrypto` from source on every target, and
  keeps its one connection behind a mutex (ADR-0006, ADR-0017). Opening brings the schema to version 2, creating it only in
  an absent or empty file and refusing a schema it doesn't know (ADR-0018). Its transaction writes take each month's bounds
  as a `core::MonthBounds` from Dart (ADR-0019). Its reads return `core::StoredTransaction` rows. `crates/report`
  (`denarii-report`) is built: it depends on `core` alone, takes the rows `store` reads and each day's first moment
  from Dart as a `MonthDays` (ADR-0021), and writes v1's CSV. `crates/keystore` (`denarii-keystore`) is built: the
  JNI calls to the Kotlin Keystore code, with `jni-sys` 0.4.1 on Android only, behind a safe `Keystore`; its `jni`
  module is the only hand-written `unsafe` code (ADR-0022). The `vault` and `backup` crates are planned.
- **Flutter:** a Dart app at the repository root, created in M1.1: Flutter 3.50.1 pinned in `pubspec.yaml`, iOS 15 and
  later (ADR-0009), `ChangeNotifier` view models with no package (ADR-0010), and Flutter's `Navigator` with no package
  (ADR-0011).
- **Platform code:** Kotlin in `android/` for the Android Keystore and `BiometricPrompt` (M3.2), and Swift in `ios/` for the
  Keychain and Face ID (M7.1).
- **Data:** v1's SQLCipher 4 database, opened from Rust (ADR-0006). The application ID stays `com.denariidolor`.

## Commands

Run the Rust checks from `rust/`. CI (`.github/workflows/ci.yml`) runs every one, and each blocks a merge:

```bash
cargo fmt --all --check                                         # fix with cargo fmt --all
cargo clippy --workspace --all-targets --locked -- -D warnings
cargo test --workspace --locked
RUSTDOCFLAGS="-D warnings" cargo doc --workspace --no-deps --locked
```

- One test file: `cargo test --locked -p denarii-core --test money`.
- The keystore crate's JNI module compiles only for Android, so the host's Clippy never sees it. CI lints it for an
  Android target too, which needs no NDK: `cargo clippy -p denarii-keystore --target aarch64-linux-android --all-targets
  --locked -- -D warnings`.
- Run the Flutter checks from the repository root. CI runs them too, then builds a debug app bundle, and a separate job
  reruns the bridge's codegen and fails if the committed bindings differ (ADR-0012):

  ```bash
  dart format --output=none --set-exit-if-changed .
  flutter analyze
  flutter test
  ```
- Measure coverage from the repository root with `tool/coverage.sh`, or `tool/coverage.sh rust` or `tool/coverage.sh dart`
  for one language. It fails below the floors at the top of the script, which only go up (ADR-0020), and CI runs it. The
  Rust part needs `rustup component add llvm-tools`, run once in `rust/`, and builds into `rust/target/coverage/`.
- Run the integration tests on an emulator or device: `flutter test integration_test -d <device>`.
- After changing the bridge's `api` module, regenerate its bindings from the repository root and commit them:
  `flutter_rust_bridge_codegen generate`. Install the codegen and `cargo-expand` as `CONTRIBUTING.md` says.
- After changing an ARB file in `lib/l10n/`, regenerate the localizations and commit them: `flutter gen-l10n`
  (`flutter pub get` and every build also run it, because `pubspec.yaml` sets `generate: true`).
- After changing `tool/CurrencySymbols.java`, regenerate the currency symbols with Java 25 and commit them (ADR-0016):
  `java tool/CurrencySymbols.java > lib/l10n/currency_symbols.dart`, then `dart format lib/l10n/currency_symbols.dart`.
- `adb` may not be on `PATH`. The SDK location is `sdk.dir` in `local.properties`.
- Use the `ci-gates` skill to run the full set and report the results.

## Architecture

`docs/01-architecture.md` is the design; this is the summary. Rust owns the rules and the data, Dart owns the screens, and
Kotlin and Swift own only what needs the operating system (ADR-0004, Proposed).

```
rust/crates/core      money (Cents, parse_cents), transactions, balance_deltas, totals, validation    built, std only
rust/crates/store     SQLCipher database, schema v2 and migrations, use cases, reads                  built (M2.1–M2.7)
rust/crates/vault     profile, key wrapping, PIN and answer, lockout, session                         M3
rust/crates/keystore  JNI calls to the Kotlin Keystore code (jni-sys, ADR-0022), exports JNI_OnLoad    built (M3.2)
rust/crates/backup    backup files, every format version                                              M5.1
rust/crates/report    monthly report, search filters, CSV                                             built (M2.7)
rust/crates/bridge    the API Dart calls (flutter_rust_bridge, ADR-0005, ADR-0014)                    built (M1.2)
lib/                  screens, ChangeNotifier view models, two scopes, locale formatting, ARB strings  M1.1
android/, ios/        Keystore and Keychain keys, the biometric prompt          M3.2 (Keystore), M3.6, M7.1
```

Dependency rules:

1. `core` depends on nothing but `std`, and no crate depends on `bridge`.
2. Screens talk only to their view models, and view models only to the bridge and the platform channels, which they get
   through their constructors from the app scope or the session scope. Dart never sees a database row or a key.
3. Every transaction write goes through a `store` use case, which runs `core::validate_transaction` and the budget check, then
   applies `core::balance_deltas` in the same database transaction as the row.
4. The database exists only after sign-in. The bridge offers no storage call until the vault has unlocked the database key,
   and locking closes the database, wipes the key, and disposes the session scope with every signed-in screen. That's a
   security control, not something to work around.
5. Rust reads no clock and no time zone. Callers pass dates as `EpochMillis`, and each month's bounds in the user's time zone
   as a `core::MonthBounds` that Dart computes (ADR-0019). The session timeout runs on the monotonic clock (CS-01).
6. Logic that doesn't need the platform lives in Rust, where `cargo test` covers it.

## Invariants (never break these)

- **Money is whole cents end to end.**
  - `Cents(i64)` in Rust and `int` in Dart. Parse input with `core::parse_cents`. Display formatting is Dart's job (`lib/l10n/money_format.dart`, ADR-0016).
  - Floating point is only for display values such as charts.
  - Never hard-code a currency symbol. The currency is a saved label that is never converted, and only currencies with 2
    decimals are allowed.
- **The kind carries the business rules.** Totals and balances call `Transaction::balance_impact` and
  `Transaction::account_impacts`. Don't match on `TransactionType` or `TransactionKind` elsewhere to compute money.
- **Stored balances stay atomic.** Any write that changes a transaction's amount, type or accounts applies
  `core::balance_deltas(previous, current)` in the same database transaction as the row.
- **Validation runs before every write.**
  - An expense that goes over its category's budget fails with `BUDGET_EXCEEDED`.
  - The caller allows it only after the user confirms.
- **Errors are values.**
  - Rust returns `Result` with a typed error; nothing panics on input, and arithmetic is checked.
  - A broken business rule is a `DomainError`. The UI shows it through `DomainError::code` and a localized message, never
    through `Display` text.
  - A new `DomainError` variant needs a code, and from M1.4 its message in all seven languages.
- **Protected data stays protected.** The seeded defaults in `core::defaults` (accounts 1–2 and categories 1–3) can't be
  deleted. Neither can a category or account that is still referenced.
- **One write at a time.** A write or export action ignores repeat calls while one is in flight, and exposes that state so
  the button is disabled.
- **v1's data is a contract.** `docs/specs/android-v1-formats.md` describes what v1 left on phones. Never change how the new
  app reads it without a decision record, never delete the database to get past an error, and keep every backup
  `FORMAT_VERSION` restorable. Schema changes only move forward from `user_version` 2.

## Security posture

Details are in `.claude/rules/security.md`, which loads when you touch security-sensitive files.

- Add no logging to shipped code: no `print!`, `eprintln!` or `dbg!` in Rust, no `print` or `debugPrint` in Dart, no `Log` or
  `println` in Kotlin, and no logging crate or package. The one exception is `package:logging`, which
  `flutter_rust_bridge`'s Dart runtime depends on; it stays inactive, because nothing connects a Rust log stream to it
  (ADR-0014).
- Add no `INTERNET` permission, network code, analytics, crash reporting or ads SDK (ADR-0008). The one exception is the
  `INTERNET` permission in Flutter's debug and profile manifests, which the Flutter tool needs to attach to a running app;
  the release build never has it (ADR-0013).
- Put no secrets in code, tests, docs or commits.
- Never weaken authentication, lockout, encryption or validation to get a change through. The same goes for lints and tests:
  don't add an `#[allow]`, an `// ignore:`, a lint baseline or an ignored test just to get a green build.
- Call out every change to a security-sensitive file in your summary, so the maintainer reviews it.
- Code comments cite stable IDs: `CS-15` is a security finding, `BUG-03` is a fixed bug and `CS-A4` is an accepted risk.
  Keep these comments, and never reuse an ID. The ledger behind them lived in `intel/`, which was deleted from the working
  tree on 2026-11-20. Read any entry with `git show aba0b55:intel/cybersec.md`. The same commit also has `maint.md`,
  `notes.md`, `plan.md` and `history.md`.

## Conventions

- Every source file starts with the proprietary copyright header. Copy it verbatim from an existing `.rs` file.
- Rust follows `rustfmt` and the lints in `rust/Cargo.toml`; a lint exception is an `#[expect(…, reason = "…")]` on the
  smallest item.
- Comment only intent that isn't obvious.
- Generated files (the bridge bindings from M1.2, the localizations from M1.4 and the currency symbols from M1.6) are
  committed and never edited by hand.
  Change their source and rerun the generator in the same change (ADR-0012).
- User-facing text lives only in the ARB files (from M1.4), in English and **all six language packs** (zh-CN, hi, es, ar, fr,
  pt). Rust returns codes, never user-facing sentences.
- Add a new dependency only when the platform and the existing dependencies can't do the job, and ask first
  (`docs/02-dependency-policy.md`). When it ships in the app, it goes in `THIRD_PARTY_NOTICES.md` and, when the license
  requires it, in `licenses/`.
- When the app's data handling changes (what it stores, exports or shares, or which permissions it requests), update
  `PRIVACY.md`, which is shown in the app. Also tell the maintainer that the Play Console Data safety form may need updating.
- Update the user guide in `README.md` for user-visible behavior, `CONTRIBUTING.md` for contributor workflow, and the
  documents in `docs/` as `.claude/rules/roadmap-and-docs.md` says.

## Rules and skills

Rules in `.claude/rules/` load automatically when you work on matching files:

| Rule | Covers |
|---|---|
| `pair-programming.md` | Every task, so it loads every time: reading first, the plan, the decisions to ask about, pushing back, small checked steps, the report |
| `rust.md` | The Rust workspace: types, errors, untrusted input, `unsafe`, docs, tests, lints |
| `flutter.md` | Dart and Flutter: widgets, state, navigation, localization, platform code, tests, release |
| `android.md` | Everything in `android/`: the Kotlin Keystore code, Gradle files, manifests, permissions, exported components, security, release |
| `testing.md` | All tests: never weakening one to make it pass, coverage, strict tests, performance, hostile input, CI |
| `security.md` | Keys, sign-in, the session, exports, backups, manifests, workflows: controls that must stay |
| `legal-docs.md` | `PRIVACY.md`, `THIRD_PARTY_NOTICES.md`, `licenses/`, `LICENSE` |
| `roadmap-and-docs.md` | The overview, architecture, roadmap, decision records and specs in `docs/` |
| `writing.md` | All prose: docs, comments, error messages, commit messages |
| `commits.md` | Commit messages, splitting work into commits, how much Git you may run, pull requests |

`flutter.md` also refers to `swift.md` for the Swift in `ios/`, which doesn't exist yet; M7.1 adds it. `android.md`
merges Doctrina's `kotlin.md` and `android.md`, which came in M3.1, trimmed on 2026-11-25 to what the Android host app
uses.

Skills in `.claude/skills/`:

| Skill | Use it to |
|---|---|
| `ci-gates` | Run the blocking checks and report the results |
| `dependency-upgrade` | Add, upgrade or remove a crate, package or plugin, or change a pinned version or action |
| `next-task` | Carry the next unblocked roadmap task, or the one named, through the definition of done (`/next-task M1.1`) |
| `fix-doc-drift` | Find and correct statements in the docs that no longer match the code or the records (`/fix-doc-drift docs/`) |
| `sweep-code` | Find bugs and dead code, prove each finding, and fix the ones the maintainer approves (`/sweep-code rust/crates/store`) |
| `security-sweep` | Find security vulnerabilities, prove each one locally, and patch the ones the maintainer approves (`/security-sweep`) |
| `simplify-code` | Simplify the approved long or convoluted code without changing what it does (`/simplify-code lib/`) |
| `draw-diagrams` | Draw or rebuild the architecture diagrams as Mermaid (`/draw-diagrams`) |
| `phase-gates` | Group the roadmap's tasks into phases with checkable gates, or check a phase's gate (`/phase-gates M2`) |
| `build-language-packs` | Draft language packs of the app's text. Name the six the app ships, because with no languages it writes nine |
| `set-up-github` | Write or update the files GitHub reads, such as CodeQL, Dependabot and `CODEOWNERS` (`/set-up-github dependabot`) |
| `build-course` | Build a course. It stops in this repository, which hasn't adopted `teaching-materials.md` |

`next-task`, `fix-doc-drift` and the eight skills after them come from Doctrina unchanged and read their values from the
rules' "Project details". Only the maintainer can start them, but you can read and follow them.

v1's `new-feature` and `room-migration` skills were Kotlin-only and left with ADR-0003. Their replacements need layers
that don't exist yet: M4.9 writes `new-feature` after the transaction form (M4.2), and M5.3 writes `schema-migration`
after the store's migrations (M2.2) and the backup formats (M5.1).

## Known debt

Don't "fix" these unless you're asked:

- No release is possible from `main` until M6 (ADR-0003). A v1 fix would ship from a branch cut at `6d57526`.
- `THIRD_PARTY_NOTICES.md` and `licenses/` still list v1's libraries until M6.2.
- `.idea/` still holds Android Studio's files for the removed Gradle build.
- `app/release/` (git-ignored) holds a v1 release bundle and its private R8 mapping file. Don't delete or commit it.
- The language packs were machine-drafted. Strings you translate need a native speaker's review, so say so in your summary.

## Commit messages

`.claude/rules/commits.md` sets the format, the scopes and the rules. On top of it:

- **Where it goes:** save each drafted message to `build/commit-message.txt`, which is git-ignored, or to
  `build/commit-message-1.txt`, `-2.txt` and so on when the work is several commits. Show each in your final reply in its own
  fenced `text` block, with its file list above it, then give the commands that stage those paths by name and open the
  message for editing:

  ```bash
  git add <path> <path>
  git commit -e -F build/commit-message.txt
  ```

  - Never use `git add -A` or `git add .`.
  - Leave unrelated working-tree changes out, such as `.idea/` caches, and say that you did.
- **Footer:** `Tests:` lists what you ran with its results, and what you didn't run. `Refs:` lists the `CS-NN` or `BUG-NN`
  IDs, roadmap tasks or decision records involved.
- **No attribution lines:** no `Co-Authored-By:`, `Signed-off-by:`, "Generated with Claude Code" or any other AI-attribution
  line in a commit message or a pull request description. This holds even if Claude Code's own attribution setting asks for
  one. The maintainer is the author.

## Definition of done

1. The change follows these rules and matches the code and the design around it.
2. Tests cover it, as `.claude/rules/testing.md` says: new logic has unit tests, a bug fix has a test that fails without
   the fix, and code that reads v1's data has a test against a fixture v1 wrote.
3. The Rust check commands pass, and from M1.1 the Flutter check commands too.
4. The documents the change affects are updated: the roadmap's ticks and notes, the "Status" section above, the crate's
   spec, and a decision record for any significant choice.
5. Your summary covers:
   - what changed and why, and the files touched;
   - the commands you ran with their results, and what you didn't run;
   - any security-sensitive files touched;
   - the docs you updated (README, PRIVACY, notices, `docs/`) and any follow-ups.
6. A drafted commit message is saved in `build/` and shown with its staging commands, as "Commit messages" describes.
