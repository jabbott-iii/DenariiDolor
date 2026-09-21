# Denarii Dolor — Work History

Chronological log of work sessions: what changed, why, and what was verified. Newest entries at the bottom. Decisions live in `notes.md`; the roadmap lives in `plan.md`.

## 2026-09-20 — Context files
- Reviewed the codebase; created `notes.md` (architecture, decisions, 20 known gaps) and `plan.md` (requirement status, phases 1–5).
- Recorded answers to the open questions: stored account balances, SQLCipher allowed, PDF + CSV export, no multi-module split.
- Housekeeping: a read-only `git status` from Claude's sandbox left an empty `.git/index.lock`; it was removed. Claude now uses `git --no-optional-locks` for reads.

## 2026-09-20 — Phase 1a: CRUD data + domain layer
**Goal:** add edit/delete for all records and keep stored account balances in sync, with no schema change (DB stays v1).

**Main code**
| File | Change |
|---|---|
| `domain/model/Transaction.kt` | New `open fun accountImpacts(): Map<Long, Double>` (defaults to `accountId → balanceImpact()`). |
| `domain/model/Transfer.kt` | Overrides `accountImpacts()`: source `-amount`, destination `+amount`. |
| `domain/model/Ledger.kt` *(new)* | `Ledger.balanceDeltas(previous, current)` — pure function giving per-account balance changes for add (`null → new`), edit (`old → new`), delete (`old → null`). |
| `domain/model/TransactionEntityMappings.kt` | Added `Transaction.toEntity()` (moved out of `AddTransactionUseCase` for reuse). |
| `data/local/db/dao/TransactionDao.kt` | `getById`, `update`, `deleteById`, `countByCategory`, `countByAccount` (source or transfer destination); expense total now takes `excludeTransactionId`. |
| `data/local/db/dao/AccountDao.kt` | `getById`, `countByName(name, excludeId)`, `rename`, `adjustBalance(id, delta)`, `deleteById`. |
| `data/local/db/dao/CategoryDao.kt` | `countByName` gains `excludeId`; `getById`, `update(id, name, iconName)`, `deleteById`. |
| `data/local/db/dao/BudgetDao.kt` | `deleteByCategoryId`. |
| `data/repository/TransactionRepository.kt` | `update`, `delete`, `getById`, counts. Add/update/delete run inside `database.withTransaction` and apply `Ledger` deltas via `adjustBalance`; a missing account throws and rolls back the whole operation. `update` preserves `createdAtEpochMillis`. |
| `data/repository/{Category,Account,Budget}Repository.kt` | Matching update/delete/getById/duplicate-name methods. |
| `domain/usecase/ValidateTransactionUseCase.kt` | Now also checks category, account, and transfer destination exist; budget check excludes the transaction being edited. Injects `AccountRepository`. |
| `domain/usecase/AddTransactionUseCase.kt` | Uses shared `toEntity()`; forces `id = 0`; repository errors return `Result.failure`. |
| `domain/usecase/UpdateTransactionUseCase.kt` *(new)* | Validate → update; fails on missing ID / unknown transaction. |
| `domain/usecase/DeleteTransactionUseCase.kt` *(new)* | Delete by ID; fails on unknown transaction. |
| `domain/usecase/CategoryUseCases.kt` *(new)* | add / update / delete. Trimmed 1–50 char names, case-insensitive duplicate check, delete blocked for default categories and categories in use. |
| `domain/usecase/AccountUseCases.kt` *(new)* | add (with opening balance) / rename / delete. Same name rules; delete blocked for default accounts and accounts in use (including as a transfer destination). |
| `domain/usecase/BudgetUseCases.kt` *(new)* | set (limit > 0, warning % 1–100, category must exist; keeps existing budget ID) / delete. |
| `util/Validators.kt` | `isValidName`, `isValidPercent`, `isValidBalance`, `MAX_NAME_LENGTH`. |
| `util/ResultExt.kt` *(new)* | `runSuspendCatching` — like `runCatching` but rethrows `CancellationException`. |

**Tests**
- New `test/.../testutil/FakeRepositories.kt` — shared in-memory fakes and `TestData` (replaces per-test anonymous repositories, which broke whenever an interface grew).
- Updated: `ValidateTransactionUseCaseTest`, `GenerateReportUseCaseTest`, `TransactionModelTest`, `ValidatorsTest`.
- New: `LedgerTest`, `TransactionCrudUseCaseTest`, `ManageReferenceDataUseCaseTest`.
- New instrumented test: `androidTest/.../data/TransactionRepositoryImplTest.kt` (in-memory Room: balance sync, transfer, `createdAt` preserved, rollback on missing account).

