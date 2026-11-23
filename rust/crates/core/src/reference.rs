/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The reference data transactions point to: accounts, categories, and monthly budgets.
//!
//! Their rules (names, duplicates, the protected defaults in [`defaults`](crate::defaults), and
//! rows still in use) need stored data, so they run in the storage layer (M2.5).

use crate::defaults::DEFAULT_CATEGORY_ICON;
use crate::ids::{AccountId, BudgetId, CategoryId};
use crate::money::Cents;

/// An account, such as cash or a bank account, with its stored balance.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Account {
    /// The row's ID, or `None` before the account is stored.
    pub id: Option<AccountId>,
    /// The name the user gave it.
    pub name: String,
    /// The balance, which every transaction write changes by its
    /// [`balance_deltas`](crate::balance_deltas).
    pub balance: Cents,
}

/// A category that transactions are grouped by.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Category {
    /// The row's ID, or `None` before the category is stored.
    pub id: Option<CategoryId>,
    /// The name the user gave it.
    pub name: String,
    /// The key of its icon, such as `ic_category_default` or `income`.
    pub icon_name: String,
}

impl Category {
    /// A new category with the default icon.
    #[must_use]
    pub fn new(name: &str) -> Self {
        Self {
            id: None,
            name: name.to_owned(),
            icon_name: DEFAULT_CATEGORY_ICON.to_owned(),
        }
    }
}

/// A monthly spending limit for one category.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Budget {
    /// The row's ID, or `None` before the budget is stored.
    pub id: Option<BudgetId>,
    /// The category it limits.
    pub category_id: CategoryId,
    /// The most the category may spend in a calendar month.
    pub monthly_limit: Cents,
    /// The share of the limit, in percent, at which the app starts warning.
    pub warning_threshold_percent: i32,
}

impl Budget {
    /// The warning threshold a new budget starts with.
    pub const DEFAULT_WARNING_PERCENT: i32 = 80;
}
