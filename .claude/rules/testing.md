---
paths:
  - "app/src/test/**"
  - "app/src/androidTest/**"
---

# Tests

## What each change needs

| Change | Test |
|---|---|
| Domain logic, a use case, a mapper, a parser or a ViewModel | JVM unit test in `app/src/test` |
| A DAO query, a migration, a repository transaction, Room/SQLCipher, Keystore, PDF rendering or a Compose screen | Instrumented test in `app/src/androidTest` |
| A new `DomainError` | Its mapping, covered by `UiMessageTest` |
| A bug fix | A test that fails without the fix |

Never delete a test, mark it `@Ignore` or loosen an assertion to get a green build. Fix the cause, or report it.

## JVM unit tests

- **Shared code:** its tests still live in `app/src/test`, in the same package, until the tests move to `commonTest`.
- **Framework:** JUnit 4 (`org.junit.Test`, `org.junit.Assert.*`) with `kotlinx-coroutines-test` (`runTest`).
- **No mocking library.** Use or extend the fakes and the `TestData` builders, which are all in
  `testutil/FakeRepositories.kt`. Ask before adding Mockito, MockK, Robolectric, Turbine or Truth.
- **ViewModels:**
  - Add `@get:Rule val mainDispatcherRule = MainDispatcherRule()`.
  - Build the ViewModel directly, with fakes, real use cases over those fakes, and a fixed clock:
    `Clock.fixed(Instant.parse("2026-09-20T12:00:00Z"), ZoneOffset.UTC)`. Use cases take it as `.asZonedClock()`.
- **`WhileSubscribed` state:** start a collector (`val collector = launch { viewModel.uiState.collect {} }`), wait with
  `first { predicate }`, assert, then `collector.cancel()`. `DashboardViewModelTest` is the model.
- **Names:** descriptive camelCase that states the behavior, such as `summarizesCurrentMonthOnlyAndFlagsBudgets`. Test one
  behavior per test.
- **Money:** write amounts as `Long` cents with separators, such as `9_000`.
- **Android APIs:** code under JVM test must not call them; they throw "Method not mocked". Move the logic into pure Kotlin
  instead.

## Instrumented tests

- **Room:** use an in-memory database or a test-only name. Migration tests use `MigrationTestHelper`. The schemas in
  `app/schemas` are an androidTest asset; follow `MigrationTest`.
- **Vault and Keystore:** use a test-only `VaultConfig` (a separate database name, key aliases and prefs file), as
  `VaultTest` does. Never use the app's real names.
- **Compose:**
  - Host screens with `createAndroidComposeRule<ComponentActivity>()`, and select nodes by the `*_TAG` constants declared in
    the screen file.
  - `ComponentActivity` sets every view-tree owner itself, but the app's `AppCompatActivity` doesn't always. The 2026-10-11
    sign-in crash passed every screen test for that reason.
  - Behavior that depends on AppCompat or `MainActivity` needs a manual sign-in on an emulator, or a test that hosts
    `MainActivity` with an unlocked test vault. That test is planned but not written yet.
- **UiAutomator doesn't wait for Compose.** Call `composeRule.waitUntil { … }` or `waitForIdle()` before you send system
  events. Before a key event aimed at a dialog, wait for its window with `UiDevice.wait(Until.hasObject(...))`. CI's API 26
  emulator is slow.
- **API 26–27:** a dialog whose first focusable element is a text field opens with the keyboard up, so the first Back only
  closes the keyboard. Call `Espresso.closeSoftKeyboard()` before testing a dialog's Back behavior.
- **API levels:** limit a test to some levels with `@SdkSuppress`, not `assumeTrue`, because AGP 9.4's test engine reports a
  failed assumption as a failure.
- **Espresso** stays at 3.7.0 or later; 3.6.1 fails on API 37 emulator images.
- **CI** runs the instrumented tests on API 26 (minSdk) and API 36 (targetSdk). The console doesn't name the failing tests,
  so read `TEST-*.xml` in the `instrumented-results-api-*` artifact.

## Running tests

```bash
./gradlew testDebugUnitTest --tests "com.denariidolor.presentation.viewmodel.DashboardViewModelTest"
./gradlew createDebugUnitTestCoverageReport
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.denariidolor.data.MigrationTest
```

- Unit test reports are in `app/build/reports/tests/testDebugUnitTest/`, and the JaCoCo report is in
  `app/build/reports/coverage/`.
- Run the instrumented tests only when `adb devices` lists a device. If none is attached, say in your summary that they
  weren't run.
