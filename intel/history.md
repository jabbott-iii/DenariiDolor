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

## 2026-09-21 — CI fix: `lintDebug` dependency resolution
- **Error:** `Could not find androidx.compose.ui:ui-test-junit4:` (empty version) while resolving `debugAndroidTestRuntimeClasspath`. The configuration-cache message in the log is a side effect of that failure.
- **Cause:** the Compose BOM was applied only to `implementation`; `androidTestImplementation` Compose artifacts had no version.
- **Fix:** `androidTestImplementation(platform(libs.androidx.compose.bom))` in `app/build.gradle.kts`. (`debugImplementation` already inherits from `implementation`.)
- **Also:** `local.properties` (machine-specific `sdk.dir`) was committed, which produced the "sdk.dir … does not exist" warning on CI. It's now in `.gitignore`; untrack it with `git rm --cached local.properties`.

## 2026-09-21 — CI fix: instrumented test failures (API 26 emulator, 4 of 39)
| Failure | Cause | Fix |
|---|---|---|
| `EncryptedDatabaseTest > initializationError` | `@Test fun … = runBlocking { … }` whose last expression returned `AppDatabase`, so the method wasn't `void` and JUnit rejected the whole class. | All 30 `= runBlocking {` test bodies (unit + instrumented) → `runBlocking<Unit>`. |
| `CrudScreensTest.dashboardShowsBudgetAlertAndMeterStatus` ("Near limit · 95% used" not found) | **Real bug:** `(0.95f * 100).toInt()` = 94 from float error, so the app showed "94%". The meter was also below the fold of a `LazyColumn`, so it wasn't composed. | New `BudgetProgress.percentUsed` (integer math) used by the meter label; dashboard list tagged `DASHBOARD_LIST_TAG`; tests `performScrollToNode` before asserting. Unit test `percentUsedUsesIntegerMath` added. |
| `ComposeScreensTest.loginScreenPrimaryActionLabelMatchesMode` ("Cannot call setContent twice") | Pre-existing test called `setContent` once per mode. | One `setContent` driven by a `mutableStateOf(mode)`. |
| `ComposeScreensTest.loginScreenWipeDialogDismissRequestTriggersCancelCallback` | Pre-existing test pressed back on the activity's dispatcher, which never reaches the `AlertDialog` window. | `Espresso.pressBack()` (key-level back to the focused dialog window). |

ktlint 1.3.1 and detekt 1.23.8 re-run on the result: 0 findings. Instrumented tests can't run here; re-run CI.

## 2026-09-21 — CI fix: wipe-dialog back-press test (39/40 → 40/40 expected)
- `ComposeScreensTest.loginScreenWipeDialogDismissRequestTriggersCancelCallback` failed with `RootViewWithoutFocusException`: `Espresso.pressBack()` targeted the activity root, which had no focus because the dialog had it.
- Fix: `UiDevice.getInstance(instrumentation).pressBack()` (UiAutomator) injects a real system back key into the focused window, the dialog, which calls `onDismissRequest` → `onCancelWipeData`.
- Added `androidx.test.uiautomator:uiautomator:2.3.0` (catalog alias `androidx-uiautomator`) as `androidTestImplementation`.

## 2026-09-21 — Fix: "Back to Sign In" on the Forgot PIN screen
- **Bug:** `LoginActivity.switchToSignIn()` called `refreshLoginState()`, whose default `preserveRecoveryMode = true` kept the screen in `RECOVER_PIN`, so the button did nothing visible.
- **Fix:** `refreshLoginState(preserveRecoveryMode = false)`.
- **Also:** `LoginScreen` adds a `BackHandler` in recovery mode, so the system back gesture returns to sign-in too (it previously closed the app).
- **Test:** `ComposeScreensTest.recoveryModeBackButtonAndSystemBackReturnToSignIn` (button and system back both return to sign-in). ktlint/detekt still clean.

## 2026-09-21 — Documentation: design document and user guides
- `diagram.md`: design document containing:
  - requirements mapping;
  - architecture diagram;
  - class diagrams (domain inheritance, Room entities, repositories/use cases, security, ViewModels);
  - design diagrams (navigation, save-transaction sequence, sign-in/lockout state machine, data protection, report export);
  - key decisions, schema history, and testing strategy.
- `guide-maint.md`: maintainer guide covering:
  - prerequisites, build/run, and project structure;
  - test, lint, and coverage commands; conventions;
  - DB migration procedure; security controls;
  - CI/CD workflows, signing secrets, and `make release`;
  - dependency upgrades and troubleshooting.
- `guide-use.md`: end-user guide covering:
  - install and first-launch setup;
  - sign-in, biometrics, lockout, Forgot PIN, and wipe;
  - Dashboard, transactions, Search, Reports/export, and Settings;
  - privacy and FAQ.
- Mermaid diagrams were checked against the grammar by hand, not rendered (no Mermaid CLI available here). Preview them on GitHub or in the Android Studio Markdown preview.

