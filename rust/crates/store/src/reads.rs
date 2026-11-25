/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Reading the ledger: a month's transactions, a search, and every category and account.
//!
//! This ports the queries of v1's `TransactionDao.observeByDateRange`, `TransactionDao.search`,
//! `CategoryDao.getAll`, and `AccountDao.getAll`, and the range check and `escapeLike` of v1's
//! `SearchTransactionUseCase` and `TransactionRepositoryImpl` (all at `6d57526`). v1's
//! `GenerateReportUseCase` and its search screen read them. Each read is one statement under the
//! store's lock, so it sees the database as one write left it, never halfway through another.

use denarii_core::{
    Account, AccountId, Category, CategoryId, Cents, DomainError, EpochMillis, MonthBounds,
    SearchFilters, StoredTransaction, TransactionId, is_valid_search_range,
};
use rusqlite::{Connection, Params, Row, params};

use crate::error::{StoreError, sqlite};
use crate::store::Store;
use crate::transactions::StoredType;

/// The columns a [`StoredTransaction`] is read from, in the order [`stored_transaction`] reads
/// them.
const TRANSACTION_COLUMNS: &str = "id, type, description, amountCents, categoryId, accountId, \
                                   transferAccountId, dateEpochMillis";

impl Store {
    /// The transactions dated in `month`, oldest first, and by ID among those dated at the same
    /// moment.
    ///
    /// `month` is a calendar month in the user's time zone, which the caller works out
    /// (ADR-0019). This is the list v1's `GenerateReportUseCase` made: it read the month from
    /// its first moment to one millisecond before the next month's, then sorted it by date and
    /// ID. A malformed transfer, one with no destination, is listed like any other row (BUG-06).
    ///
    /// # Errors
    ///
    /// [`StoreError::Poisoned`] if the connection can't be used, or [`StoreError::Sqlite`] if
    /// SQLite fails, which includes a row whose `type` isn't one of v1's three names.
    pub fn transactions_in_month(
        &self,
        month: MonthBounds,
    ) -> Result<Vec<StoredTransaction>, StoreError> {
        let connection = self.lock()?;
        read_transactions(
            &connection,
            &format!(
                "SELECT {TRANSACTION_COLUMNS} FROM transactions \
                 WHERE dateEpochMillis >= ?1 AND dateEpochMillis < ?2 \
                 ORDER BY dateEpochMillis, id"
            ),
            params![month.start().0, month.next_start().0],
            "read the month's transactions",
        )
    }

    /// The transactions that match every filter that `filters` sets, newest first, and by ID
    /// from the highest among those dated at the same moment.
    ///
    /// A filter left `None` matches every row. The description matches when it contains the
    /// filter's text, in which `%`, `_`, and `\` match only themselves; SQLite's `LIKE` ignores
    /// the case of ASCII letters only, as it did in v1. The amounts and the dates are inclusive
    /// bounds. The caller passes the moments that its dates stand for in the user's time zone
    /// (ADR-0019): v1 searched from the first moment of the start date to one millisecond before
    /// the first moment of the day after the end date.
    ///
    /// # Errors
    ///
    /// - [`StoreError::Domain`] with [`DomainError::InvalidSearchRange`] if the minimum amount is
    ///   above the maximum or the start date after the end date. Nothing is read.
    /// - [`StoreError::Poisoned`] if the connection can't be used, or [`StoreError::Sqlite`] if
    ///   SQLite fails, which includes a row whose `type` isn't one of v1's three names.
    pub fn search_transactions(
        &self,
        filters: &SearchFilters,
    ) -> Result<Vec<StoredTransaction>, StoreError> {
        if !is_valid_search_range(filters) {
            return Err(StoreError::Domain(DomainError::InvalidSearchRange));
        }
        let connection = self.lock()?;
        read_transactions(
            &connection,
            &format!(
                "SELECT {TRANSACTION_COLUMNS} FROM transactions \
                 WHERE (?1 IS NULL OR description LIKE '%' || ?1 || '%' ESCAPE '\\') \
                 AND (?2 IS NULL OR categoryId = ?2) \
                 AND (?3 IS NULL OR amountCents >= ?3) \
                 AND (?4 IS NULL OR amountCents <= ?4) \
                 AND (?5 IS NULL OR dateEpochMillis >= ?5) \
                 AND (?6 IS NULL OR dateEpochMillis <= ?6) \
                 ORDER BY dateEpochMillis DESC, id DESC"
            ),
            params![
                filters.description.as_deref().map(escape_like),
                filters.category_id.map(|id| id.0),
                filters.min_amount.map(|amount| amount.0),
                filters.max_amount.map(|amount| amount.0),
                filters.start_date.map(|date| date.0),
                filters.end_date.map(|date| date.0),
            ],
            "search the transactions",
        )
    }

