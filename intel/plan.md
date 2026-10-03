# Denarii Dolor — Plan

Roadmap derived from the project requirements and the 2026-09-20 code review. See `notes.md` for rationale and the full gap list, and `cybersec.md` for security findings. Items are proposals until confirmed.

## Requirement Status
| Requirement | Status | Notes |
|---|---|---|
| Inheritance / polymorphism / encapsulation | ✅ Done | `Transaction` → `Expense`/`Income`/`Transfer`, `balanceImpact()` |
| Search with multi-row results | ✅ Done | Typed multi-row results with count; tap to edit |
| Secure DB add / edit / delete | ✅ Done | Full add/edit/delete in UI; SQLCipher-encrypted DB |
| Reports (multi-column, rows, timestamp, title) | ✅ Done | 6 columns, title, period, timestamp, totals; CSV + PDF save/share |
| Validation | ✅ Done | Amount, description, date, references, budget limit, duplicate names (category/account) |
| Security | ✅ Done | PIN (PBKDF2) + lockout, strong biometrics, encrypted prefs, SQLCipher DB, no backups, R8, session timeout |
| Scalability | ✅ Done | MVVM, repos, use cases, Hilt, typed enums, cents, explicit migrations + exported schema, lint/static analysis/coverage in CI |
| GUI | ✅ Done | Bottom nav, FAB, monthly dashboard (hero, tiles, chart, budget meters, alerts), category icons, dark mode, manage screens |

## Phase 1 — Complete Core CRUD
### 1a — Data + domain layer ✅ (pending local build verification)
- [x] `@Update` / delete + `getById` on all DAOs; exposed via repositories.
- [x] `UpdateTransactionUseCase` / `DeleteTransactionUseCase` (reuse validation; edited row excluded from budget totals).
- [x] `CategoryUseCases`: add/rename/delete, duplicate check, delete blocked when in use or default.
- [x] `BudgetUseCases`: set limit + warning threshold per category, delete.
- [x] `AccountUseCases`: add (opening balance), rename, delete (blocked when in use or default).
- [x] Stored balances updated atomically on add/edit/delete via `Ledger.balanceDeltas()` (a transfer debits the source and credits the destination).
- [x] Unit tests (fakes) + instrumented Room test.
- [ ] Verify locally: `./gradlew testDebugUnitTest connectedAndroidTest`.

### 1b — UI ✅ (pending local build verification)
- [x] Recent transactions on the Dashboard: tap to edit (Add screen reused in edit mode), delete with confirmation.
- [x] Account balances on the Dashboard.
- [x] Category, account, and budget management screens + ViewModels, opened from Settings.
- [x] Category/account pickers on Add/Edit Transaction (replaced raw ID fields).
- [x] Compose UI tests + JVM tests for mappers.
- [ ] Verify locally: `./gradlew testDebugUnitTest connectedAndroidTest`.

## Phase 2 — Security Hardening ✅ (pending local build verification)
- [x] Encrypt Room with SQLCipher 4.6.1; random 256-bit key in a separate encrypted prefs file.
- [x] Plaintext or orphaned DB is wiped and reseeded (user choice: no migration).
- [x] Exclude DB, prefs, and files from backup and device transfer (`allowBackup=false` + extraction rules).
- [x] Enable R8 (`isMinifyEnabled`, `isShrinkResources`) with keep rules.
- [x] Removed `fallbackToDestructiveMigration()`; `exportSchema = true`.
- [x] PIN and security-answer lockout: 5 tries, then 30s doubling to 15 min.
- [x] `BIOMETRIC_STRONG` only.
- [ ] Verify locally: `./gradlew testDebugUnitTest connectedAndroidTest assembleRelease`; commit `app/schemas/`.
- Deferred (not selected): audit log table, `FLAG_SECURE`, biometric `CryptoObject`.
- Follow-up: upgrade SQLCipher to 4.1x with the Phase 5 toolchain upgrade (Kotlin 2.x / compileSdk 35+).

## Phase 3 — Reports & Search UX ✅ (pending local build verification)
- [x] Report columns: Date, Type, Category (name), Description, Amount, Payment Method (= account).
- [x] Report screen: title, generated timestamp, month navigation, scrollable table.
- [x] CSV **and** PDF export via Save (SAF) and Share (FileProvider).
- [x] Search: category picker, typed multi-row results with count, tap to edit, no duplicate collectors.
- [ ] Verify locally: `./gradlew testDebugUnitTest connectedAndroidTest assembleRelease`.

