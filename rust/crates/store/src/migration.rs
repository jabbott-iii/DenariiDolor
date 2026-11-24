/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Bringing a database to schema version 2 as it opens, as Room did for v1 (ADR-0018).
//!
//! A new file gets version 2's tables and write-ahead logging, a version 1 file is migrated with
//! v1's own statements, and a version 2 file is checked and left as it is. Anything else is
//! refused before anything is written. This is a module of its own because it is the only code
//! that writes to a file before the use cases run, and every write it makes happens in one
//! transaction.

use std::fs;
use std::io;
use std::path::Path;

use rusqlite::{Connection, TransactionBehavior};

use crate::error::{StoreError, sqlite};
use crate::schema;

/// What was at the database's path just before SQLite opened it.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub(crate) enum FileOrigin {
    /// No file, or a file of 0 bytes. Only such a file may get a new schema.
    New,
    /// A file holding at least one byte, or a path whose size couldn't be read.
    Existing,
}

impl FileOrigin {
    /// Looks at `path` before it is opened. Opening creates an absent file, so the answer must be
    /// read first.
    pub(crate) fn of(path: &Path) -> Self {
        match fs::metadata(path) {
            Ok(metadata) if metadata.is_file() && metadata.len() == 0 => Self::New,
            Err(error) if error.kind() == io::ErrorKind::NotFound => Self::New,
            Ok(_) | Err(_) => Self::Existing,
        }
    }
}

/// What the open must do to reach schema version 2.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum Step {
    /// The file is at version 2 already: check Room's identity hash and write nothing.
    Check,
    /// The file is new and empty: create version 2's tables.
    Create,
    /// The file is at version 1: run v1's migration to version 2.
    MigrateFrom1,
}

/// v1's `MIGRATION_1_2` (`app/src/main/java/com/denariidolor/data/local/db/Migrations.kt` at
/// `6d57526`), statement for statement: each money column moves from `REAL` dollars to `INTEGER`
/// cents with `CAST(ROUND(<column> * 100) AS INTEGER)`. Each table is rebuilt rather than altered,
/// as v1 did, because dropping a column needs SQLite 3.35 or later.
const MIGRATE_1_TO_2: &str = concat!(
    "CREATE TABLE IF NOT EXISTS `accounts_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ",
    "`name` TEXT NOT NULL, `balanceCents` INTEGER NOT NULL);",
    "INSERT INTO `accounts_new` (`id`, `name`, `balanceCents`) ",
    "SELECT `id`, `name`, CAST(ROUND(`balance` * 100) AS INTEGER) FROM `accounts`;",
    "DROP TABLE `accounts`;",
    "ALTER TABLE `accounts_new` RENAME TO `accounts`;",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_name` ON `accounts` (`name`);",
    "CREATE TABLE IF NOT EXISTS `budgets_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ",
    "`categoryId` INTEGER NOT NULL, `monthlyLimitCents` INTEGER NOT NULL, ",
    "`warningThresholdPercent` INTEGER NOT NULL, ",
    "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ",
    "ON DELETE CASCADE );",
    "INSERT INTO `budgets_new` (`id`, `categoryId`, `monthlyLimitCents`, ",
    "`warningThresholdPercent`) ",
    "SELECT `id`, `categoryId`, CAST(ROUND(`monthlyLimit` * 100) AS INTEGER), ",
    "`warningThresholdPercent` FROM `budgets`;",
    "DROP TABLE `budgets`;",
    "ALTER TABLE `budgets_new` RENAME TO `budgets`;",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`);",
    "CREATE TABLE IF NOT EXISTS `transactions_new` (",
    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ",
    "`type` TEXT NOT NULL, `description` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, ",
    "`categoryId` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `transferAccountId` INTEGER, ",
    "`dateEpochMillis` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, ",
    "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ",
    "ON DELETE RESTRICT , ",
    "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ",
    "ON DELETE RESTRICT );",
    "INSERT INTO `transactions_new` (`id`, `type`, `description`, `amountCents`, `categoryId`, ",
    "`accountId`, `transferAccountId`, `dateEpochMillis`, `createdAtEpochMillis`) ",
    "SELECT `id`, `type`, `description`, CAST(ROUND(`amount` * 100) AS INTEGER), `categoryId`, ",
    "`accountId`, `transferAccountId`, `dateEpochMillis`, `createdAtEpochMillis` ",
    "FROM `transactions`;",
    "DROP TABLE `transactions`;",
    "ALTER TABLE `transactions_new` RENAME TO `transactions`;",
    "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ",
    "ON `transactions` (`categoryId`);",
    "CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`);",
    "CREATE INDEX IF NOT EXISTS `index_transactions_dateEpochMillis` ",
    "ON `transactions` (`dateEpochMillis`);",
);

