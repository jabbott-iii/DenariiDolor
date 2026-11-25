/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The errors the domain rules return: a broken business rule ([`DomainError`]) and arithmetic
//! that left the range of a cents value ([`ArithmeticOverflow`]).
//!
//! The user interface shows a [`DomainError`] through its [`code`](DomainError::code), which
//! names a localized message, never through its `Display` text, which is for tests and
//! debugging.

use std::fmt;

use crate::money::Cents;

/// Arithmetic on amounts went past the range of an `i64` number of cents.
///
/// No amount a user can type causes it (see [`Cents::MAX_PARSED`]); stored or restored data that
/// was never validated could.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct ArithmeticOverflow;

impl fmt::Display for ArithmeticOverflow {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("an amount went past the range of a 64-bit number of cents")
    }
}

impl std::error::Error for ArithmeticOverflow {}

/// How far an expense would take its category past the monthly budget.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct BudgetOverage {
    /// The category's name, which the message shows.
    pub category_name: String,
    /// How much the month's expenses in the category would exceed the limit.
    pub over_by: Cents,
    /// The category's monthly limit.
    pub limit: Cents,
}

/// A business rule that a request broke.
///
/// The variants are v1's `DomainError` values, in the same order, and [`code`](Self::code)
/// returns v1's name for each, so the localized messages keep their keys.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
#[non_exhaustive]
pub enum DomainError {
    /// An update or delete named no stored row.
    IdRequired,
    /// The amount isn't more than zero.
    InvalidAmount,
    /// The description is empty or only whitespace.
    BlankDescription,
    /// The description is longer than `max_units` UTF-16 code units once trimmed.
    DescriptionTooLong {
        /// The limit, which the message shows.
        max_units: usize,
    },
    /// The date isn't after the start of 1970 (UTC).
    InvalidDate,
    /// The account ID isn't a stored row's ID.
    InvalidAccount,
    /// The category ID isn't a stored row's ID.
    InvalidCategory,
    /// A transfer names no destination account.
    TransferDestinationRequired,
    /// A transfer's destination is its source account.
    TransferDestinationSame,
    /// The category doesn't exist.
    CategoryNotFound,
    /// The account doesn't exist.
    AccountNotFound,
    /// The transfer's destination account doesn't exist.
    TransferDestinationNotFound,
    /// The expense would take its category past the monthly budget, and the user hasn't
    /// confirmed it.
    BudgetExceeded(BudgetOverage),
    /// A category or account name is empty, or longer than `max_units` UTF-16 code units once
    /// trimmed.
    InvalidName {
        /// The limit, which the message shows.
        max_units: usize,
    },
    /// Another category already has this name, compared as v1 compared it: SQLite's `LOWER`,
    /// which folds only the ASCII letters.
    DuplicateCategoryName {
        /// The trimmed name the request gave, which the message shows.
        name: String,
    },
    /// Another account already has this name, compared as for
    /// [`DuplicateCategoryName`](Self::DuplicateCategoryName).
    DuplicateAccountName {
        /// The trimmed name the request gave, which the message shows.
        name: String,
    },
    /// The category is one of the seeded defaults, which can't be deleted.
    DefaultCategory,
    /// The account is one of the seeded defaults, which can't be deleted.
    DefaultAccount,
    /// Transactions still refer to the category. A budget doesn't count: deleting the category
    /// deletes its budget too.
    CategoryInUse {
        /// How many transactions refer to it, which the message shows.
        transactions: u32,
    },
    /// Transactions still refer to the account, as the account they use or as a transfer's
    /// destination.
    AccountInUse {
        /// How many transactions refer to it, which the message shows.
        transactions: u32,
    },
    /// A budget's monthly limit isn't more than zero.
    InvalidBudgetLimit,
    /// A budget's warning threshold isn't between 1 and 100 percent.
    InvalidWarningPercent,
    /// A search's minimum is above its maximum, or its start date is after its end date.
    InvalidSearchRange,
}

