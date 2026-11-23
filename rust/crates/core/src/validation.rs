/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The checks every write runs before it reaches the database.
//!
//! [`validate_transaction`] turns a [`TransactionDraft`], which is what a form or the bridge
//! sends, into a [`Transaction`], so code after it never checks the fields again. It applies v1's
//! field rules (`ValidateTransactionUseCase` and `Validators` in v1) in v1's order, so the user
//! sees the same error first. The checks that need stored data (that the category and accounts
//! exist, and the monthly budget) belong to the storage layer, which lands in M2.4;
//! [`budget_overage`] is the arithmetic part of the budget check.
//!
//! Lengths are UTF-16 code units and whitespace is Kotlin's, as `crate::text` explains, so text
//! v1 accepted is accepted here.

use crate::error::{ArithmeticOverflow, BudgetOverage, DomainError};
use crate::ids::{AccountId, CategoryId, EpochMillis, TransactionId};
use crate::money::Cents;
use crate::search::SearchFilters;
use crate::text;
use crate::transaction::{Transaction, TransactionKind, TransactionType};

/// The longest category or account name, in UTF-16 code units, once trimmed.
pub const MAX_NAME_UNITS: usize = 50;

/// The longest transaction description, in UTF-16 code units, once trimmed.
pub const MAX_DESCRIPTION_UNITS: usize = 200;

/// The highest budget warning threshold, in percent.
pub const MAX_PERCENT: i32 = 100;

/// A transaction as the user entered it, before validation.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct TransactionDraft {
    /// The stored row being edited, or `None` for a new transaction.
    pub id: Option<TransactionId>,
    /// Expense, income, or transfer.
    pub transaction_type: TransactionType,
    /// What the transaction was for, untrimmed.
    pub description: String,
    /// The amount.
    pub amount: Cents,
    /// The chosen category.
    pub category_id: CategoryId,
    /// The account it is paid from, received into, or moved from.
    pub account_id: AccountId,
    /// The destination of a transfer; ignored for an expense or income.
    pub transfer_account_id: Option<AccountId>,
    /// When it happened.
    pub date: EpochMillis,
}

/// Checks a draft's fields and returns the transaction it describes, with its description
/// trimmed as v1 stored it (BUG-08).
///
/// # Errors
///
/// The first rule the draft breaks, checked in this order:
///
/// 1. [`DomainError::InvalidAmount`] unless the amount is more than zero.
/// 2. [`DomainError::BlankDescription`] for an empty or all-whitespace description.
/// 3. [`DomainError::DescriptionTooLong`] past [`MAX_DESCRIPTION_UNITS`] once trimmed.
/// 4. [`DomainError::InvalidDate`] unless the date is after 1970-01-01T00:00:00Z.
/// 5. [`DomainError::InvalidAccount`] unless the account ID is more than zero.
/// 6. [`DomainError::InvalidCategory`] unless the category ID is more than zero.
/// 7. [`DomainError::TransferDestinationRequired`] for a transfer with no destination.
/// 8. [`DomainError::TransferDestinationSame`] for a transfer to its own account.
///
/// # Examples
///
/// ```
/// use denarii_core::{
///     AccountId, CategoryId, Cents, DomainError, EpochMillis, TransactionDraft, TransactionType,
///     validate_transaction,
/// };
///
/// let draft = TransactionDraft {
///     id: None,
///     transaction_type: TransactionType::Transfer,
///     description: "  Savings  ".to_owned(),
///     amount: Cents(10_000),
///     category_id: CategoryId(3),
///     account_id: AccountId(1),
///     transfer_account_id: Some(AccountId(1)),
///     date: EpochMillis(1_764_000_000_000),
/// };
/// assert_eq!(validate_transaction(&draft), Err(DomainError::TransferDestinationSame));
/// ```
pub fn validate_transaction(draft: &TransactionDraft) -> Result<Transaction, DomainError> {
    if !is_valid_amount(draft.amount) {
        return Err(DomainError::InvalidAmount);
    }
    if text::is_blank(&draft.description) {
        return Err(DomainError::BlankDescription);
    }
    if !is_valid_description(&draft.description) {
        return Err(DomainError::DescriptionTooLong {
            max_units: MAX_DESCRIPTION_UNITS,
        });
    }
    if !is_valid_date(draft.date) {
        return Err(DomainError::InvalidDate);
    }
    if draft.account_id.0 <= 0 {
        return Err(DomainError::InvalidAccount);
    }
    if draft.category_id.0 <= 0 {
        return Err(DomainError::InvalidCategory);
    }
    let kind = match draft.transaction_type {
        TransactionType::Expense => TransactionKind::Expense,
        TransactionType::Income => TransactionKind::Income,
        TransactionType::Transfer => {
            let to_account = draft
                .transfer_account_id
                .ok_or(DomainError::TransferDestinationRequired)?;
            if to_account == draft.account_id {
                return Err(DomainError::TransferDestinationSame);
            }
            TransactionKind::Transfer { to_account }
        }
    };
    Ok(Transaction {
        id: draft.id,
        description: text::trim(&draft.description).to_owned(),
        amount: draft.amount,
        category_id: draft.category_id,
        account_id: draft.account_id,
        date: draft.date,
        kind,
    })
}