/// Brings the keyed `connection` to schema version 2, or refuses the file without writing to it.
///
/// A version 2 file is only read. Creating and migrating happen in one immediate transaction,
/// which takes the write lock before it reads, and the step is chosen again under that lock, so
/// two connections can't both act on what one of them read first. A failure rolls the
/// transaction back, which leaves the file as it was. A file this created then switches to
/// write-ahead logging, which SQLite can't do inside a transaction.
///
/// This leaves foreign keys off after creating or migrating; the caller turns them on.
pub(crate) fn bring_to_current(
    connection: &mut Connection,
    origin: FileOrigin,
) -> Result<(), StoreError> {
    if next_step(connection, origin)? == Step::Check {
        return check_identity(connection);
    }
    // Room ran migrations before it turned foreign keys on, so dropping and renaming the parent
    // tables applies no `RESTRICT` or `CASCADE`. This build turns them on by default
    // (`SQLITE_DEFAULT_FOREIGN_KEYS=1`), and SQLite ignores this pragma inside a transaction.
    connection
        .execute_batch("PRAGMA foreign_keys = OFF;")
        .map_err(|source| sqlite("turn off foreign keys", source))?;
    let transaction = connection
        .transaction_with_behavior(TransactionBehavior::Immediate)
        .map_err(|source| sqlite("start the schema transaction", source))?;
    let step = next_step(&transaction, origin)?;
    match step {
        Step::Check => {}
        Step::Create => {
            transaction
                .execute_batch(schema::CREATE_TABLES)
                .map_err(|source| sqlite("create the schema", source))?;
            record_version_2(&transaction)?;
        }
        Step::MigrateFrom1 => {
            transaction
                .execute_batch(MIGRATE_1_TO_2)
                .map_err(|source| sqlite("migrate the schema from version 1 to 2", source))?;
            record_version_2(&transaction)?;
        }
    }
    check_identity(&transaction)?;
    transaction
        .commit()
        .map_err(|source| sqlite("commit the schema transaction", source))?;
    if step == Step::Create {
        use_write_ahead_log(connection)?;
    }
    Ok(())
}

/// Chooses the step from the schema version, or refuses a file the store doesn't know.
fn next_step(connection: &Connection, origin: FileOrigin) -> Result<Step, StoreError> {
    let version: i32 = connection
        .pragma_query_value(None, "user_version", |row| row.get(0))
        .map_err(|source| sqlite("read the schema version", source))?;
    match version {
        schema::VERSION => Ok(Step::Check),
        1 => Ok(Step::MigrateFrom1),
        0 if origin == FileOrigin::New && schema_is_empty(connection)? => Ok(Step::Create),
        version if version > schema::VERSION => Err(StoreError::NewerSchema { version }),
        version => Err(StoreError::UnrecognizedSchema { version }),
    }
}

fn schema_is_empty(connection: &Connection) -> Result<bool, StoreError> {
    connection
        .query_row("SELECT count(*) = 0 FROM sqlite_master", [], |row| {
            row.get(0)
        })
        .map_err(|source| sqlite("read the schema", source))
}

/// Writes Room's bookkeeping table and the schema version, as Room did after it created or
/// migrated a schema, inside the caller's transaction.
fn record_version_2(connection: &Connection) -> Result<(), StoreError> {
    connection
        .execute_batch(schema::RECORD_IDENTITY)
        .map_err(|source| sqlite("record the schema's identity", source))?;
    connection
        .pragma_update(None, "user_version", schema::VERSION)
        .map_err(|source| sqlite("write the schema version", source))
}

