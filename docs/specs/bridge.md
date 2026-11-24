# Subsystem: `bridge`

`denarii-bridge` (`rust/crates/bridge`) is the only Rust code the Flutter app calls. Its functions
check what Dart sends, call the domain crates, and return plain values and error enums that Dart
can match on, so no business rule lives in Dart and no Rust error text reaches a screen. ADR-0005
(Dart calls Rust through `flutter_rust_bridge`) and ADR-0014 (the bridge's version, features, and
native-assets build) shaped it, and ADR-0012 (generated code is committed) governs its generated
files. It sits on top of ADR-0004 (Proposed: Rust owns the rules and the data). Built in M1.2 as
described here, with one function so far; the API reference is
`cargo doc -p denarii-bridge --open`, run from `rust/`.

## Why this design

Dart can call native code only through `dart:ffi`, which sees C functions. Calling Rust from Dart
by hand means writing a C interface in Rust, with `unsafe` code and manual memory management for
every call, and matching Dart declarations. `flutter_rust_bridge` generates both sides from
ordinary Rust functions and types instead: a Dart function for each Rust function, Dart classes and
enums for Rust structs and enums, and the glue between them. Its generated glue is the only
`unsafe` code in the workspace.

The crate stays thin. A function here converts types, checks input that only Dart can get wrong
(such as the decimal separator below), and calls a domain crate; the rules themselves stay in
`core` and, later, `store`, `vault`, `backup`, and `report`, where `cargo test` covers them without
Flutter.

What it trades away:

- **Generated code in two languages.** 560 lines of Dart and 349 lines of Rust for one function,
  committed and reviewed with each API change (ADR-0012).
- **Dependencies.** 30 third-party crates ship in the Android library, and the Dart runtime package
  brings `package:logging` (ADR-0014 lists them and their licenses).
- **An idle runtime.** The generated code needs the `rust-async` feature, so the bridge creates a
  multi-threaded `tokio` runtime at its first call, with one worker thread per CPU core that waits
  idle, because no function here is `async`.

## How a call crosses

1. Dart calls the generated function, such as `parseCents` in
   `lib/platform/rust/api/money.dart`. The generated code encodes the arguments into bytes and
   passes them, with a port for the reply, to an exported C function in the Rust library through
   `dart:ffi`.
2. The generated Rust handler queues the call on a pool of worker threads (`threadpool`, one thread
   per CPU core, all started at the first call), so Flutter's UI thread never waits for Rust. On the worker
   it decodes the arguments and runs the function inside `std::panic::catch_unwind`, so a panic
   becomes an error value instead of ending the app.
3. The handler encodes the result and posts it to the Dart port. The Dart `Future` completes with
   the value, or throws the generated Dart form of the error enum (for example `AmountError`).
4. Hand-written code in `lib/platform/` turns a thrown error enum into a result value, so view
   models never catch bridge errors themselves (`RustMoneyParser` in
   `lib/platform/money_parser.dart`).

The decoders in the generated Rust call `unwrap` on bytes they read and `unimplemented!` on an
unknown enum index. The bytes come only from the generated Dart encoder in the same process, so a
malformed message would be a bug in the generator, and step 2 turns any such panic into a Dart
`PanicException`.

## Building and loading

- **Build:** Flutter runs `hook/build.dart`, its native-assets build hook (ADR-0014), during every
  build and `flutter test` run. The hook calls `flutter_rust_bridge_hooks`, which runs
  `cargo build --release` on `rust/crates/bridge` through `native_toolchain_rust`, with the NDK of
  Flutter's Android build, for each ABI the build targets: `arm64-v8a`
  (`aarch64-linux-android`), `armeabi-v7a` (`armv7-linux-androideabi`), and `x86_64`
  (`x86_64-linux-android`). The workspace's release profile applies, so overflow checks stay on.
- **Toolchain:** `native_toolchain_rust` reads an exact Rust version and the `targets` list from a
  `rust-toolchain.toml` beside the crate's `Cargo.toml`. `rust/crates/bridge/rust-toolchain.toml`
  is a symbolic link to `rust/rust-toolchain.toml`, which lists the three Android targets and the
  Linux host that runs `flutter test`. M7.1 adds the iOS targets.
- **Packaging:** the build puts `libdenarii_bridge.so` in the APK under `lib/<abi>/`. The crate's
  `crate-type` is `cdylib` for that library and `lib` for its own tests. In M1.2's release APK the
  library is 661 KB for `arm64-v8a`, 444 KB for `armeabi-v7a`, and 695 KB for `x86_64`. The two
  64-bit builds have 16 KB-aligned segments, which Google Play requires for 64-bit ABIs; the 32-bit
  build keeps 4 KB pages.
- **Loading:** `main` awaits `RustLib.init()` before `runApp`. On Android it opens
  `libdenarii_bridge.so` by name and checks that the library's content hash matches the bindings,
  so a stale library fails at startup rather than on a call.

## Regenerating the bindings

`flutter_rust_bridge.yaml` at the repository root configures the codegen: it reads `crate::api`
from `rust/crates/bridge/` and writes the Dart bindings to `lib/platform/rust/` and the Rust glue to
`rust/crates/bridge/src/frb_generated.rs`. After any change to `api`, run this from the repository
root, with Flutter 3.50.1 on `PATH`, and commit the result with the change:

