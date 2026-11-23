/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Typed IDs for stored rows, so an account's ID can't be passed where a category's belongs, and
//! the type that dates are stored in.
//!
//! Each ID wraps the `INTEGER PRIMARY KEY` that SQLite assigns to the row, as v1 stored it. A row
//! that isn't stored yet has no ID: its `id` field is `None`.

macro_rules! row_id {
    ($(#[$doc:meta])* $name:ident) => {
        $(#[$doc])*
        #[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord)]
        pub struct $name(pub i64);
    };
}

row_id!(
    /// The ID of a row in the `accounts` table.
    AccountId
);
row_id!(
    /// The ID of a row in the `categories` table.
    CategoryId
);
row_id!(
    /// The ID of a row in the `transactions` table.
    TransactionId
);
row_id!(
    /// The ID of a row in the `budgets` table.
    BudgetId
);

/// A moment, in milliseconds since 1970-01-01T00:00:00Z, which is how v1 stores dates.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct EpochMillis(pub i64);
