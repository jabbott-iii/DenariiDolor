/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The filters a transaction search can combine. The search itself runs in the storage layer
//! (M2.7).

use crate::ids::{CategoryId, EpochMillis};
use crate::money::Cents;

/// What a search matches; a field left `None` matches everything.
#[derive(Debug, Clone, PartialEq, Eq, Hash, Default)]
pub struct SearchFilters {
    /// Text the description must contain.
    pub description: Option<String>,
    /// The category the transaction must belong to.
    pub category_id: Option<CategoryId>,
    /// The smallest amount, inclusive.
    pub min_amount: Option<Cents>,
    /// The largest amount, inclusive.
    pub max_amount: Option<Cents>,
    /// The earliest date, inclusive.
    pub start_date: Option<EpochMillis>,
    /// The latest date, inclusive.
    pub end_date: Option<EpochMillis>,
}
