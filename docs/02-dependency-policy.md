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
| Calls from Dart to Rust | Buy: `flutter_rust_bridge` 2.13.0 (MIT), pinned exactly as a Rust crate with only the `dart-opaque`, `rust-async`, and `thread-pool` features, as a Dart package, and as its codegen, `flutter_rust_bridge_codegen` (a build tool). The crate brings 29 more crates into the Android library, among them `tokio`, `futures`, `allo-isolate` (Apache-2.0), and `dart-sys`, which compiles the Dart SDK's `dart_api_dl.c` (BSD-3-Clause); every one is MIT, Apache-2.0, or offers MIT. The Dart package brings `args`, `async`, `logging`, and `web` from the Dart team (BSD-3-Clause) and `build_cli_annotations` (MIT) | Approved by the maintainer, 2026-11-23 (ADR-0005, ADR-0014) | M1.2 |
| Logging in the Dart runtime of `flutter_rust_bridge`, which depends on `package:logging` | Allowed as an inactive transitive dependency, on the condition that nothing in the app connects a Rust log stream or listens to its records | Approved by the maintainer, 2026-11-23 (ADR-0014) | M1.2 |
| Building the Rust library for each Android ABI inside the Flutter build | Buy: Flutter's native-assets build hook with `flutter_rust_bridge_hooks` 2.13.0 (MIT) and `native_toolchain_rust` 1.1.1 (MIT, one maintainer), over `hooks` 2.2.0 and `code_assets` 2.2.0 from the Dart team (BSD-3-Clause), with `toml` and `petitparser` (MIT) and Dart-team packages (BSD-3-Clause). They run only during the build, and the app never imports them | Approved by the maintainer, 2026-11-23 (ADR-0014) | M1.2 |
| Expanding macros for the bridge's codegen | Buy: `cargo-expand` 1.0.127 (MIT OR Apache-2.0), a build tool installed with `--locked`, so the codegen never installs an unpinned one by itself | Installed for M1.2, 2026-11-23, as a requirement of the approved codegen (ADR-0014) | M1.2 |
| Integration tests on a device | Buy: `integration_test` from the Flutter SDK, a dev dependency, which M1.2's `Verify` line requires. Its Android module compiles against Android SDK Platform 36 | Added for M1.2, 2026-11-23 | M1.2 |
| SQLCipher from Rust | Buy: `rusqlite` 0.43.0 (MIT), pinned exactly in `denarii-store` with its default features off and only `bundled-sqlcipher-vendored-openssl` on. It compiles SQLCipher 4.20.0 over SQLite 3.54.1 (BSD-3-Clause and public domain) from `sqlcipher-src` 420.0.0, and OpenSSL 3.6.3's `libcrypto` (Apache-2.0) from `openssl-src` 300.6.1, on every target. Seven crates ship: `rusqlite`, `libsqlite3-sys`, and `openssl-sys` (MIT), and `bitflags`, `fallible-iterator`, `fallible-streaming-iterator`, and `smallvec` (MIT or Apache-2.0). `sqlcipher-src`, `openssl-src`, `pkg-config`, and `vcpkg` run only during the build | Approved by the maintainer, 2026-11-23 (ADR-0006, ADR-0017) | M2.1 |
| Calls from Rust to the Kotlin Keystore code in the app's Java runtime, through the Java Native Interface (JNI) (ADR-0007) | Buy: `jni-sys` 0.4.1 (MIT OR Apache-2.0, from the `jni-rs` project), pinned exactly in `denarii-keystore` and compiled for Android only: raw declarations of `jni.h` and its constants, behind the crate's hand-written safe wrapper. Its macro crate, `jni-sys-macros` 0.4.1 (MIT OR Apache-2.0), runs only during the build, with `quote` and `syn`, which the workspace already builds. No logging crate. `jni` 0.22.4 (MIT OR Apache-2.0), the safe wrapper, wasn't added, because it depends on `log`, which can't be turned off | Approved by the maintainer, 2026-11-25, after M3.1's spike (ADR-0022) | M3.2 |
| PBKDF2-HMAC-SHA256, AES-256-GCM, and wiping secrets from memory | Open: the RustCrypto crates (`pbkdf2`, `hmac`, `sha2`, `aes-gcm`, `zeroize`) or `ring` | Open | M3.3 |
| Month boundaries in the user's time zone | Build: Dart computes each month's first moment and the next month's with `DateTime`, and passes them as `core::MonthBounds`; no Rust time-zone crate | Decided by the maintainer, 2026-11-24 (ADR-0019) | M2.4 |
| State management in Flutter | Build: `ChangeNotifier` view models and two `InheritedWidget` scopes, no package | Decided, 2026-11-23 (ADR-0010) | M1.1 |
| Navigation in Flutter | Build: Flutter's `Navigator`, no package | Decided, 2026-11-23 (ADR-0011) | M1.1 |
| Pinning the Flutter release | Build: an exact constraint in `pubspec.yaml`, no version manager | Decided, 2026-11-23 (ADR-0009) | M1.1 |
| Installing the pinned Flutter release in CI | Build: a shallow `git clone` of `github.com/flutter/flutter` at the `3.50.1` tag, refused unless the tag points to commit `ae9b586f34a8443573f82ef57c3bf914a908cbf7`; no third-party action such as `subosito/flutter-action` | Decided by the maintainer, 2026-11-23 | M1.3 |
| Caching downloads and builds between CI runs | Not added: no `actions/cache`, so every run downloads Flutter, its packages, and Gradle, and compiles the codegen and `cargo-expand` | Decided by the maintainer, 2026-11-23 | M1.3 |
| Material Design widgets, which Flutter moved out of the SDK into a package (`package:flutter/material.dart` is deprecated since Flutter 3.47) | Buy: `material_ui` 1.11.1 from the Flutter authors (BSD-3-Clause, pure Dart). It brings `cupertino_ui` 1.1.7 (BSD-3-Clause), `intl` 0.20.3 (BSD-3-Clause) with `clock` 1.1.3 (Apache-2.0) and `path` 1.9.1 (BSD-3-Clause), and the SDK's `flutter_localizations` | Approved by the maintainer, 2026-11-23 | M1.1 |
| Generated localizations from the ARB files (`flutter gen-l10n`), whose output imports `flutter_localizations` and `intl` | Buy: the SDK's `flutter_localizations` and `intl` 0.20.3 as direct dependencies. Both already shipped in the app through `material_ui`, so `pubspec.lock` changes only from transitive to direct and no package is added | Approved by the maintainer, 2026-11-23 | M1.4 |
| Money and month text (v1's `PlatformFormat.kt`) | Buy: `intl` 0.20.3, already a direct dependency (M1.4), for its CLDR 48 number and date data and its currency format | Decided by the maintainer, 2026-11-23 (ADR-0016) | M1.6 |
| Currency symbols for each locale, which `intl` doesn't have (it has one symbol per currency for every language) | Build: `lib/l10n/currency_symbols.dart`, generated by `tool/CurrencySymbols.java` from the Unicode CLDR 47 data of Java 25, a build tool already on GitHub's runners. CI reruns it and fails on any difference | Decided by the maintainer, 2026-11-23 (ADR-0016) | M1.6 |
| Measuring test coverage, and failing below a floor (`.claude/rules/testing.md`) | Build: `tool/coverage.sh`, over rustc's `-C instrument-coverage` with `llvm-profdata` and `llvm-cov` from the `llvm-tools` component of the pinned Rust release, and Flutter's `flutter test --branch-coverage`. CI installs the component with `rustup` and downloads nothing else. `cargo-llvm-cov` wasn't added | Decided by the maintainer, 2026-11-24 (ADR-0020) | M1.3 |
| Recommended lints for Flutter apps | Buy: `flutter_lints` 6.0.0, a dev dependency from the Flutter authors (BSD-3-Clause), with `lints` 6.1.0 | Approved by the maintainer, 2026-11-23 | M1.1 |
| Cupertino icon font, which `flutter create` adds | Not added: the app draws no Cupertino icons, so `cupertino_icons` is left out | Decided, 2026-11-23 | M1.1 |
| Charts on the dashboard (v1 used Vico) | Open | Open | M4.1 |
| PDF reports (v1 used Android's `PdfDocument`) | Open | Open | M4.5 |
| The v1.0.x profile upgrade, which needs `androidx.security:security-crypto` in Kotlin | Open: keep it, or drop support for upgrading v1.0.x installs | Open | M3.4 |
