---
paths:
  - "android/**"
---

# Android host app and Kotlin standards

These rules cover everything in `android/`: the Kotlin, the Gradle files, the manifests, the
resources, and the keep rules for R8, Android's code shrinker, of Flutter's Android host app. Flutter draws every screen, and
Kotlin holds only what needs the Android runtime, so one rule covers the Kotlin and the Android
app together. KDoc, error messages, and other text follow `writing.md` in this folder, and
`security.md`, which also loads for `android/`, lists the controls that must stay.

This rule merges and trims `kotlin.md` and `android.md` from Doctrina commit `dc6eb2d`. It leaves
out what this app doesn't have: Android screens and Jetpack Compose, ViewModels and repositories,
Android navigation, text in Android resources, Room and DataStore, coroutines and Flow, background
work, Kotlin Multiplatform, and published libraries. Adding any of them is the user's decision
(see "Decisions that are the user's"); bring the matching section back from Doctrina in the same
change.

Apply them to code you write or edit. Don't rewrite existing code only to make it conform; fix a
passage when you are already changing it, and mention anything else you notice.

## Project details

A repository fills in this section when it adopts the rule, and the rest of the rule refers to
each value by its bold name. If a value still reads `[FILL IN: …]`, the rule isn't set up here
yet. Work out that value from the repository itself, and tell the user which values are missing
so they can fill them in.

- **Modules:** one Gradle module, `:app` in `android/app/`, Flutter's Android host app. Its
  Kotlin is in `android/app/src/main/kotlin/com/denariidolor/`: `MainActivity`, `VaultKeystore`
  (M3.2), which Rust calls for the Keystore, and the `BiometricPrompt` code from M3.6. Flutter
  writes `android/app/src/main/java/io/flutter/plugins/GeneratedPluginRegistrant.java`, which is
  never edited by hand
- **Architecture:** no Android UI: Flutter draws every screen inside one `FlutterActivity`,
  `MainActivity`, and the screens follow `flutter.md`, with Flutter's `Navigator` (ADR-0011) and
  Material 3 theme. `LaunchTheme` and `NormalTheme` in `res/values*/styles.xml` come from
  Flutter's template and show only while Flutter starts. Kotlin holds only what needs the Android
  runtime: the Android Keystore and `BiometricPrompt`, which Rust calls through the Java Native
  Interface (JNI) (ADR-0007, accepted in M3.1: hardware-key operations stay in platform code, and
  Rust calls them directly through JNI). Logic that doesn't need them belongs in Rust
  (`CLAUDE.md`, "Architecture", rule 6; ADR-0004, Proposed: Rust owns the rules and the data). No
  dependency injection library
- **Platform boundary:** Android APIs are called only from Kotlin in
  `android/app/src/main/kotlin/com/denariidolor/`. Rust reaches `VaultKeystore` through JNI from
  the `denarii-keystore` crate (ADR-0022), and Dart reaches the rest only through the **Platform
  channels** in `flutter.md`
- **Public API:** none outside the app, but Rust looks up the `@JvmStatic` methods of
  `VaultKeystore` and the `code` field of `KeystoreException` by name and signature in
  `rust/crates/keystore/src/jni.rs`, and `android/app/proguard-rules.pro` keeps them from R8.
  Renaming one, or changing its signature, still compiles and then fails at run time, so change
  the Kotlin, the Rust, and the keep rules together
- **SDK levels:** minSdk 26, targetSdk 36, and compileSdk 37, set in
  `android/app/build.gradle.kts`, the same as v1 (`docs/specs/android-v1-formats.md`, section 1)
- **Toolchain:** Kotlin 2.4.20 (the Kotlin Android Gradle plugin) and the Android Gradle plugin
  9.3.1, pinned in `android/settings.gradle.kts`; Gradle 9.5.0, pinned by the wrapper in
  `android/gradle/wrapper/gradle-wrapper.properties`; and Java 17 for `sourceCompatibility`,
  `targetCompatibility`, and `jvmTarget` in `android/app/build.gradle.kts`. The Flutter Gradle
  plugin comes from the pinned Flutter SDK (the **Toolchain** in `flutter.md`). Change any of
  them with the `dependency-upgrade` skill
- **Check commands:** the **Check commands** in `flutter.md`, then `flutter build appbundle --debug`
  from the repository root, which CI runs (M1.3) and which compiles the Kotlin; read its Kotlin
  warnings. When a change touches `android/`, the `ci-gates` skill also builds the release APK,
  where R8 runs, and lists the permissions it asks for. Kotlin has no check of its own: no ktlint,
  detekt, Android lint, or Gradle test task runs, and adding one is the maintainer's decision,
  under `docs/02-dependency-policy.md`