/// How far an expense of `amount` takes its category past `monthly_limit`, given what was
/// already spent in the category that month, or `None` when it stays within the limit.
///
/// `spent_this_month` must leave out the expense being edited, as v1's query did.
///
/// # Errors
///
/// Returns [`ArithmeticOverflow`] when the sum leaves the range of [`Cents`].
pub fn budget_overage(
    category_name: &str,
    spent_this_month: Cents,
    amount: Cents,
    monthly_limit: Cents,
) -> Result<Option<BudgetOverage>, ArithmeticOverflow> {
    let over_by = spent_this_month
        .checked_add(amount)?
        .checked_sub(monthly_limit)?;
    Ok(over_by.is_positive().then(|| BudgetOverage {
        category_name: category_name.to_owned(),
        over_by,
        limit: monthly_limit,
    }))
}

/// Whether an amount can be stored on a transaction: it must be more than zero.
#[must_use]
pub const fn is_valid_amount(amount: Cents) -> bool {
    amount.is_positive()
}

/// Whether a description has 1 to [`MAX_DESCRIPTION_UNITS`] UTF-16 code units once trimmed.
#[must_use]
pub fn is_valid_description(description: &str) -> bool {
    (1..=MAX_DESCRIPTION_UNITS).contains(&text::utf16_len(text::trim(description)))
}

/// Whether a date is after 1970-01-01T00:00:00Z.
#[must_use]
pub const fn is_valid_date(date: EpochMillis) -> bool {
    date.0 > 0
}

/// Whether a category or account name has 1 to [`MAX_NAME_UNITS`] UTF-16 code units once
/// trimmed.
#[must_use]
pub fn is_valid_name(name: &str) -> bool {
    (1..=MAX_NAME_UNITS).contains(&text::utf16_len(text::trim(name)))
}

/// Whether a budget warning threshold is from 1 to [`MAX_PERCENT`] percent.
#[must_use]
pub fn is_valid_percent(percent: i32) -> bool {
    (1..=MAX_PERCENT).contains(&percent)
}

/// Whether a search's minimum is at most its maximum and its start at most its end, where both
/// bounds are set.
#[must_use]
pub fn is_valid_search_range(filters: &SearchFilters) -> bool {
    let amounts_ok = match (filters.min_amount, filters.max_amount) {
        (Some(min), Some(max)) => min <= max,
        _ => true,
    };
    let dates_ok = match (filters.start_date, filters.end_date) {
        (Some(start), Some(end)) => start <= end,
        _ => true,
    };
    amounts_ok && dates_ok
}
