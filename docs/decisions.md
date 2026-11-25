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

- **Status:** Accepted by the maintainer on 2026-11-23, in M2.1. ADR-0017 adds the crates'
  versions, the OpenSSL build, and how `store` holds the connection.
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

- **Status:** Accepted by the maintainer on 2026-11-25, in M3.1, after the spike in its context.
- **Date:** 2026-11-23
- **Context:** v1 wraps the database key under keys derived from the PIN and the security answer:
  `HMAC-SHA256(device key, context || PBKDF2-HMAC-SHA256(secret, salt, iterations))`, where the
  device key is a non-exportable Android Keystore key, so guesses can only be checked on the phone
  (CS-15). The profile is sealed under a Keystore storage key, and biometric sign-in decrypts
  through a `CryptoObject` that `BiometricPrompt` authorizes (CS-07). Only code running in the
  app's Java runtime can use these keys and that prompt. Every option below keeps them, so
  ADR-0002 holds; they differ in what passes through Dart.
  - **M3.1's spike.** On 2026-11-25 the maintainer chose to build the spike in a throwaway clone
    of commit `78871b5`, outside the repository, with `jni-sys` 0.4.1 (MIT OR Apache-2.0: raw
    Rust declarations of the Java Native Interface header, `jni.h`) for the calls; none of it is
    merged. `jni` 0.22.4, the safe wrapper from the same project, was the other crate offered,
    and it depends on the `log` crate, which can't be turned off. A Kotlin object made a device
    key (HMAC-SHA256) and a storage key (AES-256-GCM) with `KeystoreSecrets.kt`'s parameters,
    under test-only aliases. Rust in the bridge called it, and a Dart integration test called the
    bridge, in a debug build on the `Denarii_API_26` (Android 8.0) and `Denarii_API_36`
    (Android 16) x86_64 emulators. The Android runtime's JNI checks (CheckJNI) were on, and both
    runs passed.
  - **Reaching the Java runtime.** Dart opens `libdenarii_bridge.so` itself, and that doesn't run
    the library's `JNI_OnLoad`. When `MainActivity` loads the library first, with
    `System.loadLibrary`, the runtime calls `JNI_OnLoad`, which keeps the `JavaVM` handle. Dart's
    later open reaches the same loaded library, so Rust found the handle on both API levels.
  - **The class loader.** The bridge's worker threads start in native code, so when Rust attaches
    one to the runtime, it looks classes up with the system class loader: `FindClass` for the
    app's class threw `ClassNotFoundException` there on both levels. Looking the class and its
    methods up once in `JNI_OnLoad`, which uses the app's class loader, and keeping a global
    reference to the class worked from every thread.
  - **The operations.** From a worker thread, the device-key HMAC returned 32 bytes, the same for
    the same input and different for another. Sealing 32 bytes returned 60: a 12-byte IV that
    the Keystore chose, new each time, then the ciphertext and the 16-byte tag. Opening returned
    the plaintext. Wrong associated data made Kotlin throw `javax.crypto.AEADBadTagException`, and
    a 3-byte blob `java.lang.IllegalArgumentException`. Rust cleared each exception and returned
    its class name as an error, and the next call succeeded. Rust overwrote each Java `byte[]` it
    passed or received with zeros after the call, as v1 did with `fill(0)`; the Keystore still
    receives each HMAC input, as it did in v1.
  - **Threads and cost.** Each call attached its worker thread and detached it at the end, and
    eight calls at once, on the bridge's worker threads, all passed. Averaged over 1,000 calls on one
    thread, attaching and detaching took 47 µs on API 26 and 23 µs on API 36, a device-key HMAC
    585 µs and 1,015 µs, and a seal with an open 1,336 µs and 2,789 µs. With eight calls at once,
    an HMAC took 1.4–2.9 ms on API 26 and 2.4–5.7 ms on API 36. A PIN sign-in needs one HMAC,
    one open of the profile, and perhaps a seal of it, a few milliseconds beside PBKDF2's hundreds.
  - **The release build.** R8, the release build's code shrinker, removed the Kotlin class,
    because only native code calls it, so `JNI_OnLoad` couldn't have found it there; the real code
    needs a narrow keep rule. With the spike, the release `x86_64` library grew from 694,632 to
    720,240 bytes, almost all of it the spike's own code, because `jni-sys` holds only
    declarations. The spike's JNI module had about 40 `unsafe` blocks in 515 lines, behind two
    safe functions.
  - **Not covered.** The biometric `CryptoObject` path (M3.6), calls from Kotlin into Rust, v1's
    real `denarii_vault_` keys on a phone with v1 data (M3.2), arm64 phones, and iOS (M7.1).
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

## ADR-0015 — Pick the app's language from the whole phone language list, matching script as Android does

