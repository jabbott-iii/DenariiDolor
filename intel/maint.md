# Denarii Dolor — Architecture and Maintainability Guide (`maint.md`)

This file is the authoritative source for how the app is structured and how it should be changed. `CONTRIBUTING.md` must stay consistent with it. The structure map and diagrams are in [`map.md`](map.md), the security requirements and findings are in [`cybersec.md`](cybersec.md), and the reasons behind past decisions are in [`notes.md`](notes.md).

## 1. System overview

- A single-user, offline Android app. There is no `INTERNET` permission, no server, and no network code.
- One Gradle module, `:app`, with package root `com.denariidolor`. Splitting it into feature modules is not required (decision of 2026-09-20).
- The layers are MVVM plus use cases plus repositories, wired with Hilt. Persistence is Room over SQLCipher.
- There are two activities:
  - `LoginActivity`: the exported launcher. It handles setup, sign-in, PIN recovery and wipe.
  - `MainActivity`: not exported. It hosts the Compose `NavHost` in `presentation/ui/AppScreens.kt`.

## 2. Layers and dependency rules

| Layer | Package | Contains | May depend on |
|---|---|---|---|
| Domain | `domain/model`, `domain/usecase`, `domain/report` | Transaction hierarchy, value types, use cases, report formatting | Repository **interfaces**, `util`, `java.time.Clock` |
| Data | `data/local/db`, `data/local/preferences`, `data/repository`, `data/export` | Room entities, DAOs, migrations, key management, encrypted prefs, repositories, CSV/PDF export | Android framework, Room, SQLCipher, security-crypto |
| Presentation | `presentation/ui/<feature>` | Compose screens (`*Route` + stateless screen), `@HiltViewModel`s, shared composables in `common/` | Use cases, repository interfaces, `ReportExporter` |
| DI | `di` | `AppModule` (Clock), `DatabaseModule` (SQLCipher Room), `RepositoryModule` (`@Binds` interface → `Impl`) | Everything it wires |
| Util | `util` | `Money`, `Validators`, `DateUtils`, `SessionManager`, `Constants`, `runSuspendCatching` | Nothing app-specific (except `SessionManager` using `SystemClock`) |

Rules:

1. Composables never touch DAOs, repositories or preferences directly. They go through a ViewModel.
2. Writes to transactions go through a use case (`Add`, `Update` or `DeleteTransactionUseCase`), so that validation and balance updates always run. Reference data goes through `CategoryUseCases`, `AccountUseCases` and `BudgetUseCases`.
3. Each aggregate has a repository interface with a `…RepositoryImpl` in the same file, bound in `RepositoryModule`. Code outside `data` and `di` depends on the interface.
4. Inject time through `Clock` (from `AppModule`) wherever a result depends on "now", so that tests can fix the time. The one exception is `SessionManager`, which must use the monotonic `SystemClock.elapsedRealtime()` (CS-01).
5. Keep logic that doesn't need Android in pure Kotlin so that JVM unit tests can cover it. Existing examples are `Ledger`, `SecurityProfileService` (behind `SecurityProfileStore`), `DatabaseKeys`, `ReportCsvFormatter`, `SearchFilterParser` and `DashboardMappers`.

## 3. Domain invariants (do not break)

