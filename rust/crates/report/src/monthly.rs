/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! A month's report: its transactions with their names and dates, and its totals.
//!
//! This ports v1's `GenerateReportUseCase` (`app/.../domain/usecase/GenerateReportUseCase.kt` at
//! `6d57526`), less its reads, which `denarii-store` runs: the caller reads the month's
//! transactions and every category and account, and passes them here with the month's days.

use std::collections::BTreeMap;

use denarii_core::{
    Account, AccountId, Category, CategoryId, Cents, EpochMillis, StoredTransaction, Totals,
    TransactionId, TransactionType, totals,
};

use crate::calendar::{LocalDate, MonthDays};
use crate::csv::ReportFormat;
use crate::error::ReportError;

/// The direction the user's language is written in, which decides the arrow in a transfer's
/// payment method.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum TextDirection {
    /// Left to right, as in English: `Cash → Savings`.
    LeftToRight,
    /// Right to left, as in Arabic. The source account is read first, on the right, so the arrow
    /// points left toward the destination: `Cash ← Savings`.
    RightToLeft,
}

/// One transaction in a report.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct ReportRow {
    /// The transaction's ID.
    pub transaction_id: TransactionId,
    /// When it happened.
    pub date: EpochMillis,
    /// Its date on the user's calendar.
    pub local_date: LocalDate,
    /// Its stored type.
    pub transaction_type: TransactionType,
    /// Its category's name, or `#` and the category's ID if no category has that ID.
    pub category_name: String,
    /// What it was for.
    pub description: String,
    /// Its stored amount.
    pub amount: Cents,
    /// Its account's name, or `source → destination` when its row names a destination account,
    /// with the arrow [`TextDirection`] picks. An account with no row shows as `#` and its ID.
    pub payment_method: String,
}

/// A month's transactions and totals, as v1's monthly report showed and exported them.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct MonthlyReport {
    year: i32,
    month: u8,
    totals: Totals,
    rows: Vec<ReportRow>,
}

impl MonthlyReport {
    /// The report of the month `days` describes, from the month's `transactions` and every
    /// category and account.
    ///
    /// The rows come oldest first, and by ID among those dated at the same moment, whatever the
    /// order of `transactions`. The totals are [`totals`] over every row that maps to a
    /// transaction: a malformed transfer, one with no destination, is listed but counts in no
    /// total (BUG-06). A row's payment method follows its stored `transfer_account_id`, as v1's
    /// did, even on an expense or income.
    ///
    /// # Errors
    ///
    /// - [`ReportError::DateOutsideMonth`] for the first of `transactions`, in the order given,
    ///   that `days` doesn't hold.
    /// - [`ReportError::Overflow`] if a total goes past the range of [`Cents`].
    ///
    /// # Examples
    ///
    /// ```
    /// use denarii_core::{
    ///     Account, AccountId, Category, CategoryId, Cents, EpochMillis, StoredTransaction,
    ///     TransactionId, TransactionType,
    /// };
    /// use denarii_report::{MonthDays, MonthlyReport, TextDirection};
    ///
    /// # fn main() -> Result<(), Box<dyn std::error::Error>> {
    /// // February 2026 in UTC.
    /// const DAY: i64 = 86_400_000;
    /// let start = 1_769_904_000_000;
    /// let starts = (0..28).map(|d| EpochMillis(start + d * DAY)).collect();
    /// let february = MonthDays::new(2026, 2, starts, EpochMillis(start + 28 * DAY))?;
    /// let savings = StoredTransaction {
    ///     id: TransactionId(3),
    ///     transaction_type: TransactionType::Transfer,
    ///     description: "Savings".to_owned(),
    ///     amount: Cents(30_000),
    ///     category_id: CategoryId(3),
    ///     account_id: AccountId(1),
    ///     transfer_account_id: Some(AccountId(2)),
    ///     date: EpochMillis(start + 9 * DAY),
    /// };
    /// let categories = [Category { id: Some(CategoryId(3)), ..Category::new("Transfer") }];
    /// let accounts = [
    ///     Account { id: Some(AccountId(1)), name: "Cash".to_owned(), balance: Cents(0) },
    ///     Account { id: Some(AccountId(2)), name: "Savings".to_owned(), balance: Cents(0) },
    /// ];
    ///
    /// let report =
    ///     MonthlyReport::new(&february, &[savings], &categories, &accounts, TextDirection::LeftToRight)?;
    /// assert_eq!(report.rows()[0].payment_method, "Cash → Savings");
    /// assert_eq!(report.rows()[0].local_date.to_string(), "2026-02-10");
    /// assert_eq!(report.totals().net, Cents(0));
    /// # Ok(())
    /// # }
    /// ```
    pub fn new(
        days: &MonthDays,
        transactions: &[StoredTransaction],
        categories: &[Category],
        accounts: &[Account],
        direction: TextDirection,
    ) -> Result<Self, ReportError> {
        let category_names: BTreeMap<CategoryId, &str> = categories
            .iter()
            .filter_map(|category| Some((category.id?, category.name.as_str())))
            .collect();
        let account_names: BTreeMap<AccountId, &str> = accounts
            .iter()
            .filter_map(|account| Some((account.id?, account.name.as_str())))
            .collect();
        let account_name = |id: AccountId| {
            account_names
                .get(&id)
                .map_or_else(|| format!("#{}", id.0), |name| (*name).to_owned())
        };

        let mut rows = Vec::with_capacity(transactions.len());
        for transaction in transactions {
            let local_date =
                days.date_of(transaction.date)
                    .ok_or(ReportError::DateOutsideMonth {
                        id: transaction.id,
                        date: transaction.date,
                    })?;
            let source = account_name(transaction.account_id);
            let payment_method = match transaction.transfer_account_id {
                Some(destination) => transfer_route(&source, &account_name(destination), direction),
                None => source,
            };
            rows.push(ReportRow {
                transaction_id: transaction.id,
                date: transaction.date,
                local_date,
                transaction_type: transaction.transaction_type,
                category_name: category_names.get(&transaction.category_id).map_or_else(
                    || format!("#{}", transaction.category_id.0),
                    |name| (*name).to_owned(),
                ),
                description: transaction.description.clone(),
                amount: transaction.amount,
                payment_method,
            });
        }
        rows.sort_by_key(|row| (row.date, row.transaction_id));

        let valid: Vec<_> = transactions
            .iter()
            .filter_map(StoredTransaction::transaction)
            .collect();
        let totals = totals(&valid).map_err(|_| ReportError::Overflow)?;
        Ok(Self {
            year: days.year(),
            month: days.month(),
            totals,
            rows,
        })
    }