- **Status:** Accepted. ADR-0016 writes money and months in the locale this record picks.
- **Date:** 2026-11-23
- **Context:** M1.4 ports v1's strings to seven ARB files, the language packs: English, Simplified
  Chinese, Hindi, Spanish, Arabic, French, and Portuguese. A phone holds an ordered list of
  languages, and the app must pick one pack from it.
  - **What v1 did.** Android's resource lookup treats two locales as the same language only when
    their scripts match, written in the tag or likely for it (`LocaleList.matchesLanguageAndScript`
    in the Android 37 sources). So zh-Hans-SG read v1's `values-zh-rCN` pack, and zh-TW and
    zh-Hant-HK, which write Traditional characters, read English. Android also picks one locale
    before it looks up any string: the first phone language that any of the app's resources
    match, with English always counted. v1's release bundle carries translations from its Compose
    and AndroidX libraries in about 80 locales (German, Japanese, zh-TW, and others), so on a phone
    set to German then Spanish, Android stopped at German, and v1, which has no German strings,
    showed English.
  - **What Flutter does.** The new app's APK has no translated Android resources
    (`aapt2 dump configurations` lists none), so Android passes the phone's list to Flutter in
    order. Flutter's default, `basicLocaleListResolution`, ignores scripts (zh-TW gets the
    Simplified pack) and falls back to a pack from the phone's country (Uyghur in China, ug-CN,
    gets the Simplified pack too).
  - **gen-l10n.** `flutter gen-l10n` refuses a `zh_CN` pack without a language-only `zh` pack to
    fall back to, and its lookup picks a pack by language code alone.
  - On 2026-11-23 the maintainer was asked twice: first whether to match v1 or keep Flutter's
    default, then, once v1's German-then-Spanish behavior came to light, whether to walk the
    whole list or copy v1 exactly.
- **Decision** (the maintainer chose the recommended option both times on 2026-11-23):
  - `resolveAppLocale` in `lib/l10n/locale_resolution.dart` walks the phone's whole list in
    order and returns the first phone language that matches a pack by language and script. A
    phone language without a script subtag gets its likely script from Unicode CLDR's likely
    subtags: Chinese is Traditional in 15 regions (among them TW, HK, MO, and US) and Simplified
    everywhere else; the other six languages have one script each.
  - It returns the phone's own locale, such as es-MX, so Material's widgets keep its region. When
    nothing matches, it returns English.
  - It never falls back to a pack from the phone's country.
  - The Simplified Chinese pack is `app_zh.arb`, which only Simplified Chinese readers reach.
- **Consequences:**
  - Good: a phone set to German then Spanish shows Spanish, the language its owner listed, as
    Android's documentation describes. Traditional Chinese readers still see English, as in v1,
    rather than Simplified characters. The rule is one tested function, with no list of library
    locales to keep.
  - Bad: the app differs from v1 for anyone whose first phone language is one that v1's
    libraries translated but no pack covers, followed by a pack language: v1 showed English,
    and the app shows the later language. When nothing matches, the app's locale is English, so
    Material's widgets write dates the English way. v1 formatted money and months with the
    device's own locale data (`PlatformFormat.kt`), and M1.6 decides how Dart matches that.
  - Follow-up: M3.2. Android moves the first phone language that matches any of the APK's own
    resources, or English, to the front before Flutter sees the list. Once the APK carries
    translated Android resources (`androidx.biometric` has some), a pack language without them
    could lose to a later English, so check that every pack language has Android resources too.
- **Alternatives considered:** Flutter's default, which gives Traditional Chinese readers and
  ug-CN the Simplified pack; copying v1 exactly, with the ~80 locales its libraries translated
  embedded in the app, which would freeze an accident of v1's dependency list; checking only the
  first phone language, which shows English to a German-then-Spanish phone for no reason the
  user can see.
- **Revisit when:** a Traditional Chinese pack is added, the app offers its own language setting
  or declares its languages to Android (a `LocaleConfig`, which changes how Android hands over the
  list), or a user reports the app in a language they didn't list.

## ADR-0016 — Write money and months with `intl`, with currency symbols from CLDR and fixes for Arabic regions

- **Status:** Accepted
- **Date:** 2026-11-23
- **Context:** M1.6 ports v1's money and month text (`PlatformFormat.kt`) to Dart with
  `package:intl` 0.20.3, whose number and date data come from Unicode CLDR 48.
  - **What v1 showed.** v1 wrote money with `java.text.NumberFormat` and the device's default
    locale, and months with the `java.time` pattern `MMMM yyyy`. Its `MoneySnapshotTest.kt` pins
    29 locale and currency rows of eight amounts each, and 15 month rows. JDK 17.0.20.1, v1's test
    toolchain (CLDR 39), reproduces every row. On phones, though, `java.text` runs on Android's
    ICU data, which changes between releases: on 2026-11-23 the same code on an API 26 emulator
    differed from the snapshot in 19 of the 29 money rows, and on an API 36 emulator (Android 16)
    in 11.
  - **What `intl` lacks.** With v1's symbols supplied, `intl` alone matched 164 of the 232 money
    strings and 14 of the 15 month rows. Its gaps:
    - It has one symbol per currency for every language (`simpleCurrencySymbols`): US dollars are
      `$` in Mexico, as Mexican pesos are, and Saudi riyals are `Riyal` in Arabic text. v1 wrote
      `USD` and `ر.س.` (followed by a right-to-left mark).
    - It has Arabic number data only for `ar`, `ar_DZ`, and `ar_EG`. Plain `ar` writes Latin
      digits, as Android 16 does, but Saudi Arabia and 19 other regions write Arabic-Indic digits
      there, and Morocco, Tunisia, and Libya a decimal comma.
    - It ignores CLDR's separate decimal mark for money, which Swiss French sets to `.`.
    - It has no Moroccan month names.
  - **Where `intl` agrees with phones.** Indian grouping (`₹12,34,567.89`), the right-to-left mark
    at the start of Arabic amounts, and the Moroccan currency's position match Android 16, though
    not the JDK 17 snapshot.
  - On 2026-11-23 the maintainer was asked three questions, each with the options below.
