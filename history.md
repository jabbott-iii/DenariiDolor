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
