/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Schema version 2: the four tables v1 keeps, in the statements Room 2.8.5 ran to create them.
//!
//! The statements are Room's, copied from v1's exported schema
//! (`app/schemas/com.denariidolor.data.local.db.AppDatabase/2.json` at `6d57526`) and run in
//! Room's order: each table, then its indexes, then Room's bookkeeping table. SQLite stores each
//! `CREATE` statement's text in `sqlite_master`, so a database created from these holds the same
//! schema text as one v1 created. Section 6 of `docs/specs/android-v1-formats.md` describes the
//! tables and their columns.

/// The schema version this build reads and writes, which SQLite keeps in `PRAGMA user_version`.
pub(crate) const VERSION: i32 = 2;

/// The identity hash that Room's compiler computed for schema version 2, which Room keeps in the
/// row of `room_master_table` whose ID is 42. It has been the same since version 2 appeared in
/// v1, at commit `bfdea7a`.
pub(crate) const VERSION_2_IDENTITY_HASH: &str = "425274f1a5ac9232a36cc07f4f479e7c";

/// Room's bookkeeping table and its one row, as Room writes them after it creates or migrates a
/// schema (the `setupQueries` of v1's exported schema).
pub(crate) const RECORD_IDENTITY: &str = concat!(
    "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT);",
    "INSERT OR REPLACE INTO room_master_table (id,identity_hash) ",
    "VALUES(42, '425274f1a5ac9232a36cc07f4f479e7c');",
);

/// The four tables and their indexes, as Room creates them in an empty database.
/// [`RECORD_IDENTITY`] follows them.
pub(crate) const CREATE_TABLES: &str = concat!(
    "CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ",
    "`type` TEXT NOT NULL, `description` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, ",
    "`categoryId` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `transferAccountId` INTEGER, ",
    "`dateEpochMillis` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, ",
    "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ",
    "ON DELETE RESTRICT , ",
    "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ",
    "ON DELETE RESTRICT );",
    "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ",
    "ON `transactions` (`categoryId`);",
    "CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`);",
    "CREATE INDEX IF NOT EXISTS `index_transactions_dateEpochMillis` ",
    "ON `transactions` (`dateEpochMillis`);",
    "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ",
    "`name` TEXT NOT NULL, `iconName` TEXT NOT NULL);",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`);",
    "CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ",
    "`categoryId` INTEGER NOT NULL, `monthlyLimitCents` INTEGER NOT NULL, ",
    "`warningThresholdPercent` INTEGER NOT NULL, ",
    "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ",
    "ON DELETE CASCADE );",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`);",
    "CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ",
    "`name` TEXT NOT NULL, `balanceCents` INTEGER NOT NULL);",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_name` ON `accounts` (`name`);",
);