**Verification status:** ⚠️ not compiled or run yet. Claude's sandbox cannot reach Google Maven or Gradle, so the build must be verified locally:
`./gradlew testDebugUnitTest` and `./gradlew connectedAndroidTest`.

**Caveat:** transactions created before this change never updated account balances. Editing or deleting them will reverse effects that were never applied. Use Settings → wipe data on dev devices before testing.

**Not yet done (Phase 1b):** UI for the transaction list with edit/delete, and management screens for categories, budgets, and accounts.

## 2026-09-20 — Phase 1b: CRUD UI
**Goal:** surface Phase 1a in the app. Per user choice: recent transactions live on the Dashboard; management screens open from Settings.

**New files**
| File | Purpose |
|---|---|
| `presentation/ui/common/FormComponents.kt` | `PickerOption`, `ReferencePicker` (dropdown by ID), `ConfirmDialog`, `FormDialog` (1..n text fields), `ScreenHeader` (title + Back), `ManagedItemRow` (title/subtitle + Edit/Delete). |
| `presentation/ui/common/UiMessage.kt` | `UiMessage` (string resource or raw text), `UiMessage.fromResult()`, `UiMessageEffect` toaster. |
| `presentation/ui/dashboard/DashboardMappers.kt` | Pure `summarize()`, `buildTransactionRows()` (newest first, names resolved, `Cash → Savings` for transfers, `#id` fallback), `formatSignedAmount()`. |
| `presentation/ui/dashboard/DashboardScreen.kt` | Totals, **account balances**, **recent transactions** (20). Tap row → edit; Delete → confirm dialog. Bottom padding clears the FAB. |
| `presentation/ui/transaction/TransactionFormInput.kt` | Prefill model for edit mode; keeps the original timestamp if the date is unchanged. |
| `presentation/ui/category/ManageCategoriesScreen.kt` | List, add, rename, delete (confirm). |
| `presentation/ui/account/AccountViewModel.kt`, `ManageAccountsScreen.kt` | List with balances, add with opening balance, rename, delete (confirm). |
| `presentation/ui/budget/ManageBudgetsScreen.kt` | Every category with its budget; Set Budget dialog (limit + warning %), Remove (only when a budget exists). |

**Modified files**
| File | Change |
|---|---|
| `presentation/ui/AppScreens.kt` | New routes `edit_transaction/{transactionId}`, `manage_categories`, `manage_accounts`, `manage_budgets`. FAB shown only on bottom-tab screens. Old Dashboard composable moved out. Add/Edit share one screen: ID text fields replaced by category/account pickers; edit mode shows header + "Update Transaction". Form resets by re-keying after a save (fixes second identical save not clearing the form). Settings gains three Manage buttons (default no-op params keep old callers compiling). |
| `presentation/ui/transaction/TransactionViewModel.kt` | Add **and** edit (`SavedStateHandle` arg), picker options, `TransactionFormState` (Loading/NotFound/Ready), one-shot `TransactionEvent` channel. `addTransaction` → `saveTransaction`; `STATUS_SAVED` removed. |
| `presentation/ui/dashboard/DashboardViewModel.kt` | `uiState` combines transactions, categories, accounts; `deleteTransaction()`. `DashboardSummary` moved to mappers file. |
| `presentation/ui/category/CategoryViewModel.kt`, `budget/BudgetViewModel.kt` | Replaced empty shells with list state + actions + messages. |
| `domain/usecase/CategoryUseCases.kt` | `update()` now keeps the existing icon unless a new one is passed (rename no longer resets icons). |
| `util/DateUtils.kt` | `formatLocalDate(epochMillis, zoneId)`. |
| `res/values/strings.xml` | ~45 new strings; removed the three unused `*_id_hint` transaction strings. |

**Tests**
- Updated `ComposeScreensTest.addTransactionScreenShowsTransferAccountFieldForTransfers` (new signature; asserts "Savings" instead of ID `2`).
- New instrumented `CrudScreensTest`: dashboard empty state, row click → edit, delete needs confirmation, edit prefill + Update label, edit keeps original timestamp, Settings manage buttons, add-category dialog, budget Remove hidden without a budget.
- New unit tests: `DashboardMappersTest`, `FormMappingTest`, `DateUtilsTest.formatLocalDate`, `ManageReferenceDataUseCaseTest.categoryRenamePreservesIcon`.
- ViewModels are thin; their logic lives in the pure functions above. No `kotlinx-coroutines-test` dependency was added.

