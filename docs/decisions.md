# Decision log

Each significant or hard-to-reverse decision gets one architecture decision record (ADR) here, in
the order it was made. Records are never renumbered. An accepted decision changes only through a
new record that supersedes it. `Proposed` records wait for the maintainer.

## ADR-0001 — Rewrite the app in Flutter and Rust, replacing Kotlin Multiplatform

- **Status:** Accepted
- **Date:** 2026-11-23
- **Context:** v1 is a Kotlin Android app of about 22,000 lines in 241 files (`app/` and
  `shared/` at commit `6d57526`). Its plan for iOS was Kotlin Multiplatform with Compose
  Multiplatform ("Phase 2" in v1's `CLAUDE.md`): code moved into the `:shared` module one reviewed
  step at a time, and the Room database would follow once an iOS spike passed. That spike hadn't
  run. On 2026-11-23 the maintainer asked for a complete rewrite as a Flutter and Rust app, and
  said the Kotlin Multiplatform plan is dropped.
- **Decision** (the maintainer's request on 2026-11-23): build Denarii Dolor as a Flutter app
  written in Dart, over a Rust core, for Android and iOS. The Doctrina rules `commits.md`,
  `flutter.md`, `roadmap-and-docs.md`, `rust.md`, and `writing.md` govern the work, in
  `.claude/rules/`.
- **Consequences:**
  - Good: one user interface for both platforms; the rules and the data code are written once,
    in a language with no garbage collector, so secrets in Rust memory can be wiped.
  - Bad: every screen and every piece of storage code is written again, and the new code must
    match v1's stored formats byte for byte (ADR-0002). Two languages cross a bridge (ADR-0005),
    and the Keystore and biometric code still needs Kotlin, and later Swift.
  - Follow-up: the milestones in `docs/roadmap.md`.
- **Alternatives considered:** continuing Kotlin Multiplatform with Compose Multiplatform, which
  the maintainer declined; a Flutter app with its storage in Dart (for example drift over
  SQLCipher), which keeps secrets and records in Dart's garbage-collected memory and wasn't asked
  for.
- **Revisit when:** the M1 exit criteria fail because Flutter can't host the Rust core on API 26,
  or the M3 exit criteria fail because a v1 install can't be opened from the new app.

## ADR-0002 — Existing installs upgrade in place

- **Status:** Accepted
- **Date:** 2026-11-23
- **Context:** Paying users have their records in a SQLCipher database whose key is wrapped by
  Android Keystore keys that belong to `com.denariidolor`. Nothing can repair a user's data by
  hand. On 2026-11-23 the maintainer was asked whether the new app should upgrade v1 installs in
  place, migrate them through a backup file, or start fresh.
- **Decision** (the maintainer chose "upgrade in place" on 2026-11-23): the new app keeps the
  application ID `com.denariidolor` and the signing setup, ships to the same Google Play listing as
  an update, and opens what v1 left on the phone without any action from the user. Kotlin code in
  `android/` keeps using v1's Keystore keys; Rust opens the same SQLCipher file and reads the same
  profile. `docs/specs/android-v1-formats.md` is the contract.
- **Consequences:**
  - Good: users only update; nobody has to remember to export a backup first.
  - Bad: the new code must reproduce v1's key wrapping, profile layout, schema, and backup format
    exactly, including edge cases such as v1.0.x profiles and short PINs from before 2026-09-21.
  - Follow-up: fixtures written by the v1 app itself (M2.3, M3.3, M5.1), and an upgrade test on a
    real phone (M3 exit criteria, M6.4).
- **Alternatives considered:** migrating through a backup file, which relies on each user acting
  before the update, so anyone who doesn't loses their data; a fresh start, which loses every
  user's records.
- **Revisit when:** a v1 format proves impossible to read from the new app on some API level,
  found by M2.1, M3.2, or M6.4.

## ADR-0003 — Replace the Kotlin app in this repository now

- **Status:** Accepted
- **Date:** 2026-11-23
- **Context:** The new app could have grown beside the Kotlin app until it reached parity, or
  replaced it at once, or moved to a new repository. On 2026-11-23 the maintainer was asked which.
- **Decision** (the maintainer chose "replace now" on 2026-11-23): remove the Kotlin app, its
  Gradle build, its release pipeline (`cd.yml` and the `Makefile`), and its Kotlin-only skills from
  the working tree, and build the new app at the repository root. v1's source stays readable at
  commit `6d57526` with `git show 6d57526:<path>`.
- **Consequences:**
  - Good: one app in the tree, so no reader confuses the old code with the new; the docs and rules
    describe one stack.
  - Bad: no release is possible from `main` until the new app reaches parity (M6). A v1 fix would
    ship from a branch cut at `6d57526`, with the Kotlin toolchain and the removed pipeline
    restored on that branch.
  - Follow-up: the hotfix-branch item in the roadmap's backlog; a new release pipeline in M6.1.
- **Alternatives considered:** building beside the Kotlin app until parity, which keeps v1
  releasable from `main` but doubles what readers and CI must handle; a new repository, which
  splits the history and the issue tracker.
- **Revisit when:** v1 needs a fix before M6 ships.

## ADR-0004 — Rust owns the rules and the data; Dart owns the screens

- **Status:** Proposed
- **Date:** 2026-11-23
- **Context:** ADR-0001 puts Rust under a Flutter app but doesn't say where the line between them
  runs. v1 already separated pure rules (`Ledger`, `Money`, `Validators`) from Android code, and
  kept the database behind use cases and repositories.
- **Decision:** Rust holds the domain model, money parsing, validation, balance changes, the
  database, the vault logic, backups, and report calculations, in the crates listed in
  `docs/01-architecture.md`. Dart holds layout, navigation, input, and formatting for the user's
  locale. Kotlin and Swift hold only what needs the operating system (ADR-0007).
- **Consequences:**
  - Good: each rule has one implementation, tested with `cargo test` on any host; the database
    and keys never live in Dart's memory.
  - Bad: every screen's data crosses the bridge, so the bridge's API must be designed with care;
    money formatting for the screen lives in Dart, apart from parsing, which lives in Rust.
  - Follow-up: the bridge API (M1.2); Dart formatting that matches v1's snapshots (M1.6).
- **Alternatives considered:** Rust for crypto only, with the rules in Dart, which puts the rules
  where they can't be shared with any future non-Flutter client and splits the storage code;
  everything in Dart, which ADR-0001 rules out.
- **Revisit when:** a screen needs data that the bridge can't deliver within the 16 ms frame
  budget in `docs/00-overview.md`.

## ADR-0005 — Dart calls Rust through `flutter_rust_bridge`

- **Status:** Accepted by the maintainer on 2026-11-23, in M1.2. ADR-0014 adds the bridge's
  version, features, and build.
- **Date:** 2026-11-23
- **Context:** Dart reaches native code through `dart:ffi`, which needs a C interface on the Rust
  side and `unsafe` code to build one. `flutter_rust_bridge` generates both sides from ordinary
  Rust functions and types, runs calls on a worker pool off the UI thread, and builds the Rust
  library for each Android and iOS target. Its license, version, and size are to be confirmed when
  M1.2 proposes it.
- **Decision:** use `flutter_rust_bridge` for every call from Dart into Rust, from one `bridge`
  crate. Its generated foreign function interface code is the only `unsafe` code allowed (the
  "Unsafe policy" in `.claude/rules/rust.md`).
- **Consequences:**
  - Good: no hand-written `unsafe`; Rust errors and records cross as typed Dart values.
  - Bad: a code generator and its runtime in both languages, which the bridge's version pins
    together; the generated code needs a commit policy (decided in M1.1).
  - Follow-up: M1.2.
- **Alternatives considered:** hand-written `dart:ffi` over a C interface, which needs `unsafe`
  code and manual memory management for every call; Pigeon, which reaches only Kotlin and Swift,
  not Rust.
- **Revisit when:** the bridge's generator stops supporting the pinned Flutter release, or its
  build fails for an Android or iOS target the app ships.

## ADR-0006 — The database stays SQLCipher, opened from Rust with `rusqlite`

- **Status:** Proposed
- **Date:** 2026-11-23
- **Context:** v1's records are in a SQLCipher 4 database (`net.zetetic:sqlcipher-android`
  4.19.1) with a raw 256-bit key and default settings, in Room's schema version 2
  (`docs/specs/android-v1-formats.md`, sections 5 and 6). ADR-0002 requires the new app to open it
  as it is. `rusqlite` can compile SQLCipher into the Rust library, with its own copy of the
  cryptography library it needs.
- **Decision:** `store` opens the database with `rusqlite` and its bundled SQLCipher build,
  continues from `PRAGMA user_version` 2 with its own forward-only migrations, and leaves Room's
  bookkeeping table in place.
- **Consequences:**
  - Good: the file on the phone stays as it is; queries are SQL with bound parameters, as in v1.
  - Bad: building SQLCipher and its cryptography library for four Android targets and the iOS
    targets lengthens builds and adds native code to audit; the app ships two copies of SQLite's
    code if a plugin brings its own.
  - Follow-up: M2.1 opens fixture F3 from the format spec before any other storage work.
- **Alternatives considered:** keeping Room in Kotlin and calling it from Dart, which keeps the
  rules split across two languages; drift with SQLCipher in Dart, which ADR-0004 rules out.
- **Revisit when:** M2.1 can't open fixture F3 with the bundled build.

## ADR-0007 — Hardware-key operations stay in platform code

- **Status:** Proposed
- **Date:** 2026-11-23
- **Context:** v1 wraps the database key under keys derived from the PIN and the security answer:
  `HMAC-SHA256(device key, context || PBKDF2-HMAC-SHA256(secret, salt, iterations))`, where the
  device key is a non-exportable Android Keystore key, so guesses can only be checked on the phone
  (CS-15). The profile is sealed under a Keystore storage key, and biometric sign-in decrypts
  through a `CryptoObject` that `BiometricPrompt` authorizes (CS-07). Only code running in the
  app's Java runtime can use these keys and that prompt. Every option below keeps them, so
  ADR-0002 holds; they differ in what passes through Dart.
- **Decision:** proposed, for the maintainer to choose in M3.1 after a spike: Rust runs PBKDF2,
  AES-GCM, the profile rules, and the lockout; Kotlin offers only the operations that need a
  Keystore key (the device-key HMAC, sealing and opening the profile, and the biometric cipher).
  Rust calls Kotlin for them directly through the Java Native Interface (JNI), so no secret passes
  through Dart. On iOS, Swift offers the same operations over the Keychain and the Secure Enclave
  (M7.1).
- **Consequences:**
  - Good: secrets stay in memory that the app can wipe; one implementation of the vault rules.
  - Bad: Rust needs a JNI binding and the app's Java runtime handle, which adds a dependency and
    `unsafe` code that the "Unsafe policy" doesn't allow yet.
  - Follow-up: M3.1 decides; M3.2 builds the Kotlin side.
- **Alternatives considered:** keeping v1's whole vault in Kotlin and passing only the 32-byte
  database key to Rust through Dart, which is the least new code but leaves the key in Dart's
  memory for a moment and needs a second vault in Swift; calling Kotlin through Pigeon from Dart
  for each Keystore operation, which needs no JNI but passes stretched secrets through Dart.
- **Revisit when:** M3.1's spike shows JNI from Rust can't reach the Keystore on API 26.

## ADR-0008 — The app stays offline

- **Status:** Accepted. ADR-0013 lets debug and profile builds on Android keep Flutter's `INTERNET`
  permission.
- **Date:** 2026-11-23
- **Context:** v1 has no `INTERNET` permission, no network code, and no analytics, crash
  reporting, or ads, and `PRIVACY.md` promises that the app collects and shares nothing. The rule
  predates this log: v1's `CLAUDE.md` ("Security posture") recorded it, and the maintainer kept it
  in every version. Recorded here so the overview's non-goals can cite it.
- **Decision:** the rewrite opens no network connection and asks for no network permission on any
  platform. No dependency that sends data anywhere is allowed (`docs/02-dependency-policy.md`).
- **Consequences:**
  - Good: network attackers are out of the threat model; `PRIVACY.md` and the Play Data safety
    form stay simple.
  - Bad: no cloud backup or sync; users move data between phones with backup files only.
  - Follow-up: M3.8 checks the merged Android manifest for the permission.
- **Alternatives considered:** none were proposed.
- **Revisit when:** the maintainer decides to offer sync or cloud backup, which needs a new
  record, a `PRIVACY.md` change, and a Data safety update.

## ADR-0009 — Pin Flutter 3.50.1 and support iOS 15 and later

- **Status:** Accepted
- **Date:** 2026-11-23
- **Context:** M1.1 needs a Flutter release and an iOS minimum. On 2026-11-23 the stable release was
  Flutter 3.50.1 with Dart 3.14.0, released 2026-11-20. Flutter 3.50 supports Android API 24 to 37
  and iOS 15 to 27 (the "Supported deployment platforms" page on docs.flutter.dev, updated
  2026-11-11). `flutter_rust_bridge` 2.13.0, its latest stable release, was tested against Flutter
  3.44. v1's Android levels (minSdk 26, targetSdk 36, compileSdk 37) are inside Flutter's range. The
  maintainer was offered 3.50.1 with iOS 15, 3.47.6 with iOS 15, or 3.50.1 with iOS 16.
- **Decision** (the maintainer chose Flutter 3.50.1 with iOS 15 on 2026-11-23): pin Flutter 3.50.1
  exactly in `pubspec.yaml` (`environment: flutter: 3.50.1`, with `sdk: ^3.14.0` for Dart), so
  `flutter pub get` fails on any other Flutter release. No version-manager tool. Android keeps v1's
  levels, and iOS supports version 15 and later.
- **Consequences:**
  - Good: the newest fixes and APIs; the version is stated in one file that the build itself
    enforces; no extra tool to install or trust.
  - Bad: `flutter_rust_bridge` isn't verified on Flutter 3.50 yet; every contributor and CI must
    install exactly 3.50.1; each upgrade is a change of its own.
  - Follow-up: M1.1 writes the pin; M1.2 checks `flutter_rust_bridge` on 3.50.1 before anything
    else; M1.3 makes CI install the pinned release; M7.1 revisits the iOS minimum.
- **Alternatives considered:** Flutter 3.47.6, the previous line with more patch releases, which
  is just as unverified with the bridge and older by release time; iOS 16, which drops iOS 15
  devices for no API the app needs; a version manager such as FVM with an `.fvmrc` file, which adds
  a tool while the `pubspec.yaml` pin already enforces the version.
- **Revisit when:** `flutter_rust_bridge` fails on Flutter 3.50.1 in M1.2, or Flutter raises its
  iOS minimum above 15 before M7 ships.

## ADR-0010 — Screens hold state in `ChangeNotifier` view models, with services from two scopes

- **Status:** Accepted
- **Date:** 2026-11-23
- **Context:** Each v1 feature had a route that wired a ViewModel (state as a `StateFlow`) to a
  stateless screen. Dependencies came from two Metro graphs: `AppGraph`, for process-wide services
  and sign-in, and `SessionGraph`, for everything that reads the database, created after sign-in and
  dropped with the signed-in activity. The flutter rule allows one state-management approach. The
  maintainer was offered `ChangeNotifier` with no package, Riverpod 3 (3.5.0), or Bloc
  (`flutter_bloc` 9.1.1, last released in April 2025).
- **Decision** (the maintainer chose `ChangeNotifier` with no package on 2026-11-23):
  - Each screen has a view model that extends `ChangeNotifier`, holds immutable state, calls the
    bridge, and exposes one write at a time (`isSaving`).
  - A route widget creates its view model, passes state and callbacks to a stateless screen, and
    disposes the view model with itself.
  - Services reach view models through their constructors, from two `InheritedWidget` scopes: an
    app scope (the bridge, the platform channels, settings) created at startup, and a session scope
    (everything that reads the database) created after sign-in and disposed when the vault locks.
  - Tests build view models directly, with hand-written fakes.
- **Consequences:**
  - Good: no dependency; the same shape as v1, so screens port one to one; view models are plain
    Dart that tests use without a widget tree; the session scope makes "the database exists only
    after sign-in" a structural fact.
  - Bad: the two scope widgets and every disposal are written by hand; there is no built-in
    caching or async state helper, so each view model handles its own loading and errors.
  - Follow-up: M1.1 adds the app scope and the first view model; M3.5 adds the session scope.
- **Alternatives considered:** Riverpod 3, which builds dependency injection and scoping in, but
  adds at least two packages and idioms of its own; Bloc, with no release since April 2025, the most
  boilerplate of the three, and `provider` pulled in with it.
- **Revisit when:** state must be shared across three or more screens in a way the two scopes
  can't express, or "setState() called after dispose()" bugs appear in two different view models.

## ADR-0011 — Navigate with Flutter's `Navigator`, switching the root between signed-out and signed-in

- **Status:** Accepted
- **Date:** 2026-11-23
- **Context:** v1 navigated with string routes in Navigation Compose (`MainDestination`, recorded
  as known debt because the routes aren't type-safe). Sign-in lived in its own activity, and the
  signed-in activity checked the session before building any UI (CS-02). v1 has no deep links: only
  the launcher activity is exported. The maintainer was offered Flutter's `Navigator` with no
  package, or `go_router` 18.1.4 (by flutter.dev, declared feature-complete).
- **Decision** (the maintainer chose `Navigator` with no package on 2026-11-23): the root widget
  follows the vault's lock state and shows either the sign-in flow or the signed-in tree, which has
  its own `Navigator` inside the session scope (ADR-0010). Locking replaces the signed-in tree, so
  its screens and view models are disposed. Screens are pushed as widget classes whose constructor
  arguments are the IDs they need, and Back is handled with `PopScope`.
- **Consequences:**
  - Good: no dependency; typed arguments without code generation; CS-02 holds by construction,
    because no signed-in screen exists while the vault is locked.
  - Bad: no URL routes, so a future deep link needs a new record.
  - Follow-up: M1.1 builds the root switch with placeholder screens; M3.5 connects it to the vault.
- **Alternatives considered:** `go_router`, whose declarative routes and redirect guard would also
  enforce the lock, but whose typed routes need `go_router_builder` (code generation), and whose
  string routes would repeat v1's known debt.
- **Revisit when:** the app needs deep links, app links, or a notification that opens a given
  screen.

## ADR-0012 — Commit generated code, and check in CI that it is current

- **Status:** Accepted
- **Date:** 2026-11-23
- **Context:** `flutter_rust_bridge_codegen` writes Dart bindings (`lib/src/rust/`) and Rust glue
  (`frb_generated*.rs`), and `flutter gen-l10n` writes the localizations class from the ARB files.
  The `flutter_rust_bridge` documentation doesn't say whether to commit its output. This was the
  last open question of M1.1, drafted for the maintainer's review.
- **Decision** (the maintainer accepted the draft on 2026-11-23): commit every generated file and never edit one by hand. CI regenerates them and
  fails if the result differs from what is committed (M1.3). The analyzer and the formatter skip
  only the generated paths.
- **Consequences:**
  - Good: a fresh clone builds without running a generator; reviewers see binding changes in the
    diff; a generator upgrade shows exactly what it changed.
  - Bad: larger diffs; every change to the bridge API or the ARB files includes regenerated files.
  - Follow-up: M1.2 (bindings), M1.3 (the CI check), M1.4 (localizations).
- **Alternatives considered:** ignoring generated output and running the generators in every
  build, which requires the generators on every machine and in CI, and hides generator drift from
  review.
- **Revisit when:** generated files make up most of the lines in typical pull requests, or a
  generator's output changes between two runs on the same input.

## ADR-0013 — Debug and profile builds keep Flutter's `INTERNET` permission

- **Status:** Accepted
- **Date:** 2026-11-23
- **Context:** ADR-0008 (the app stays offline) says the rewrite asks for no network permission on
  any platform. `flutter create` in Flutter 3.50.1 adds `android.permission.INTERNET` to
  `android/app/src/debug/AndroidManifest.xml` and `android/app/src/profile/AndroidManifest.xml`,
  but not to the main manifest. In those two builds the Flutter tool connects to the Dart VM service
  (the debugging server inside the running app) over a socket, and hot reload, the debugger,
  DevTools, and `flutter test integration_test` (M1.2) all need that connection. Android merges a
  build type's manifest only into that build type, so a release build gets neither file. On
  2026-11-23, during M1.1, the maintainer was asked whether to keep the permission in those two
  manifests or remove it everywhere.
- **Decision** (the maintainer chose to keep it in debug and profile builds only, on 2026-11-23):
  keep Flutter's `INTERNET` permission in the debug and profile manifests, each with a comment that
  cites this record. Never add it to `android/app/src/main/AndroidManifest.xml` or to a release
  source set. The release build, which is the one uploaded to Google Play, asks for no network
  permission, as ADR-0008 requires.
- **Consequences:**
  - Good: `flutter run`, hot reload, the debugger, and integration tests work on emulators and
    devices, and the Google Play build keeps the promise in `PRIVACY.md` and the store listing.
  - Bad: debug and profile builds can open sockets, so a check of a debug build doesn't show what
    the release build asks for. A plugin can also add the permission to the release build through
    its own manifest, so only the merged release manifest is proof.
  - Follow-up: M3.8 checks the merged release manifest for the permission; M6.1 builds only release
    bundles for upload.
- **Alternatives considered:** removing the permission from every manifest, which keeps every
  build offline but stops the Flutter tool from attaching to the app: no hot reload, no debugger,
  and no integration tests on a device.
- **Revisit when:** the Flutter tool can attach to an Android app without a network socket, or a
  merged release manifest is found to contain `INTERNET`.

## ADR-0014 — Build the bridge with Flutter's native-assets hook, with `flutter_rust_bridge`'s default features off

- **Status:** Accepted
- **Date:** 2026-11-23
- **Context:** M1.2 adds the `bridge` crate under ADR-0005 (Dart calls Rust through
  `flutter_rust_bridge`). On 2026-11-23 the maintainer was asked three questions that ADR-0005
  left open, and one more after the first answer turned out not to compile:
  - **Features.** `flutter_rust_bridge` 2.13.0, released 2026-08-23 under the MIT license, turns
    on eight Cargo features by default. Two of them, `log` and `user-utils`, bring the `log`,
    `android_logger`, and `oslog` crates and a setup function that sends Rust's log records to
    logcat and the iOS console. Shipped code may not log (`CLAUDE.md`, "Security posture"). The
    Dart runtime package depends on `package:logging` from the Dart team, which stays silent
    unless generated code connects a Rust log stream to it.
  - **Build.** Flutter has to compile the Rust library for each Android ABI (the processor
    architecture a native library is built for) and package it in the app. The codegen's default
    way, CargoKit, copies 66 files (about 4,700 lines) of build scripts into the repository as a
    local plugin. Its build tool fetches its own Dart packages, among them `http` and `github`,
    at build time with no lock file. Flutter's native-assets build hooks instead run a Dart script,
    `hook/build.dart`, that Flutter calls with the target ABI and the NDK of its Android build.
    `flutter_rust_bridge` supports them from 2.13.0, through its `flutter_rust_bridge_hooks`
    package over `native_toolchain_rust`.
  - **ABIs.** M1.2's `Do` line listed `arm64-v8a`, `armeabi-v7a`, `x86_64`, and `x86`, but
    Flutter 3.50.1 builds apps only for `android-arm`, `android-arm64`, and `android-x64`
    (`flutter build apk -h`), so an `x86` library could never load.
  - **The features the generated code needs.** The maintainer first chose `thread-pool` alone,
    but the code that the 2.13.0 codegen writes uses items behind `rust-async` and `dart-opaque`
    as well, and doesn't compile without them. The maintainer was asked again the same day. With
    those three features on, 30 third-party crates ship in the Android library:
    `flutter_rust_bridge`, `tokio` 1.54.1, and `slab` under MIT; `allo-isolate` under Apache-2.0;
    and each of the other 26 under MIT or, at the licensee's choice, another permissive license
    (Apache-2.0, 0BSD, Unlicense, or Zlib). `dart-sys` compiles `dart_api_dl.c` from the Dart SDK
    3.2.3 headers, under the Dart project's BSD-3-Clause license, with the NDK's C compiler.
- **Decision** (the maintainer chose the recommended option for each question on 2026-11-23, and
  the three features below when asked again):
  - Pin `flutter_rust_bridge` 2.13.0 exactly in the Rust crate, the Dart package, the codegen,
    and `flutter_rust_bridge_hooks`, and change them only together.
  - Turn the crate's default features off, and turn on only the three that the generated code
    needs: `thread-pool`, which runs each call on a worker thread off Flutter's UI thread, and
    `rust-async` and `dart-opaque`. The bridge sets up no logger. `package:logging` is
    allowed in the app as a dependency of the Dart runtime, on the condition that nothing in the
    app connects a Rust log stream or listens to its records.
  - Build with Flutter's native-assets hook: `hook/build.dart` calls `flutter_rust_bridge_hooks`,
    which runs `cargo build` through `native_toolchain_rust` with the NDK of Flutter's Android
    build. `native_toolchain_rust` reads an exact Rust version and a `targets` list from a
    `rust-toolchain.toml` beside the crate's `Cargo.toml`, so `rust/crates/bridge/rust-toolchain.toml`
    is a symbolic link to `rust/rust-toolchain.toml`, and the toolchain is still pinned in one file.
  - Build `arm64-v8a`, `armeabi-v7a`, and `x86_64`, the ABIs Flutter supports, and not `x86`.
  - Exempt only the generated Rust module from `unsafe_code` and the other lints it trips, with
    `#[expect(…, reason = "…")]` on its `mod` line. Cargo can't relax one lint that a crate
    inherits from the workspace, so the `bridge` crate repeats the workspace's lints in a table of
    its own, with `unsafe_code` at `deny` instead of `forbid`.
- **Consequences:**
  - Good: no logging crate ships, and no Rust log record can reach logcat. Every package the
    build hook uses is pinned in `pubspec.lock`, and no build script is copied into the repository. The
    Rust library builds with the same NDK as the rest of the app, and its 64-bit builds are aligned
    to 16 KB memory pages, which Google Play requires for 64-bit ABIs.
  - Bad: `rust-async` makes the bridge create a multi-threaded `tokio` runtime at its first call,
    with one worker thread per CPU core that waits idle, because no bridge function is `async`.
    The native-assets way is new in `flutter_rust_bridge` (2.13.0 is its first stable release
    with it), and `native_toolchain_rust` has a single maintainer. Every Flutter build and
    `flutter test` run now compiles the Rust library, so wherever Flutter builds, the Rust
    toolchain and its targets must be installed. The codegen runs `cargo expand`, and installs the
    latest `cargo-expand` by itself when it is missing, so contributors install a pinned version
    first (`CONTRIBUTING.md`). The bridge's lints table must change whenever the workspace's does.
  - Follow-up: M1.3 (CI installs the targets, the codegen, and `cargo-expand`, and checks that the
    generated files are current); M6.2 (notices for the new crates and packages); M7.1 (the iOS
    targets in `rust-toolchain.toml`).
- **Alternatives considered:** the default features, which ship three logging crates;
  `thread-pool` alone, which the generated code doesn't compile with;
  CargoKit, which copies build scripts into the repository and fetches unlocked packages at build
  time; a hand-written Gradle task that runs `cargo-ndk`, which leaves iOS needing its own build
  script in M7 and the glue code to maintain; building `x86` as well, which no Flutter app can
  load.
- **Revisit when:** `flutter_rust_bridge` drops its native-assets support, `native_toolchain_rust`
  stops supporting the pinned Flutter release, or a Flutter release adds or drops an Android ABI.