## Phase 4 — GUI Polish ✅ (pending local build verification)
- [x] Dashboard for the current month: hero net figure, income/expense tiles, spending-by-category chart (Vico) + value list.
- [x] Category icons (24 built-in) with a picker; icons in rows, the chart list, and budgets.
- [x] Budget meters + warning banner at `warningThresholdPercent` / over limit.
- [x] Light + dark theme; data-viz color roles.
- [x] Split `AppScreens.kt` into per-screen files.
- [x] Filtering transactions is covered by the Search tab (no separate list filter).
- [ ] Verify locally: `./gradlew testDebugUnitTest connectedAndroidTest assembleRelease`.
- Later: chart tooltip/marker; upgrade Vico to 2.x with the Kotlin 2 toolchain.

## Phase 5 — Code Quality & Scalability ✅ (pending local build verification)
- [x] `TransactionType` enum replaces type strings.
- [x] Money as `Long` cents end to end; DB v2 with the first Room `Migration` + migration test.
- [x] Version catalog cleaned; root build uses aliases.
- [x] ktlint + detekt + JaCoCo configured; CI/CD on JDK 17; sonar/dependency-check removed.
- [x] `NOTICE` rewritten; license headers on all Kotlin sources.
- [x] Tests: DAO (instrumented), migration, ViewModels (`kotlinx-coroutines-test`), Money.
- [ ] Commit `app/schemas/.../2.json` after the first build.
- [ ] `./gradlew ktlintFormat`; triage detekt; then make both CI steps blocking.
- [ ] Verify locally: `./gradlew testDebugUnitTest createDebugUnitTestCoverageReport connectedAndroidTest assembleRelease`.
- Feature modules: not required (user decision).

## Phase 6 — Toolchain upgrade ✅ (pending local build verification)
- [x] Gradle 8.11.1, AGP 8.7.3, Kotlin 2.0.21 + Compose compiler plugin, KSP 2.0.21-1.0.28, Compose BOM 2024.12.01, compileSdk/targetSdk 35.
- [x] Vico 2.0.0 (chart rewritten); detekt 1.23.8; AndroidX bumps.
- [x] Edge-to-edge + IME insets for targetSdk 35; `menuAnchor(MenuAnchorType)`.
- [x] SQLCipher kept at 4.6.1 (16 KB page-size compatible).
- [ ] Verify locally (see history.md), including inset layout on an API 35 device.
- Later: Kotlin 2.1+ (needs a Hilt release that supports Kotlin 2.1 metadata), Room 2.7/KSP2, SQLCipher 4.1x (compileSdk 37 / Room 3).

## Phase 7 — Bug and security remediation (review of 2026-10-03)
Source: a full review of `app/src/main`, the build, the workflows and the repository on 2026-10-03. Security findings are written up in `cybersec.md` as CS-13 – CS-22. Bugs are listed below as BUG-NN. Nothing in this phase is implemented yet.

**Baseline (2026-10-03):**
- `./gradlew testDebugUnitTest --offline` passed 117/117.
- CI on `4abfc8e` (2026-09-27) passed lint, unit tests, the release build, and the instrumented tests on API 26 and 35.
- CD published v1.0.1.
- Instrumented tests and `assembleRelease` were not run locally.

