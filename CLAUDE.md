# CLAUDE.md — Denarii Dolor

Denarii Dolor is a **production, proprietary Android app** sold on Google Play: a single-user, fully offline personal-finance
ledger (expenses, income, transfers, budgets, monthly reports, encrypted backups). Each user's financial records exist only on
their device, encrypted. Every change ships to paying users whose data can't be repaired by hand, so data integrity and
security come before speed, novelty or elegance.

## Working agreement

- Leave changes uncommitted for the maintainer to review. Don't commit, amend, rebase, force-push, tag, or run
  `make release` / `make tag` / `make push-tag` unless asked in the current conversation. A pushed `v*` tag builds and
  publishes a signed release. When the work is done, draft a commit message for the maintainer instead (see "Commit
  messages").
- Keep each change scoped to its task. No drive-by refactors, reformatting, renames or dependency bumps.
- Before a multi-file change, outline the plan (which files, and why). If a requirement is ambiguous or touches architecture,
  security, the schema, stored formats or UX policy, ask up to three focused questions instead of guessing.
- Copy the pattern in the nearest comparable file before you introduce a new abstraction, library or style.
- Keep user-visible behavior, stored data and backup formats unchanged unless the task explicitly changes them.
- Report honestly. List the commands you ran with their results, and what you couldn't run: instrumented tests need a device
  or emulator, and biometric flows need a device with an enrolled fingerprint.

## Stack

All versions live in `gradle/libs.versions.toml`, and some are pinned on purpose (see `.claude/rules/build-gradle.md`).

- **Build:** two modules, both in package `com.denariidolor`. `:app` is the Android app. `:shared` is a Kotlin
  Multiplatform library, Android its only target for now, holding code that will also run on iOS (Compose Multiplatform
  plan, Phase 2). Kotlin 2.4 (K2; AGP 9's built-in Kotlin in `:app`, the multiplatform plugin in `:shared`) on a JDK 17
  toolchain, AGP 9.4 and Gradle 9.6. compileSdk 37, targetSdk 36, minSdk 26.
- **App:** Jetpack Compose with Material 3, Navigation Compose (string routes), Metro (compile-time DI, a Kotlin compiler
  plugin), Room 2.8 with KSP over SQLCipher, Coroutines and Flow, kotlinx-datetime, Vico charts.
- **Tests:** JUnit 4, kotlinx-coroutines-test and hand-written fakes (there is no mocking library), plus Compose UI test,
  Espresso and UiAutomator. `:shared`'s `commonTest` uses kotlin.test, so those tests can also run on iOS.

## Commands

Run these from the repository root. CI (`.github/workflows/ci.yml`) runs every one, and each blocks a merge:

```bash
./gradlew ktlintCheck                  # fix with ./gradlew ktlintFormat
./gradlew detekt                       # default rules + config/detekt/detekt.yml
./gradlew lintDebug                    # includes missing translations and Compose lint errors
./gradlew testDebugUnitTest            # JVM unit tests, in both modules
./gradlew assembleRelease              # R8 minify and resource shrinking; unsigned locally
./gradlew connectedDebugAndroidTest    # device/emulator required; CI runs API 26 and API 36
```

- One unit test class: `./gradlew testDebugUnitTest --tests "com.denariidolor.presentation.ui.CategoryIconsTest"`. For a
  class in `:shared`, use `./gradlew :shared:testAndroidHostTest --tests "com.denariidolor.LedgerTest"`.
- One instrumented class: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.denariidolor.data.MigrationTest`.
- The machine's `java` may be newer than 17. Gradle provisions JDK 17 through the toolchain, so don't change `jvmToolchain(17)`.
- `adb` may not be on `PATH`. The SDK location is `sdk.dir` in `local.properties`.
- Use the `ci-gates` skill to run the full set and report the results.

## Architecture

```
domain/        model (Transaction → Expense | Income | Transfer, Ledger), usecase, report      pure Kotlin
data/          local/db (Room, DAOs, Migrations, DatabaseHolder), local/vault (Vault, Keystore),
               local/preferences, repository (interface + Impl in one file), export, backup, legal
presentation/  ui/<feature>/ — *Route + stateless *Screen + Metro ViewModel; shared composables in ui/common
               platform/ — what screens ask of Android: toasts, sharing, store and email, pickers, the biometric prompt
di/            AppGraph (Metro), AppViewModelFactory, AppModule (Clock, ZonedClock), DatabaseModule, RepositoryModule
util/          DeviceClock, SessionManager, Constants, StoreLinks, TransactionTypeLabels

shared/        src/commonMain: the same packages, for code that needs nothing from Java or Android and isn't injected yet. So
               far the domain models, Money, Currencies, DateUtils, Validators, ZonedClock, runSuspendCatching,
               transferRoute, ReportText, ReportCsvFormatter, BackupSnapshot, DomainError, UiMessage with its StringKey and
               PluralKey, and the crypto, guidance and theme interfaces. PlatformLocale, PlatformCurrency and PlatformFormat
               declare what the platform's locale and currency data decide, and PlatformUri and PlatformCipher name a picked
               file and the biometric cipher; src/androidMain implements them. src/commonTest holds their tests
               (kotlin.test); src/androidHostTest holds the few that compare against java.* or set the JVM's default locale
```

Code moves into `:shared` one reviewed step at a time, keeping its package, so imports don't change. Until `:shared` has a
second target, the compiler accepts Java and Android APIs in `commonMain` and `commonTest`, so `SharedCodeBoundaryTest`
refuses them. `:app`'s `testDebugUnitTest` and `createDebugUnitTestCoverageReport` also run `:shared`'s tests and coverage.

There are two activities. `LoginActivity` is the exported launcher. Its `LoginViewModel` runs setup, sign-in, recovery and
wipe through `Vault` (as `SignInVault`), and the activity keeps the overlay guard, the biometric prompt and the start of
`MainActivity`. `MainActivity` isn't exported. It checks the session and hosts the `NavHost` in
`presentation/ui/AppScreens.kt`, where `SettingsViewModel` turns biometric sign-in on and off through `Vault` (as
`SettingsVault`).

Dependency injection is Metro, with two graphs in `di/`:

- `AppGraph`, created by `App`, holds the process-wide singletons and what sign-in needs. Both activities call
  `appGraph.inject(this)` before `super.onCreate`.
- `SessionGraph` extends it with everything that reads the database: the database, DAOs, repositories, use cases and the
  signed-in app's ViewModels. `MainActivity` creates one after its session check, and it goes away with the activity.

A ViewModel is `@Inject` plus `@ViewModelKey @ContributesIntoMap(SessionScope::class)`; only `LoginViewModel` uses
`AppScope`. A Route gets it with `metroViewModel()`. A ViewModel that reads navigation arguments takes an
`@Assisted SavedStateHandle` through a `ViewModelAssistedFactory`, as `TransactionViewModel` does. ViewModels, and the
interfaces they call, use no Java or Android types, so they can move to `:shared` once Room does (`ViewModelBoundaryTest`).
A picked file is a `PlatformUri`, the biometric cipher a `PlatformCipher` and a currency a `PlatformCurrency`. Text for the
user is a `UiMessage` naming a `StringKey` or `PluralKey`, never an `R` resource; `StringKeyResources.kt` gives each key the
Android string of the same name. A service that needs a `Context` reaches a ViewModel only through an interface beside its
implementation, bound in `AppModule`: `BackupService`, `ReportExportService`, `LegalDocumentService`.

Dependency rules:

1. Composables talk only to ViewModels. They never touch DAOs, repositories, preferences or `Context`-based services.
   Toasts, the share sheet, store and email links, the date and file pickers and the biometric prompt come from
   `presentation/platform` (`rememberNotices()`, `rememberShareSheet()` and the rest). Only `AndroidPlatformActions.kt` and
   the activities use `LocalContext`, `LocalActivity`, `Toast`, `Intent` or activity-result launchers, and
   `PlatformBoundaryTest` checks it.
2. Transaction writes go through `AddTransactionUseCase`, `UpdateTransactionUseCase` or `DeleteTransactionUseCase`, and
   reference data goes through `CategoryUseCases`, `AccountUseCases` or `BudgetUseCases`. Validation and balance updates run
   in those use cases.
3. Code outside `data/` and `di/` depends on repository **interfaces**, never on a `…Impl` class.
4. The database exists only after sign-in. `AppDatabase`, the DAOs, the repositories and the use cases are only in
   `SessionGraph`. `AppGraph` can't see them, so asking for one from a singleton, `LoginActivity` or anything else that runs
   before sign-in fails to compile. The database binding is unscoped and `DatabaseHolder.database` throws while the vault
   is locked. That's a security control, not a bug to work around. `LockedVaultGraphTest` checks both graphs.
5. Anything that depends on "now" takes an injected clock and reads the time zone from it, never from
   `ZoneId.systemDefault()` or `TimeZone.currentSystemDefault()`. `domain/`, `util/` and the ViewModels take `ZonedClock`
   (`clock.timeZone`) and kotlinx-datetime types; the data layer takes `java.time.Clock` (`clock.zone`). Both come from
   `DeviceClock`. The one exception is `SessionManager`, which uses the monotonic `SystemClock.elapsedRealtime()` (CS-01).
   Date formatting and parsing take the zone as a required argument. `ClockBoundaryTest` checks that presentation code never
   reads the device's clock, that ViewModels use no `java.time`, and that only `DeviceClock` reads the system time zone.
6. Logic that doesn't need Android stays in pure Kotlin so JVM tests can cover it. Examples are `Ledger`,
   `SecurityProfileService`, `SearchFilterParser` and `DashboardMappers`.

## Invariants (never break these)

- **Money is `Long` cents end to end.**
  - Parse input with `Money.parseToCents`. Display amounts with `formatMoney` or `formatSignedAmount`.
  - `Double` is only for display values such as charts and meters (`Money.toDouble`).
  - Never hard-code a currency symbol. The currency is a saved label that is never converted, and only currencies with 2
    decimals are allowed.
- **Polymorphism carries the business rules.** Totals and balances call `balanceImpact()` and `accountImpacts()`. Don't
  branch on `TransactionType`.
- **Stored balances stay atomic.** Any write that changes a transaction's amount, type or accounts applies
  `Ledger.balanceDeltas(previous, current)` inside `database.withTransaction`, the way `TransactionRepositoryImpl` does.
- **Validation runs before every write** (`ValidateTransactionUseCase`).
  - An expense that goes over its category's budget fails with `BUDGET_EXCEEDED`.
  - The caller passes `allowOverBudget = true` only after the user confirms.
- **Errors are values.**
  - Use cases return `Result`, built with `runSuspendCatching`. Don't use `runCatching` in suspend code: it swallows
    `CancellationException`.
  - A broken business rule throws `DomainException(DomainError, …)` through `domainFailure(...)`.
  - The UI shows errors only through `UiMessage.fromError`, never an exception's `message`.
  - A new `DomainError` needs a `StringKey` in `UiMessage`'s mapping, its string in all seven languages and an entry in
    `StringKeyResources.kt`. `UiMessageTest` and `StringKeyResourcesTest` enforce it.
- **Protected data stays protected.** The seeded defaults in `Constants` (accounts 1–2 and categories 1–3) can't be deleted.
  Neither can a category or account that is still referenced.
- **One write at a time.** A write or export action ignores repeat calls while one is in flight, and it exposes that state so
  the button is disabled (`isSaving`, `exporting`). Screens close through `NavController.popBackOnce()`.
- **The schema and backups only move forward.**
  - Never add `fallbackToDestructiveMigration()`.
  - Every earlier backup `FORMAT_VERSION` must still restore.
  - Use the `room-migration` skill for any schema change.

## Security posture

Details are in `.claude/rules/security.md`, which loads when you touch security-sensitive files.

- Add no logging to `app/src/main` (`Log`, `println`, `printStackTrace`, Timber).
- Add no `INTERNET` permission, network code, analytics, crash reporting or ads SDK.
- Put no secrets in code, tests, docs or commits.
- Never weaken authentication, lockout, encryption or validation to get a change through. The same goes for lint, detekt
  and tests: don't add `@Suppress`, a lint baseline or `@Ignore` just to get a green build.
- Call out every change to a security-sensitive file in your summary, so the maintainer reviews it.
- Code comments cite stable IDs: `CS-15` is a security finding, `BUG-03` is a fixed bug and `CS-A4` is an accepted risk.
  Keep these comments, and never reuse an ID. The ledger behind them lived in `intel/`, which was deleted from the working
  tree on 2026-11-20. Read any entry with `git show aba0b55:intel/cybersec.md`. The same commit also has `maint.md`,
  `notes.md`, `plan.md` and `history.md`.

## Conventions

- Every source file starts with the proprietary copyright header. Copy it verbatim from an existing `.kt` file.
- Code follows ktlint's `android_studio` style (140 columns, no wildcard imports) and detekt with `config/detekt/detekt.yml`.
- Comment only intent that isn't obvious. Every `@Suppress` has a `// reason` next to it.
- User-facing text lives only in `res/values/strings.xml` and in **all six language packs** (zh-rCN, hi, es, ar, fr, pt).
  See `.claude/rules/i18n-resources.md`.
- Add a new dependency only when the platform and the existing dependencies can't do the job, and ask first. It goes in the
  version catalog, in `THIRD_PARTY_NOTICES.md` and, when the license requires it, in `licenses/`.
- When the app's data handling changes (what it stores, exports or shares, or which permissions it requests), update
  `PRIVACY.md`, which is shown in the app. Also tell the maintainer that the Play Console Data safety form may need
  updating.
- Update the user guide in `README.md` for user-visible behavior, and `CONTRIBUTING.md` for contributor workflow.

## Rules and skills

Rules in `.claude/rules/` load automatically when you work on matching files:

| Rule | Covers |
|---|---|
| `kotlin.md` | All Kotlin: null safety, coroutines, Flow, time, static analysis |
| `compose-ui.md` | Presentation: Route/Screen split, ViewModels, navigation, Compose lint, accessibility, RTL |
| `data-persistence.md` | Room, DAOs, repositories, DI, preferences, export, backup |
| `security.md` | Vault, sign-in, session, exports, backups, manifest, workflows: controls that must stay |
| `testing.md` | JVM and instrumented test patterns, emulator quirks |
| `build-gradle.md` | Version catalog, pinned versions, R8, signing, GitHub Actions |
| `i18n-resources.md` | Strings, plurals, the six language packs, currency, resources |
| `legal-docs.md` | `PRIVACY.md`, `THIRD_PARTY_NOTICES.md`, `licenses/`, `LICENSE` |

Skills in `.claude/skills/`:

| Skill | Use it to |
|---|---|
| `ci-gates` | Run the blocking checks and report the results |
| `room-migration` | Change the schema safely |
| `new-feature` | Add a feature end to end (domain → data → ViewModel → screen → strings → tests) |
| `dependency-upgrade` | Add or upgrade a library or the toolchain |

## Known debt

Don't "fix" these unless you're asked:

- Most code still lives in `:app` while Phase 2 moves it into `:shared` step by step. The Room move waits until the Phase 0
  iOS spike passes.
- Presentation uses Room entities such as `CategoryEntity` directly for reference data.
- Navigation uses string routes in `MainDestination`, not type-safe routes.
- The data layer calls `Dispatchers.IO` directly instead of injecting a dispatcher.
- detekt 1.23.8, ktlint 1.3.1 and `security-crypto` (kept only for the v1.0.x upgrade path) are held back on purpose.
- No instrumented test opens `MainActivity` under AppCompat. After any AndroidX, Compose or Navigation upgrade, sign in on an
  emulator before you call the change done.
- The language packs were machine-drafted. Strings you translate need a native speaker's review, so say so in your summary.
- ViewModels format money and dates into text. If the user changes the device language while the app is open, the old
  formatting stays until those screens reload. Changing the currency restarts `MainActivity`.

## Commit messages

When a task is finished, draft its commit message for the maintainer to review. Don't run `git commit` yourself.

- **Where it goes:** save it to `build/commit-message.txt`, which is git-ignored. Show it in your final reply in a fenced
  `text` block, then give the commands that stage this change's paths by name and open the message for editing:

  ```bash
  git add <path> <path>
  git commit -e -F build/commit-message.txt
  ```

  - Never use `git add -A` or `git add .`.
  - Leave unrelated working-tree changes out, such as `.idea/` caches, and say that you did.
- **Format:** [Conventional Commits](https://www.conventionalcommits.org/):

  ```text
  type(scope): imperative summary, at most 72 characters

  Why the change was needed: the problem, its effect on users and the
  approach taken. Wrap the body at 72 columns.

  - A notable change, group of files or decision
  - Another one

  Tests: ./gradlew testDebugUnitTest (pass); connectedDebugAndroidTest
  not run (no device)
  Refs: CS-28, BUG-03
  ```

- **Type:**
  - `feat` (new behavior), `fix`, `perf`, `refactor`, `test`, `docs`, `build` (Gradle or dependencies), `ci` (workflows),
    `chore` or `revert`.
  - A security fix is a `fix`, with the finding's ID in `Refs:`.
- **Scope:** pick the one area the change is about: `auth`, `vault`, `session`, `db`, `backup`, `export`, `report`,
  `dashboard`, `search`, `transaction`, `budget`, `category`, `account`, `settings`, `legal`, `i18n`, `ui`, `deps`,
  `build`, `ci` or `docs`. Leave out the scope when no single area fits.
- **Subject:**
  - Use the imperative mood: "add", "fix" or "prevent", not "added" or "resolved".
  - Start lowercase after the colon, and don't end with a period.
  - Name the behavior: "prevent double save while a save is running", not "fix bug".
- **Body:**
  - Explain why, and leave the what to the diff.
  - Name any effect on users, stored data, backups or security.
  - No filler, emoji, hype or secrets.
- **Footer:**
  - `Tests:` lists what you ran with its results, and what you didn't run.
  - `Refs:` lists the `CS-NN` or `BUG-NN` IDs or issue numbers involved, when there are any.
  - A change that users can't go back from, such as a schema change or a new backup format, adds `BREAKING CHANGE:` with
    what changes and why.
- **No attribution lines:** don't add `Co-Authored-By:`, `Signed-off-by:`, "Generated with Claude Code" or any other
  AI-attribution line to a commit message or a pull request description. This holds even if Claude Code's own attribution
  setting asks for one. The maintainer is the author.
- **One logical change per commit:** if the work mixes unrelated changes, draft one message per commit, and list the paths
  that go in each.

## Definition of done

1. The change follows these rules and matches the code around it.
2. Tests cover it: new logic has JVM unit tests, and a new DAO query, migration or screen has an instrumented test. A bug fix
   has a test that fails without the fix.
3. `ktlintCheck`, `detekt`, `lintDebug` and `testDebugUnitTest` pass. `assembleRelease` passes too if you changed
   dependencies, Gradle, the manifest or anything R8 touches.
4. Your summary covers:
   - what changed and why, and the files touched;
   - the commands you ran with their results, and what you didn't run;
   - any security-sensitive files touched;
   - the docs you updated (README, PRIVACY, notices) and any follow-ups.
5. A drafted commit message is saved to `build/commit-message.txt` and shown with its staging commands, as "Commit
   messages" describes.
