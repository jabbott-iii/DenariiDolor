# Denarii Dolor — Plan

Roadmap derived from the project requirements and the 2026-09-20 code review. See `notes.md` for rationale and the full gap list. Items are proposals until confirmed.

## Requirement Status
| Requirement | Status | Notes |
|---|---|---|
| Inheritance / polymorphism / encapsulation | ✅ Done | `Transaction` → `Expense`/`Income`/`Transfer`, `balanceImpact()` |
| Search with multi-row results | 🟡 Partial | Query + parser done; results shown as plain strings |
| Secure DB add / edit / delete | 🟡 Partial | Full add/edit/delete in UI; DB still unencrypted (Phase 2) |
| Reports (multi-column, rows, timestamp, title) | ✅ Done | 6 columns, title, period, timestamp, totals; CSV + PDF save/share |
| Validation | ✅ Done | Amount, description, date, references, budget limit, duplicate names (category/account) |
| Security | ✅ Done | PIN (PBKDF2) + lockout, strong biometrics, encrypted prefs, SQLCipher DB, no backups, R8, session timeout |
| Scalability | 🟡 Partial | MVVM, repos, Hilt, use cases; single module (accepted); explicit migrations + exported schema |
| GUI | 🟡 Partial | Bottom nav, FAB, dashboard summary + balances + recent list, manage screens; charts, icons, filters pending |

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

## Phase 4 — GUI Polish
- [ ] Dashboard cards (income/expense/net, budget progress), spending-by-category chart.
- [ ] Category icons; filters on transaction list.
- [ ] Budget warning banner at `warningThresholdPercent`.
- [ ] Split `AppScreens.kt` into per-feature screen files; add theme/typography tokens.

## Phase 5 — Code Quality & Scalability
- [ ] Replace string `type` with enum/sealed class + Room `TypeConverter`.
- [ ] Money as `Long` cents (or `BigDecimal`) — decide before adding migrations.
- [x] ~~Feature modules~~ — not required; keep single `:app` module with package-level separation.
- [ ] Upgrade toolchain (Kotlin 2.x, compileSdk 35+) and SQLCipher 4.1x.
- [ ] Clean `libs.versions.toml` (duplicate activity-compose aliases, unused navigation-fragment/ui, recyclerview, constraintlayout).
- [ ] Configure or remove detekt/ktlint/jacoco/dependency-check/sonar in Gradle; align CI/CD JDK versions.
- [ ] Rewrite `NOTICE`; add license headers per `CONTRIBUTING.md`.
- [ ] Tests: DAO tests (in-memory Room), use-case tests for update/delete, ViewModel tests with `kotlinx-coroutines-test`.

## Resolved Questions (2026-09-20)
1. Account balances — stored, and updated from transaction calculations.
2. SQLCipher — permitted.
3. Export — both PDF and CSV required.
4. Multi-module split — not required.
