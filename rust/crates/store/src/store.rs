/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The open database: one SQLCipher connection behind a mutex.

use std::path::Path;
use std::sync::{Mutex, MutexGuard, PoisonError};

use rusqlite::{Connection, ErrorCode, OpenFlags, OptionalExtension};

use crate::error::{StoreError, sqlite};
use crate::key::{DatabaseKey, KeyPragma};
use crate::migration::{self, FileOrigin};

/// The SQLCipher database, opened with its key.
///
/// A `Store` holds one connection, and each operation locks it for the whole of its database
/// work, so calls from several threads run one after another and never interleave (ADR-0017).
/// `Store` is `Send` and `Sync`: the bridge can share one through an `Arc`.
///
/// Dropping a `Store` closes the database, and SQLCipher wipes its copy of the key. Use
/// [`close`](Self::close) to see whether closing succeeded.
#[derive(Debug)]
pub struct Store {
    connection: Mutex<Connection>,
}

impl Store {
    /// Opens the database at `path` with `key`, and brings it to schema version 2.
    ///
    /// The first open in a process sets SQLCipher up and turns off its own log. Every open then
    /// checks that this build has SQLCipher, hands it the key in v1's raw-key syntax, pins
    /// SQLCipher 4's settings (`PRAGMA cipher_compatibility = 4`), and reads the schema to check
    /// the key, as section 5 of `docs/specs/android-v1-formats.md` describes. Then it brings the
    /// schema to version 2, as v1's Room did (ADR-0018), and turns on foreign keys:
    ///
    /// - A file at version 2 is checked and left as it is.
    /// - A file at version 1 is migrated with v1's own statements, which turn its `REAL` dollars
    ///   into whole cents.
    /// - A file that was absent, or held 0 bytes, gets version 2's empty tables, then switches to
    ///   write-ahead logging, as v1's files did.
    ///
    /// Creating and migrating each run in one transaction. A failed open closes the connection
    /// again, and leaves an existing file as it was.
    ///
    /// # Errors
    ///
    /// - [`StoreError::CipherUnavailable`] if this build's SQLite has no SQLCipher codec.
    /// - [`StoreError::KeyRejected`] if the file doesn't open with `key`: the key is wrong, or the
    ///   file is plaintext, too short to hold a page, or not a database at all.
    /// - [`StoreError::NewerSchema`] if the file's schema version is above 2.
    /// - [`StoreError::UnrecognizedSchema`] if the schema version is negative, if a file that held
    ///   at least one byte is at version 0, or if a version 2 file lacks version 2's Room identity
    ///   hash.
    /// - [`StoreError::Sqlite`] if SQLite can't open the file, run a setup statement, or migrate
    ///   the schema, or, with SQLite's `DatabaseCorrupt` code, if the key opens the first page
    ///   but the schema is damaged.
    pub fn open(path: impl AsRef<Path>, key: &DatabaseKey) -> Result<Self, StoreError> {
        let path = path.as_ref();
        // No SQLITE_OPEN_URI: the path is a file name, never a `file:` URI with options in it.
        let flags = OpenFlags::SQLITE_OPEN_READ_WRITE
            | OpenFlags::SQLITE_OPEN_CREATE
            | OpenFlags::SQLITE_OPEN_NO_MUTEX;
        // Read before SQLite opens the file, which creates it if it is absent.
        let origin = FileOrigin::of(path);
        prepare_sqlcipher()?;
        let mut connection = Connection::open_with_flags(path, flags)
            .map_err(|source| sqlite("open the database file", source))?;
        require_sqlcipher(&connection)?;
        apply_key(&connection, key)?;
        connection
            .execute_batch("PRAGMA cipher_compatibility = 4;")
            .map_err(|source| sqlite("pin SQLCipher 4's settings", source))?;
        check_key(&connection)?;
        migration::bring_to_current(&mut connection, origin)?;
        connection
            .execute_batch("PRAGMA foreign_keys = ON;")
            .map_err(|source| sqlite("turn on foreign keys", source))?;
        Ok(Self {
            connection: Mutex::new(connection),
        })
    }