- **Decision** (the maintainer chose the recommended option for all three on 2026-11-23):
  - **Symbols:** `tool/CurrencySymbols.java` writes `lib/l10n/currency_symbols.dart` from the
    locale data of Java 25 (CLDR 47): the symbol that each of Java's 266 locales in the app's seven
    languages writes for every currency with two decimals, 476 entries in all. A currency the table
    doesn't list is written as its ISO 4217 code, as CLDR does. CI reruns the tool with the
    runner's Java 25 (`JAVA_HOME_25_X64`) and fails on any difference (ADR-0012).
  - **Locale:** money and months are written in the locale that `resolveAppLocale` returned
    (ADR-0015), which widgets read with `Localizations.localeOf`: the phone language that matched
    a pack, with its region, or English when none matched.
  - **Gaps:** `lib/l10n/money_format.dart` sends each Arabic region to `intl`'s Egyptian data
    (Arabic-Indic digits) or Algerian data (Latin digits and a decimal comma), as Android 16
    groups them, and writes Swiss French money with `.`.
  - Money is `intl`'s currency format with exactly two decimals, fed whole cents through
    `MicroMoney` so no floating point touches the amount. A plus sign goes in front of a positive
    amount when asked, as v1 put it in front of the positive prefix. A month is the month's name
    from `intl`'s date data, a space, and the year in four ASCII digits, which is what `MMMM yyyy`
    wrote.
- **Consequences:**
  - Good:
    - Each currency keeps the symbol its language gives it: `US$` in British English, Chinese, and
      Portuguese, `$US` in French, `USD` in Mexican Spanish, and Arabic symbols in Arabic.
    - Saudi, Egyptian, and Moroccan users see the digits and separators Android 16 shows them.
      Swiss French users keep `1 234.50 CHF`.
    - Money and words on one screen use the same language.
    - `test/money_format_test.dart` checks every row of v1's snapshot, and lists each row where
      the app differs with the reason.
  - Bad:
    - The table and the Arabic map are data the project keeps. The table changes when Java's CLDR
      data does, and CI fails until someone reruns the tool.
    - A phone whose first language has no pack writes money the English way: a German-only phone
      showed `1.234,50 $` in v1 and shows `$1,234.50` now.
    - Flutter's `Locale` carries no Unicode extensions, so a phone set to Latin digits for Arabic
      (`-u-nu-latn`) still gets its region's digits.
    - The text differs from v1's snapshot in these rows, each listed in the test: Indian grouping
      in en-IN and hi-IN, the right-to-left mark in ar-SA and ar-EG, Moroccan amounts and month
      names, and Canadian French dollars (`$`, where JDK 17 wrote `$ CA`).
  - Follow-up:
    - M1.3: the "Generated code is current" job runs the tool, so its pull request check covers
      the step.
    - M4.6 passes the saved currency, which v1 kept as `currency` in `ui_prefs`.
    - M6.2 lists the CLDR data in `THIRD_PARTY_NOTICES.md` under the Unicode License V3.
- **Alternatives considered:**
  - For the symbols, `intl`'s single symbols, which make US dollars look like pesos and write
    Arabic currencies in Latin letters; or ISO codes everywhere, which never mislead but match no
    row of v1's.
  - For the locale, the phone's first language as v1 did, which would need symbols for every
    language rather than seven.
  - For the gaps, keeping `intl`'s data, which would switch Saudi users from Arabic-Indic digits to
    Latin ones.
  - Asking the platform's own formatter through a channel, which would match v1 on each Android
    release but needs Kotlin, Swift, and an asynchronous call before M3.2 sets up platform code.
- **Revisit when:** `intl` ships symbols per locale or data for more Arabic regions; GitHub's
  `ubuntu-24.04` image drops Java 25; the app gets its own language setting; or a user reports a
  symbol or digits that differ from the rest of their phone.

## ADR-0017 — Build SQLCipher with OpenSSL compiled from source on every target, and keep the one connection behind a mutex