    /// Every category, ordered by name as SQLite compares text by default: by its UTF-8 bytes,
    /// so `Z` comes before `a`, and `a` before `É`. This is v1's `CategoryDao.getAll`.
    ///
    /// # Errors
    ///
    /// [`StoreError::Poisoned`] if the connection can't be used, or [`StoreError::Sqlite`] if
    /// SQLite fails.
    pub fn categories(&self) -> Result<Vec<Category>, StoreError> {
        let connection = self.lock()?;
        read_rows(
            &connection,
            "SELECT id, name, iconName FROM categories ORDER BY name ASC",
            [],
            |row| {
                Ok(Category {
                    id: Some(CategoryId(row.get(0)?)),
                    name: row.get(1)?,
                    icon_name: row.get(2)?,
                })
            },
            "read the categories",
        )
    }

    /// Every account with its stored balance, ordered by name as
    /// [`categories`](Self::categories) orders them. This is v1's `AccountDao.getAll`.
    ///
    /// # Errors
    ///
    /// [`StoreError::Poisoned`] if the connection can't be used, or [`StoreError::Sqlite`] if
    /// SQLite fails.
    pub fn accounts(&self) -> Result<Vec<Account>, StoreError> {
        let connection = self.lock()?;
        read_rows(
            &connection,
            "SELECT id, name, balanceCents FROM accounts ORDER BY name ASC",
            [],
            |row| {
                Ok(Account {
                    id: Some(AccountId(row.get(0)?)),
                    name: row.get(1)?,
                    balance: Cents(row.get(2)?),
                })
            },
            "read the accounts",
        )
    }
}

/// `text` with `\`, `%`, and `_` escaped for `LIKE … ESCAPE '\'`, so each matches only itself.
/// This is v1's `escapeLike`.
fn escape_like(text: &str) -> String {
    text.replace('\\', "\\\\")
        .replace('%', "\\%")
        .replace('_', "\\_")
}

fn read_transactions(
    connection: &Connection,
    sql: &str,
    params: impl Params,
    operation: &'static str,
) -> Result<Vec<StoredTransaction>, StoreError> {
    read_rows(connection, sql, params, stored_transaction, operation)
}

