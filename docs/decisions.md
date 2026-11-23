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

- **Status:** Proposed
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

- **Status:** Accepted
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
