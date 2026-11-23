---
paths:
  - "app/src/main/java/com/denariidolor/presentation/**"
  - "app/src/main/java/com/denariidolor/MainActivity.kt"
  - "app/src/androidTest/java/com/denariidolor/presentation/**"
---

# Compose UI and presentation

## Screen structure

- **Route and screen.** Each destination has two composables:
  - `XxxRoute(…, viewModel: XxxViewModel = metroViewModel())` collects state with `collectAsStateWithLifecycle()`, wires
    `UiMessageEffect(viewModel.messages)` and calls the screen.
  - `XxxScreen(state…, onAction lambdas)` is stateless. It never takes a ViewModel, `NavController`, repository or
    `Context`-based service, so it can be tested on its own. See `ManageCategoriesRoute` and `ManageCategoriesScreen`.
- **Platform calls.** Screens reach Android only through `presentation/platform`, so they can move to shared code:
  - `rememberNotices()` for toasts, `rememberShareSheet()`, `rememberExternalApps()` (store listing, feedback email),
    `rememberAppDetails()`, `rememberDatePicker()`, `rememberSaveFilePicker()` / `rememberOpenFilePicker()`, and
    `rememberBiometricPrompter()` for a prompt bound to a `CryptoObject` (CS-07).
  - Their interfaces are plain Kotlin in `PlatformActions.kt`, and `AndroidPlatformActions.kt` implements them.
  - Only that file and the activities may use `LocalContext`, `LocalActivity`, `Toast`, `Intent`, `DatePickerDialog` or
    activity-result launchers. `PlatformBoundaryTest` fails on any other file that does, and `PlatformIntentsTest` pins
    each intent.
- **ViewModels.**
  - Declare them as `@Inject @ViewModelKey @ContributesIntoMap(AppScope::class) class XxxViewModel(…) : ViewModel()`.
  - One that reads navigation arguments is `@AssistedInject` with an `@Assisted savedStateHandle: SavedStateHandle` and a
    nested `ViewModelAssistedFactory` that builds the handle with `extras.createSavedStateHandle()`. See
    `LegalDocumentViewModel`.
  - They depend on use cases, repository interfaces and `Clock`.
  - Keep Android types (`Context`, `Uri`, `Bitmap`) out, so JVM tests can cover them. The `Uri` that `BackupViewModel` and
    `ReportViewModel` take for picked files is the documented exception until shared code gets its own file handle. The
    `Cipher` that `LoginViewModel` and `SettingsViewModel` hand to the biometric prompt is JVM-only for the same reason.
  - Navigation arguments come from `SavedStateHandle`.
- **State and events.**
  - Long-lived state is one immutable `…UiState` data class (or a few `StateFlow`s) per screen.
  - One-shot outcomes (saved, deleted, failed) are a `UiMessage` sent over a `Channel`.
  - Format text from `Result` and `DomainException` only through `UiMessage.fromResult` and `UiMessage.fromError`.
- **In-flight guard.** A write or export action returns early while the previous one is running. It exposes `isSaving` or
  `exporting` so the button is disabled (BUG-02).
- **Navigation.**
  - Add the destination to `MainDestination` in `AppScreens.kt` (string route, title and icon resources) with a matching
    `composable(...)`.
  - Close a screen with `navController.popBackOnce()` (BUG-03), never a bare `popBackStack()`.
  - Handle Back with `BackHandler`. Never override `onBackPressed()` and never intercept `KEYCODE_BACK`: with targetSdk 36,
    predictive back is on and neither is called.

## Composition rules (several of these are lint errors)

- **Strings:** read them with `stringResource`, `pluralStringResource` or `LocalResources.current`, never
  `LocalContext.current.getString(...)`. That lint error (`LocalContextGetResourceValueCall`) also fires inside a
  `LaunchedEffect`, so read the string during composition and use the value in the effect, as `AddTransactionRoute` does.
- **Locale:** read it with `LocalLocale.current.platformLocale`, never `Locale.getDefault()`. The `NonObservableLocale` lint
  check is an error.
- **Stored values:** never show them raw. Use `TransactionType.labelRes()` for types, `transferRoute(...)` for transfer
  arrows (they follow the text direction) and `formatMoney` / `formatSignedAmount` for amounts.
- **State holders:** use `rememberSaveable` for form input, so it survives rotation and process death, and `remember` for
  transient flags such as dialog visibility.
- **Side effects:** put them only in `LaunchedEffect` or `DisposableEffect`, keyed on what they depend on. Wrap callbacks
  that a long-running effect captures in `rememberUpdatedState`.
- **No work in composition:** no I/O, sorting or heavy formatting. Map data in the ViewModel or in pure mappers such as
  `DashboardMappers`.
- **Lists:** use `LazyColumn` with stable keys: `items(rows, key = { it.id })`.
- **Performance:** strong skipping is on by default with the Kotlin 2 Compose compiler. Don't sprinkle `@Stable`,
  `@Immutable` or `remember {}` around lambdas speculatively. Measure recompositions in Layout Inspector before optimizing.
  Use `derivedStateOf` only when the derived value changes less often than its inputs.

## Look, layout and accessibility

- **Theme:**
  - Use Material 3 components and `MaterialTheme.colorScheme` / `typography` tokens.
  - Chart colors come from `VizColors`.
  - No hard-coded colors or text sizes in screens.
  - Activities set their content through `setThemedContent` (`ThemedContent.kt`), which handles dark mode and edge-to-edge.
- **Edge-to-edge** is on. Apply `Scaffold`'s `innerPadding`, or `consumeWindowInsets`, and never assume system bar heights.
  `MainActivity` uses `adjustResize` for the keyboard.
- **Right to left:** Arabic ships, so use `start`/`end` (never `left`/`right`). Use `Icons.AutoMirrored.*` for directional
  icons. Check any new layout with a right-to-left locale.
- **Accessibility:**
  - Every actionable icon has a `contentDescription` from strings; a decorative icon uses `null`.
  - Keep touch targets at 48dp or more (the Material 3 defaults).
  - Text sizes use `sp`, and layouts must survive 200% font scale.
  - Express selection and state with `semantics` (see `ManageCategoriesScreen`).
- **Test tags:** tests find nodes by `testTag`. Declare `const val XXX_TAG = "…"` next to the screen and reuse it from
  androidTest.

## Session and security in the UI

- `MainActivity` checks the session before it composes anything (CS-02), every 30 seconds, on resume and on user
  interaction. Never compose financial content before that check, and never add an entry point that skips it.
- `LoginActivity` calls `window.guardAgainstOverlays()` (CS-11). Any new screen that shows PIN, recovery or wipe controls
  needs the same guard.
- Don't add `FLAG_SECURE`. The maintainer declined it twice (CS-A4).
- A currency change restarts `MainActivity`, because ViewModels hold amounts that are already formatted.