- **Status:** Accepted
- **Date:** 2026-11-23
- **Context:** M2.1 adds the `store` crate under ADR-0006 (the database stays SQLCipher, opened
  from Rust with `rusqlite`). On 2026-11-23 the maintainer was asked how to build it, how to get a
  database that v1's library wrote, and how `store` serializes calls.
  - **The crates.** `rusqlite` 0.43.0 (MIT) wraps `libsqlite3-sys` 0.40.0 (MIT). Its
    `bundled-sqlcipher` feature compiles the SQLCipher amalgamation (all of SQLCipher's C code in
    one file) from the `sqlcipher-src` crate, 420.0.0: SQLCipher 4.20.0 over SQLite 3.54.1, under
    MIT for the crate, BSD-3-Clause for SQLCipher, and SQLite's public-domain dedication. One
    person publishes `sqlcipher-src`. The crate also holds libtomcrypt (public domain or WTFPL),
    which `libsqlite3-sys` doesn't compile.
  - **The crypto library.** SQLCipher needs a crypto provider. Off Apple platforms,
    `libsqlite3-sys` links OpenSSL's `libcrypto`: the machine's, or, with the
    `bundled-sqlcipher-vendored-openssl` feature, one that `openssl-src` 300.6.1 compiles from
    OpenSSL 3.6.3 (Apache-2.0). Android offers apps no `libcrypto`, so Android builds need the
    compiled one. v1's `sqlcipher-android` 4.19.1 ships its own, with SQLCipher 4.19.0 inside: its
    `libsqlcipher.so` is 2.1 MB for `arm64-v8a`.
  - **What ships.** Seven crates are new to the Android library: `rusqlite`, `libsqlite3-sys`,
    `openssl-sys` (MIT), and `bitflags`, `fallible-iterator`, `fallible-streaming-iterator`, and
    `smallvec` (MIT or Apache-2.0). `libc` already ships with the bridge. Four more run only
    during the build: `sqlcipher-src`, `openssl-src`, `pkg-config`, and `vcpkg` (MIT or
    Apache-2.0). `Cargo.lock` also lists `sqlite-wasm-rs`, `rsqlite-vfs`, `thiserror`, and
    `thiserror-impl`, because Cargo records a crate that a feature names weakly
    (`sqlite-wasm-rs?/sqlcipher` in `rusqlite`) even when nothing turns it on. They are for
    WebAssembly, and `cargo tree --target all` shows that no build of the workspace compiles them.
  - **Measured.** On the maintainer's Linux machine, a scratch crate holding only `rusqlite` with
    the vendored feature built in release mode from an empty target folder in 50 s for
    `aarch64-linux-android` (NDK 28.2.13676358) and 84 s for the host. Its stripped `arm64-v8a`
    library is 6.2 MB, and it needs only Android's `libc`, `libm`, `libdl`, and `liblog`.
  - **Threading.** The bridge's worker pool can run two calls at once (ADR-0014), and
    `rusqlite`'s `Connection` can move between threads but can't be used from two at once.
- **Decision** (the maintainer chose the recommended option for each question on 2026-11-23):
  - `store` depends on `rusqlite` 0.43.0, pinned exactly, with its default features off and only
    `bundled-sqlcipher-vendored-openssl` on. An upgrade changes the SQLCipher build that opens
    users' files, so it goes through the `dependency-upgrade` skill and must keep the v1 fixture
    test passing.
  - OpenSSL is compiled from source on every target, the host included, so `cargo test` runs the
    same SQLCipher and crypto library that phones run.
  - `Store` keeps its one connection behind a `std::sync::Mutex`. Each operation holds the lock for
    the whole of its database work, so two calls never interleave. `Store` is `Send` and `Sync`,
    and the bridge can share it through an `Arc`. A lock poisoned by a panic in another call
    becomes an error, not a second panic. `store` starts no thread.
  - M2.1's fixture is a database that v1's own code wrote with `sqlcipher-android` 4.19.1 on an
    emulator, from an instrumented test in a scratch copy of v1 at `6d57526`. The test's source is
    kept beside the fixture; nothing is added to v1's tree or to the app.
