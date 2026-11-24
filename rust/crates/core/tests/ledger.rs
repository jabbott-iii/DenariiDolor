/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Transactions and the balance changes they cause. Every case of v1's `LedgerTest.kt` and
//! `TransactionModelTest.kt` (at commit 6d57526) is here with v1's expected result.

use std::collections::BTreeMap;

use denarii_core::{
    AccountId, CategoryId, Cents, EpochMillis, Transaction, TransactionId, TransactionKind,
    TransactionType, balance_deltas,
};

fn transaction(kind: TransactionKind, amount: i64, account: i64) -> Transaction {
    Transaction {
        id: Some(TransactionId(1)),
        description: "Food".to_owned(),
        amount: Cents(amount),
        category_id: CategoryId(1),
        account_id: AccountId(account),
        date: EpochMillis(1),
        kind,
    }
}

fn expense() -> Transaction {
    transaction(TransactionKind::Expense, 4_500, 1)
}

fn transfer() -> Transaction {
    let to_account = AccountId(2);
    transaction(TransactionKind::Transfer { to_account }, 10_000, 1)
}

fn deltas(pairs: &[(i64, i64)]) -> BTreeMap<AccountId, Cents> {
    pairs
        .iter()
        .map(|&(account, delta)| (AccountId(account), Cents(delta)))
        .collect()
}

#[test]
fn each_kind_reports_its_balance_impact() {
    let income = transaction(TransactionKind::Income, 125_000, 1);
    assert_eq!(expense().balance_impact(), Ok(Cents(-4_500)));
    assert_eq!(income.balance_impact(), Ok(Cents(125_000)));
    assert_eq!(transfer().balance_impact(), Ok(Cents::ZERO));
}

#[test]
fn each_kind_reports_its_impact_on_each_account() {
    let income = transaction(TransactionKind::Income, 125_000, 1);
    assert_eq!(expense().account_impacts(), Ok(deltas(&[(1, -4_500)])));
    assert_eq!(income.account_impacts(), Ok(deltas(&[(1, 125_000)])));
    assert_eq!(
        transfer().account_impacts(),
        Ok(deltas(&[(1, -10_000), (2, 10_000)]))
    );
}

/// Only expenses count against a budget, as in v1's `ValidateTransactionUseCase`.
#[test]
fn only_an_expense_counts_against_its_categorys_budget() {
    let income = transaction(TransactionKind::Income, 125_000, 1);
    assert_eq!(expense().budgeted_amount(), Some(Cents(4_500)));
    assert_eq!(income.budgeted_amount(), None);
    assert_eq!(transfer().budgeted_amount(), None);
}

#[test]
fn each_kind_reports_its_type() {
    let income = transaction(TransactionKind::Income, 1, 1);
    assert_eq!(expense().transaction_type(), TransactionType::Expense);
    assert_eq!(income.transaction_type(), TransactionType::Income);
    assert_eq!(transfer().transaction_type(), TransactionType::Transfer);
}

#[test]
fn type_parsing_ignores_case_and_whitespace_and_rejects_other_names() {
    assert_eq!(
        TransactionType::parse(" income "),
        Ok(TransactionType::Income)
    );
    assert_eq!(
        TransactionType::parse("Transfer"),
        Ok(TransactionType::Transfer)
    );
    assert!(TransactionType::parse("REFUND").is_err());
    assert!(TransactionType::parse("").is_err());
    for kind in TransactionType::ALL {
        assert_eq!(TransactionType::parse(kind.stored_name()), Ok(kind));
    }
}

#[test]
fn a_stored_transfer_without_a_destination_is_malformed() {
    let destination = Some(AccountId(2));
    assert_eq!(
        TransactionKind::from_stored(TransactionType::Transfer, None),
        None
    );
    assert_eq!(
        TransactionKind::from_stored(TransactionType::Transfer, destination),
        Some(TransactionKind::Transfer {
            to_account: AccountId(2)
        })
    );
    assert_eq!(
        TransactionKind::from_stored(TransactionType::Expense, destination),
        Some(TransactionKind::Expense)
    );
}