- **Style and lint configuration:** the `[*.{kt,kts}]` section of `.editorconfig`, kept from v1:
  ktlint's `android_studio` code style, 4-space indents, lines of at most 140 characters, and no
  wildcard imports. Nothing runs ktlint since v1's Gradle build left the working tree
  (ADR-0003), so format by hand to that style
- **Dependency policy:** ask first, and record each decision in `docs/02-dependency-policy.md`. The
  app's Gradle build has no library of its own yet; the Flutter Gradle plugin adds the
  embedding's. No mocking library
- **Errors:** for the calls that come from Rust, catch the Keystore's and storage's exceptions
  where they are thrown, as v1's `keystoreCall` did (`KeystoreSecrets.kt` at 6d57526), and throw
  a `KeystoreException` carrying a `KeystoreFailure` code, which Rust reads through JNI and maps
  to `KeystoreError` (M3.2, ADR-0022, `docs/specs/keystore.md`). Anything else Kotlin throws
  reaches Rust as `Unexpected`. Show no exception text to users
- **Concurrency:** none: no coroutines, and no thread of Kotlin's own. Each call runs on its
  caller's thread, never Android's main thread, because Keystore operations block. Code that
  creates a Keystore key is `@Synchronized`, as v1's `SecurityProfileService` was, so two
  callers can't both create it
- **Time:** Kotlin reads no wall clock. The lockout and the session timeout run on
  `SystemClock.elapsedRealtime()`, the lockout also on `Settings.Global.BOOT_COUNT` (CS-01,
  CS-13). The vault takes both from its caller (M3.3, `docs/specs/vault.md`); M3.5 and M3.7
  decide which layer reads them
- **Logging:** none. Shipped code has no `Log`, no `println`, and no logging library (`CLAUDE.md`,
  "Security posture")
- **Languages:** Kotlin shows no text except the `BiometricPrompt` dialog (M3.6). Users see the
  app's text in seven languages from the ARB files in `lib/l10n/` (the **Localization** in
  `flutter.md`), not from Android resources, and Kotlin returns codes, never sentences.
  `res/values/strings.xml` holds only the launcher label, `app_name`, which isn't translated. A
  native speaker reviews every string you translate
- **Persistence:** none in Kotlin besides the Keystore. Rust opens v1's SQLCipher database
  (ADR-0006), and the vault keeps reading `shared_prefs/vault_profile.xml` and the `ui_prefs`
  file as `docs/specs/android-v1-formats.md` describes them (M3.3, which reads the profile through
  `VaultKeystore`, and M4.6). No Room and no
  DataStore
- **Background work:** none: no services, WorkManager, alarms, or broadcast receivers of the
  app's own
- **Untrusted input:** the profile blob in `shared_prefs/vault_profile.xml` and the two v1.0.x
  files (M3.4), which may be damaged, and every value that Rust or Dart passes in. Their formats
  and limits are in sections 2–4 of `docs/specs/android-v1-formats.md`
- **Permissions:** the release manifest requests one, `HIDE_OVERLAY_WINDOWS`, for the overlay guard
  (CS-11), as v1's did, besides AndroidX's own `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`; never
  `INTERNET` (ADR-0008). The debug and profile manifests keep Flutter's `INTERNET` (ADR-0013). Each
  new permission is the maintainer's decision, and changes `PRIVACY.md` and the Play Console Data
  safety form (`security.md`)
- **Exported components:** one of the app's own: the launcher `activity-alias`
  `.presentation.ui.auth.LoginActivity`, v1's launcher name, kept so home-screen icons survive the
  update; `MainActivity` isn't exported. AndroidX merges in two more, which v1's release had too
  and which the maintainer chose to keep in M3.8: `androidx.startup.InitializationProvider`, which
  isn't exported, and `androidx.profileinstaller.ProfileInstallReceiver`, which is exported but
  takes broadcasts only from callers that hold `android.permission.DUMP` (the shell and the system)
- **Security controls:** `.claude/rules/security.md`, with the `CS-NN` controls and the accepted
  risks (no `FLAG_SECURE`, CS-A4), and `docs/specs/android-v1-formats.md` for the keys and
  formats on phones today
- **Design docs:** `docs/01-architecture.md`, ADR-0007 and ADR-0022 in `docs/decisions.md`, and
  `docs/specs/keystore.md`; what v1 left on phones is in `docs/specs/android-v1-formats.md`
