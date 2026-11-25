/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Adding, editing, and deleting the reference data that transactions point to: categories,
//! accounts, and monthly budgets.
//!
//! This ports v1's `CategoryUseCases`, `AccountUseCases`, and `BudgetUseCases`, with the SQL of
//! Room's generated code for the DAOs behind them (all at `6d57526`). Each operation checks the
//! request in v1's order, then writes, inside one `BEGIN IMMEDIATE` transaction under the store's
//! lock, so no other call can store a name or a transaction between a check and the write. v1 ran
//! its checks outside any database transaction.
//!
//! Names are compared as v1's DAOs compared them, with SQLite's `LOWER`, which folds only the
//! ASCII letters: `Groceries` and `groceries` are the same name, and `École` and `école` aren't.
//! v1's SQLCipher build had no International Components for Unicode (ICU), the library that
//! would fold the other letters, and this build's has none either.

use denarii_core::{
    AccountId, BudgetId, CategoryId, Cents, DomainError, defaults, is_blank, is_valid_amount,
    is_valid_percent, validate_name,
};
use rusqlite::{Connection, OptionalExtension, params};

use crate::error::{StoreError, sqlite};
use crate::store::Store;
use crate::transactions::{begin, commit};

impl Store {
    /// Adds a category and returns its ID.
    ///
    /// The name is stored trimmed with Kotlin's whitespace, as v1 stored it. A blank `icon_name`
    /// stores [`DEFAULT_CATEGORY_ICON`](defaults::DEFAULT_CATEGORY_ICON); any other text is
    /// stored as it is, because v1 didn't check icon keys, and its screens showed the default
    /// icon for a key they didn't know.
    ///
    /// # Errors
    ///
    /// The first of these that applies, in v1's order. Nothing is written when any of them is
    /// returned.
    ///
    /// 1. [`StoreError::Domain`] with [`DomainError::InvalidName`] if the trimmed name is empty
    ///    or longer than [`MAX_NAME_UNITS`](denarii_core::MAX_NAME_UNITS).
    /// 2. [`StoreError::Domain`] with [`DomainError::DuplicateCategoryName`] if another category
    ///    has the name, with ASCII letters compared without regard to case.
    /// 3. [`StoreError::Poisoned`] or [`StoreError::Sqlite`] if the connection can't be used, or
    ///    SQLite fails.
    pub fn add_category(&self, name: &str, icon_name: &str) -> Result<CategoryId, StoreError> {
        let name = validate_name(name).map_err(StoreError::Domain)?;
        let icon_name = if is_blank(icon_name) {
            defaults::DEFAULT_CATEGORY_ICON
        } else {
            icon_name
        };
        let mut connection = self.lock()?;
        let write = begin(&mut connection)?;
        require_unique_name(&write, NamedTable::Categories, name, NO_ROW)?;
        write
            .execute(
                "INSERT INTO categories (name, iconName) VALUES (?1, ?2)",
                params![name, icon_name],
            )
            .map_err(|source| sqlite("insert the category", source))?;
        let id = CategoryId(write.last_insert_rowid());
        commit(write)?;
        Ok(id)
    }

    /// Renames the category `id`, and changes its icon when `icon_name` isn't blank.
    ///
    /// The name is stored trimmed. `None` or a blank `icon_name` keeps the stored icon, as v1's
    /// rename did. The seeded categories can be renamed: their names are ordinary data.
    ///
    /// # Errors
    ///
    /// The first of these that applies, in v1's order. Nothing is written when any of them is
    /// returned.
    ///
    /// 1. [`StoreError::Domain`] with [`DomainError::IdRequired`] if `id` isn't more than zero.
    /// 2. [`StoreError::CategoryNotFound`] if no category has the ID.
    /// 3. Steps 1–3 of [`add_category`](Self::add_category)'s errors; the category's own name
    ///    isn't a duplicate, so a rename may change only its case.
    pub fn update_category(
        &self,
        id: CategoryId,
        name: &str,
        icon_name: Option<&str>,
    ) -> Result<(), StoreError> {
        if id.0 <= 0 {
            return Err(StoreError::Domain(DomainError::IdRequired));
        }
        let mut connection = self.lock()?;
        let write = begin(&mut connection)?;
        let stored_icon: String = write
            .query_row(
                "SELECT iconName FROM categories WHERE id = ?1",
                [id.0],
                |row| row.get(0),
            )
            .optional()
            .map_err(|source| sqlite("read the category", source))?
            .ok_or(StoreError::CategoryNotFound { id })?;
        let name = validate_name(name).map_err(StoreError::Domain)?;
        require_unique_name(&write, NamedTable::Categories, name, id.0)?;
        let icon_name = icon_name
            .filter(|icon| !is_blank(icon))
            .unwrap_or(&stored_icon);
        write
            .execute(
                "UPDATE categories SET name = ?1, iconName = ?2 WHERE id = ?3",
                params![name, icon_name, id.0],
            )
            .map_err(|source| sqlite("update the category", source))?;
        commit(write)
    }

    /// Deletes the category `id`, and its budget with it.
    ///
    /// The budget goes through the `ON DELETE CASCADE` foreign key that v1's schema gives
    /// `budgets.categoryId`, so a budget never keeps a category from being deleted.
    ///
    /// # Errors
    ///
    /// The first of these that applies, in v1's order. Nothing is written when any of them is
    /// returned.
    ///
    /// 1. [`StoreError::Domain`] with [`DomainError::DefaultCategory`] if `id` is one of the
    ///    seeded categories in [`defaults`].
    /// 2. [`StoreError::Domain`] with [`DomainError::CategoryInUse`], carrying the count, if any
    ///    transaction is in the category.
    /// 3. [`StoreError::CategoryNotFound`] if no category has the ID, the ID 0 and below
    ///    included: v1 didn't check the ID of a delete.
    /// 4. [`StoreError::Poisoned`] or [`StoreError::Sqlite`] if the connection can't be used, or
    ///    SQLite fails.
    pub fn delete_category(&self, id: CategoryId) -> Result<(), StoreError> {
        if defaults::is_default_category(id) {
            return Err(StoreError::Domain(DomainError::DefaultCategory));
        }
        let mut connection = self.lock()?;
        let write = begin(&mut connection)?;
        let transactions = count_transactions(
            &write,
            "SELECT COUNT(*) FROM transactions WHERE categoryId = ?1",
            id.0,
        )?;
        if transactions != 0 {
            return Err(StoreError::Domain(DomainError::CategoryInUse {
                transactions,
            }));
        }
        let deleted = write
            .execute("DELETE FROM categories WHERE id = ?1", [id.0])
            .map_err(|source| sqlite("delete the category", source))?;
        if deleted == 0 {
            return Err(StoreError::CategoryNotFound { id });
        }
        commit(write)
    }

