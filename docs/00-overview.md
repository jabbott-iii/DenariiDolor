# 00 — Overview

Draft of 2026-11-23 for the maintainer's review. The goals, non-goals, budgets, and assumptions
below are proposals until the maintainer confirms them, except where a line names an accepted
decision record.

## Purpose

Denarii Dolor is a single-user, offline personal-finance ledger sold on Google Play: expenses,
income, transfers, budgets, monthly reports, and encrypted backups. Version 1 (v1) is a Kotlin
Android app. This project rewrites it as a Flutter app with a Rust core, so one code base ships on
Android and iOS (ADR-0001, rewrite in Flutter and Rust).

One rule shapes the rest: each user's records exist only on their own phone, encrypted, and an
update must never lose or corrupt them. So the new app opens the data v1 left on the phone, as it
is (ADR-0002, existing installs upgrade in place).

## Goals

1. A phone with v1.x installed updates to the new app and keeps everything without any action from
   the user: the same PIN, security answer, accounts, balances, and transactions (ADR-0002).
   Checked by the exit criteria of M3 and by task M6.4.
2. Every flow in the README's user guide works on Android as it does in v1 (milestone M4).
3. The money, ledger, validation, storage, and backup rules exist once, in Rust, with host tests
   that need no device (ADR-0004, Proposed).
4. Every control in `.claude/rules/security.md` holds in the new app, or a decision record says
   why it changed.
5. Every backup file that any v1 release wrote still restores (CS-28, M5.1).
6. An iOS app with the same features and protections ships after the Android release (M7).

## Non-goals

- Network features of any kind: sync, cloud backup, accounts, analytics, crash reporting, or ads.
  The app stays offline (ADR-0008).
- Desktop and web builds (ADR-0001 covers phones only).
- More than one user, or shared ledgers.
- Converting between currencies. The currency is a saved label, and amounts are never converted.
- Releases of the new app before it reaches parity with v1 (ADR-0003, replace the Kotlin app now).

## Budgets

| Budget | Target |
| :- | :- |
| Sign-in time, from tapping the last PIN digit to the dashboard showing | ≤ 1 s, on a phone that M3.3 records; v1's time on the same phone is the baseline |
| Scrolling the transaction list with 1,000 transactions, in profile mode | No frame over 16 ms, on the phone that M4.1 records |
| Download size of the arm64 APK from the app bundle | At most v1's size plus 10 MB; M6.3 records v1's size |
| `cargo test --workspace` | ≤ 60 s on the maintainer's Linux machine |

## Assumptions

Revisit these if any turns out to be wrong.

- The new build keeps the application ID `com.denariidolor` and the same Play signing setup, so
  Google Play installs it as an update rather than a new app.
- Android Keystore keys that v1 created stay usable after the update, because Android ties them to
  the app's user ID, which an update keeps. M3.2 checks this on a phone.
- A Rust build of SQLCipher opens a database written by `net.zetetic:sqlcipher-android` 4.19.1
  with its default settings. M2.1 checks this against a fixture.
- Flutter's Android embedding can host the Kotlin code that calls the Keystore and shows
  `BiometricPrompt` with a `CryptoObject`. M3.2 checks this.
- The highest `versionCode` uploaded for v1 is known before M6.1, because the new build number
  must be higher.
- v1 needs no fix while the rewrite is under way. If it does, the fix ships from a branch cut at
  commit `6d57526` (ADR-0003).

## Scope estimate

Sizes: S is up to one week of focused work, M is one to three weeks, L is three to six weeks, and
XL is more than six weeks.

| Area | Size | Why |
| :- | :- | :- |
| Domain core (`core`) | S | Pure logic with tests to port; done in M0 |
| Storage (`store`) on the v1 database | L | A SQLCipher build for two platforms, an exact schema match, atomic balance updates, and fixtures from v1 |
| Vault and sign-in (`vault`, plus Kotlin and Swift code) | XL | Hardware-backed keys on two platforms, byte-exact compatibility with v1 profiles, biometric sign-in, and the lockout |
| Backups (`backup`) | M | A binary codec that reads every v1 format version, with one fixture each |
| Screens (Flutter) | XL | About 14 screens, seven languages, right-to-left layout, and accessibility |
| Reports and export | M | Monthly totals, CSV, PDF rendering, and the share sheet |
| Release pipeline | M | Signing in CD, legal notices, and the Play review |
| iOS | L | A Keychain and Secure Enclave design, Face ID, and App Store requirements |

## Top risks and mitigations

| Risk | Mitigation |
| :- | :- |
| A v1 install doesn't open after the update, and its user loses access to their records | Specify every stored format before writing code that reads it (`docs/specs/android-v1-formats.md`); test against fixtures that the v1 app itself wrote; upgrade a real phone in M3 and M6.4; roll out in stages |
| Rust's SQLCipher build uses different settings than `sqlcipher-android` | M2.1 opens a v1 fixture database before any other storage work starts |
| Secrets pass through Dart memory, which the app can't zero | ADR-0007, accepted after M3.1's spike, keeps hardware-key work in Kotlin and Swift, which Rust calls directly, so no key or stretched secret passes through Dart; the typed PIN leaves Dart once, on its way to the vault |
| The rewrite takes months and v1 needs a fix meanwhile | A hotfix branch from `6d57526` (backlog item with its trigger) |
| Dart code is written before it can be built | M1.1 installs and pins the Flutter SDK before any Dart is written |
| Machine-drafted translations reach users unreviewed | A native speaker reviews each language pack before the M6 release |
