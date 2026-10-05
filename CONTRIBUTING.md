# Contributing to Denarii Dolor

Thank you for helping. Please follow the [Code of Conduct](CODE_OF_CONDUCT.md). The architecture and maintainability rules in [`intel/maint.md`](intel/maint.md) are authoritative, and this guide summarizes what a contributor needs from them.

Security vulnerabilities are the exception: report them through a private security advisory on the repository, not a public issue.

Denarii Dolor is proprietary software owned by Joseph Anthony Abbott III (see [LICENSE](LICENSE)). A contribution can be accepted only after its author signs a written agreement that assigns their rights in it to him.

## Setup

**Prerequisites**

- Git.
- A JDK to run Gradle. The project compiles with a JDK 17 toolchain (`jvmToolchain(17)`), which the foojay resolver in `settings.gradle.kts` can download if it's missing. CI uses Temurin 17. `gradle/gradle-daemon-jvm.properties` requests JDK 21 for the Gradle daemon.
- The Android SDK with platform 37 (`compileSdk = 37`). Point Gradle at it with `ANDROID_HOME` or with `sdk.dir` in `local.properties`, which is git-ignored.
- For instrumented tests, a device or emulator running API 26 or newer. CI runs API 26 and 36. With Espresso 3.7.0 the Compose UI tests also run on API 37 images.

Use the Gradle wrapper (`./gradlew`); don't install Gradle separately.

```bash
git clone https://github.com/jabbott-iii/DenariiDolor.git
cd DenariiDolor
./gradlew installDebug        # build and install the debug app on a connected device or emulator
```

## Workflow

1. Get the concept approved on its issue (see the rules above).
2. Branch from `main`, and keep the branch focused on that issue. Don't mix in unrelated refactoring, formatting, dependency upgrades or renames.
3. Make the change following the coding expectations below.
4. Run the validation commands and fix any failures.
5. Open a pull request against `main` that links the issue.

The maintainer (`@jabbott-iii`, see `CODEOWNERS`) reviews every pull request. Releases are cut from `main` by the maintainer with `make release VERSION=vX.Y.Z`; contributors don't push tags.

## Validation

Run these from the repository root before opening a pull request. CI runs the same checks, and all of them block a merge.

```bash
./gradlew ktlintCheck                         # Kotlin style (fix with ./gradlew ktlintFormat)
./gradlew detekt                              # static analysis (default rules + config/detekt/detekt.yml)
./gradlew lintDebug                           # Android lint
./gradlew testDebugUnitTest                   # JVM unit tests
./gradlew createDebugUnitTestCoverageReport   # JaCoCo coverage report
./gradlew assembleRelease                     # release build with R8 shrinking (unsigned locally)
./gradlew connectedDebugAndroidTest           # instrumented tests (device or emulator required)
```

The security workflow also runs CodeQL, gitleaks and, on pull requests, dependency review, which fails on high-severity vulnerabilities.

If you run detekt-cli outside Gradle, pass `--build-upon-default-config` so that it matches CI.

## Coding expectations

- **Architecture:** follow the layers in `intel/maint.md`:
  - Composables talk only to ViewModels.
  - Transaction writes go through the use cases, so validation and balance updates always run.
  - Code outside `data` and `di` depends on repository interfaces, not on the `Impl` classes.
  - The database exists only after sign-in. Don't inject `AppDatabase`, a DAO or a repository into anything that runs before sign-in (`LoginActivity`, singletons); sign-in, recovery and wipe go through `Vault`.
- **Money:** amounts are `Long` cents. Parse input with `Money.parseToCents` and show amounts with `formatMoney`/`formatSignedAmount`, which use the saved currency and the device's locale. Never store or compare amounts as `Double`, and never write a currency symbol into code or strings.
- **Text and languages:**
  - User-facing text lives only in `res/values/strings.xml`. Add every new string to all six packs (`values-zh-rCN`, `values-hi`, `values-es`, `values-ar`, `values-fr`, `values-pt`), or mark it `translatable="false"`; `lintDebug` fails on a missing translation.
  - Translations keep the source's format arguments (`%1$s`, `%d`, `%%`), escape `'` and `"`, and give plurals every quantity the language needs (Chinese: `other`; Hindi: `one`, `other`; Spanish, French, Portuguese: `one`, `many`, `other`; Arabic: `zero`, `one`, `two`, `few`, `many`, `other`).
  - Don't show stored values as text: a `TransactionType` is shown through `labelRes()`, and the CSV keeps its English keys.
  - Layouts must work right to left (Arabic). Use `start`/`end`, not `left`/`right`.
- **Errors:** use cases return `Result`. In suspend code, use `runSuspendCatching` rather than `runCatching`.
- **Database changes:**
  1. Bump the version.
  2. Add a `Migration` and register it in `DatabaseHolder`.
  3. Commit the new schema JSON in `app/schemas/`.
  4. Extend `MigrationTest`.

  Never add `fallbackToDestructiveMigration()`. Queries use Room `@Query` with bound parameters only.
- **Tests:**
  - New business logic needs JVM unit tests. Use the fakes in `app/src/test/.../testutil`.
  - New DAO queries, migrations or screens need instrumented tests.
  - In Compose tests, find elements by `testTag`.
- **Style:**
  - Start every source file with the proprietary copyright notice (copy it from an existing file), and follow ktlint's Android style.
  - A new dependency must be added to `THIRD_PARTY_NOTICES.md`, with its license text in `licenses/` when that license requires it.
  - `THIRD_PARTY_NOTICES.md` and `PRIVACY.md` are shown inside the app, so keep them to the Markdown the app renders (see `intel/maint.md` §7); `LegalTextTest` checks them. A change to what the app stores or shares needs a matching change to `PRIVACY.md`.
  - Composables are PascalCase, with a `…Route` wrapper that wires the ViewModel.
  - Add comments only for intent that isn't obvious.
  - Don't suppress a warning without a comment explaining why.
- **Dependencies:** add a new library only when an existing dependency or the platform can't do the job. Set versions only in `gradle/libs.versions.toml`. Some versions are pinned or held back on purpose (Kotlin and KSP move together; detekt 1.23.8, ktlint 1.3.1, Vico 2.0.0); check `intel/maint.md` section 8 before bumping one.
- **Security:**
  - Don't add logging (`Log`, `println`, `printStackTrace`), network access, or secrets of any kind.
  - Don't weaken authentication, encryption, validation, lint or tests to get a change through.
  - Changes to the security-sensitive files listed in `intel/maint.md` section 5 need an entry in `intel/cybersec.md`.

## Pull request expectations

- Link the approved issue, and describe what changed and why.
- List the validation commands you ran and their results. Say which checks you didn't run (for example, instrumented tests without a device).
- Keep the change small and focused, and preserve existing behavior unless the issue approves a change.
- Point out any security-sensitive change so that it gets a careful review.
- Update the documentation only where your change affects it: `README.md` for user-visible behavior, and the `intel/` documents for architecture, structure or security.
- CI and the security workflow must pass before merge.