    /// Adds an account whose balance starts at `opening_balance`, and returns its ID.
    ///
    /// The name is stored trimmed. The opening balance may be any number of cents, negative for
    /// a debt, as v1 allowed; it is stored nowhere but in the balance itself.
    ///
    /// # Errors
    ///
    /// The first of these that applies, in v1's order. Nothing is written when any of them is
    /// returned.
    ///
    /// 1. [`StoreError::Domain`] with [`DomainError::InvalidName`] if the trimmed name is empty
    ///    or longer than [`MAX_NAME_UNITS`](denarii_core::MAX_NAME_UNITS).
    /// 2. [`StoreError::Domain`] with [`DomainError::DuplicateAccountName`] if another account
    ///    has the name, with ASCII letters compared without regard to case.
    /// 3. [`StoreError::Poisoned`] or [`StoreError::Sqlite`] if the connection can't be used, or
    ///    SQLite fails.
    pub fn add_account(&self, name: &str, opening_balance: Cents) -> Result<AccountId, StoreError> {
        let name = validate_name(name).map_err(StoreError::Domain)?;
        let mut connection = self.lock()?;
        let write = begin(&mut connection)?;
        require_unique_name(&write, NamedTable::Accounts, name, NO_ROW)?;
        write
            .execute(
                "INSERT INTO accounts (name, balanceCents) VALUES (?1, ?2)",
                params![name, opening_balance.0],
            )
            .map_err(|source| sqlite("insert the account", source))?;
        let id = AccountId(write.last_insert_rowid());
        commit(write)?;
        Ok(id)
    }

    /// Renames the account `id`.
    ///
    /// The name is stored trimmed, and the balance doesn't change. The seeded accounts can be
    /// renamed: their names are ordinary data.
    ///
    /// # Errors
    ///
    /// The first of these that applies, in v1's order, which checks the name before it looks for
    /// the row. Nothing is written when any of them is returned.
    ///
    /// 1. [`StoreError::Domain`] with [`DomainError::IdRequired`] if `id` isn't more than zero.
    /// 2. Steps 1 and 2 of [`add_account`](Self::add_account)'s errors; the account's own name
    ///    isn't a duplicate, so a rename may change only its case.
    /// 3. [`StoreError::AccountNotFound`] if no account has the ID.
    /// 4. [`StoreError::Poisoned`] or [`StoreError::Sqlite`] if the connection can't be used, or
    ///    SQLite fails.
    pub fn rename_account(&self, id: AccountId, name: &str) -> Result<(), StoreError> {
        if id.0 <= 0 {
            return Err(StoreError::Domain(DomainError::IdRequired));
        }
        let name = validate_name(name).map_err(StoreError::Domain)?;
        let mut connection = self.lock()?;
        let write = begin(&mut connection)?;
        require_unique_name(&write, NamedTable::Accounts, name, id.0)?;
        let renamed = write
            .execute(
                "UPDATE accounts SET name = ?1 WHERE id = ?2",
                params![name, id.0],
            )
            .map_err(|source| sqlite("rename the account", source))?;
        if renamed == 0 {
            return Err(StoreError::AccountNotFound { id });
        }
        commit(write)
    }

    /// Deletes the account `id`.
    ///
    /// An account that no transaction uses can be deleted whatever its balance, as in v1.
    ///
    /// # Errors
    ///
    /// The first of these that applies, in v1's order. Nothing is written when any of them is
    /// returned.
    ///
    /// 1. [`StoreError::Domain`] with [`DomainError::DefaultAccount`] if `id` is one of the
    ///    seeded accounts in [`defaults`].
    /// 2. [`StoreError::Domain`] with [`DomainError::AccountInUse`], carrying the count, if any
    ///    transaction uses the account or is a transfer into it.
    /// 3. [`StoreError::AccountNotFound`] if no account has the ID, the ID 0 and below included:
    ///    v1 didn't check the ID of a delete.
    /// 4. [`StoreError::Poisoned`] or [`StoreError::Sqlite`] if the connection can't be used, or
    ///    SQLite fails.
    pub fn delete_account(&self, id: AccountId) -> Result<(), StoreError> {
        if defaults::is_default_account(id) {
            return Err(StoreError::Domain(DomainError::DefaultAccount));
        }
        let mut connection = self.lock()?;
        let write = begin(&mut connection)?;
        // `transferAccountId` has no foreign key, so only this count keeps a transfer from
        // losing its destination.
        let transactions = count_transactions(
            &write,
            "SELECT COUNT(*) FROM transactions WHERE accountId = ?1 OR transferAccountId = ?1",
            id.0,
        )?;
        if transactions != 0 {
            return Err(StoreError::Domain(DomainError::AccountInUse {
                transactions,
            }));
        }
        let deleted = write
            .execute("DELETE FROM accounts WHERE id = ?1", [id.0])
            .map_err(|source| sqlite("delete the account", source))?;
        if deleted == 0 {
            return Err(StoreError::AccountNotFound { id });
        }
        commit(write)
    }

