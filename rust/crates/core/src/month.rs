/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! A calendar month in the user's time zone, as the two moments that bound it.
//!
//! Rust reads no clock and no time zone, so the caller turns the month into moments: Dart takes
//! the first moment of the month's first day and of the next month's, in the phone's current
//! time zone (ADR-0019). This module checks only that the two moments can bound a month. Which
//! month they bound, and in which zone, is the caller's to get right.

use std::fmt;

use crate::ids::EpochMillis;

/// The moments a calendar month starts and the next one starts, in the user's time zone.
///
/// A month holds every moment from [`start`](Self::start), inclusive, to
/// [`next_start`](Self::next_start), exclusive. The first moment of a day is usually midnight,
/// but not where a daylight-saving change skips midnight: `America/Asuncion` went from midnight
/// to 01:00 on 2023-10-01, so October 2023 started at 01:00 there. v1's budget check took the
/// same moments, with kotlinx-datetime's `atStartOfDayIn`.
///
/// # Examples
///
/// ```
/// use denarii_core::{EpochMillis, MonthBounds};
///
/// # fn main() -> Result<(), Box<dyn std::error::Error>> {
/// // November 2026 in UTC.
/// let november = MonthBounds::new(EpochMillis(1_793_491_200_000), EpochMillis(1_796_083_200_000))?;
/// assert!(november.contains(EpochMillis(1_793_491_200_000)));
/// assert!(!november.contains(november.next_start()));
/// # Ok(())
/// # }
/// ```
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct MonthBounds {
    start: EpochMillis,
    next_start: EpochMillis,
}

impl MonthBounds {
    /// The shortest span accepted, 27 days in milliseconds.
    ///
    /// The shortest month in the time-zone database (tzdata 2026e), over every zone and every
    /// month from 1970 to 2100, lasts 27.96 days: February 1975 in `America/Adak`, which lost an
    /// hour to daylight saving. A day's margin below it refuses no real month.
    pub const MIN_SPAN_MILLIS: i64 = 2_332_800_000;

    /// The longest span accepted, 32 days in milliseconds.
    ///
    /// The longest month in the same data lasts 31.29 days: January 1994 in `Antarctica/Vostok`,
    /// whose offset from UTC changed by seven hours.
    pub const MAX_SPAN_MILLIS: i64 = 2_764_800_000;

    /// The month from `start` to `next_start`.
    ///
    /// # Errors
    ///
    /// Returns [`InvalidMonthBounds`] unless `next_start` comes after `start` by
    /// [`MIN_SPAN_MILLIS`](Self::MIN_SPAN_MILLIS) to [`MAX_SPAN_MILLIS`](Self::MAX_SPAN_MILLIS),
    /// inclusive. That refuses swapped bounds, and a day's or a year's bounds passed as a month's.
    pub fn new(start: EpochMillis, next_start: EpochMillis) -> Result<Self, InvalidMonthBounds> {
        let span = next_start
            .0
            .checked_sub(start.0)
            .ok_or(InvalidMonthBounds)?;
        if (Self::MIN_SPAN_MILLIS..=Self::MAX_SPAN_MILLIS).contains(&span) {
            Ok(Self { start, next_start })
        } else {
            Err(InvalidMonthBounds)
        }
    }

    /// The month's first moment, which belongs to the month.
    #[must_use]
    pub const fn start(self) -> EpochMillis {
        self.start
    }

    /// The next month's first moment, which doesn't belong to this month.
    #[must_use]
    pub const fn next_start(self) -> EpochMillis {
        self.next_start
    }

    /// Whether `date` falls in the month.
    #[must_use]
    pub const fn contains(self, date: EpochMillis) -> bool {
        self.start.0 <= date.0 && date.0 < self.next_start.0
    }
}

/// [`MonthBounds::new`] got moments that can't bound a calendar month.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct InvalidMonthBounds;

impl fmt::Display for InvalidMonthBounds {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("the moments don't bound a calendar month: the span must be 27 to 32 days")
    }
}

impl std::error::Error for InvalidMonthBounds {}
