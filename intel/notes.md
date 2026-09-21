# Denarii Dolor — Project Notes

Running log of context, decisions, clarifications, and preferences. Read this (and `plan.md`, `cysec.md`) before making changes.

## Project Summary
Personal budget and expense tracker for Android. Users log income, expenses, transfers, categories, budgets, and accounts; search transactions; and generate monthly reports. Built to satisfy course requirements: OOP (inheritance/polymorphism/encapsulation), multi-row search, secure CRUD database, multi-column timestamped reports, validation, industry-appropriate security, scalable design, and a user-friendly GUI.

## Tech Stack (as built)
| Area | Choice |
|---|---|
| Language | Kotlin 2.0.21 (Phase 6), JVM toolchain 17 |
| UI | Jetpack Compose (Material 3, BOM 2024.12.01, Kotlin Compose compiler plugin), Navigation Compose, Vico 2.0.0 charts |
| Architecture | MVVM + Repository + use cases (`domain/usecase`) |
| DI | Hilt 2.52 (KSP) |
| Database | Room 2.6.1 + SQLCipher 4.6.1, DB `denarii_dolor.db`, schema v2 (Long cents) |
| Async | Coroutines + Flow/StateFlow |
| Auth | PIN (PBKDF2) + BiometricPrompt (`BIOMETRIC_WEAK`) |
| Secure storage | `EncryptedSharedPreferences` (security-crypto 1.1.0-alpha06), AES256-GCM master key |
| SDK | minSdk 26, target/compile 35, AGP 8.7.3, Gradle 8.11.1 |

## Package Layout (`app/src/main/java/com/denariidolor`)
- `domain/model` — `Transaction` (abstract) → `Expense`, `Income`, `Transfer`; `Account`, `Budget`, `Category`, `SearchFilters`, `MonthlyReport`/`ReportRow`, entity↔domain mappings.
- `domain/usecase` — `AddTransaction`, `ValidateTransaction`, `SearchTransaction`, `GenerateReport`.
- `data/local/db` — `AppDatabase`, entities, DAOs, `DefaultDataInitializer` (seeds Cash/Savings accounts and 3 default categories).
- `data/local/preferences` — `EncryptedPreferencesManager` (Android wrapper) + `SecurityProfileService` (pure-Kotlin, unit-testable PIN/recovery logic behind a `SecurityProfileStore` interface).
- `data/repository` — interface + `Impl` per aggregate (Transaction, Category, Budget, Account).
- `di` — `AppModule` (Clock), `DatabaseModule`, `RepositoryModule`.
- `presentation/ui` — `AppScreens.kt` (all Compose screens, ~900 lines), per-feature ViewModels, `auth/LoginActivity` (launcher), `search/SearchFilterParser`.
- `util` — `Constants`, `DateUtils`, `SessionManager`, `Validators`.

## Key Design Decisions (observed in code)
- **Polymorphism:** each `Transaction` subclass overrides `balanceImpact()` — Expense `-amount`, Income `+amount`, Transfer `0.0` (net-neutral across accounts). Dashboard and reports sum `balanceImpact()` for net.
- **Persistence model:** single `transactions` table with a `type` discriminator string (`EXPENSE`/`INCOME`/`TRANSFER`) and nullable `transferAccountId`. FKs to categories/accounts use `RESTRICT`; budgets cascade on category delete; unique index on category name and account name; one budget per category.
- **Validation:** `Validators` (amount > 0 and finite, non-blank description, positive epoch, sane ranges) + `ValidateTransactionUseCase` (valid account/category, transfer destination required and different, expense must not push monthly category spend over `Budget.monthlyLimit`). Returns `Result` rather than throwing.
- **Search:** single parameterized Room query with nullable filters (description LIKE, category, amount range, date range). UI inputs parsed by `SearchFilterParser` (ISO dates → start/end of day in the device zone).
- **Reports:** `GenerateReportUseCase(year, month)` → rows, totals, net, `generatedAt`, and a CSV string (RFC-4180-style quoting on description).
- **Auth / PIN lifecycle:** single-user profile. First launch = setup (PIN 6–12 digits + security question/answer). PIN and answer stored as PBKDF2-HMAC-SHA256 hashes (210k iterations, 16-byte salt, 256-bit key) inside encrypted prefs; constant-time comparisons. Recovery via security answer. Legacy plaintext `key_pin` migrates on setup. "Wipe all data" clears Room tables, secure prefs, cache, then reseeds defaults.
- **Session:** `SessionManager` singleton, 5-minute inactivity timeout; `MainActivity` checks every 30s, on resume, and on each user interaction, then redirects to `LoginActivity` with a cleared task.