- **Polymorphism carries the business rules.** `Transaction` is abstract. `Expense`, `Income` and `Transfer` override `balanceImpact()` and `accountImpacts()`. Totals and balances must call these methods rather than branch on `TransactionType`.
- **Money is `Long` cents end to end** (`amountCents`, `balanceCents`, `monthlyLimitCents`). User input is parsed with `Money.parseToCents`, which uses `BigDecimal` and rejects more than 2 decimals. Never introduce `Double` for stored or compared amounts. Floating point is acceptable only for display, as in the chart values (`Money.toDouble`) and budget meter fractions.
- **Stored balances stay atomic.** `TransactionRepositoryImpl` applies `Ledger.balanceDeltas(previous, current)` inside `database.withTransaction` on every add, edit and delete. Any new write path that changes a transaction's amount, type or accounts must do the same.
- **Validation happens before writes.** `ValidateTransactionUseCase` checks the amount, description, date, account and category IDs, the transfer destination (required and different from the source) and that the referenced rows exist. It also blocks an expense that would exceed its category's monthly budget; on an edit, the edited row is excluded from the total.
- **Seeded defaults are protected.** Cash and Savings (account IDs 1 and 2) and the three default categories (IDs 1–3) come from `Constants`. `DefaultDataInitializer` seeds them, and they cannot be deleted. A category or account that is still referenced cannot be deleted either (no reassignment).
- **Errors are values.** Use cases return `Result`. Wrap suspend work in `runSuspendCatching`, not `runCatching`, so that `CancellationException` still propagates.
- **ViewModel output:** long-lived UI state goes in `StateFlow` (`stateIn(…, WhileSubscribed(5_000), …)`). One-shot events such as "saved" or "failed" go through a `Channel` exposed as a `Flow`, or through `UiMessage`.

## 4. Persistence and migrations

- The database is `denarii_dolor.db`, currently schema **v2**, with `exportSchema = true`. Schemas are exported to `app/schemas/` and are also an `androidTest` asset for `MigrationTest`.
- The four tables are `transactions`, `categories`, `budgets` and `accounts`.
  - `transactions.type` stores the `TransactionType` enum by name.
  - Foreign keys from `transactions.categoryId` and `transactions.accountId` use `RESTRICT`. `transactions.transferAccountId` has **no** foreign key: `AccountUseCases` prevents deleting an account a transfer points to, because `countByAccount` also counts `transferAccountId`. Keep that check if account deletion changes.
  - A budget cascades when its category is deleted, and there is one budget per category (unique index).
  - Category and account names have unique indexes. The duplicate-name checks in the use cases are also case-insensitive (`LOWER(name)`).
- All queries are Room `@Query` with bound parameters. Never build SQL strings.
- **Changing the schema:**
  1. Bump `version` in `AppDatabase`.
  2. Add a `Migration` in `Migrations.kt` and register it in `DatabaseModule.addMigrations(...)`.
  3. Build, then commit the new `app/schemas/.../<n>.json`.
  4. Extend `MigrationTest`.

  `fallbackToDestructiveMigration()` must not be reintroduced. Where the minimum-API SQLite (API 26) lacks a feature such as `DROP COLUMN`, recreate the table as `MIGRATION_1_2` does.
- **Encryption:** `DatabaseKeyProvider` keeps a random 256-bit key in its own EncryptedSharedPreferences file, `db_key_prefs`. `DatabaseModule` passes the key to SQLCipher as a raw key. If a database file exists but the key was just created, or the file is plaintext, the database is deleted and reseeded (`DatabaseKeys.shouldDiscard`).

## 5. Security-sensitive code

Changes to the files below are security-sensitive. Flag them for human review and record them in [`cybersec.md`](cybersec.md). Never weaken a documented control there.