- **Test layout:** no Kotlin or Android tests of their own, and no test library for them: the
  maintainer chose on 2026-11-25 to test the Keystore code through Dart instead (ADR-0022).
  `integration_test/keystore_test.dart` runs Dart → bridge → Rust → JNI → Kotlin on the
  `Denarii_API_26` and `Denarii_API_36` emulators, with test-only aliases that start with
  `denarii_test_` (the **Test layout** in `flutter.md`). A test never touches the app's real
  `denarii_vault_` aliases, profile, or database (`security.md`). Biometric flows need a manual
  check on a phone with an enrolled fingerprint
- **Release:** none yet. M6.1 builds a signed `flutter build appbundle` in CD, as the **Release**
  in `flutter.md` says; until then the release build type signs with the debug keys
  (`android/app/build.gradle.kts`)

## Decisions that are the user's

Stop and ask the user before you:

- add a library, an SDK, or a Gradle plugin, especially analytics, ads, or crash reporting;
- add a permission, export a component, or add an intent filter;
- change the Kotlin, JDK, Gradle, or Android Gradle plugin version, or the **SDK levels**;
- change the **Public API**, a stored or serialized format, or a **Security controls** item, the
  backup rules among them;
- add an Android screen, which also needs a decision record, a `WebView`, a service, a new
  activity, or anything else this rule leaves out;
- opt in to an experimental API (`@OptIn`) or add a `-X` compiler flag;
- change anything the store shows, such as the app name, icon, or Data safety answers;
- or weaken a lint, a test, or another check.

## Making a change

- **Read first.** Read the Kotlin around the change, the Rust in `rust/crates/keystore/` that
  calls it, and `integration_test/keystore_test.dart`, which tests it. Check the **Modules** to
  find where new code belongs.
- **Check each step.** Build with the **Check commands** after each step, and read the Kotlin
  warnings. Run the integration tests on an emulator after a change that Rust or Dart can reach.
- **Fix the cause, not the symptom.** For a bug, first write a test that fails, then fix the bug
  and show the test passing. Don't add a `?.` or `?: return` that quietly skips a null that
  should never be there; find out why it is null.
- **Don't silence the compiler.** Never add `!!`, an unchecked cast, a `@Suppress`,
  `@SuppressLint`, `tools:ignore`, a `lateinit`, or `runBlocking` only to make an error or a
  warning go away. Work out what it is telling you, then fix the types or the design.

## Kotlin

### Types and null safety

- Model closed sets with an `enum class` or a `sealed interface`, as `KeystoreFailure` does, and
  match them with a `when` that has no `else` branch. Then adding a case makes the compiler list
  every place that must handle it.
- Prefer `val` to `var`, and read-only `List`, `Set`, and `Map` to their mutable versions in
  signatures.
- Classes are final by default; keep them that way. Give declarations explicit return types and
  the smallest visibility that works: `private`, then `internal`, then public.
- Don't use `!!`. Use `?:`, an early return, or `checkNotNull(x) { "why" }` and
  `requireNotNull(x) { "why" }`, which fail with a message that says what went wrong.
- Use `lateinit` only for fields a framework injects.
- Android's APIs are Java, and Java code returns platform types, whose nullability Kotlin doesn't
  know. Give them an explicit Kotlin type where they enter, so a null can't travel far before it
  fails.
- Use `as?` instead of `as` when a value might have another type, and never make an unchecked
  cast on data from outside the program.

### Errors

- Follow the **Errors** pattern. Across JNI, a failure the caller should handle is a
  `KeystoreException`. Use `require` for bad arguments, `check` for bad state, and `error("…")`
  for code that should be unreachable.
- Kotlin has no checked exceptions, so document every exception a function can throw with KDoc's
  `@throws`.
- Catch `Exception` or `Throwable` only at a boundary that reports the failure. Never swallow an
  exception silently.
- Close every `Closeable` with `use { }`, which also closes it when an exception is thrown.
- Say in each error message what failed, with the identifying context (the alias, the file), and
  never a secret.

### Time and numbers

- Get the time from the source the **Time** value names, never from
  `System.currentTimeMillis()` or `Instant.now()`.
- Pass durations as `kotlin.time.Duration`, not as a `Long` whose unit only a comment states.
- Integer arithmetic overflows silently: `Int.MAX_VALUE + 1` is `Int.MIN_VALUE`, with no
  exception. Where input decides the values, use `Math.addExact` and `Math.multiplyExact`, which
  throw on overflow, or check the bounds first.
- Conversions lose data silently: `Long.toInt()` keeps only the low 32 bits.

### Style

