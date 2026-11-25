---
paths:
  - "**/*.kt"
  - "**/AndroidManifest.xml"
  - "**/src/*/res/**"
  - "**/proguard-rules.pro"
  - "**/consumer-rules.pro"
  - "**/schemas/**"
---

# Android and Jetpack Compose standards

These rules cover Android apps and their Jetpack Compose UI in this repository: screens, ViewModels,
the manifest, resources, persistence, background work, and release builds. They describe how a
senior mobile developer builds for Android. Kotlin itself follows `kotlin.md` in this folder, and
strings and other text follow `writing.md`.

Apply them to code you write or edit. Don't rewrite existing code only to make it conform; fix a
passage when you are already changing it, and mention anything else you notice.

## Project details

A repository fills in this section when it adopts the rule, and the rest of the rule refers to
each value by its bold name. If a value still reads `[FILL IN: …]`, the rule isn't set up here
yet. Work out that value from the repository itself, and tell the user which values are missing
so they can fill them in.

- **Modules:** `:app` in `android/app/`, Flutter's Android host app (the **Modules** in `kotlin.md`)
- **SDK levels:** minSdk 26, targetSdk 36, and compileSdk 37, set in `android/app/build.gradle.kts`,
  the same as v1 (`docs/specs/android-v1-formats.md`, section 1)
- **Architecture:** no Android UI: Flutter draws every screen inside one `FlutterActivity`,
  `MainActivity`, and the screens follow `flutter.md`. Kotlin holds only Keystore keys and the
  biometric prompt, which Rust calls through JNI (ADR-0007, accepted in M3.1; ADR-0022: the
  `keystore` crate calls `VaultKeystore`, and R8 keeps it by name in `proguard-rules.pro`). No
  dependency injection library. This rule's Compose sections apply only if a task adds an Android
  screen, which needs a decision record
- **Navigation:** none in Kotlin: Flutter's `Navigator` (ADR-0011)
- **Design system:** none in Kotlin: Flutter's Material 3 theme (the **Design system** in
  `flutter.md`). `LaunchTheme` and `NormalTheme` in `res/values*/styles.xml` come from Flutter's
  template and show only while Flutter starts
- **Platform boundary:** Android APIs are called only from Kotlin in
  `android/app/src/main/kotlin/com/denariidolor/`: `MainActivity`, `VaultKeystore` (M3.2), and
  from M3.6 the biometric code. Dart reaches them only through the **Platform channels** in
  `flutter.md`
- **Languages:** the app's seven languages live in the ARB files (the **Localization** in
  `flutter.md`), not in Android resources. `res/values/strings.xml` holds only the launcher
  label, `app_name`, which isn't translated
- **Persistence:** none in Kotlin besides the Keystore. Rust opens v1's SQLCipher database
  (ADR-0006), and the vault keeps reading `shared_prefs/vault_profile.xml` and the `ui_prefs`
  file as `docs/specs/android-v1-formats.md` describes them (M3.3, M4.6). No Room and no
  DataStore
- **Background work:** none: no services, WorkManager, alarms, or broadcast receivers of the app's own
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
- **Check commands:** the **Check commands** in `flutter.md`, then `flutter build appbundle --debug`
  from the repository root, which CI runs (M1.3). No Gradle lint or test task runs: the Keystore
  code is tested through `integration_test/keystore_test.dart` on an emulator (ADR-0022)
- **Test layout:** no Android tests of their own (ADR-0022). Flutter's `integration_test/` runs
  on the `Denarii_API_26` and `Denarii_API_36` emulators, and `keystore_test.dart` there checks
  the Keystore code with test-only aliases. Biometric flows need a manual check on a phone with an
  enrolled fingerprint
- **Release:** none yet. M6.1 builds a signed `flutter build appbundle` in CD, as the **Release**
  in `flutter.md` says; until then the release build type signs with the debug keys
  (`android/app/build.gradle.kts`)

## Decisions that are the user's

Stop and ask the user before you:

- add a permission, export a component, or add an intent filter;
- add a library or SDK, especially analytics, ads, or crash reporting;
- change the **SDK levels**;
- change a database schema or another stored format;
- change a **Security controls** item, the backup rules, or the network security config;
- add a `WebView`, a foreground service, or a new activity;
- change the design (theme, colors, type, or spacing) beyond what was asked;
- change anything the store shows, such as the app name, icon, or Data safety answers;
- or weaken a lint check, a test, or another check.

## Making a change

- **Read first.** Read the screen's route and composables, its ViewModel, and the components the
  **Design system** already has, and reuse them. Check the **Modules** to find where new code
  belongs.
- **Check each step on a device.** After a UI change, install the app on an emulator or a device and
  use the changed screen. A preview isn't the app: it skips the activity, the real theme, insets,
  and the data.
