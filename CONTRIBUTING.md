# Contributing to Denarii Dolor

Thank you for helping. Please follow the [Code of Conduct](CODE_OF_CONDUCT.md). The architecture and maintainability rules in [`CLAUDE.md`](CLAUDE.md), [`.claude/rules/`](.claude/rules/) and [`docs/`](docs/) are authoritative. Claude Code reads them automatically, and they apply to every contributor. This guide summarizes what a contributor needs from them.

Security vulnerabilities are the exception: report them through a private security advisory on the repository, not a public issue.

Denarii Dolor is proprietary software owned by Joseph Anthony Abbott III (see [LICENSE](LICENSE)). A contribution can be accepted only after its author signs a written agreement that assigns their rights in it to him.

The app is being rewritten as a Flutter app over a Rust core ([ADR-0001](docs/decisions.md)). Start with [`docs/00-overview.md`](docs/00-overview.md) and [`docs/roadmap.md`](docs/roadmap.md), which say what is built and what comes next. v1, the Kotlin app on Google Play, is at commit `6d57526`; read any of its files with `git show 6d57526:<path>`.

## Setup

**Prerequisites**

- Git.
- `rustup`. The Rust version is pinned in `rust/rust-toolchain.toml`, and `rustup toolchain install` in `rust/` installs it with Clippy and rustfmt.
- From milestone M1.1: Flutter 3.50.1, the exact version `pubspec.yaml` pins (`flutter pub get` fails on any other), the Android SDK with platform 37, and an emulator or device running API 26 or newer. CI will run API 26 and 36, as it did for v1.
- From milestone M1.2: the Android targets of the pinned Rust toolchain, because every Flutter build and `flutter test` run compiles the Rust bridge through `hook/build.dart`. Install them from `rust/` with `rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android`.
- To change the bridge's API: the codegen and `cargo-expand`, both pinned. Install `cargo-expand` first, because the codegen installs an unpinned one by itself when it finds none:

  ```bash
  cargo install cargo-expand --version 1.0.127 --locked
  cargo install flutter_rust_bridge_codegen --version 2.13.0 --locked
  ```
- For iOS work (milestone M7): a Mac with Xcode.

```bash
git clone https://github.com/jabbott-iii/DenariiDolor.git
cd DenariiDolor/rust
rustup toolchain install
cargo test --workspace --locked
```

## Workflow

1. Get the concept approved on its issue, and find its task in [`docs/roadmap.md`](docs/roadmap.md).
2. Branch from `main`, and keep the branch focused on that task. Don't mix in unrelated refactoring, formatting, dependency upgrades or renames.
3. Make the change following the coding expectations below.
4. Run the validation commands and fix any failures.
5. Open a pull request against `main` that links the issue.

The maintainer (`@jabbott-iii`, see `CODEOWNERS`) reviews every pull request. There's no release pipeline until milestone M6 rebuilds one; contributors don't push tags.

## Validation

Run these from `rust/` before opening a pull request. CI runs the same checks, and all of them block a merge.

```bash
cargo fmt --all --check                                         # fix with cargo fmt --all
cargo clippy --workspace --all-targets --locked -- -D warnings  # lints, set in rust/Cargo.toml
cargo test --workspace --locked                                 # unit, integration and doc tests
RUSTDOCFLAGS="-D warnings" cargo doc --workspace --no-deps --locked
```

Also run these from the repository root, with Flutter 3.50.1. CI runs the first three, then builds a debug app bundle. A separate CI job reruns the bridge's codegen and fails if the committed bindings differ from its output (ADR-0012: generated code is committed, and CI checks that it is current). The integration tests need an emulator, so CI doesn't run them.

```bash
dart format --output=none --set-exit-if-changed .
flutter analyze
flutter test
flutter test integration_test -d <device>   # on an emulator or device, from milestone M1.2
```

The security workflow also runs CodeQL, gitleaks and, on pull requests, dependency review, which fails on high-severity vulnerabilities.

## Coding expectations

