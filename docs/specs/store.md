# Subsystem: `store`

`denarii-store` (`rust/crates/store`) owns the SQLCipher database: it opens the file that v1, the
Kotlin app, left on users' phones, with the key the vault unwraps at sign-in, and runs every query
and write on it. It guarantees that the file opens only with its key, that opening changes nothing
in an existing file, and that calls from several threads never interleave on its one connection.
ADR-0006 (the database stays SQLCipher, opened from Rust with `rusqlite`) and ADR-0017 (SQLCipher
and OpenSSL compiled from source on every target, and one connection behind a mutex) shaped it, and
section 5 of `docs/specs/android-v1-formats.md` is the file it must read. Built in M2.1 as
described here, with opening and closing only; the schema and its migrations land in M2.2, the use
cases in M2.4 to M2.6. The API reference is `cargo doc -p denarii-store --open`, run from `rust/`.

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
2. **Open the file** for reading and writing, creating it if it is absent. Paths are file names,
   never `file:` URIs, because the open doesn't pass `SQLITE_OPEN_URI`.
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
7. **Turn on foreign keys** (`PRAGMA foreign_keys = ON`), as Room did for v1 (invariant 8 of the
   format spec).

Opening reads nothing else. It doesn't read or change the schema version or the journal mode:
v1's files are in write-ahead-log mode, which SQLite records in the file and keeps. M2.2 adds the
schema check and the migrations.

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
| An absent file, an empty file, or the fixture's first byte alone | Opens as an empty database; `user_version` is 0, and opening writes nothing |

The last row matters for M2.2: SQLite reads a 1-byte file as an empty database, so "`user_version`
is 0" doesn't prove that the install is new.

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

`StoreError` has four variants. None carries the key.

| Variant | When |
| :- | :- |
| `CipherUnavailable` | The build's SQLite has no SQLCipher codec: a build error, never a user's |
| `KeyRejected` | The file doesn't open with the key: the key is wrong, or the file is plaintext, too short to hold a page, or not a database |
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
2. Opening and closing, with or without an error, never deletes a file or changes a byte of an
   existing one.
3. A file that v1's `sqlcipher-android` 4.19.1 wrote opens with SQLCipher 4's settings, reads back
   v1's rows, and reports `user_version` 2.
4. A database the store creates is encrypted: it has no plaintext SQLite header, and its rows'
   text doesn't appear in the file.
5. Every open connection has foreign keys on and SQLCipher's log off. SQLCipher's setup and the
   log pragmas run once per process, before the first file connection opens.
6. The key never appears in `Debug` output or in an error.
7. `Store` is `Send` and `Sync`, and calls from several threads all succeed.

## Tests

| Invariant | Checked by |
| :- | :- |
| 1, 2 | `tests/open.rs`: another key, noise, the fixture cut off at eight lengths from 0 bytes to two pages, a changed byte, and an unchanged file after each; `src/store.rs`: a plaintext file |
| 3 | `tests/open.rs` (`user_version`, and every byte unchanged after closing) and `src/store.rs` (the two accounts, the table names, and the cipher settings), against `tests/fixtures/v1-sqlcipher-android-4.19.1.db`, which `tests/fixtures/README.md` describes |
| 4 | `src/store.rs`: a new database, reopened with its key and refused with another |
| 5 | `src/store.rs`: `foreign_keys` is 1 and `cipher_log_level` is `NONE` after opening the fixture; `tests/concurrent_open.rs` for the setup |
| 6 | `src/key.rs`: the `Debug` output |
| 7 | `tests/open.rs`: a compile-time check, and eight threads sharing one `Store`; `tests/concurrent_open.rs`: 64 stores opened at the same moment, the first opens of a process of their own. The races it guards against are rare, so one passing run proves little; the evidence is the loop counts in the roadmap's note on M2.1 |
| The key pragma's text | `src/key.rs`: v1's raw-key syntax for a known key, and a length equal to the capacity |

Run them with `cargo test --locked -p denarii-store` from `rust/`. Fixture F3 of the format spec,
with every transaction type, comes in M2.3.
