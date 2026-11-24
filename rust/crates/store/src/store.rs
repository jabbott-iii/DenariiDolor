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
    use std::fs;
    use std::path::{Path, PathBuf};

    use rusqlite::Connection;

    use super::{Store, prepare_sqlcipher};
    use crate::{DatabaseKey, StoreError};

    const PLAINTEXT_HEADER: &[u8] = b"SQLite format 3\0";

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
