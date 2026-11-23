/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The accounts and categories every install starts with, which can't be deleted.
//!
//! Setup stores them with these IDs and their names in the user's language (M2.6); after that
//! the names are ordinary data the user can change. The IDs and icons are v1's (`Constants.kt`
//! in v1).

use crate::ids::{AccountId, CategoryId};

/// The cash account.
pub const CASH_ACCOUNT: AccountId = AccountId(1);

/// The savings account.
pub const SAVINGS_ACCOUNT: AccountId = AccountId(2);

/// The category new expenses start in.
pub const EXPENSE_CATEGORY: CategoryId = CategoryId(1);

/// The category new income starts in.
pub const INCOME_CATEGORY: CategoryId = CategoryId(2);

/// The category transfers start in.
pub const TRANSFER_CATEGORY: CategoryId = CategoryId(3);

/// The icon a category gets when none is chosen, and the expense category's icon.
pub const DEFAULT_CATEGORY_ICON: &str = "ic_category_default";

/// The seeded accounts, in the order setup stores them.
pub const ACCOUNTS: [AccountId; 2] = [CASH_ACCOUNT, SAVINGS_ACCOUNT];

/// The seeded categories and their icon keys, in the order setup stores them.
pub const CATEGORIES: [(CategoryId, &str); 3] = [
    (EXPENSE_CATEGORY, DEFAULT_CATEGORY_ICON),
    (INCOME_CATEGORY, "income"),
    (TRANSFER_CATEGORY, "transfer"),
];

/// Whether `id` is a seeded account, which can't be deleted.
#[must_use]
pub fn is_default_account(id: AccountId) -> bool {
    ACCOUNTS.contains(&id)
}

/// Whether `id` is a seeded category, which can't be deleted.
#[must_use]
pub fn is_default_category(id: CategoryId) -> bool {
    CATEGORIES.iter().any(|&(category, _)| category == id)
}
