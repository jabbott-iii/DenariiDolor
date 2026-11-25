---
paths:
  - "**/*.dart"
  - "**/pubspec.yaml"
  - "**/pubspec.lock"
  - "**/analysis_options.yaml"
  - "**/l10n.yaml"
  - "**/*.arb"
  - "**/build.yaml"
  - "**/.fvmrc"
---

# Dart and Flutter standards

These rules cover Dart code and Flutter apps for Android and iOS in this repository: widgets,
state, navigation, localization, platform integration, data, tests, and release builds. They
describe how a senior mobile developer builds cross-platform apps with Flutter, and how to work
with the user as a pair: you write the code, and they review it and make the calls that are
theirs. Native code follows its own rules in this folder: Kotlin in `android/` follows
`kotlin.md` and `android.md`, and Swift in `ios/` follows `swift.md`. Text follows `writing.md`.

Apply them to code you write or edit. Don't rewrite existing code only to make it conform; fix a
passage when you are already changing it, and mention anything else you notice.

## Project details

A repository fills in this section when it adopts the rule, and the rest of the rule refers to
each value by its bold name. If a value still reads `[FILL IN: …]`, the rule isn't set up here
yet. Work out that value from the repository itself, and tell the user which values are missing
so they can fill them in.

- **Packages and layout:** one app package, `denarii_dolor`, at the repository root, with
  `lib/app/` (startup, theme, routes), `lib/features/<feature>/` holding `ui/` and `state/`,
  `lib/platform/` (the Rust bridge and the platform channels, from M1.2), and `lib/l10n/` (from
  M1.4). Domain rules and storage live in Rust under `rust/` (ADR-0004, Proposed: Rust owns
  the rules and the data)
- **Platforms:** Android with minSdk 26, targetSdk 36, and compileSdk 37, the same as v1, set in
  `android/app/build.gradle.kts`; the application ID stays `com.denariidolor`
  (ADR-0002, existing installs upgrade in place). iOS 15 and later, set in the Xcode project, with
  the iOS app following in M7 (ADR-0009, Flutter 3.50.1 and iOS 15)
- **Toolchain:** Flutter 3.50.1 (stable, with Dart 3.14.0), pinned exactly by
  `environment: flutter: 3.50.1` and `sdk: ^3.14.0` in `pubspec.yaml`; no
  version manager (ADR-0009). As of 2026-11-23 the maintainer's Linux machine has Flutter 3.50.1
  in `~/flutter`, not on `PATH`, and an Android SDK with the command-line tools, NDK
  28.2.13676358 (Flutter 3.50's default), and emulators for API 26, 36, and 37
- **Check commands:** `dart format --output=none --set-exit-if-changed .`, `flutter
  analyze`, and `flutter test`, plus the Rust **Check commands** in `rust.md`
- **Lint configuration:** `package:flutter_lints/flutter.yaml` in
  `analysis_options.yaml`, with `strict-casts`, `strict-inference`, and `strict-raw-types` on and
  the `unawaited_futures` lint enabled
- **Dependency policy:** ask first, and record each decision in `docs/02-dependency-policy.md`. No
  network, analytics, crash-reporting, or ads package, ever. A plugin must support both Android
  and iOS. No mocking package: tests use hand-written fakes
- **State management:** `ChangeNotifier` view models, with no package (ADR-0010). Each screen keeps
  v1's shape: a route widget creates and disposes its view model, and a stateless screen takes its
  state and callbacks. View models hold immutable state, call the bridge, and expose one write at a
  time (`isSaving`). Services reach them through constructors, from two `InheritedWidget` scopes:
  the app scope, created at startup, and the session scope, created after sign-in and disposed
  when the vault locks
- **Navigation:** Flutter's `Navigator`, with no package (ADR-0011). The root widget shows the
  sign-in flow or the signed-in tree, whose own `Navigator` lives inside the session scope, so no
  signed-in screen exists while the vault is locked (CS-02). Screens are pushed as widget classes
  that take IDs as constructor arguments. v1's destinations are listed in
  `app/src/main/java/com/denariidolor/presentation/ui/AppScreens.kt` at 6d57526
- **Design system:** Material 3 with v1's colors (`Composables.kt`, `ThemedContent.kt`, and
  `VizColors.kt` at 6d57526): `lightTheme`, `darkTheme`, and `systemBarsStyle` in
  `lib/app/theme.dart`, with the chart colors in the `VizColors` theme extension (M1.5).
  `DenariiDolorApp.themeMode` picks light, dark, or the device's choice, and M4.6 passes it the
  user's setting. How far iOS adapts its controls is decided in M7. Import Material from
  `package:material_ui/material_ui.dart`, because Flutter 3.50 deprecated
  `package:flutter/material.dart`