/// Switches a file the store has just created to write-ahead logging, the mode Room gave every
/// file v1 created on most phones (ADR-0018). SQLite records the mode in the file, and later
/// opens keep it. If SQLite can't use write-ahead logging for the file, it keeps the rollback
/// journal and answers with that mode instead of an error. That file is still a correct
/// database, so only an error fails the open.
fn use_write_ahead_log(connection: &Connection) -> Result<(), StoreError> {
    connection
        .query_row("PRAGMA journal_mode = WAL", [], |row| {
            row.get::<_, String>(0)
        })
        .map(|_| ())
        .map_err(|source| sqlite("switch to write-ahead logging", source))
}

/// Refuses a version 2 file unless `room_master_table` holds version 2's identity hash, as Room
/// did. Room failed on another hash; a missing table, which Room would have checked column by
/// column, is refused too (ADR-0018).
fn check_identity(connection: &Connection) -> Result<(), StoreError> {
    let recorded: bool = connection
        .query_row(
            "SELECT count(*) > 0 FROM sqlite_master \
             WHERE type = 'table' AND name = 'room_master_table'",
            [],
            |row| row.get(0),
        )
        .map_err(|source| sqlite("read the schema", source))?;
    let matches = recorded
        && connection
            .query_row(
                "SELECT count(*) > 0 FROM room_master_table WHERE id = 42 AND identity_hash = ?1",
                [schema::VERSION_2_IDENTITY_HASH],
                |row| row.get(0),
            )
            .map_err(|source| sqlite("read the schema's identity", source))?;
    if matches {
        Ok(())
    } else {
        Err(StoreError::UnrecognizedSchema {
            version: schema::VERSION,
        })
    }
}

#[cfg(test)]
mod tests {
    use std::fs;
    use std::path::{Path, PathBuf};

    use rusqlite::Connection;
    use rusqlite::types::Value;

    use super::FileOrigin;
    use crate::key::KeyPragma;
    use crate::store::prepare_sqlcipher;
    use crate::{DatabaseKey, Store, StoreError};

