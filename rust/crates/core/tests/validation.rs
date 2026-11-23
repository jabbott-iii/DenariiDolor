/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Validation. Every case of v1's `ValidatorsTest.kt` (at commit 6d57526) is here with v1's
//! expected result, plus the order and the field rules of v1's `ValidateTransactionUseCase`.

use denarii_core::{
    AccountId, BudgetOverage, CategoryId, Cents, DomainError, EpochMillis, MAX_DESCRIPTION_UNITS,
    MAX_NAME_UNITS, SearchFilters, TransactionDraft, TransactionKind, TransactionType,
    budget_overage, defaults, is_valid_amount, is_valid_description, is_valid_name,
    is_valid_percent, is_valid_search_range, validate_transaction,
};

fn draft() -> TransactionDraft {
    TransactionDraft {
        id: None,
        transaction_type: TransactionType::Expense,
        description: "Rent".to_owned(),
        amount: Cents(120_000),
        category_id: CategoryId(1),
        account_id: AccountId(1),
        transfer_account_id: None,
        date: EpochMillis(1_764_000_000_000),
    }
}

fn transfer_draft(to_account: Option<i64>) -> TransactionDraft {
    TransactionDraft {
        transaction_type: TransactionType::Transfer,
        transfer_account_id: to_account.map(AccountId),
        ..draft()
    }
}

#[test]
fn amount_and_description_rules() {
    assert!(is_valid_amount(Cents(1_000)));
    assert!(!is_valid_amount(Cents(0)));
    assert!(!is_valid_amount(Cents(-1)));
    assert!(is_valid_description("Rent"));
    assert!(!is_valid_description("   "));
}

#[test]
fn inverted_search_ranges_are_rejected() {
    let amounts = |min, max| SearchFilters {
        min_amount: Some(Cents(min)),
        max_amount: Some(Cents(max)),
        ..SearchFilters::default()
    };
    let dates = SearchFilters {
        start_date: Some(EpochMillis(2)),
        end_date: Some(EpochMillis(1)),
        ..SearchFilters::default()
    };
    assert!(!is_valid_search_range(&amounts(10_000, 5_000)));
    assert!(!is_valid_search_range(&dates));
    assert!(is_valid_search_range(&amounts(100, 200)));
    assert!(is_valid_search_range(&amounts(100, 100)));
    assert!(is_valid_search_range(&SearchFilters::default()));
}

#[test]
fn name_and_percent_rules() {
    assert!(is_valid_name("Groceries"));
    assert!(!is_valid_name("   "));
    assert!(is_valid_name(&"x".repeat(MAX_NAME_UNITS)));
    assert!(!is_valid_name(&"x".repeat(MAX_NAME_UNITS + 1)));
    assert!(is_valid_percent(80));
    assert!(is_valid_percent(1));
    assert!(is_valid_percent(100));
    assert!(!is_valid_percent(0));
    assert!(!is_valid_percent(101));
}

#[test]
fn descriptions_are_trimmed_and_capped() {
    let longest = "x".repeat(MAX_DESCRIPTION_UNITS);
    assert!(is_valid_description(&longest));
    assert!(is_valid_description(&format!("  {longest}  ")));
    assert!(!is_valid_description(
        &"x".repeat(MAX_DESCRIPTION_UNITS + 1)
    ));
}

#[test]
fn lengths_count_utf16_code_units_as_v1_did() {
    // Each coin emoji is two UTF-16 code units, so 100 of them fill the limit.
    assert!(is_valid_description(&"💰".repeat(100)));
    assert!(!is_valid_description(&format!("{}x", "💰".repeat(100))));
    assert!(is_valid_name(&"é".repeat(MAX_NAME_UNITS)));
}

#[test]
fn whitespace_is_kotlins() {
    // U+001F counts as whitespace in Kotlin but not in Rust; U+0085 the other way around.
    assert!(!is_valid_description("\u{1F}\u{1F}"));
    assert!(is_valid_description("\u{85}"));
}

#[test]
fn a_valid_draft_becomes_a_trimmed_transaction() {
    let transaction = validate_transaction(&TransactionDraft {
        description: "  Rent \u{A0}".to_owned(),
        ..draft()
    })
    .unwrap();
    assert_eq!(transaction.description, "Rent");
    assert_eq!(transaction.kind, TransactionKind::Expense);
    assert_eq!(transaction.amount, Cents(120_000));
}

#[test]
fn an_expense_ignores_a_transfer_destination() {
    let transaction = validate_transaction(&TransactionDraft {
        transfer_account_id: Some(AccountId(2)),
        ..draft()
    })
    .unwrap();
    assert_eq!(transaction.kind, TransactionKind::Expense);
}