### Bugs
| ID | Priority | Bug | Where | Fix | Tests |
|---|---|---|---|---|---|
| BUG-01 | P1 | Amounts typed with a decimal comma are read 10–100× too large, because `parseToCents` strips every comma. `"12,50"` becomes $1,250.00, `"12,5"` $125.00 and `"1,2,3"` $123.00 (reproduced against the compiled class). This affects the transaction amount, search min/max, budget limit and opening balance, wherever the keyboard offers `,` as the decimal key. | `util/Money.kt` | Accept a comma only as a thousands separator in valid groups (`^\d{1,3}(,\d{3})+(\.\d+)?$`). Anything else returns `null`, which shows "invalid amount". | `MoneyTest`: `12,50`, `12,5`, `1,2,3` and `,5` return null; `1,234` and `1,234.56` still parse. |
| BUG-02 | P1 | A quick double tap on **Save** inserts the transaction twice and applies the balance change twice. Each tap launches a new coroutine, the form resets only after `Saved`, and the budget check can pass for both. In edit mode the two `Saved` events trigger BUG-03. The manage dialogs and report Save/Share follow the same pattern, but duplicates there fail harmlessly or race on `cache/reports/`. | `TransactionViewModel.saveTransaction`, `TransactionFormScreen.kt`; also `AccountViewModel`, `CategoryViewModel`, `BudgetViewModel`, `ReportViewModel` | Add an `isSaving` state: ignore calls while one is in flight and disable the button. Apply the same guard to Save/Share. | `TransactionViewModelTest`: two rapid saves give one insert. Compose test: the button is disabled while saving. |
| BUG-03 | P2 | Every `onFinished` and `onBack` calls `navController.popBackStack()` without a guard. A double tap on **Back**, or two `Saved` events, pops past the screen and the Dashboard start destination, leaving a blank NavHost. | `presentation/ui/AppScreens.kt` | Wrap the call in `dropUnlessResumed { navController.popBackStack() }`. `lifecycle-runtime-compose` 2.8.7 is already a dependency. | Compose test: double-clicking **Back** on Add Transaction still shows the Dashboard. |
| BUG-04 | P2 | Sign-in, setup and recovery run PBKDF2-HMAC-SHA256 (210 000 iterations) on the main thread from click handlers: one hash per sign-in, two per setup or recovery. `EncryptedSharedPreferences`, Keystore and SQLCipher setup also run on the main thread during Hilt injection. On slow API 26 devices this can freeze the UI for seconds, with a risk of an ANR. | `LoginActivity.kt`, `EncryptedPreferencesManager.kt`, `DatabaseModule.kt` | Run this work on `Dispatchers.Default` or `IO`, with `actionInProgress` set. Make `SecurityProfileService` attempt, recover and setup mutually exclusive (`@Synchronized`). Otherwise moving off the main thread would allow parallel guesses past the lockout, because the failure counter is updated with a non-atomic read-modify-write. This is also a good time to move `LoginActivity` state into a ViewModel (`maint.md` §10). | Unit test: concurrent wrong attempts are all counted. Compose test: inputs are disabled while checking. StrictMode is clean in a debug build. |
| BUG-05 | P2 | If the Keystore master key or an encrypted-prefs keyset becomes unreadable (a known `security-crypto` failure mode), `EncryptedSharedPreferences.create` throws during Hilt injection and the app crashes on every launch. The only way out is the system **Clear storage**. | `EncryptedPreferencesManager.kt`, `DatabaseKeyProvider.kt` | Catch `GeneralSecurityException` and `IOException` at creation. Show a reset screen that deletes the prefs files, Keystore aliases and database, then returns to setup. Fold this into the CS-09 / CS-15 rework. | Unit test with a failing store factory. Manual: delete the Keystore alias on a debug build. |
| BUG-06 | P3 | A TRANSFER row without a `transferAccountId` makes `toDomainTransaction()` throw. On the Dashboard this happens inside `combine` with no `catch`, so the start screen crashes on every launch. Nothing in the schema prevents such rows (there is no CHECK constraint). | `domain/model/TransactionEntityMappings.kt`, `dashboard/DashboardMappers.kt`, `DashboardViewModel.kt` | Make the read-path mapping total: a malformed transfer has no impact and is flagged. Add `.catch` that leads to an error state. Add a CHECK constraint and a repair step in the next migration. | `DashboardMappersTest` with a malformed row. `DashboardViewModelTest` for the error state. |
| BUG-07 | P3 | Search treats `%` and `_` in the description as LIKE wildcards, so `_` matches everything. This is not an injection, because the value is bound. | `TransactionDao.search`, `SearchTransactionUseCase` | Escape `\`, `%` and `_`, and add `ESCAPE '\'`. | `TransactionDaoTest`: `50%` and `_` match literally. |
| BUG-08 | P3 | Descriptions have no maximum length and aren't trimmed. Names are capped at 50 characters. | `util/Validators.kt`, `TransactionFormScreen.kt` | Trim descriptions and cap them, for example at 200 characters, in both the validator and the field. | Boundary cases in `ValidatorsTest` and `ValidateTransactionUseCaseTest`. |
| BUG-09 | P3 | Budget checks use `ZoneId.systemDefault()`, but the Dashboard and reports use the `Clock` zone captured at app start. After a time-zone change, a transaction near midnight on the 1st can count toward different months. This also breaks `maint.md` rule 4. | `ValidateTransactionUseCase.monthBounds`, `di/AppModule.kt` | Inject `Clock` into the validator, and provide a `Clock` whose zone follows the current device zone. | `ValidateTransactionUseCaseTest` with a fixed non-UTC clock. |
| BUG-10 | P3 | On every change, the Dashboard reloads and maps every transaction ever recorded, then filters the current month in memory. It also doesn't roll over to a new month until the data changes. | `DashboardViewModel.kt`, `TransactionDao.kt` | Add DAO queries for the current month and the 20 most recent rows. Recompute the period on resume. | `TransactionDaoTest` for the new queries, and `DashboardViewModelTest`. |
| BUG-11 | P3 | Errors are shown as the raw `Throwable.message`: English only, and sometimes SQLite text. | `UiMessage.fromResult`, `TransactionEvent.Failed` | Map domain errors to string resources, falling back to `generic_error`. | Unit tests for the mapping. |
| BUG-12 | P3 | A legacy path is dead: `SecurityProfileService` still reads a plaintext `key_pin`. A 4- or 5-digit legacy PIN can never pass setup under the 6–12 digit rule, and `LEGACY_PIN_MISMATCH` isn't throttled. No released build ever stored a plaintext PIN. | `SecurityProfileService.kt`, `LoginActivity.kt`, `strings.xml` | Remove the legacy branch, and delete any `key_pin` value on start. | Update `SecurityProfileServiceTest`: remove the legacy test and check that a stray `key_pin` is deleted. |