    /// The database v1's own code wrote on an emulator (`tests/fixtures/README.md`).
    const FIXTURE: &str = concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/tests/fixtures/v1-sqlcipher-android-4.19.1.db"
    );

    /// Version 1's tables, as Room created them, with money in `REAL` dollars, and Room's
    /// bookkeeping for version 1 (`app/schemas/com.denariidolor.data.local.db.AppDatabase/1.json`
    /// at `6d57526`).
    const VERSION_1: &str = concat!(
        "CREATE TABLE IF NOT EXISTS `transactions` (",
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ",
        "`type` TEXT NOT NULL, `description` TEXT NOT NULL, `amount` REAL NOT NULL, ",
        "`categoryId` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, ",
        "`transferAccountId` INTEGER, ",
        "`dateEpochMillis` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, ",
        "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ",
        "ON DELETE RESTRICT , ",
        "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ",
        "ON DELETE RESTRICT );",
        "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ",
        "ON `transactions` (`categoryId`);",
        "CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ",
        "ON `transactions` (`accountId`);",
        "CREATE INDEX IF NOT EXISTS `index_transactions_dateEpochMillis` ",
        "ON `transactions` (`dateEpochMillis`);",
        "CREATE TABLE IF NOT EXISTS `categories` (",
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ",
        "`name` TEXT NOT NULL, `iconName` TEXT NOT NULL);",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`);",
        "CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ",
        "`categoryId` INTEGER NOT NULL, `monthlyLimit` REAL NOT NULL, ",
        "`warningThresholdPercent` INTEGER NOT NULL, ",
        "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ",
        "ON DELETE CASCADE );",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`);",
        "CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ",
        "`name` TEXT NOT NULL, `balance` REAL NOT NULL);",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_name` ON `accounts` (`name`);",
        "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT);",
        "INSERT OR REPLACE INTO room_master_table (id,identity_hash) ",
        "VALUES(42, '15f1c4acb81d97b124713b69e7ec869d');",
        "PRAGMA user_version = 1;",
    );

    /// The rows of v1's `MigrationTest.migrate1To2ConvertsMoneyToCentsAndKeepsData`
    /// (`app/src/androidTest/java/com/denariidolor/data/MigrationTest.kt` at `6d57526`).
    const MIGRATION_TEST_ROWS: &str = concat!(
        "INSERT INTO categories (id, name, iconName) VALUES (1, 'Dining', 'dining');",
        "INSERT INTO accounts (id, name, balance) VALUES (1, 'Cash', 12.34), (2, 'Card', -0.1);",
        "INSERT INTO budgets (id, categoryId, monthlyLimit, warningThresholdPercent) ",
        "VALUES (1, 1, 250.5, 75);",
        "INSERT INTO transactions (id, type, description, amount, categoryId, accountId, ",
        "transferAccountId, dateEpochMillis, createdAtEpochMillis) VALUES ",
        "(1, 'EXPENSE', 'Lunch', 19.99, 1, 1, NULL, 100, 200), ",
        "(2, 'TRANSFER', 'Pay card', 0.29, 1, 1, 2, 300, 400);",
    );

    /// A fresh folder for one test, under the system's temporary folder.
    fn scratch_dir(test: &str) -> PathBuf {
        let dir = std::env::temp_dir().join(format!(
            "denarii-store-migration-{}-{test}",
            std::process::id()
        ));
        let _ = fs::remove_dir_all(&dir); // Left over from an earlier run, or absent.
        fs::create_dir_all(&dir).unwrap();
        dir
    }

    /// The fixture's key, a test value: the bytes 0x00 to 0x1f.
    fn fixture_key() -> DatabaseKey {
        DatabaseKey::from_bytes(std::array::from_fn(|i| u8::try_from(i).unwrap()))
    }

    /// A copy of the fixture, so no test opens the committed file: opening a database in
    /// write-ahead-log mode creates files beside it.
    fn copy_fixture(dir: &Path) -> PathBuf {
        let path = dir.join("denarii_dolor.db");
        fs::copy(FIXTURE, &path).unwrap();
        path
    }

    /// A connection keyed as the store keys one, for building and reading files around it.
    /// SQLCipher is set up first, so this connection stays out of the races that
    /// `prepare_sqlcipher` guards against.
    fn keyed(path: &Path) -> Connection {
        prepare_sqlcipher().unwrap();
        let connection = Connection::open(path).unwrap();
        connection
            .execute_batch(KeyPragma::new(&fixture_key()).as_str())
            .unwrap();
        connection
            .execute_batch("PRAGMA cipher_compatibility = 4;")
            .unwrap();
        connection
    }

    /// Writes a version 1 database in write-ahead-log mode, as Room left one on a phone, with
    /// `rows` in it. Closing the connection folds the log back into the file.
    fn write_version_1(path: &Path, rows: &str) {
        let connection = keyed(path);
        connection
            .execute_batch("PRAGMA journal_mode = WAL;")
            .unwrap();
        connection.execute_batch(VERSION_1).unwrap();
        connection.execute_batch(rows).unwrap();
        connection.close().unwrap();
    }

    /// Every row a query returns, each as a list of SQLite values.
    fn rows(connection: &Connection, sql: &str) -> Vec<Vec<Value>> {
        let mut statement = connection.prepare(sql).unwrap();
        let columns = statement.column_count();
        statement
            .query_map([], |row| (0..columns).map(|i| row.get(i)).collect())
            .unwrap()
            .collect::<Result<_, _>>()
            .unwrap()
    }

    fn user_version(connection: &Connection) -> i32 {
        connection
            .pragma_query_value(None, "user_version", |row| row.get(0))
            .unwrap()
    }

    /// What Room's schema check compared for each table: its columns, foreign keys, and indexes.
    fn table_shape(connection: &Connection, table: &str) -> Vec<Vec<Value>> {
        let mut shape = rows(connection, &format!("PRAGMA table_info(`{table}`)"));
        shape.extend(rows(
            connection,
            &format!("PRAGMA foreign_key_list(`{table}`)"),
        ));
        let mut indexes = rows(
            connection,
            &format!(
                "SELECT name FROM sqlite_master \
                 WHERE type = 'index' AND tbl_name = '{table}' ORDER BY name"
            ),
        );
        for index in &mut indexes {
            let Value::Text(name) = &index[0] else {
                panic!("an index name that isn't text: {index:?}");
            };
            let unique = rows(
                connection,
                &format!(
                    "SELECT \"unique\" FROM pragma_index_list('{table}') WHERE name = '{name}'"
                ),
            );
            let columns = rows(connection, &format!("PRAGMA index_info(`{name}`)"));
            index.extend(unique.into_iter().flatten());
            index.extend(columns.into_iter().flatten());
        }
        shape.extend(indexes);
        shape
    }

    fn assert_unrecognized(path: &Path, version: i32) {
        let before = fs::read(path).unwrap();
        let result = Store::open(path, &fixture_key());
        let Err(StoreError::UnrecognizedSchema { version: found }) = &result else {
            panic!("expected an unrecognized schema: {result:?}");
        };
        assert_eq!(*found, version);
        assert_eq!(fs::read(path).unwrap(), before, "the file changed");
    }

    #[test]
    fn a_new_database_gets_the_same_schema_v1_creates() {
        let dir = scratch_dir("a_new_database_gets_the_same_schema_v1_creates");
        let absent = dir.join("absent.db");
        let empty = dir.join("empty.db");
        fs::write(&empty, b"").unwrap();
        let fixture = keyed(&copy_fixture(&dir));
        let schema = "SELECT type, name, tbl_name, rootpage, sql FROM sqlite_master ORDER BY rowid";
        let identity = "SELECT id, identity_hash FROM room_master_table";
        let journal_mode = "PRAGMA journal_mode";
        for path in [&absent, &empty] {
            Store::open(path, &fixture_key()).unwrap().close().unwrap();
            let created = keyed(path);
            assert_eq!(rows(&created, schema), rows(&fixture, schema), "{path:?}");
            assert_eq!(
                rows(&created, identity),
                rows(&fixture, identity),
                "{path:?}"
            );
            assert_eq!(user_version(&created), 2, "{path:?}");
            assert_eq!(
                rows(&created, journal_mode),
                rows(&fixture, journal_mode),
                "{path:?}"
            );
            for table in ["accounts", "budgets", "categories", "transactions"] {
                let count = format!("SELECT count(*) FROM `{table}`");
                assert_eq!(rows(&created, &count), [[Value::Integer(0)]], "{table}");
            }
        }
    }

    #[test]
    fn leaves_a_version_2_file_as_it_is() {
        let dir = scratch_dir("leaves_a_version_2_file_as_it_is");
        let path = copy_fixture(&dir);
        let store = Store::open(&path, &fixture_key()).unwrap();
        assert_eq!(store.user_version().unwrap(), 2);
        store.close().unwrap();
        assert_eq!(fs::read(&path).unwrap(), fs::read(FIXTURE).unwrap());
    }

    #[test]
    fn migrates_version_1_to_the_cents_v1s_migration_test_expects() {
        let dir = scratch_dir("migrates_version_1_to_the_cents_v1s_migration_test_expects");
        let path = dir.join("denarii_dolor.db");
        write_version_1(&path, MIGRATION_TEST_ROWS);

        let store = Store::open(&path, &fixture_key()).unwrap();
        assert_eq!(store.user_version().unwrap(), 2);
        store.close().unwrap();

        let migrated = keyed(&path);
        assert_eq!(
            rows(
                &migrated,
                "SELECT id, name, balanceCents FROM accounts ORDER BY id"
            ),
            [
                [1.into(), Value::Text("Cash".into()), 1_234.into()],
                [2.into(), Value::Text("Card".into()), (-10).into()],
            ]
        );
        assert_eq!(
            rows(
                &migrated,
                "SELECT id, categoryId, monthlyLimitCents, warningThresholdPercent FROM budgets"
            ),
            [[1.into(), 1.into(), 25_050.into(), 75.into()]]
        );
        assert_eq!(
            rows(
                &migrated,
                "SELECT id, type, description, amountCents, categoryId, accountId, \
                 transferAccountId, dateEpochMillis, createdAtEpochMillis \
                 FROM transactions ORDER BY id"
            ),
            [
                [
                    1.into(),
                    Value::Text("EXPENSE".into()),
                    Value::Text("Lunch".into()),
                    1_999.into(),
                    1.into(),
                    1.into(),
                    Value::Null,
                    100.into(),
                    200.into(),
                ],
                [
                    2.into(),
                    Value::Text("TRANSFER".into()),
                    Value::Text("Pay card".into()),
                    29.into(),
                    1.into(),
                    1.into(),
                    2.into(),
                    300.into(),
                    400.into(),
                ],
            ]
        );
        assert_eq!(
            rows(&migrated, "SELECT id, name, iconName FROM categories"),
            [[
                1.into(),
                Value::Text("Dining".into()),
                Value::Text("dining".into())
            ]]
        );
        let violations = rows(&migrated, "PRAGMA foreign_key_check");
        assert_eq!(violations, Vec::<Vec<Value>>::new());
        assert_eq!(
            rows(&migrated, "SELECT id, identity_hash FROM room_master_table"),
            [[
                42.into(),
                Value::Text("425274f1a5ac9232a36cc07f4f479e7c".into())
            ]]
        );
    }

    #[test]
    fn migrated_tables_have_the_columns_keys_and_indexes_of_v1s_own_version_2_file() {
        let dir = scratch_dir("migrated_tables_have_the_columns_keys_and_indexes");
        let path = dir.join("migrated.db");
        write_version_1(&path, MIGRATION_TEST_ROWS);
        Store::open(&path, &fixture_key()).unwrap().close().unwrap();

        let migrated = keyed(&path);
        let fixture = keyed(&copy_fixture(&dir));
        let tables = "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name";
        assert_eq!(rows(&migrated, tables), rows(&fixture, tables));
        for table in ["accounts", "budgets", "categories", "transactions"] {
            assert_eq!(
                table_shape(&migrated, table),
                table_shape(&fixture, table),
                "{table}"
            );
        }
    }

    /// `ROUND` with no digits adds or subtracts 0.5 and drops the fraction, so a half rounds away
    /// from zero. It works on the product in binary: 1.005 is stored a little below 1.005, and
    /// times 100 it stays below 100.5, so it rounds down. 19.995 is stored a little below 19.995
    /// too, but the product rounds to exactly 1999.5, which rounds up. 0.005 and 19.995 are the
    /// values the format spec names for fixture F4, which M2.3 makes with v1 itself.
    #[test]
    fn migration_rounds_dollars_to_cents_as_v1s_statements_do() {
        let dir = scratch_dir("migration_rounds_dollars_to_cents_as_v1s_statements_do");
        let path = dir.join("denarii_dolor.db");
        let dollars_to_cents = [
            ("0.005", 1),
            ("19.995", 2_000),
            ("-0.005", -1),
            ("1.005", 100),
            ("0.0049", 0),
            ("-12.345", -1_235),
            ("100000000.01", 10_000_000_001),
        ];
        let accounts: Vec<String> = dollars_to_cents
            .iter()
            .enumerate()
            .map(|(i, (dollars, _))| format!("({}, 'Account {i}', {dollars})", i + 1))
            .collect();
        write_version_1(
            &path,
            &format!(
                "INSERT INTO accounts (id, name, balance) VALUES {};",
                accounts.join(", ")
            ),
        );
        Store::open(&path, &fixture_key()).unwrap().close().unwrap();

        let expected: Vec<Vec<Value>> = dollars_to_cents
            .iter()
            .map(|&(_, cents)| vec![Value::Integer(cents)])
            .collect();
        assert_eq!(
            rows(
                &keyed(&path),
                "SELECT balanceCents FROM accounts ORDER BY id"
            ),
            expected
        );
    }

    /// Dropping a table deletes its `sqlite_sequence` row, and copying the rows into the new
    /// table records only the highest ID copied, or 0 when no row is copied. v1's migration had
    /// the same effect, so an ID deleted before it can be handed out again.
    #[test]
    fn migration_resets_each_autoincrement_counter_to_the_highest_id_left() {
        let dir = scratch_dir("migration_resets_each_autoincrement_counter");
        let path = dir.join("denarii_dolor.db");
        write_version_1(
            &path,
            "INSERT INTO accounts (id, name, balance) VALUES (1, 'Cash', 0), (5, 'Old', 0);\
             DELETE FROM accounts WHERE id = 5;",
        );
        let sequence = "SELECT name, seq FROM sqlite_sequence ORDER BY name";
        assert_eq!(
            rows(&keyed(&path), sequence),
            [[Value::Text("accounts".into()), 5.into()]]
        );
        Store::open(&path, &fixture_key()).unwrap().close().unwrap();
        assert_eq!(
            rows(&keyed(&path), sequence),
            [
                [Value::Text("accounts".into()), 1.into()],
                [Value::Text("budgets".into()), 0.into()],
                [Value::Text("transactions".into()), 0.into()],
            ]
        );
    }

    #[test]
    fn a_failed_migration_leaves_the_version_1_file_unchanged() {
        let dir = scratch_dir("a_failed_migration_leaves_the_version_1_file_unchanged");
        let path = dir.join("denarii_dolor.db");
        // `accounts` migrates first; the missing `budgets` then fails the migration, and the
        // rollback must undo `accounts` too.
        write_version_1(
            &path,
            "INSERT INTO accounts VALUES (1, 'Cash', 1.5); DROP TABLE budgets;",
        );
        let before = fs::read(&path).unwrap();

        let result = Store::open(&path, &fixture_key());
        assert!(
            matches!(
                result,
                Err(StoreError::Sqlite {
                    operation: "migrate the schema from version 1 to 2",
                    ..
                })
            ),
            "{result:?}"
        );
        assert_eq!(fs::read(&path).unwrap(), before);
        let unchanged = keyed(&path);
        assert_eq!(user_version(&unchanged), 1);
        assert_eq!(
            rows(&unchanged, "SELECT id, name, balance FROM accounts"),
            [[1.into(), Value::Text("Cash".into()), Value::Real(1.5)]]
        );
    }

    #[test]
    fn refuses_a_newer_schema_without_writing_to_it() {
        let dir = scratch_dir("refuses_a_newer_schema_without_writing_to_it");
        let path = copy_fixture(&dir);
        keyed(&path)
            .execute_batch("PRAGMA user_version = 3;")
            .unwrap();
        let before = fs::read(&path).unwrap();
        let result = Store::open(&path, &fixture_key());
        assert!(
            matches!(result, Err(StoreError::NewerSchema { version: 3 })),
            "{result:?}"
        );
        assert_eq!(fs::read(&path).unwrap(), before);
    }

    #[test]
    fn refuses_a_negative_schema_version() {
        let dir = scratch_dir("refuses_a_negative_schema_version");
        let path = copy_fixture(&dir);
        keyed(&path)
            .execute_batch("PRAGMA user_version = -1;")
            .unwrap();
        assert_unrecognized(&path, -1);
    }

    #[test]
    fn refuses_version_2_without_version_2s_identity_hash() {
        let dir = scratch_dir("refuses_version_2_without_version_2s_identity_hash");
        let other_hash = copy_fixture(&dir);
        keyed(&other_hash)
            .execute_batch(
                "UPDATE room_master_table SET identity_hash = '15f1c4acb81d97b124713b69e7ec869d';",
            )
            .unwrap();
        assert_unrecognized(&other_hash, 2);

        let no_table = dir.join("no-table.db");
        fs::copy(FIXTURE, &no_table).unwrap();
        keyed(&no_table)
            .execute_batch("DROP TABLE room_master_table;")
            .unwrap();
        assert_unrecognized(&no_table, 2);
    }

    #[test]
    fn refuses_a_version_0_file_that_held_bytes_before_it_opened() {
        let dir = scratch_dir("refuses_a_version_0_file_that_held_bytes");
        let with_table = dir.join("with-table.db");
        keyed(&with_table)
            .execute_batch("CREATE TABLE notes(text TEXT);")
            .unwrap();
        assert_unrecognized(&with_table, 0);

        // SQLite reads a 1-byte file as an empty database, which isn't proof of a new install.
        let one_byte = dir.join("one-byte.db");
        fs::write(&one_byte, &fs::read(FIXTURE).unwrap()[..1]).unwrap();
        assert_unrecognized(&one_byte, 0);
    }

    #[test]
    fn only_an_absent_or_empty_file_counts_as_new() {
        let dir = scratch_dir("only_an_absent_or_empty_file_counts_as_new");
        let empty = dir.join("empty.db");
        let one_byte = dir.join("one-byte.db");
        fs::write(&empty, b"").unwrap();
        fs::write(&one_byte, b"S").unwrap();
        assert_eq!(FileOrigin::of(&dir.join("absent.db")), FileOrigin::New);
        assert_eq!(FileOrigin::of(&empty), FileOrigin::New);
        assert_eq!(FileOrigin::of(&one_byte), FileOrigin::Existing);
        assert_eq!(FileOrigin::of(&dir), FileOrigin::Existing);
    }
}
