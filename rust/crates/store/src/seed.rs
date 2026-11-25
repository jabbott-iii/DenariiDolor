/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Seeding the accounts and categories that every install starts with.
//!
//! This ports v1's `DefaultDataInitializer` (at `6d57526`), which ran after every sign-in: in one
//! database transaction, it stored the default accounts if `accounts` was empty and the default
//! categories if `categories` was empty, with the IDs and icons in
//! [`defaults`](denarii_core::defaults) and names in the phone's language at that moment. A table
//! that holds any row is left as it is, so the user's names for the defaults are kept, and running
//! it again changes nothing.
//!
//! Rust holds no text that users read, so the caller passes the names. They cross the bridge from
//! Dart, so they go through the same name rules as any other category or account, which v1's
//! translations always met.

use denarii_core::{AccountId, CategoryId, defaults, validate_name};
use rusqlite::{Connection, params};

use crate::error::{StoreError, sqlite};
use crate::reference::{NO_ROW, NamedTable, require_unique_name};
use crate::store::Store;
use crate::transactions::{begin, commit};

/// The names of the seeded accounts and categories, in the user's language.
///
/// Dart reads them from the language pack chosen for the phone's languages: the messages
/// `defaultAccountCash`, `defaultAccountSavings`, `defaultCategoryExpense`,
/// `defaultCategoryIncome`, and `defaultCategoryTransfer`. The English values are in each field's
/// docs.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DefaultNames {
    /// The name of account [`CASH_ACCOUNT`](defaults::CASH_ACCOUNT): "Cash" in English.
    pub cash_account: String,
    /// The name of account [`SAVINGS_ACCOUNT`](defaults::SAVINGS_ACCOUNT): "Savings" in English.
    pub savings_account: String,
    /// The name of category [`EXPENSE_CATEGORY`](defaults::EXPENSE_CATEGORY): "General Expense"
    /// in English.
    pub expense_category: String,
    /// The name of category [`INCOME_CATEGORY`](defaults::INCOME_CATEGORY): "General Income" in
    /// English.
    pub income_category: String,
    /// The name of category [`TRANSFER_CATEGORY`](defaults::TRANSFER_CATEGORY): "Transfer" in
    /// English.
    pub transfer_category: String,
}

