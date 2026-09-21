# Denarii Dolor — Plan

Roadmap derived from the project requirements and the 2026-09-20 code review. See `notes.md` for rationale and the full gap list, and `cysec.md` for security findings. Items are proposals until confirmed.

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

## Settings
- [x] Removed Recover PIN from Settings; added a persisted dark mode switch (2026-09-21).

## CI/CD
- [x] security.yml, ci.yml, cd.yml rebuilt; Makefile release flow verified (see history.md).
- [ ] Add repository secrets `ANDROID_SIGNING_KEY`, `ANDROID_SIGNING_KEYSTORE_PASSWORD`, `ANDROID_SIGNING_KEY_ALIAS`, `ANDROID_SIGNING_KEY_PASSWORD`; optionally protect the `production` environment.

## Resolved Questions (2026-09-20)
1. Account balances — stored, and updated from transaction calculations.
2. SQLCipher — permitted.
3. Export — both PDF and CSV required.
4. Multi-module split — not required.

## Security (tracked in `cysec.md`)
- [x] CS-01 – CS-05 fixed 2026-09-21 (session clock, session gate, CI token persistence, share-cache cleanup, Dependabot).
- [ ] Verify: `./gradlew testDebugUnitTest connectedDebugAndroidTest assembleRelease`, plus the manual clock-rollback check.
- [x] CS-06 FLAG_SECURE — declined (accepted risk CS-A4).
- [x] CS-10 6–12 digit PIN, enforced for setup, reset and sign-in (2026-09-21).
- [ ] CS-07 biometric `CryptoObject` bound to a Keystore key.
- [ ] CS-08 pin workflow actions to commit SHAs.
- [ ] CS-09 migrate off deprecated `security-crypto`.
- [ ] CS-11 hide overlays / tapjacking protection on the login screen.
- [ ] CS-12 audit log table.
