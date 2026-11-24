/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The SQLCipher database of Denarii Dolor, opened from Rust as v1, the Kotlin app, left it on
//! users' phones.
//!
//! v1 keeps every record in one SQLCipher 4 database (SQLite with each page encrypted with
//! AES-256 and authenticated with HMAC-SHA512), under a raw 32-byte key that the vault unwraps
//! at sign-in. This crate opens that file as it is, with `rusqlite` and the SQLCipher and OpenSSL
//! builds it compiles in (ADR-0006, ADR-0017). Today it opens and closes the database; the
//! schema, its migrations, and the use cases land in M2.2 to M2.6. The bridge reaches it only
//! after the vault has unlocked the key (M3). Its design is in `docs/specs/store.md`, and the
//! file it reads is in section 5 of `docs/specs/android-v1-formats.md`.
//!
//! # The parts
//!
//! - [`Store`]: the open database, with [`Store::open`], [`Store::user_version`], and
//!   [`Store::close`].
//! - [`DatabaseKey`]: the key, which no `Debug` output shows and which is wiped when dropped.
//! - [`StoreError`]: why an operation failed, such as [`StoreError::KeyRejected`].
//!
//! # Example
//!
//! A new database opens with its key, and its schema version starts at 0:
//!
//! ```
//! use denarii_store::{DatabaseKey, Store};
//!
//! # fn main() -> Result<(), Box<dyn std::error::Error>> {
//! # let dir = std::env::temp_dir().join(format!("denarii-store-doc-{}", std::process::id()));
//! # std::fs::create_dir_all(&dir)?;
//! // A test value; the real key comes from the vault.
//! let key = DatabaseKey::from_bytes([7; DatabaseKey::LEN]);
//! let store = Store::open(dir.join("denarii_dolor.db"), &key)?;
//! assert_eq!(store.user_version()?, 0);
//! store.close()?;
//! # std::fs::remove_dir_all(&dir)?;
//! # Ok(())
//! # }
//! ```
//!
//! # What callers must know
//!
//! - Opening never deletes or rewrites a file. A wrong key, a plaintext file, and a file cut off
//!   inside its first page return [`StoreError::KeyRejected`]. A file whose first page opens but
//!   whose schema is damaged returns [`StoreError::Sqlite`] with SQLite's `DatabaseCorrupt` code.
//!   Either way the file stays as it was.
//! - An absent or empty file opens as an empty database, with a schema version of 0. So does a
//!   1-byte file, because SQLite reads it that way.
//! - [`Store`] is `Send` and `Sync`. Its operations take turns on one connection, so a slow one
//!   makes the next wait.
//! - The first [`Store::open`] in a process sets SQLCipher up and turns its log off, once, under a
//!   lock, which works around two races in SQLCipher's setup. Open the database only through
//!   [`Store::open`]: another SQLite connection opened at the same moment as the first store could
//!   still meet them.
//! - Nothing here panics on a file's contents, and nothing prints or logs.

mod error;
mod key;
mod store;

pub use error::StoreError;
pub use key::DatabaseKey;
pub use store::Store;