    /// The report's year.
    #[must_use]
    pub const fn year(&self) -> i32 {
        self.year
    }

    /// The report's month, from 1 to 12.
    #[must_use]
    pub const fn month(&self) -> u8 {
        self.month
    }

    /// The month's income, expenses, and net change.
    #[must_use]
    pub const fn totals(&self) -> Totals {
        self.totals
    }

    /// The month's transactions, oldest first.
    #[must_use]
    pub fn rows(&self) -> &[ReportRow] {
        &self.rows
    }

    /// The name of the report's file in `format`: `spending-report-2026-10.csv`, for example.
    #[must_use]
    pub fn file_name(&self, format: ReportFormat) -> String {
        format!(
            "spending-report-{:04}-{:02}.{}",
            self.year,
            self.month,
            format.extension()
        )
    }
}

/// A transfer's accounts, source first, as v1's `transferRoute` wrote them.
fn transfer_route(source: &str, destination: &str, direction: TextDirection) -> String {
    match direction {
        TextDirection::LeftToRight => format!("{source} → {destination}"),
        TextDirection::RightToLeft => format!("{source} ← {destination}"),
    }
}

#[cfg(test)]
mod tests {
    use denarii_core::{
        Account, AccountId, Category, CategoryId, Cents, EpochMillis, StoredTransaction, Totals,
        TransactionId, TransactionType,
    };

    use super::{MonthlyReport, TextDirection};
    use crate::calendar::{LocalDate, MonthDays};
    use crate::csv::ReportFormat;
    use crate::error::ReportError;

    const DAY: i64 = 86_400_000;
    /// 2026-09-01T00:00:00Z, the start of v1's `GenerateReportUseCaseTest` month in UTC.
    const SEPTEMBER: i64 = 1_788_220_800_000;

    fn september() -> MonthDays {
        let mut moments =
            std::iter::successors(Some(SEPTEMBER), |s| s.checked_add(DAY)).map(EpochMillis);
        let starts = moments.by_ref().take(30).collect();
        MonthDays::new(2026, 9, starts, moments.next().unwrap()).unwrap()
    }

