/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Adding, editing, and deleting transactions, each with its balance changes in the same
//! database transaction as the row.
//!
//! This ports v1's `AddTransactionUseCase`, `UpdateTransactionUseCase`, and
//! `DeleteTransactionUseCase`, the checks of `ValidateTransactionUseCase` that need stored rows,
//! and the writes of `TransactionRepositoryImpl` (all at `6d57526`). Each operation checks the
//! request in v1's order, then writes the row and changes the stored balances by
//! [`balance_deltas`](denarii_core::balance_deltas) inside one `BEGIN IMMEDIATE` transaction, so
//! a failure at any step leaves the file as it was. v1 ran its checks before it opened the
//! database transaction; here they run inside it, under the store's lock, so no other call can
//! change what they read before the row is written.

use std::collections::BTreeMap;

use denarii_core::{
    AccountId, CategoryId, Cents, DomainError, EpochMillis, MonthBounds, Transaction,
    TransactionDraft, TransactionId, TransactionKind, TransactionType, UnknownTransactionType,
    balance_deltas, budget_overage, validate_transaction,
};
use rusqlite::types::{FromSql, FromSqlError, FromSqlResult, ValueRef};
use rusqlite::{Connection, OptionalExtension, TransactionBehavior, params};

use crate::error::{StoreError, sqlite};
use crate::store::Store;

/// Whether an expense may take its category past the monthly budget.
///
/// v1's form saved with [`Refuse`](Self::Refuse) first. When that failed with
/// [`DomainError::BudgetExceeded`], it asked the user, and saved again with
/// [`Allow`](Self::Allow) once they confirmed: a budget is a target, not a hard limit.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum OverBudget {
    /// Fail with [`DomainError::BudgetExceeded`] if the expense takes its category past the
    /// month's limit.
    Refuse,
    /// Write the expense whatever the budget; the user has confirmed it. Every other check still
    /// runs.
    Allow,
}

impl Store {
    /// Adds a transaction and changes the stored balances by its impact, returning the new row's
    /// ID.
    ///
    /// `month` holds the transaction's date: it is the date's calendar month in the user's time
    /// zone, which the caller works out (ADR-0019). `created_at` is the time now, which the row
    /// keeps through every edit; Rust reads no clock. `draft.id` is ignored, as v1 ignored it:
    /// SQLite picks the ID.
    ///
    /// # Errors
    ///
    /// The first of these that applies, in v1's order. Nothing is written when any of them is
    /// returned.
    ///
    /// 1. [`StoreError::Domain`] with the first field rule the draft breaks, in the order
    ///    [`validate_transaction`] lists.
    /// 2. [`StoreError::DateOutsideMonth`] if `month` doesn't hold the date.
    /// 3. [`StoreError::Domain`] with [`DomainError::CategoryNotFound`],
    ///    [`DomainError::AccountNotFound`], or [`DomainError::TransferDestinationNotFound`] if
    ///    the category, the account, or a transfer's destination has no row, checked in that
    ///    order.
    /// 4. [`StoreError::Domain`] with [`DomainError::BudgetExceeded`] if the transaction is an
    ///    expense, `over_budget` is [`OverBudget::Refuse`], and the category's expenses in
    ///    `month`, with this one, would go past its monthly budget.
    /// 5. [`StoreError::Overflow`] if a balance or the month's expenses would go past the range
    ///    of [`Cents`], and [`StoreError::MissingAccount`] if a balance change names an account
    ///    with no row; only a damaged file causes either.
    /// 6. [`StoreError::Poisoned`] or [`StoreError::Sqlite`] if the connection can't be used, or
    ///    SQLite fails.
    pub fn add_transaction(
        &self,
        draft: &TransactionDraft,
        month: MonthBounds,
        created_at: EpochMillis,
        over_budget: OverBudget,
    ) -> Result<TransactionId, StoreError> {
        let transaction = Transaction {
            id: None,
            ..validated(draft, month)?
        };
        let mut connection = self.lock()?;
        let write = begin(&mut connection)?;
        check_stored_rules(&write, &transaction, month, over_budget)?;
        let id = insert(&write, &transaction, created_at)?;
        apply_balance_deltas(&write, None, Some(&transaction))?;
        commit(write)?;
        Ok(id)
    }

    /// Replaces the stored transaction `draft.id` with `draft`, and changes the stored balances by
    /// the difference between the two versions' impacts.
    ///
    /// `month` holds the new date, as for [`add_transaction`](Self::add_transaction). The row
    /// keeps its `createdAtEpochMillis`. An edited expense's budget check leaves out the stored
    /// version's amount. A stored transfer with no destination has no impact to take back
    /// (BUG-06), so editing one applies only the new version's impact.
    ///
    /// # Errors
    ///
    /// The first of these that applies, in v1's order. Nothing is written when any of them is
    /// returned.
    ///
    /// 1. [`StoreError::Domain`] with [`DomainError::IdRequired`] if `draft.id` is `None` or
    ///    isn't more than zero.
    /// 2. Steps 1–4 of [`add_transaction`](Self::add_transaction)'s errors.
    /// 3. [`StoreError::TransactionNotFound`] if no row has the ID.
    /// 4. Steps 5 and 6 of [`add_transaction`](Self::add_transaction)'s errors, and
    ///    [`StoreError::Sqlite`] if the stored row's type isn't one of v1's three.
    pub fn update_transaction(
        &self,
        draft: &TransactionDraft,
        month: MonthBounds,
        over_budget: OverBudget,
    ) -> Result<(), StoreError> {
        let id = stored_id(draft.id)?;
        let transaction = validated(draft, month)?;
        let mut connection = self.lock()?;
        let write = begin(&mut connection)?;
        check_stored_rules(&write, &transaction, month, over_budget)?;
        let previous = read_stored(&write, id)?.ok_or(StoreError::TransactionNotFound { id })?;
        write
            .execute(
                "UPDATE transactions SET type = ?1, description = ?2, amountCents = ?3, \
                 categoryId = ?4, accountId = ?5, transferAccountId = ?6, dateEpochMillis = ?7 \
                 WHERE id = ?8",
                params![
                    transaction.transaction_type().stored_name(),
                    transaction.description,
                    transaction.amount.0,
                    transaction.category_id.0,
                    transaction.account_id.0,
                    transaction.kind.transfer_account().map(|account| account.0),
                    transaction.date.0,
                    id.0,
                ],
            )
            .map_err(|source| sqlite("update the transaction", source))?;
        apply_balance_deltas(&write, previous.transaction(), Some(&transaction))?;
        commit(write)
    }