- **Fix the cause, not the symptom.** If state is lost on rotation, fix how the state is saved;
  don't lock the orientation or add `android:configChanges` to avoid re-creating the activity. If a
  test is flaky, wait for the real condition; don't add a sleep.
- **Don't paper over problems.** Never add `@SuppressLint`, `tools:ignore`, a lint baseline entry,
  or an `@OptIn` only to make a warning go away. Work out what it is telling you, then fix that.

## App architecture

- Follow the **Architecture**. Split the app into a UI layer (composables and ViewModels), an
  optional domain layer (use cases in pure Kotlin), and a data layer (repositories and their data
  sources). Dependencies point inward: the domain layer imports no `android.*` types.
- Keep one source of truth for each piece of data, and let data flow one way: state goes down to
  the UI, and events come back up as function calls.
- ViewModels:
  - Expose screen state as a `StateFlow` of one immutable `UiState`, or a few flows, and send
    one-time events through a `Channel`, as `kotlin.md` describes.
  - Never hold an `Activity`, a `View`, or a `Context` other than the application's. A ViewModel
    outlives the activity that created it, so holding one leaks it.
  - Read navigation arguments from `SavedStateHandle`.
  - Ignore a repeated action while the previous one runs, and expose that state (`isSaving`) so
    the button shows it.
- Repositories expose a `Flow` for data that changes and `suspend` functions for one-time
  operations, and are main-safe.
- Get dependencies through constructor injection, never from a service locator inside a
  composable.

## Compose

### Screen structure

- Split each destination in two. A route composable gets the ViewModel, collects its state with
  `collectAsStateWithLifecycle()`, and handles navigation and events. A screen composable takes
  the state and event lambdas, and nothing else: no ViewModel, `NavController`, or repository.
  The screen can then be previewed and tested on its own.
- Keep Android calls (`Context`, `Intent`, `Toast`, activity result launchers) inside the
  **Platform boundary**, behind small interfaces that screens receive or `remember`.
- Hoist state to the lowest composable that every reader shares. A reusable component takes a
  value and an `onValueChange` lambda instead of keeping its own copy.
- Give every reusable composable a `modifier: Modifier = Modifier` parameter, the first one with
  a default value, and apply it once, to the root.
- Name composables that emit UI as nouns in `PascalCase` (`AccountRow`). Name composables that
  return a value in `camelCase`, starting with `remember` when they remember it
  (`rememberSearchState`).

### State and side effects

- Use `rememberSaveable` for what the user entered or chose, which must survive rotation and the
  system killing the process. Use `remember` for state that may reset, such as whether a menu is
  open.
- Run side effects only in effect handlers:
  - `LaunchedEffect`, keyed on every value the effect reads, so it restarts when one changes.
  - `DisposableEffect`, with the cleanup in `onDispose`.
  - `rememberUpdatedState` for a callback that a long-running effect calls, so it uses the
    latest one.
  - `rememberCoroutineScope` for coroutines that start from an event, such as a click.
- Do no work in composition: no I/O, sorting, or heavy formatting. Composition can run often and
  in any order, so map data in the ViewModel or in pure functions.
- Read strings with `stringResource` and `pluralStringResource` during composition, and pass the
  value to any effect that needs it. Read the locale and configuration through
  CompositionLocals, so screens update when the user changes them; `Locale.getDefault()` doesn't
  notify Compose.

### Performance

- Give `LazyColumn` and `LazyRow` items a stable `key` (`items(rows, key = { it.id })`), and a
  `contentType` when the list mixes kinds of rows.
- The Compose compiler skips unchanged composables by default (strong skipping, on since Kotlin
  2.0.20). Don't add `@Stable`, `@Immutable`, or `remember` around lambdas speculatively; measure
  recompositions with Layout Inspector first.
- Use `derivedStateOf` only when the result changes less often than its inputs, such as whether
  a list has scrolled past its first item.
- Pass a lambda for a value that changes every frame, as `Modifier.offset { }` and
  `graphicsLayer { }` take, so only layout or drawing runs again instead of composition.
- Never write to state you have already read in the same composition; that causes an endless
  loop of recompositions.
- Measure performance only on a release build with R8, never a debug build, which runs Compose
  far more slowly. Use Macrobenchmark, and ship Baseline Profiles for startup and scrolling.

### Previews

- Give every screen `@Preview`s with sample state, in light and dark themes and at a large font
  scale (`@PreviewLightDark`, `@PreviewFontScale`), and right to left if the app ships a
  right-to-left language.
- Previews never create a ViewModel or read real data. The route and screen split makes that
  possible.

## Layout, theming, and accessibility

- Use the **Design system** components and the `MaterialTheme` color scheme, typography, and
  shapes. Don't hard-code colors, text sizes, or one-off spacing in screens.