#[test]
fn add_applies_the_impact() {
    assert_eq!(
        balance_deltas(None, Some(&expense())),
        Ok(deltas(&[(1, -4_500)]))
    );
}

#[test]
fn delete_reverses_the_impact() {
    assert_eq!(
        balance_deltas(Some(&expense()), None),
        Ok(deltas(&[(1, 4_500)]))
    );
}

#[test]
fn editing_the_amount_applies_only_the_difference() {
    let edited = Transaction {
        amount: Cents(5_000),
        ..expense()
    };
    assert_eq!(
        balance_deltas(Some(&expense()), Some(&edited)),
        Ok(deltas(&[(1, -500)]))
    );
}

#[test]
fn editing_the_account_moves_the_impact_between_accounts() {
    let edited = Transaction {
        account_id: AccountId(2),
        ..expense()
    };
    assert_eq!(
        balance_deltas(Some(&expense()), Some(&edited)),
        Ok(deltas(&[(1, 4_500), (2, -4_500)]))
    );
}

#[test]
fn an_unchanged_edit_produces_no_deltas() {
    let edited = Transaction {
        description: "Lunch".to_owned(),
        ..expense()
    };
    assert_eq!(
        balance_deltas(Some(&expense()), Some(&edited)),
        Ok(BTreeMap::new())
    );
}

#[test]
fn changing_an_expense_to_income_doubles_the_swing() {
    let income = Transaction {
        description: "Refund".to_owned(),
        category_id: CategoryId(2),
        kind: TransactionKind::Income,
        ..expense()
    };
    assert_eq!(
        balance_deltas(Some(&expense()), Some(&income)),
        Ok(deltas(&[(1, 9_000)]))
    );
}

#[test]
fn a_transfer_moves_money_between_accounts() {
    assert_eq!(
        balance_deltas(None, Some(&transfer())),
        Ok(deltas(&[(1, -10_000), (2, 10_000)]))
    );
    assert_eq!(
        balance_deltas(Some(&transfer()), None),
        Ok(deltas(&[(1, 10_000), (2, -10_000)]))
    );
}

#[test]
fn nothing_changes_for_no_transaction() {
    assert_eq!(balance_deltas(None, None), Ok(BTreeMap::new()));
}

#[test]
fn a_transfer_to_its_own_account_changes_nothing() {
    let to_self = transaction(
        TransactionKind::Transfer {
            to_account: AccountId(1),
        },
        10_000,
        1,
    );
    assert_eq!(to_self.account_impacts(), Ok(deltas(&[(1, 0)])));
    assert_eq!(balance_deltas(None, Some(&to_self)), Ok(BTreeMap::new()));
}

#[test]
fn cents_avoid_floating_point_drift() {
    let ten_cents = Transaction {
        amount: Cents(10),
        ..expense()
    };
    let mut total = Cents::ZERO;
    for _ in 0..3 {
        let delta = balance_deltas(None, Some(&ten_cents)).unwrap()[&AccountId(1)];
        total = total.checked_add(delta).unwrap();
    }
    assert_eq!(total, Cents(-30));
}

#[test]
fn out_of_range_amounts_report_overflow_instead_of_wrapping() {
    let smallest = transaction(TransactionKind::Expense, i64::MIN, 1);
    assert!(smallest.balance_impact().is_err());
    assert!(balance_deltas(None, Some(&smallest)).is_err());
    let largest = transaction(TransactionKind::Income, i64::MAX, 1);
    let one_cent = transaction(TransactionKind::Expense, 1, 1);
    assert!(balance_deltas(Some(&one_cent), Some(&largest)).is_err());
    assert_eq!(
        balance_deltas(Some(&largest), Some(&largest)),
        Ok(BTreeMap::new())
    );
}