    /// Deletes the transaction `id`, and takes its impact back off the stored balances.
    ///
    /// A stored transfer with no destination has no impact, so deleting one changes no balance
    /// (BUG-06).
    ///
    /// # Errors
    ///
    /// Nothing is written when any of these is returned.
    ///
    /// - [`StoreError::Domain`] with [`DomainError::IdRequired`] if `id` isn't more than zero.
    /// - [`StoreError::TransactionNotFound`] if no row has the ID.
    /// - Steps 5 and 6 of [`add_transaction`](Self::add_transaction)'s errors, and
    ///   [`StoreError::Sqlite`] if the stored row's type isn't one of v1's three.
    pub fn delete_transaction(&self, id: TransactionId) -> Result<(), StoreError> {
        let id = stored_id(Some(id))?;
        let mut connection = self.lock()?;
        let write = begin(&mut connection)?;
        let previous = read_stored(&write, id)?.ok_or(StoreError::TransactionNotFound { id })?;
        write
            .execute("DELETE FROM transactions WHERE id = ?1", [id.0])
            .map_err(|source| sqlite("delete the transaction", source))?;
        apply_balance_deltas(&write, previous.transaction(), None)?;
        commit(write)
    }
}

/// The ID of a stored row: v1 required one more than zero for an edit or a delete.
fn stored_id(id: Option<TransactionId>) -> Result<TransactionId, StoreError> {
    match id {
        Some(id) if id.0 > 0 => Ok(id),
        Some(_) | None => Err(StoreError::Domain(DomainError::IdRequired)),
    }
}

/// Runs the field checks, then checks that `month` holds the date.
fn validated(draft: &TransactionDraft, month: MonthBounds) -> Result<Transaction, StoreError> {
    let transaction = validate_transaction(draft).map_err(StoreError::Domain)?;
    if month.contains(transaction.date) {
        Ok(transaction)
    } else {
        Err(StoreError::DateOutsideMonth {
            date: transaction.date,
            month,
        })
    }
}

/// Starts a write that holds the database's write lock before it reads anything.
fn begin(connection: &mut Connection) -> Result<rusqlite::Transaction<'_>, StoreError> {
    connection
        .transaction_with_behavior(TransactionBehavior::Immediate)
        .map_err(|source| sqlite("start the write", source))
}

/// Commits a write. A write dropped without this rolls back.
fn commit(write: rusqlite::Transaction<'_>) -> Result<(), StoreError> {
    write
        .commit()
        .map_err(|source| sqlite("commit the write", source))
}

/// The checks of v1's `ValidateTransactionUseCase` that need stored rows, in its order: the
/// category, the account, and a transfer's destination exist, then the budget.
fn check_stored_rules(
    connection: &Connection,
    transaction: &Transaction,
    month: MonthBounds,
    over_budget: OverBudget,
) -> Result<(), StoreError> {
    let category_name = category_name(connection, transaction.category_id)?
        .ok_or(StoreError::Domain(DomainError::CategoryNotFound))?;
    if !account_exists(connection, transaction.account_id)? {
        return Err(StoreError::Domain(DomainError::AccountNotFound));
    }
    if let Some(destination) = transaction.kind.transfer_account()
        && !account_exists(connection, destination)?
    {
        return Err(StoreError::Domain(DomainError::TransferDestinationNotFound));
    }
    match (over_budget, transaction.budgeted_amount()) {
        (OverBudget::Refuse, Some(amount)) => {
            check_budget(connection, transaction, amount, &category_name, month)
        }
        (OverBudget::Allow, _) | (OverBudget::Refuse, None) => Ok(()),
    }
}

/// Fails with [`DomainError::BudgetExceeded`] if the category has a budget and its expenses in
/// `month`, leaving out the stored version of this transaction and adding `amount`, go past it.
fn check_budget(
    connection: &Connection,
    transaction: &Transaction,
    amount: Cents,
    category_name: &str,
    month: MonthBounds,
) -> Result<(), StoreError> {
    let Some(limit) = monthly_limit(connection, transaction.category_id)? else {
        return Ok(());
    };
    let spent = expenses_in_month(connection, transaction.category_id, month, transaction.id)?;
    match budget_overage(category_name, spent, amount, limit).map_err(|_| StoreError::Overflow)? {
        Some(overage) => Err(StoreError::Domain(DomainError::BudgetExceeded(overage))),
        None => Ok(()),
    }
}

fn category_name(connection: &Connection, id: CategoryId) -> Result<Option<String>, StoreError> {
    connection
        .query_row("SELECT name FROM categories WHERE id = ?1", [id.0], |row| {
            row.get(0)
        })
        .optional()
        .map_err(|source| sqlite("read the category", source))
}

fn account_exists(connection: &Connection, id: AccountId) -> Result<bool, StoreError> {
    connection
        .query_row("SELECT 1 FROM accounts WHERE id = ?1", [id.0], |_| Ok(()))
        .optional()
        .map(|found| found.is_some())
        .map_err(|source| sqlite("read the account", source))
}

/// The category's monthly limit, or `None` when it has no budget.
fn monthly_limit(connection: &Connection, id: CategoryId) -> Result<Option<Cents>, StoreError> {
    connection
        .query_row(
            "SELECT monthlyLimitCents FROM budgets WHERE categoryId = ?1 LIMIT 1",
            [id.0],
            |row| row.get(0).map(Cents),
        )
        .optional()
        .map_err(|source| sqlite("read the category's budget", source))
}

/// The sum of the category's expenses dated in `month`, leaving out the row `exclude`.
///
/// v1 selected the dates from the month's start to one millisecond before the next month's,
/// both inclusive; for whole milliseconds that is the same rows as `start <= date < next start`.
fn expenses_in_month(
    connection: &Connection,
    category: CategoryId,
    month: MonthBounds,
    exclude: Option<TransactionId>,
) -> Result<Cents, StoreError> {
    connection
        .query_row(
            "SELECT COALESCE(SUM(amountCents), 0) FROM transactions \
             WHERE type = ?1 AND categoryId = ?2 \
             AND dateEpochMillis >= ?3 AND dateEpochMillis < ?4 AND id IS NOT ?5",
            params![
                TransactionType::Expense.stored_name(),
                category.0,
                month.start().0,
                month.next_start().0,
                exclude.map(|id| id.0),
            ],
            |row| row.get(0).map(Cents),
        )
        .map_err(|source| sqlite("add up the month's expenses", source))
}

