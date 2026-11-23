---
name: ci-gates
description: Run Denarii Dolor's blocking checks locally (Rust formatting, Clippy, tests and docs; the Flutter formatter, analyzer and tests; and, when Android files or packages change, the release build and its permissions), fix or report the failures, and summarize each result. Use before calling a change done, before a pull request, or when asked to verify, check or validate the build.
---

# Run the CI gates

Run every check below, in this order, and report each result. CI (`.github/workflows/ci.yml`) runs the Rust checks
today, and each one blocks a merge. The Flutter checks join CI in M1.3, the task that adds Flutter jobs to `ci.yml`.
Until then they run only here, and the "Definition of done" in `CLAUDE.md` still requires them.

## 1. Look at what changed

```bash
git status --short
```

Note which files are yours, so you can spot formatter changes to files you didn't touch.

## 2. Rust checks

Run them from `rust/`. The toolchain comes from `rust/rust-toolchain.toml`; if it's missing, `rustup toolchain install`
in that folder installs it.

```bash
cd rust
cargo fmt --all --check
cargo clippy --workspace --all-targets --locked -- -D warnings
cargo test --workspace --locked
RUSTDOCFLAGS="-D warnings" cargo doc --workspace --no-deps --locked
```

How to fix each kind of failure:

- **Format:** run `cargo fmt --all`, then `git diff --stat`, and revert hunks in files you didn't change. A generated file,
  such as `crates/core/src/text/unicode_tables.rs`, is regenerated with the command in its generator's docstring.
- **Clippy:** fix the code. Where a lint is wrong in one place, use `#[expect(clippy::lint_name, reason = "…")]` on the
  smallest item. Never weaken `[workspace.lints]` or `clippy.toml`.
- **`--locked` fails:** `Cargo.lock` is stale because a manifest changed. Run `cargo update -w` only if the change was
  meant to alter dependencies, which needs the maintainer's approval (`docs/02-dependency-policy.md`).
- **Tests:** fix the cause. Never `#[ignore]` a test or weaken an assertion. For one test file, run
  `cargo test --locked -p denarii-core --test money`.
- **Docs:** a broken intra-doc link or a link to a private item. Fix the link, or the item's visibility.

## 3. Flutter checks

Run them from the repository root, with Flutter 3.50.1, the exact release that `pubspec.yaml` pins (ADR-0009). On the
maintainer's Linux machine, Flutter is in `~/flutter` and not on `PATH`. Each shell command starts a fresh shell, so put
the `export` in the same command as the checks:

```bash
export PATH="$HOME/flutter/bin:$PATH"
dart format --output=none --set-exit-if-changed .
flutter analyze
flutter test
```

How to fix each kind of failure:

- **Format:** run `dart format .`, then `git diff --stat`, and revert hunks in files you didn't change.
- **Analyzer:** fix the types or the design. Never add `!`, `late`, `dynamic`, an `as` cast or an `// ignore:` comment
  only to silence a finding, and never weaken `analysis_options.yaml`. `dart fix --dry-run` lists the fixes the analyzer
  can make by itself; read each one before you apply them with `dart fix --apply`.
- **`flutter pub get` rejects the SDK version:** the shell found a Flutter release other than 3.50.1. Fix `PATH`; never
  change the pin.
- **`pubspec.lock` changed:** `flutter analyze` and `flutter test` run `flutter pub get` first, which rewrites the lock
  file when `pubspec.yaml` changed. Keep the change only if it was meant to alter dependencies, which needs the
  maintainer's approval (`docs/02-dependency-policy.md`). Otherwise, restore it with `git restore pubspec.lock`.
- **Tests:** fix the cause. Never skip a test (`skip:`) or loosen an expectation. For one test file, run
  `flutter test test/app/root_switch_test.dart`. Advance time with `tester.pump`, never by waiting for real time.

Generated Dart files, the bridge bindings from M1.2 and the localizations from M1.4, are never formatted or fixed by
hand. Change their source and rerun the generator in the same change (ADR-0012: generated code is committed, and CI
checks that it is current).

## 4. Android release build

Run this when the change touches `android/`, `pubspec.yaml`, or `pubspec.lock`. R8, Android's code shrinker, runs only
on the release build, and the release manifest differs from the debug one, so a debug build or a unit test can't catch a
problem in either.

```bash
export PATH="$HOME/flutter/bin:$PATH"
flutter build apk --release
```

The release APK is signed with the debug key (`android/app/build.gradle.kts`) until M6.1 builds a signed release
pipeline, so a local build needs no secrets.

Then list the permissions the release APK asks for, with `aapt2`, the Android SDK's packaging tool:

```bash
"$(sed -n 's/^sdk.dir=//p' local.properties)/build-tools/36.0.0/aapt2" dump permissions build/app/outputs/flutter-apk/app-release.apk
```

Expect only `com.denariidolor.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, which AndroidX adds. Any other permission is a
failure to report, and `android.permission.INTERNET` most of all: the release build never has it (ADR-0008, the app stays
offline; ADR-0013, only debug and profile builds keep it). The merged release manifest is at
`build/app/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml`.

## 5. Integration tests

None exist yet. M1.2 adds the first ones and the command that runs them on an emulator. Until then, write "none yet" in
the report's row.

## 6. Report

End with a table, and don't call a check passed unless you ran it in this session and it passed:

| Check | Result | Notes |
|---|---|---|
| `cargo fmt` | ✅ / ❌ / not run | |
| `cargo clippy` | | |
| `cargo test` | | X passed, Y failed |
| `cargo doc` | | |
| `dart format` | | |
| `flutter analyze` | | |
| `flutter test` | | X passed, Y failed |
| `flutter build apk --release` | | or why the change didn't need it |
| Release permissions | | the permissions `aapt2` listed |
| Integration tests | | none yet (M1.2) |

Also list what you couldn't run here, and why: iOS builds need a Mac with Xcode (M7), upgrade checks need a phone with v1
installed, and biometric flows need a phone with an enrolled fingerprint.