## 2026-09-21 — Documentation exported to PDF
- Rendered `diagram.md`, `guide-maint.md` and `guide-use.md` to `diagram.pdf`, `guide-maint.pdf` and `guide-use.pdf` in the project root (US Letter, page numbers in the footer).
- All 11 Mermaid diagrams were rendered with mermaid-cli, so they are now syntax-validated. They are embedded as vector SVG, so you can zoom in without losing detail. Wide diagrams (architecture, 4.3, 4.4, 5.1, 5.2, 5.4, 5.5) sit on their own landscape pages.
- Pipeline: `mmdc` (Markdown → SVG) → pandoc (HTML) → headless Chromium (PDF). The Markdown files remain the source of truth; regenerate the PDFs after editing them.
- Fixed a stale copy of `guide-use.md`: the date fields use a calendar picker, not typed `YYYY-MM-DD`.

## 2026-09-21 — README rewrite
- `README.md` rewritten with:
  - CI, security and license badges;
  - an overview, a feature table and use cases;
  - install steps;
  - a condensed user guide (first launch, sign-in and lockout, navigation, transactions, budgets/categories/accounts, reports);
  - worked examples (a sample month, a search, and the exact CSV export format from `ReportCsvFormatter`);
  - a developer section (stack, architecture diagram, commands, layout, CI/CD, signing secrets), security, and license.
- The standalone docs (`diagram.md`, the guides) are no longer in the repo, so the README is self-contained and doesn't link to them.

## 2026-09-21 — Unit test plan, scripts, results and change summary
- **Test plan focus:** transaction validation (`ValidateTransactionUseCase` + `Validators` + `Money.parseToCents`). 20 cases, TC-01 – TC-20.
- **Gap analysis:** 6 of the 11 `failure(...)` branches had no test. Added 9 tests to `ValidateTransactionUseCaseTest.kt`:
  - invalid amount, blank description, invalid date, invalid IDs;
  - transfer without a destination; a valid transfer;
  - income in a budgeted category; a category with no budget;
  - a repository error becoming `Result.failure`.
  
  The class went from 6 to 15 tests, and the unit suite from 107 to 116. ktlint and detekt report 0 findings.
- **Local run** (2026-09-21 11:33, `./gradlew testDebugUnitTest createDebugUnitTestCoverageReport`): **116/116 passed**, 0 failures. `ValidateTransactionUseCase` coverage: 97.4 % of lines, 94.4 % of branches. The only gaps are a compiler artifact, a coroutine-resume branch, and an unreachable guard on line 56. No production code changes were needed.
- **Deliverables** in `testing/`: `test-plan.pdf`, `test-scripts.pdf`, `test-results.pdf`, `test-changes.pdf`. The screenshots in them come from the real Gradle and JaCoCo HTML reports, rendered in headless Chromium, plus source and diff views. The Mermaid diagrams are rendered to SVG.

