# Denarii Dolor — Repository Map (`map.md`)

A concise map of the repository and its main flows. The architecture rules are in [`maint.md`](maint.md).

## Repository layout

| Path | Contents |
|---|---|
| `app/build.gradle.kts` | Android config (min 26 / target 35), R8, env-based release signing, ktlint and detekt setup |
| `app/proguard-rules.pro` | R8 keep rules (the only keep file AGP reads) |
| `app/schemas/` | Exported Room schemas `1.json` and `2.json`; also an `androidTest` asset for `MigrationTest` |
| `app/src/main/AndroidManifest.xml` | `LoginActivity` (launcher), `MainActivity`, FileProvider; `allowBackup=false` |
| `app/src/main/res/xml/` | Backup and data-extraction exclusions; FileProvider path `cache/reports/` |
| `app/src/main/java/com/denariidolor/` | App source (below) |
| `app/src/test/` | JVM unit tests; fakes in `testutil/` |
| `app/src/androidTest/` | Room/SQLCipher, migration, repository, PDF and Compose UI tests |
| `gradle/libs.versions.toml` | Version catalog (the only place versions are set) |
| `config/detekt/detekt.yml` | detekt overrides on top of the default rules |
| `.github/workflows/` | `ci.yml`, `security.yml`, `cd.yml` |
| `.github/dependabot.yml` | Weekly Gradle and Actions updates |
| `Makefile` | `make release VERSION=vX.Y.Z` → tag → CD |
| `intel/` | `maint.md`, `map.md`, `cybersec.md`, `notes.md`, `plan.md`, `history.md` |
| `AGENTS.md`, `CONTRIBUTING.md`, `README.md` | Agent rules, contribution rules, user and developer overview |

## Source packages (`com.denariidolor`)

| Package | Key components |
|---|---|
| (root) | `App` (`@HiltAndroidApp`), `MainActivity` (session gate + Compose host) |
| `domain/model` | `Transaction` → `Expense` / `Income` / `Transfer`; `TransactionType`; `Account`, `Category`, `Budget`; `Ledger`; `SearchFilters`; `MonthlyReport`; entity ↔ domain mappings |
| `domain/usecase` | `Add`/`Update`/`Delete`/`Validate`/`SearchTransactionUseCase`, `GenerateReportUseCase`, `Category`/`Account`/`BudgetUseCases` |
| `domain/report` | `ReportCsvFormatter` (formula-injection guard), `ReportText` |
| `data/local/db` | `AppDatabase` (v2), `entity/`, `dao/`, `Migrations.kt` (`MIGRATION_1_2`), `DefaultDataInitializer`, `security/` (`DatabaseKeyProvider`, `DatabaseKeys`) |
| `data/local/preferences` | `SecurityProfileService` (PIN/answer hashing, lockout; pure Kotlin), `EncryptedPreferencesManager` (`secure_prefs`), `ThemePreferences` (`ui_prefs`) |
| `data/repository` | `Transaction`/`Category`/`Budget`/`AccountRepository` + `Impl` |
| `data/export` | `ReportExporter` (SAF save, FileProvider share, share-cache cleanup), `ReportPdfRenderer` |
| `di` | `AppModule`, `DatabaseModule`, `RepositoryModule` |
| `presentation/ui` | `AppScreens.kt` (NavHost), `LoginScreen.kt`, `TransactionFormScreen.kt`, `SettingsScreen.kt`; feature packages `auth`, `dashboard`, `search`, `report`, `transaction`, `category`, `account`, `budget`, `settings`, `common` |
| `util` | `Money`, `Validators`, `DateUtils`, `SessionManager`, `Constants`, `ResultExt` |

## Layer dependencies

```mermaid
flowchart LR
    subgraph Presentation
        UI[Compose screens] --> VM[ViewModels]
    end
    subgraph Domain
        UC[Use cases]
        M[Transaction model<br/>+ Ledger]
    end
    subgraph Data
        R[Repository interfaces] -.implemented by.-> RI[Repository Impls]
        RI --> DAO[DAOs] --> DB[(Room + SQLCipher<br/>denarii_dolor.db)]
        EX[ReportExporter]
        P[Encrypted prefs]
    end
    VM --> UC
    VM --> R
    VM --> EX
    UC --> R
    UC --> M
    RI --> M
    DI[Hilt modules] -.wires.-> RI & DB
    KP[DatabaseKeyProvider<br/>db_key_prefs] --> DB
```

## Sign-in and session

```mermaid
flowchart TD
    L[LoginActivity<br/>clears share cache, seeds defaults] --> S{Profile configured?}
    S -- no --> SETUP[Setup: PIN 6–12 digits<br/>+ security question] --> SI
    S -- yes --> SI[Sign in]
    SI -- PIN ok / strong biometric --> AUTH[SessionManager.markAuthenticated]
    SI -- 5 failures --> LOCK[Lockout 30 s, doubling to 15 min]
    SI -- Forgot PIN --> REC[Answer question + new PIN] --> SI
    SI -- Wipe --> WIPE[Clear DB, secure prefs, cache<br/>reseed defaults] --> S
    AUTH --> MAIN[MainActivity]
    MAIN -- no session in onCreate / onResume,<br/>idle > 5 min, sign-out --> OUT[invalidate + clear share cache] --> L
```

## Saving a transaction

```mermaid
sequenceDiagram
    participant F as TransactionFormScreen
    participant VM as TransactionViewModel
    participant UC as Add/UpdateTransactionUseCase
    participant V as ValidateTransactionUseCase
    participant R as TransactionRepositoryImpl
    participant DB as Room (SQLCipher)
    F->>VM: saveTransaction(...)
    VM->>UC: Expense / Income / Transfer
    UC->>V: validate(entity)
    V->>DB: check references + monthly budget
    V-->>UC: Result
    UC->>R: add / update
    R->>DB: withTransaction: write row, apply Ledger.balanceDeltas
    R-->>UC: id or ok
    UC-->>VM: Result
    VM-->>F: event Saved / Failed
```

## Data model (schema v2)

```mermaid
erDiagram
    ACCOUNTS ||--o{ TRANSACTIONS : "accountId (RESTRICT)"
    ACCOUNTS ||--o{ TRANSACTIONS : "transferAccountId (no FK)"
    CATEGORIES ||--o{ TRANSACTIONS : "categoryId (RESTRICT)"
    CATEGORIES ||--o| BUDGETS : "categoryId (CASCADE, unique)"
    ACCOUNTS { long id PK
        string name UK
        long balanceCents }
    CATEGORIES { long id PK
        string name UK
        string iconName }
    BUDGETS { long id PK
        long categoryId FK
        long monthlyLimitCents
        int warningThresholdPercent }
    TRANSACTIONS { long id PK
        string type
        string description
        long amountCents
        long categoryId FK
        long accountId FK
        long transferAccountId
        long dateEpochMillis
        long createdAtEpochMillis }
```

## CI/CD

| Workflow | Trigger | Jobs |
|---|---|---|
| `ci.yml` | push/PR to `main`, manual | ktlint → detekt → lint → unit tests → JaCoCo → `assembleRelease`; then emulator tests on API 26 and 35 |
| `security.yml` | push/PR to `main`, Mondays 02:00 UTC, manual | CodeQL (`security-extended`), dependency graph (push/schedule), dependency review (PR, fails on high), gitleaks |
| `cd.yml` | `v*` tag | unit tests → signed AAB/APK → signature check → GitHub Release with `SHA256SUMS.txt`; private R8 mapping artifact |