impl Store {
    /// Stores the default accounts if `accounts` is empty, and the default categories if
    /// `categories` is empty, with the names in `names`.
    ///
    /// Each default gets its ID from [`defaults`](denarii_core::defaults), whatever ID SQLite
    /// would hand out next. The accounts' balances start at 0 cents, and each category gets its
    /// icon key from [`defaults::CATEGORIES`]. The names are stored trimmed with Kotlin's
    /// whitespace. A table that holds any row is left as it is, whatever it holds, so seeding again
    /// changes nothing. v1 seeded after every sign-in, which made the tables of a new or replaced
    /// database whole before its first screen.
    ///
    /// # Errors
    ///
    /// The first of these that applies. Nothing is written when any of them is returned.
    ///
    /// 1. [`StoreError::Domain`] with [`DomainError::InvalidName`](denarii_core::DomainError) if
    ///    any of the five trimmed names is empty or longer than
    ///    [`MAX_NAME_UNITS`](denarii_core::MAX_NAME_UNITS), whatever the tables hold.
    /// 2. [`StoreError::Domain`] with
    ///    [`DomainError::DuplicateAccountName`](denarii_core::DomainError) or
    ///    [`DomainError::DuplicateCategoryName`](denarii_core::DomainError), carrying the later
    ///    name, if two names that an empty table would get are the same, with ASCII letters
    ///    compared without regard to case, as every other name is checked.
    /// 3. [`StoreError::Poisoned`] or [`StoreError::Sqlite`] if the connection can't be used, or
    ///    SQLite fails.
    ///
    /// # Examples
    ///
    /// ```
    /// use denarii_store::{DatabaseKey, DefaultNames, Store};
    ///
    /// # fn main() -> Result<(), Box<dyn std::error::Error>> {
    /// # let dir = std::env::temp_dir().join(format!("denarii-store-seed-doc-{}", std::process::id()));
    /// # std::fs::create_dir_all(&dir)?;
    /// // A test value; the real key comes from the vault.
    /// let key = DatabaseKey::from_bytes([7; DatabaseKey::LEN]);
    /// let store = Store::open(dir.join("denarii_dolor.db"), &key)?;
    /// let english = DefaultNames {
    ///     cash_account: "Cash".to_owned(),
    ///     savings_account: "Savings".to_owned(),
    ///     expense_category: "General Expense".to_owned(),
    ///     income_category: "General Income".to_owned(),
    ///     transfer_category: "Transfer".to_owned(),
    /// };
    /// store.seed_defaults(&english)?;
    /// // The tables now hold rows, so seeding again leaves them as they are.
    /// store.seed_defaults(&english)?;
    /// store.close()?;
    /// # std::fs::remove_dir_all(&dir)?;
    /// # Ok(())
    /// # }
    /// ```
    pub fn seed_defaults(&self, names: &DefaultNames) -> Result<(), StoreError> {
        let valid = |name| validate_name(name).map_err(StoreError::Domain);
        let accounts: [(AccountId, &str); 2] = [
            (defaults::CASH_ACCOUNT, valid(&names.cash_account)?),
            (defaults::SAVINGS_ACCOUNT, valid(&names.savings_account)?),
        ];
        let category_names = [
            valid(&names.expense_category)?,
            valid(&names.income_category)?,
            valid(&names.transfer_category)?,
        ];
        let mut connection = self.lock()?;
        let write = begin(&mut connection)?;
        if is_empty(&write, NamedTable::Accounts)? {
            for (id, name) in accounts {
                insert_account(&write, id, name)?;
            }
        }
        if is_empty(&write, NamedTable::Categories)? {
            for (&(id, icon_name), name) in defaults::CATEGORIES.iter().zip(category_names) {
                insert_category(&write, id, name, icon_name)?;
            }
        }
        commit(write)
    }
}

/// Whether `table` holds no row, by v1's `count()` query of `AccountDao` and `CategoryDao`.
fn is_empty(connection: &Connection, table: NamedTable) -> Result<bool, StoreError> {
    let (sql, operation) = match table {
        NamedTable::Accounts => ("SELECT COUNT(*) FROM accounts", "count the accounts"),
        NamedTable::Categories => ("SELECT COUNT(*) FROM categories", "count the categories"),
    };
    let rows: i64 = connection
        .query_row(sql, [], |row| row.get(0))
        .map_err(|source| sqlite(operation, source))?;
    Ok(rows == 0)
}

/// Stores a default account with a balance of 0 cents, after v1's duplicate check against the
/// accounts already stored.
fn insert_account(connection: &Connection, id: AccountId, name: &str) -> Result<(), StoreError> {
    require_unique_name(connection, NamedTable::Accounts, name, NO_ROW)?;
    connection
        .execute(
            "INSERT INTO accounts (id, name, balanceCents) VALUES (?1, ?2, 0)",
            params![id.0, name],
        )
        .map_err(|source| sqlite("insert a default account", source))?;
    Ok(())
}

/// Stores a default category, after v1's duplicate check against the categories already stored.
fn insert_category(
    connection: &Connection,
    id: CategoryId,
    name: &str,
    icon_name: &str,
) -> Result<(), StoreError> {
    require_unique_name(connection, NamedTable::Categories, name, NO_ROW)?;
    connection
        .execute(
            "INSERT INTO categories (id, name, iconName) VALUES (?1, ?2, ?3)",
            params![id.0, name, icon_name],
        )
        .map_err(|source| sqlite("insert a default category", source))?;
    Ok(())
}