- Format to the **Style and lint configuration** by hand, because nothing runs ktlint.
- Follow the Kotlin coding conventions for names: `PascalCase` for types, `camelCase` for
  functions and properties, and `UPPER_SNAKE_CASE` for `const val` and other deeply immutable
  top-level values.
- Add no new compiler warnings, and use only language features and APIs that are stable in the
  pinned Kotlin version.
- Don't leave `println`, `Log`, debugging code, or `TODO()` behind. `TODO()` compiles, then throws
  `NotImplementedError` when it runs.

## Security and privacy

- Never weaken an item in the **Security controls**. Name every control your change touches in
  your report, and say why it still holds.
- Mark every component `android:exported="false"` unless another app must reach it. An exported
  component is listed in the **Exported components**, and is protected by a permission or
  validates every input.
- Use explicit intents inside the app, and `PendingIntent.FLAG_IMMUTABLE` unless another app must
  fill in the intent. Validate every extra and URI an intent brings in.
- Add a permission only after it is in the **Permissions** inventory and the user agrees; each
  changes the privacy policy and the store's data disclosures. Prefer an option that needs no
  permission.
- Decide the backup rules (`allowBackup`, `dataExtractionRules`, and `fullBackupContent`) on
  purpose, and exclude secrets and tokens from every backup.
- Keep keys in the Android Keystore. Never put a secret in `SharedPreferences`, logs, or
  `BuildConfig`: anything inside the APK can be extracted.
- Follow the **Security controls** decisions on `FLAG_SECURE` and on hiding overlay windows.
- Treat every value in the **Untrusted input** as possibly malformed or hostile:
  - Reject malformed input with a typed failure, as the **Errors** pattern says. Catch a
    library's exceptions where its parser is called, and map them to that failure.
  - Check every claimed size against a limit and against the input that remains before you
    allocate or slice.
  - Never use Java serialization (`ObjectInputStream`) on outside data: it can run code while it
    reads.
- Keep secrets in a `ByteArray` or `CharArray`, never a `String`, which can't be cleared. Zero the
  array with `fill(0)` in a `finally` block, and compare secrets with `MessageDigest.isEqual`,
  which takes the same time whether or not they match.

## Tests

- Follow the **Test layout**. Give every behavior change a test, and start every bug fix with a
  test that fails before the fix.
- Run the integration tests on both emulators, API 26 and API 36: the minSdk and targetSdk levels.
- Run them only when a device or emulator is attached; otherwise say that they weren't run.

## Build and release

- Change the **SDK levels** only in `android/app/build.gradle.kts`, never in a manifest. Raise
  targetSdk before Google Play's yearly deadline (August 31 in recent years), and review that API
  level's behavior changes first. For example, Android 15 draws apps that target API 35 or later
  edge to edge, and Android 16 ignores orientation and resizability limits on large screens for
  apps that target API 36; `flutter.md` says how the screens handle both.
- R8 runs on the release build and removes or renames code that is reached only through
  reflection or JNI. Keep the keep rules narrow (never a blanket
  `-keep class **`), and keep only what Rust looks up, as the **Public API** says.
- Check the merged release manifest whenever a dependency changes. Libraries can add permissions
  and exported components of their own.
- Keep the native libraries, the Rust ones included, aligned to 16 KB memory pages; Google Play
  requires it for 64-bit application binary interfaces (ABIs) (`docs/specs/bridge.md`).
- Increase `versionCode` for every upload; the store rejects one it has already seen.
- Use no dynamic versions in Gradle (`+`, `latest.release`), and add no repositories. Don't change
  the **Toolchain** or the Gradle wrapper as part of another task; a bump is a change of its own.
- Follow the **Release** process. Signing happens only there: never commit a keystore,
  `local.properties`, an APK, or an AAB.

## Before finishing

1. Run every **Check commands** command and read its output. Fix every Kotlin warning, not only
   the errors.
2. If you changed the manifest, a resource, or a dependency, read the merged release manifest for
   new permissions and exported components.
3. If you changed the **Public API** or the keep rules, build and run the release build.
4. Run the integration tests if an emulator is attached, and say which API levels they ran on.
5. If you added text that users see, check that it comes from the ARB files in all seven
   languages, and list the drafted translations.
6. Review your diff as a reviewer would. Look for `!!`, `lateinit`, `runBlocking`, an unchecked
   cast, a `@Suppress`, `println` or `Log`, `TODO()`, a clock read, a secret in a `String` or left
   uncleared, Android calls outside the **Platform boundary**, new exported components or
   permissions, and missing tests. Then report what you ran, what you didn't, and what remains.