## 2026-09-21 — CI fix: detekt `UseCheckOrError` in the new validation test
- **CI:** `./gradlew detekt` failed with `ValidateTransactionUseCaseTest.kt:152:71: Use check() or error() instead of throwing an IllegalStateException. [UseCheckOrError]`.
- **Fix:** the failing fake in TC-15 now calls `error("database closed")` instead of `throw IllegalStateException("database closed")`. `error()` throws the same exception type with the same message, so the test's behaviour and assertions are unchanged.
- **Root cause of the miss:** my local detekt-cli check ran without `--build-upon-default-config`, but Gradle's `detekt { buildUponDefaultConfig = true }` enables the default rule set. With that flag, the local CLI reproduces the CI finding on the old code and reports 0 findings after the fix. ktlint is still clean.
- Testing PDFs regenerated to show the corrected line and this finding (`test-changes.pdf` #12).

## 2026-09-21 — CI fix: flaky `recoveryModeBackButtonAndSystemBackReturnToSignIn` (API 26)
- **Failure:** `NullPointerException: Cannot run onActivity since Activity has been destroyed already` (40/41 passed).
- **Cause:** a test timing race, not an app bug. The test clicked **Forgot PIN?** and immediately sent a system back press with `UiDevice.pressBack()`. UiAutomator doesn't wait for Compose, so on the slow API 26 emulator the back press arrived before recovery mode had recomposed. The `BackHandler` wasn't enabled yet, so the press reached the activity and finished it.
- **Fix (test only):**
  - Wait for the recovery screen (`Back to Sign In` visible) and call `waitForIdle()`.
  - Then send the back press through the activity's `onBackPressedDispatcher` on the UI thread. That is the dispatcher `BackHandler` registers with, so the result is deterministic.
  - The wipe-dialog test keeps `UiDevice.pressBack()`, because dialogs need a real key event.
- ktlint and detekt (with the default rule set, as Gradle runs it) report 0 findings. Re-run CI to confirm 41/41.

## 2026-09-21 — `intel/` directory and security log
- Created `intel/` at the repo root and moved the context files into it: `notes.md`, `plan.md`, `history.md`. Added `intel/cysec.md`, which is updated continuously with security findings: open, fixed or accepted, with stable `CS-NN` IDs.
- Security review of the manifest, auth, session, storage, exports, build and CI. Fixes, left uncommitted:
  - **CS-01** `SessionManager` now uses `SystemClock.elapsedRealtime()`. Setting the wall clock back could previously keep a session alive forever.
  - **CS-02** `MainActivity.onCreate` redirects to sign-in before `setContent` when there is no session. This covers the task being restored after process death.
  - **CS-03** `persist-credentials: false` on all 7 `actions/checkout` steps, so `GITHUB_TOKEN` no longer sits in `.git/config` during write-permission jobs.
  - **CS-04** `ReportExporter.clearShareCache()` runs on sign-out, on timeout and on every `LoginActivity` start.
  - **CS-05** `.github/dependabot.yml` (github-actions + gradle, weekly).
- Open: CS-06 (FLAG_SECURE) and CS-10 (6-digit PIN minimum) need your decision; CS-07 (biometric `CryptoObject`), CS-08 (SHA-pinned actions), CS-09 (deprecated `security-crypto`), CS-11 (tapjacking) and CS-12 (audit log) are planned.
- ktlint and detekt report 0 findings, and the YAML is valid. Not compiled here; CI will verify.

## 2026-09-21 — Security decisions: CS-06 declined, CS-10 applied
- **CS-06 (FLAG_SECURE):** declined, because this is a productivity app and not a finance app. Recorded as accepted risk **CS-A4** in `cysec.md`.
- **CS-10 (PIN length):** PINs must now be **6–12 digits** (`^[0-9]{6,12}$`) in `SecurityProfileService` (setup and reset) and `LoginActivity` (sign-in). The error message is now "PIN must be 6 to 12 digits." As you chose, existing 4–5-digit PINs no longer work; those users reset through **Forgot PIN?**.
- Tests: `SecurityProfileServiceTest` now uses 6-digit PINs, and there is a new `pinMustBeSixToTwelveDigits` test, so the unit suite has 117 tests. `ComposeScreensTest` sample PIN is now 6 digits. `README.md` says 6–12 digits.
- ktlint and detekt (with the default rule set) report 0 findings. Not compiled here; CI will verify.

## 2026-09-22 — Security log renamed to `intel/cybersec.md`
- Renamed `intel/cysec.md` to `intel/cybersec.md`, the name `AGENTS.md` requires. Content unchanged apart from the title line.
- Updated the current references in `notes.md` (intro, conventions, new decision-log row) and `plan.md`. Earlier dated entries in `notes.md` and this file still say `cysec.md` and are left as written.

## 2026-09-22 — `intel/maint.md`, `intel/map.md`, security-log format, doc fixes
- **New `intel/maint.md`** (authoritative architecture guide), built from the current code, build files and workflows. It covers:
  - layers and dependency rules, and the domain invariants (cents, atomic `Ledger` balance updates, validation before writes, protected defaults, `Result`/`runSuspendCatching`);
  - the migration procedure, the security-sensitive files, the testing split, the quality gates;
  - the pinned toolchain and why each pin exists, the release flow, and known maintainability debt.
  - It records that `transactions.transferAccountId` has no foreign key; account deletion relies on `AccountUseCases` counting transfer references.
- **New `intel/map.md`:** repository layout, package map, CI/CD table, and four Mermaid diagrams (layer dependencies, sign-in and session, saving a transaction, schema v2). All four were rendered with mermaid-cli to check the syntax.
- **`intel/cybersec.md`** aligned with the item format in `AGENTS.md`:
  - every open and remediated item has a Validation entry, and statuses use `Open`, `In Progress`, `Blocked` or `Closed`;
  - "Fixed on 2026-09-21" is now "Remediated on 2026-09-21". CS-01, CS-02, CS-04, CS-05 and CS-10 are `In Progress` until their validation is recorded;
  - **CS-03 closed** after a static check: 7 `actions/checkout` steps, 7 `persist-credentials: false`, and no `git push` or `git commit` in any workflow;
  - the update procedure (section 5) now describes these statuses.
- **`intel/notes.md`:** corrected stale facts. Biometrics are `BIOMETRIC_STRONG` and PINs 6–12 digits; the package layout now lists all use cases, `domain/report`, `data/export`, the split screens and `util`; the CI, security and CD descriptions match the workflows (no Play upload); gap #16 is marked resolved. It also points to `maint.md` and `map.md`.
- **`intel/plan.md`:** added a task to run the remaining `cybersec.md` validations.
- **`README.md`:**
  - Fixed the Dining budget example, which read "*Near limit · 64% used*… no, *On track*". It now reads *On track · 64% used* and says the meter changes to *Near limit* at $40.
  - The checksum step now names the real release asset, `DenariiDolor-<version>.apk`, rather than `app-release.apk`.
- Documentation only; no code, build or CI changes. GitHub was not reachable from this session, so CI results were not checked.

## 2026-09-22 — `CONTRIBUTING.md` expanded
- Kept the four existing contribution rules word for word, and added:
  - a Code of Conduct link and the private security-advisory route;
  - setup (JDK 17 toolchain and the foojay resolver, a JDK 21 Gradle daemon, Android SDK platform 35, `local.properties`, a device on API 26+);
  - the workflow, the full validation command list matching `ci.yml`, coding expectations summarized from `intel/maint.md`, and pull-request expectations.
- Documentation only. The commands were taken from the workflows and the Gradle configuration, not run here.

## 2026-10-03 — Bug and security review; remediation plan
- Reviewed every `app/src/main` source file, the Gradle build, the three workflows and the repository contents, following `AGENTS.md`. No app, build or CI changes were made.
- **Baseline:**
  - `./gradlew testDebugUnitTest --offline` passed 117/117.
  - CI on `4abfc8e` (2026-09-27) was green, including the instrumented tests on API 26 and 35.
  - CD published v1.0.1.
- **Reproduced against the compiled classes** with a throwaway jshell probe (not added to the repo):
  - `Money.parseToCents("12,50")` returns 125 000 cents ($1,250.00). Logged as BUG-01.
  - Moving the injected wall clock forward skipped 240 minutes of lockout: all 25 wrong PINs were checked, and the correct PIN still worked. Logged as CS-13.
- **`cybersec.md`:**
  - Added CS-13 – CS-22, and pointed CS-07 and CS-09 to CS-15.
  - Closed CS-05, because Dependabot opened PRs #12–#17.
  - Closed CS-10 on CI run 36357715389.
  - CS-01 has its CI evidence but still needs the manual clock check.
- **`plan.md`:** new Phase 7 with BUG-01 – BUG-12, the order of work and the decisions needed. The signing-secrets task is marked done, because CD signed v1.0.1.
- **Also found:**
  - A release-signed APK committed in `908ec5c` under `app/release/`. Logged as CS-18.
  - Dependabot's security update for the build-time `netty`, `protobuf-java`, `commons-io` and `logback-core` failed on 2026-09-27. None of them is in `releaseRuntimeClasspath`. Logged as CS-22.

## 2026-10-03 — Phase 7 remediation; `plan.md` trimmed to open work
- **Reconciled with `9f967c6`.** That commit landed after the review entry above. Besides the review, it implemented most of the security work, but only `maint.md` and `map.md` were updated with it:
  - the vault: the database key wrapped by the PIN, the answer and biometrics; `DatabaseHolder` opening the database only after sign-in (CS-15, CS-07);
  - Keystore AES-GCM storage instead of `security-crypto`, with a retry-or-wipe screen for Keystore failures (CS-09, BUG-05);
  - the elapsed-time lockout (CS-13), stricter recovery answers (CS-14, option B), wipe friction (CS-16) and crypto-erase (CS-17);
  - decimal-comma parsing (BUG-01, accepting locale decimal separators), vault work off the main thread with serialized attempts (BUG-04), and removal of the plaintext `key_pin` path (BUG-12).
  
  `cybersec.md` and this entry now record all of it.
- **Bugs fixed in this session** (uncommitted). The bug table has been removed from `plan.md`, so each fix is recorded here:
  - **BUG-02 (double save):** `TransactionViewModel.isSaving` ignores a save while one is in flight. After a successful edit it stays set while the screen closes, and the Save button is disabled meanwhile. `ReportViewModel` runs one export at a time. The manage dialogs are unchanged: they close on the first tap, and a duplicate fails on the unique name. Tests:
    - `TransactionViewModelTest.saveWhileOneIsInFlightIsIgnored`, `successfulEditKeepsSaveDisabledWhileTheScreenCloses` and `failedSaveCanBeRetried`;
    - `CrudScreensTest.saveButtonIsDisabledWhileSaving`.
  - **BUG-03 (double pop):** every `onFinished`/`onBack` goes through `NavController.popBackOnce()` (`dropUnlessResumed`). The edit screen's "not found" close waits for the resumed state (`LifecycleResumeEffect`). Test: `NavigationTest.repeatedBackPopsOnlyOnce`.
  - **BUG-06 (malformed transfer):** read paths use `toDomainTransactionOrNull()`, so a TRANSFER row without a destination has no balance impact. Editing or deleting such a row reverses nothing, and its row shows `Account → ?`. A Dashboard load error now shows an error message instead of crashing. Enforcing this in the database is deferred to the next migration (`plan.md`). Tests:
    - `DashboardMappersTest.malformedTransferHasNoImpactAndIsFlagged`;
    - `TransactionEntityMappingsTest.readPathMappingSkipsMalformedTransfer`;
    - `DashboardViewModelTest.loadErrorShowsErrorStateInsteadOfCrashing`;
    - `TransactionRepositoryImplTest.malformedTransferCanBeDeletedWithoutTouchingBalances`.
  - **BUG-07 (LIKE wildcards):** `escapeLike()` escapes `\`, `%` and `_`, and the search query uses `ESCAPE '\'`. Tests: `EscapeLikeTest` and `TransactionDaoTest.searchMatchesWildcardsLiterally`.
  - **BUG-08 (description length):** descriptions are trimmed when written (`Transaction.toEntity()`), capped at 200 characters (`Validators.MAX_DESCRIPTION_LENGTH`), and the field stops at 200. Tests:
    - `ValidatorsTest.descriptionIsTrimmedAndCapped`;
    - `ValidateTransactionUseCaseTest.descriptionOverTheLimitFails`;
    - `TransactionCrudUseCaseTest.addAndUpdateTrimTheDescription`.
  - **BUG-09 (time zone):** `AppModule` now provides `DeviceClock`, whose zone follows the device's current zone. `ValidateTransactionUseCase` computes budget months in the injected clock's zone; it used `ZoneId.systemDefault()` twice. Tests: `DeviceClockTest` and `ValidateTransactionUseCaseTest.budgetMonthFollowsTheClockZone`.
  - **BUG-10 (Dashboard loads):** the Dashboard loads only the current month (`observeByDateRange`) and the 20 latest rows (`observeRecent`). It recomputes the month and zone on resume. The unused transaction `getAll()` and `getByDateRange()` were removed; reports read `observeByDateRange(...).first()`. Tests:
    - `DashboardViewModelTest.refreshPeriodRollsOverToTheNewMonth`;
    - `TransactionDaoTest.dashboardQueriesReturnTheMonthAndTheLatestRows`.
  - **BUG-11 (raw error text):** use cases throw `DomainException` with a `DomainError`, and `UiMessage.fromError()` maps it to a string resource. `NoSuchElementException` maps to "This item no longer exists", and anything else to the generic message. Exception text is never shown. Tests: `UiMessageTest`, plus the updated `TransactionViewModelTest.invalidTransferEmitsFailure`.
- **Security fixes in this session** (details in `cybersec.md` section 2):
  - CS-08: every action SHA-pinned, and all jobs on `ubuntu-24.04`;
  - CS-11: overlay hiding / obscured-touch filtering on the sign-in window;
  - CS-18: `app/release/` untracked (staged) and build outputs ignored;
  - CS-19: CD split into `ci-gate` → `build` → `publish`;
  - CS-20: CSV guard after whitespace, `;` and tabs;
  - CS-21: empty `taskAffinity` on both activities;
  - CS-22: build-time constraints for netty, protobuf-java, commons-io and logback, the duplicate Google repository removed, and PRs #12 – #17 triaged.
- **CI fix:** CI run 37161706133 on `9f967c6` failed on API 26 in `ComposeScreensTest.loginScreenWipeDialogDismissRequestTriggersCancelCallback`; API 35 passed 58/58. This is the race recorded on 2026-09-21: UiAutomator pressed Back before the now heavier wipe dialog was on screen. The test now waits for the dialog window (`Until.hasObject`) and for idle before `pressBack()`. Test-only change.
- **Device checks on the API 37 emulator** (read-only AVD, debug build):
  - CS-01: with the clock set back an hour (`cmd alarm set-time`), the app signed out after 5 minutes without input.
  - CS-02: after `am kill` in the background, restoring from Recents showed only the sign-in screen; `MainActivity` never drew.
  - CS-04: `cache/reports` was gone after sign-out, and again after a timeout.
  - CS-11: `HIDE_NON_SYSTEM_OVERLAY_WINDOWS` set on the sign-in window.
  - CS-21: `taskAffinity=null`, and one task through sign-in, sign-out and timeout.
  - BUG-02 / BUG-03: a simultaneous double tap on Save saved one row, and a double tap on Back stayed on the Dashboard.
  - Phase 6: edge-to-edge insets are correct (content clears the status bar, and the navigation bar sits above the gesture bar).
- **Validation run:**
  - `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleRelease`: passed, with 149/149 unit tests (18 new).
  - `connectedDebugAndroidTest` on the API 37 emulator: the 29 non-UI tests passed, including the 3 new DAO and repository tests and all of `VaultTest` (the v1.0.x upgrade tests among them). The 35 Compose tests can't run on API 37 with Espresso 3.6.1 (`NoSuchMethodException: InputManager.getInstance`), so they're left to CI on API 26 and 35. That includes the other 3 new instrumented tests (`NavigationTest`, `CrudScreensTest.saveButtonIsDisabledWhileSaving`, and `OverlayGuardTest`, which runs only below API 31).
  - Not run: CD, Dependabot alerts (HTTP 403 for this token), and devices with API 26 or 35 or with an enrolled biometric.
- **`plan.md` trimmed to open work.** Removed:
  - the Phase 1–6 checklists and their "verify locally" items, which CI covered (green on `4abfc8e`; `9f967c6` passed everything except the API 26 test fixed above);
  - the resolved questions and the Phase 7 "Decisions needed", now in `notes.md`;
  - the finished Settings, CI/CD and security checklists, and the Phase 7 bug table and order of work, now recorded in this entry.
  
  The Phase 6 "later" items moved under **Follow-on work**.
- Also updated: `cybersec.md`, `maint.md`, `map.md`, `notes.md`, `README.md` and `CONTRIBUTING.md`.

## 2026-10-03 — Dependabot alerts triaged; CS-23 and CS-24
- Read the Dependabot alerts and the dependency graph in the GitHub web UI, because this session had no GitHub API access to the repository. 65 alerts were open. All of them were on `settings.gradle.kts` (the Gradle dependency-graph snapshot), and none was for an app dependency.
- **49 already fixed by CS-22.** The dependency graph for `288ff67` (SBOM export) lists only netty 4.1.138.Final, protobuf-java 3.25.9, commons-io 2.22.0 and logback 1.5.38, and shows no alerts on them. Their 41 netty, 1 protobuf-java, 1 commons-io and 6 logback-core alerts were still open about 20 minutes after the push, waiting for Dependabot to close them.
- **CS-23 (15 alerts):** AGP 8.7.3 also brings Bouncy Castle 1.77, commons-compress 1.21, jdom2 2.0.6 and jose4j 0.9.5. The root `buildscript` now constrains them to Bouncy Castle 1.86 (all three modules), commons-compress 1.28.0, jdom2 2.0.6.1 and jose4j 0.9.6. The versions are in the catalog.
- **CS-24 (alert #60):** the Kotlin Gradle plugin 2.0.21 is affected by CVE-2026-53914, which is fixed in Kotlin 2.4.20. Logged as open, because it needs the toolchain upgrade.
- Also updated: `cybersec.md`, `plan.md` (Phase 7 item 3, and the toolchain upgrade) and `maint.md` (the supply-chain row, the pinned versions and the debt list).
- **Validation:** nothing was built. Neither the session's cloud workspace nor the workstation shell it used could reach Maven Central, Google Maven or the Gradle distribution server, so `./gradlew` wasn't run. The versions and their POM dependencies were read from Maven Central in the browser. Still to do: `./gradlew buildEnvironment`, the usual checks, and then CI.

## 2026-10-03 — Dependabot alerts closed; API 26 wipe-dialog test fixed
- **Dependabot:** after `b36b92e`, all 64 alerts for CS-22 and CS-23 show as fixed. The dependency graph lists Bouncy Castle 1.86, commons-compress 1.28.0, jdom2 2.0.6.1, jose4j 0.9.6 and netty 4.1.138.Final. Alert #60 (Kotlin Gradle plugin) was dismissed on GitHub as a tolerable risk at your request, so CS-24 moved to the accepted risks. No alerts are open.
- **CI on `b36b92e`:** "Lint, test, build" and the API 35 instrumented tests passed. API 26 failed on `ComposeScreensTest.loginScreenWipeDialogDismissRequestTriggersCancelCallback`, as it had on `9f967c6` and `288ff67`, so the earlier wait for the dialog window (2026-10-03) didn't fix it.
- **Root cause** (from the run's `instrumented-results-api-26` artifact): the assertion that failed was `assertTrue(cancelTriggered)` at line 480, after Back. The logcat shows the keyboard attaching to an input field as the dialog opened, then handling the Back press. On Android 8.x the platform gives a new window initial focus even in touch mode. Since CS-16, the wipe dialog's first focusable element is the "type WIPE" field, so the field took focus and the keyboard opened, and the first Back only closed the keyboard. API 28+ doesn't assign initial focus in touch mode, which is why API 35 passed. On real Android 8.x devices this is standard behaviour, so the app is unchanged.
- **Fix (test only):** the test calls `Espresso.closeSoftKeyboard()` (a no-op when no keyboard is shown) before `pressBack()`, and the assertion now has a message. `maint.md` §6 records the API 26–27 focus behaviour.
- **Validation:** not run yet. It needs the API 26 CI job. The change is limited to that one test.

## 2026-10-03 — Toolchain upgrade (working tree)
- **Decisions (yours):** targetSdk 36, which Google Play has required for new apps and updates since 2026-08-31, with compileSdk 37; only the plan's list of upgrades; validation on a branch and pull request through CI. detekt 1.23.8, ktlint 1.3.1 and Vico 2.0.0 stay.
- **Build:**
  - Gradle 8.11.1 → 9.6.1. The wrapper checksum `9c0f7fae…9e14` matches gradle.org's checksum page, the release notes and the GitHub release asset.
  - AGP 8.7.3 → 9.4.1, which needs Gradle 9.6 or later (Dependabot PR #15 failed on exactly that).
  - Built-in Kotlin: `org.jetbrains.kotlin.android` is removed from both build files and from the `pluginManagement` mapping. The root `buildscript` adds `kotlin-gradle-plugin` 2.4.20, because AGP 9 otherwise brings KGP 2.2.10.
  - The androidTest schema assets use `assets.directories`. `room-ktx` is dropped, because it's merged into `room-runtime`.
  - detekt's own classpath is pinned to the Kotlin version it embeds (`getSupportedKotlinVersion()`).
  - CI's emulator matrix is API 26 and 36 (minSdk and targetSdk).
- **Versions:** Kotlin 2.0.21 → 2.4.20, KSP → 2.3.12, Hilt 2.52 → 2.60.1, Room 2.6.1 → 2.8.5, SQLCipher 4.6.1 → 4.19.1, `androidx.sqlite` 2.4.0 → 2.7.1, Compose BOM 2024.12.01 → 2026.09.00 (UI 1.12.1, Material 3 1.4.0), activity → 1.13.0, lifecycle → 2.11.0, navigation → 2.10.2, core-ktx → 1.19.1, coroutines → 1.11.0, Espresso → 3.7.0, ext-junit → 1.3.0, ktlint-gradle → 14.2.0.
- **Checked against the AGP 9 defaults:** the R8 keep rules use `{ *; }`, so the stricter full mode still keeps constructors, and nothing loads resources by name, so optimized resource shrinking is safe. The code already uses the current Material 3 APIs. Room's Kotlin code generation is safe because the non-null scalar DAO queries use `COUNT` or `COALESCE`. CodeQL's current release supports Kotlin up to 2.4.20.
- **Security log:** CS-22 and CS-23 closed on CI run 37178965892 (`f8e3fc7`). CS-24 moved back from the accepted risks to In Progress.
- **Validation:** nothing was built. Neither environment could reach Maven Central, Google Maven or the Gradle distribution server. Versions came from Maven Central metadata and the AndroidX release pages. What's still to do is in `plan.md`, Follow-on work.

## 2026-10-04 — Toolchain upgrade: first CI run and fixes
- **CI on `97d6cbe`:**
  - Passed: "Lint, test, build" (ktlint, detekt, lint, unit tests, coverage, `assembleRelease`); CodeQL, now analysing Kotlin 2.4.20; the dependency graph; the secret scan.
  - Gradle 9.6.1 downloaded and passed the wrapper checksum check.
  - Failed: the instrumented tests on API 26 (1 of 64) and API 36 (2 of 64). AGP 9.4's test engine doesn't name failing tests in the log; they were read from the `instrumented-results-api-*` artifacts, downloaded with your approval.
- **`MigrationTest.migrate1To2ConvertsMoneyToCentsAndKeepsData` (API 26 and 36):**
  - Symptom: `AbstractMethodError` on `GeneratedSerializer.typeParametersSerializers()`, from `androidx.room.migration.bundle.FieldBundle$$serializer`.
  - Cause: Room 2.8.5's `room-migration` (test only) is built against kotlinx-serialization 1.8.1, but navigation, lifecycle and savedstate bring 1.7.3 into the app. The test runs against the app's copy, and AGP 9 no longer aligns test dependencies with the app's (`android.dependency.useConstraints` now defaults to false).
  - Fix: a dependency constraint in `app/build.gradle.kts` raises the app's kotlinx-serialization-core to 1.8.1 (catalog `kotlinxSerialization`).
- **`OverlayGuardTest` (API 36):**
  - Cause: AGP 9.4's test engine reports the test's `assumeTrue(SDK_INT < S)` skip as a failure.
  - Fix: the test uses `@SdkSuppress(maxSdkVersion = R)` instead, so the runner doesn't run it on API 31+.
- **Dependency graph after `97d6cbe`:**
  - Only `kotlin-gradle-plugin` 2.4.20 is present (CS-24).
  - Netty is gone (AGP 9.4.1's new test engine no longer uses gRPC), so the netty BOM constraint was removed from the root `buildscript`, the test-platform block and the catalog.
  - Six Dependabot alerts opened against AGP 9.4.1 components outside the root plugin classpath: Bouncy Castle 1.80.2 through lint, HttpClient 4.5.6 and commons-lang3 3.16.0. Logged as CS-25 (open).
- **Validation:** not run yet. It needs the next CI run.

## 2026-10-04 — Toolchain upgrade green; CS-25 fix
- **CI run 37183326846 on `2704d72` is fully green**, including the instrumented tests on API 26 and 36. The kotlinx-serialization alignment and the `@SdkSuppress` change fixed the two failures from `97d6cbe`. The dependency graph now has kotlinx-serialization-core 1.8.1 in the app, plus 1.4.1 on detekt's own classpath.
- **CS-24 closed:** the graph lists only `kotlin-gradle-plugin` 2.4.20, and CI is green.
- **CS-25 (six Dependabot alerts):**
  - Cause: Bouncy Castle 1.80.2 (#29, #30, #64, #65), HttpClient 4.5.6 (#66) and commons-lang3 3.16.0 (#67) are still resolved next to the patched copies. They come from lint's tool classpath and AGP's test-engine worker classpath in the app module, which the root `buildscript` constraints don't reach.
  - Fix: `app/build.gradle.kts` adds a component metadata rule, `RaiseBuildToolDependencies`. It covers detached configurations as well as named ones, and wherever a dependency asks for an older version it raises Bouncy Castle to 1.86, commons-lang3 to 3.18.0 and HttpClient to 4.5.14 (catalog `buildBouncyCastle`, `buildCommonsLang3`, `buildHttpClient`).
- **Checks:** ktlint 1.3.1, the same version CI uses, run with your approval in Claude's workspace, passes on `app/build.gradle.kts` and `build.gradle.kts`. It flagged an existing trailing space at the end of `settings.gradle.kts`, which CI's ktlint doesn't check; left unchanged. The catalog parses. Not run: Gradle; this needs the next CI run and dependency snapshot.
- Also updated: `cybersec.md`, `maint.md` (the `build*` row) and `plan.md` (the toolchain upgrade's remaining items).

## 2026-10-04 — Production-readiness review; CS-26 and CS-27
- Read-only review of `8a09fc6` (sign-in, vault, Keystore, database holder, session, exports, repositories, validation, the Gradle build and the three workflows) to answer whether the app is production ready. No code changed.
- **Security log:** CS-26 (Low, the vault stays unlocked while the app is in the background) and CS-27 (Medium, on-device PIN brute force on a rooted device; decision needed) added as open findings. CS-18 closed: the removal of `app/release/` was committed in `288ff67`, and no APK, AAB or DM file is tracked on `8a09fc6`.
- **Not run:** Gradle (neither the workstation shell nor the cloud workspace can reach Maven Central, Google Maven or the Gradle distribution server), device or emulator checks, and the GitHub API (no access in this session).

## 2026-10-04 — Over-budget expenses are confirmed, not blocked
- **Why:** the production-readiness review found that an expense past its category's monthly budget was rejected with no way to record it, so the records drifted from what was actually spent. Your choice: confirm before saving.
- **Change:**
  - `ValidateTransactionUseCase` takes `allowOverBudget` (default `false`). Without it, the budget failure now carries a `BudgetOverage` (category name, amount over, limit). With it, only the budget check is skipped.
  - `Add`/`UpdateTransactionUseCase` pass the flag through.
  - `TransactionViewModel` turns `BUDGET_EXCEEDED` into an `overBudget` `StateFlow` (kept across rotation) plus `confirmOverBudget()` and `dismissOverBudget()`. Save is ignored while the prompt is open.
  - `TransactionFormScreen` shows `OverBudgetDialog` (the shared `ConfirmDialog`): "This expense puts Dining $50.00 over its monthly budget of $200.00." with **Save anyway** / **Cancel**.
  - New strings `over_budget_title`, `over_budget_message` and `over_budget_confirm`. `error_budget_exceeded` stays as the `UiMessage` fallback for `BUDGET_EXCEEDED`.
- **Tests:** `ValidateTransactionUseCaseTest` (the failure carries the overage; a confirmed expense passes; confirming still runs every other check), `TransactionViewModelTest` (asks before saving, confirm saves, dismiss saves nothing and asks again), `CrudScreensTest.overBudgetDialogShowsTheOverageAndLetsTheUserChoose`.
- Also updated: `README.md`, `maint.md`, `map.md`, `notes.md`, `plan.md`.
- **Validation:** not run here; Gradle can't reach its repositories from either environment. It needs CI (`testDebugUnitTest`, and the instrumented tests for the dialog).

## 2026-10-04 — Language packs and a currency setting
- **Your choices:** the currency is a saved setting, not part of a language; the most-spoken variant of each language; default account and category names translated on new installs only.
- **Language packs:** `values-zh-rCN`, `values-hi`, `values-es` (Latin American), `values-ar`, `values-fr` and `values-pt` (Brazilian). Each has all 226 translatable strings and the 3 plurals, with every quantity the language needs (Arabic has six). They were drafted by Claude in parallel and checked with a script that compares names, format arguments, escaping and plural quantities against `values/strings.xml`. Arabic "Cash" was changed from نقدًا to النقد.
- **Currency:**
  - `CurrencyPreferences` saves an ISO code in `ui_prefs`. A new profile takes the region's currency (`Currencies.regionDefault`); installs from before the setting keep US dollars. Only currencies with 2 decimals are allowed.
  - **Settings → Currency** lists 21 common currencies plus the saved one, and notes that amounts aren't converted. Changing it restarts `MainActivity`.
  - `formatMoney` uses `NumberFormat` for the device's locale with the saved currency (`$1,234.50`, `1 234,50 €`, `R$ 1.234,56`, Arabic digits in Arabic). Positive income keeps its `+`.
  - `Money.parseToCents` accepts the currency's symbol or code on either side and any Unicode digits, plus the Arabic decimal and thousands separators. `DateUtils.parseIsoDate` accepts non-ASCII digits too.
  - The chart's axis uses the language's short number form (`android.icu.text.CompactDecimalFormat`) without a symbol, and `compactMoney` and its test were removed.
- **Text moved out of the code:**
  - The transaction type dropdown showed the stored values (`EXPENSE`…). It now shows `transaction_type_*` labels and still saves `TransactionType.name`, and the `transaction_types` array is gone.
  - The report title, the PDF headers, page number, totals and empty text, the 24 category icon labels, and the default account and category names are now string resources.
  - `app_name` is `translatable="false"`.
- **Transfers:** routes use `transferRoute`; in right-to-left languages the arrow points left (`النقد ← المدخرات`).
- **CSV:** unchanged keys, type codes and plain amounts; only the title is translated.
- **Tests:**
  - New: `CurrenciesTest`, `TextDirectionTest`; `MoneyTest` covers locale formatting, symbols on either side, and Arabic-Indic and Devanagari digits; `DateUtilsTest` covers non-ASCII dates; `CrudScreensTest.settingsCurrencyPickerReportsTheChosenCode`.
  - Updated: `DashboardMappersTest` pins `Locale.US`; `ComposeScreensTest` picks "Transfer"; `VaultTest` passes a context to `DefaultDataInitializer`.
- **Validation:**
  - ktlint 1.3.1 and detekt 1.23.8 (repository config) pass on the 32 changed Kotlin files.
  - `MoneyTest`, `CurrenciesTest`, `TextDirectionTest` and `DateUtilsTest` (20 tests) pass, compiled with kotlinc 2.1.0 on JDK 21 against a JUnit stand-in. CI uses JDK 17, whose locale data is older.
  - Not run: Gradle, Android lint, the Android-dependent unit tests and the instrumented tests. No environment here can reach Maven.
- **For a native speaker's review** (from the translators):
  - All: `over_budget_message` word order.
  - zh: 结余 for "Net"; 交易 for "Transaction".
  - hi: "Capital letters … don't count" in the security-answer rules; colloquial category labels.
  - es: "Informes" vs "Reportes"; "Auto"/"Supermercado".
  - ar: label-then-number phrasing for minutes and seconds; the dual in plurals.
  - fr: "plafond" for limit; "%2$d % du budget utilisé" is long.
  - pt: "Saldo" for both "Net" and an account balance; "celular" for "device".
