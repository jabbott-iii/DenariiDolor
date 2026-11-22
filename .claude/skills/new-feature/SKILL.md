---
name: new-feature
description: Add a user-facing feature or screen to Denarii Dolor end to end, following its layers (domain model and use case, repository, Hilt ViewModel, Compose Route and Screen, navigation, strings in all seven languages, tests and docs). Use when asked to build, add or extend a screen, flow, setting or capability of the app.
---

# Add a feature end to end

Work from the inside out: domain, then data, then ViewModel, then UI. For each layer, copy the closest existing example,
named below.

## 0. Plan first

Before you write code, write a short plan and confirm it with the maintainer:

- the user-visible behavior, including the empty, error and loading states;
- the layers and files involved;
- whether the schema changes (if so, also use the `room-migration` skill);
- whether data handling changes (if so, `PRIVACY.md` and the Data safety form are affected);
- any new dependency. Avoid one if you can, and ask before adding it.

Ask up to three questions about anything ambiguous. Don't guess product rules: validation limits, defaults, what's blocked
and what's confirmed.

## 1. Domain (pure Kotlin, JVM-tested)

- Put models in `domain/model`, and keep behavior on them. Transaction rules belong in the `Transaction` subclasses.
- Write use cases in `domain/usecase`:
  - an `@Inject constructor` class that returns `Result<T>` through `runSuspendCatching { }`;
  - rule violations throw `domainFailure(DomainError.NEW_RULE, "dev message", arg)`.
  - Examples: `CategoryUseCases`, `ValidateTransactionUseCase`.
- For a new `DomainError`, add the constant, its mapping in `UiMessage.DOMAIN_ERROR_STRINGS` (or a plural branch), and the
  string resource in all seven `strings.xml` files.
- "Now" comes from the injected `ZonedClock`, and the zone from `clock.timeZone`. Use kotlinx-datetime types, not `java.time`.

## 2. Data

- **DAO:** add a `@Query` with bound parameters to the DAO in `data/local/db/dao`. Return a `Flow` for observed data and
  `suspend` for one-shot reads. Load only what the screen needs.
- **Repository:** add to the interface and its `Impl` in the same file. Writes that touch balances run inside
  `withTransaction` with `Ledger.balanceDeltas`.
- **A new repository:** `@Binds` it in `di/RepositoryModule.kt`, unscoped.
- **Fakes:** update the fake in `app/src/test/java/com/denariidolor/testutil/FakeRepositories.kt`.

## 3. ViewModel

Put it in `presentation/ui/<feature>/<Feature>ViewModel.kt`. Examples: `CategoryViewModel` (simple CRUD) and
`TransactionViewModel` (form, save guard, over-budget confirmation).

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(private val useCases: FeatureUseCases, repository: FeatureRepository) : ViewModel() {
    val uiState: StateFlow<FeatureUiState> = repository.observe()
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeatureUiState())

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()
}
```

- No `Context` and no Android types. Navigation arguments come from `SavedStateHandle`.
- Write actions ignore repeat calls while one is in flight, and expose `isSaving` so the button disables.
- Turn results into messages with `UiMessage.fromResult(result, R.string.feature_saved)` or `UiMessage.fromError(e)`.

## 4. UI

Put it in `presentation/ui/<feature>/<Feature>Screen.kt`.

- `FeatureRoute(onBack: () -> Unit, viewModel: FeatureViewModel = hiltViewModel())` calls `collectAsStateWithLifecycle()`
  and `UiMessageEffect(viewModel.messages)`, then the screen.
- `FeatureScreen(state, onAction…)` is stateless. Use `ScreenHeader`, `ConfirmDialog`, `ManagedItemRow` and the form
  components in `presentation/ui/common`.
- Declare `const val FEATURE_…_TAG` constants for anything a test needs to find.
- For toasts, sharing, store or email links and the date or file pickers, use the `remember…()` functions in
  `presentation/platform`. Never use `LocalContext`, `Toast` or `Intent` in a screen; `PlatformBoundaryTest` fails on it.
- Follow `.claude/rules/compose-ui.md`: `stringResource` only, `LocalLocale`, `start`/`end`, insets, `contentDescription`,
  48dp touch targets.
- **Navigation:**
  - Add an entry to `MainDestination` in `presentation/ui/AppScreens.kt` and a `composable(...)`.
  - Close the screen with `navController.popBackOnce()`.
  - Settings sub-screens are opened from a new `SettingsScreen` callback, wired to `navController.navigate(...)` in
    `AppScreens.kt`.

## 5. Strings

- Add every new string to `res/values/strings.xml` and to the six packs: `values-zh-rCN`, `values-hi`, `values-es`,
  `values-ar`, `values-fr` and `values-pt`.
- Plurals need every quantity each language requires (see `.claude/rules/i18n-resources.md`).
- Your summary lists the new keys as needing review by a native speaker.

## 6. Tests

- **JVM:**
  - use case rules: success and each `DomainError`;
  - mappers and parsers;
  - the ViewModel, with `MainDispatcherRule`, fakes and a fixed `Clock`. Follow `DashboardViewModelTest` and
    `TransactionViewModelTest`.
- **Instrumented:**
  - new DAO queries (`TransactionDaoTest` style);
  - the screen, with `createAndroidComposeRule<ComponentActivity>()` and test tags (`CrudScreensTest` style);
  - navigation, if it changed (`NavigationTest`).

## 7. Docs and checks

- **README:** update the user guide section for the feature, and the feature list if it changes.
- **PRIVACY.md:** update it if what the app stores, exports or shares changed.
- **Gates:** run the `ci-gates` skill.
- **Summary:**
  - the behavior;
  - the files by layer;
  - the tests added;
  - the commands and their results;
  - the strings that need translation review;
  - anything left for manual checks (an emulator sign-in, right-to-left layout, dark mode, font scale).
