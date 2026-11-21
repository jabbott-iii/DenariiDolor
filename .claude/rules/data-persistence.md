---
paths:
  - "app/src/main/java/com/denariidolor/data/**"
  - "app/src/main/java/com/denariidolor/di/**"
  - "app/src/main/java/com/denariidolor/domain/**"
  - "app/schemas/**"
  - "app/src/androidTest/java/com/denariidolor/data/**"
---

# Data layer: Room, repositories, DI, export and backup

## Room

- `AppDatabase` is `denarii_dolor.db`, at schema v2, with `exportSchema = true`.
  - Its tables are `transactions`, `categories`, `budgets` and `accounts`.
  - The schema JSON in `app/schemas/` is committed, and it is an androidTest asset for `MigrationTest`.
- **Queries:**
  - Use Room `@Query` with bound `:params` only. Never concatenate SQL, and never pass user input to `@RawQuery`.
  - User text in a `LIKE` goes through `escapeLike()` with `ESCAPE '\'` (BUG-07).
- **DAOs:**
  - Use `suspend` for one-shot reads and writes, and `Flow` for observed reads.
  - Load only what the screen shows, such as a month's date range or `observeRecent(limit)`. Never load a whole table for the
    UI (BUG-10).
- **Transactions:**
  - Multi-step writes go inside `database.withTransaction { }`.
  - Adding, editing or deleting a transaction always applies `Ledger.balanceDeltas(previous, current)` in the same
    transaction.
- **Foreign keys:**
  - `transactions.categoryId` and `transactions.accountId` use `RESTRICT`.
  - A budget cascades when its category is deleted, and there is one budget per category.
  - `transactions.transferAccountId` has **no** foreign key. `AccountUseCases` guards it through `countByAccount`, which
    counts both account columns. Keep that check if account deletion changes.
- **Uniqueness:** category and account names have unique indexes. The duplicate checks in the use cases are case-insensitive
  (`LOWER(name)`).
- **Enums are stored by name** (`TransactionType`). Renaming an enum constant is a data migration.
- **Reading rows:**
  - Read paths map rows with `toDomainTransactionOrNull()`, which tolerates a TRANSFER row without a destination (BUG-06).
  - Use `toDomainTransaction()` only for rows that were just validated.

## Schema changes

Use the `room-migration` skill. The rules that never bend:

- Never add `fallbackToDestructiveMigration()`.
- Never edit a migration that has shipped, or a committed schema JSON.
- Every migration is registered in `DatabaseHolder.open(...)` and tested by `MigrationTest`.
- Every schema change is carried into the backup format (CS-28), and old backup files must still restore.
- Write migration SQL that runs on API 26's platform SQLite, because `MigrationTestHelper` uses it there. That means no
  `DROP COLUMN` or `RENAME COLUMN`: rebuild the table instead, as `MIGRATION_1_2` does.

The next planned migration (v3) adds a database-level guard for TRANSFER rows without `transferAccountId`. It's a
`BEFORE INSERT/UPDATE` trigger, created both in the migration and for new installs, plus a step that repairs or reports
existing bad rows.

## DatabaseHolder and DI

- `DatabaseModule` provides `AppDatabase` and the DAOs **unscoped**, from `DatabaseHolder.database`, which throws while the
  vault is locked.
  - Code that outlives a session injects `DatabaseHolder` and reads it when it runs: `@Singleton`s,
    `DefaultDataInitializer`, `BackupManager`.
  - Never cache `AppDatabase` or a DAO in a field that outlives sign-out.
- The database is opened, closed and deleted only through `DatabaseHolder`, which serializes these steps on one worker thread.
- **A new repository:**
  1. Write the interface and its `…Impl @Inject constructor(...)` in the same file in `data/repository`.
  2. `@Binds` it in `RepositoryModule`, unscoped.
  3. Add a fake to `app/src/test/java/com/denariidolor/testutil/FakeRepositories.kt`.
- **A new use case:** an `@Inject constructor` class in `domain/usecase` that returns `Result<T>` through
  `runSuspendCatching`. Group related operations the way `CategoryUseCases` does.

## Domain layer

- `domain/` doesn't import `android.*` or Room annotations. It reads entities only through the mapping in
  `TransactionEntityMappings.kt`.
- A new transaction behavior goes into the `Transaction` subclasses (`balanceImpact()`, `accountImpacts()`), not into a
  `when` on `TransactionType` at the call site.
- The rules in `Validators` and `ValidateTransactionUseCase` are user-facing contracts. A description is 1–200 characters
  after trimming, an amount is greater than 0, and a transfer's destination is required and different from its source.
  Changing a rule is a product decision, so ask first.

## Preferences

- Non-sensitive UI settings go in plain `ui_prefs` (`ThemePreferences`, `CurrencyPreferences`).
- Anything about credentials, keys or financial data goes in the vault or the encrypted database, never in plain prefs.
- Don't migrate to DataStore unless you're asked.

## Export and backup

These are security-sensitive; see `security.md`.

- **Reports:**
  - **Save** writes through a Storage Access Framework `CreateDocument` URI.
  - **Share** uses a FileProvider URI under `cache/reports/`, which is cleared at the end of every session (CS-04).
  - No storage permissions.
- **CSV** goes through `ReportCsvFormatter`, which has the formula-injection guard (CS-20) and keeps English column keys.
  **PDF** goes through `ReportPdfRenderer` (`PdfDocument`), with column widths from `PdfColumnWidths`.
- **Backups (CS-28):**
  - A backup is encrypted under the user's passphrase (12–256 characters), carries a version, and passes
    `BackupSnapshot.isConsistent` before `BackupStore.replaceAll` runs in one transaction.
  - A restore is all or nothing.
  - Never add a path that skips encryption, the version check or validation.