#[cfg(test)]
mod tests {
    use std::fs;
    use std::path::{Path, PathBuf};

    use denarii_core::{AccountId, CategoryId, Cents, DomainError, MAX_NAME_UNITS, defaults};
    use rusqlite::Connection;
    use rusqlite::types::Value;

    use crate::{DatabaseKey, DefaultNames, Store, StoreError};

    /// Fixture F3, whose defaults v1's `DefaultDataInitializer` seeded in English
    /// (`tests/fixtures/README.md`).
    const LEDGER_FIXTURE: &str = concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/tests/fixtures/v1-f3-ledger.db"
    );

    fn scratch_dir(test: &str) -> PathBuf {
        let dir =
            std::env::temp_dir().join(format!("denarii-store-seed-{}-{test}", std::process::id()));
        let _ = fs::remove_dir_all(&dir); // Left over from an earlier run, or absent.
        fs::create_dir_all(&dir).unwrap();
        dir
    }

    fn fixture_key() -> DatabaseKey {
        DatabaseKey::from_bytes(std::array::from_fn(|i| u8::try_from(i).unwrap()))
    }

    fn new_store(test: &str) -> Store {
        Store::open(scratch_dir(test).join("ledger.db"), &fixture_key()).unwrap()
    }

    fn copy_ledger_fixture(test: &str) -> PathBuf {
        let path = scratch_dir(test).join("ledger.db");
        fs::copy(LEDGER_FIXTURE, &path).unwrap();
        path
    }

    fn open(path: &Path) -> Store {
        Store::open(path, &fixture_key()).unwrap()
    }

    /// v1's English names (`app/src/main/res/values/strings.xml` at `6d57526`), which are also
    /// `lib/l10n/app_en.arb`'s.
    fn english() -> DefaultNames {
        DefaultNames {
            cash_account: "Cash".to_owned(),
            savings_account: "Savings".to_owned(),
            expense_category: "General Expense".to_owned(),
            income_category: "General Income".to_owned(),
            transfer_category: "Transfer".to_owned(),
        }
    }

    /// `lib/l10n/app_fr.arb`'s names, which differ from the English ones in every field.
    fn french() -> DefaultNames {
        DefaultNames {
            cash_account: "Esp\u{e8}ces".to_owned(),
            savings_account: "\u{c9}pargne".to_owned(),
            expense_category: "D\u{e9}penses g\u{e9}n\u{e9}rales".to_owned(),
            income_category: "Revenus g\u{e9}n\u{e9}raux".to_owned(),
            transfer_category: "Virement".to_owned(),
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

    fn execute(store: &Store, sql: &str) {
        store.lock().unwrap().execute_batch(sql).unwrap();
    }

    fn accounts(store: &Store) -> Vec<Vec<Value>> {
        query(store, "SELECT * FROM accounts ORDER BY id")
    }

    fn categories(store: &Store) -> Vec<Vec<Value>> {
        query(store, "SELECT * FROM categories ORDER BY id")
    }

    fn account(id: i64, name: &str, balance: i64) -> Vec<Value> {
        vec![Value::Integer(id), text(name), Value::Integer(balance)]
    }

    fn category(id: i64, name: &str, icon_name: &str) -> Vec<Value> {
        vec![Value::Integer(id), text(name), text(icon_name)]
    }

    fn counter(table: &str, value: i64) -> Vec<Value> {
        vec![text(table), Value::Integer(value)]
    }

    fn counters(store: &Store) -> Vec<Vec<Value>> {
        query(store, "SELECT name, seq FROM sqlite_sequence ORDER BY name")
    }

    /// Every row of every table, to show that a seeding changed nothing.
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

    /// The business rule a failed seeding broke.
    fn rule(result: Result<(), StoreError>) -> DomainError {
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

    #[test]
    fn a_new_database_gets_v1s_default_accounts_and_categories_with_their_ids_and_icons() {
        let store = new_store(
            "a_new_database_gets_v1s_default_accounts_and_categories_with_their_ids_and_icons",
        );
        store.seed_defaults(&english()).unwrap();
        assert_eq!(
            accounts(&store),
            [account(1, "Cash", 0), account(2, "Savings", 0)]
        );
        assert_eq!(
            categories(&store),
            [
                category(1, "General Expense", "ic_category_default"),
                category(2, "General Income", "income"),
                category(3, "Transfer", "transfer"),
            ]
        );
        assert_eq!(
            query(&store, "SELECT * FROM budgets"),
            Vec::<Vec<Value>>::new()
        );
        assert_eq!(
            query(&store, "SELECT * FROM transactions"),
            Vec::<Vec<Value>>::new()
        );
        assert_eq!(
            counters(&store),
            [counter("accounts", 2), counter("categories", 3)]
        );
        // The rows the user adds take the IDs after the defaults, as in fixture F3.
        assert_eq!(
            store.add_account("Checking", Cents(0)).unwrap(),
            AccountId(3)
        );
        assert_eq!(
            store
                .add_category("Groceries", defaults::DEFAULT_CATEGORY_ICON)
                .unwrap(),
            CategoryId(4)
        );
    }

    /// Fixture F3's accounts 1–2 and categories 1–3 are the rows v1 seeded with its English
    /// names; the accounts' balances have since changed.
    #[test]
    fn seeding_in_english_stores_the_rows_v1_seeded_in_fixture_f3() {
        let seeded = new_store("seeding_in_english_stores_the_rows_v1_seeded_in_fixture_f3");
        seeded.seed_defaults(&english()).unwrap();
        let v1 = open(&copy_ledger_fixture(
            "seeding_in_english_stores_the_rows_v1_seeded_in_fixture_f3-v1",
        ));
        let defaults_of = |store: &Store| {
            [
                query(
                    store,
                    "SELECT id, name FROM accounts WHERE id <= 2 ORDER BY id",
                ),
                query(store, "SELECT * FROM categories WHERE id <= 3 ORDER BY id"),
            ]
        };
        assert_eq!(defaults_of(&seeded), defaults_of(&v1));
        assert_eq!(defaults_of(&seeded)[0].len(), 2);
        assert_eq!(defaults_of(&seeded)[1].len(), 3);
    }

    /// v1 seeded after every sign-in, in the phone's language at that moment, and kept the names
    /// already stored.
    #[test]
    fn seeding_again_in_another_language_changes_nothing() {
        let store = new_store("seeding_again_in_another_language_changes_nothing");
        store.seed_defaults(&english()).unwrap();
        let before = snapshot(&store);
        store.seed_defaults(&french()).unwrap();
        store.seed_defaults(&english()).unwrap();
        assert_eq!(snapshot(&store), before);
    }

    /// Seeding a database whose tables hold rows writes nothing at all: closing it leaves every
    /// byte of fixture F3 as v1 wrote it.
    #[test]
    fn seeding_the_f3_fixture_changes_no_byte_of_it() {
        let path = copy_ledger_fixture("seeding_the_f3_fixture_changes_no_byte_of_it");
        let store = open(&path);
        store.seed_defaults(&french()).unwrap();
        store.close().unwrap();
        assert_eq!(fs::read(&path).unwrap(), fs::read(LEDGER_FIXTURE).unwrap());
        let mut leftovers: Vec<_> = fs::read_dir(path.parent().unwrap())
            .unwrap()
            .map(|entry| entry.unwrap().file_name())
            .collect();
        leftovers.sort();
        assert_eq!(leftovers, ["ledger.db"]);
    }

    #[test]
    fn a_table_with_any_row_keeps_it_and_gets_no_defaults() {
        let accounts_only = new_store("a_table_with_any_row_keeps_it_and_gets_no_defaults");
        execute(
            &accounts_only,
            "INSERT INTO accounts (id, name, balanceCents) VALUES (3, 'Checking', -500)",
        );
        accounts_only.seed_defaults(&english()).unwrap();
        assert_eq!(accounts(&accounts_only), [account(3, "Checking", -500)]);
        assert_eq!(categories(&accounts_only).len(), 3);

        let categories_only = new_store("a_table_with_any_row_keeps_it_and_gets_no_defaults-2");
        execute(
            &categories_only,
            "INSERT INTO categories (id, name, iconName) VALUES (2, 'Wages', 'bank')",
        );
        categories_only.seed_defaults(&english()).unwrap();
        assert_eq!(
            accounts(&categories_only),
            [account(1, "Cash", 0), account(2, "Savings", 0)]
        );
        assert_eq!(categories(&categories_only), [category(2, "Wages", "bank")]);
    }

    /// An emptied table still gets IDs 1–3, and its `AUTOINCREMENT` counter keeps the highest ID
    /// it ever handed out, as SQLite keeps it for v1's explicit IDs.
    #[test]
    fn an_emptied_table_gets_the_default_ids_and_keeps_its_counter() {
        let store = new_store("an_emptied_table_gets_the_default_ids_and_keeps_its_counter");
        execute(
            &store,
            "INSERT INTO categories (id, name, iconName) VALUES (7, 'Old', 'pets');
             DELETE FROM categories;",
        );
        store.seed_defaults(&english()).unwrap();
        assert_eq!(
            query(&store, "SELECT id FROM categories ORDER BY id"),
            [
                [Value::Integer(1)],
                [Value::Integer(2)],
                [Value::Integer(3)]
            ]
        );
        assert_eq!(
            counters(&store),
            [counter("accounts", 2), counter("categories", 7)]
        );
        assert_eq!(
            store
                .add_category("Groceries", defaults::DEFAULT_CATEGORY_ICON)
                .unwrap(),
            CategoryId(8)
        );
    }

    #[test]
    fn names_are_stored_trimmed_with_kotlins_whitespace_at_up_to_50_utf16_code_units() {
        let store = new_store(
            "names_are_stored_trimmed_with_kotlins_whitespace_at_up_to_50_utf16_code_units",
        );
        // 25 emoji are 50 UTF-16 code units.
        let longest = "\u{1f4b0}".repeat(25);
        let names = DefaultNames {
            cash_account: " \u{a0}Cash\t".to_owned(),
            savings_account: longest.clone(),
            expense_category: "\u{3000}D\u{e9}penses\u{2028}".to_owned(),
            income_category: "x".to_owned(),
            transfer_category: format!("\n{}\n", "t".repeat(MAX_NAME_UNITS)),
        };
        store.seed_defaults(&names).unwrap();
        assert_eq!(
            accounts(&store),
            [account(1, "Cash", 0), account(2, &longest, 0)]
        );
        assert_eq!(
            categories(&store),
            [
                category(1, "D\u{e9}penses", "ic_category_default"),
                category(2, "x", "income"),
                category(3, &"t".repeat(MAX_NAME_UNITS), "transfer"),
            ]
        );
    }

    /// Each field in turn gets a blank name, then one past the limit, and the seeding fails
    /// before it writes either table.
    #[test]
    fn an_invalid_name_in_any_field_is_refused_and_nothing_is_written() {
        let store = new_store("an_invalid_name_in_any_field_is_refused_and_nothing_is_written");
        let before = snapshot(&store);
        let fields: [fn(&mut DefaultNames) -> &mut String; 5] = [
            |names| &mut names.cash_account,
            |names| &mut names.savings_account,
            |names| &mut names.expense_category,
            |names| &mut names.income_category,
            |names| &mut names.transfer_category,
        ];
        for field in fields {
            for invalid in [
                String::new(),
                " \u{a0}\t".to_owned(),
                "\u{1f4b0}".repeat(25) + "x",
                "t".repeat(MAX_NAME_UNITS + 1),
            ] {
                let mut names = english();
                *field(&mut names) = invalid;
                assert_eq!(rule(store.seed_defaults(&names)), invalid_name());
                assert_eq!(snapshot(&store), before);
            }
        }
    }

    /// The names are checked even when both tables hold rows, so a bad name is found on any
    /// install, not only a new one.
    #[test]
    fn an_invalid_name_is_refused_even_when_no_table_is_empty() {
        let path = copy_ledger_fixture("an_invalid_name_is_refused_even_when_no_table_is_empty");
        let store = open(&path);
        let before = snapshot(&store);
        let mut names = english();
        names.transfer_category = "   ".to_owned();
        assert_eq!(rule(store.seed_defaults(&names)), invalid_name());
        assert_eq!(snapshot(&store), before);
    }

    /// Two defaults with one name, as v1's duplicate check compares names, would leave one of
    /// them out under v1's `INSERT OR IGNORE`. Seeding refuses them, and a refused category
    /// takes the accounts written before it back out.
    #[test]
    fn two_defaults_with_one_name_are_refused_and_nothing_is_written() {
        let store = new_store("two_defaults_with_one_name_are_refused_and_nothing_is_written");
        let before = snapshot(&store);
        let mut names = english();
        names.savings_account = "CASH".to_owned();
        assert_eq!(
            rule(store.seed_defaults(&names)),
            DomainError::DuplicateAccountName {
                name: "CASH".to_owned()
            }
        );
        assert_eq!(snapshot(&store), before);

        let mut names = english();
        names.transfer_category = "  general expense ".to_owned();
        assert_eq!(
            rule(store.seed_defaults(&names)),
            DomainError::DuplicateCategoryName {
                name: "general expense".to_owned()
            }
        );
        assert_eq!(snapshot(&store), before);
    }

    /// SQLite's `LOWER` folds only ASCII letters, so names that differ in the case of another
    /// letter are two names, as for every other category or account.
    #[test]
    fn defaults_that_differ_only_in_a_non_ascii_letters_case_are_two_names() {
        let store =
            new_store("defaults_that_differ_only_in_a_non_ascii_letters_case_are_two_names");
        let mut names = english();
        names.cash_account = "\u{e9}pargne".to_owned();
        names.savings_account = "\u{c9}pargne".to_owned();
        store.seed_defaults(&names).unwrap();
        assert_eq!(
            accounts(&store),
            [account(1, "\u{e9}pargne", 0), account(2, "\u{c9}pargne", 0)]
        );
    }

    /// An insert that fails takes back out every default written before it, the accounts
    /// included when a category fails: both tables are seeded in one database transaction.
    #[test]
    fn a_failed_insert_rolls_back_every_default_written_before_it() {
        let failed_to = |result: Result<(), StoreError>| match result {
            Err(StoreError::Sqlite { operation, .. }) => operation,
            other => panic!("expected a SQLite error, got {other:?}"),
        };
        let store = new_store("a_failed_insert_rolls_back_every_default_written_before_it");
        execute(
            &store,
            "CREATE TRIGGER refuse_savings BEFORE INSERT ON accounts WHEN NEW.id = 2 \
             BEGIN SELECT RAISE(ABORT, 'refused by the test'); END;",
        );
        let before = snapshot(&store);
        assert_eq!(
            failed_to(store.seed_defaults(&english())),
            "insert a default account"
        );
        assert_eq!(snapshot(&store), before);

        let store = new_store("a_failed_insert_rolls_back_every_default_written_before_it-2");
        execute(
            &store,
            "CREATE TRIGGER refuse_transfer BEFORE INSERT ON categories WHEN NEW.id = 3 \
             BEGIN SELECT RAISE(ABORT, 'refused by the test'); END;",
        );
        let before = snapshot(&store);
        assert_eq!(
            failed_to(store.seed_defaults(&english())),
            "insert a default category"
        );
        assert_eq!(snapshot(&store), before);
        assert_eq!(accounts(&store), Vec::<Vec<Value>>::new());
    }

    /// A file whose tables are missing fails with SQLite's error and what the store was doing.
    #[test]
    fn a_missing_table_fails_with_sqlites_error_and_writes_nothing() {
        let store = new_store("a_missing_table_fails_with_sqlites_error_and_writes_nothing");
        execute(
            &store,
            "DROP TABLE transactions; DROP TABLE budgets; DROP TABLE categories;",
        );
        match store.seed_defaults(&english()) {
            Err(StoreError::Sqlite { operation, .. }) => {
                assert_eq!(operation, "count the categories");
            }
            other => panic!("expected a SQLite error, got {other:?}"),
        }
        assert_eq!(accounts(&store), Vec::<Vec<Value>>::new());

        let store = new_store("a_missing_table_fails_with_sqlites_error_and_writes_nothing-2");
        execute(&store, "DROP TABLE transactions; DROP TABLE accounts;");
        match store.seed_defaults(&english()) {
            Err(StoreError::Sqlite { operation, .. }) => {
                assert_eq!(operation, "count the accounts");
            }
            other => panic!("expected a SQLite error, got {other:?}"),
        }
        assert_eq!(categories(&store), Vec::<Vec<Value>>::new());
    }

    /// In each of five new databases, eight threads, started together, seed in English or French,
    /// four through each of two `Store`s on the file. Each `Store` has its own lock, so only the
    /// `BEGIN IMMEDIATE` transaction around each check of an empty table and its inserts keeps
    /// two calls apart. Every call succeeds, and both tables get the defaults once, in the
    /// language of one call.
    #[test]
    fn seeding_from_several_threads_and_connections_at_once_stores_the_defaults_once() {
        const ROUNDS: usize = 5;
        const THREADS: usize = 8;
        let seeded_once = [
            [
                text("Cash|Savings"),
                text("General Expense|General Income|Transfer"),
            ],
            [
                text("Esp\u{e8}ces|\u{c9}pargne"),
                text("D\u{e9}penses g\u{e9}n\u{e9}rales|Revenus g\u{e9}n\u{e9}raux|Virement"),
            ],
        ];
        for round in 0..ROUNDS {
            let path = scratch_dir(&format!(
                "seeding_from_several_threads_and_connections_at_once_stores_the_defaults_once-{round}"
            ))
            .join("ledger.db");
            let stores = [open(&path), open(&path)];
            let start = std::sync::Barrier::new(THREADS);
            let results: Vec<Result<(), StoreError>> = std::thread::scope(|scope| {
                let threads: Vec<_> = (0..THREADS)
                    .map(|thread| {
                        let store = &stores[thread % 2];
                        let start = &start;
                        scope.spawn(move || {
                            let names = if thread < THREADS / 2 {
                                english()
                            } else {
                                french()
                            };
                            start.wait();
                            store.seed_defaults(&names)
                        })
                    })
                    .collect();
                threads
                    .into_iter()
                    .map(|thread| thread.join().unwrap())
                    .collect()
            });
            for result in results {
                assert!(result.is_ok(), "round {round}: {result:?}");
            }
            let names = query(
                &stores[0],
                "SELECT (SELECT group_concat(name, '|') FROM (SELECT name FROM accounts ORDER BY id)), \
                 (SELECT group_concat(name, '|') FROM (SELECT name FROM categories ORDER BY id))",
            );
            assert!(
                seeded_once
                    .iter()
                    .any(|expected| names == [expected.to_vec()]),
                "round {round}: the defaults weren't seeded once in one language: {names:?}"
            );
            assert_eq!(
                counters(&stores[0]),
                [counter("accounts", 2), counter("categories", 3)]
            );
        }
    }
}