- **Localization:** seven languages: English (the default), Simplified Chinese (`zh-CN`), Hindi
  (`hi`), Spanish (`es`), Arabic (`ar`, right to left), French (`fr`), and Portuguese (`pt`). ARB
  files are in `lib/l10n/`, ported from v1's `app/src/main/res/values*/strings.xml` in M1.4, with
  lowerCamelCase keys; the Simplified Chinese pack is `app_zh.arb`. `resolveAppLocale` in
  `lib/l10n/locale_resolution.dart` picks the pack (ADR-0015: the phone's whole language list,
  matched by script as Android does). The app takes Material's delegates from `material_ui`
  (`GlobalMaterialLocalizations.delegates`), not from `AppLocalizations.localizationsDelegates`. The
  packs were machine-drafted, so a native speaker reviews every string you translate
- **Platform channels:** the Rust core through `flutter_rust_bridge` (ADR-0005), whose bindings
  live in `lib/platform/rust/` and which view models reach through hand-written interfaces in
  `lib/platform/`, such as `MoneyParser`; and
  the Kotlin and Swift code that holds hardware-backed keys, which Rust calls directly, through
  the Java Native Interface (JNI) on Android, so no key passes through Dart (ADR-0007,
  accepted in M3.1: hardware-key operations stay in platform code). How Dart starts the
  biometric prompt is settled in M3.6
- **Data layer:** no network client, because the app is offline. Every record, the vault profile,
  and every backup goes through the Rust core (ADR-0006, Proposed: SQLCipher opened from Rust).
  v1's formats are in `docs/specs/android-v1-formats.md`, and the new app must keep reading them
- **Security controls:** `.claude/rules/security.md` (the controls, with their `CS-NN` IDs) and
  `docs/specs/android-v1-formats.md` (keys and formats on phones today). The history of every
  finding is in `git show aba0b55:intel/cybersec.md`
- **Code generation:** `flutter_rust_bridge_codegen generate` for the bridge, configured by
  `flutter_rust_bridge.yaml` (from M1.2), `flutter gen-l10n` for strings (from M1.4), and
  `tool/CurrencySymbols.java`, run with Java 25, for `lib/l10n/currency_symbols.dart` (from M1.6,
  ADR-0016). Their output is committed and never edited by hand; CI regenerates it and fails on any difference
  (M1.3), and the analyzer and the formatter skip only the generated paths (ADR-0012)