**Verification status:** ⚠️ still not compiled or run. Claude's sandbox has no Google Maven access. Run `./gradlew testDebugUnitTest connectedAndroidTest` locally.

**Known limits / follow-ups**
- Manage dialogs close on submit even if validation fails (the error shows as a toast; reopen to retry).
- Search still takes a raw category ID (Phase 3 will switch it to a picker).
- Budget warning threshold is stored and editable but not yet surfaced (Phase 4).

## 2026-09-20 — Phase 2: Security hardening
**User choices:** wipe (not migrate) any plaintext DB; escalating PIN lockout; strong biometrics only. Audit log and screenshot blocking were **not** selected.

| File | Change |
|---|---|
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | Added `net.zetetic:sqlcipher-android:4.6.1` + `androidx.sqlite:sqlite:2.4.0`. Release build: `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-rules.pro`. KSP `room.schemaLocation = app/schemas`. |
| `app/proguard-rules.pro` *(new)* | Keep SQLCipher JNI classes; `-dontwarn` for Tink's optional annotation deps. |
| `data/local/db/security/DatabaseKeys.kt` *(new)* | 256-bit hex key generation/validation, SQLCipher raw-key passphrase (`x'…'`, skips PBKDF2), plaintext-header detection, `shouldDiscard()` rule. |
| `data/local/db/security/DatabaseKeyProvider.kt` *(new)* | Gets or creates the DB key in its own EncryptedSharedPreferences file `db_key_prefs` (not touched by "wipe all data"). |
| `di/DatabaseModule.kt` | Loads SQLCipher, deletes a plaintext or orphaned DB, opens Room through `SupportOpenHelperFactory`. **Removed `fallbackToDestructiveMigration()`**. |
| `data/local/db/AppDatabase.kt` | `exportSchema = true` (schema JSON is generated under `app/schemas/` on build; commit it). |
| `AndroidManifest.xml`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` | `allowBackup=false`; database, sharedpref, and file domains excluded from cloud backup and device transfer. |
| `data/local/preferences/SecurityProfileService.kt` | `attemptPin()` → `PinAttemptResult` (Success / Invalid(attemptsLeft) / LockedOut(remainingMs)); lockout 30s at the 5th failure, doubling to a 15-min cap; state persisted; clock rollback can't shorten it; wrong security answers count too (`RecoverPinResult.LOCKED_OUT`); success, recovery, biometric, and wipe reset it. `verifyPin()` is now `internal`. Store gained `getLong`/`putLong`. |
| `data/local/preferences/EncryptedPreferencesManager.kt` | Exposes `attemptPin`, `recordSuccessfulAuthentication`, `lockoutRemainingMillis`; `verifyPin` removed from the public API; writes now use `commit()` (so a killed process can't drop a failure count). |
| `presentation/ui/auth/LoginActivity.kt` | Uses `attemptPin`; shows attempts left or lockout seconds; clears the PIN field after a failure; recovery shows the lockout; biometric success resets the counter; prompt restricted to strong biometrics. |
| `presentation/ui/auth/BiometricAuthManager.kt` | `BIOMETRIC_STRONG` via shared `ALLOWED_AUTHENTICATORS`. |
| `res/values/strings.xml` | `invalid_pin_attempts_left`, `pin_locked_out`; removed unused `invalid_pin`. |

**Tests**
- `SecurityProfileServiceTest`: store supports longs; 9 new lockout tests (duration curve, lock at 5th failure, correct PIN blocked while locked, doubling, reset on success/biometric/recovery/wipe, clock rollback, shared lockout for security answers).
- New `DatabaseKeysTest` (JVM): key format, raw-key syntax, plaintext detection, discard rules.
- New instrumented `EncryptedDatabaseTest`: DB file has no plaintext header and reopens with the same key; a wrong key cannot read it.

**Verification status:** ⚠️ not compiled or run (no Google Maven access from Claude's sandbox). Please run:
`./gradlew testDebugUnitTest connectedAndroidTest assembleRelease` (the last checks R8 with the new keep rules).

**Behaviour notes**
- First launch after upgrading deletes the old plaintext DB and reseeds defaults. The PIN profile is kept, since it lives in prefs.
- Any future `AppDatabase` version bump now **requires** a `Migration`; the app will crash rather than silently wipe data.
- Key creation and DB open happen on first injection (main thread), as `EncryptedPreferencesManager` already did.

## 2026-09-20 — Phase 3: Reports & Search
**User choices:** Payment Method = the account (no schema change); exports via Save (system file picker) **and** Share.

| File | Change |
|---|---|
| `domain/model/MonthlyReport.kt` | Report now carries `title`, `period: YearMonth`, `generatedAtEpochMillis`, totals, and rows with **Date, Type, Category (name), Description, Amount, Payment Method**. The `csv` field was removed (formatting moved out). |
| `domain/usecase/GenerateReportUseCase.kt` | Takes a `YearMonth`; resolves category and account names; rows in chronological order; transfers show `Source → Destination`; uses the injected `Clock` for the month range and generated timestamp. |
| `domain/report/ReportText.kt` *(new)* | Title constant, period label ("September 2026"), generated label (`yyyy-MM-dd HH:mm z`), file name `spending-report-YYYY-MM.ext`. |
| `domain/report/ReportCsvFormatter.kt` *(new)* | Metadata block + header + rows, CRLF, RFC 4180 quoting, **CSV formula-injection guard** (leading `= + - @` text is prefixed with `'`). |
| `data/export/ReportPdfRenderer.kt` *(new)* | US-Letter PDF via `PdfDocument`: title/period, generated time, totals, header row repeated on each page, ellipsized cells, right-aligned amounts, page numbers. |
| `data/export/ReportExporter.kt` *(new)* | `writeTo(uri)` for Save; `createShareUri()` writes to `cache/reports/` (cleared on each share) and returns a FileProvider URI. |
| `AndroidManifest.xml`, `res/xml/file_paths.xml` *(new)* | Non-exported `FileProvider` (`${applicationId}.fileprovider`) limited to `cache/reports/`. |
| `presentation/ui/report/ReportViewModel.kt`, `ReportScreen.kt` *(new screen file)* | Month navigation, refresh on tab entry, title/timestamp/totals, horizontally scrollable 6-column table, Save CSV/PDF (`CreateDocument`) and Share CSV/PDF (chooser with read grant). |
| `presentation/ui/search/SearchViewModel.kt`, `SearchScreen.kt` *(new screen file)* | Category **picker** with "All categories"; Clear button; inline error text; result count; multi-row results reuse the dashboard row; tap opens Edit. The VM uses `flatMapLatest` over a filters `StateFlow` (old code started a new collector on every search). |
| `presentation/ui/common/TransactionRows.kt` *(new)* | `TransactionRow`, `buildTransactionRows`, `TransactionRowItem` moved here from the dashboard (shared). Tag renamed `TransactionRowTagPrefix`. |
| `util/MoneyFormat.kt` *(new)* | `formatMoney`, `formatSignedAmount` (moved from the dashboard). |
| `presentation/ui/AppScreens.kt` | Old Search/Report composables removed (now ~815 lines); Search results navigate to Edit. |
| `res/values/strings.xml` | Report/search strings added; removed unused `report_summary`, `search_category_id_hint`. |