fn read_rows<T>(
    connection: &Connection,
    sql: &str,
    params: impl Params,
    map: impl FnMut(&Row<'_>) -> rusqlite::Result<T>,
    operation: &'static str,
) -> Result<Vec<T>, StoreError> {
    let mut statement = connection
        .prepare(sql)
        .map_err(|source| sqlite(operation, source))?;
    statement
        .query_map(params, map)
        .and_then(Iterator::collect)
        .map_err(|source| sqlite(operation, source))
}

/// A row selected with [`TRANSACTION_COLUMNS`].
fn stored_transaction(row: &Row<'_>) -> rusqlite::Result<StoredTransaction> {
    let StoredType(transaction_type) = row.get(1)?;
    let transfer_account: Option<i64> = row.get(6)?;
    Ok(StoredTransaction {
        id: TransactionId(row.get(0)?),
        transaction_type,
        description: row.get(2)?,
        amount: Cents(row.get(3)?),
        category_id: CategoryId(row.get(4)?),
        account_id: AccountId(row.get(5)?),
        transfer_account_id: transfer_account.map(AccountId),
        date: EpochMillis(row.get(7)?),
    })
}

#[cfg(test)]
mod tests {
    use std::fs;
    use std::path::PathBuf;

    use denarii_core::{
        Account, AccountId, Category, CategoryId, Cents, DomainError, EpochMillis, MonthBounds,
        SearchFilters, StoredTransaction, TransactionId, TransactionType,
    };

    use super::escape_like;
    use crate::{DatabaseKey, Store, StoreError};

    // Moments in UTC, in milliseconds since 1970.
    const OCTOBER_START: i64 = 1_790_812_800_000;
    const NOVEMBER_START: i64 = 1_793_491_200_000;
    const DECEMBER_START: i64 = 1_796_083_200_000;
    const DAY: i64 = 86_400_000;

    /// Fixture F3, which v1's own seeding and use cases wrote (`tests/fixtures/README.md`).
    const LEDGER_FIXTURE: &str = concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/tests/fixtures/v1-f3-ledger.db"
    );

    fn scratch_dir(test: &str) -> PathBuf {
        let dir =
            std::env::temp_dir().join(format!("denarii-store-reads-{}-{test}", std::process::id()));
        let _ = fs::remove_dir_all(&dir); // Left over from an earlier run, or absent.
        fs::create_dir_all(&dir).unwrap();
        dir
    }

    fn fixture_key() -> DatabaseKey {
        DatabaseKey::from_bytes(std::array::from_fn(|i| u8::try_from(i).unwrap()))
    }

    fn month(start: i64, next_start: i64) -> MonthBounds {
        MonthBounds::new(EpochMillis(start), EpochMillis(next_start)).unwrap()
    }

    fn execute(store: &Store, sql: &str) {
        store.lock().unwrap().execute_batch(sql).unwrap();
    }

    /// A new database with two categories and two accounts, as v1's `TransactionDaoTest` set up.
    fn test_store(test: &str) -> Store {
        let store = Store::open(scratch_dir(test).join("ledger.db"), &fixture_key()).unwrap();
        execute(
            &store,
            "INSERT INTO categories (id, name, iconName) \
             VALUES (1, 'Dining', 'dining'), (2, 'Rent', 'ic_category_default');
             INSERT INTO accounts (id, name, balanceCents) VALUES (1, 'Cash', 0), (2, 'Savings', 0);",
        );
        store
    }

    /// v1's `TransactionDaoTest` rows: two expenses, an income, and a transfer, at moments 1,000
    /// to 4,000.
    fn dao_test_store(test: &str) -> Store {
        let store = test_store(test);
        insert(&store, 1, "EXPENSE", "Coffee beans", 1_250, 1, None, 1_000);
        insert(
            &store,
            2,
            "EXPENSE",
            "Rent September",
            150_000,
            2,
            None,
            2_000,
        );
        insert(&store, 3, "INCOME", "Coffee refund", 500, 1, None, 3_000);
        insert(
            &store,
            4,
            "TRANSFER",
            "To savings",
            10_000,
            1,
            Some(2),
            4_000,
        );
        store
    }

    fn ledger_store(test: &str) -> Store {
        let path = scratch_dir(test).join("ledger.db");
        fs::copy(LEDGER_FIXTURE, &path).unwrap();
        Store::open(&path, &fixture_key()).unwrap()
    }

    #[expect(
        clippy::too_many_arguments,
        reason = "one argument for each column of the row"
    )]
    fn insert(
        store: &Store,
        id: i64,
        stored_type: &str,
        description: &str,
        amount: i64,
        category: i64,
        destination: Option<i64>,
        date: i64,
    ) {
        store
            .lock()
            .unwrap()
            .execute(
                "INSERT INTO transactions (id, type, description, amountCents, categoryId, \
                 accountId, transferAccountId, dateEpochMillis, createdAtEpochMillis) \
                 VALUES (?1, ?2, ?3, ?4, ?5, 1, ?6, ?7, 0)",
                rusqlite::params![
                    id,
                    stored_type,
                    description,
                    amount,
                    category,
                    destination,
                    date
                ],
            )
            .unwrap();
    }

    fn ids(transactions: &[StoredTransaction]) -> Vec<i64> {
        transactions
            .iter()
            .map(|transaction| transaction.id.0)
            .collect()
    }

    fn search(store: &Store, filters: &SearchFilters) -> Vec<i64> {
        ids(&store.search_transactions(filters).unwrap())
    }

    fn describing(text: &str) -> SearchFilters {
        SearchFilters {
            description: Some(text.to_owned()),
            ..SearchFilters::default()
        }
    }

    #[test]
    fn a_months_transactions_come_oldest_first_and_by_id_at_one_moment() {
        let store = test_store("month_order");
        let noon = NOVEMBER_START + DAY / 2;
        insert(&store, 5, "EXPENSE", "Later", 100, 1, None, noon + DAY);
        insert(
            &store,
            7,
            "EXPENSE",
            "Same moment, higher ID",
            100,
            1,
            None,
            noon,
        );
        insert(
            &store,
            6,
            "INCOME",
            "Same moment, lower ID",
            100,
            2,
            None,
            noon,
        );
        insert(
            &store,
            8,
            "EXPENSE",
            "Earliest",
            100,
            1,
            None,
            NOVEMBER_START + 1,
        );

        let november = store
            .transactions_in_month(month(NOVEMBER_START, DECEMBER_START))
            .unwrap();

        assert_eq!(ids(&november), [8, 6, 7, 5]);
    }

    #[test]
    fn a_month_holds_its_first_moment_and_not_the_next_months() {
        let store = test_store("month_bounds");
        insert(
            &store,
            1,
            "EXPENSE",
            "October's last",
            100,
            1,
            None,
            NOVEMBER_START - 1,
        );
        insert(
            &store,
            2,
            "EXPENSE",
            "November's first",
            100,
            1,
            None,
            NOVEMBER_START,
        );
        insert(
            &store,
            3,
            "EXPENSE",
            "November's last",
            100,
            1,
            None,
            DECEMBER_START - 1,
        );
        insert(
            &store,
            4,
            "EXPENSE",
            "December's first",
            100,
            1,
            None,
            DECEMBER_START,
        );

        let november = month(NOVEMBER_START, DECEMBER_START);
        assert_eq!(ids(&store.transactions_in_month(november).unwrap()), [2, 3]);
        let october = month(OCTOBER_START, NOVEMBER_START);
        assert_eq!(ids(&store.transactions_in_month(october).unwrap()), [1]);
    }

    #[test]
    fn a_month_with_no_transactions_reads_empty() {
        let store = dao_test_store("month_empty");
        let november = month(NOVEMBER_START, DECEMBER_START);
        assert_eq!(store.transactions_in_month(november).unwrap(), []);
    }

    #[test]
    fn rows_read_back_every_stored_column_a_malformed_transfer_included() {
        let store = test_store("columns");
        let date = NOVEMBER_START + 5;
        insert(
            &store,
            1,
            "TRANSFER",
            "Virement sans destination",
            4_200,
            2,
            None,
            date,
        );
        insert(
            &store,
            2,
            "EXPENSE",
            "Odd row 🍜",
            500,
            1,
            Some(2),
            date + 1,
        );

        let november = store
            .transactions_in_month(month(NOVEMBER_START, DECEMBER_START))
            .unwrap();

        let row =
            |id, transaction_type, description: &str, amount, category, to: Option<i64>, date| {
                StoredTransaction {
                    id: TransactionId(id),
                    transaction_type,
                    description: description.to_owned(),
                    amount: Cents(amount),
                    category_id: CategoryId(category),
                    account_id: AccountId(1),
                    transfer_account_id: to.map(AccountId),
                    date: EpochMillis(date),
                }
            };
        assert_eq!(
            november,
            [
                row(
                    1,
                    TransactionType::Transfer,
                    "Virement sans destination",
                    4_200,
                    2,
                    None,
                    date
                ),
                row(
                    2,
                    TransactionType::Expense,
                    "Odd row 🍜",
                    500,
                    1,
                    Some(2),
                    date + 1
                ),
            ]
        );
    }

    #[test]
    fn a_stored_type_other_than_v1s_three_names_fails_the_read() {
        let store = test_store("unknown_type");
        insert(
            &store,
            1,
            "expense",
            "Lower case",
            100,
            1,
            None,
            NOVEMBER_START,
        );

        let month_read = store.transactions_in_month(month(NOVEMBER_START, DECEMBER_START));
        assert!(
            matches!(
                month_read,
                Err(StoreError::Sqlite {
                    operation: "read the month's transactions",
                    ..
                })
            ),
            "{month_read:?}"
        );
        let search_read = store.search_transactions(&SearchFilters::default());
        assert!(
            matches!(
                search_read,
                Err(StoreError::Sqlite {
                    operation: "search the transactions",
                    ..
                })
            ),
            "{search_read:?}"
        );
    }

    #[test]
    fn the_f3_fixtures_months_hold_the_rows_v1_dated_in_them() {
        let store = ledger_store("f3_months");

        let november = store
            .transactions_in_month(month(NOVEMBER_START, DECEMBER_START))
            .unwrap();
        assert_eq!(ids(&november), [1, 6, 3, 4, 5, 7, 8, 9, 10, 12]);
        let malformed = november.last().unwrap();
        assert_eq!(malformed.transaction_type, TransactionType::Transfer);
        assert_eq!(malformed.transfer_account_id, None);
        assert_eq!(malformed.description, "Virement sans destination");

        let october = store
            .transactions_in_month(month(OCTOBER_START, NOVEMBER_START))
            .unwrap();
        assert_eq!(ids(&october), [2]);
        assert_eq!(october[0].description, "Épicerie d'octobre");
    }

    #[test]
    fn search_filters_combine_and_sort_newest_first() {
        // Every case of v1's TransactionDaoTest.searchFiltersCombineAndSortNewestFirst.
        let store = dao_test_store("search_combine");

        assert_eq!(search(&store, &SearchFilters::default()), [4, 3, 2, 1]);
        assert_eq!(search(&store, &describing("coffee")), [3, 1]);
        let dining = SearchFilters {
            category_id: Some(CategoryId(1)),
            ..SearchFilters::default()
        };
        assert_eq!(search(&store, &dining), [4, 3, 1]);
        let amounts = SearchFilters {
            min_amount: Some(Cents(1_000)),
            max_amount: Some(Cents(10_000)),
            ..SearchFilters::default()
        };
        assert_eq!(search(&store, &amounts), [4, 1]);
        let dates = SearchFilters {
            start_date: Some(EpochMillis(2_000)),
            end_date: Some(EpochMillis(3_000)),
            ..SearchFilters::default()
        };
        assert_eq!(search(&store, &dates), [3, 2]);
        let all_at_once = SearchFilters {
            description: Some("COFFEE".to_owned()),
            category_id: Some(CategoryId(1)),
            min_amount: Some(Cents(500)),
            max_amount: Some(Cents(1_250)),
            start_date: Some(EpochMillis(1_000)),
            end_date: Some(EpochMillis(3_000)),
        };
        assert_eq!(search(&store, &all_at_once), [3, 1]);
    }

    #[test]
    fn search_without_matches_is_empty() {
        // v1's TransactionDaoTest.searchWithoutMatchesIsEmpty.
        let store = dao_test_store("search_empty");

        assert_eq!(search(&store, &describing("zzz")), Vec::<i64>::new());
        let too_much = SearchFilters {
            min_amount: Some(Cents(200_000)),
            ..SearchFilters::default()
        };
        assert_eq!(search(&store, &too_much), Vec::<i64>::new());
    }

    #[test]
    fn search_matches_wildcards_and_the_escape_character_literally() {
        // v1's TransactionDaoTest.searchMatchesWildcardsLiterally, plus the backslash.
        let store = dao_test_store("search_wildcards");
        insert(&store, 5, "EXPENSE", "Tip 50% off", 100, 1, None, 5_000);
        insert(&store, 6, "EXPENSE", "snake_case", 100, 1, None, 6_000);
        insert(&store, 7, "EXPENSE", r"C:\temp", 100, 1, None, 7_000);

        assert_eq!(search(&store, &describing("50%")), [5]);
        assert_eq!(search(&store, &describing("_")), [6]);
        assert_eq!(search(&store, &describing("\\")), [7]);
        assert_eq!(search(&store, &describing(r"C:\t")), [7]);
        assert_eq!(search(&store, &describing("%")), [5]);
    }

    #[test]
    fn like_escaping_is_v1s() {
        assert_eq!(escape_like(r"50%_\x"), r"50\%\_\\x");
        assert_eq!(escape_like("plain"), "plain");
        assert_eq!(escape_like(""), "");
    }

    #[test]
    fn search_ignores_the_case_of_ascii_letters_only() {
        let store = test_store("search_case");
        insert(&store, 1, "EXPENSE", "Café ☕", 450, 1, None, 1_000);
        insert(&store, 2, "EXPENSE", "ÉPICERIE", 450, 1, None, 2_000);

        assert_eq!(search(&store, &describing("CAFé")), [1]);
        assert_eq!(search(&store, &describing("café ☕")), [1]);
        // SQLite's LIKE folds only ASCII letters, so É and é differ, as they did in v1.
        assert_eq!(search(&store, &describing("CAFÉ")), Vec::<i64>::new());
        assert_eq!(search(&store, &describing("épicerie")), Vec::<i64>::new());
        assert_eq!(search(&store, &describing("ÉPIcerie")), [2]);
    }

    #[test]
    fn search_bounds_are_inclusive() {
        let store = dao_test_store("search_inclusive");

        let exact_amount = SearchFilters {
            min_amount: Some(Cents(500)),
            max_amount: Some(Cents(500)),
            ..SearchFilters::default()
        };
        assert_eq!(search(&store, &exact_amount), [3]);
        let exact_date = SearchFilters {
            start_date: Some(EpochMillis(2_000)),
            end_date: Some(EpochMillis(2_000)),
            ..SearchFilters::default()
        };
        assert_eq!(search(&store, &exact_date), [2]);
        let only_start = SearchFilters {
            start_date: Some(EpochMillis(3_000)),
            ..SearchFilters::default()
        };
        assert_eq!(search(&store, &only_start), [4, 3]);
        let only_end = SearchFilters {
            end_date: Some(EpochMillis(1_999)),
            ..SearchFilters::default()
        };
        assert_eq!(search(&store, &only_end), [1]);
        let only_max = SearchFilters {
            max_amount: Some(Cents(1_249)),
            ..SearchFilters::default()
        };
        assert_eq!(search(&store, &only_max), [3]);
    }

    #[test]
    fn search_lists_rows_at_one_moment_by_id_from_the_highest() {
        let store = test_store("search_ties");
        insert(&store, 2, "EXPENSE", "Tie", 100, 1, None, 5_000);
        insert(&store, 9, "EXPENSE", "Tie", 100, 2, None, 5_000);
        insert(&store, 4, "EXPENSE", "Tie", 100, 1, None, 5_000);
        insert(&store, 1, "EXPENSE", "Tie", 100, 1, None, 6_000);

        assert_eq!(search(&store, &describing("Tie")), [1, 9, 4, 2]);
        let dining = SearchFilters {
            category_id: Some(CategoryId(1)),
            ..describing("Tie")
        };
        assert_eq!(search(&store, &dining), [1, 4, 2]);
    }

    #[test]
    fn an_inverted_search_range_is_refused() {
        let store = dao_test_store("search_inverted");

        let amounts = SearchFilters {
            min_amount: Some(Cents(10_001)),
            max_amount: Some(Cents(10_000)),
            ..SearchFilters::default()
        };
        let dates = SearchFilters {
            start_date: Some(EpochMillis(3_001)),
            end_date: Some(EpochMillis(3_000)),
            ..SearchFilters::default()
        };
        for filters in [amounts, dates] {
            let result = store.search_transactions(&filters);
            assert!(
                matches!(
                    result,
                    Err(StoreError::Domain(DomainError::InvalidSearchRange))
                ),
                "{result:?}"
            );
        }
    }

    #[test]
    fn the_f3_fixtures_search_finds_its_non_ascii_descriptions() {
        let store = ledger_store("f3_search");

        assert_eq!(search(&store, &describing("☕")), [3]);
        assert_eq!(search(&store, &describing("هدية")), [9]);
        let groceries = SearchFilters {
            category_id: Some(CategoryId(4)),
            ..SearchFilters::default()
        };
        assert_eq!(search(&store, &groceries), [5, 4, 2]);
        let transfers_of_november = SearchFilters {
            category_id: Some(CategoryId(3)),
            start_date: Some(EpochMillis(NOVEMBER_START)),
            end_date: Some(EpochMillis(DECEMBER_START - 1)),
            ..SearchFilters::default()
        };
        assert_eq!(search(&store, &transfers_of_november), [12, 10, 7, 6]);
    }

    #[test]
    fn categories_and_accounts_come_ordered_by_their_names_bytes() {
        let store = test_store("names_order");
        execute(
            &store,
            "INSERT INTO categories (id, name, iconName) \
             VALUES (3, 'Épicerie', 'groceries'), (4, 'cafés', 'dining'), (5, 'Zoo', 'fun');
             INSERT INTO accounts (id, name, balanceCents) VALUES (3, 'card', -4200), (4, 'Écu', 7);",
        );

        let categories = store.categories().unwrap();
        let names: Vec<&str> = categories.iter().map(|c| c.name.as_str()).collect();
        assert_eq!(names, ["Dining", "Rent", "Zoo", "cafés", "Épicerie"]);
        assert_eq!(
            categories[4],
            Category {
                id: Some(CategoryId(3)),
                name: "Épicerie".to_owned(),
                icon_name: "groceries".to_owned(),
            }
        );

        assert_eq!(
            store.accounts().unwrap(),
            [
                Account {
                    id: Some(AccountId(1)),
                    name: "Cash".to_owned(),
                    balance: Cents(0),
                },
                Account {
                    id: Some(AccountId(2)),
                    name: "Savings".to_owned(),
                    balance: Cents(0),
                },
                Account {
                    id: Some(AccountId(3)),
                    name: "card".to_owned(),
                    balance: Cents(-4_200),
                },
                Account {
                    id: Some(AccountId(4)),
                    name: "Écu".to_owned(),
                    balance: Cents(7),
                },
            ]
        );
    }

    #[test]
    fn an_empty_database_has_no_categories_or_accounts() {
        let store = Store::open(scratch_dir("names_empty").join("new.db"), &fixture_key()).unwrap();
        assert_eq!(store.categories().unwrap(), []);
        assert_eq!(store.accounts().unwrap(), []);
    }

    #[test]
    fn the_f3_fixtures_categories_and_accounts_read_as_v1_stored_them() {
        let store = ledger_store("f3_names");

        let categories: Vec<(i64, String, String)> = store
            .categories()
            .unwrap()
            .into_iter()
            .map(|c| (c.id.unwrap().0, c.name, c.icon_name))
            .collect();
        let category = |id, name: &str, icon: &str| (id, name.to_owned(), icon.to_owned());
        assert_eq!(
            categories,
            [
                category(1, "General Expense", "ic_category_default"),
                category(2, "General Income", "income"),
                category(5, "Restaurants", "dining"),
                category(6, "Salaire", "income"),
                category(3, "Transfer", "transfer"),
                category(4, "Épicerie 🛒", "groceries"),
            ]
        );

        let accounts: Vec<(i64, String, i64)> = store
            .accounts()
            .unwrap()
            .into_iter()
            .map(|a| (a.id.unwrap().0, a.name, a.balance.0))
            .collect();
        let account = |id, name: &str, balance| (id, name.to_owned(), balance);
        assert_eq!(
            accounts,
            [
                account(3, "Carte bancaire 💳", 269_655),
                account(4, "Carte de crédit", -44_000),
                account(1, "Cash", -2_300),
                account(2, "Savings", 50_000),
            ]
        );
    }
}
