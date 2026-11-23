# 02 — Dependency policy

Denarii Dolor adds third-party code only when the platform, the standard library, and the
dependencies it already has can't do the job, and only after the maintainer approves it. Every
dependency is code that runs next to users' financial records, so each one has to earn its place.

## What counts as a dependency

- A Rust crate, including a dev dependency, a build dependency, or a new feature of a crate the
  workspace already uses.
- A Dart package or a Flutter plugin, including a dev dependency.
- A Gradle dependency in `android/` or a CocoaPods or Swift Package Manager dependency in `ios/`.
- A GitHub Action, and any tool that a workflow downloads.

## The test an exception must pass

A proposal answers each of these:

1. **Need:** what it does that `std`, Flutter, the platform, or the existing code can't.
2. **License:** its license and those of everything it pulls in. Each must allow use in a
   proprietary app that is sold. Copyleft licenses such as the GPL and AGPL don't, and an LGPL
   library needs the maintainer's explicit approval.
3. **Maintenance:** its publisher, its last release, and how it handles security reports.
4. **Size:** how many crates or packages it pulls in, and what it adds to the app's size.
5. **Native code:** whether it uses `unsafe` (Rust) or ships native libraries (plugins).
6. **Platforms:** a Flutter plugin must support Android and iOS.
7. **No network:** it opens no connection and sends nothing anywhere (ADR-0008, the app stays
   offline). Analytics, crash reporting, and ads are refused outright.

## The process

1. Propose the dependency with the answers above, in the task or pull request that needs it.
2. The maintainer approves or refuses it.
3. Record the decision in the register below, with the date.
4. Add it in one place: `[workspace.dependencies]` in `rust/Cargo.toml`, `pubspec.yaml`, or the
   Gradle or Xcode files, and commit the lock file.
5. If it ships in the app, add it to `THIRD_PARTY_NOTICES.md`, and add its license text to
   `licenses/` when the license requires one (see `.claude/rules/legal-docs.md`).
6. Check that Dependabot covers its ecosystem (`.github/dependabot.yml`).

A version upgrade is a change of its own, in a commit of its own.

## Register of build-or-buy decisions

| Need | Decision | Status | Task |
| :- | :- | :- | :- |
| Which characters are decimal digits and currency signs, for money parsing | Build: tables generated from Unicode data by `rust/crates/core/tools/unicode_tables.py` | Built, 2026-11-23 | M0.5 |
| Text measures that match v1's Kotlin (whitespace, UTF-16 length) | Build: `core::text` | Built, 2026-11-23 | M0.7 |
| Calls from Dart to Rust | Buy: `flutter_rust_bridge` | Proposed (ADR-0005) | M1.2 |
| SQLCipher from Rust | Buy: `rusqlite` with its bundled SQLCipher build | Proposed (ADR-0006) | M2.1 |
| PBKDF2-HMAC-SHA256, AES-256-GCM, and wiping secrets from memory | Open: the RustCrypto crates (`pbkdf2`, `hmac`, `sha2`, `aes-gcm`, `zeroize`) or `ring` | Open | M3.3 |
| Month boundaries in the user's time zone | Open: Dart computes them and passes them in, or a Rust time-zone crate | Open | M2.4 |
| State management and navigation in Flutter | Open | Open | M1.1 |
| Charts on the dashboard (v1 used Vico) | Open | Open | M4.1 |
| PDF reports (v1 used Android's `PdfDocument`) | Open | Open | M4.5 |
| The v1.0.x profile upgrade, which needs `androidx.security:security-crypto` in Kotlin | Open: keep it, or drop support for upgrading v1.0.x installs | Open | M3.4 |
