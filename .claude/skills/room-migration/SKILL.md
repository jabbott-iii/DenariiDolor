---
name: room-migration
description: Change Denarii Dolor's Room schema (an entity, column, index, foreign key or trigger) without losing user data. Covers the version bump, the Migration, the exported schema JSON, MigrationTest, and the matching backup-format change that keeps old backup files restorable. Use for any edit to an @Entity, AppDatabase, Migrations.kt, BackupSnapshot or BackupCodec.
---

# Change the Room schema

Users have data in every schema version that has shipped, and they restore backup files made by old versions. A mistake
here destroys financial records that can't be recovered, so follow every step.

Before you start, confirm the design with the maintainer: the new columns, their defaults, how existing rows are filled, and
how the change shows up in backups.

## 1. Entities and the version

- Edit the entity in `data/local/db/entity/`. Money columns are `Long` cents named `…Cents`, and enums are stored by name.
- Bump `version` in `AppDatabase` by exactly one, and keep `exportSchema = true`.

## 2. Write the migration

Write it in `data/local/db/Migrations.kt`, following the style of `MIGRATION_1_2`:

```kotlin
/** v2 → v3: <what changes and why>. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("…")
    }
}
```

The SQL rules:

- It must run on the **platform SQLite of API 26** (3.18), because `MigrationTestHelper` uses it.
  - No `DROP COLUMN` (3.35) and no `RENAME COLUMN` (3.25).
  - To change or remove a column, rebuild the table: `CREATE TABLE x_new …`, `INSERT INTO x_new SELECT …`,
    `DROP TABLE x`, `ALTER TABLE x_new RENAME TO x`, then re-create the indexes.
  - Room runs migrations before it enables foreign keys, so parent tables can be swapped.
- An `ADD COLUMN … NOT NULL` needs a `DEFAULT` that matches the entity's `@ColumnInfo(defaultValue = …)`.
- Copy the DDL Room generates (table, indexes, foreign keys) from the new schema JSON after step 4, so that
  `runMigrationsAndValidate` matches it exactly.
- Triggers and checks that Room doesn't model must be created both in the migration and for new installs, with a
  `RoomDatabase.Callback.onCreate` that `DatabaseHolder.open` registers.
- Use only literal SQL and bound values. Never interpolate user data.

Never edit a migration that has shipped, and never add `fallbackToDestructiveMigration()`.

## 3. Register it

In `DatabaseHolder.open`, change `.addMigrations(MIGRATION_1_2)` to `.addMigrations(MIGRATION_1_2, MIGRATION_2_3)`.

## 4. Export the schema

```bash
./gradlew assembleDebug
git status app/schemas
```

The new `app/schemas/com.denariidolor.data.local.db.AppDatabase/<n>.json` must exist, and it ships with the change. Don't
hand-edit any schema JSON.

## 5. Test the migration

In `app/src/androidTest/java/com/denariidolor/data/MigrationTest.kt`, add `migrate2To3…`:

1. `helper.createDatabase(dbName, 2)` and insert realistic rows with raw SQL, including edge cases: a TRANSFER row with and
   without `transferAccountId`, negative balances, and the seeded IDs 1–3.
2. `helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3)`.
3. Assert every value that was carried over or derived.

Also add a test that migrates from **v1 to the latest version** through every migration in a row, so the whole chain is
covered.

If a DAO changed, update `TransactionDaoTest` or the matching instrumented test, and the fakes in
`app/src/test/.../testutil/FakeRepositories.kt`.

## 6. Carry the change into backups (CS-28)

Backups hold a snapshot of every table, and users restore files made by older app versions.

- `data/local/db/dao/BackupDao.kt`: the whole-table reads and writes.
- `data/backup/BackupSnapshot.kt`: the records, and the `isConsistent` checks for the new rules.
- `data/backup/BackupStore.kt`: `read` and `replaceAll`, inside `withTransaction`.
- `data/backup/BackupCodec.kt` and `BackupFile.kt`:
  - Bump `BackupFile.FORMAT_VERSION`.
  - `decode` must still read **every** earlier version and fill new fields with the same defaults as the migration.
  - A file from a newer version still fails with `BackupError.NEWER_VERSION`.
- Tests: add `BackupFileTest` and `BackupSnapshotTest` cases (JVM, samples in `BackupSamples.kt`) that decode an
  old-version file, and extend `BackupStoreTest` (instrumented).

## 7. Check the rest

- Update the domain mapping (`TransactionEntityMappings.kt`), the validation, and the read paths that tolerate old data.
- Update the CSV and PDF exports if the new data belongs in reports.
- If the change alters what is stored, update `PRIVACY.md`.
- Update the schema version number in `.claude/rules/data-persistence.md`.
- Run the `ci-gates` skill. The instrumented migration tests are mandatory, so if no device is attached, say that CI must
  run them.
- Ask the maintainer to do a manual update test: install the current release build with data, update to the new build,
  and confirm the data and balances are intact.