## Tooling & Process
- CI (`.github/workflows/ci.yml`): lint, `assembleDebug`, `testDebugUnitTest`, emulator `connectedAndroidTest` (API 33), plus detekt/ktlint/sonar/dependency-check steps (currently non-blocking).
- Security workflow: CodeQL (java-kotlin) weekly + on push/PR.
- CD: tag `vX.Y.Z` (`make release VERSION=vX.Y.Z`) → signed release APK/AAB, GitHub Release, Play internal track.
- Contribution rules: issue first, approval before PR, license header required on every source file.
- Recent work lands via Copilot branches (`copilot/*`) merged into `main`.

## Known Gaps / Observations (from review 2026-09-20)
1. ~~**CRUD incomplete**~~ — done in Phases 1a/1b (see `history.md`).
2. ~~Room DB not encrypted~~ — SQLCipher in Phase 2.
3. ~~Backups include data~~ — excluded in Phase 2.
4. ~~R8 off~~ — enabled in Phase 2 via `app/proguard-rules.pro`. `src/main/keepRules/rules.keep` is an unused template (AGP 8.5 doesn't read that folder).
5. ~~Destructive migrations~~ — removed in Phase 2; schema exported to `app/schemas/`.
6. ~~Account balances never updated~~ — fixed in Phase 1a (stored balances updated atomically). Data created before then may be out of sync.
7. ~~Duplicate-category validation / empty ViewModels~~ — done in Phases 1a/1b.
8. ~~Budget warning threshold unused~~ — dashboard meters and banner (Phase 4).
9. ~~Report columns / export~~ — done in Phase 3.
10. ~~Search result strings~~ — typed rows in Phase 3.
11. ~~String transaction types~~ — `TransactionType` enum (Phase 5).
12. ~~Money as Double~~ — Long cents + DB v2 migration (Phase 5).
13. No audit log or role checks (listed in the recommended stack).
14. Biometrics now `BIOMETRIC_STRONG` (Phase 2); still no `CryptoObject` (deferred).
15. `SessionManager` state is in-memory only; process death resets to unauthenticated (safe), but `LoginActivity` launch is the only gate.
16. Dependency hygiene: `libs.versions.toml` has five unused `activity-compose` version aliases; `app/build.gradle.kts` pulls both `activity-compose` 1.9.0 and 1.9.2, plus unused `navigation-fragment`/`navigation-ui` (XML-era).
17. ~~NOTICE~~ — rewritten (Phase 5).
18. ~~License headers~~ — added to all Kotlin files (Phase 5); add them to new files going forward.
19. ~~CI JDK mismatch / unconfigured tools~~ — JDK 17 everywhere; ktlint/detekt/JaCoCo configured (Phase 5). CD `publishBundle` still unconfigured (Phase 6).
20. ~~`AppScreens.kt` size~~ — split in Phase 4 (~150 lines, navigation only); split per feature as screens grow. Spec calls for separate feature modules — currently single `:app` module.

## Preferences & Conventions
- Pair-programming style: minimal, clean, tested, secure code; no filler comments.
- Ask up to 3 clarifying questions when requirements are ambiguous; never guess architectural decisions.
- Outline a plan before multi-file changes; present per-file changes and get confirmation before finalizing.
- Record plans, clarifications, rationale, and confirmed preferences in this file.
- User reviews diffs and commits changes themselves; Claude leaves work uncommitted.
- Context files live in `intel/` at the repo root: `notes.md` (decisions), `plan.md` (roadmap), `history.md` (session log), `cysec.md` (security findings). Read them before making changes.
- `intel/cysec.md` is updated continuously: every change that touches auth, storage, exports, build or CI records new findings, fixes (keeping stable `CS-NN` IDs) or accepted risks.

## Decision Log
| Date | Decision | Rationale |
|---|---|---|
| 2026-09-20 | Created `notes.md` and `plan.md` from a codebase review. | Establish shared context for future sessions. |
| 2026-09-20 | Account balances are stored and updated from transaction calculations (`balanceImpact()`). | User confirmed; fast dashboard reads, must stay consistent via DB transactions on add/edit/delete. |
| 2026-09-20 | SQLCipher is permitted for Room encryption. | User confirmed; resolves gap #2. |
| 2026-09-20 | Reports must export to both PDF and CSV. | User confirmed. |
| 2026-09-20 | Multi-module split not required; stay single `:app` module. | User confirmed; gap #20 reduced to splitting `AppScreens.kt`. |
| 2026-09-20 | Balance sync lives in `TransactionRepositoryImpl` inside `withTransaction`, driven by the pure `Ledger.balanceDeltas()`, which uses polymorphic `accountImpacts()`. | Atomic (no half-applied edits); logic is unit-testable without Room; showcases polymorphism. |
| 2026-09-20 | Deleting a category or account that is still referenced is **blocked** (no reassignment); seeded default categories/accounts cannot be deleted. | Safest reversible default; the Add Transaction screen relies on default IDs. Revisit if reassign-on-delete is wanted. |
| 2026-09-20 | No schema change for Phase 1a (DB stays v1). | Avoids a migration while `fallbackToDestructiveMigration()` is still in place. |
| 2026-09-20 | Use cases return `Result`; `runSuspendCatching` used instead of `runCatching`. | Keeps coroutine cancellation working. |
| 2026-09-20 | Context files: `notes.md` (decisions), `plan.md` (roadmap), `history.md` (session log). | User request. |
| 2026-09-20 | User approved Phase 1a decisions (block delete when in use, protect defaults). | Confirmed. |
| 2026-09-20 | Transaction list lives on the **Dashboard** as "Recent transactions" (tap = edit, delete with confirm). | User choice. |
| 2026-09-20 | Category / Account / Budget management screens open from **Settings**. | User choice. |
| 2026-09-20 | Phase 1b plan: shared UI components in `presentation/ui/common`; one Add/Edit screen (edit via `edit_transaction/{transactionId}` + `SavedStateHandle`); one-shot VM events via `Channel` instead of `StateFlow<String>` (fixes repeat-save not resetting the form); new screens in per-feature files; logic kept in pure functions for JVM tests (no coroutines-test dependency added). | Keeps `AppScreens.kt` from growing; testable without new deps. |
| 2026-09-20 | Phase 2 scope (user): plaintext DB on upgrade is **wiped and reseeded** (no migration); PIN lockout = **escalating delay** (5 failures → 30s, doubling, max 15 min, persisted); **BIOMETRIC_STRONG** only. Audit log and FLAG_SECURE **not** included. | User choices. |
| 2026-09-20 | DB key: random 256-bit, Base64 in a **separate** EncryptedSharedPreferences file (`db_key_prefs`) so "wipe all data" (which clears `secure_prefs`) can't orphan the DB key. Missing key + existing DB file → DB deleted and reseeded. | Avoids unopenable DB after wipe/key loss. |
| 2026-09-20 | Pinned `net.zetetic:sqlcipher-android:4.6.1` + `androidx.sqlite:sqlite:2.4.0`. | Latest (4.18+) targets compileSdk 37/Room 3; 4.6.1 matches Room 2.6.1 / Kotlin 1.9 toolchain. Upgrade with Phase 5 toolchain bump. |
| 2026-09-20 | Lockout also throttles security-answer attempts; biometric sign-in stays allowed during a PIN lockout and resets the counter on success. | Recovery is otherwise a brute-force bypass; biometrics have their own OS-level lockout. |
| 2026-09-20 | Phase 3: **Payment Method = the transaction's account** (transfers show `Source → Destination`); no schema change. | User choice. |
| 2026-09-20 | Phase 3: exports offer **Save** (SAF `CreateDocument`) and **Share** (FileProvider over `cache/reports/`, cleared on each share). PDF via `android.graphics.pdf.PdfDocument`; CSV via a pure formatter with formula-injection guarding. | User choice; no storage permissions or third-party PDF library. |
| 2026-09-20 | Phase 3: `TransactionRow` + row composable move to `presentation/ui/common` (shared by Dashboard and Search); money formatting moves to `util/MoneyFormat.kt`; search uses `flatMapLatest` over a filters `StateFlow`. | Reuse; fixes duplicate collectors on repeated searches. |
| 2026-09-20 | Phase 4 (user): charts via **Vico**; dashboard summarizes the **current month**; categories get a **built-in icon picker** (material-icons-extended, curated set). | User choices. |
| 2026-09-20 | Pinned **Vico 1.14.0** (`compose`, `compose-m3`, `core`): last release built with Kotlin 1.9.22 / Compose compiler 1.5.8. Vico 1.15+ / 2.x are built with Kotlin 2.0/2.1 and can't be consumed by Kotlin 1.9.24. Upgrade with the Phase 5 toolchain bump. | Binary compatibility. |
| 2026-09-20 | Dataviz rules applied: spending-by-category = single-series column chart in categorical slot 1 (blue `#2a78d6` light / `#3987e5` dark, validator PASS both modes), top 5 + "Other", no legend (title names it), text list under the chart as the table view; budgets = meters (accent → warning `#fab219` ≥ threshold → critical `#d03b3b` > 100%) with icon + label, never color alone; one hero figure (net this month). | Accessibility / consistency. |
| 2026-09-20 | `AppScreens.kt` split into `LoginScreen.kt`, `TransactionFormScreen.kt`, `SettingsScreen.kt` in the **same package** (`presentation.ui`). | Pure file split; no API or test changes. |
| 2026-09-20 | Phase 5 (user): toolchain upgrade (Kotlin 2 / AGP / compileSdk 35 / Vico 2 / SQLCipher latest) is a **separate later step**; money moves to **Long cents**; **ktlint + detekt + jacoco** configured, sonar/dependency-check steps removed, CI/CD on JDK 17. | User choices. |
| 2026-09-20 | Phase 5 design: `TransactionType` enum (Room's built-in enum-by-name storage, so the column stays TEXT and values are unchanged); money = `Long` cents end to end (`amountCents`, `balanceCents`, `monthlyLimitCents`); input parsed with `BigDecimal` (max 2 decimals, rejects more); **DB v2 migration** recreates `accounts`, `budgets`, `transactions` (copy with `CAST(ROUND(x*100) AS INTEGER)`), because `DROP COLUMN` needs SQLite 3.35 (API 34+) on the framework test helper; migration test via `MigrationTestHelper` + exported schemas. Coverage via AGP `enableUnitTestCoverage` (`createDebugUnitTestCoverageReport`). License headers added to all Kotlin sources. | Exact arithmetic; safe first migration; testable. |
| 2026-09-20 | CI/CD rebuilt (user): Android-only CD (no iOS target in repo), signed AAB+APK to GitHub Release on `v*` tags; signing via Gradle env vars; ktlint/detekt blocking in CI; security = CodeQL + dependency graph/review + gitleaks. | User request; minimal, production-gate focused. |
| 2026-09-20 | Phase 6: Kotlin **2.0.21** (not 2.1) because Hilt 2.52 reads Kotlin metadata ≤ 2.0 and no Hilt version supporting 2.1 was verified; Vico 2.0.0; SQLCipher stays 4.6.1 (16 KB-ready; newer versions need compileSdk 37 / Room 3); targetSdk 35 edge-to-edge handled with insets. | Binary compatibility; minimal risk. |
| 2026-09-21 | Settings: Recover PIN removed (recovery stays on the login screen via "Forgot PIN?"); **dark mode switch** follows the device until changed, then persists (`ThemePreferences`, plain prefs) and applies to login + main UI and system bar icons. | User request. |
| 2026-09-21 | Context `.md` files moved to `intel/`; `cysec.md` created and maintained continuously for security findings. | User request. |
| 2026-09-21 | Security review: fixed CS-01 (monotonic session clock), CS-02 (session check before UI in `MainActivity.onCreate`), CS-03 (`persist-credentials: false` in CI), CS-04 (share cache cleared at session end), CS-05 (Dependabot). Two decisions are left to you: CS-06 FLAG_SECURE (previously declined) and CS-10 (6-digit PIN minimum). | Low-risk fixes applied; UX-affecting changes need confirmation. |
| 2026-09-21 | **CS-06 FLAG_SECURE declined** (accepted risk CS-A4): this is a productivity app, not a finance app. **CS-10 applied:** PINs must be 6–12 digits for setup, reset and sign-in; existing short PINs are not supported (reset through Forgot PIN). | User decisions. |
| 2026-09-21 | PDF deliverables (docs, testing, references) are no longer needed; don't regenerate them. | User decision. |