- Draw edge to edge, which Android 15 enforces for apps that target API 35 or later. Apply the
  `Scaffold`'s `innerPadding` or `WindowInsets.safeDrawing`, use `imePadding()` for the
  keyboard, and never assume the height of a system bar.
- Build layouts that adapt to the window, not to the device: phones, foldables, tablets,
  multi-window, and desktop windows. Use window size classes, and don't lock the orientation;
  Android 16 ignores orientation and resizability limits on large screens for apps that target
  API 36.
- Size text in `sp`, and make every layout work at 200% font scale, which Android 14 and later
  allow.
- Support right to left: use `start` and `end`, never `left` and `right`, and
  `Icons.AutoMirrored` for icons that point a direction. Turn on pseudolocales in debug builds
  (`isPseudoLocalesEnabled = true`) and check layouts in `ar-XB`, which mirrors them, and
  `en-XA`, which lengthens the text.
- Accessibility:
  - Give every actionable icon a `contentDescription` from strings, and decorative ones `null`.
  - Keep touch targets at 48 dp or more; Material 3 components enforce this by default.
  - Merge a row's children into one element for TalkBack (`semantics(mergeDescendants = true)`,
    which `clickable` does), and describe state with `stateDescription`, `selected`, or
    `toggleableState`, and the element's `role`.
  - Mark section titles with `semantics { heading() }`, so TalkBack users can move between
    them.
  - Announce status messages with `liveRegion = LiveRegionMode.Polite`.
  - Keep contrast of at least 4.5:1 for text and 3:1 for icons and large text, and never use
    color alone to show state.
  - Make every control reachable with a keyboard or switch, in a logical order.

## Strings, languages, and resources

- Put every user-facing string in `strings.xml`, in every language the **Languages** value
  lists. Mark a string that must not be translated with `translatable="false"`.
- Keep format arguments identical in every translation, use positional arguments (`%1$s`) when
  there is more than one, and escape `'` and `"` as `\'` and `\"`.
- Use `<plurals>` for any count, with every quantity the language needs (Arabic needs all six:
  `zero`, `one`, `two`, `few`, `many`, and `other`), and read them with
  `pluralStringResource(id, count, count)`.
- Never build a sentence from separately translated fragments. Use one string with arguments.
- Format numbers, dates, and currency for the user's locale (`NumberFormat`,
  `DateTimeFormatter.ofLocalizedDate`), and never write a currency symbol into a string.
- Turn on per-app language settings with `androidResources { generateLocaleConfig = true }` and a
  `resources.properties` file naming the default locale.
- Treat translations you write as drafts, and list their keys in your report for a native
  speaker to review.
- Use vector drawables for icons, and adaptive icons for the launcher.

## Lifecycle and navigation

- Follow the **Navigation** setup. Pass IDs between destinations, never whole objects; the
  destination loads what it needs. Arguments travel in a `Bundle`, which has a size limit.
- Handle Back with `BackHandler` or `PredictiveBackHandler`. Never override `onBackPressed()` or
  intercept `KEYCODE_BACK`: for apps that target API 36, Android 16 calls neither, because
  predictive back is on.
- Treat deep link arguments as input from outside the app, and validate them.
- Expect the system to kill the app's process while it is in the background. Restore what the
  user was doing from `SavedStateHandle`, `rememberSaveable`, and stored data.
- Prefer one activity with Compose navigation. Add an activity only for a separate entry point.

## Background work

- Follow the **Background work** setup. Use WorkManager for work that must finish even if the
  user leaves or the device restarts, with constraints such as network or charging. Keep
  coroutines in `viewModelScope` for work that belongs to a screen.
- Use a foreground service only for ongoing work the user notices, and declare its
  `foregroundServiceType` with the matching permission, which apps that target API 34 or later
  need.
- Use exact alarms only for a clock or calendar feature; Android 14 denies
  `SCHEDULE_EXACT_ALARM` to most new installs by default.
- Ask for `POST_NOTIFICATIONS` (Android 13 and later) when the user turns on a feature that
  notifies, and give each kind of notification its own channel.
- Never block the main thread with disk or network work. Turn on `StrictMode` in debug builds to
  catch it.

## Data and persistence

- Follow the **Persistence** setup.
- With Room:
  - Keep `exportSchema = true`, and commit the schema files.
  - Give every schema change a `Migration` and a test with `MigrationTestHelper`.
  - Never call `fallbackToDestructiveMigration()` in a release build; it deletes the user's
    data.
  - Never edit a migration that has shipped, or a committed schema file.
  - An enum stored by name is data: renaming a constant needs a migration.
- Use DataStore rather than `SharedPreferences` for new settings, unless the project already
  decided otherwise. Don't migrate existing settings unless asked.