- **Consequences:**
  - Good:
    - One SQLCipher and one crypto library on every target, and the host tests run them.
    - Host builds need no system OpenSSL.
    - Calls can't interleave inside a database transaction, and no thread needs an owner.
  - Bad:
    - Every build from an empty target folder compiles OpenSSL and SQLCipher, which takes more than
      a minute on the maintainer's machine. CI caches nothing (M1.3), so every CI run pays it.
      `cargo test --workspace` from an empty target folder took 86.8 s, against 17.0 s before
      M2.1, over the 60 s budget in `docs/00-overview.md` if that budget counts such a build; with
      nothing to rebuild it took 0.8 s.
    - Builds need `perl` and `make`, which GitHub's `ubuntu-24.04` image has.
    - The Android library grows by about 6 MB per ABI, three times v1's `libsqlcipher.so`, against
      the download-size budget in `docs/00-overview.md`.
    - Users' files were written by SQLCipher 4.19.0, the version `sqlcipher-android` 4.19.1
      reports, and are opened by 4.20.0. `store` pins SQLCipher 4's settings
      (`PRAGMA cipher_compatibility = 4`), and the fixture test checks that they match the file.
    - SQLCipher logs warnings to logcat, the Apple system log, or stderr once it starts, as v1's
      library did. Shipped code doesn't log, so `store` turns that log off.
    - SQLCipher 4.20.0's process-wide setup has two races, found by tests that opened stores on
      several threads at once: SQLite runs SQLCipher's initialization after it has marked itself
      initialized, so a `PRAGMA key` on another thread can fail with "sqlcipher not
      initialized", and two `PRAGMA cipher_log` statements at once can crash the process with
      `fclose(NULL)`. `store` sets SQLCipher up and turns its log off once per process, under a
      lock of its own, before it opens any file (`docs/specs/store.md`). Code outside `store`
      that opens a SQLite connection could still meet the first race.
    - `sqlcipher-src` has one maintainer, and its amalgamation is generated code that nobody here
      has compared with SQLCipher's signed release.
    - A slow database call, such as a long report, makes the next one wait.
  - Follow-up:
    - M6.2 lists SQLCipher, SQLite, OpenSSL, and the crates in `THIRD_PARTY_NOTICES.md`, and
      compares `sqlcipher-src`'s amalgamation with one built from SQLCipher's signed v4.20.0 tag.
    - M6.3 measures the library's size against the budget, and tries smaller builds.
    - M7.1 checks the iOS build, which also links the compiled OpenSSL under this feature.
- **Alternatives considered:**
  - For the crypto library, the machine's OpenSSL on the host and the compiled one only on
    Android, which builds faster but tests a different `libcrypto` from the phones' and needs
    `libssl-dev` on every contributor's machine.
  - For the fixture, leaving a file made by v1 to M2.3, which would leave M2.1's acceptance
    unchecked; or a generator project under `tool/`, which adds a Gradle build to the repository.
  - For threading, letting the caller wrap `Store` in a mutex, which every caller would have to
    remember; or one worker thread with a bounded queue, as v1's `DatabaseHolder` had, which breaks
    the Rust rule that no crate starts a thread.
- **Revisit when:** a `rusqlite` upgrade moves to SQLCipher 5 or drops the vendored feature; the
  v1 fixture stops opening; M6.3 can't meet the download-size budget; or a user-visible screen
  waits on a database call that another call is holding.

## ADR-0018 — The store writes a new schema only into an absent or empty file, and refuses a schema it doesn't know