    /// The schema version SQLite keeps in the file's header (`PRAGMA user_version`), which is 2
    /// once [`open`](Self::open) has succeeded.
    ///
    /// # Errors
    ///
    /// [`StoreError::Poisoned`] if an earlier call panicked while it held the connection, or
    /// [`StoreError::Sqlite`] if SQLite can't read the header.
    pub fn user_version(&self) -> Result<i32, StoreError> {
        self.lock()?
            .pragma_query_value(None, "user_version", |row| row.get(0))
            .map_err(|source| sqlite("read the schema version", source))
    }

    /// Closes the database, and SQLCipher wipes its copy of the key.
    ///
    /// # Errors
    ///
    /// [`StoreError::Poisoned`] if an earlier call panicked while it held the connection, or
    /// [`StoreError::Sqlite`] if SQLite reports an error while closing. The connection is
    /// dropped either way, which closes it.
    pub fn close(self) -> Result<(), StoreError> {
        let connection = self
            .connection
            .into_inner()
            .map_err(|_| StoreError::Poisoned)?;
        connection
            .close()
            .map_err(|(_, source)| sqlite("close the database", source))
    }

    fn lock(&self) -> Result<MutexGuard<'_, Connection>, StoreError> {
        self.connection.lock().map_err(|_| StoreError::Poisoned)
    }
}

/// Whether this process has set SQLCipher up and turned its log off.
///
/// Two races in SQLCipher's process-wide setup make this lock necessary; each crashed or failed
/// `tests/concurrent_open.rs` when the first opens of a process ran at once:
///
/// - SQLite marks itself initialized, then runs SQLCipher's own initialization
///   (`SQLITE_EXTRA_INIT`) outside its lock. Another thread that opens a connection in between
///   finds SQLite initialized, and its `PRAGMA key` fails with "sqlcipher not initialized".
/// - SQLCipher changes its log settings without a lock. `PRAGMA cipher_log` reads the log file's
///   pointer three times before it closes the file, so two of them at once can see the pointer
///   become null between the reads and call `fclose(NULL)`, which crashes the process.
static SQLCIPHER_READY: Mutex<bool> = Mutex::new(false);

/// Sets SQLCipher up and turns its log off, once per process, before any file is opened.
///
/// An in-memory connection, opened while this thread holds `SQLCIPHER_READY`, runs SQLite's and
/// SQLCipher's initialization to the end before any other thread can open a connection. Then the
/// log goes off: when SQLCipher starts, it logs warnings and errors, such as a failed page check,
/// to logcat on Android, the system log on Apple platforms, and stderr elsewhere, and shipped code
/// doesn't log (`CLAUDE.md`, "Security posture"). No store has keyed a connection yet, so nothing
/// is logging; the level still goes to `NONE` before the log file is unset, so a message logged by
/// some other connection at that moment is dropped by the level check.
pub(crate) fn prepare_sqlcipher() -> Result<(), StoreError> {
    // The flag is set only after every step succeeds, so it stays accurate even if a panic in an
    // earlier attempt poisoned the lock.
    let mut ready = SQLCIPHER_READY
        .lock()
        .unwrap_or_else(PoisonError::into_inner);
    if !*ready {
        let setup =
            Connection::open_in_memory().map_err(|source| sqlite("set up SQLCipher", source))?;
        require_sqlcipher(&setup)?;
        setup
            .execute_batch("PRAGMA cipher_log_level = NONE; PRAGMA cipher_log = off;")
            .map_err(|source| sqlite("turn off SQLCipher's log", source))?;
        *ready = true;
    }
    Ok(())
}

/// Refuses a SQLite without SQLCipher, which would ignore `PRAGMA key` and write plaintext.
/// SQLite ignores a pragma it doesn't know and returns no row.
fn require_sqlcipher(connection: &Connection) -> Result<(), StoreError> {
    connection
        .query_row("PRAGMA cipher_version", [], |row| row.get::<_, String>(0))
        .optional()
        .map_err(|source| sqlite("read SQLCipher's version", source))?
        .map(|_| ())
        .ok_or(StoreError::CipherUnavailable)
}

/// Hands SQLCipher the key. It must be the first statement that touches the file.
fn apply_key(connection: &Connection, key: &DatabaseKey) -> Result<(), StoreError> {
    let pragma = KeyPragma::new(key);
    connection
        .execute_batch(pragma.as_str())
        .map_err(|source| sqlite("set the database key", source))
}