fn insert(
    connection: &Connection,
    transaction: &Transaction,
    created_at: EpochMillis,
) -> Result<TransactionId, StoreError> {
    connection
        .execute(
            "INSERT INTO transactions (type, description, amountCents, categoryId, accountId, \
             transferAccountId, dateEpochMillis, createdAtEpochMillis) \
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)",
            params![
                transaction.transaction_type().stored_name(),
                transaction.description,
                transaction.amount.0,
                transaction.category_id.0,
                transaction.account_id.0,
                transaction.kind.transfer_account().map(|account| account.0),
                transaction.date.0,
                created_at.0,
            ],
        )
        .map_err(|source| sqlite("insert the transaction", source))?;
    Ok(TransactionId(connection.last_insert_rowid()))
}

/// A `type` column's value, read the way Room's generated code for v1 read it: exactly one of
/// the three stored names.
struct StoredType(TransactionType);

impl FromSql for StoredType {
    fn column_result(value: ValueRef<'_>) -> FromSqlResult<Self> {
        let name = value.as_str()?;
        TransactionType::ALL
            .into_iter()
            .find(|kind| kind.stored_name() == name)
            .map(Self)
            .ok_or_else(|| FromSqlError::Other(Box::new(UnknownTransactionType(name.to_owned()))))
    }
}

/// A stored transaction row, as the balance changes see it.
#[derive(Debug, Clone, PartialEq, Eq)]
enum StoredRow {
    /// A row that maps to a transaction.
    Valid(Transaction),
    /// A transfer with no destination. Validation never writes one, but the schema allows it, so
    /// v1's `toDomainTransactionOrNull` read it as having no balance impact (BUG-06).
    MalformedTransfer,
}

impl StoredRow {
    /// What [`balance_deltas`] takes for the row.
    const fn transaction(&self) -> Option<&Transaction> {
        match self {
            Self::Valid(transaction) => Some(transaction),
            Self::MalformedTransfer => None,
        }
    }
}

/// The stored row `id`, or `None` when there is none.
fn read_stored(
    connection: &Connection,
    id: TransactionId,
) -> Result<Option<StoredRow>, StoreError> {
    connection
        .query_row(
            "SELECT type, description, amountCents, categoryId, accountId, transferAccountId, \
             dateEpochMillis FROM transactions WHERE id = ?1",
            [id.0],
            |row| {
                let StoredType(transaction_type) = row.get(0)?;
                let transfer_account: Option<i64> = row.get(5)?;
                let kind =
                    TransactionKind::from_stored(transaction_type, transfer_account.map(AccountId));
                let Some(kind) = kind else {
                    return Ok(StoredRow::MalformedTransfer);
                };
                Ok(StoredRow::Valid(Transaction {
                    id: Some(id),
                    description: row.get(1)?,
                    amount: Cents(row.get(2)?),
                    category_id: CategoryId(row.get(3)?),
                    account_id: AccountId(row.get(4)?),
                    date: EpochMillis(row.get(6)?),
                    kind,
                }))
            },
        )
        .optional()
        .map_err(|source| sqlite("read the transaction", source))
}