- **Status:** Accepted
- **Date:** 2026-11-23
- **Context:** M2.2 makes `Store::open` bring each file to schema version 2, as v1's
  `DatabaseHolder.open` did through Room 2.8.5. Room chose what to do from the file's
  `PRAGMA user_version`, and from its bookkeeping table `room_master_table`, which holds an
  identity hash (a fingerprint of the schema that Room's compiler computes) in the row with ID 42.
  This is Room's behavior, read in Room 2.8.5's source (`RoomOpenHelper` and
  `RoomConnectionManager`), not code in v1's repository:
  - At version 0, Room created any missing tables, wrote `room_master_table`, and set the version
    to 2. If the file already held tables, it then compared them with version 2's, column by
    column, and failed if they differed.
  - At version 1, it ran v1's `MIGRATION_1_2`, compared the result with version 2's tables, and
    wrote version 2's hash.
  - At version 2, it compared the stored identity hash with version 2's, and failed on a
    difference. It also accepted a legacy hash, which works around a Room 1.x bug; Room never
    writes one, and v1 used only Room 2.6.1, 2.8.0, and 2.8.5. If `room_master_table` was
    missing, it compared the tables column by column, then wrote the hash.
  - Above version 2, it failed, because v1 has no migration to an older schema.

  M2.1 found that SQLite reads an absent file, a 0-byte file, and a 1-byte file all as an empty
  database at version 0 (`rust/crates/store/tests/open.rs`), so Room's rule would write a new
  schema over a damaged 1-byte file. Room writes `room_master_table` in the same transaction that
  creates or migrates the schema, so every v1 file has one. Version 2's hash,
  `425274f1a5ac9232a36cc07f4f479e7c`, has stayed the same since version 2 appeared in commit
  `bfdea7a` (2026-09-20, Room 2.6.1), through Room 2.8.5. Room also put every file it opened in
  write-ahead-log mode, except on a phone that Android reports as low on memory, where it turned
  write-ahead logging off and left `sqlcipher-android`'s rollback journal. SQLite records the mode
  in the file; the M2.1 fixture is in write-ahead-log mode. A file that `rusqlite` creates starts in SQLite's default rollback-journal mode. On
  2026-11-23 the maintainer was asked which files may get a new schema, what `open` does with a
  schema it doesn't know, and which journal mode a new file gets.
- **Decision** (the maintainer chose the recommended option for all three questions on
  2026-11-23):
  - `Store::open` writes a new schema only into a file that was absent or held 0 bytes before it
    opened, and that then reads as version 0 with no tables. It writes Room's version 2 statements
    and `room_master_table`, as Room did, then switches the file to write-ahead logging. Any
    other file at version 0 returns `StoreError::UnrecognizedSchema` and isn't written.
  - A version above 2 returns `StoreError::NewerSchema`. A negative version, or a version 2 file
    whose `room_master_table` is missing or holds another hash, returns
    `StoreError::UnrecognizedSchema`. Neither writes to the file.
  - A version 1 file migrates with v1's `MIGRATION_1_2` statements, unchanged, in one transaction,
    with foreign keys off as Room had them.
- **Consequences:**
  - Good:
    - A new schema never overwrites a file that holds data, and a schema the store doesn't know
      stays as it was for a later build to read.
    - A file the store creates holds the same schema text, and is in the same journal mode, as one
      v1 creates on most phones, so later code never has to tell them apart.
  - Bad:
    - Two kinds of file open differently from v1. A version 0 file that isn't empty, such as a
      1-byte file, would have been written over by Room. A version 2 file without
      `room_master_table` would have been checked and accepted. No v1 install is known to hold
      either, and a user who has one gets an error instead of a database.
    - Room also compared a migrated version 1 file with version 2's tables. The store doesn't:
      its statements rebuild `accounts`, `budgets`, and `transactions` with version 2's
      definitions, and `categories`, which the migration doesn't touch, is the one Room created.
    - The size check runs just before SQLite opens the file, so a file that another program
      writes in between would be taken for a new one. Only the app writes to its private folder.
    - On a low-memory phone, a new file gets write-ahead logging where v1 kept a rollback
      journal. If switching fails, the open fails and the file stays a valid version 2
      database in rollback-journal mode, which later opens keep.
  - Follow-up: M3.5 maps the two new errors to codes and messages. The first schema version
    after 2 decides what `room_master_table` holds from then on, through the `schema-migration`
    skill (M5.3).
- **Alternatives considered:**
  - Room's rule, which creates the schema in any version 0 file with no tables, a 1-byte file
    included.
  - A separate `Store::create` for setup, with `open` refusing a file that has no schema. v1's
    sign-in made a new database when the file was missing, and the vault (M3) would have to choose
    which call to make.
  - Room's handling of a version 2 file without `room_master_table`: compare its tables column by
    column and write the hash. That is more code, for a file that no v1 install produces.
  - Leaving a new file in the rollback-journal mode, and deciding in M2.4 with the first writes.
- **Revisit when:** a user reports that the new app refuses a database that v1 opened, or the
  first schema version after 2 is planned.

## ADR-0019 — Dart passes each month's bounds to Rust, which reads no time zone

- **Status:** Accepted. ADR-0021 adds the first moment of each of a report's days.
- **Date:** 2026-11-24
- **Context:** v1's budget check (`ValidateTransactionUseCase` at `6d57526`) adds up a category's
  expenses in the calendar month of the transaction's date, in the phone's time zone. It reads
  the zone from its injected clock on every call, so a zone change applies at once (BUG-09). It
  takes the month from the first moment of its first day to one millisecond before the first
  moment of the next month's first day, with kotlinx-datetime's `atStartOfDayIn`. The first
  moment isn't always midnight: in `America/Asuncion`, clocks went from midnight to 01:00 on
  2023-10-01. Rust reads no clock and no time zone (`docs/01-architecture.md`, rule 5), so M2.4
  had to decide how the month reaches the store. Monthly reports and the dashboard (M2.7, M4.1)
  need the same boundaries. Two facts were checked on 2026-11-24 on the maintainer's Linux
  machine:
  - Dart's `DateTime(2023, 10, 1)`, run with `TZ=America/Asuncion` under the Dart of Flutter
    3.50.1, is 01:00 local time, 2023-10-01T04:00:00Z. That is the day's first moment, which
    `atStartOfDayIn` also gives. In `Europe/Berlin` and `America/Sao_Paulo` it is midnight.
  - Across the 599 zones of the time-zone database (tzdata 2026e) and every month from 1970 to
    2100, the shortest span from one month's first moment to the next month's is 27.96 days
    (`America/Adak`, February 1975), and the longest is 31.29 days (`Antarctica/Vostok`, January
    1994).

  Dart's core libraries give no zone's IANA name, the `Europe/Berlin` kind of name that a
  time-zone library needs: `DateTime.timeZoneName` gives an abbreviation such as `CET`.
- **Decision** (the maintainer chose the recommended option on 2026-11-24):
  - Dart turns every calendar date into moments, in the phone's current time zone, and passes
    the moments to Rust. Rust gets no time-zone crate.
  - A transaction write takes the start of its date's month and the start of the next month, as
    `EpochMillis`, in a `core::MonthBounds`. Dart computes them with `DateTime(year, month, 1)`
    and `DateTime(year, month + 1, 1)`. Every add and edit passes them, whatever its type, though
    only an expense in a category with a budget uses them.
  - `MonthBounds::new` refuses bounds whose start isn't before the next month's start, or which
    span less than 27 days or more than 32. The store refuses a write whose date lies outside
    its bounds, with `StoreError::DateOutsideMonth`.
  - The budget check adds up the expenses dated from the start, inclusive, to the next month's
    start, exclusive. Those are the rows v1's inclusive range up to one millisecond before the
    next month's start selected.
- **Consequences:**
  - Good:
    - No new dependency, and no time-zone database inside the app to update.
    - Dart reads the zone from the operating system when it converts each date, so a zone change
      applies to the next write, as BUG-09 requires.
    - Rust's tests choose any bounds they like, with no zone set up.
  - Bad:
    - Rust can't tell whether the bounds are the right month in the right zone. Bounds from the
      wrong zone, or of another month that happens to contain the date, give a wrong budget
      check. The span and containment checks catch a day's bounds, a year's, or another month's
      that doesn't contain the date, but nothing finer.
    - Every Dart caller computes the bounds, so a mistake in that code can recur in each one.
      One helper must compute them for every caller.
  - Follow-up:
    - M4.2 (the transaction form) writes that Dart helper and passes its bounds to the store, with
      a test that runs under `TZ=America/Asuncion` and expects 2023-10-01T04:00:00Z for October
      2023.
    - M2.7 and M4.1 take `MonthBounds` for a report's or the dashboard's month.
- **Alternatives considered:**
  - A Rust time-zone crate, such as `jiff`, with the time-zone database. It needs a dependency to
    approve and to list in the notices, a database bundled into the library (larger, and stale
    between releases) or read from the operating system (Android and iOS keep it in different
    formats), and the zone's IANA name, which Dart can't supply without Kotlin and Swift code or
    a plugin.
  - Dart passes the zone's offset from UTC. One offset can't describe a month that crosses a
    daylight-saving change.
- **Revisit when:** a user reports a budget check or a monthly total that disagrees with the
  phone's calendar, or Rust needs calendar rules with no Dart caller, such as a scheduled task.

## ADR-0020 — Measure coverage with the toolchains' own tools, and fail below today's figures

- **Status:** Accepted
- **Date:** 2026-11-24
- **Context:** `.claude/rules/testing.md`, adopted on 2026-11-24, asks for a coverage command that
  fails below a floor, for branch coverage as well as line coverage, and for coverage to leave
  out only code that can't run under test. Until then, nothing measured coverage. Facts checked
  that day on the maintainer's Linux machine, at commit `0d3df07`:
  - Rust 1.100.0 adds counters to the code it compiles with `-C instrument-coverage`, and its
    `llvm-tools` rustup component ships `llvm-profdata` and `llvm-cov`, which read the counts,
    from the same release.
    `llvm-cov` reported no branches: branch instrumentation needs a nightly compiler, so a stable
    one measures lines, functions, and regions. A region is a stretch of code that runs or
    doesn't, such as each side of `&&` or each `match` arm.
  - Flutter 3.50.1's `flutter test --branch-coverage` writes an lcov file (a plain-text coverage
    format) with each line's count and each branch's (`BRDA` records), but no branch totals. It leaves out every library that no
    test loads, as if the library weren't there.
  - With the generated code below left out, Rust (`core`, `store`, and `bridge`) covered 92.90%
    of lines (2,370 of 2,551) and 90.15% of regions (2,857 of 3,169). Dart's code in `lib/`
    covered 91.33% of lines (295 of 323) and 88.89% of branches (120 of 135), counting
    `lib/main.dart`, which only the integration test loads.
  - The first run, from an empty target folder, took 109 s for both languages, 80 s of it
    compiling the instrumented Rust build with SQLCipher and OpenSSL.