/// Reads the schema, which decrypts the first page and checks its authentication code, so a
/// wrong key fails here rather than at the first query. SQLCipher's documentation suggests this
/// query for the check.
fn check_key(connection: &Connection) -> Result<(), StoreError> {
    match connection.query_row("SELECT count(*) FROM sqlite_master", [], |row| {
        row.get::<_, i64>(0)
    }) {
        Ok(_) => Ok(()),
        Err(rusqlite::Error::SqliteFailure(error, _)) if error.code == ErrorCode::NotADatabase => {
            Err(StoreError::KeyRejected)
        }
        Err(source) => Err(sqlite("check the database key", source)),
    }
}

#[cfg(test)]
mod tests {
    use std::collections::BTreeMap;
    use std::fs;
    use std::path::{Path, PathBuf};

    use denarii_core::{
        AccountId, CategoryId, Cents, EpochMillis, Transaction, TransactionId, TransactionKind,
        TransactionType,
    };
    use rusqlite::Connection;
    use rusqlite::types::Value;

    use super::{Store, prepare_sqlcipher};
    use crate::{DatabaseKey, StoreError};

    const PLAINTEXT_HEADER: &[u8] = b"SQLite format 3\0";

    /// Fixture F3, which v1's own seeding and use cases wrote: every transaction type, edits, a
    /// delete, two budgets, and one malformed transfer (`tests/fixtures/README.md`).
    const LEDGER_FIXTURE: &str = concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/tests/fixtures/v1-f3-ledger.db"
    );

    /// Each F3 account's opening balance in cents. v1 keeps it nowhere but in the stored balance,
    /// so it comes from the code that wrote the fixture.
    const LEDGER_OPENING_BALANCES: [(i64, i64); 4] = [(1, 0), (2, 0), (3, 125_000), (4, -30_000)];

    /// A fresh folder for one test, under the system's temporary folder.
    fn scratch_dir(test: &str) -> PathBuf {
        let dir =
            std::env::temp_dir().join(format!("denarii-store-unit-{}-{test}", std::process::id()));
        let _ = fs::remove_dir_all(&dir); // Left over from an earlier run, or absent.
        fs::create_dir_all(&dir).unwrap();
        dir
    }

    fn fixture_key() -> DatabaseKey {
        DatabaseKey::from_bytes(std::array::from_fn(|i| u8::try_from(i).unwrap()))
    }

    fn copy_fixture(dir: &Path) -> PathBuf {
        let path = dir.join("fixture.db");
        fs::copy(
            concat!(
                env!("CARGO_MANIFEST_DIR"),
                "/tests/fixtures/v1-sqlcipher-android-4.19.1.db"
            ),
            &path,
        )
        .unwrap();
        path
    }

    fn copy_ledger_fixture(dir: &Path) -> PathBuf {
        let path = dir.join("ledger.db");
        fs::copy(LEDGER_FIXTURE, &path).unwrap();
        path
    }

    /// A pragma's value, written as text.
    fn pragma(connection: &Connection, name: &str) -> String {
        connection
            .query_row(&format!("PRAGMA {name}"), [], |row| row.get::<_, Value>(0))
            .map(|value| match value {
                Value::Integer(number) => number.to_string(),
                Value::Text(text) => text,
                other => format!("{other:?}"),
            })
            .unwrap()
    }

    /// Every row of `transactions` with its ID, mapped as v1's `toDomainTransactionOrNull` mapped
    /// one: a transfer with no destination maps to `None` (BUG-06).
    fn stored_transactions(connection: &Connection) -> Vec<(i64, Option<Transaction>)> {
        let mut statement = connection
            .prepare(
                "SELECT id, type, description, amountCents, categoryId, accountId, \
                 transferAccountId, dateEpochMillis FROM transactions ORDER BY id",
            )
            .unwrap();
        statement
            .query_map([], |row| {
                let id: i64 = row.get(0)?;
                let stored_type: String = row.get(1)?;
                let transfer_account: Option<i64> = row.get(6)?;
                let kind = TransactionKind::from_stored(
                    TransactionType::parse(&stored_type).unwrap(),
                    transfer_account.map(AccountId),
                );
                let transaction = match kind {
                    None => None,
                    Some(kind) => Some(Transaction {
                        id: Some(TransactionId(id)),
                        description: row.get(2)?,
                        amount: Cents(row.get(3)?),
                        category_id: CategoryId(row.get(4)?),
                        account_id: AccountId(row.get(5)?),
                        date: EpochMillis(row.get(7)?),
                        kind,
                    }),
                };
                Ok((id, transaction))
            })
            .unwrap()
            .collect::<Result<_, _>>()
            .unwrap()
    }

    fn stored_balances(connection: &Connection) -> BTreeMap<AccountId, Cents> {
        let mut statement = connection
            .prepare("SELECT id, balanceCents FROM accounts ORDER BY id")
            .unwrap();
        statement
            .query_map([], |row| Ok((AccountId(row.get(0)?), Cents(row.get(1)?))))
            .unwrap()
            .collect::<Result<_, _>>()
            .unwrap()
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

    fn text(value: &str) -> Value {
        Value::Text(value.to_owned())
    }

    /// Invariant 11 of `docs/specs/store.md`, on the balances v1's own writes left: each stored
    /// balance is the opening balance plus the impact of every transaction still stored, as
    /// `core` computes it. A malformed transfer has no impact (invariant 11 of
    /// `docs/specs/android-v1-formats.md`).
    #[test]
    fn reproduces_every_stored_balance_of_the_f3_fixture_from_its_transactions() {
        let dir =
            scratch_dir("reproduces_every_stored_balance_of_the_f3_fixture_from_its_transactions");
        let store = Store::open(copy_ledger_fixture(&dir), &fixture_key()).unwrap();
        let connection = store.lock().unwrap();
        let mut computed: BTreeMap<AccountId, Cents> = LEDGER_OPENING_BALANCES
            .iter()
            .map(|&(id, cents)| (AccountId(id), Cents(cents)))
            .collect();
        let mut malformed = Vec::new();
        for (id, transaction) in stored_transactions(&connection) {
            let Some(transaction) = transaction else {
                malformed.push(id);
                continue;
            };
            for (account, impact) in transaction.account_impacts().unwrap() {
                let balance = computed.get_mut(&account).unwrap_or_else(|| {
                    panic!("transaction {id} names {account:?}, which has no row")
                });
                *balance = balance.checked_add(impact).unwrap();
            }
        }
        let stored = stored_balances(&connection);
        assert_eq!(computed, stored);
        assert_eq!(
            stored,
            BTreeMap::from([
                (AccountId(1), Cents(-2_300)),
                (AccountId(2), Cents(50_000)),
                (AccountId(3), Cents(269_655)),
                (AccountId(4), Cents(-44_000)),
            ])
        );
        assert_eq!(malformed, [12]);
    }

    #[test]
    fn reads_the_f3_fixtures_names_budgets_and_id_counters() {
        let dir = scratch_dir("reads_the_f3_fixtures_names_budgets_and_id_counters");
        let store = Store::open(copy_ledger_fixture(&dir), &fixture_key()).unwrap();
        let connection = store.lock().unwrap();
        assert_eq!(pragma(&connection, "integrity_check"), "ok");
        let violations = rows(&connection, "PRAGMA foreign_key_check");
        assert_eq!(violations, Vec::<Vec<Value>>::new());
        // The defaults v1 seeds in English, then the accounts and categories its use cases added.
        // Account 5 was added and deleted.
        assert_eq!(
            rows(&connection, "SELECT id, name FROM accounts ORDER BY id"),
            [
                [Value::Integer(1), text("Cash")],
                [Value::Integer(2), text("Savings")],
                [Value::Integer(3), text("Carte bancaire \u{1f4b3}")],
                [Value::Integer(4), text("Carte de cr\u{e9}dit")],
            ]
        );
        assert_eq!(
            rows(
                &connection,
                "SELECT id, name, iconName FROM categories ORDER BY id"
            ),
            [
                [
                    Value::Integer(1),
                    text("General Expense"),
                    text("ic_category_default")
                ],
                [Value::Integer(2), text("General Income"), text("income")],
                [Value::Integer(3), text("Transfer"), text("transfer")],
                [
                    Value::Integer(4),
                    text("\u{c9}picerie \u{1f6d2}"),
                    text("groceries")
                ],
                [Value::Integer(5), text("Restaurants"), text("dining")],
                [Value::Integer(6), text("Salaire"), text("income")],
            ]
        );
        assert_eq!(
            rows(
                &connection,
                "SELECT id, categoryId, monthlyLimitCents, warningThresholdPercent \
                 FROM budgets ORDER BY id"
            ),
            [[1, 4, 40_000, 75], [2, 5, 15_000, 80]].map(|row| row.map(Value::Integer))
        );
        assert_eq!(
            rows(
                &connection,
                "SELECT name, seq FROM sqlite_sequence ORDER BY name"
            ),
            [
                [text("accounts"), Value::Integer(5)],
                [text("budgets"), Value::Integer(2)],
                [text("categories"), Value::Integer(6)],
                [text("transactions"), Value::Integer(12)],
            ]
        );
        // Descriptions are stored trimmed (BUG-08), and the edits replaced rows 9 and 10.
        assert_eq!(
            rows(
                &connection,
                "SELECT id, type, description, amountCents, transferAccountId \
                 FROM transactions WHERE id IN (3, 9, 10, 12) ORDER BY id"
            ),
            [
                [
                    Value::Integer(3),
                    text("EXPENSE"),
                    text("Caf\u{e9} \u{2615}"),
                    Value::Integer(450),
                    Value::Null
                ],
                [
                    Value::Integer(9),
                    text("INCOME"),
                    text("\u{647}\u{62f}\u{64a}\u{629}"),
                    Value::Integer(8_000),
                    Value::Null
                ],
                [
                    Value::Integer(10),
                    text("TRANSFER"),
                    text("Mouvement"),
                    Value::Integer(12_000),
                    Value::Integer(3)
                ],
                [
                    Value::Integer(12),
                    text("TRANSFER"),
                    text("Virement sans destination"),
                    Value::Integer(4_200),
                    Value::Null
                ],
            ]
        );
    }

    /// Invariant 8 of the format spec: the file v1's use cases wrote decrypts and authenticates
    /// with SQLCipher 4's settings, which section 5 of the format spec says v1 relied on.
    #[test]
    fn opens_the_f3_fixture_with_sqlcipher_4_settings_and_foreign_keys_on() {
        let dir = scratch_dir("opens_the_f3_fixture_with_sqlcipher_4_settings_and_foreign_keys_on");
        let path = copy_ledger_fixture(&dir);
        assert!(!fs::read(&path).unwrap().starts_with(PLAINTEXT_HEADER));
        let store = Store::open(&path, &fixture_key()).unwrap();
        assert_eq!(store.user_version().unwrap(), 2);
        let connection = store.lock().unwrap();
        assert_eq!(pragma(&connection, "cipher_page_size"), "4096");
        assert_eq!(pragma(&connection, "cipher_hmac_algorithm"), "HMAC_SHA512");
        assert_eq!(
            pragma(&connection, "cipher_kdf_algorithm"),
            "PBKDF2_HMAC_SHA512"
        );
        assert_eq!(pragma(&connection, "cipher_plaintext_header_size"), "0");
        assert_eq!(pragma(&connection, "foreign_keys"), "1");
        assert_eq!(pragma(&connection, "journal_mode"), "wal");
    }

    #[test]
    fn reads_the_rows_v1_wrote() {
        let dir = scratch_dir("reads_the_rows_v1_wrote");
        let store = Store::open(copy_fixture(&dir), &fixture_key()).unwrap();
        let connection = store.lock().unwrap();
        let mut statement = connection
            .prepare("SELECT id, name, balanceCents FROM accounts ORDER BY id")
            .unwrap();
        let accounts: Vec<(i64, String, i64)> = statement
            .query_map([], |row| Ok((row.get(0)?, row.get(1)?, row.get(2)?)))
            .unwrap()
            .collect::<Result<_, _>>()
            .unwrap();
        assert_eq!(
            accounts,
            [
                (1, "Cash".to_owned(), 1_250),
                (2, "\u{c9}pargne \u{1f4b0}".to_owned(), -4_200)
            ]
        );
        let tables: Vec<String> = connection
            .prepare("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name")
            .unwrap()
            .query_map([], |row| row.get(0))
            .unwrap()
            .collect::<Result<_, _>>()
            .unwrap();
        assert_eq!(
            tables,
            [
                "accounts",
                "budgets",
                "categories",
                "room_master_table",
                "sqlite_sequence",
                "transactions"
            ]
        );
    }

    #[test]
    fn opens_with_sqlcipher_4_settings_and_foreign_keys_on() {
        let dir = scratch_dir("opens_with_sqlcipher_4_settings_and_foreign_keys_on");
        let store = Store::open(copy_fixture(&dir), &fixture_key()).unwrap();
        let connection = store.lock().unwrap();
        let pragma = |name: &str| -> String {
            connection
                .query_row(&format!("PRAGMA {name}"), [], |row| {
                    row.get::<_, rusqlite::types::Value>(0)
                })
                .map(|value| match value {
                    rusqlite::types::Value::Integer(number) => number.to_string(),
                    rusqlite::types::Value::Text(text) => text,
                    other => format!("{other:?}"),
                })
                .unwrap()
        };
        assert!(pragma("cipher_version").starts_with("4."));
        assert_eq!(pragma("cipher_page_size"), "4096");
        assert_eq!(pragma("cipher_hmac_algorithm"), "HMAC_SHA512");
        assert_eq!(pragma("cipher_kdf_algorithm"), "PBKDF2_HMAC_SHA512");
        assert_eq!(pragma("cipher_plaintext_header_size"), "0");
        assert_eq!(pragma("foreign_keys"), "1");
        assert_eq!(pragma("journal_mode"), "wal");
        assert_eq!(pragma("cipher_log_level"), "NONE");
    }

    #[test]
    fn a_new_database_is_encrypted_and_opens_only_with_its_key() {
        let dir = scratch_dir("a_new_database_is_encrypted_and_opens_only_with_its_key");
        let path = dir.join("new.db");
        let store = Store::open(&path, &fixture_key()).unwrap();
        assert_eq!(store.user_version().unwrap(), 2);
        store
            .lock()
            .unwrap()
            .execute_batch("CREATE TABLE notes(text TEXT); INSERT INTO notes VALUES ('kept');")
            .unwrap();
        store.close().unwrap();

        let bytes = fs::read(&path).unwrap();
        assert!(!bytes.starts_with(PLAINTEXT_HEADER));
        assert!(
            !bytes.windows(4).any(|window| window == b"kept"),
            "the row's text is in the file unencrypted"
        );

        let reopened = Store::open(&path, &fixture_key()).unwrap();
        let text: String = reopened
            .lock()
            .unwrap()
            .query_row("SELECT text FROM notes", [], |row| row.get(0))
            .unwrap();
        assert_eq!(text, "kept");
        reopened.close().unwrap();

        let other_key = DatabaseKey::from_bytes([0xa5; DatabaseKey::LEN]);
        assert!(matches!(
            Store::open(&path, &other_key),
            Err(StoreError::KeyRejected)
        ));
        assert_eq!(fs::read(&path).unwrap(), bytes);
    }

    #[test]
    fn rejects_a_plaintext_database_and_leaves_it_unchanged() {
        let dir = scratch_dir("rejects_a_plaintext_database_and_leaves_it_unchanged");
        let path = dir.join("plain.db");
        // A connection with no key writes an ordinary SQLite file, as builds before SQLCipher did.
        // Opening it only after SQLCipher is set up keeps it out of the races in SQLCIPHER_READY.
        prepare_sqlcipher().unwrap();
        let plain = Connection::open(&path).unwrap();
        plain
            .execute_batch("CREATE TABLE notes(text TEXT); INSERT INTO notes VALUES ('plain');")
            .unwrap();
        plain.close().unwrap();
        let before = fs::read(&path).unwrap();
        assert!(before.starts_with(PLAINTEXT_HEADER));

        assert!(matches!(
            Store::open(&path, &fixture_key()),
            Err(StoreError::KeyRejected)
        ));
        assert_eq!(fs::read(&path).unwrap(), before);
    }
}
