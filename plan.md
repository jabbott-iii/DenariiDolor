# Denarii Dolor — Plan

Roadmap derived from the project requirements and the 2026-09-20 code review. See `notes.md` for rationale and the full gap list. Items are proposals until confirmed.

## Requirement Status
| Requirement | Status | Notes |
|---|---|---|
| Inheritance / polymorphism / encapsulation | ✅ Done | `Transaction` → `Expense`/`Income`/`Transfer`, `balanceImpact()` |
| Search with multi-row results | 🟡 Partial | Query + parser done; results shown as plain strings |
| Secure DB add / edit / delete | 🟡 Partial | Full add/edit/delete in UI; DB still unencrypted (Phase 2) |
| Reports (multi-column, rows, timestamp, title) | 🟡 Partial | Missing category name + payment method; no export |
| Validation | ✅ Done | Amount, description, date, references, budget limit, duplicate names (category/account) |
| Security | 🟡 Partial | PIN (PBKDF2) + biometric + encrypted prefs + timeout done; DB encryption, backup rules, R8 pending |
| Scalability | 🟡 Partial | MVVM, repos, Hilt, use cases; single module (accepted), destructive migrations |
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

## Phase 2 — Security Hardening
- [ ] Encrypt Room with SQLCipher (approved); passphrase generated once and stored via Keystore/encrypted prefs.
- [ ] Exclude DB and prefs from backup (`allowBackup=false` or explicit exclude rules).
- [ ] Enable R8 (`isMinifyEnabled = true`, shrink resources) with keep rules for Room/Hilt.
- [ ] Replace `fallbackToDestructiveMigration()` with explicit migrations; `exportSchema = true`.
- [ ] PIN attempt throttling / lockout.
- [ ] Optional: `BIOMETRIC_STRONG` + `CryptoObject`.
- [ ] Audit log table for create/update/delete events.
- [ ] `FLAG_SECURE` on sensitive screens.

## Phase 3 — Reports & Search UX
- [ ] Report model columns: Date, Category (name), Description, Amount, Payment Method (add payment method / account name to transactions).
- [ ] Report screen: title, generated timestamp, month picker, table via `LazyColumn`.
- [ ] CSV **and** PDF export (both required) through SAF (`ACTION_CREATE_DOCUMENT`) / share sheet; PDF via `android.graphics.pdf.PdfDocument` unless a library is preferred.
- [ ] Search results as a typed row model with multi-column layout; category dropdown instead of ID entry; date pickers.

## Phase 4 — GUI Polish
- [ ] Dashboard cards (income/expense/net, budget progress), spending-by-category chart.
- [ ] Category icons; filters on transaction list.
- [ ] Budget warning banner at `warningThresholdPercent`.
- [ ] Split `AppScreens.kt` into per-feature screen files; add theme/typography tokens.

## Phase 5 — Code Quality & Scalability
- [ ] Replace string `type` with enum/sealed class + Room `TypeConverter`.
- [ ] Money as `Long` cents (or `BigDecimal`) — decide before adding migrations.
- [x] ~~Feature modules~~ — not required; keep single `:app` module with package-level separation.
- [ ] Clean `libs.versions.toml` (duplicate activity-compose aliases, unused navigation-fragment/ui, recyclerview, constraintlayout).
- [ ] Configure or remove detekt/ktlint/jacoco/dependency-check/sonar in Gradle; align CI/CD JDK versions.
- [ ] Rewrite `NOTICE`; add license headers per `CONTRIBUTING.md`.
- [ ] Tests: DAO tests (in-memory Room), use-case tests for update/delete, ViewModel tests with `kotlinx-coroutines-test`.

## Resolved Questions (2026-09-20)
1. Account balances — stored, and updated from transaction calculations.
2. SQLCipher — permitted.
3. Export — both PDF and CSV required.
4. Multi-module split — not required.