- **Architecture:** follow the layers and dependency rules in [`docs/01-architecture.md`](docs/01-architecture.md) and [`CLAUDE.md`](CLAUDE.md):
  - Rust owns the rules and the data; Dart owns the screens; Kotlin and Swift own only what needs the operating system.
  - `rust/crates/core` uses only `std`.
  - Transaction writes go through the storage use cases, so validation and balance updates always run.
  - The database exists only after sign-in.
- **v1's data:** the new app opens what v1 left on phones, as it is. [`docs/specs/android-v1-formats.md`](docs/specs/android-v1-formats.md) is the contract; a change to how the app reads any of it needs a decision record.
- **Money:** amounts are whole cents (`Cents(i64)` in Rust). Parse input with `core::parse_cents`. Never store or compare amounts as floating point, and never write a currency symbol into code or strings.
- **Text and languages (from M1.4):** user-facing text lives only in the ARB files, in English and the six language packs (zh-CN, hi, es, ar, fr, pt). Rust returns error codes, never sentences for users. Layouts must work right to left (Arabic).
- **Errors:** Rust returns `Result` with a typed error, never panics on input, and checks its arithmetic.
- **Tests:**
  - New logic needs unit tests, and a bug fix a test that fails without the fix.
  - Code that reads v1's data needs a test against a fixture that v1 wrote.
  - Integration tests live in `integration_test/` and run on an emulator or device.
- **Style:**
  - Start every source file with the proprietary copyright notice (copy it from an existing `.rs` file).
  - Follow `rustfmt` and the workspace lints. Silence a lint only on the smallest item, with `#[expect(…, reason = "…")]`.
  - Add comments only for intent that isn't obvious.
- **Dependencies:** add one only when the platform or an existing dependency can't do the job, and ask first. [`docs/02-dependency-policy.md`](docs/02-dependency-policy.md) has the test it must pass and the register of decisions. A dependency that ships in the app goes in `THIRD_PARTY_NOTICES.md`, with its license text in `licenses/` when that license requires it.
- **Security:**
  - Don't add logging, network access, or secrets of any kind.
  - Don't weaken authentication, encryption, validation, lints or tests to get a change through.
  - The security-sensitive files are the `paths` listed at the top of [`.claude/rules/security.md`](.claude/rules/security.md).
    A change to one of them must keep every control in that file's table, and the pull request must point it out. A new
    security finding takes the next free `CS-NN` ID. Earlier findings are in the git history:
    `git show aba0b55:intel/cybersec.md`.

## Documentation

Update the documents a change affects in the same pull request:

| Change | Update |
|---|---|
| A change in behavior or design | The part's spec in `docs/specs/` |
| A significant or hard-to-reverse decision | A new record in `docs/decisions.md` (`Proposed` until the maintainer accepts it) |
| A roadmap task done | Its tick and notes in `docs/roadmap.md`, the "Status" section of `CLAUDE.md`, and the status line in `README.md` |
| A new command, module or rule | The "Read before working" table in `CLAUDE.md` |
| The bridge's API (`rust/crates/bridge/src/api/`) | The bindings, regenerated from the repository root with `flutter_rust_bridge_codegen generate` and committed with the change (ADR-0012), and `docs/specs/bridge.md` |
| User-visible behavior | The user guide in `README.md` |
| What the app stores, exports or shares, or its permissions | `PRIVACY.md`, and tell the maintainer about the Play Data safety form |
| The workflow or the checks | This file |

Write prose by [`.claude/rules/writing.md`](.claude/rules/writing.md), and plans and specs by [`.claude/rules/roadmap-and-docs.md`](.claude/rules/roadmap-and-docs.md).

## Pull request expectations

- Link the approved issue and the roadmap task, and describe what changed and why.
- List the validation commands you ran and their results. Say which checks you didn't run (for example, integration tests without an emulator, or biometric checks without a phone).
- Keep the change small and focused, and preserve v1's behavior unless the task and a decision record change it.
- Point out any security-sensitive change so that it gets a careful review.
- CI and the security workflow must pass before merge.