#[test]
fn a_transfer_needs_another_account() {
    assert_eq!(
        validate_transaction(&transfer_draft(None)),
        Err(DomainError::TransferDestinationRequired)
    );
    assert_eq!(
        validate_transaction(&transfer_draft(Some(1))),
        Err(DomainError::TransferDestinationSame)
    );
    assert_eq!(
        validate_transaction(&transfer_draft(Some(2))).map(|t| t.kind),
        Ok(TransactionKind::Transfer {
            to_account: AccountId(2)
        })
    );
}

#[test]
fn each_field_rule_has_its_error() {
    let cases = [
        (
            TransactionDraft {
                amount: Cents(0),
                ..draft()
            },
            DomainError::InvalidAmount,
        ),
        (
            TransactionDraft {
                description: " \t".to_owned(),
                ..draft()
            },
            DomainError::BlankDescription,
        ),
        (
            TransactionDraft {
                description: "x".repeat(MAX_DESCRIPTION_UNITS + 1),
                ..draft()
            },
            DomainError::DescriptionTooLong {
                max_units: MAX_DESCRIPTION_UNITS,
            },
        ),
        (
            TransactionDraft {
                date: EpochMillis(0),
                ..draft()
            },
            DomainError::InvalidDate,
        ),
        (
            TransactionDraft {
                account_id: AccountId(0),
                ..draft()
            },
            DomainError::InvalidAccount,
        ),
        (
            TransactionDraft {
                category_id: CategoryId(-1),
                ..draft()
            },
            DomainError::InvalidCategory,
        ),
    ];
    for (draft, error) in cases {
        assert_eq!(validate_transaction(&draft), Err(error));
    }
}

#[test]
fn errors_are_reported_in_v1s_order() {
    let everything_wrong = TransactionDraft {
        transaction_type: TransactionType::Transfer,
        description: String::new(),
        amount: Cents(-1),
        category_id: CategoryId(0),
        account_id: AccountId(0),
        transfer_account_id: None,
        date: EpochMillis(0),
        ..draft()
    };
    assert_eq!(
        validate_transaction(&everything_wrong),
        Err(DomainError::InvalidAmount)
    );
    let valid_amount = TransactionDraft {
        amount: Cents(1),
        ..everything_wrong.clone()
    };
    assert_eq!(
        validate_transaction(&valid_amount),
        Err(DomainError::BlankDescription)
    );
    let valid_text = TransactionDraft {
        description: "Move".to_owned(),
        ..valid_amount
    };
    assert_eq!(
        validate_transaction(&valid_text),
        Err(DomainError::InvalidDate)
    );
    let valid_date = TransactionDraft {
        date: EpochMillis(1),
        ..valid_text
    };
    assert_eq!(
        validate_transaction(&valid_date),
        Err(DomainError::InvalidAccount)
    );
    let valid_account = TransactionDraft {
        account_id: AccountId(1),
        ..valid_date
    };
    assert_eq!(
        validate_transaction(&valid_account),
        Err(DomainError::InvalidCategory)
    );
}

#[test]
fn budget_overage_is_the_amount_past_the_limit() {
    assert_eq!(
        budget_overage("Food", Cents(9_000), Cents(1_500), Cents(10_000)),
        Ok(Some(BudgetOverage {
            category_name: "Food".to_owned(),
            over_by: Cents(500),
            limit: Cents(10_000),
        }))
    );
    assert_eq!(
        budget_overage("Food", Cents(9_000), Cents(1_000), Cents(10_000)),
        Ok(None)
    );
    assert_eq!(
        budget_overage("Food", Cents(0), Cents(1), Cents(0)).map(|o| o.map(|o| o.over_by)),
        Ok(Some(Cents(1)))
    );
    assert!(budget_overage("Food", Cents(i64::MAX), Cents(1), Cents(0)).is_err());
}

#[test]
fn error_codes_are_v1s_names() {
    assert_eq!(DomainError::InvalidAmount.code(), "INVALID_AMOUNT");
    assert_eq!(
        DomainError::DescriptionTooLong { max_units: 200 }.code(),
        "DESCRIPTION_TOO_LONG"
    );
    let overage = BudgetOverage {
        category_name: "Food".to_owned(),
        over_by: Cents(1),
        limit: Cents(1),
    };
    assert_eq!(
        DomainError::BudgetExceeded(overage).code(),
        "BUDGET_EXCEEDED"
    );
    assert_eq!(
        DomainError::InvalidSearchRange.code(),
        "INVALID_SEARCH_RANGE"
    );
}

#[test]
fn the_seeded_rows_are_protected() {
    assert!(defaults::is_default_account(AccountId(1)));
    assert!(defaults::is_default_account(AccountId(2)));
    assert!(!defaults::is_default_account(AccountId(3)));
    for id in 1..=3 {
        assert!(defaults::is_default_category(CategoryId(id)));
    }
    assert!(!defaults::is_default_category(CategoryId(4)));
    assert_eq!(defaults::CATEGORIES[0].1, "ic_category_default");
}