- **Decision** (the maintainer chose the recommended option on 2026-11-24):
  - `tool/coverage.sh` measures Rust with `-C instrument-coverage` and `llvm-cov` from
    `llvm-tools`, and Dart with `flutter test --branch-coverage`. It fails below a floor for each
    of Rust lines and regions and Dart lines and branches. No third-party tool is added.
  - The floors start at the figures above, rounded down to whole percent: 92 and 90 for Rust,
    91 and 88 for Dart. They only go up, and the script says when a figure has passed its floor.
  - Coverage leaves out only generated code: `rust/crates/bridge/src/frb_generated.rs`,
    `lib/platform/rust/`, and `lib/l10n/app_localizations*.dart`. It also leaves out each
    crate's `tests/` folder. The `#[cfg(test)]` modules inside `src/` count, because a stable
    compiler can't leave them out.
  - The script writes a test that imports every library in `lib/`, so a library no test loads
    counts as uncovered.
  - In CI, "Rust checks" installs `llvm-tools` and runs `tool/coverage.sh rust`, and "Flutter
    checks" runs its tests through `tool/coverage.sh dart`.
- **Consequences:**
  - Good:
    - No new dependency: each tool moves with the pinned Rust or Flutter release.
    - The same command runs locally and in CI, and a drop in coverage fails the build.
  - Bad:
    - Rust has no branch coverage on a stable compiler. Regions are a coarser stand-in.
    - The test modules inside `src/` count as covered code, so the Rust figures run a little
      high.
    - "Rust checks" builds the workspace a second time, instrumented, in `target/coverage/`.
    - The script reads the text of `llvm-cov`'s report, whose columns a later LLVM could change.
      It fails with a message when the TOTAL row isn't in the columns it expects.
  - Follow-up:
    - Raise each floor in the change that passes it.
    - M6.5's `04-quality.md` describes the check with the other test layers.
