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
//! builds it compiles in (ADR-0006, ADR-0017). Today it opens the database, brings its schema to
//! version 2 as v1 did (ADR-0018), adds, edits, and deletes transactions with v1's checks and
//! balance changes, and categories, accounts, and budgets with v1's rules, seeds the default
//! accounts and categories, and closes the database. The bridge reaches it only after the vault
//! has unlocked the key (M3). Its design is in
//! `docs/specs/store.md`, and the file it reads is in sections 5 and 6 of
//! `docs/specs/android-v1-formats.md`.
//!
//! # The parts
//!
//! - [`Store`]: the open database, with [`Store::open`], [`Store::user_version`], and
//!   [`Store::close`].
//! - Transaction writes: [`Store::add_transaction`], [`Store::update_transaction`], and
//!   [`Store::delete_transaction`], with [`OverBudget`] for an expense the user has confirmed.
//!   The caller passes each month as a [`MonthBounds`](denarii_core::MonthBounds) (ADR-0019).
//! - Categories, accounts, and budgets: [`Store::add_category`], [`Store::update_category`],
//!   [`Store::delete_category`], [`Store::add_account`], [`Store::rename_account`],
//!   [`Store::delete_account`], [`Store::set_budget`], and [`Store::delete_budget`].
//! - The defaults: [`Store::seed_defaults`], with their [`DefaultNames`] in the user's language.
//! - [`DatabaseKey`]: the key, which no `Debug` output shows and which is wiped when dropped.
//! - [`StoreError`]: why an operation failed, such as [`StoreError::KeyRejected`].
//!
//! # Example
//!
//! A new database opens with its key and gets schema version 2's empty tables:
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
//! assert_eq!(store.user_version()?, 2);
//! store.close()?;
//! # std::fs::remove_dir_all(&dir)?;
//! # Ok(())
//! # }
//! ```
//!
//! # What callers must know
//!
//! - Opening never deletes a file. It writes to one only to bring it to schema version 2: an
//!   absent or 0-byte file gets version 2's empty tables and write-ahead logging, and a version 1
//!   file is migrated, each in one transaction. Any failure leaves an existing file as it was.
//! - A wrong key, a plaintext file, and a file cut off inside its first page return
//!   [`StoreError::KeyRejected`]. A file whose first page opens but whose schema is damaged
//!   returns [`StoreError::Sqlite`] with SQLite's `DatabaseCorrupt` code.
//! - A schema the store doesn't know returns [`StoreError::NewerSchema`] or
//!   [`StoreError::UnrecognizedSchema`], and is never written over. That includes a 1-byte file,
//!   which SQLite reads as an empty database.
//! - A transaction write checks the request in v1's order, then writes the row and its balance
//!   changes in one database transaction. A write that fails, for any reason, leaves the file as
//!   it was.
//! - Every other write also checks the request in v1's order inside one database transaction.
//!   Category and account names are stored trimmed, and two names are duplicates when they
//!   differ only in the case of ASCII letters, as v1 compared them. The seeded rows in
//!   [`defaults`](denarii_core::defaults) can't be deleted, nor can a category or account that a
//!   transaction uses; deleting a category deletes its budget.
//! - Seeding writes the defaults only into an empty `accounts` or `categories` table, so it can
//!   run after every sign-in, as v1 ran it. It checks the names the caller passes as it checks
//!   every other name.
//! - [`Store`] is `Send` and `Sync`. Its operations take turns on one connection, so a slow one
//!   makes the next wait.
//! - The first [`Store::open`] in a process sets SQLCipher up and turns its log off, once, under a
//!   lock, which works around two races in SQLCipher's setup. Open the database only through
//!   [`Store::open`]: another SQLite connection opened at the same moment as the first store could
//!   still meet them.
//! - Nothing here panics on a file's contents, and nothing prints or logs.

mod error;
mod key;
mod migration;
mod reference;
mod schema;
mod seed;
mod store;
mod transactions;

pub use error::StoreError;
pub use key::DatabaseKey;
pub use seed::DefaultNames;
pub use store::Store;
pub use transactions::OverBudget;