    /// v1's `TestData.categories`.
    fn categories() -> Vec<Category> {
        [
            (1, "General Expense"),
            (2, "General Income"),
            (3, "Transfer"),
            (4, "Groceries"),
        ]
        .into_iter()
        .map(|(id, name)| Category {
            id: Some(CategoryId(id)),
            ..Category::new(name)
        })
        .collect()
    }

    /// v1's `TestData.accounts`.
    fn accounts() -> Vec<Account> {
        [(1, "Cash"), (2, "Savings"), (3, "Checking")]
            .into_iter()
            .map(|(id, name)| Account {
                id: Some(AccountId(id)),
                name: name.to_owned(),
                balance: Cents::ZERO,
            })
            .collect()
    }

    fn row(
        id: i64,
        transaction_type: TransactionType,
        amount: i64,
        category: i64,
        transfer: Option<i64>,
        date: i64,
    ) -> StoredTransaction {
        StoredTransaction {
            id: TransactionId(id),
            transaction_type,
            description: format!("Row {id}"),
            amount: Cents(amount),
            category_id: CategoryId(category),
            account_id: AccountId(1),
            transfer_account_id: transfer.map(AccountId),
            date: EpochMillis(date),
        }
    }

    /// v1's `GenerateReportUseCaseTest` rows of September 2026: rent, a salary, and a transfer.
    fn v1_rows() -> Vec<StoredTransaction> {
        vec![
            row(
                1,
                TransactionType::Expense,
                90_000,
                4,
                None,
                SEPTEMBER + 2_000,
            ),
            row(
                2,
                TransactionType::Income,
                200_000,
                2,
                None,
                SEPTEMBER + 1_000,
            ),
            row(
                3,
                TransactionType::Transfer,
                30_000,
                3,
                Some(2),
                SEPTEMBER + 3_000,
            ),
        ]
    }

    fn report(rows: &[StoredTransaction]) -> Result<MonthlyReport, ReportError> {
        MonthlyReport::new(
            &september(),
            rows,
            &categories(),
            &accounts(),
            TextDirection::LeftToRight,
        )
    }

    #[test]
    fn report_has_its_month_and_totals() {
        // v1's GenerateReportUseCaseTest.reportHasTitlePeriodTimestampAndTotals; the title and the
        // time it was made are Dart's now.
        let report = report(&v1_rows()).unwrap();

        assert_eq!((report.year(), report.month()), (2026, 9));
        assert_eq!(
            report.totals(),
            Totals {
                income: Cents(200_000),
                expense: Cents(90_000),
                net: Cents(110_000),
            }
        );
    }

    #[test]
    fn rows_are_chronological_with_names_and_payment_method() {
        // v1's GenerateReportUseCaseTest.rowsAreChronologicalWithNamesAndPaymentMethod.
        let report = report(&v1_rows()).unwrap();
        let rows = report.rows();

        let ids: Vec<i64> = rows.iter().map(|row| row.transaction_id.0).collect();
        assert_eq!(ids, [2, 1, 3]);
        assert_eq!(rows[1].category_name, "Groceries");
        assert_eq!(rows[1].payment_method, "Cash");
        assert_eq!(rows[2].payment_method, "Cash → Savings");
    }

    #[test]
    fn each_row_carries_its_transactions_fields_and_date() {
        let report = report(&v1_rows()).unwrap();

        assert_eq!(
            report.rows()[0],
            super::ReportRow {
                transaction_id: TransactionId(2),
                date: EpochMillis(SEPTEMBER + 1_000),
                local_date: LocalDate {
                    year: 2026,
                    month: 9,
                    day: 1
                },
                transaction_type: TransactionType::Income,
                category_name: "General Income".to_owned(),
                description: "Row 2".to_owned(),
                amount: Cents(200_000),
                payment_method: "Cash".to_owned(),
            }
        );
    }

    #[test]
    fn unknown_category_and_account_fall_back_to_their_ids() {
        // v1's GenerateReportUseCaseTest.unknownCategoryFallsBackToId, and the same for accounts.
        let mut unknown = row(4, TransactionType::Transfer, 5_000, 99, Some(8), SEPTEMBER);
        unknown.account_id = AccountId(7);

        let report = report(&[unknown]).unwrap();

        assert_eq!(report.rows()[0].category_name, "#99");
        assert_eq!(report.rows()[0].payment_method, "#7 → #8");
    }

    #[test]
    fn a_right_to_left_language_points_the_arrow_to_the_destination_on_the_left() {
        let report = MonthlyReport::new(
            &september(),
            &v1_rows(),
            &categories(),
            &accounts(),
            TextDirection::RightToLeft,
        )
        .unwrap();

        assert_eq!(report.rows()[2].payment_method, "Cash ← Savings");
        assert_eq!(report.rows()[1].payment_method, "Cash");
    }