impl DomainError {
    /// v1's name for the error, such as `BUDGET_EXCEEDED`, which names its localized message.
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::IdRequired => "ID_REQUIRED",
            Self::InvalidAmount => "INVALID_AMOUNT",
            Self::BlankDescription => "BLANK_DESCRIPTION",
            Self::DescriptionTooLong { .. } => "DESCRIPTION_TOO_LONG",
            Self::InvalidDate => "INVALID_DATE",
            Self::InvalidAccount => "INVALID_ACCOUNT",
            Self::InvalidCategory => "INVALID_CATEGORY",
            Self::TransferDestinationRequired => "TRANSFER_DESTINATION_REQUIRED",
            Self::TransferDestinationSame => "TRANSFER_DESTINATION_SAME",
            Self::CategoryNotFound => "CATEGORY_NOT_FOUND",
            Self::AccountNotFound => "ACCOUNT_NOT_FOUND",
            Self::TransferDestinationNotFound => "TRANSFER_DESTINATION_NOT_FOUND",
            Self::BudgetExceeded(_) => "BUDGET_EXCEEDED",
            Self::InvalidName { .. } => "INVALID_NAME",
            Self::DuplicateCategoryName { .. } => "DUPLICATE_CATEGORY_NAME",
            Self::DuplicateAccountName { .. } => "DUPLICATE_ACCOUNT_NAME",
            Self::DefaultCategory => "DEFAULT_CATEGORY",
            Self::DefaultAccount => "DEFAULT_ACCOUNT",
            Self::CategoryInUse { .. } => "CATEGORY_IN_USE",
            Self::AccountInUse { .. } => "ACCOUNT_IN_USE",
            Self::InvalidBudgetLimit => "INVALID_BUDGET_LIMIT",
            Self::InvalidWarningPercent => "INVALID_WARNING_PERCENT",
            Self::InvalidSearchRange => "INVALID_SEARCH_RANGE",
        }
    }
}

impl fmt::Display for DomainError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::IdRequired => f.write_str("the request names no stored row"),
            Self::InvalidAmount => f.write_str("the amount must be more than zero"),
            Self::BlankDescription => f.write_str("the description is blank"),
            Self::DescriptionTooLong { max_units } => {
                write!(f, "the description is longer than {max_units} characters")
            }
            Self::InvalidDate => f.write_str("the date is invalid"),
            Self::InvalidAccount => f.write_str("the account is invalid"),
            Self::InvalidCategory => f.write_str("the category is invalid"),
            Self::TransferDestinationRequired => f.write_str("the transfer has no destination"),
            Self::TransferDestinationSame => {
                f.write_str("the transfer's destination is its source account")
            }
            Self::CategoryNotFound => f.write_str("the category doesn't exist"),
            Self::AccountNotFound => f.write_str("the account doesn't exist"),
            Self::TransferDestinationNotFound => {
                f.write_str("the transfer's destination account doesn't exist")
            }
            Self::BudgetExceeded(overage) => write!(
                f,
                "the expense goes {} cents past the monthly budget of {}",
                overage.over_by.0, overage.category_name
            ),
            Self::InvalidName { max_units } => {
                write!(f, "the name is empty or longer than {max_units} characters")
            }
            Self::DuplicateCategoryName { name } => {
                write!(f, "another category is named \"{name}\"")
            }
            Self::DuplicateAccountName { name } => write!(f, "another account is named \"{name}\""),
            Self::DefaultCategory => f.write_str("a default category can't be deleted"),
            Self::DefaultAccount => f.write_str("a default account can't be deleted"),
            Self::CategoryInUse { transactions } => {
                write!(f, "{transactions} transactions still use the category")
            }
            Self::AccountInUse { transactions } => {
                write!(f, "{transactions} transactions still use the account")
            }
            Self::InvalidBudgetLimit => f.write_str("the monthly limit must be more than zero"),
            Self::InvalidWarningPercent => {
                f.write_str("the warning threshold must be from 1 to 100 percent")
            }
            Self::InvalidSearchRange => f.write_str("the search range is inverted"),
        }
    }
}

impl std::error::Error for DomainError {}
