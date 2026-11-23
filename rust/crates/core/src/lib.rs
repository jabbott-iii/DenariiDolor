/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The domain rules of Denarii Dolor, an offline personal-finance ledger: money in cents,
//! transactions, the balance changes they cause, and the checks they must pass.
//!
//! This crate uses only `std` and depends on no other crate of the workspace. The planned storage,
//! vault, backup, and report crates build on it, and the Flutter app reaches it through the
//! bridge crate, `denarii-bridge` (`docs/01-architecture.md`). Its design, and where it differs
//! from v1's Kotlin code, is in `docs/specs/core.md`.
//!
//! # The parts
//!
//! - Money: [`Cents`] holds every amount; [`parse_cents`] reads what a user typed, with the
//!   locale's [`NumberSymbols`]; [`Cents::to_plain`] and [`Cents::to_input`] write the plain forms.
//! - Transactions: [`Transaction`], its [`TransactionKind`], and the stored [`TransactionType`].
//! - Balances: [`balance_deltas`] gives the change to each account's stored balance when a
//!   transaction is added, edited, or deleted.
//! - Validation: [`validate_transaction`] turns a [`TransactionDraft`] into a [`Transaction`];
//!   [`budget_overage`] does the budget check's arithmetic; the `is_valid_*` functions check names,
//!   percentages, and search ranges.
//! - Reference data: [`Account`], [`Category`], [`Budget`], and the seeded rows in [`defaults`].
//! - IDs: [`AccountId`], [`CategoryId`], [`TransactionId`], [`BudgetId`], and [`EpochMillis`].
//! - Errors: [`DomainError`] names a broken business rule, [`MoneyParseError`] a rejected amount,
//!   and [`ArithmeticOverflow`] a sum out of range.
//!
//! # Example
//!
//! A form's input becomes a validated transaction, and its balance changes:
//!
//! ```
//! use std::collections::BTreeMap;
//! use denarii_core::{
//!     AccountId, CategoryId, Cents, EpochMillis, NumberSymbols, TransactionDraft,
//!     TransactionType, balance_deltas, parse_cents, validate_transaction,
//! };
//!
//! # fn main() -> Result<(), Box<dyn std::error::Error>> {
//! let english = NumberSymbols {
//!     decimal_separator: '.',
//!     currency_marks: vec!["$".to_owned(), "$".to_owned(), "USD".to_owned()],
//! };
//! let draft = TransactionDraft {
//!     id: None,
//!     transaction_type: TransactionType::Transfer,
//!     description: "Savings".to_owned(),
//!     amount: parse_cents("$1,000", &english)?,
//!     category_id: CategoryId(3),
//!     account_id: AccountId(1),
//!     transfer_account_id: Some(AccountId(2)),
//!     date: EpochMillis(1_764_000_000_000),
//! };
//! let transfer = validate_transaction(&draft)?;
//! let deltas = balance_deltas(None, Some(&transfer))?;
//! assert_eq!(deltas, BTreeMap::from([(AccountId(1), Cents(-100_000)), (AccountId(2), Cents(100_000))]));
//! # Ok(())
//! # }
//! ```
//!
//! # What callers must know
//!
//! - No function panics on any input, and no arithmetic wraps: sums that leave the range of an
//!   `i64` return [`ArithmeticOverflow`].
//! - Text lengths are counted in UTF-16 code units and whitespace is Kotlin's, as v1 counted them,
//!   so a limit such as [`MAX_DESCRIPTION_UNITS`] means what it meant in v1.
//! - Nothing here reads a clock or a time zone. Callers pass dates as [`EpochMillis`].
//! - Every type is plain data (`Send` and `Sync`), and every function is synchronous.

mod error;
mod ids;
mod ledger;
mod money;
mod reference;
mod search;
mod text;
mod transaction;
mod validation;

pub mod defaults;

pub use error::{ArithmeticOverflow, BudgetOverage, DomainError};
pub use ids::{AccountId, BudgetId, CategoryId, EpochMillis, TransactionId};
pub use ledger::balance_deltas;
pub use money::{
    Cents, MAX_WHOLE_DIGITS, MoneyParseError, NumberSymbols, normalize_digits, parse_cents,
};
pub use reference::{Account, Budget, Category};
pub use search::SearchFilters;
pub use transaction::{Transaction, TransactionKind, TransactionType, UnknownTransactionType};
pub use validation::{
    MAX_DESCRIPTION_UNITS, MAX_NAME_UNITS, MAX_PERCENT, TransactionDraft, budget_overage,
    is_valid_amount, is_valid_date, is_valid_description, is_valid_name, is_valid_percent,
    is_valid_search_range, validate_transaction,
};
