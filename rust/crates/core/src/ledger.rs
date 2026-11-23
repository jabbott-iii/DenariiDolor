/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! How a write changes the balances stored on each account.
//!
//! The database stores each account's balance rather than adding up its transactions on every
//! read. So every write that adds, edits, or deletes a transaction must change the stored
//! balances by exactly [`balance_deltas`], in the same database transaction as the row, or the
//! two drift apart.

use std::collections::BTreeMap;

use crate::error::ArithmeticOverflow;
use crate::ids::AccountId;
use crate::money::Cents;
use crate::transaction::{Transaction, add_to};

/// The change to each account's stored balance when `previous` becomes `current`.
///
/// Pass `None` for `previous` when adding a transaction, `None` for `current` when deleting one,
/// and both when editing. Accounts whose balance doesn't change are left out, so an edit that
/// changes only the description gives an empty map. The map is ordered by account ID.
///
/// # Errors
///
/// Returns [`ArithmeticOverflow`] when a sum or a negation leaves the range of [`Cents`], which
/// only unvalidated stored data can cause.
///
/// # Examples
///
/// ```
/// use std::collections::BTreeMap;
/// use denarii_core::{
///     AccountId, CategoryId, Cents, EpochMillis, Transaction, TransactionKind, balance_deltas,
/// };
///
/// # fn main() -> Result<(), Box<dyn std::error::Error>> {
/// let lunch = Transaction {
///     id: None,
///     description: "Lunch".to_owned(),
///     amount: Cents(1_250),
///     category_id: CategoryId(1),
///     account_id: AccountId(1),
///     date: EpochMillis(1_764_000_000_000),
///     kind: TransactionKind::Expense,
/// };
/// let added = balance_deltas(None, Some(&lunch))?;
/// assert_eq!(added, BTreeMap::from([(AccountId(1), Cents(-1_250))]));
///
/// let moved = Transaction { account_id: AccountId(2), ..lunch.clone() };
/// let edited = balance_deltas(Some(&lunch), Some(&moved))?;
/// assert_eq!(edited, BTreeMap::from([(AccountId(1), Cents(1_250)), (AccountId(2), Cents(-1_250))]));
/// # Ok(())
/// # }
/// ```
pub fn balance_deltas(
    previous: Option<&Transaction>,
    current: Option<&Transaction>,
) -> Result<BTreeMap<AccountId, Cents>, ArithmeticOverflow> {
    let mut deltas = BTreeMap::new();
    if let Some(previous) = previous {
        for (account, impact) in previous.account_impacts()? {
            add_to(&mut deltas, account, impact.checked_neg()?)?;
        }
    }
    if let Some(current) = current {
        for (account, impact) in current.account_impacts()? {
            add_to(&mut deltas, account, impact)?;
        }
    }
    deltas.retain(|_, delta| *delta != Cents::ZERO);
    Ok(deltas)
}
