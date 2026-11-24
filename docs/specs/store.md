# Subsystem: `store`

`denarii-store` (`rust/crates/store`) owns the SQLCipher database: it opens the file that v1, the
Kotlin app, left on users' phones, with the key the vault unwraps at sign-in, and runs every query
and write on it. It guarantees that the file opens only with its key, that opening writes to a
file only to bring it to schema version 2 and changes nothing when it fails, and that calls from
several threads never interleave on its one connection. ADR-0006 (the database stays SQLCipher,
opened from Rust with `rusqlite`), ADR-0017 (SQLCipher and OpenSSL compiled from source on every
target, and one connection behind a mutex), and ADR-0018 (a new schema only in an absent or empty
file, and no write to a schema the store doesn't know) shaped it. Sections 5 and 6 of
`docs/specs/android-v1-formats.md` describe the file it must read. Opening and closing were built
in M2.1, and the schema and its migration in M2.2, as described here; the use cases land in M2.4
to M2.6. The API reference is `cargo doc -p denarii-store --open`, run from `rust/`.

## Why this design

v1's records are in a SQLCipher 4 database: SQLite with every 4,096-byte page encrypted with
AES-256 and checked with an HMAC-SHA512 authentication code, so a wrong key or a changed byte is
detected rather than read as data. Existing installs upgrade in place (ADR-0002), so the new app
must open that exact file. Opening it from Rust keeps the storage rules in one language with the
domain rules in `core`, where `cargo test` covers them on the host.

`rusqlite` compiles SQLCipher into the Rust library with its own copy of OpenSSL's `libcrypto`,
because Android offers apps no crypto library to link. The same build runs on the host, so the
tests exercise the code that phones run.

What it trades away:

- **Native code and size.** SQLCipher, SQLite, and OpenSSL are C code compiled into the library:
  about 6 MB per Android ABI, against v1's 2.1 MB `libsqlcipher.so` (ADR-0017).
- **Build time.** A build from an empty target folder compiles OpenSSL and SQLCipher, which takes
  more than a minute, and needs `perl` and `make`.
- **One call at a time.** Every operation waits for the one before it, so a slow query delays the
  next call.

## Opening

```rust
pub struct DatabaseKey(/* 32 bytes */);
impl DatabaseKey {
    pub const LEN: usize = 32;
    pub fn from_bytes(bytes: [u8; 32]) -> Self;
}

pub struct Store { /* Mutex<rusqlite::Connection> */ }
impl Store {
    pub fn open(path: impl AsRef<Path>, key: &DatabaseKey) -> Result<Store, StoreError>;
    pub fn user_version(&self) -> Result<i32, StoreError>;
    pub fn close(self) -> Result<(), StoreError>;
}
```

`Store::open` runs these steps, in this order. Any failure drops the connection, which closes it,
so nothing is left half open, as v1's `DatabaseHolder.open` closed a database that failed to open.

1. **Set SQLCipher up and turn its log off**, once per process. While it holds a process-wide
   lock, the first open opens an in-memory connection, which runs SQLite's and SQLCipher's
   initialization to the end in that thread, and runs `PRAGMA cipher_log_level = NONE`, then
   `PRAGMA cipher_log = off`. Every later open finds the work done. SQLCipher otherwise logs
   warnings and errors, such as a failed page check, to logcat on Android, the system log on Apple
   platforms, and stderr elsewhere, and shipped code doesn't log (`CLAUDE.md`, "Security
   posture"). v1's `libsqlcipher.so` holds the same messages and calls Android's
   `__android_log_write`; users never saw them.

   The lock works around two races in SQLCipher 4.20.0's process-wide setup, both found on
   2026-11-23 by tests that opened stores on several threads at once:

   - SQLite marks itself initialized, releases its lock, and only then runs SQLCipher's own
     initialization (`SQLITE_EXTRA_INIT`). A thread that opens a connection in that window finds
     SQLite initialized, and its `PRAGMA key` fails with "sqlcipher not initialized". With the
     log pragmas already guarded, 64 threads opening at once failed in 4 of 877 runs; the one
     failure whose output was kept showed this error.
   - SQLCipher changes its log settings without a lock. `PRAGMA cipher_log` reads the log file's
     pointer three times before it closes the file, so two of them at once can see it become null
     between the reads and call `fclose(NULL)`. When every open ran the pragmas, the unit tests
     crashed in 10 of 300 runs.

   Setting the level first means that a message being logged at that moment is dropped by the
   level check before the log file is unset.
2. **Open the file** for reading and writing, creating it if it is absent. Just before, note
   whether the file is absent or holds 0 bytes, because opening creates an absent file and the
   next steps can't tell the two apart. Paths are file names, never `file:` URIs, because the
   open doesn't pass `SQLITE_OPEN_URI`.
3. **Require SQLCipher.** `PRAGMA cipher_version` must return a row. Plain SQLite ignores a pragma
   it doesn't know, so without this check a build without the codec would ignore the key and
   write plaintext.
4. **Set the key** with `PRAGMA key = "x'<64 lowercase hex digits>'";`. That is v1's raw-key
   passphrase (section 5 of the format spec), so SQLCipher uses the 32 bytes as the encryption key
   with no key derivation.
5. **Pin SQLCipher 4's settings** with `PRAGMA cipher_compatibility = 4`: a 4,096-byte page,
   HMAC-SHA512, PBKDF2-HMAC-SHA512 for the key derivation that a raw key skips, and no plaintext
   header. These are SQLCipher 4's defaults, which v1 relied on without naming (open question 6 in
   the format spec); naming them keeps a later SQLCipher with other defaults reading the file.