    #[test]
    fn rows_at_one_moment_come_by_id_whatever_the_order_given() {
        let rows = [
            row(9, TransactionType::Expense, 100, 1, None, SEPTEMBER + DAY),
            row(4, TransactionType::Expense, 100, 1, None, SEPTEMBER + DAY),
            row(7, TransactionType::Income, 100, 2, None, SEPTEMBER),
            row(5, TransactionType::Expense, 100, 1, None, SEPTEMBER + DAY),
        ];
        let report = report(&rows).unwrap();

        let ids: Vec<i64> = report
            .rows()
            .iter()
            .map(|row| row.transaction_id.0)
            .collect();
        assert_eq!(ids, [7, 4, 5, 9]);
    }

    #[test]
    fn a_malformed_transfer_is_listed_but_counts_in_no_total() {
        let rows = [
            row(1, TransactionType::Income, 2_000, 2, None, SEPTEMBER),
            row(12, TransactionType::Transfer, 4_200, 3, None, SEPTEMBER + 1),
        ];
        let report = report(&rows).unwrap();

        assert_eq!(report.rows().len(), 2);
        assert_eq!(report.rows()[1].transaction_type, TransactionType::Transfer);
        assert_eq!(report.rows()[1].payment_method, "Cash");
        assert_eq!(
            report.totals(),
            Totals {
                income: Cents(2_000),
                expense: Cents::ZERO,
                net: Cents(2_000),
            }
        );
    }

    #[test]
    fn an_expense_whose_row_names_a_destination_shows_it_as_v1_did() {
        let report =
            report(&[row(8, TransactionType::Expense, 500, 1, Some(2), SEPTEMBER)]).unwrap();

        assert_eq!(report.rows()[0].payment_method, "Cash → Savings");
        assert_eq!(report.totals().expense, Cents(500));
        assert_eq!(report.totals().net, Cents(-500));
    }

    #[test]
    fn an_empty_month_has_no_rows_and_zero_totals() {
        let report = report(&[]).unwrap();
        assert_eq!(report.rows(), []);
        assert_eq!(report.totals(), Totals::default());
    }

    #[test]
    fn a_transaction_outside_the_month_is_refused() {
        let rows = [
            row(1, TransactionType::Expense, 100, 1, None, SEPTEMBER),
            row(4, TransactionType::Expense, 5_000, 99, None, SEPTEMBER - 1),
            row(
                5,
                TransactionType::Expense,
                100,
                1,
                None,
                SEPTEMBER + 30 * DAY,
            ),
        ];
        assert_eq!(
            report(&rows),
            Err(ReportError::DateOutsideMonth {
                id: TransactionId(4),
                date: EpochMillis(SEPTEMBER - 1),
            })
        );
        assert_eq!(
            report(&rows[2..]),
            Err(ReportError::DateOutsideMonth {
                id: TransactionId(5),
                date: EpochMillis(SEPTEMBER + 30 * DAY),
            })
        );
    }

    #[test]
    fn totals_past_the_range_of_cents_are_refused() {
        let rows = [
            row(1, TransactionType::Income, i64::MAX, 2, None, SEPTEMBER),
            row(2, TransactionType::Income, 1, 2, None, SEPTEMBER),
        ];
        assert_eq!(report(&rows), Err(ReportError::Overflow));
    }

    #[test]
    fn a_name_without_an_id_matches_no_row() {
        let unsaved = [Category::new("Unsaved")];
        let accounts = [Account {
            id: None,
            name: "Unsaved".to_owned(),
            balance: Cents::ZERO,
        }];
        let report = MonthlyReport::new(
            &september(),
            &v1_rows()[..1],
            &unsaved,
            &accounts,
            TextDirection::LeftToRight,
        )
        .unwrap();

        assert_eq!(report.rows()[0].category_name, "#4");
        assert_eq!(report.rows()[0].payment_method, "#1");
    }

    #[test]
    fn file_names_are_v1s() {
        // v1's ReportFormattingTest.labelsAndFileName.
        let report = report(&[]).unwrap();
        assert_eq!(
            report.file_name(ReportFormat::Pdf),
            "spending-report-2026-09.pdf"
        );
        assert_eq!(
            report.file_name(ReportFormat::Csv),
            "spending-report-2026-09.csv"
        );
    }
}