    /// Sets the monthly budget of `category`, replacing the budget it had, and returns the
    /// budget's ID.
    ///
    /// A category has at most one budget. Replacing it keeps its ID, as v1's
    /// `INSERT OR REPLACE` with the stored ID did; a new budget gets the next ID.
    /// `warning_threshold_percent` is the share of the limit at which the app starts warning;
    /// v1's screens offered [`Budget::DEFAULT_WARNING_PERCENT`](denarii_core::Budget) first.
    ///
    /// # Errors
    ///
    /// The first of these that applies, in v1's order. Nothing is written when any of them is
    /// returned.
    ///
    /// 1. [`StoreError::Domain`] with [`DomainError::InvalidBudgetLimit`] unless the limit is
    ///    more than zero.
    /// 2. [`StoreError::Domain`] with [`DomainError::InvalidWarningPercent`] unless the
    ///    threshold is from 1 to [`MAX_PERCENT`](denarii_core::MAX_PERCENT).
    /// 3. [`StoreError::Domain`] with [`DomainError::CategoryNotFound`] if no category has the
    ///    ID.
    /// 4. [`StoreError::Poisoned`] or [`StoreError::Sqlite`] if the connection can't be used, or
    ///    SQLite fails.
    pub fn set_budget(
        &self,
        category: CategoryId,
        monthly_limit: Cents,
        warning_threshold_percent: i32,
    ) -> Result<BudgetId, StoreError> {
        if !is_valid_amount(monthly_limit) {
            return Err(StoreError::Domain(DomainError::InvalidBudgetLimit));
        }
        if !is_valid_percent(warning_threshold_percent) {
            return Err(StoreError::Domain(DomainError::InvalidWarningPercent));
        }
        let mut connection = self.lock()?;
        let write = begin(&mut connection)?;
        let category_exists = write
            .query_row(
                "SELECT 1 FROM categories WHERE id = ?1",
                [category.0],
                |_| Ok(()),
            )
            .optional()
            .map_err(|source| sqlite("read the category", source))?
            .is_some();
        if !category_exists {
            return Err(StoreError::Domain(DomainError::CategoryNotFound));
        }
        let stored_id: Option<i64> = write
            .query_row(
                "SELECT id FROM budgets WHERE categoryId = ?1 LIMIT 1",
                [category.0],
                |row| row.get(0),
            )
            .optional()
            .map_err(|source| sqlite("read the category's budget", source))?;
        // A null ID makes SQLite pick the next one, as Room's `nullif(?, 0)` did for v1's ID 0.
        write
            .execute(
                "INSERT OR REPLACE INTO budgets \
                 (id, categoryId, monthlyLimitCents, warningThresholdPercent) \
                 VALUES (?1, ?2, ?3, ?4)",
                params![
                    stored_id,
                    category.0,
                    monthly_limit.0,
                    warning_threshold_percent
                ],
            )
            .map_err(|source| sqlite("write the budget", source))?;
        let id = BudgetId(write.last_insert_rowid());
        commit(write)?;
        Ok(id)
    }

    /// Deletes the budget of `category`.
    ///
    /// # Errors
    ///
    /// Nothing is written when any of these is returned.
    ///
    /// - [`StoreError::BudgetNotFound`] if the category has no budget, or there is no such
    ///   category.
    /// - [`StoreError::Poisoned`] or [`StoreError::Sqlite`] if the connection can't be used, or
    ///   SQLite fails.
    pub fn delete_budget(&self, category: CategoryId) -> Result<(), StoreError> {
        let mut connection = self.lock()?;
        let write = begin(&mut connection)?;
        let deleted = write
            .execute("DELETE FROM budgets WHERE categoryId = ?1", [category.0])
            .map_err(|source| sqlite("delete the budget", source))?;
        if deleted == 0 {
            return Err(StoreError::BudgetNotFound { category });
        }
        commit(write)
    }
}

/// The ID v1 excluded from an add's duplicate check: no stored row has it.
pub(crate) const NO_ROW: i64 = 0;

/// A table whose rows must have different names.
#[derive(Debug, Clone, Copy)]
pub(crate) enum NamedTable {
    Categories,
    Accounts,
}

impl NamedTable {
    /// v1's `countByName` query, from Room's generated code for `CategoryDao` and `AccountDao`.
    const fn count_by_name(self) -> &'static str {
        match self {
            Self::Categories => {
                "SELECT COUNT(*) FROM categories WHERE LOWER(name) = LOWER(?1) AND id != ?2"
            }
            Self::Accounts => {
                "SELECT COUNT(*) FROM accounts WHERE LOWER(name) = LOWER(?1) AND id != ?2"
            }
        }
    }

    fn duplicate(self, name: &str) -> DomainError {
        let name = name.to_owned();
        match self {
            Self::Categories => DomainError::DuplicateCategoryName { name },
            Self::Accounts => DomainError::DuplicateAccountName { name },
        }
    }
}

/// Fails with the table's duplicate-name error if a row other than `exclude` has `name`, with
/// ASCII letters compared without regard to case.
pub(crate) fn require_unique_name(
    connection: &Connection,
    table: NamedTable,
    name: &str,
    exclude: i64,
) -> Result<(), StoreError> {
    let matches: i64 = connection
        .query_row(table.count_by_name(), params![name, exclude], |row| {
            row.get(0)
        })
        .map_err(|source| sqlite("look for the name", source))?;
    if matches == 0 {
        Ok(())
    } else {
        Err(StoreError::Domain(table.duplicate(name)))
    }
}

/// How many transactions `sql` counts for the row `id`.
fn count_transactions(connection: &Connection, sql: &str, id: i64) -> Result<u32, StoreError> {
    connection
        .query_row(sql, [id], |row| row.get(0))
        .map_err(|source| sqlite("count the transactions that use the row", source))
}

#[cfg(test)]
mod tests {
    use std::fs;
    use std::path::PathBuf;

    use denarii_core::{
        AccountId, Budget, BudgetId, CategoryId, Cents, DomainError, MAX_NAME_UNITS, TransactionId,
        defaults,
    };
    use rusqlite::Connection;
    use rusqlite::types::Value;

    use crate::{DatabaseKey, Store, StoreError};