```bash
flutter_rust_bridge_codegen generate
```

- The codegen is pinned to 2.13.0, the same version as the crate and the Dart packages; install it
  with `cargo install flutter_rust_bridge_codegen --version 2.13.0 --locked`.
- It runs `cargo expand`, and installs the latest `cargo-expand` by itself when it is missing, so
  install a pinned one first: `cargo install cargo-expand --version 1.0.127 --locked`.
- It formats what it writes. `rustfmt` skips `frb_generated.rs` (`#[rustfmt::skip]` on its `mod`
  line) and the analyzer skips `lib/platform/rust/`, as ADR-0012 says for generated paths.
- `auto_upgrade_dependency: false` stops it from adding or upgrading packages by itself.

## API

Built in M1.2 as described here.

```rust
// rust/crates/bridge/src/api/money.rs
pub struct NumberSymbols { pub decimal_separator: String, pub currency_marks: Vec<String> }
pub enum AmountError { Empty, Malformed, TooManyDecimals, TooLarge, InvalidDecimalSeparator }
pub fn parse_cents(text: String, symbols: NumberSymbols) -> Result<i64, AmountError>;
```

- `parse_cents` gives `core::parse_cents`'s result in cents, and maps each `MoneyParseError`
  variant to the `AmountError` variant of the same name. `MoneyParseError` is `#[non_exhaustive]`,
  so a variant added to it later maps to `Malformed` until the bridge gives it its own variant.
- The decimal separator crosses as a `String`, not a `char`, because a Dart `String` can hold any
  number of characters. `parse_cents` checks that it holds exactly one Unicode scalar value before
  it reads the text, and returns `InvalidDecimalSeparator` otherwise.
- The generated Dart API is `Future<int> parseCents({required String text, required NumberSymbols
  symbols})`, which throws an `AmountError`. View models use the `MoneyParser` interface in
  `lib/platform/money_parser.dart` instead, whose `parseCents` returns an `AmountInCents` or a
  `RejectedAmount` and never throws for a rejected amount.

Rules for each function added to `api`:

- Take owned types (`String`, `Vec<T>`), because the generated code passes each argument by value.
- Check every value from Dart before it reaches a domain crate, and never panic on it.
- Return a `Result` whose error is a plain enum (variants without data). An enum whose variants
  carry data makes the codegen require the `freezed`, `freezed_annotation`, and `build_runner`
  packages, which the dependency policy hasn't approved.
- Don't offer a storage call until the vault has unlocked the database key (`CLAUDE.md`,
  dependency rule 4).

## Lints and `unsafe`

The `bridge` crate repeats the workspace's lints in its own `[lints]` table, because Cargo can't
relax one lint a crate inherits; the only difference is `unsafe_code = "deny"` instead of
`forbid`. `src/lib.rs` exempts the generated module alone, with an `#[expect(…, reason = "…")]` on
its `mod` line that lists `unsafe_code`, `unreachable_pub`, and the Clippy lints the generated code
trips. Clippy doesn't check wildcard imports in a test build, so `clippy::wildcard_imports` sits in
a second `expect` that applies outside tests only (`cfg_attr(not(test), …)`). Because these are
`expect` and not `allow`, a regeneration that stops tripping one of the lints fails the build until
the list is updated. The hand-written modules keep every lint.

## Invariants

1. No crate depends on `denarii-bridge`, and Dart reaches Rust only through its `api` module.
2. No function in `api` panics on any value Dart can send.
3. `parse_cents` with a one-character separator gives `core::parse_cents`'s cents, and its error
   variant of the same name, for every text.
4. `parse_cents` with a separator that isn't exactly one character returns
   `InvalidDecimalSeparator`, whatever the text.
5. The shipped library contains no logging crate: `log`, `android_logger`, and `oslog` are absent
   from `cargo tree -p denarii-bridge -e normal --target aarch64-linux-android`.
6. The generated files are exactly what codegen 2.13.0 writes from `api`.

## Tests

| Invariant | Checked by |
| :- | :- |
| 2, 3, 4 | `tests/money.rs`, which covers each error variant, the locale symbols, separators of zero, two, and supplementary-plane characters, and 100,000-character text; the doc tests in `src/lib.rs` and `src/api/money.rs` |
| 1, 5 | Review of `rust/Cargo.toml` and `cargo tree`; no automated check yet |
| 6 | The `generated` job in `.github/workflows/ci.yml`, which installs codegen 2.13.0, runs it again, and fails if any file differs from the committed one (ADR-0012). The same job fails when the crate's version in `rust/Cargo.lock`, the two Dart packages' versions in `pubspec.lock`, and the codegen's version differ |
| Dart reaches the packaged library on a device | `integration_test/bridge_test.dart`: `flutter test integration_test` on an emulator, which calls the real library and checks the placeholder screen |
| The view model and screen | `test/features/auth/state/sign_in_placeholder_view_model_test.dart` and `test/features/auth/ui/sign_in_placeholder_route_test.dart`, with a fake `MoneyParser` |

Run the Rust tests with `cargo test --workspace --locked` from `rust/`, and the device tests with
`flutter test integration_test -d <device>` from the repository root.
