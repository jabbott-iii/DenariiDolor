/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Transactions: expenses, income, and transfers, and how each one changes account balances.
//!
//! v1 modeled the three kinds as subclasses of an abstract `Transaction`, each overriding
//! `balanceImpact()` and `accountImpacts()`, so that no total or balance branched on the type.
//! Here [`TransactionKind`] is an enum, and [`Transaction::balance_impact`] and
//! [`Transaction::account_impacts`] are the only places that match on it. Only a transfer carries
//! a destination account, so a transfer without one, or an expense with one, can't be built.

use std::collections::BTreeMap;
use std::fmt;

use crate::error::ArithmeticOverflow;
use crate::ids::{AccountId, CategoryId, EpochMillis, TransactionId};
use crate::money::Cents;
use crate::text;

/// The kind of a transaction, as the database's `type` column stores it.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub enum TransactionType {
    /// Money spent from an account.
    Expense,
    /// Money received into an account.
    Income,
    /// Money moved from one account to another.
    Transfer,
}

impl TransactionType {
    /// Every type, in v1's order.
    pub const ALL: [Self; 3] = [Self::Expense, Self::Income, Self::Transfer];

    /// The name stored in the database and written to CSV files: `EXPENSE`, `INCOME`, or
    /// `TRANSFER`.
    #[must_use]
    pub const fn stored_name(self) -> &'static str {
        match self {
            Self::Expense => "EXPENSE",
            Self::Income => "INCOME",
            Self::Transfer => "TRANSFER",
        }
    }

    /// Reads a stored name, ignoring surrounding whitespace and ASCII case, as v1's
    /// `TransactionType.parse` did.
    ///
    /// # Errors
    ///
    /// Returns [`UnknownTransactionType`] for any other text.
    pub fn parse(value: &str) -> Result<Self, UnknownTransactionType> {
        let name = text::trim(value);
        Self::ALL
            .into_iter()
            .find(|kind| kind.stored_name().eq_ignore_ascii_case(name))
            .ok_or_else(|| UnknownTransactionType(value.to_owned()))
    }
}

impl fmt::Display for TransactionType {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(self.stored_name())
    }
}

/// [`TransactionType::parse`] got a name that isn't a transaction type.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct UnknownTransactionType(pub String);

impl fmt::Display for UnknownTransactionType {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "unknown transaction type {:?}", self.0)
    }
}

impl std::error::Error for UnknownTransactionType {}

/// What a transaction does, with the data only that kind has.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum TransactionKind {
    /// Money spent from the transaction's account.
    Expense,
    /// Money received into the transaction's account.
    Income,
    /// Money moved from the transaction's account to `to_account`.
    Transfer {
        /// The destination account.
        to_account: AccountId,
    },
}

impl TransactionKind {
    /// The kind of a stored row, from its `type` and `transfer_account_id` columns.
    ///
    /// Returns `None` for a transfer with no destination. Validation never writes one, but the
    /// schema allows it, so a read treats such a row as malformed and gives it no balance impact
    /// instead of failing (BUG-06). An expense's or income's destination column is ignored, as v1
    /// ignored it.
    #[must_use]
    pub const fn from_stored(
        transaction_type: TransactionType,
        transfer_account_id: Option<AccountId>,
    ) -> Option<Self> {
        match (transaction_type, transfer_account_id) {
            (TransactionType::Expense, _) => Some(Self::Expense),
            (TransactionType::Income, _) => Some(Self::Income),
            (TransactionType::Transfer, Some(to_account)) => Some(Self::Transfer { to_account }),
            (TransactionType::Transfer, None) => None,
        }
    }

    /// The type stored for this kind.
    #[must_use]
    pub const fn transaction_type(self) -> TransactionType {
        match self {
            Self::Expense => TransactionType::Expense,
            Self::Income => TransactionType::Income,
            Self::Transfer { .. } => TransactionType::Transfer,
        }
    }

    /// The destination account of a transfer, or `None` for an expense or income.
    #[must_use]
    pub const fn transfer_account(self) -> Option<AccountId> {
        match self {
            Self::Transfer { to_account } => Some(to_account),
            Self::Expense | Self::Income => None,
        }
    }
}

/// A stored or validated transaction.
///
/// [`validate_transaction`](crate::validate_transaction) builds one from user input; the storage
/// layer builds one from a row with [`TransactionKind::from_stored`].
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Transaction {
    /// The row's ID, or `None` before the transaction is stored.
    pub id: Option<TransactionId>,
    /// What the transaction was for.
    pub description: String,
    /// The amount, which validation requires to be more than zero.
    pub amount: Cents,
    /// The category it belongs to.
    pub category_id: CategoryId,
    /// The account it is paid from, received into, or moved from.
    pub account_id: AccountId,
    /// When it happened.
    pub date: EpochMillis,
    /// Whether it is an expense, income, or a transfer.
    pub kind: TransactionKind,
}

impl Transaction {
    /// The type stored for this transaction.
    #[must_use]
    pub const fn transaction_type(&self) -> TransactionType {
        self.kind.transaction_type()
    }

    /// The transaction's net effect on the user's total money: minus the amount for an expense,
    /// the amount for income, and zero for a transfer.
    ///
    /// # Errors
    ///
    /// Returns [`ArithmeticOverflow`] for an expense of `i64::MIN` cents.
    pub fn balance_impact(&self) -> Result<Cents, ArithmeticOverflow> {
        match self.kind {
            TransactionKind::Expense => self.amount.checked_neg(),
            TransactionKind::Income => Ok(self.amount),
            TransactionKind::Transfer { .. } => Ok(Cents::ZERO),
        }
    }

    /// The transaction's effect on each account's balance.
    ///
    /// A transfer takes the amount from its account and adds it to its destination. A transfer
    /// to its own account has no effect; v1 gave it the amount instead, but validation never let
    /// one be stored (`docs/specs/core.md`, "Differences from v1").
    ///
    /// # Errors
    ///
    /// Returns [`ArithmeticOverflow`] when an amount can't be negated.
    pub fn account_impacts(&self) -> Result<BTreeMap<AccountId, Cents>, ArithmeticOverflow> {
        let mut impacts = BTreeMap::new();
        match self.kind {
            TransactionKind::Expense | TransactionKind::Income => {
                impacts.insert(self.account_id, self.balance_impact()?);
            }
            TransactionKind::Transfer { to_account } => {
                add_to(&mut impacts, self.account_id, self.amount.checked_neg()?)?;
                add_to(&mut impacts, to_account, self.amount)?;
            }
        }
        Ok(impacts)
    }
}

/// Adds `amount` to `account`'s entry in `totals`, starting from zero.
pub(crate) fn add_to(
    totals: &mut BTreeMap<AccountId, Cents>,
    account: AccountId,
    amount: Cents,
) -> Result<(), ArithmeticOverflow> {
    let total = totals.entry(account).or_insert(Cents::ZERO);
    *total = total.checked_add(amount)?;
    Ok(())
}