    /// Fixture F3, which v1's own seeding and use cases wrote (`tests/fixtures/README.md`).
    const LEDGER_FIXTURE: &str = concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/tests/fixtures/v1-f3-ledger.db"
    );

    const DEFAULT_ICON: &str = defaults::DEFAULT_CATEGORY_ICON;
    const DEFAULT_PERCENT: i32 = Budget::DEFAULT_WARNING_PERCENT;

    fn scratch_dir(test: &str) -> PathBuf {
        let dir = std::env::temp_dir().join(format!(
            "denarii-store-reference-{}-{test}",
            std::process::id()
        ));
        let _ = fs::remove_dir_all(&dir); // Left over from an earlier run, or absent.
        fs::create_dir_all(&dir).unwrap();
        dir
    }

    fn fixture_key() -> DatabaseKey {
        DatabaseKey::from_bytes(std::array::from_fn(|i| u8::try_from(i).unwrap()))
    }

    /// A new database with what v1's `ManageReferenceDataUseCaseTest` starts from: `TestData`'s
    /// three accounts and four categories (`app/src/test/.../testutil/FakeRepositories.kt` at
    /// `6d57526`), no budget, and one 2,000-cent expense from account 3 in category 4.
    fn test_store(test: &str) -> Store {
        let store = Store::open(scratch_dir(test).join("ledger.db"), &fixture_key()).unwrap();
        store
            .lock()
            .unwrap()
            .execute_batch(
                "INSERT INTO accounts (id, name, balanceCents) \
                 VALUES (1, 'Cash', 0), (2, 'Savings', 0), (3, 'Checking', -2000);
                 INSERT INTO categories (id, name, iconName) VALUES \
                 (1, 'General Expense', 'ic_category_default'), (2, 'General Income', 'income'), \
                 (3, 'Transfer', 'transfer'), (4, 'Groceries', 'ic_category_default');
                 INSERT INTO transactions (id, type, description, amountCents, categoryId, \
                 accountId, transferAccountId, dateEpochMillis, createdAtEpochMillis) \
                 VALUES (1, 'EXPENSE', 'Coffee', 2000, 4, 3, NULL, 1795435200000, 1);",
            )
            .unwrap();
        store
    }

    fn ledger_store(test: &str) -> Store {
        let path = scratch_dir(test).join("ledger.db");
        fs::copy(LEDGER_FIXTURE, &path).unwrap();
        Store::open(&path, &fixture_key()).unwrap()
    }

    /// Stores a transaction directly, as a test's starting data.
    fn insert_transaction(store: &Store, category: i64, account: i64, destination: Option<i64>) {
        let kind = if destination.is_some() {
            "TRANSFER"
        } else {
            "EXPENSE"
        };
        store
            .lock()
            .unwrap()
            .execute(
                "INSERT INTO transactions (type, description, amountCents, categoryId, \
                 accountId, transferAccountId, dateEpochMillis, createdAtEpochMillis) \
                 VALUES (?1, 'Test', 100, ?2, ?3, ?4, 1795435200000, 1)",
                rusqlite::params![kind, category, account, destination],
            )
            .unwrap();
    }

    /// The business rule a failed write broke.
    fn rule<T: std::fmt::Debug>(result: Result<T, StoreError>) -> DomainError {
        match result {
            Err(StoreError::Domain(error)) => error,
            other => panic!("expected a broken business rule, got {other:?}"),
        }
    }

    fn invalid_name() -> DomainError {
        DomainError::InvalidName {
            max_units: MAX_NAME_UNITS,
        }
    }

    fn duplicate_category(name: &str) -> DomainError {
        DomainError::DuplicateCategoryName {
            name: name.to_owned(),
        }
    }

    fn duplicate_account(name: &str) -> DomainError {
        DomainError::DuplicateAccountName {
            name: name.to_owned(),
        }
    }

    fn text(value: &str) -> Value {
        Value::Text(value.to_owned())
    }

    fn rows(connection: &Connection, sql: &str) -> Vec<Vec<Value>> {
        let mut statement = connection.prepare(sql).unwrap();
        let columns = statement.column_count();
        statement
            .query_map([], |row| (0..columns).map(|i| row.get(i)).collect())
            .unwrap()
            .collect::<Result<_, _>>()
            .unwrap()
    }

    fn query(store: &Store, sql: &str) -> Vec<Vec<Value>> {
        rows(&store.lock().unwrap(), sql)
    }

    /// Every row of every table the use cases read or write, to show that a failed write changed
    /// nothing.
    fn snapshot(store: &Store) -> Vec<Vec<Value>> {
        let connection = store.lock().unwrap();
        let mut all = Vec::new();
        for table in [
            "accounts",
            "budgets",
            "categories",
            "transactions",
            "sqlite_sequence",
        ] {
            all.push(vec![text(table)]);
            all.extend(rows(
                &connection,
                &format!("SELECT * FROM {table} ORDER BY 1"),
            ));
        }
        all
    }

    fn category_row(store: &Store, id: i64) -> Vec<Vec<Value>> {
        query(
            store,
            &format!("SELECT name, iconName FROM categories WHERE id = {id}"),
        )
    }

    fn account_row(store: &Store, id: i64) -> Vec<Vec<Value>> {
        query(
            store,
            &format!("SELECT name, balanceCents FROM accounts WHERE id = {id}"),
        )
    }

    fn budgets(store: &Store) -> Vec<Vec<Value>> {
        query(
            store,
            "SELECT id, categoryId, monthlyLimitCents, warningThresholdPercent \
             FROM budgets ORDER BY id",
        )
    }

    fn budget_row(id: i64, category: i64, limit: i64, percent: i64) -> Vec<Value> {
        [id, category, limit, percent].map(Value::Integer).to_vec()
    }

    // v1's `ManageReferenceDataUseCaseTest`.

    #[test]
    fn a_new_category_is_stored_trimmed_and_a_duplicate_in_another_case_is_refused() {
        let store = test_store(
            "a_new_category_is_stored_trimmed_and_a_duplicate_in_another_case_is_refused",
        );
        assert_eq!(
            store.add_category("  Dining  ", DEFAULT_ICON).unwrap(),
            CategoryId(5)
        );
        assert_eq!(
            category_row(&store, 5),
            [[text("Dining"), text(DEFAULT_ICON)]]
        );
        let before = snapshot(&store);
        assert_eq!(
            rule(store.add_category("groceries", DEFAULT_ICON)),
            duplicate_category("groceries")
        );
        assert_eq!(
            rule(store.add_category("   ", DEFAULT_ICON)),
            invalid_name()
        );
        assert_eq!(snapshot(&store), before);
    }

    #[test]
    fn a_category_rename_may_keep_its_own_name_in_another_case() {
        let store = test_store("a_category_rename_may_keep_its_own_name_in_another_case");
        store
            .update_category(CategoryId(4), "GROCERIES", None)
            .unwrap();
        assert_eq!(
            category_row(&store, 4),
            [[text("GROCERIES"), text(DEFAULT_ICON)]]
        );
        let before = snapshot(&store);
        assert_eq!(
            rule(store.update_category(CategoryId(4), "Transfer", None)),
            duplicate_category("Transfer")
        );
        assert!(matches!(
            store.update_category(CategoryId(99), "Other", None),
            Err(StoreError::CategoryNotFound { id: CategoryId(99) })
        ));
        assert_eq!(snapshot(&store), before);
    }

    #[test]
    fn a_category_rename_keeps_its_icon_unless_it_is_given_one() {
        let store = test_store("a_category_rename_keeps_its_icon_unless_it_is_given_one");
        let id = store.add_category("Pets", "ic_pets").unwrap();
        store.update_category(id, "Pet Care", None).unwrap();
        assert_eq!(
            category_row(&store, id.0),
            [[text("Pet Care"), text("ic_pets")]]
        );
        store
            .update_category(id, "Pet Care", Some(" \u{A0}"))
            .unwrap();
        assert_eq!(
            category_row(&store, id.0),
            [[text("Pet Care"), text("ic_pets")]]
        );
        store
            .update_category(id, "Pet Care", Some("gifts"))
            .unwrap();
        assert_eq!(
            category_row(&store, id.0),
            [[text("Pet Care"), text("gifts")]]
        );
    }

    #[test]
    fn a_default_or_used_category_cant_be_deleted() {
        let store = test_store("a_default_or_used_category_cant_be_deleted");
        let before = snapshot(&store);
        for id in 1..=3 {
            assert_eq!(
                rule(store.delete_category(CategoryId(id))),
                DomainError::DefaultCategory
            );
        }
        assert_eq!(
            rule(store.delete_category(CategoryId(4))),
            DomainError::CategoryInUse { transactions: 1 }
        );
        assert_eq!(snapshot(&store), before);
        let id = store.add_category("Temp", DEFAULT_ICON).unwrap();
        store.delete_category(id).unwrap();
        assert_eq!(category_row(&store, id.0), Vec::<Vec<Value>>::new());
    }

    #[test]
    fn a_new_account_keeps_its_opening_balance_and_a_duplicate_is_refused() {
        let store =
            test_store("a_new_account_keeps_its_opening_balance_and_a_duplicate_is_refused");
        assert_eq!(
            store.add_account("Credit Card", Cents(-25_000)).unwrap(),
            AccountId(4)
        );
        assert_eq!(
            account_row(&store, 4),
            [[text("Credit Card"), Value::Integer(-25_000)]]
        );
        let before = snapshot(&store);
        assert_eq!(
            rule(store.add_account("cash", Cents(0))),
            duplicate_account("cash")
        );
        assert_eq!(rule(store.add_account("", Cents(0))), invalid_name());
        assert_eq!(snapshot(&store), before);
    }

    #[test]
    fn a_default_or_used_account_cant_be_deleted() {
        let store = test_store("a_default_or_used_account_cant_be_deleted");
        let before = snapshot(&store);
        for id in 1..=2 {
            assert_eq!(
                rule(store.delete_account(AccountId(id))),
                DomainError::DefaultAccount
            );
        }
        assert_eq!(
            rule(store.delete_account(AccountId(3))),
            DomainError::AccountInUse { transactions: 1 }
        );
        assert_eq!(snapshot(&store), before);
        // An unused account goes whatever its balance.
        let id = store.add_account("Temp", Cents(5_000)).unwrap();
        store.delete_account(id).unwrap();
        assert_eq!(account_row(&store, id.0), Vec::<Vec<Value>>::new());
    }

    #[test]
    fn setting_a_budget_validates_it_and_replaces_the_categorys_budget() {
        let store = test_store("setting_a_budget_validates_it_and_replaces_the_categorys_budget");
        let first = store
            .set_budget(CategoryId(4), Cents(20_000), DEFAULT_PERCENT)
            .unwrap();
        let second = store.set_budget(CategoryId(4), Cents(30_000), 90).unwrap();
        assert_eq!(first, BudgetId(1));
        assert_eq!(second, first);
        assert_eq!(budgets(&store), [budget_row(1, 4, 30_000, 90)]);
        let before = snapshot(&store);
        assert_eq!(
            rule(store.set_budget(CategoryId(4), Cents(0), DEFAULT_PERCENT)),
            DomainError::InvalidBudgetLimit
        );
        assert_eq!(
            rule(store.set_budget(CategoryId(4), Cents(10_000), 0)),
            DomainError::InvalidWarningPercent
        );
        assert_eq!(
            rule(store.set_budget(CategoryId(99), Cents(10_000), DEFAULT_PERCENT)),
            DomainError::CategoryNotFound
        );
        assert_eq!(snapshot(&store), before);
    }

    #[test]
    fn a_budget_is_deleted_once() {
        let store = test_store("a_budget_is_deleted_once");
        store
            .set_budget(CategoryId(4), Cents(20_000), DEFAULT_PERCENT)
            .unwrap();
        store.delete_budget(CategoryId(4)).unwrap();
        assert_eq!(budgets(&store), Vec::<Vec<Value>>::new());
        assert!(matches!(
            store.delete_budget(CategoryId(4)),
            Err(StoreError::BudgetNotFound {
                category: CategoryId(4)
            })
        ));
        assert!(matches!(
            store.delete_budget(CategoryId(99)),
            Err(StoreError::BudgetNotFound {
                category: CategoryId(99)
            })
        ));
    }

    // The order of v1's checks, and the edges of each rule.

    #[test]
    fn a_category_edit_checks_the_id_then_the_row_then_the_name() {
        let store = test_store("a_category_edit_checks_the_id_then_the_row_then_the_name");
        let before = snapshot(&store);
        for id in [0, -1] {
            assert_eq!(
                rule(store.update_category(CategoryId(id), "   ", None)),
                DomainError::IdRequired
            );
        }
        assert!(matches!(
            store.update_category(CategoryId(99), "   ", None),
            Err(StoreError::CategoryNotFound { id: CategoryId(99) })
        ));
        assert_eq!(
            rule(store.update_category(CategoryId(4), "   ", None)),
            invalid_name()
        );
        assert_eq!(
            rule(store.update_category(CategoryId(4), &"x".repeat(51), None)),
            invalid_name()
        );
        assert_eq!(
            rule(store.update_category(CategoryId(4), " transfer\u{3000}", Some("gifts"))),
            duplicate_category("transfer")
        );
        assert_eq!(snapshot(&store), before);
    }

    #[test]
    fn an_account_rename_checks_the_id_then_the_name_then_the_row() {
        let store = test_store("an_account_rename_checks_the_id_then_the_name_then_the_row");
        let before = snapshot(&store);
        for id in [0, -1] {
            assert_eq!(
                rule(store.rename_account(AccountId(id), "Other")),
                DomainError::IdRequired
            );
        }
        // v1 checked the name before it looked for the row.
        assert_eq!(
            rule(store.rename_account(AccountId(99), "  ")),
            invalid_name()
        );
        assert_eq!(
            rule(store.rename_account(AccountId(99), "CASH")),
            duplicate_account("CASH")
        );
        assert!(matches!(
            store.rename_account(AccountId(99), "Other"),
            Err(StoreError::AccountNotFound { id: AccountId(99) })
        ));
        assert_eq!(snapshot(&store), before);
        // A seeded account can be renamed, and an account may keep its own name in another case.
        store.rename_account(AccountId(1), " Wallet ").unwrap();
        store.rename_account(AccountId(2), "SAVINGS").unwrap();
        assert_eq!(
            account_row(&store, 1),
            [[text("Wallet"), Value::Integer(0)]]
        );
        assert_eq!(
            account_row(&store, 2),
            [[text("SAVINGS"), Value::Integer(0)]]
        );
        assert_eq!(
            account_row(&store, 3),
            [[text("Checking"), Value::Integer(-2_000)]]
        );
    }

    /// v1's SQLCipher had no ICU, so `LOWER` folded only the ASCII letters: `É` stays `É`.
    #[test]
    fn names_differ_only_in_the_case_of_ascii_letters_to_be_duplicates() {
        let store = test_store("names_differ_only_in_the_case_of_ascii_letters_to_be_duplicates");
        store.add_category("\u{c9}picerie", DEFAULT_ICON).unwrap();
        assert_eq!(
            rule(store.add_category("\u{c9}PICERIE", DEFAULT_ICON)),
            duplicate_category("\u{c9}PICERIE")
        );
        assert_eq!(
            store.add_category("\u{e9}picerie", DEFAULT_ICON).unwrap(),
            CategoryId(6)
        );
        store.add_account("Cr\u{e9}dit", Cents(0)).unwrap();
        assert_eq!(
            rule(store.add_account("CR\u{e9}DIT", Cents(0))),
            duplicate_account("CR\u{e9}DIT")
        );
        assert_eq!(
            store.add_account("CR\u{c9}DIT", Cents(0)).unwrap(),
            AccountId(5)
        );
        // Kotlin's whitespace is trimmed before the comparison.
        assert_eq!(
            rule(store.add_account("\u{A0}cash\u{1F}", Cents(0))),
            duplicate_account("cash")
        );
    }

    #[test]
    fn a_name_may_have_up_to_50_utf16_code_units() {
        let store = test_store("a_name_may_have_up_to_50_utf16_code_units");
        let coins = "\u{1f4b0}".repeat(25);
        let id = store.add_category(&coins, DEFAULT_ICON).unwrap();
        assert_eq!(
            category_row(&store, id.0),
            [[text(&coins), text(DEFAULT_ICON)]]
        );
        assert_eq!(
            rule(store.add_category(&format!("{coins}x"), DEFAULT_ICON)),
            invalid_name()
        );
        let longest = "x".repeat(50);
        store
            .add_account(&format!("  {longest}  "), Cents(0))
            .unwrap();
        assert_eq!(
            rule(store.add_account(&"y".repeat(51), Cents(0))),
            invalid_name()
        );
        assert_eq!(
            rule(store.rename_account(AccountId(1), &"y".repeat(51))),
            invalid_name()
        );
    }

    #[test]
    fn a_blank_icon_stores_the_default_icon_and_any_other_is_kept_as_given() {
        let store =
            test_store("a_blank_icon_stores_the_default_icon_and_any_other_is_kept_as_given");
        let blank = store.add_category("Pets", " \t").unwrap();
        let empty = store.add_category("Fun", "").unwrap();
        let unknown = store.add_category("Odd", " not_a_key ").unwrap();
        assert_eq!(
            category_row(&store, blank.0),
            [[text("Pets"), text(DEFAULT_ICON)]]
        );
        assert_eq!(
            category_row(&store, empty.0),
            [[text("Fun"), text(DEFAULT_ICON)]]
        );
        assert_eq!(
            category_row(&store, unknown.0),
            [[text("Odd"), text(" not_a_key ")]]
        );
    }

    /// The seeded rows are refused before their transactions are counted, as in v1.
    #[test]
    fn a_used_default_category_or_account_is_refused_as_a_default() {
        let store = test_store("a_used_default_category_or_account_is_refused_as_a_default");
        insert_transaction(&store, 1, 1, None);
        assert_eq!(
            rule(store.delete_category(CategoryId(1))),
            DomainError::DefaultCategory
        );
        assert_eq!(
            rule(store.delete_account(AccountId(1))),
            DomainError::DefaultAccount
        );
    }

    #[test]
    fn a_transfers_destination_counts_as_a_use_of_the_account() {
        let store = test_store("a_transfers_destination_counts_as_a_use_of_the_account");
        let wallet = store.add_account("Wallet", Cents(0)).unwrap();
        insert_transaction(&store, 3, 3, Some(wallet.0));
        let before = snapshot(&store);
        assert_eq!(
            rule(store.delete_account(wallet)),
            DomainError::AccountInUse { transactions: 1 }
        );
        assert_eq!(
            rule(store.delete_account(AccountId(3))),
            DomainError::AccountInUse { transactions: 2 }
        );
        assert_eq!(
            rule(store.delete_category(CategoryId(4))),
            DomainError::CategoryInUse { transactions: 1 }
        );
        assert_eq!(snapshot(&store), before);
    }

    /// v1 didn't check a delete's ID: one with no row, 0 and below included, wasn't found.
    #[test]
    fn deleting_a_missing_category_or_account_reports_its_id() {
        let store = test_store("deleting_a_missing_category_or_account_reports_its_id");
        let before = snapshot(&store);
        for id in [99, 0, -1] {
            assert!(matches!(
                store.delete_category(CategoryId(id)),
                Err(StoreError::CategoryNotFound { id: found }) if found == CategoryId(id)
            ));
            assert!(matches!(
                store.delete_account(AccountId(id)),
                Err(StoreError::AccountNotFound { id: found }) if found == AccountId(id)
            ));
        }
        assert_eq!(snapshot(&store), before);
    }

    #[test]
    fn deleting_a_category_deletes_its_budget() {
        let store = test_store("deleting_a_category_deletes_its_budget");
        let pets = store.add_category("Pets", DEFAULT_ICON).unwrap();
        store.set_budget(pets, Cents(5_000), 50).unwrap();
        store
            .set_budget(CategoryId(4), Cents(9_000), DEFAULT_PERCENT)
            .unwrap();
        store.delete_category(pets).unwrap();
        assert_eq!(budgets(&store), [budget_row(2, 4, 9_000, 80)]);
        assert_eq!(category_row(&store, pets.0), Vec::<Vec<Value>>::new());
    }

    #[test]
    fn a_budgets_limit_and_threshold_are_checked_in_v1s_order() {
        let store = test_store("a_budgets_limit_and_threshold_are_checked_in_v1s_order");
        let before = snapshot(&store);
        let cases = [
            (99, 0, 0, DomainError::InvalidBudgetLimit),
            (99, -1, DEFAULT_PERCENT, DomainError::InvalidBudgetLimit),
            (99, 1, 0, DomainError::InvalidWarningPercent),
            (99, 1, 101, DomainError::InvalidWarningPercent),
            (99, 1, -80, DomainError::InvalidWarningPercent),
            (99, 1, 100, DomainError::CategoryNotFound),
            (0, 1, 1, DomainError::CategoryNotFound),
        ];
        for (category, limit, percent, error) in cases {
            assert_eq!(
                rule(store.set_budget(CategoryId(category), Cents(limit), percent)),
                error,
                "{category}, {limit}, {percent}"
            );
        }
        assert_eq!(snapshot(&store), before);
        store.set_budget(CategoryId(4), Cents(1), 1).unwrap();
        assert_eq!(budgets(&store), [budget_row(1, 4, 1, 1)]);
        store
            .set_budget(CategoryId(4), Cents(i64::MAX), 100)
            .unwrap();
        assert_eq!(budgets(&store), [budget_row(1, 4, i64::MAX, 100)]);
    }

    #[test]
    fn a_deleted_rows_id_is_never_handed_out_again() {
        let store = test_store("a_deleted_rows_id_is_never_handed_out_again");
        let pets = store.add_category("Pets", DEFAULT_ICON).unwrap();
        store.delete_category(pets).unwrap();
        assert_eq!(
            store.add_category("Pets", DEFAULT_ICON).unwrap(),
            CategoryId(6)
        );
        let temp = store.add_account("Temp", Cents(0)).unwrap();
        store.delete_account(temp).unwrap();
        assert_eq!(store.add_account("Temp", Cents(0)).unwrap(), AccountId(5));
        assert_eq!(
            store.set_budget(CategoryId(4), Cents(100), 80).unwrap(),
            BudgetId(1)
        );
        assert_eq!(
            store.set_budget(CategoryId(1), Cents(100), 80).unwrap(),
            BudgetId(2)
        );
        store.delete_budget(CategoryId(4)).unwrap();
        assert_eq!(
            store.set_budget(CategoryId(4), Cents(100), 80).unwrap(),
            BudgetId(3)
        );
    }

    /// The duplicate check and the insert run under one lock, so a name added from several
    /// threads at once is stored once. v1's check ran outside a transaction, where the unique
    /// index would have failed the second insert with a SQLite error.
    #[test]
    fn a_name_added_from_several_threads_at_once_is_stored_once() {
        /// The name's number, and how adding a category and an account with that name went.
        type Attempt = (
            usize,
            Result<CategoryId, StoreError>,
            Result<AccountId, StoreError>,
        );
        const THREADS: usize = 8;
        const NAMES: usize = 50;
        let store = test_store("a_name_added_from_several_threads_at_once_is_stored_once");
        let start = std::sync::Barrier::new(THREADS);
        let results: Vec<Attempt> = std::thread::scope(|scope| {
            let threads: Vec<_> = (0..THREADS)
                .map(|_| {
                    scope.spawn(|| {
                        start.wait();
                        (0..NAMES)
                            .map(|n| {
                                let name = format!("Name {n}");
                                let category = store.add_category(&name, DEFAULT_ICON);
                                let account = store.add_account(&name, Cents(0));
                                (n, category, account)
                            })
                            .collect::<Vec<_>>()
                    })
                })
                .collect();
            threads
                .into_iter()
                .flat_map(|thread| thread.join().unwrap())
                .collect()
        });
        let mut added = [(0, 0); NAMES];
        for (n, category, account) in results {
            let name = format!("Name {n}");
            match category {
                Ok(_) => added[n].0 += 1,
                refused => assert_eq!(rule(refused), duplicate_category(&name)),
            }
            match account {
                Ok(_) => added[n].1 += 1,
                refused => assert_eq!(rule(refused), duplicate_account(&name)),
            }
        }
        assert_eq!(added, [(1, 1); NAMES]);
        assert_eq!(
            query(
                &store,
                "SELECT (SELECT count(*) FROM categories), (SELECT count(*) FROM accounts)"
            ),
            [[Value::Integer(4 + 50), Value::Integer(3 + 50)]]
        );
    }

    /// A file whose tables are missing fails with SQLite's error and what the store was doing,
    /// and the tables that are left don't change.
    #[test]
    fn a_damaged_file_fails_with_sqlites_error_and_changes_nothing() {
        let store = test_store("a_damaged_file_fails_with_sqlites_error_and_changes_nothing");
        store
            .lock()
            .unwrap()
            .execute_batch("DROP TABLE budgets; DROP TABLE transactions;")
            .unwrap();
        let remaining = || {
            [
                query(&store, "SELECT * FROM categories ORDER BY id"),
                query(&store, "SELECT * FROM accounts ORDER BY id"),
            ]
        };
        let before = remaining();
        let failed_to = |result: Result<(), StoreError>| match result {
            Err(StoreError::Sqlite { operation, .. }) => operation,
            other => panic!("expected a SQLite error, got {other:?}"),
        };
        assert_eq!(
            failed_to(
                store
                    .set_budget(CategoryId(4), Cents(100), DEFAULT_PERCENT)
                    .map(|_| ())
            ),
            "read the category's budget"
        );
        assert_eq!(
            failed_to(store.delete_budget(CategoryId(4))),
            "delete the budget"
        );
        assert_eq!(
            failed_to(store.delete_category(CategoryId(4))),
            "count the transactions that use the row"
        );
        assert_eq!(
            failed_to(store.delete_account(AccountId(3))),
            "count the transactions that use the row"
        );
        assert_eq!(remaining(), before);
    }

    // Fixture F3, which v1's use cases wrote.

    /// F3's transactions use each category and account as `tests/fixtures/README.md` lists them;
    /// row 10 is a transfer into account 3, and row 12 a transfer with no destination.
    #[test]
    fn the_f3_fixtures_used_categories_and_accounts_report_how_many_transactions_use_them() {
        let store = ledger_store(
            "the_f3_fixtures_used_categories_and_accounts_report_how_many_transactions_use_them",
        );
        let before = snapshot(&store);
        for (category, transactions) in [(4, 3), (5, 1), (6, 1)] {
            assert_eq!(
                rule(store.delete_category(CategoryId(category))),
                DomainError::CategoryInUse { transactions }
            );
        }
        for category in 1..=3 {
            assert_eq!(
                rule(store.delete_category(CategoryId(category))),
                DomainError::DefaultCategory
            );
        }
        for (account, transactions) in [(3, 6), (4, 4)] {
            assert_eq!(
                rule(store.delete_account(AccountId(account))),
                DomainError::AccountInUse { transactions }
            );
        }
        for account in 1..=2 {
            assert_eq!(
                rule(store.delete_account(AccountId(account))),
                DomainError::DefaultAccount
            );
        }
        assert!(matches!(
            store.delete_account(AccountId(5)),
            Err(StoreError::AccountNotFound { id: AccountId(5) })
        ));
        assert_eq!(snapshot(&store), before);
    }

    /// F3's names hold non-ASCII letters and emoji, and v1 deleted account 5.
    #[test]
    fn the_f3_fixture_takes_new_names_and_budgets_after_its_last_ids() {
        let store = ledger_store("the_f3_fixture_takes_new_names_and_budgets_after_its_last_ids");
        assert_eq!(
            rule(store.add_category("\u{c9}PICERIE \u{1f6d2}", "groceries")),
            duplicate_category("\u{c9}PICERIE \u{1f6d2}")
        );
        assert_eq!(
            rule(store.add_account("carte de cr\u{e9}dit", Cents(0))),
            duplicate_account("carte de cr\u{e9}dit")
        );
        assert_eq!(
            store
                .add_category("\u{e9}picerie \u{1f6d2}", "groceries")
                .unwrap(),
            CategoryId(7)
        );
        // The deleted account's name is free again, and its ID stays used.
        assert_eq!(
            store.add_account("Compte ferm\u{e9}", Cents(0)).unwrap(),
            AccountId(6)
        );
        assert_eq!(
            store.set_budget(CategoryId(4), Cents(45_000), 90).unwrap(),
            BudgetId(1)
        );
        assert_eq!(
            store
                .set_budget(CategoryId(6), Cents(5_000), DEFAULT_PERCENT)
                .unwrap(),
            BudgetId(3)
        );
        assert_eq!(
            budgets(&store),
            [
                budget_row(1, 4, 45_000, 90),
                budget_row(2, 5, 15_000, 80),
                budget_row(3, 6, 5_000, 80)
            ]
        );
        store
            .update_category(CategoryId(4), "\u{c9}picerie", None)
            .unwrap();
        assert_eq!(
            category_row(&store, 4),
            [[text("\u{c9}picerie"), text("groceries")]]
        );
    }

    /// Once row 3, the only expense in "Restaurants", is deleted, the category goes, and its
    /// 15,000-cent budget with it.
    #[test]
    fn deleting_the_f3_fixtures_emptied_category_deletes_its_budget() {
        let store = ledger_store("deleting_the_f3_fixtures_emptied_category_deletes_its_budget");
        store.delete_transaction(TransactionId(3)).unwrap();
        store.delete_category(CategoryId(5)).unwrap();
        assert_eq!(budgets(&store), [budget_row(1, 4, 40_000, 75)]);
        assert_eq!(category_row(&store, 5), Vec::<Vec<Value>>::new());
        assert_eq!(
            query(&store, "PRAGMA foreign_key_check"),
            Vec::<Vec<Value>>::new()
        );
    }
}