- **Test layout:** unit and widget tests in `test/`, with hand-written fakes; whole
  flows in `integration_test/` (from M1.2) on an emulator or device (API 26 and API 36, as v1's CI ran). Rust
  tests live in `rust/` (see `rust.md`)
- **Release:** none yet. M6 builds a signed `flutter build appbundle` in CD with the existing Play
  upload key, keeps v1's split of the pipeline into `ci-gate`, `build`, and `publish` jobs
  (CS-19), and sets a build number above every v1 upload

## Working as a pair

- **Read before you write.** Read the widget you're changing, the state that drives it, the
  shared widgets the **Design system** already has, and the tests that cover them. Check the
  **Packages and layout** to find where new code belongs, and match the patterns already there
  unless they are the problem.
- **Say the plan first.** For a change bigger than a few lines, say in two or three sentences
  what will change, where, and how you will check it on each platform. Then go ahead, unless the
  plan includes one of the decisions in the next point.
- **Ask before decisions that are the user's.** Stop and ask before you:
  - add a package or plugin, especially one with native code;
  - change the **State management** approach, or mix in a second one;
  - change the **Platforms**, a minimum OS version, or the **Toolchain**;
  - add a permission, a platform channel, or native code;
  - change stored data or a database schema;
  - change the design beyond what was asked, or anything the app stores show;
  - weaken a lint, a test, or a check;
  - or build something different from what was asked.

  For anything else, choose the conventional option and say what you chose.
- **Push back with reasons.** If a requested approach blocks the UI thread, leaks resources,
  loses user data, or breaks accessibility, say so before you implement it, with the reason and
  a concrete alternative. Once the user decides, follow their decision.
- **Work in small steps, and look at each one.** Run the app after each UI step. Hot reload keeps
  the app's state and doesn't rerun `main` or `initState`, so use a hot restart or a fresh run
  to check a change to startup or initialization.
- **Fix the cause, not the symptom.** For a "RenderFlex overflowed" error, fix the constraints
  (with `Expanded`, `Flexible`, or a scrolling parent where content can scroll); don't hide it
  with a fixed size. For "setState() called after dispose()", cancel the work in `dispose`;
  don't wrap the call in `try`. For a flaky test, wait for the real condition.
- **Don't silence the analyzer.** Never add `!`, `late`, `dynamic`, an `as` cast, or an
  `// ignore:` comment only to make an error or a warning go away. Work out what it is telling
  you, then fix the types or the design.
- **Explain what isn't obvious.** In your reply, say why a widget is stateless or stateful, why
  it is `const`, why it needs a `Key`, or why work moved to another isolate. Write for the
  audience in `writing.md`, and cover only what this change needs.
- **Keep the change focused.** No unrelated refactors, renames, or reformatting. When the
  formatter or `dart fix` changes files you didn't otherwise touch, revert those changes.
  Mention problems you notice outside the task instead of fixing them.
- **Review your own diff** before you report, as a reviewer would. Look for `!`, `dynamic`,
  missing `const`, a `BuildContext` used after an `await` without a `mounted` check, controllers
  that are never disposed, hard-coded strings, colors, and sizes, missing semantic labels, and
  `print` calls.
- **Report honestly.** Say what changed and why, each command you ran with its real result, which
  devices, OS versions, and settings (text size, dark theme, right to left, TalkBack, VoiceOver)
  you checked, what you couldn't run and why (iOS builds need a Mac), your assumptions, and the
  risks that remain. Never say a check passed unless you ran it in this session and saw it pass.

## Dart

### Types and null safety

- Don't use the null assertion `!`. Use `?.`, `??`, an early return, or a local variable the
  analyzer can promote. Promotion works only for local variables, parameters, and private final
  fields, so copy any other field into a local first.
- Use `late` only when the value is certainly set before its first read. Reading it earlier
  throws an `Error` at run time, whose message starts with `LateInitializationError`.
- Avoid `dynamic`, and turn on the analyzer's strict modes (`strict-casts`, `strict-inference`,
  and `strict-raw-types`) in the **Lint configuration** where they aren't on yet.
- Make state immutable: `final` fields, `const` constructors, and a `copyWith` (or generated
  equivalents). Override `==` and `hashCode` together, or use records or generated classes.
- Model closed sets with a `sealed` class or an `enum`, and match them with a `switch`
  expression that has no `_` or `default` case. Then adding a case makes the analyzer list every
  place that must handle it.
- Use records for small groups of values a function returns, and a class for anything with a
  name of its own.
- Use the class modifiers (`final`, `base`, `interface`, `sealed`) in shared packages to say how
  a class may be extended or implemented.
- Wrap IDs and units in extension types (`extension type UserId(String value) {}`) to stop
  mix-ups at compile time. They are erased at run time (`is` and `as` see the `String`), so
  validate input with a real class.
- Follow Effective Dart's names: `UpperCamelCase` for types, `lowerCamelCase` for everything
  else, constants included, and `lowercase_with_underscores` for files, packages, and import
  prefixes.