### Order of work
1. **Repository and CI hygiene (no app changes).**
   - CS-18: untrack `app/release/` and ignore build outputs.
   - CS-22: review the Dependabot alerts and triage PRs #12–#17.
   - CI: pin `runs-on: ubuntu-24.04` before GitHub moves `ubuntu-latest` to Ubuntu 26 on 2026-10-19, because the emulator jobs depend on the runner image.
   - Do the manual device checks for CS-01, CS-02 and CS-04, then close them.
2. **Data integrity:** BUG-01, BUG-02, BUG-03.
3. **Sign-in hardening:**
   - CS-13 (monotonic lockout), together with BUG-04 (off the main thread, attempts serialized) and BUG-12.
   - Then CS-14 and CS-16, once decided.
4. **Key management:** CS-15 together with CS-07, CS-09, CS-17 and BUG-05, as one design. Write the design into `maint.md` first. It covers:
   - a Keystore AES-GCM wrapper that replaces `security-crypto`;
   - the database key wrapped by the PIN, biometrics and the recovery secret;
   - a closable database holder;
   - a cryptographic erase on wipe;
   - a reset screen for Keystore failures;
   - the upgrade migration from v1.0.1.
5. **Robustness:** BUG-06 – BUG-11.
6. **Platform and supply chain:**
   - CS-08 + CS-19: pin actions to SHAs after merging PR #17, and split CD.
   - CS-11 + CS-21: overlay and task-hijacking hardening.
   - CS-20: CSV guard.
   - CS-12: audit log (still deferred).

Every step needs:
- the tests named in the tables;
- `./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleRelease`;
- `connectedDebugAndroidTest` (or CI) for database and UI changes;
- a green CI run.

Then update `cybersec.md` (status and validation evidence) and `history.md`.

### Decisions needed
1. **CS-14 recovery:** a recovery code (recommended), a stricter security question, or no recovery (wipe only)?
2. **CS-15 scope:** do the key-binding redesign now (the largest item; it changes app start-up), or record it as an accepted risk for now?
3. **CS-16 wipe:** add friction, or accept it as CS-A5?
4. **BUG-01:** reject comma decimals (minimal), or support locale decimal separators? Money is formatted as US dollars throughout.

## Settings
- [x] Removed Recover PIN from Settings; added a persisted dark mode switch (2026-09-21).

## CI/CD
- [x] security.yml, ci.yml, cd.yml rebuilt; Makefile release flow verified (see history.md).
- [x] Add repository secrets `ANDROID_SIGNING_KEY`, `ANDROID_SIGNING_KEYSTORE_PASSWORD`, `ANDROID_SIGNING_KEY_ALIAS`, `ANDROID_SIGNING_KEY_PASSWORD` (CD signed and published v1.0.1 on 2026-09-27).
- [ ] Optionally protect the `production` environment with required reviewers.

## Resolved Questions (2026-09-20)
1. Account balances — stored, and updated from transaction calculations.
2. SQLCipher — permitted.
3. Export — both PDF and CSV required.
4. Multi-module split — not required.

## Security (tracked in `cybersec.md`)
- [x] CS-01 – CS-05 fixed 2026-09-21 (session clock, session gate, CI token persistence, share-cache cleanup, Dependabot).
- [ ] Verify: `./gradlew testDebugUnitTest connectedDebugAndroidTest assembleRelease` (done: CI green on `4abfc8e`, 2026-09-27), plus the manual clock-rollback check (still open).
- [ ] Run the validation listed for CS-01, CS-02, CS-04, CS-05 and CS-10 in `cybersec.md`, then mark each `Closed`. CS-03 was closed 2026-09-22, and CS-05 and CS-10 on 2026-10-03. CS-01, CS-02 and CS-04 still need their manual checks.
- [x] CS-06 FLAG_SECURE — declined (accepted risk CS-A4).
- [x] CS-10 6–12 digit PIN, enforced for setup, reset and sign-in (2026-09-21).
- [ ] CS-07 biometric `CryptoObject` bound to a Keystore key (planned with CS-15, Phase 7 step 4).
- [ ] CS-08 pin workflow actions to commit SHAs.
- [ ] CS-09 migrate off deprecated `security-crypto` (planned with CS-15).
- [ ] CS-11 hide overlays / tapjacking protection on the login screen.
- [ ] CS-12 audit log table.
- [ ] CS-13 – CS-22 (review of 2026-10-03): see Phase 7.