**Tests**
- Rewrote `GenerateReportUseCaseTest` (fixed clock: title/period/timestamp/totals, chronological rows, names, payment method, fallback).
- New `ReportFormattingTest` (labels, file name, CSV layout/quoting, formula-injection guard).
- `DashboardMappersTest`: imports moved; `formatMoney` check.
- New instrumented `ReportPdfRendererTest` (valid `%PDF`, 1 page when empty, paginates 150 rows via `PdfRenderer`).
- New Compose `ReportSearchScreensTest` (report title/columns/rows/export callbacks, empty state, search count + open result, "All categories" → no category filter). `CrudScreensTest` imports updated.

**Verification status:** ⚠️ not compiled or run by Claude. Run `./gradlew testDebugUnitTest connectedAndroidTest assembleRelease`.

## 2026-09-20 — Phase 4: GUI polish
**User choices:** Vico charts; dashboard summarizes the current month; built-in category icon picker. Charts and colors follow the dataviz method (single-series palette validated in light and dark; status colors always paired with icon + label).

| File | Change |
|---|---|
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | Added Vico **1.14.0** (`compose`, `compose-m3`, `core`; the last release built with Kotlin 1.9) and `material-icons-extended` (BOM-managed; R8 strips unused icons in release). |
| `presentation/ui/common/Composables.kt` | `DenariiDolorTheme` now has light **and** dark color schemes (blue brand, reference surfaces) plus `LocalVizColors` (series 1, meter track, good/warning/critical). |
| `presentation/ui/common/CategoryIcons.kt` *(new)* | 24 curated icons keyed by string (the stored `iconName`); `forKey()` falls back to General; `CategoryIconBadge`. |
| `util/Constants.kt` | Seeded "General Income" / "Transfer" use the `income` / `transfer` icons (new installs only). |
| `presentation/ui/common/TransactionRows.kt` | `TransactionRow.categoryIcon` (defaults to General); rows show an icon badge. |
| `presentation/ui/category/*` | Add/Edit dialog with name + icon grid (selected state exposed to accessibility); list shows icons. VM `rename` → `update(id, name, icon)`. |
| `presentation/ui/dashboard/DashboardMappers.kt` | Pure `spendingByCategory` (expenses, largest first, top 5 + "Other"), `budgetProgress`, `budgetStatus` (OK / WARNING ≥ threshold % / OVER > limit), `DashboardUiState.budgetAlerts`. |
| `presentation/ui/dashboard/DashboardViewModel.kt` | Combines transactions, categories, accounts, budgets; current month from the injected `Clock`. |
| `presentation/ui/dashboard/SpendingChart.kt` *(new)* | Vico column chart: one series in slot-1 blue, rounded 4dp tops, compact $ axis, truncated category labels, zoom off, content description lists the values. |
| `presentation/ui/dashboard/DashboardScreen.kt` | Month label, **hero figure** (net this month), Income/Expense stat tiles, **budget alert banner**, spending chart plus a value list (table view), **budget meters** (status icon + label + % used), balances, recent transactions. |
| `presentation/ui/AppScreens.kt` → + `LoginScreen.kt`, `SettingsScreen.kt`, `TransactionFormScreen.kt` | File split, same package: `AppScreens.kt` is now ~150 lines (navigation only). `AddTransactionRoute` is `internal`. |
| `res/values/strings.xml` | Dashboard/budget strings, `budget_alerts` plurals, category icon strings; removed unused `dashboard_*`, `rename_category`, `account_balance_line`. |