### Errors

- Throw an `Exception` subtype for failures a caller should handle, or return a sealed result
  type. `Error` subtypes mean a bug in the program; don't catch them.
- Catch with an `on` clause for the types you handle, never a bare `catch`, and use `rethrow`,
  which keeps the original stack trace.
- Use `int.tryParse` and similar `try…` parsers for input, rather than catching a
  `FormatException`.

### Async code and isolates

- `await` every `Future`, or mark it with `unawaited()` when not waiting is deliberate. Turn on
  the `unawaited_futures` lint.
- Cancel every `StreamSubscription` and timer when its owner goes away, usually in `dispose`.
- Each isolate runs one thread, so heavy computation (parsing a large JSON document, processing
  an image) freezes the UI. Move it to another isolate with `Isolate.run`, or Flutter's
  `compute`.

### Numbers, money, time, and text

- On Android and iOS, `int` is 64-bit two's complement, and arithmetic that goes past its range
  wraps silently. On the web, integers are JavaScript numbers, exact only up to 2^53. Check
  bounds where input decides the values.
- `toInt()` throws on `NaN` and infinity, so check `isFinite` first.
- Never use `double` for money. Use an integer count of the smallest unit (cents), or a decimal
  type.
- Get the current time through an injectable clock (for example `package:clock`), so tests can
  fix it. Store `DateTime` values in UTC and ISO 8601, and format them for the user's locale
  only at the edge.
- `String.length` counts UTF-16 code units, not the characters users see. Use the `characters`
  package to count, cut, or reverse user-visible text.

## Flutter

### Widgets and `build`

- Split UI into small widget classes, not helper methods that return widgets. A class gets its
  own element, so Flutter can rebuild it on its own and skip it when it is `const`.
- Mark every widget constructor and instance `const` where possible; Flutter skips rebuilding a
  `const` widget.
- Keep `build` cheap and free of side effects, because Flutter calls it often: no I/O, no heavy
  computation, and no new controllers, futures, or streams. A `FutureBuilder` whose future is
  created in `build` starts the work again on every rebuild, so create the future in `initState`
  or in the state layer.
- In a `StatefulWidget`, create controllers, focus nodes, and subscriptions in `initState`,
  update them in `didUpdateWidget` when the widget's inputs change, and dispose of every one in
  `dispose`.
- After an `await`, check `mounted` (inside a `State`) or `context.mounted` before you use a
  `BuildContext` again; the widget may have left the screen. The
  `use_build_context_synchronously` lint finds the cases.
- Give list items a `ValueKey` from their data when the list can reorder, so each item keeps its
  state. Use a `GlobalKey` only when nothing else works.

### Layout and performance

- Remember Flutter's layout rule: constraints go down, sizes go up, and the parent sets the
  position. Size with `Expanded`, `Flexible`, and `LayoutBuilder`, not fixed sizes around text.
- Build long lists lazily with `ListView.builder` or slivers, never `ListView(children: …)` over
  a large list. Avoid a scrollable with `shrinkWrap: true` inside another scrollable; combine
  them in one `CustomScrollView` instead.
- Read only the `MediaQuery` property you need (`MediaQuery.sizeOf(context)`,
  `MediaQuery.paddingOf(context)`), so the widget rebuilds only when that property changes.
- Decode images at the size they are shown (`cacheWidth`, `cacheHeight`), and prefer
  `FadeTransition` or a color's alpha to the `Opacity` widget for fades.
- Measure performance in profile mode (`flutter run --profile`) on a real device, never in debug
  mode, which is built for fast development rather than speed. Use DevTools to find the frames
  that miss their budget, and check app size with `--analyze-size`.

### State management

- Follow the **State management** approach, and don't mix in a second one. Keep business logic
  out of widgets, in classes that tests can use without a widget tree.
- Let data flow one way: state goes down to widgets, and events come back up as callbacks.
- Dispose of every notifier, controller, and stream the state layer creates.

### Navigation

- Follow the **Navigation** setup. Pass IDs between screens, not objects; the destination loads
  what it needs. Validate deep link parameters like any input from outside the app.