6. **Check the key** with `SELECT count(*) FROM sqlite_master`, which decrypts the first page and
   checks its authentication code. SQLite's `NotADatabase` error here becomes
   `StoreError::KeyRejected`.
7. **Bring the schema to version 2**: check it, create it, or migrate it, as
   [Schema version 2](#schema-version-2) describes.
8. **Turn on foreign keys** (`PRAGMA foreign_keys = ON`), as Room did for v1 (invariant 8 of the
   format spec).

Opening doesn't change an existing file's journal mode. v1's files are in write-ahead-log mode,
which SQLite records in the file and keeps.

What opens and what doesn't, measured against the M2.1 fixture:

| File | Result |
| :- | :- |
| The fixture, with its key | Opens; `user_version` is 2; every byte is the same after closing |
| The fixture, with one bit of the key changed | `KeyRejected`; the file is unchanged |
| A plaintext SQLite file | `KeyRejected`; the file is unchanged |
| 8,192 bytes of noise | `KeyRejected` |
| The fixture's first 15 to 4,095 bytes | `KeyRejected` |
| The fixture's first 4,096 or 8,192 bytes | `Sqlite`, with SQLite's `DatabaseCorrupt` code: the first page opens, but pages of the schema are missing |
| The fixture with one byte of its first page changed | `KeyRejected` |
| An absent or empty file | Opens, with version 2's empty tables in write-ahead-log mode |
| The fixture's first byte alone | `UnrecognizedSchema`, with version 0; the file is unchanged |

SQLite reads a 1-byte file as an empty database too, so "`user_version` is 0" doesn't prove that
the install is new. That is why the open looks at the file's size before SQLite does.

## Schema version 2

Built in M2.2 as described here. Every open brings the file to schema version 2, the four tables
in section 6 of the format spec, as Room 2.8.5 did for v1 (ADR-0018). `src/schema.rs` holds
Room's statements for version 2, and `src/migration.rs` chooses the step and runs it. The store
doesn't map rows to Rust types yet; the use cases that read them do, from M2.4.

### Choosing the step

SQLite keeps the schema version in the file's header, where `PRAGMA user_version` reads it.
Room also keeps a bookkeeping table, `room_master_table`, whose row with ID 42 holds the schema's
identity hash: a fingerprint of the schema that Room's compiler computes. Version 2's hash is
`425274f1a5ac9232a36cc07f4f479e7c`, and version 1's was `15f1c4acb81d97b124713b69e7ec869d`.

| `user_version` | And | What `open` does |
| :- | :- | :- |
| 2 | Row 42 of `room_master_table` holds version 2's hash | Nothing: the file is only read |
| 2 | The table or the row is missing, or holds another hash | Returns `UnrecognizedSchema`, and writes nothing |
| 1 | | Migrates the file to version 2 |
| 0 | The file was absent or held 0 bytes, and has no tables | Creates version 2's tables |
| 0 | Anything else, such as a 1-byte file or one with tables | Returns `UnrecognizedSchema`, and writes nothing |
| Above 2 | | Returns `NewerSchema`, and writes nothing |
| Below 0 | | Returns `UnrecognizedSchema`, and writes nothing |

This is Room's rule with three cases made stricter (ADR-0018). Room wrote a new schema into any
version 0 file with no tables, so it would have written over the 1-byte file. For a version 2 file
without `room_master_table`, Room compared the tables with version 2's, column by column, then
wrote the hash. And Room also accepted a legacy hash, the workaround for a Room 1.x bug, which no
v1 file holds: v1 used only Room 2.6.1 and later. No v1 install is known to hold any of these
files, and a schema the store refuses stays as it was, for a later build to read.

A version 2 file is only read, so opening one that v1 wrote changes no byte of it. Creating and
migrating run in one `BEGIN IMMEDIATE` transaction, which takes the database's write lock before
it reads anything. The step is chosen again inside it, so two connections can't both act on what
one of them read before the other wrote. Any failure rolls the transaction back, which leaves the
file as it was.

### Creating

A new file gets Room's statements for version 2, copied from v1's exported schema
(`app/schemas/com.denariidolor.data.local.db.AppDatabase/2.json` at `6d57526`), in the order Room
ran them: each table, then its indexes. Then come `room_master_table` with version 2's hash, and
`user_version` 2. SQLite keeps each `CREATE` statement's text in `sqlite_master`, so the new file's
schema text matches the M2.1 fixture's, row for row. SQLite adds `sqlite_sequence` with the first
`AUTOINCREMENT` table, as it did for v1.

Once the transaction commits, the file switches to write-ahead logging, as Room put every file it
opened on most phones; SQLite can't change the journal mode inside a transaction. If SQLite
reports an error there, the open fails, and the file stays a valid version 2 database in the
rollback-journal mode, which later opens keep. If SQLite can't use write-ahead logging for the
file, it keeps the rollback journal without an error, answering with the mode it kept, and the
open succeeds.

### Migrating from version 1

Version 1 kept money in `REAL` dollars (section 6 of the format spec). The migration runs v1's
`MIGRATION_1_2` statements unchanged
(`app/src/main/java/com/denariidolor/data/local/db/Migrations.kt` at `6d57526`): for `accounts`,
`budgets`, and `transactions` in turn, it creates the table under its version 2 definition, copies
the rows with `CAST(ROUND(<column> * 100) AS INTEGER)`, drops the old table, renames the new one,
and creates its indexes again. `categories` is the same in both versions, so the migration leaves
it alone. Then it writes version 2's hash into `room_master_table` and sets `user_version` to 2, as
Room did after a migration.

- **Foreign keys are off** while it runs, as Room had them. This build turns them on by default
  (`-DSQLITE_DEFAULT_FOREIGN_KEYS=1`), and with them on, dropping `accounts` would fail on the
  `RESTRICT` key from `transactions`. SQLite ignores the pragma inside a transaction, so it is set
  before the transaction starts. Step 8 of the open turns them back on.
- **Rounding.** `ROUND` with no digits adds 0.5 to the product, or subtracts it from a negative
  one, and drops the fraction, so a half rounds away from zero. It works on the product as a
  binary fraction, so values that look alike can round apart:

  | Stored dollars | `<column> * 100` as SQLite computes it | Cents |
  | :- | :- | :- |
  | `0.005` | `0.5` | `1` |
  | `1.005` | `100.49999999999999` | `100` |
  | `19.995` | `1999.5` | `2000` |
  | `-12.345` | `-1234.5` | `-1235` |

  These come from SQLite 3.54.1, the version this build compiles. v1 ran the same statements on
  SQLCipher 4.19.0's SQLite; fixture F4 (M2.3), which v1 writes, checks that the two agree.
- **`AUTOINCREMENT` counters.** Dropping a table deletes its row in `sqlite_sequence`, and copying
  rows into the new table records only the highest ID copied, or 0 when there is none. So after
  the migration, an ID deleted before it can be handed out again. v1's migration did the same.
- **No check of the result.** Room compared the migrated tables with version 2's. The store
  doesn't: the statements are fixed, and they create `accounts`, `budgets`, and `transactions`
  with version 2's definitions (ADR-0018). A file whose version 1 tables lack a column fails the
  migration, which rolls back.

## The key

`DatabaseKey` holds the 32 bytes. Its `Debug` output is `DatabaseKey(..)`, and dropping it
overwrites the bytes with zeros. The key pragma's text is built in a `String` allocated at its
exact length, 83 bytes, so it never reallocates and leaves no copy behind, and it is overwritten
with zeros once SQLCipher has read it. `rusqlite` hands that text to SQLite without copying it.

The wiping is best effort, as v1's `fill(0)` was: the writes go through `std::hint::black_box` so
the compiler keeps them, but copies made when a key moves, the caller's own array, and SQLite's
parse buffers aren't wiped. M3.3 chooses the crates for wiping secrets
(`docs/02-dependency-policy.md`). SQLCipher wipes its own copy of the key when the connection
closes.

## Errors

`StoreError` has six variants. None carries the key.

| Variant | When |
| :- | :- |
| `CipherUnavailable` | The build's SQLite has no SQLCipher codec: a build error, never a user's |
| `KeyRejected` | The file doesn't open with the key: the key is wrong, or the file is plaintext, too short to hold a page, or not a database |
| `NewerSchema { version }` | The schema version is above 2: a later build of the app wrote the file |
| `UnrecognizedSchema { version }` | The schema isn't one the store knows: a negative version, a version 0 file that held a byte before it opened, or a version 2 file without version 2's identity hash |
| `Poisoned` | An earlier call panicked while it held the connection, so its state is unknown |
| `Sqlite { operation, source }` | Any other SQLite error, with what the store was doing and SQLite's error as its source |

The bridge will map each variant to a stable code for the user interface once sign-in opens the
database (M3.5).

## Threading

A `Store` owns one `rusqlite::Connection` inside a `std::sync::Mutex` (ADR-0017). `rusqlite`'s
connection can move between threads but can't be used from two at once, and the bridge's worker
pool can run two calls at the same time. Each operation takes the lock for the whole of its
database work, so a write's row and its balance changes (M2.4) can't interleave with another call.
`Store` is `Send` and `Sync`, and the bridge can share one through an `Arc`. `store` starts no
thread.

A lock poisoned by a panic in another call returns `StoreError::Poisoned` rather than panicking
again. `close` takes the `Store` by value, so no other call can hold the lock while it runs.

## Build

The workspace pins `rusqlite` 0.43.0 exactly, with only the `bundled-sqlcipher-vendored-openssl`
feature. It compiles SQLCipher 4.20.0 over SQLite 3.54.1 from the `sqlcipher-src` crate and OpenSSL
3.6.3 from `openssl-src`, with `-DSQLITE_DEFAULT_FOREIGN_KEYS=1` and an in-memory temporary store,
so SQLite writes no plaintext temporary files. Android builds take the NDK's compiler from the
environment that Flutter's build hook or a contributor sets up. The register in
`docs/02-dependency-policy.md` lists the crates and their licenses.

## Invariants

1. The database opens only with its key. Another key, a plaintext file, and a file too short to
   hold a page return `KeyRejected`.
2. Opening and closing never delete a file. Opening writes to a file only to create version 2's
   tables in a file that was absent or held 0 bytes, or to migrate a version 1 file. A failed open
   leaves an existing file as it was, and opening a version 2 file changes none of its bytes.
3. A file that v1's `sqlcipher-android` 4.19.1 wrote opens with SQLCipher 4's settings, reads back
   v1's rows, and reports `user_version` 2.
4. A database the store creates is encrypted: it has no plaintext SQLite header, and its rows'
   text doesn't appear in the file.
5. Every open connection has foreign keys on and SQLCipher's log off. SQLCipher's setup and the
   log pragmas run once per process, before the first file connection opens.
6. The key never appears in `Debug` output or in an error.
7. `Store` is `Send` and `Sync`, and calls from several threads all succeed.
8. A database the store creates holds the same schema text as v1's, with version 2's identity
   hash, and is in write-ahead-log mode.
9. A version 1 database migrates as v1's `MIGRATION_1_2` did: v1's own test values come out the
   same, and the migrated tables have the same columns, foreign keys, and indexes as a version 2
   file v1 wrote.
10. A file at a schema version above 2, below 0, at version 2 without version 2's identity hash,
    or at version 0 after holding a byte, is refused, and no byte of it changes.

## Tests

| Invariant | Checked by |
| :- | :- |
| 1, 2 | `tests/open.rs`: another key, noise, the fixture cut off at six lengths from 15 bytes to two pages, a changed byte, and an unchanged file after each; `src/store.rs`: a plaintext file; `src/migration.rs`: a failed migration |
| 3 | `tests/open.rs` (`user_version`, and every byte unchanged after closing) and `src/store.rs` (the two accounts, the table names, and the cipher settings), against `tests/fixtures/v1-sqlcipher-android-4.19.1.db`, which `tests/fixtures/README.md` describes |
| 4 | `src/store.rs`: a new database, reopened with its key and refused with another |
| 5 | `src/store.rs`: `foreign_keys` is 1 and `cipher_log_level` is `NONE` after opening the fixture; `tests/concurrent_open.rs` for the setup |
| 6 | `src/key.rs`: the `Debug` output |
| 7 | `tests/open.rs`: a compile-time check, and eight threads sharing one `Store`; `tests/concurrent_open.rs`: 64 stores opened at the same moment, the first opens of a process of their own. The races it guards against are rare, so one passing run proves little; the evidence is the loop counts in the roadmap's note on M2.1 |
| 8 | `src/migration.rs`: an absent and an empty file, compared with the fixture's `sqlite_master`, `room_master_table`, and journal mode; `tests/open.rs`: the same two files through the public API |
| 9 | `src/migration.rs`: a version 1 file written with Room's version 1 statements and the rows of v1's `MigrationTest`, then its cents, its table shapes against the fixture's, its foreign key check, its rounding, and its `sqlite_sequence` |
| 10 | `src/migration.rs`: each refused case, with the file's bytes compared before and after; `tests/open.rs`: the 1-byte file |
| The key pragma's text | `src/key.rs`: v1's raw-key syntax for a known key, and a length equal to the capacity |

Run them with `cargo test --locked -p denarii-store` from `rust/`, or only the schema's with
`cargo test --locked -p denarii-store migration`. Fixtures F3, with every transaction type, and
F4, a version 1 file written by v1 itself, come in M2.3.