**Tests**
- `DashboardMappersTest`: row icon, status thresholds, top-5 + Other folding, budget progress sorting/sums, compact axis labels.
- New `CategoryIconsTest`: unique keys, fallback, seeded icons are known.
- `CrudScreensTest`: category dialog submits name **and** icon (selection state); dashboard alert banner + "Near limit · 95% used" meter + chart present; banner hidden when on track.

**Verification status:** ⚠️ not compiled or run by Claude. Run `./gradlew testDebugUnitTest connectedAndroidTest assembleRelease`.

**Known limits**
- The Vico chart has no per-bar tooltip (the marker API wasn't used); the value list under the chart carries exact numbers.
- Existing installs keep the old default icon on the seeded Income/Transfer categories; edit them to pick an icon.

## 2026-09-20 — Phase 5: Code quality & scalability
**User choices:** toolchain upgrade deferred to its own step; money → **Long cents**; **ktlint + detekt + jacoco** configured.

**Money & types**
| Area | Change |
|---|---|
| `domain/model/TransactionType.kt` *(new)* | Enum with a lenient `parse()`. Room stores it by name, so the column stays TEXT with the same values. |
| `util/MoneyFormat.kt` | `Money.parseToCents` (BigDecimal; rejects >2 decimals, exponents, out-of-range), `toPlain`, `toInput`, `toDouble`; `formatMoney(cents)` gives `-$1.00` style; `formatSignedAmount(TransactionType, cents)`. |
| Entities / DAOs | `amountCents`, `balanceCents`, `monthlyLimitCents` (Long); `adjustBalance(deltaCents)`; expense total returns Long; search uses `min/maxAmountCents`. |
| `data/local/db/Migrations.kt` *(new)*, `AppDatabase` v2, `DatabaseModule` | **First real migration**: rebuilds `accounts`, `budgets`, `transactions`, converting with `CAST(ROUND(x*100) AS INTEGER)`, then recreates indices. |
| Domain | `Transaction.amountCents`, `type`, `balanceImpact(): Long`, `accountImpacts(): Map<Long, Long>`; Ledger in Long; validation/budget checks in Long; report totals and rows in cents with `TransactionType`. |
| Presentation | Parsing goes through `Money` (form, search, opening balance, budget limit), with an inline "invalid amount" message on the form; dashboard/budget math in integer cents (`spent*100 >= limit*pct`); chart converts cents → dollars only at the edge; strings switched from `$%.2f` to `%s` with `formatMoney`. |

**Tooling**
- Version catalog rewritten: duplicate/unused aliases removed (5× activity-compose, constraintlayout, recyclerview, navigation-fragment/ui, android-library plugin); root build uses catalog aliases.
- `ktlint` (Gradle plugin 12.1.1, ktlint 1.3.1, `android_studio` style via `.editorconfig`, Compose naming allowed) and `detekt` 1.23.6 (`config/detekt/detekt.yml`, Compose-aware overrides).
- JaCoCo via AGP `enableUnitTestCoverage` → `createDebugUnitTestCoverageReport`.
- CI: JDK 17 everywhere; coverage step uses the real task; SonarQube step and dependency-check job removed; ktlint/detekt run for real but are **non-blocking** until the first cleanup pass. security.yml: empty dependency-scanning stub removed; CodeQL builds with `assembleDebug` (so lint findings can't break it).
- `NOTICE` lists third-party attributions (including SQLCipher's BSD-style notice); Apache license header added to all 116 Kotlin files (CONTRIBUTING.md requirement).

**Tests**
- All unit and instrumented tests converted to cents/enum.
- New: `MoneyTest`, `TransactionModelTest` (type + parse), `LedgerTest.centsAvoidFloatingPointDrift`, `ValidateTransactionUseCaseTest.expenseExactlyAtLimitIsAllowed`.
- New ViewModel tests with `kotlinx-coroutines-test` + `MainDispatcherRule`: `DashboardViewModelTest`, `TransactionViewModelTest`.
- New instrumented: `MigrationTest` (v1→v2 values, FK check), `TransactionDaoTest` (search filters, expense total exclusion, transfer-destination count).
- Fixed a pre-existing compile error: `import androidx.compose.ui.test.assertDoesNotExist` (a member function, not importable) in `ComposeScreensTest` and `CrudScreensTest`.

**Independent review:** a separate agent reviewed every file as a "compiler". It found the import error above (fixed) and the schema-file issue below; the migration schema, symbols, strings, catalog aliases, and all test arithmetic checked out.

**Before first run**
1. Build once, then **commit `app/schemas/.../2.json`**. `MigrationTest` reads it from test assets, and a clean CI checkout may not have it otherwise.
2. `./gradlew ktlintFormat` once, then review `./gradlew detekt` (or `detektBaseline`) before making those CI steps blocking.
3. Verify: `./gradlew testDebugUnitTest createDebugUnitTestCoverageReport connectedAndroidTest assembleRelease`.

## 2026-09-20 — CI/CD rebuild + release Makefile
**User choices:** Android only (no iOS target exists in this repo; iOS would need a Kotlin Multiplatform port); signed builds go to a **GitHub Release** (no Play upload).

| File | Now does |
|---|---|
| `.github/workflows/security.yml` | CodeQL `security-extended` for Kotlin (manual `assembleDebug` build); Gradle dependency graph submission on push/schedule (feeds Dependabot alerts); dependency review on PRs (fails on high severity); gitleaks secret scan. Gradle wrapper validation comes built into `setup-gradle@v6`. Weekly schedule; least-privilege permissions per job. |
| `.github/workflows/ci.yml` | **verify**: ktlint → detekt → Android lint → unit tests → JaCoCo → R8 `assembleRelease` (all blocking), reports uploaded. **instrumented**: emulator with KVM, API 26 (minSdk) and 34 (targetSdk), `connectedDebugAndroidTest`. Concurrency cancels superseded runs. Sonar, PR-comment and duplicate steps removed. |
| `.github/workflows/cd.yml` | On `v*` tag: validate tag → `versionName`/`versionCode` (`MAJOR*1e6+MINOR*1e3+PATCH`) → unit tests → restore keystore from secret → Gradle-signed **AAB + APK** → `apksigner`/`jarsigner` verification → SHA256SUMS → GitHub Release (auto notes; `-rc` tags marked prerelease). R8 mapping kept as a private workflow artifact (90 days), not published. Keystore deleted in `always()`. Runs in the `production` environment. |
| `app/build.gradle.kts` | Release `signingConfig` built only from env vars (`ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`), so local/CI builds without them stay unsigned; `-PversionCode` / `-PversionName` overrides. |
| `.gitignore` | Ignores `*.jks`, `*.keystore`, `service-account*.json`. |
| `Makefile` | Verified that the tag push triggers CD. Version regex now matches cd.yml (no `+build`, parts ≤ 999); `release` also checks for a clean tree, being on `main`, and that `main` matches `origin/main`; pushes `refs/tags/<v>` explicitly and prints the Actions URL. |

**Required repository secrets:** `ANDROID_SIGNING_KEY` (base64 of the .jks), `ANDROID_SIGNING_KEYSTORE_PASSWORD`, `ANDROID_SIGNING_KEY_ALIAS`, `ANDROID_SIGNING_KEY_PASSWORD`. Optional: add reviewers to the `production` environment.
**Notes:** ktlint/detekt are now **blocking**, so run `./gradlew ktlintFormat` and fix or baseline detekt before pushing. Dependency review needs the dependency graph enabled (private repos need GitHub Advanced Security). gitleaks needs `GITLEAKS_LICENSE` only for organization-owned repos.

## 2026-09-20 — Phase 6: Toolchain upgrade
Aligned to the toolchain Vico 2.0.0 is built with, minus Kotlin 2.1: Hilt 2.52's metadata reader supports Kotlin ≤ 2.0, and no Hilt release was verified for 2.1, so **Kotlin 2.0.21** was chosen (it can still read Vico's 2.1 binaries).

| Item | From → To |
|---|---|
| Gradle wrapper | 8.7 → **8.11.1** (sha256 updated in `gradle-wrapper.properties`) |
| AGP | 8.5.2 → **8.7.3** |
| Kotlin / KSP | 1.9.24 / 1.9.24-1.0.20 → **2.0.21 / 2.0.21-1.0.28** |
| Compose compiler | `composeOptions` 1.5.14 → **`org.jetbrains.kotlin.plugin.compose`** (versioned with Kotlin) |
| Compose BOM | 2024.09.03 → **2024.12.01** (material3 1.3.1) |
| compileSdk / targetSdk | 34 → **35** |
| AndroidX | activity 1.9.3, core-ktx 1.15.0, lifecycle 2.8.7, navigation 2.8.5 |
| Vico | 1.14.0 → **2.0.0** |
| detekt | 1.23.6 → 1.23.8 |
| SQLCipher | **kept 4.6.1**: already 16 KB page-size compatible (Zetetic); newer lines target compileSdk 37 / Room 3 |
| Room / Hilt | kept 2.6.1 / 2.52 |

**Code changes**
- `dashboard/SpendingChart.kt` rewritten for Vico 2 (`CartesianChartHost` + `rememberColumnCartesianLayer`, `CartesianValueFormatter`, `CorneredShape.rounded`, `rememberM3VicoTheme`, scroll/zoom off, static `CartesianChartModel`). Returns early for empty data (Vico throws on empty series); x labels never empty (Vico requires it). Public API, tag and colors unchanged.
- targetSdk 35 enforces **edge-to-edge**: `enableEdgeToEdge()` in both activities; login content uses `safeDrawingPadding()`; `NavHost` consumes the Scaffold insets; Add/Edit form uses `imePadding()` so the keyboard doesn't cover fields.
- `menuAnchor()` → `menuAnchor(MenuAnchorType.PrimaryNotEditable)` (deprecated overload removed from our code).
- CI instrumented matrix: API 26 + **35**.

**Verification status:** ⚠️ not compiled. The Vico 2 API was verified against the v2.0.0 source by a separate agent. First sync will download Gradle 8.11.1; if the checksum doesn't match, re-run `./gradlew wrapper --gradle-version 8.11.1`. Then run `./gradlew ktlintFormat detekt testDebugUnitTest connectedAndroidTest assembleRelease` and check the login, dashboard and add-transaction screens on an API 35 device for inset/keyboard layout.
**Future:** Kotlin 2.1+ needs a Hilt release that supports Kotlin 2.1 metadata; SQLCipher 4.1x with compileSdk 37 / Room 3.

## 2026-09-21 — Settings: remove Recover PIN, add dark mode switch
| File | Change |
|---|---|
| `presentation/ui/SettingsScreen.kt` | Removed the **Recover PIN** button and its `onResetSecurityProfile` callback. Added a **Dark mode** row (`Switch`; the whole row is a `toggleable` with `Role.Switch`; tag `DarkModeSwitchTag`). |
| `presentation/ui/settings/SettingsScreenState.kt` | New `darkMode: Boolean`. |
| `data/local/preferences/ThemePreferences.kt` *(new)* | `darkModeOverride: StateFlow<Boolean?>`, where `null` = follow the device; stored in plain app-private prefs `ui_prefs` (not sensitive; backups already excluded). |
| `presentation/ui/common/ThemedContent.kt` *(new)* | `ThemePreferences.isDarkTheme()` (override ?: system) and `ComponentActivity.setThemedContent()`, which wraps content in `DenariiDolorTheme(darkTheme)` and re-applies `enableEdgeToEdge` so status/nav bar icons match the chosen theme. |
| `MainActivity.kt` | Uses `setThemedContent`; passes `darkMode` + `themePreferences::setDarkMode` down; `redirectToLogin()` no longer carries a recovery flag. |
| `presentation/ui/AppScreens.kt` | `MainActivityContent(settingsState, onSignOut, onDarkModeChange)` replaces `onSettingsAction(startPinRecovery)`. |
| `presentation/ui/auth/LoginActivity.kt` | Uses `setThemedContent` (login follows the same theme); removed `EXTRA_START_RECOVERY` / auto-start recovery. "Forgot PIN?" on the login screen is unchanged. |
| `res/values/strings.xml` | `settings_recover_pin` → `settings_dark_mode`. |
| `CrudScreensTest` | Settings test updated; new `settingsHasNoRecoverPinAndTogglesDarkMode` (no Recover PIN; switch off → on → off updates state). |

**Verification status:** ⚠️ not compiled. Check manually that toggling in Settings switches the whole app, the choice survives a restart, and the login screen matches.

## 2026-09-21 — Back button on Add Transaction
- `presentation/ui/TransactionFormScreen.kt`: the header (title + **Back**) now shows in add mode too. Its title is "Add Transaction" or "Edit Transaction" by mode. Back always calls `onFinished` → `navController.popBackStack()`, returning to whichever screen the **+** button was pressed on (Dashboard, Search, Reports or Settings). System back already did this. After a save, add mode still stays on a cleared form so several entries can be made in a row.
- `CrudScreensTest.addModeShowsHeaderWithBackThatReturns` added.
- ⚠️ Not compiled here; verify on device from each tab.

## 2026-09-21 — CI fix: ktlint (and detekt) failures
CI run failed at **ktlint** (`ktlintAndroidTestSourceSetCheck`, `ktlintTestSourceSetCheck`: import order, argument wrapping, lines > 140). Fixed in the project using the same **ktlint 1.3.1** and **detekt 1.23.8** versions as CI, run against the repo's `.editorconfig` and `config/detekt/detekt.yml`. Both now report **0 findings** for main, test and androidTest.

- `ktlint --format` over all Kotlin sources (formatting only, 70+ files).
- Test-tag constants renamed to SCREAMING_SNAKE_CASE (ktlint `property-naming`), e.g. `SpendingChartTag` → `SPENDING_CHART_TAG`, `TransactionRowTagPrefix` → `TRANSACTION_ROW_TAG_PREFIX` (16 constants, all usages updated).
- `Validators.isValidSearchRange` long line wrapped.
- detekt (the next CI step) fixes:
  - `LoginActivity`: wipe flow split into `wipeStorage()` / `resetCacheDir()` (was complexity 17); `MILLIS_PER_SECOND` constant.
  - `ReportCsvFormatter.escape`: named `looksLikeFormula` + `QUOTE_TRIGGERS` set (same behaviour).
  - `SearchFilterParser`: `throw IllegalArgumentException` → `require(...)` (same exception type and messages).
  - Named constants replace magic numbers (`THOUSAND`/`MILLION`, `PERCENT`, `CENTS_PER_UNIT`, `MAX_PERCENT`); `ReportPdfRenderer` gets a documented `@Suppress("MagicNumber")` for layout coordinates.
  - `runSuspendCatching`: documented `@Suppress("TooGenericExceptionCaught")` (catch-all by design).
  - Spread operators removed (`toMutableStateList()`, `addMigrations(MIGRATION_1_2)`; `ALL_MIGRATIONS` removed).
  - Unused `transferAccountId` parameter removed from `applyTransactionTypeDefaults` (and its test calls).
  - File/declaration names: `util/MoneyFormat.kt` → `util/Money.kt`; `VizColors` moved to `common/VizColors.kt`.
  - `detekt.yml`: `ReturnCount.excludeGuardClauses`, `TooManyFunctions.ignorePrivate/ignoreOverridden`; the PascalCase constant exception was removed (no longer needed).

**Not verifiable here:** Android lint, compilation, and tests (no Google Maven access). Push and let CI run the remaining steps.