/// Changes each account's stored balance by its share of `balance_deltas(previous, current)`.
///
/// Each balance is read, added to with checked arithmetic, and written back. v1 added in SQL
/// (`balanceCents = balanceCents + :delta`), where an integer past the range of 64 bits silently
/// turns into a floating-point number.
fn apply_balance_deltas(
    connection: &Connection,
    previous: Option<&Transaction>,
    current: Option<&Transaction>,
) -> Result<(), StoreError> {
    let deltas: BTreeMap<AccountId, Cents> =
        balance_deltas(previous, current).map_err(|_| StoreError::Overflow)?;
    for (account, delta) in deltas {
        let balance: Cents = connection
            .query_row(
                "SELECT balanceCents FROM accounts WHERE id = ?1",
                [account.0],
                |row| row.get(0).map(Cents),
            )
            .optional()
            .map_err(|source| sqlite("read the account's balance", source))?
            .ok_or(StoreError::MissingAccount { id: account })?;
        let balance = balance
            .checked_add(delta)
            .map_err(|_| StoreError::Overflow)?;
        connection
            .execute(
                "UPDATE accounts SET balanceCents = ?1 WHERE id = ?2",
                params![balance.0, account.0],
            )
            .map_err(|source| sqlite("change the account's balance", source))?;
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use std::collections::BTreeMap;
    use std::fs;
    use std::path::PathBuf;

    use denarii_core::{
        AccountId, BudgetOverage, CategoryId, Cents, DomainError, EpochMillis, MonthBounds,
        TransactionDraft, TransactionId, TransactionType,
    };
    use rusqlite::Connection;
    use rusqlite::types::Value;

    use super::{OverBudget, StoredRow, read_stored};
    use crate::{DatabaseKey, Store, StoreError};

    // Moments in UTC, in milliseconds since 1970.
    const SEPTEMBER_15: i64 = 1_789_430_400_000;
    const OCTOBER_START: i64 = 1_790_812_800_000;
    const OCTOBER_1_AT_3: i64 = 1_790_823_600_000;
    const OCTOBER_15: i64 = 1_792_022_400_000;
    const NOVEMBER_START: i64 = 1_793_491_200_000;
    const NOVEMBER_23_AT_NOON: i64 = 1_795_435_200_000;
    const DECEMBER_START: i64 = 1_796_083_200_000;
    const JANUARY_2027_START: i64 = 1_798_761_600_000;
    const SEVEN_HOURS: i64 = 25_200_000;

    /// Fixture F3, which v1's own seeding and use cases wrote (`tests/fixtures/README.md`).
    const LEDGER_FIXTURE: &str = concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/tests/fixtures/v1-f3-ledger.db"
    );

    /// Each F3 account's opening balance in cents, from `tests/fixtures/README.md`.
    const LEDGER_OPENING_BALANCES: [(i64, i64); 4] = [(1, 0), (2, 0), (3, 125_000), (4, -30_000)];

    fn scratch_dir(test: &str) -> PathBuf {
        let dir = std::env::temp_dir().join(format!(
            "denarii-store-transactions-{}-{test}",
            std::process::id()
        ));
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

    fn october() -> MonthBounds {
        month(OCTOBER_START, NOVEMBER_START)
    }

    fn november() -> MonthBounds {
        month(NOVEMBER_START, DECEMBER_START)
    }

    /// A new database with v1's `TestData` (`app/src/test/.../testutil/FakeRepositories.kt` at
    /// `6d57526`): three accounts at zero, four categories, and the 10,000-cent monthly budget on
    /// Groceries that v1's `ValidateTransactionUseCaseTest` sets up.
    fn test_store(test: &str) -> Store {
        let store = Store::open(scratch_dir(test).join("ledger.db"), &fixture_key()).unwrap();
        store
            .lock()
            .unwrap()
            .execute_batch(
                "INSERT INTO accounts (id, name, balanceCents) \
                 VALUES (1, 'Cash', 0), (2, 'Savings', 0), (3, 'Checking', 0);
                 INSERT INTO categories (id, name, iconName) VALUES \
                 (1, 'General Expense', 'ic_category_default'), (2, 'General Income', 'income'), \
                 (3, 'Transfer', 'transfer'), (4, 'Groceries', 'ic_category_default');
                 INSERT INTO budgets (categoryId, monthlyLimitCents, warningThresholdPercent) \
                 VALUES (4, 10000, 80);",
            )
            .unwrap();
        store
    }

    fn ledger_store(test: &str) -> Store {
        let path = scratch_dir(test).join("ledger.db");
        fs::copy(LEDGER_FIXTURE, &path).unwrap();
        Store::open(&path, &fixture_key()).unwrap()
    }

    /// v1's `TestData.expense`: "Coffee", 2,000 cents from account 1 in Groceries, dated in
    /// November 2026.
    fn expense(amount: i64) -> TransactionDraft {
        TransactionDraft {
            id: None,
            transaction_type: TransactionType::Expense,
            description: "Coffee".to_owned(),
            amount: Cents(amount),
            category_id: CategoryId(4),
            account_id: AccountId(1),
            transfer_account_id: None,
            date: EpochMillis(NOVEMBER_23_AT_NOON),
        }
    }

    fn transfer(amount: i64, from: i64, to: Option<i64>) -> TransactionDraft {
        TransactionDraft {
            transaction_type: TransactionType::Transfer,
            category_id: CategoryId(3),
            account_id: AccountId(from),
            transfer_account_id: to.map(AccountId),
            ..expense(amount)
        }
    }

    fn add(store: &Store, draft: &TransactionDraft) -> Result<TransactionId, StoreError> {
        store.add_transaction(draft, november(), EpochMillis(1), OverBudget::Refuse)
    }

    fn edit(store: &Store, id: i64, draft: TransactionDraft) -> Result<(), StoreError> {
        let draft = TransactionDraft {
            id: Some(TransactionId(id)),
            ..draft
        };
        store.update_transaction(&draft, november(), OverBudget::Refuse)
    }

    /// The business rule a failed write broke.
    fn rule<T: std::fmt::Debug>(result: Result<T, StoreError>) -> DomainError {
        match result {
            Err(StoreError::Domain(error)) => error,
            other => panic!("expected a broken business rule, got {other:?}"),
        }
    }

    fn overage(category_name: &str, over_by: i64, limit: i64) -> DomainError {
        DomainError::BudgetExceeded(BudgetOverage {
            category_name: category_name.to_owned(),
            over_by: Cents(over_by),
            limit: Cents(limit),
        })
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
            all.push(vec![Value::Text(table.to_owned())]);
            all.extend(rows(
                &connection,
                &format!("SELECT * FROM {table} ORDER BY 1"),
            ));
        }
        all
    }

    fn balances(store: &Store) -> BTreeMap<i64, i64> {
        let connection = store.lock().unwrap();
        let mut statement = connection
            .prepare("SELECT id, balanceCents FROM accounts ORDER BY id")
            .unwrap();
        statement
            .query_map([], |row| Ok((row.get(0)?, row.get(1)?)))
            .unwrap()
            .collect::<Result<_, _>>()
            .unwrap()
    }

    fn balance(store: &Store, account: i64) -> i64 {
        balances(store)[&account]
    }

    fn set_balance(store: &Store, account: i64, cents: i64) {
        store
            .lock()
            .unwrap()
            .execute(
                "UPDATE accounts SET balanceCents = ?1 WHERE id = ?2",
                [cents, account],
            )
            .unwrap();
    }

    /// One stored column of the row `id`.
    fn column(store: &Store, id: i64, name: &str) -> Value {
        store
            .lock()
            .unwrap()
            .query_row(
                &format!("SELECT {name} FROM transactions WHERE id = ?1"),
                [id],
                |row| row.get(0),
            )
            .unwrap()
    }

    fn count(store: &Store) -> i64 {
        store
            .lock()
            .unwrap()
            .query_row("SELECT count(*) FROM transactions", [], |row| row.get(0))
            .unwrap()
    }

    /// Invariant 11 of `docs/specs/store.md` after the use cases' writes: each F3 account's
    /// stored balance is its opening balance plus the impact of every transaction still stored,
    /// read with the use cases' own row mapping.
    fn assert_ledger_balances_match_its_transactions(store: &Store) {
        let mut computed: BTreeMap<i64, i64> = LEDGER_OPENING_BALANCES.into_iter().collect();
        {
            let connection = store.lock().unwrap();
            let ids: Vec<i64> = rows(&connection, "SELECT id FROM transactions ORDER BY id")
                .into_iter()
                .map(|row| match row.as_slice() {
                    [Value::Integer(id)] => *id,
                    other => panic!("unexpected row {other:?}"),
                })
                .collect();
            for id in ids {
                let row = read_stored(&connection, TransactionId(id))
                    .unwrap()
                    .unwrap();
                let Some(transaction) = row.transaction() else {
                    continue;
                };
                for (account, impact) in transaction.account_impacts().unwrap() {
                    let entry = computed.get_mut(&account.0).unwrap();
                    *entry = entry.checked_add(impact.0).unwrap();
                }
            }
        }
        assert_eq!(balances(store), computed);
    }

    // v1's `ValidateTransactionUseCaseTest`.

    #[test]
    fn an_expense_past_its_budget_fails_with_the_overage() {
        let store = test_store("an_expense_past_its_budget_fails_with_the_overage");
        add(&store, &expense(9_000)).unwrap();
        let before = snapshot(&store);
        assert_eq!(
            rule(add(&store, &expense(2_000))),
            overage("Groceries", 1_000, 10_000)
        );
        assert_eq!(rule(add(&store, &expense(2_000))).code(), "BUDGET_EXCEEDED");
        assert_eq!(snapshot(&store), before);
    }

    #[test]
    fn a_confirmed_expense_may_go_past_its_budget() {
        let store = test_store("a_confirmed_expense_may_go_past_its_budget");
        add(&store, &expense(9_000)).unwrap();
        let id = store
            .add_transaction(
                &expense(2_000),
                november(),
                EpochMillis(1),
                OverBudget::Allow,
            )
            .unwrap();
        assert_eq!(id, TransactionId(2));
        assert_eq!(balance(&store, 1), -11_000);
    }

    #[test]
    fn confirming_the_budget_still_checks_everything_else() {
        let store = test_store("confirming_the_budget_still_checks_everything_else");
        add(&store, &expense(9_000)).unwrap();
        let before = snapshot(&store);
        let allow = |draft: &TransactionDraft| {
            store.add_transaction(draft, november(), EpochMillis(1), OverBudget::Allow)
        };
        assert_eq!(rule(allow(&expense(0))), DomainError::InvalidAmount);
        let missing_category = TransactionDraft {
            category_id: CategoryId(99),
            ..expense(2_000)
        };
        assert_eq!(
            rule(allow(&missing_category)),
            DomainError::CategoryNotFound
        );
        assert_eq!(snapshot(&store), before);
    }

    #[test]
    fn an_edited_expense_leaves_its_stored_amount_out_of_the_budget() {
        let store = test_store("an_edited_expense_leaves_its_stored_amount_out_of_the_budget");
        let id = add(&store, &expense(8_000)).unwrap();
        // A new expense counts the stored 8,000 cents.
        assert_eq!(
            rule(add(&store, &expense(2_001))),
            overage("Groceries", 1, 10_000)
        );
        // The edit of that row doesn't.
        edit(&store, id.0, expense(10_000)).unwrap();
        assert_eq!(
            rule(edit(&store, id.0, expense(10_001))),
            overage("Groceries", 1, 10_000)
        );
        assert_eq!(column(&store, id.0, "amountCents"), Value::Integer(10_000));
        assert_eq!(balance(&store, 1), -10_000);
    }

    #[test]
    fn an_expense_exactly_at_the_limit_is_allowed() {
        let store = test_store("an_expense_exactly_at_the_limit_is_allowed");
        add(&store, &expense(8_000)).unwrap();
        assert_eq!(
            rule(add(&store, &expense(2_001))),
            overage("Groceries", 1, 10_000)
        );
        add(&store, &expense(2_000)).unwrap();
    }

    #[test]
    fn a_missing_category_account_or_destination_fails_in_v1s_order() {
        let store = test_store("a_missing_category_account_or_destination_fails_in_v1s_order");
        let before = snapshot(&store);
        let nothing_stored = TransactionDraft {
            category_id: CategoryId(99),
            ..transfer(1_000, 98, Some(99))
        };
        assert_eq!(
            rule(add(&store, &nothing_stored)),
            DomainError::CategoryNotFound
        );
        assert_eq!(
            rule(add(&store, &transfer(1_000, 98, Some(99)))),
            DomainError::AccountNotFound
        );
        assert_eq!(
            rule(add(&store, &transfer(1_000, 1, Some(99)))),
            DomainError::TransferDestinationNotFound
        );
        assert_eq!(snapshot(&store), before);
    }

    /// The field rules run before anything is read, in the order `validate_transaction` lists,
    /// and a draft that breaks one writes nothing.
    #[test]
    fn the_field_rules_run_first_and_write_nothing() {
        let store = test_store("the_field_rules_run_first_and_write_nothing");
        let before = snapshot(&store);
        let cases = [
            (expense(0), DomainError::InvalidAmount),
            (expense(-1), DomainError::InvalidAmount),
            (
                TransactionDraft {
                    description: "   ".to_owned(),
                    ..expense(2_000)
                },
                DomainError::BlankDescription,
            ),
            (
                TransactionDraft {
                    description: "x".repeat(201),
                    ..expense(2_000)
                },
                DomainError::DescriptionTooLong { max_units: 200 },
            ),
            (
                TransactionDraft {
                    date: EpochMillis(0),
                    ..expense(2_000)
                },
                DomainError::InvalidDate,
            ),
            (
                TransactionDraft {
                    account_id: AccountId(0),
                    ..expense(2_000)
                },
                DomainError::InvalidAccount,
            ),
            (
                TransactionDraft {
                    category_id: CategoryId(0),
                    ..expense(2_000)
                },
                DomainError::InvalidCategory,
            ),
            (
                transfer(1_000, 1, None),
                DomainError::TransferDestinationRequired,
            ),
            (
                transfer(1_000, 1, Some(1)),
                DomainError::TransferDestinationSame,
            ),
            // Missing rows are checked after the fields.
            (
                TransactionDraft {
                    category_id: CategoryId(99),
                    ..expense(0)
                },
                DomainError::InvalidAmount,
            ),
        ];
        for (draft, error) in cases {
            assert_eq!(rule(add(&store, &draft)), error, "{draft:?}");
            // Row 1 doesn't exist: the fields are checked before the row is looked for.
            let edited = TransactionDraft {
                id: Some(TransactionId(1)),
                ..draft
            };
            assert_eq!(
                rule(store.update_transaction(&edited, november(), OverBudget::Refuse)),
                error
            );
        }
        assert_eq!(snapshot(&store), before);
        let longest = TransactionDraft {
            description: "x".repeat(200),
            ..expense(2_000)
        };
        add(&store, &longest).unwrap();
    }

    #[test]
    fn a_valid_transfer_passes() {
        let store = test_store("a_valid_transfer_passes");
        add(&store, &transfer(4_000, 1, Some(2))).unwrap();
        assert_eq!(
            balances(&store),
            BTreeMap::from([(1, -4_000), (2, 4_000), (3, 0)])
        );
    }

    #[test]
    fn only_expenses_count_against_a_budget() {
        let store = test_store("only_expenses_count_against_a_budget");
        add(&store, &expense(9_000)).unwrap();
        let income = TransactionDraft {
            transaction_type: TransactionType::Income,
            ..expense(5_000)
        };
        add(&store, &income).unwrap();
        let transfer_in_groceries = TransactionDraft {
            category_id: CategoryId(4),
            ..transfer(5_000, 1, Some(2))
        };
        add(&store, &transfer_in_groceries).unwrap();
        // Neither counts toward the month's expenses.
        add(&store, &expense(1_000)).unwrap();
    }

    #[test]
    fn an_expense_in_a_category_without_a_budget_has_no_limit() {
        let store = test_store("an_expense_in_a_category_without_a_budget_has_no_limit");
        let general = TransactionDraft {
            category_id: CategoryId(1),
            ..expense(500_000)
        };
        add(&store, &general).unwrap();
        add(&store, &general).unwrap();
        assert_eq!(balance(&store, 1), -1_000_000);
    }

    /// v1's `budgetMonthFollowsTheClockZone`: 03:00 UTC on October 1 is still September 30 in
    /// Los Angeles, so September's spending counts. Here the caller's bounds say which month.
    #[test]
    fn the_budget_counts_the_month_the_caller_passes() {
        let store = test_store("the_budget_counts_the_month_the_caller_passes");
        let los_angeles_september = month(
            OCTOBER_START - 30 * 86_400_000 + SEVEN_HOURS,
            OCTOBER_START + SEVEN_HOURS,
        );
        let mid_september = TransactionDraft {
            date: EpochMillis(SEPTEMBER_15),
            ..expense(9_000)
        };
        store
            .add_transaction(
                &mid_september,
                los_angeles_september,
                EpochMillis(1),
                OverBudget::Refuse,
            )
            .unwrap();
        let early_october_utc = TransactionDraft {
            date: EpochMillis(OCTOBER_1_AT_3),
            ..expense(2_000)
        };
        assert_eq!(
            rule(store.add_transaction(
                &early_october_utc,
                los_angeles_september,
                EpochMillis(1),
                OverBudget::Refuse,
            )),
            overage("Groceries", 1_000, 10_000)
        );
        store
            .add_transaction(
                &early_october_utc,
                october(),
                EpochMillis(1),
                OverBudget::Refuse,
            )
            .unwrap();
    }

    /// The month holds its first moment and not the next month's first moment, as v1's
    /// inclusive range up to one millisecond before the next month held them.
    #[test]
    fn the_budget_counts_the_months_first_moment_and_not_the_next_months() {
        let store = test_store("the_budget_counts_the_months_first_moment_and_not_the_next_months");
        let dated = |date: i64, amount: i64| TransactionDraft {
            date: EpochMillis(date),
            ..expense(amount)
        };
        let december = month(DECEMBER_START, JANUARY_2027_START);
        let refuse = OverBudget::Refuse;
        let at = EpochMillis(1);
        store
            .add_transaction(&dated(NOVEMBER_START - 1, 6_000), october(), at, refuse)
            .unwrap();
        store
            .add_transaction(&dated(NOVEMBER_START, 6_000), november(), at, refuse)
            .unwrap();
        store
            .add_transaction(&dated(DECEMBER_START, 6_000), december, at, refuse)
            .unwrap();
        // November holds 6,000 cents: only the expense at its first moment.
        assert_eq!(
            rule(add(&store, &expense(4_001))),
            overage("Groceries", 1, 10_000)
        );
        add(&store, &expense(4_000)).unwrap();
    }

    #[test]
    fn a_date_outside_the_callers_month_is_refused_for_every_type() {
        let store = test_store("a_date_outside_the_callers_month_is_refused_for_every_type");
        let before = snapshot(&store);
        let income = TransactionDraft {
            transaction_type: TransactionType::Income,
            ..expense(5_000)
        };
        for draft in [expense(1_000), income, transfer(1_000, 1, Some(2))] {
            let result =
                store.add_transaction(&draft, october(), EpochMillis(1), OverBudget::Allow);
            assert!(
                matches!(
                    result,
                    Err(StoreError::DateOutsideMonth { date, month })
                        if date == EpochMillis(NOVEMBER_23_AT_NOON) && month == october()
                ),
                "{result:?}"
            );
        }
        let edited = TransactionDraft {
            id: Some(TransactionId(1)),
            ..expense(1_000)
        };
        assert!(matches!(
            store.update_transaction(&edited, october(), OverBudget::Refuse),
            Err(StoreError::DateOutsideMonth { .. })
        ));
        assert_eq!(snapshot(&store), before);
    }

    // v1's `TransactionCrudUseCaseTest`.

    #[test]
    fn add_ignores_a_caller_supplied_id() {
        let store = test_store("add_ignores_a_caller_supplied_id");
        assert_eq!(add(&store, &expense(1_000)).unwrap(), TransactionId(1));
        let with_id = TransactionDraft {
            id: Some(TransactionId(1)),
            ..expense(500)
        };
        assert_eq!(add(&store, &with_id).unwrap(), TransactionId(2));
        assert_eq!(count(&store), 2);
        assert_eq!(column(&store, 1, "amountCents"), Value::Integer(1_000));
    }

    #[test]
    fn an_edit_replaces_the_stored_transaction() {
        let store = test_store("an_edit_replaces_the_stored_transaction");
        add(&store, &expense(1_000)).unwrap();
        let refund = TransactionDraft {
            transaction_type: TransactionType::Income,
            description: "Refund".to_owned(),
            category_id: CategoryId(2),
            ..expense(3_000)
        };
        edit(&store, 1, refund).unwrap();
        assert_eq!(count(&store), 1);
        assert_eq!(column(&store, 1, "type"), Value::Text("INCOME".to_owned()));
        assert_eq!(column(&store, 1, "amountCents"), Value::Integer(3_000));
        assert_eq!(column(&store, 1, "categoryId"), Value::Integer(2));
        assert_eq!(balance(&store, 1), 3_000);
    }

    #[test]
    fn an_edit_needs_an_id_and_a_stored_row() {
        let store = test_store("an_edit_needs_an_id_and_a_stored_row");
        add(&store, &expense(1_000)).unwrap();
        let before = snapshot(&store);
        let no_id = expense(500);
        assert_eq!(
            rule(store.update_transaction(&no_id, november(), OverBudget::Refuse)),
            DomainError::IdRequired
        );
        assert_eq!(rule(edit(&store, 0, expense(500))), DomainError::IdRequired);
        assert_eq!(
            rule(edit(&store, -1, expense(500))),
            DomainError::IdRequired
        );
        // v1 checked the ID before the fields.
        assert_eq!(rule(edit(&store, 0, expense(0))), DomainError::IdRequired);
        assert!(matches!(
            edit(&store, 42, expense(500)),
            Err(StoreError::TransactionNotFound {
                id: TransactionId(42)
            })
        ));
        assert_eq!(snapshot(&store), before);
    }

    #[test]
    fn an_edit_runs_validation() {
        let store = test_store("an_edit_runs_validation");
        add(&store, &expense(1_000)).unwrap();
        assert_eq!(
            rule(edit(&store, 1, expense(-100))),
            DomainError::InvalidAmount
        );
        assert_eq!(column(&store, 1, "amountCents"), Value::Integer(1_000));
    }

    #[test]
    fn delete_removes_the_row_and_reports_a_missing_one() {
        let store = test_store("delete_removes_the_row_and_reports_a_missing_one");
        add(&store, &expense(1_000)).unwrap();
        store.delete_transaction(TransactionId(1)).unwrap();
        assert_eq!(count(&store), 0);
        assert!(matches!(
            store.delete_transaction(TransactionId(1)),
            Err(StoreError::TransactionNotFound {
                id: TransactionId(1)
            })
        ));
        assert_eq!(
            rule(store.delete_transaction(TransactionId(0))),
            DomainError::IdRequired
        );
    }

    #[test]
    fn add_and_edit_store_the_description_trimmed() {
        let store = test_store("add_and_edit_store_the_description_trimmed");
        let lunch = TransactionDraft {
            description: "  Lunch  ".to_owned(),
            ..expense(500)
        };
        let id = add(&store, &lunch).unwrap();
        assert_eq!(
            column(&store, id.0, "description"),
            Value::Text("Lunch".to_owned())
        );
        let dinner = TransactionDraft {
            description: " Dinner ".to_owned(),
            ..expense(500)
        };
        edit(&store, id.0, dinner).unwrap();
        assert_eq!(
            column(&store, id.0, "description"),
            Value::Text("Dinner".to_owned())
        );
    }

    // v1's `TransactionRepositoryImplTest`.

    #[test]
    fn add_edit_and_delete_keep_balances_in_step() {
        let store = test_store("add_edit_and_delete_keep_balances_in_step");
        set_balance(&store, 1, 10_000);
        let id = add(&store, &expense(3_000)).unwrap();
        assert_eq!(balance(&store, 1), 7_000);

        let from_savings = TransactionDraft {
            account_id: AccountId(2),
            ..expense(5_000)
        };
        edit(&store, id.0, from_savings).unwrap();
        assert_eq!(balance(&store, 1), 10_000);
        assert_eq!(balance(&store, 2), -5_000);

        store.delete_transaction(id).unwrap();
        assert_eq!(balance(&store, 2), 0);
        assert_eq!(count(&store), 0);
    }

    #[test]
    fn a_transfer_debits_its_account_and_credits_its_destination() {
        let store = test_store("a_transfer_debits_its_account_and_credits_its_destination");
        set_balance(&store, 1, 10_000);
        add(&store, &transfer(4_000, 1, Some(2))).unwrap();
        assert_eq!(balance(&store, 1), 6_000);
        assert_eq!(balance(&store, 2), 4_000);
        assert_eq!(column(&store, 1, "transferAccountId"), Value::Integer(2));
        // An expense's destination column stays empty, whatever the draft held.
        let expense_with_destination = TransactionDraft {
            transfer_account_id: Some(AccountId(2)),
            ..expense(1_000)
        };
        let id = add(&store, &expense_with_destination).unwrap();
        assert_eq!(column(&store, id.0, "transferAccountId"), Value::Null);
    }

    #[test]
    fn an_edit_keeps_the_rows_creation_time() {
        let store = test_store("an_edit_keeps_the_rows_creation_time");
        let id = store
            .add_transaction(
                &expense(1_000),
                november(),
                EpochMillis(5),
                OverBudget::Refuse,
            )
            .unwrap();
        edit(&store, id.0, expense(1_200)).unwrap();
        assert_eq!(
            column(&store, id.0, "createdAtEpochMillis"),
            Value::Integer(5)
        );
        assert_eq!(
            column(&store, id.0, "dateEpochMillis"),
            Value::Integer(NOVEMBER_23_AT_NOON)
        );
    }

    /// v1's `missingAccountRollsBackWholeOperation`, where the balance change fails after the row
    /// is written: here a balance at the edge of the range overflows.
    #[test]
    fn a_balance_change_that_fails_after_the_row_is_written_rolls_the_row_back() {
        let store =
            test_store("a_balance_change_that_fails_after_the_row_is_written_rolls_the_row_back");
        set_balance(&store, 2, i64::MAX - 10);
        let before = snapshot(&store);
        assert!(matches!(
            add(&store, &transfer(11, 1, Some(2))),
            Err(StoreError::Overflow)
        ));
        assert_eq!(snapshot(&store), before);
        assert_eq!(count(&store), 0);
        // The next ID is still 1: the counter in `sqlite_sequence` rolled back too.
        assert_eq!(
            add(&store, &transfer(10, 1, Some(2))).unwrap(),
            TransactionId(1)
        );
        assert_eq!(balance(&store, 2), i64::MAX);
    }

    /// A transfer whose destination account was deleted can't be validated, so only a damaged
    /// file holds one; deleting it fails at the balance change and changes nothing.
    #[test]
    fn a_balance_change_for_an_account_with_no_row_rolls_back() {
        let store = test_store("a_balance_change_for_an_account_with_no_row_rolls_back");
        let id = add(&store, &transfer(1_000, 1, Some(3))).unwrap();
        // `transferAccountId` has no foreign key, so nothing stops this.
        store
            .lock()
            .unwrap()
            .execute("DELETE FROM accounts WHERE id = 3", [])
            .unwrap();
        let before = snapshot(&store);
        assert!(matches!(
            store.delete_transaction(id),
            Err(StoreError::MissingAccount { id: AccountId(3) })
        ));
        assert_eq!(snapshot(&store), before);
    }

    #[test]
    fn edit_and_delete_report_an_unknown_id() {
        let store = test_store("edit_and_delete_report_an_unknown_id");
        assert!(matches!(
            edit(&store, 42, expense(100)),
            Err(StoreError::TransactionNotFound {
                id: TransactionId(42)
            })
        ));
        assert!(matches!(
            store.delete_transaction(TransactionId(42)),
            Err(StoreError::TransactionNotFound {
                id: TransactionId(42)
            })
        ));
    }

    #[test]
    fn a_malformed_transfer_is_deleted_without_touching_balances() {
        let store = test_store("a_malformed_transfer_is_deleted_without_touching_balances");
        set_balance(&store, 1, 10_000);
        store
            .lock()
            .unwrap()
            .execute(
                "INSERT INTO transactions (type, description, amountCents, categoryId, \
                 accountId, transferAccountId, dateEpochMillis, createdAtEpochMillis) \
                 VALUES ('TRANSFER', 'Coffee', 500, 1, 1, NULL, 1000, 1)",
                [],
            )
            .unwrap();
        store.delete_transaction(TransactionId(1)).unwrap();
        assert_eq!(count(&store), 0);
        assert_eq!(balance(&store, 1), 10_000);
    }

    #[test]
    fn a_stored_type_other_than_v1s_three_names_is_refused() {
        let store = test_store("a_stored_type_other_than_v1s_three_names_is_refused");
        add(&store, &expense(1_000)).unwrap();
        // Room's generated code for v1 read only the exact names.
        store
            .lock()
            .unwrap()
            .execute("UPDATE transactions SET type = 'expense' WHERE id = 1", [])
            .unwrap();
        let before = snapshot(&store);
        assert!(matches!(
            store.delete_transaction(TransactionId(1)),
            Err(StoreError::Sqlite {
                operation: "read the transaction",
                ..
            })
        ));
        assert!(matches!(
            edit(&store, 1, expense(1_000)),
            Err(StoreError::Sqlite {
                operation: "read the transaction",
                ..
            })
        ));
        assert_eq!(snapshot(&store), before);
    }

    /// Each write holds the store's lock for its row and its balance changes, so writes from
    /// several threads never lose a balance change.
    #[test]
    fn writes_from_several_threads_keep_every_balance_change() {
        let store = test_store("writes_from_several_threads_keep_every_balance_change");
        let general = TransactionDraft {
            category_id: CategoryId(1),
            ..expense(7)
        };
        std::thread::scope(|scope| {
            for _ in 0..8 {
                scope.spawn(|| {
                    for _ in 0..25 {
                        add(&store, &general).unwrap();
                        add(&store, &transfer(3, 1, Some(2))).unwrap();
                    }
                });
            }
        });
        assert_eq!(count(&store), 400);
        assert_eq!(
            balances(&store),
            BTreeMap::from([(1, -2_000), (2, 600), (3, 0)])
        );
    }

    // Fixture F3, which v1's use cases wrote.

    /// F3's groceries category has a 40,000-cent budget, and November 2026 already holds 42,345
    /// cents of its expenses (`tests/fixtures/README.md`).
    #[test]
    fn an_expense_in_the_f3_fixtures_full_november_needs_confirming() {
        let store = ledger_store("an_expense_in_the_f3_fixtures_full_november_needs_confirming");
        let before = snapshot(&store);
        let groceries = TransactionDraft {
            account_id: AccountId(3),
            ..expense(100)
        };
        assert_eq!(
            rule(add(&store, &groceries)),
            overage("\u{c9}picerie \u{1f6d2}", 2_445, 40_000)
        );
        assert_eq!(snapshot(&store), before);

        let id = store
            .add_transaction(&groceries, november(), EpochMillis(1), OverBudget::Allow)
            .unwrap();
        // Row 11 was deleted in v1, and the counter stood at 12.
        assert_eq!(id, TransactionId(13));
        assert_eq!(balance(&store, 3), 269_655 - 100);
        assert_ledger_balances_match_its_transactions(&store);
    }

    /// October's 35,000 cents of groceries count only in October.
    #[test]
    fn the_f3_fixtures_october_has_5_000_cents_left_in_its_budget() {
        let store = ledger_store("the_f3_fixtures_october_has_5_000_cents_left_in_its_budget");
        let mid_october = |amount: i64| TransactionDraft {
            account_id: AccountId(3),
            date: EpochMillis(OCTOBER_15),
            ..expense(amount)
        };
        let refuse = OverBudget::Refuse;
        assert_eq!(
            rule(store.add_transaction(&mid_october(5_001), october(), EpochMillis(1), refuse)),
            overage("\u{c9}picerie \u{1f6d2}", 1, 40_000)
        );
        store
            .add_transaction(&mid_october(5_000), october(), EpochMillis(1), refuse)
            .unwrap();
        assert_ledger_balances_match_its_transactions(&store);
    }

    /// Editing row 5, the 30,000-cent expense that took November past its budget, leaves its own
    /// amount out: the other 12,345 cents leave 27,655 for it.
    #[test]
    fn editing_the_f3_fixtures_over_budget_expense_leaves_its_amount_out() {
        let store =
            ledger_store("editing_the_f3_fixtures_over_budget_expense_leaves_its_amount_out");
        let row_5 = |amount: i64| TransactionDraft {
            description: "Grande course".to_owned(),
            account_id: AccountId(4),
            date: EpochMillis(1_794_441_600_000),
            ..expense(amount)
        };
        assert_eq!(
            rule(edit(&store, 5, row_5(27_656))),
            overage("\u{c9}picerie \u{1f6d2}", 1, 40_000)
        );
        edit(&store, 5, row_5(27_655)).unwrap();
        assert_eq!(balance(&store, 4), -44_000 + 2_345);
        assert_ledger_balances_match_its_transactions(&store);
    }

    /// Row 12 is a transfer with no destination (BUG-06): it has no impact to take back.
    #[test]
    fn the_f3_fixtures_malformed_transfer_is_edited_and_deleted_by_its_impact_alone() {
        let store = ledger_store(
            "the_f3_fixtures_malformed_transfer_is_edited_and_deleted_by_its_impact_alone",
        );
        {
            let connection = store.lock().unwrap();
            assert_eq!(
                read_stored(&connection, TransactionId(12)).unwrap(),
                Some(StoredRow::MalformedTransfer)
            );
        }
        let before = balances(&store);
        let fixed = TransactionDraft {
            description: "Virement sans destination".to_owned(),
            ..transfer(4_200, 1, Some(2))
        };
        edit(&store, 12, fixed).unwrap();
        assert_eq!(balance(&store, 1), before[&1] - 4_200);
        assert_eq!(balance(&store, 2), before[&2] + 4_200);
        assert_ledger_balances_match_its_transactions(&store);

        let store = ledger_store(
            "the_f3_fixtures_malformed_transfer_is_edited_and_deleted_by_its_impact_alone-2",
        );
        store.delete_transaction(TransactionId(12)).unwrap();
        assert_eq!(balances(&store), before);
        assert_ledger_balances_match_its_transactions(&store);
    }

    /// Deleting every F3 transaction brings each account back to its opening balance.
    #[test]
    fn deleting_every_transaction_of_the_f3_fixture_leaves_the_opening_balances() {
        let store = ledger_store(
            "deleting_every_transaction_of_the_f3_fixture_leaves_the_opening_balances",
        );
        for id in (1..=12).filter(|&id| id != 11) {
            store.delete_transaction(TransactionId(id)).unwrap();
        }
        assert_eq!(count(&store), 0);
        assert_eq!(
            balances(&store),
            LEDGER_OPENING_BALANCES.into_iter().collect()
        );
        let connection = store.lock().unwrap();
        assert_eq!(
            rows(&connection, "PRAGMA foreign_key_check"),
            Vec::<Vec<Value>>::new()
        );
    }
}