- Handle Back with `PopScope` (`canPop` and `onPopInvokedWithResult`). `WillPopScope` is
  deprecated, and Android's predictive back doesn't work with it.
- Check the iOS edge-swipe back gesture on every screen you push.

### Platform look and layout

- Follow the **Design system**. Material 3 is Flutter's default; don't set
  `useMaterial3: false`, which is a temporary opt-out. Use the theme's `ColorScheme` and
  `TextTheme`, and no hard-coded colors, font sizes, or one-off spacing.
- Adapt to each platform where users notice it: the `.adaptive` constructors (`Switch.adaptive`,
  `AlertDialog.adaptive`), or a Cupertino widget on iOS, as the **Design system** decides.
- Lay out by the window's width, not by device type, so the app works on phones, tablets,
  foldables, split screen, and in both orientations. Don't lock the orientation.
- Keep content clear of system bars, the notch, and the keyboard with `SafeArea` and
  `MediaQuery.viewInsetsOf`.
- Give the app a `darkTheme`, and respect the user's text size. Read it with
  `MediaQuery.textScalerOf` (`textScaleFactor` is deprecated), never cap it to protect a
  layout, and make every screen work at 200%.

### Accessibility

- Give every icon-only control a label: a `tooltip` on an `IconButton`, a `semanticLabel` on an
  `Icon` or `Image`, or a `Semantics` widget. Exclude decorative images with
  `ExcludeSemantics`.
- Group a row's parts into one element with `MergeSemantics`, mark section titles with
  `Semantics(header: true)`, and announce changes that happen without a new screen with a live
  region (`Semantics(liveRegion: true)`).
- Keep tap targets at least 48 × 48 logical pixels, contrast at least 4.5:1 for text, and never
  use color alone to show state. Respect reduced motion (`MediaQuery.disableAnimationsOf`).
- Check changed screens with TalkBack on Android and VoiceOver on iOS.

### Localization

- Follow the **Localization** setup with `flutter_localizations` and generated localizations
  (`l10n.yaml` and ARB files). Set `nullable-getter: false`, so `AppLocalizations.of(context)`
  needs no `!`.
- Put every user-facing string in the ARB files, with a description for translators. Use ICU
  plural and select messages (`{count, plural, one{…} other{…}}`) for counts, and never build a
  sentence from separately translated fragments.
- Format numbers, dates, and currency for the user's locale with `package:intl`.
- Support right to left with the directional classes (`EdgeInsetsDirectional`,
  `AlignmentDirectional`), and `matchTextDirection: true` on icons that point a direction.
- On iOS, also add the supported languages in the Xcode project, as Flutter's
  internationalization guide describes.
- Treat translations you write as drafts, and list their keys in your report for a native
  speaker to review.

## Platform integration

- Follow the **Platform channels** setup. Prefer a well-maintained plugin to writing native code.
  When the app needs its own native code, generate type-safe channels with Pigeon instead of
  hand-written `MethodChannel` strings.
- Write the native side under the rules for its language: `android.md` and `kotlin.md` for
  `android/`, and `swift.md` for `ios/`.
- Handle `PlatformException` and `MissingPluginException` from every channel call. A plugin may
  not implement a method on every platform.
- Use platform views (`AndroidView`, `UiKitView`) only where a native view is required, such as
  a map; each one costs performance.
- Permissions:
  - Declare each one natively: in the Android manifest, and with a usage description in the iOS
    `Info.plist`.
  - Ask at the moment the feature needs it, explain why first, and give the feature a fallback
    for when the user says no.
- Follow the store and platform rules that `android.md` and `swift.md` describe: targetSdk
  deadlines, edge to edge, privacy manifests, and the merged Android manifest.

## Data, security, and generated code

- Follow the **Data layer** setup, and know how its client reports HTTP errors. `package:http`
  returns a 404 or 500 as an ordinary response, so check `statusCode`; `dio` throws a
  `DioException` for them by default. Set timeouts, and validate decoded JSON before you use it.