| Area | Files |
|---|---|
| Sign-in, lockout, recovery, wipe | `presentation/ui/auth/LoginActivity.kt`, `BiometricAuthManager.kt`, `data/local/preferences/SecurityProfileService.kt`, `EncryptedPreferencesManager.kt` |
| Session gate | `util/SessionManager.kt`, `MainActivity.kt` |
| Encryption at rest | `data/local/db/security/*`, `di/DatabaseModule.kt` |
| Exports | `data/export/*`, `domain/report/ReportCsvFormatter.kt`, `res/xml/file_paths.xml` |
| Platform surface | `AndroidManifest.xml`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`, `app/proguard-rules.pro` |
| Supply chain | `.github/workflows/*`, `.github/dependabot.yml`, `gradle/libs.versions.toml`, `gradle/wrapper/*` |

Standing rules:

- Add no logging (`Log`, `println`, `printStackTrace`) to `app/src/main`.
- Add no network permission or network code.
- Non-sensitive settings go in plain prefs, as `ThemePreferences` does. Anything about credentials or data goes in encrypted storage.

## 6. Testing

| Kind | Location | Use for | Command |
|---|---|---|---|
| JVM unit | `app/src/test` | Domain, use cases (fakes in `testutil/FakeRepositories.kt`), ViewModels (`MainDispatcherRule`, `kotlinx-coroutines-test`), pure helpers | `./gradlew testDebugUnitTest` |
| Coverage | — | JaCoCo via AGP `enableUnitTestCoverage` | `./gradlew createDebugUnitTestCoverageReport` |
| Instrumented | `app/src/androidTest` | Room/SQLCipher, migrations, repository transactions, Compose screens, PDF rendering | `./gradlew connectedDebugAndroidTest` |

- New business logic needs unit tests. New DAO queries, migrations or screens need instrumented tests.
- Compose tests select elements with `testTag` constants defined next to the screen, such as `BIOMETRIC_BUTTON_TAG`.
- UiAutomator does not wait for Compose. Wait for the target state (`waitUntil` or `waitForIdle`) before sending system events, because CI runs the API 26 emulator slowly.

## 7. Code quality gates

All of these run in CI (`ci.yml`) and are blocking:

- `./gradlew ktlintCheck` (ktlint 1.3.1, Android style). Run `./gradlew ktlintFormat` to fix.
- `./gradlew detekt` (1.23.8, `buildUponDefaultConfig = true` plus `config/detekt/detekt.yml`). A local detekt-cli run must use `--build-upon-default-config` to match CI.
- `./gradlew lintDebug`
- `./gradlew assembleRelease`, so that R8 shrinking is exercised. Add keep rules to `app/proguard-rules.pro`. `src/main/keepRules/rules.keep` is not read by AGP.

Conventions:

- Every source file carries the Apache 2.0 license header.
- Composables are PascalCase, and a screen is split into a `…Route` (ViewModel wiring) and a stateless screen.
- Warnings are not suppressed without a reason next to the suppression, for example `@Suppress("TooGenericExceptionCaught") // Intentional: …`.

## 8. Toolchain and dependency constraints

Versions live only in `gradle/libs.versions.toml`, and every reference goes through a catalog alias. Some pins must not be bumped on their own:

| Pin | Constraint |
|---|---|
| Kotlin 2.0.21 / KSP 2.0.21-1.0.28 | Hilt 2.52 reads Kotlin metadata only up to 2.0. Kotlin 2.1+ needs a Hilt release verified to support it. |
| Room 2.6.1 | Moving to Room 2.7 or 3 goes together with KSP2 and SQLCipher. |
| SQLCipher 4.6.1 + `androidx.sqlite` 2.4.0 | Newer SQLCipher needs compileSdk 37 or Room 3. 4.6.1 supports 16 KB page sizes. |
| `security-crypto` 1.1.0-alpha06 | Deprecated. Replacement tracked as CS-09. |
| AGP 8.7.3, Gradle 8.11.1, JDK 17, compileSdk/targetSdk 35, minSdk 26 | CI and CD use Temurin 17. |

Dependabot proposes weekly Gradle and Actions updates. Each one must pass CI and the security workflow.

## 9. Build and release

- Release signing uses only environment variables set by `cd.yml`. Keystores and `local.properties` are git-ignored and must never be committed.
- `make release VERSION=vX.Y.Z` checks for a clean `main` and pushes an annotated tag. `cd.yml` then does the following:
  1. Runs the unit tests.
  2. Builds a signed AAB and APK.
  3. Verifies the signatures.
  4. Publishes `DenariiDolor-<version>.apk`, `.aab` and `SHA256SUMS.txt` to a GitHub Release.
  5. Keeps the R8 mapping as a private workflow artifact.
- `versionCode` = `MAJOR*1_000_000 + MINOR*1_000 + PATCH`, derived from the tag.

## 10. Known maintainability debt

- There is a single `:app` module. This is acceptable at the current size. Revisit it if build times or ownership boundaries become a problem.
- `LoginActivity` holds its screen state in the activity rather than in a ViewModel.
- `SessionManager` state is in memory only. Process death safely resets it to signed out.
- The pinned toolchain in section 8 blocks newer Kotlin, Room and SQLCipher. Upgrade those together.
- `.idea/` project files are tracked in git.