- Keep files in app-private directories (`filesDir`, `cacheDir`). Reach the user's files through
  the Storage Access Framework, and shared media through `MediaStore` or the photo picker, which
  needs no permission.
- Check the size and format of any file the user picks before you parse it.

## Security and privacy

- Never weaken an item in the **Security controls**. Name every control your change touches in
  your report, and say why it still holds.
- Mark every component `android:exported="false"` unless another app must reach it. An exported
  component is listed in the **Exported components**, and is protected by a permission or
  validates every input.
- Use explicit intents inside the app, and `PendingIntent.FLAG_IMMUTABLE` unless another app must
  fill in the intent. Validate every extra and URI an intent brings in.
- Permissions:
  - Add one only after it is in the **Permissions** inventory and the user agrees; each changes
    the privacy policy and the store's data disclosures.
  - Prefer an option that needs no permission, such as the photo picker.
  - Ask at the moment the feature needs it, explain why first, and give the feature a fallback
    for when the user says no.
- Use HTTPS only. Keep the network security config free of cleartext traffic and user-added
  certificate authorities in release builds.
- Decide the backup rules (`allowBackup`, `dataExtractionRules`, and `fullBackupContent`) on
  purpose, and exclude secrets and tokens from every backup.
- Keep keys in the Android Keystore. Never put a secret in `SharedPreferences`, logs, or
  `BuildConfig`: anything inside the APK can be extracted, so an API key there is public.
- Avoid `WebView`. If one is required, load only the app's own content, keep JavaScript off
  unless that content needs it, and never expose an `addJavascriptInterface` object to content
  from outside.
- On screens that show secrets, follow the **Security controls** decisions on `FLAG_SECURE`
  and on hiding overlay windows.
- Never copy a secret to the clipboard. If the user copies one themselves, mark the clip with
  `ClipDescription.EXTRA_IS_SENSITIVE`, so Android 13 and later hide its preview.
- Never log personal data. Keep verbose logs out of release builds.

## Tests

- Follow the **Test layout**. Test ViewModels and other logic in JVM unit tests with fakes, as
  `kotlin.md` describes. Keep Android APIs out of them.
- Test stateless screens with `createComposeRule()`. Find nodes by what users perceive (text,
  content description, role) before a `testTag`; a test that finds a button by its label also
  checks that the label exists.
- A test hosted in `ComponentActivity` doesn't prove a screen works in the app's own activity,
  which may set up the window and its owners differently. Cover each activity with at least one
  test that launches it.
- Never sleep in a test. Wait for the condition with `waitUntil`, or let Compose and Espresso
  wait for idle.
- Run instrumented tests on the minSdk and targetSdk API levels from the **SDK levels**. Limit a
  test to some levels with `@SdkSuppress`.
- Read a screenshot test's changed images before you accept them.
- Run instrumented tests only when a device or emulator is attached; otherwise say that they
  weren't run.

## Build and release

- Set the **SDK levels** only in the version catalog and convention plugins, never in a module's
  manifest. Raise targetSdk before Google Play's yearly deadline (August 31 in recent years), and
  review that API level's behavior changes first.
- Build releases with R8 (`isMinifyEnabled = true`, `isShrinkResources = true`). Keep keep rules
  narrow (never a blanket `-keep class **`), and test the release build: R8 removes code that is
  reached only through reflection.
- Check the merged release manifest whenever a dependency changes. Libraries can add permissions
  and exported components of their own.
- Give native libraries 16 KB page alignment; Google Play requires it for apps that target
  Android 15 or later.
- Increase `versionCode` for every upload; the store rejects one it has already seen.
- Follow the **Release** process. Signing happens only there: never commit a keystore,
  `local.properties`, an APK, or an AAB.

## Before finishing

1. Run every **Check commands** command and read its output. Fix every lint warning instead of
   adding it to a baseline.
2. If you changed the UI, install the app on an emulator or device and use the changed screen:
   1. in light and dark themes, at 200% font scale, and in the `ar-XB` pseudolocale if the app
      ships a right-to-left language;
   2. with TalkBack, if you changed interactive elements;
   3. after rotating and resizing the window;
   4. after sending the app to the background and killing it (`adb shell am kill <package>`),
      to check that it restores what the user was doing.
3. If you changed the manifest or a dependency, read the merged release manifest for new
   permissions and exported components.
4. If you changed keep rules, reflection, or serialization, build and run the release build.
5. If you changed a schema, run its migration test.
6. Run the instrumented tests if a device is attached, and say which API levels they ran on.
7. If you added strings, check that every language has them, and list the drafted translations.
8. Review your diff as a reviewer would. Look for hard-coded strings, colors, and sizes; Android
   calls outside the **Platform boundary**; icons without a `contentDescription`; missing
   translations; new exported components or permissions; and logs that contain personal data. Then
   report what you checked, what you didn't, and what remains.