- Give every database schema change a migration and a test, and never delete the database to
  get past a migration error, which deletes the user's data.
- Keep only small, non-sensitive settings in `shared_preferences`.
- Never weaken an item in the **Security controls**, and name every control your change touches
  in your report.
- Keep secrets and tokens in the platform's secure storage (the Keychain and the Android
  Keystore, through a secure storage plugin). Never in the code, assets, or `--dart-define`
  values, which are compiled into the app and can be extracted.
- Obfuscate release builds with `--obfuscate --split-debug-info=<dir>` where the
  **Security controls** call for it, and keep the symbol files private, because crash reports
  need them. Obfuscation doesn't protect secrets or resources.
- Log through a logger, never `print`, and never log personal data. `debugPrint` still prints in
  release builds, so guard debug-only logging with `kDebugMode`.
- Follow the **Code generation** setup. Never hand-edit generated files (`*.g.dart`,
  `*.freezed.dart`); change the source and run
  `dart run build_runner build --delete-conflicting-outputs`.

## Tests

- Follow the **Test layout**. Test logic with unit tests, widgets with widget tests
  (`testWidgets`), and whole flows on a device with `integration_test`.
- Give every behavior change a test, and start every bug fix with a test that fails before the
  fix.
- Find widgets by what users perceive (`find.text`, `find.bySemanticsLabel`) before a `Key`.
- Never wait for real time in a test. Use `tester.pump` with a duration, fake time, or an
  injected clock. `pumpAndSettle` times out on an animation that never ends.
- Check accessibility in widget tests with `meetsGuideline`: `androidTapTargetGuideline`,
  `iOSTapTargetGuideline`, `labeledTapTargetGuideline`, and `textContrastGuideline`.
- Golden tests can differ between operating systems because of fonts and rendering, so generate
  and compare them on one OS, usually in CI. Read a changed golden image before you accept it.
- Prefer fakes to mocks, and use a mocking package only if the **Dependency policy** allows one.

## Style, lints, and the toolchain

- Format with `dart format`, and keep `flutter analyze` free of issues. Silence a lint only on
  one line, with a comment that says why.
- Keep the **Toolchain**. Don't change the Flutter or Dart version, the channel, or the SDK
  constraints as a side effect of another task; an upgrade is a change of its own. Before you
  use a new API, check that the pinned version has it.
- Follow the **Dependency policy**. When you propose a package, say what it does that Flutter or
  existing code can't, and give its license, its publisher, how well it is maintained, its
  platform support, and what it pulls in. Commit `pubspec.lock` for the app.
- Don't hand-edit generated platform files, such as `GeneratedPluginRegistrant` or the Xcode
  project, beyond what a change needs.

## Release

- Follow the **Release** process. Set the version in `pubspec.yaml` (`version: 1.2.3+45`, where
  the number after `+` is the build number), and increase the build number for every upload.
- Build releases with `flutter build appbundle` and `flutter build ipa`, with the flavor and the
  obfuscation the **Release** and **Security controls** call for.
- Never commit signing keys, `key.properties`, certificates, provisioning profiles, or store
  credentials. Signing happens only in the **Release** process.

## Before finishing

1. Run every **Check commands** command and read its output. Fix every issue, not only the
   errors.
2. If you changed a class that a generator reads, run the **Code generation** step, and check
   that the generated files changed as expected.
3. If you changed the UI, run the app with a fresh start on an Android emulator and, where a Mac
   is available, an iOS simulator. Check the changed screen:
   1. in light and dark themes, at 200% text size, and in a right-to-left language if the app
      ships one;
   2. with TalkBack and VoiceOver, if you changed interactive elements;
   3. after rotating and resizing it, and on a tablet size;
   4. after sending the app to the background and back.
4. If you changed native files (the Android manifest, Gradle files, `Info.plist`, or the
   Podfile), list each change in your report, and build both platforms in release mode.
5. Run the integration tests on a device or emulator if one is attached, and say where they
   ran. If you couldn't run the iOS checks, say so.
6. Review your diff as described in "Working as a pair", then report what you checked, what you
   didn't, and what remains.
