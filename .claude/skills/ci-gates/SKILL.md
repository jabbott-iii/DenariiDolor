---
name: ci-gates
description: Run Denarii Dolor's blocking CI checks locally (ktlint, detekt, Android lint, JVM unit tests, the R8 release build, and the instrumented tests when a device is attached), fix or report the failures, and summarize each result. Use before calling a change done, before a pull request, or when asked to verify, check or validate the build.
---

# Run the CI gates

CI (`.github/workflows/ci.yml`) blocks a merge on every one of these checks. Run the same checks, in the same order.

## 1. Look at what changed

```bash
git status --short
```

Note which files are yours, so you can spot formatter changes to files you didn't touch.

## 2. Static checks and unit tests

Run them in one Gradle invocation, so configuration is shared and every failure is collected:

```bash
./gradlew --continue ktlintCheck detekt lintDebug testDebugUnitTest
```

This can take several minutes, so run it in the background if your shell has a timeout.

If something fails, read the report rather than guessing:

| Check | Report |
|---|---|
| ktlint | the console output, and `app/build/reports/ktlint/` |
| detekt | `app/build/reports/detekt/` (`detekt.txt` or `.html`) |
| Android lint | `app/build/reports/lint-results-debug.*` (HTML and XML) |
| Unit tests | `app/build/reports/tests/testDebugUnitTest/index.html` and `app/build/test-results/testDebugUnitTest/TEST-*.xml` |

How to fix each kind of failure:

- **ktlint:** run `./gradlew ktlintFormat`, then `git diff --stat`. Revert any hunks in files you didn't change, and run
  `ktlintCheck` again.
- **detekt:** refactor the code. Suppress only with a narrow `@Suppress("Rule") // Intentional: reason`. Never add a
  baseline file.
- **lint:**
  - A `MissingTranslation` error means a string is missing from one of the six packs. Add it there (see
    `.claude/rules/i18n-resources.md`).
  - `NonObservableLocale` and `LocalContextGetResourceValueCall` mean a composable reads its locale or a string the wrong
    way (see `.claude/rules/compose-ui.md`).
  - Never add a lint baseline or a `lint {}` disable.
- **Tests:** fix the cause. Never `@Ignore` a test or weaken an assertion.

## 3. Release build

Run this when the change touches dependencies, Gradle, the manifest, reflection, serialization, Room entities or anything
else R8 could strip:

```bash
./gradlew assembleRelease
```

It is unsigned locally, which is expected. A failure that mentions missing classes needs a narrow keep rule in
`app/proguard-rules.pro`.

## 4. Instrumented tests (only with a device)

```bash
"$(sed -n 's/^sdk.dir=//p' local.properties)/platform-tools/adb" devices
```

If a device or emulator is listed, run the tests in the background, because they take several minutes:

```bash
./gradlew connectedDebugAndroidTest
```

To run one class:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.denariidolor.data.MigrationTest
```

The results are in `app/build/outputs/androidTest-results/`. If no device is attached, say so: CI runs these on API 26 and
API 36.

## 5. Report

End with a table, and don't call a check passed unless you ran it and it passed:

| Check | Result | Notes |
|---|---|---|
| ktlintCheck | ✅ / ❌ / not run | |
| detekt | | |
| lintDebug | | |
| testDebugUnitTest | | X tests, Y failures |
| assembleRelease | | |
| connectedDebugAndroidTest | | the device's API level, or "no device" |

Also list anything only CI or a person can check: the API 26 emulator, a signed release, biometrics, an update from the
installed release.
