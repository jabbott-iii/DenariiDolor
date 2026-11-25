/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The monthly report of Denarii Dolor, its CSV export, and the search screen's filters, built
//! from rows the caller reads.
//!
//! This crate depends on `denarii-core` alone and touches no database (`docs/01-architecture.md`).
//! The caller, the bridge from M4.5, reads a month's transactions and every category and account
//! with `denarii-store`, and passes them here, with the first moment of each of the month's days
//! in the user's time zone, which Dart computes (ADR-0019, ADR-0021). For a search, it turns the
//! screen's input into the filters that `denarii-store` searches with. Its design, and where it
//! differs from v1, the Kotlin app, is in `docs/specs/report.md`.
//!
//! # The parts
//!
//! - The month: [`MonthDays`], which turns a moment into its [`LocalDate`], and gives the
//!   [`MonthBounds`](denarii_core::MonthBounds) a store read takes.
//! - The report: [`MonthlyReport`], its [`ReportRow`]s and [`Totals`](denarii_core::Totals),
//!   with each transfer's arrow in the [`TextDirection`] of the user's language.
//! - The export: [`MonthlyReport::to_csv`] with the [`CsvLabels`] Dart formats,
//!   [`escape_cell`], which guards cells against formulas (CS-20), and
//!   [`MonthlyReport::file_name`] for each [`ReportFormat`].
//! - The search: [`parse_search_input`] turns a [`SearchInput`] into
//!   [`SearchFilters`](denarii_core::SearchFilters).
//! - Errors: [`ReportError`].
//!
//! # Example
//!
//! A month with one expense becomes a CSV file:
//!
//! ```
//! use denarii_core::{
//!     Account, AccountId, Category, CategoryId, Cents, EpochMillis, StoredTransaction,
//!     TransactionId, TransactionType,
//! };
//! use denarii_report::{CsvLabels, MonthDays, MonthlyReport, ReportFormat, TextDirection};
//!
//! # fn main() -> Result<(), Box<dyn std::error::Error>> {
//! // February 2026 in UTC, whose days all last 24 hours.
//! const DAY: i64 = 86_400_000;
//! let start = 1_769_904_000_000;
//! let starts = (0..28).map(|d| EpochMillis(start + d * DAY)).collect();
//! let february = MonthDays::new(2026, 2, starts, EpochMillis(start + 28 * DAY))?;
//!
//! // What a store read returns.
//! let lunch = StoredTransaction {
//!     id: TransactionId(1),
//!     transaction_type: TransactionType::Expense,
//!     description: "Lunch, \"team\"".to_owned(),
//!     amount: Cents(1_250),
//!     category_id: CategoryId(5),
//!     account_id: AccountId(1),
//!     transfer_account_id: None,
//!     date: EpochMillis(start + DAY / 2),
//! };
//! let categories = [Category { id: Some(CategoryId(5)), ..Category::new("Dining") }];
//! let accounts = [Account { id: Some(AccountId(1)), name: "Cash".to_owned(), balance: Cents(0) }];
//!
//! let report =
//!     MonthlyReport::new(&february, &[lunch], &categories, &accounts, TextDirection::LeftToRight)?;
//! let labels = CsvLabels {
//!     title: "Monthly Spending Report".to_owned(),
//!     period: "February 2026".to_owned(),
//!     generated_at: "2026-03-01 09:00 UTC".to_owned(),
//! };
//! let csv = report.to_csv(&labels);
//!
//! assert!(csv.starts_with("title,Monthly Spending Report\r\nperiod,February 2026\r\n"));
//! assert!(csv.ends_with("2026-02-01,EXPENSE,Dining,\"Lunch, \"\"team\"\"\",12.50,Cash\r\n"));
//! assert_eq!(report.file_name(ReportFormat::Csv), "spending-report-2026-02.csv");
//! # Ok(())
//! # }
//! ```
//!
//! # What callers must know
//!
//! - Nothing here reads a clock, a time zone, or a locale. The caller passes each day's first
//!   moment, the labels in the user's language, the text direction, and the locale's
//!   [`NumberSymbols`](denarii_core::NumberSymbols), each from Dart.
//! - [`MonthDays::new`] checks only that the moments can describe the month. Moments from the
//!   wrong zone, or of another month of the same length, give wrong dates, which nothing here
//!   can see.
//! - The CSV text is what v1 wrote, byte for byte, for the same rows and labels: fixture F8,
//!   which v1's own code wrote, checks it. The one difference, in v1's favor on phones, is in the
//!   formula guard after a `;` or a tab, which follows Android's idea of whitespace
//!   ([`escape_cell`]).
//! - No function panics on any input, and no sum wraps: a total past the range of an `i64`
//!   returns [`ReportError::Overflow`].
//! - Every type is plain data (`Send` and `Sync`), every function is synchronous, and nothing
//!   prints or logs.

mod calendar;
mod csv;
mod error;
mod monthly;
mod search;

pub use calendar::{LocalDate, MonthDays};
pub use csv::{CsvLabels, ReportFormat, escape_cell};
pub use error::ReportError;
pub use monthly::{MonthlyReport, ReportRow, TextDirection};
pub use search::{SearchInput, parse_search_input};
