---
name: ci-gates
description: Run Denarii Dolor's blocking CI checks locally (Rust formatting, Clippy, tests and docs today; the Flutter checks once the Flutter app exists), fix or report the failures, and summarize each result. Use before calling a change done, before a pull request, or when asked to verify, check or validate the build.
---

# Run the CI gates

CI (`.github/workflows/ci.yml`) blocks a merge on every one of these checks. Run the same checks, in the same order.

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

## 3. Flutter checks (from M1.1)

The Flutter app doesn't exist yet (`docs/roadmap.md`, M1.1). Once `pubspec.yaml` exists, run these from the repository
root as well, and add them to this skill with their failure fixes:

```bash
dart format --output=none --set-exit-if-changed .
flutter analyze
flutter test
```

## 4. Report

List each command with its result (pass, or the failure and what you did about it). Say which checks you couldn't run and
why: the Flutter checks before M1.1, integration tests without an emulator or device, and biometric flows without a
phone with an enrolled fingerprint.