- **Alternatives considered:**
  - `cargo-llvm-cov`, which wraps the same instrumentation and has `--fail-under-*` flags. It is
    a third-party tool that CI would have to download and pin, so it would need the dependency
    policy's approval, and it adds no branch coverage on a stable compiler either.
  - A floor of 100% from the start. It fails until every gap has tests, among them the `Display`
    code of the two error enums, `core/src/reference.rs`, the bridge's money API, `lib/main.dart`,
    and `RustMoneyParser`, the Dart class that calls Rust, which only the integration test
    reaches.
- **Revisit when:** a stable Rust release can measure branches, or a Rust or Flutter upgrade
  changes the report the script reads.

## ADR-0021 — The store runs a report's reads, and Dart passes the first moment of each of its days

- **Status:** Accepted
- **Date:** 2026-11-24
- **Context:** v1's `GenerateReportUseCase` (at `6d57526`) read a month's transactions and every
  category and account, and its `ReportCsvFormatter` wrote each transaction's date as
  `YYYY-MM-DD` in the phone's time zone. The CSV also holds a translated title, the month in the
  phone's language, the time it was made with the zone's abbreviation, and, for a transfer, an
  arrow that points the other way in a right-to-left language. v1's search screen parsed the
  typed amounts, and its DAO ran the search. M2.7 ports them, and two rules stood in the way:
  - Rust reads no clock and no time zone (ADR-0019: Dart passes each month's bounds, and Rust
    reads no time zone), so Rust can't tell which day a moment falls on.
  - The layer table in `docs/01-architecture.md` lets `report` call only `core`, while `store`
    owns the queries, and before M2.7 the store had no read.

  A day isn't always 24 hours long. In `Asia/Beirut`, October 24, 2026 lasts 25 hours, because the
  clocks go back from midnight to 23:00; Samoa skipped December 30, 2011, when it moved across the
  date line. Dart's `DateTime(year, month, day)` gives a day's first moment, as ADR-0019 checked
  for `America/Asuncion`.
- **Decision** (the maintainer chose the recommended answer to each of two questions on
  2026-11-24):
  - `denarii-store` runs the reads: a month's transactions, the search, and every category and
    account. `denarii-report` depends on `denarii-core` alone, as the layer table says, and builds
    the report, its totals, its CSV, and the search filters from what it is given. The bridge
    joins the two (M4.3, M4.5).
  - For a report, Dart passes the year, the month, the first moment of each of its days, and the
    first moment of the next month, each in the phone's current time zone: `DateTime(year, month,
    day)` for each day, and `DateTime(year, month + 1, 1)`. `report::MonthDays` refuses them unless
    there is one moment for each day of the month in the Gregorian calendar, the first and the
    next month's make a `core::MonthBounds`, and each day lasts 0 to 48 hours. A transaction's
    date is the last day whose first moment is at or before the transaction's.
  - Dart formats the CSV's title, month, and time made, and passes them with the language's text
    direction. Rust writes everything else in the CSV.
  - A search takes the first moment of its start date and of the day after its end date, and
    searches to one millisecond before the latter, as v1 did.
- **Consequences:**
  - Good:
    - Rust still has no time-zone data, and a zone change applies to the next report, because
      Dart reads the zone each time.
    - Rust writes the whole CSV in one call, so the quoting and the formula guard (CS-20) live in
      one place, and fixture F8, which v1's own code wrote, checks every byte.
    - The report crate's tests need no database, and the store's reads are tested against
      fixture F3.
  - Bad:
    - Rust can't tell whether the day starts belong to the right zone. Starts from another zone,
      or of another month of the same length, give wrong dates, which only Dart's tests can
      catch.
    - Dart computes 29 to 32 moments for each report, and the labels and the text direction
      besides. The CSV matches v1's only if Dart writes the labels as v1 did: Java's `MMMM yyyy`
      for the month, and `yyyy-MM-dd HH:mm z` in US English for the time.
  - Follow-up:
    - M4.5 writes the Dart helper for a month's day starts, beside ADR-0019's month bounds, with a
      test under `TZ=Asia/Beirut` that expects a 25-hour October 24, 2026; formats the labels;
      and calls `Store::transactions_in_month`, `Store::categories`, `Store::accounts`, and
      `MonthlyReport::new` through the bridge.
    - M4.3 passes the search's dates as first moments, and turns the search's errors into v1's
      message.
- **Alternatives considered:**
  - `report` calls `store` and runs both use cases end to end, as v1's did. It changes the layer
    table, and the report's tests would need a database.
  - Dart formats each row's date and passes the texts back to get the CSV. It takes two calls,
    and Rust could check only that there are as many dates as rows.
  - A time-zone crate in Rust, which ADR-0019 turned down.
- **Revisit when:** a user reports a date in a CSV export that disagrees with the phone's
  calendar, or a report needs dates from more than one month, such as a yearly report.
