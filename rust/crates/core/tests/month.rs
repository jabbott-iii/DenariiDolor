/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The bounds of a calendar month that Dart passes in (ADR-0019): which spans `MonthBounds`
//! accepts, and which moments belong to the month.

use denarii_core::{EpochMillis, InvalidMonthBounds, MonthBounds};

const DAY: i64 = 86_400_000;

/// 2026-11-01T00:00:00Z and 2026-12-01T00:00:00Z: November 2026 in UTC.
const NOVEMBER_START: i64 = 1_793_491_200_000;
const DECEMBER_START: i64 = 1_796_083_200_000;

fn bounds(start: i64, next_start: i64) -> Result<MonthBounds, InvalidMonthBounds> {
    MonthBounds::new(EpochMillis(start), EpochMillis(next_start))
}

#[test]
fn a_month_holds_its_start_and_not_the_next_months_start() {
    let november = bounds(NOVEMBER_START, DECEMBER_START).unwrap();
    assert_eq!(november.start(), EpochMillis(NOVEMBER_START));
    assert_eq!(november.next_start(), EpochMillis(DECEMBER_START));
    assert!(!november.contains(EpochMillis(NOVEMBER_START - 1)));
    assert!(november.contains(EpochMillis(NOVEMBER_START)));
    assert!(november.contains(EpochMillis(DECEMBER_START - 1)));
    assert!(!november.contains(EpochMillis(DECEMBER_START)));
}

/// The shortest and longest months since 1970 in tzdata 2026e, and the limits around them.
#[test]
fn accepts_spans_from_27_to_32_days() {
    assert_eq!(MonthBounds::MIN_SPAN_MILLIS, 27 * DAY);
    assert_eq!(MonthBounds::MAX_SPAN_MILLIS, 32 * DAY);
    // February 1975 in America/Adak, 27.96 days, and January 1994 in Antarctica/Vostok, 31.29.
    assert!(bounds(0, 28 * DAY - 3_600_000).is_ok());
    assert!(bounds(0, 31 * DAY + 7 * 3_600_000).is_ok());
    assert!(bounds(0, 27 * DAY).is_ok());
    assert!(bounds(0, 32 * DAY).is_ok());
    assert_eq!(bounds(0, 27 * DAY - 1), Err(InvalidMonthBounds));
    assert_eq!(bounds(0, 32 * DAY + 1), Err(InvalidMonthBounds));
}

#[test]
fn refuses_swapped_empty_and_wrong_sized_bounds() {
    assert_eq!(
        bounds(DECEMBER_START, NOVEMBER_START),
        Err(InvalidMonthBounds)
    );
    assert_eq!(
        bounds(NOVEMBER_START, NOVEMBER_START),
        Err(InvalidMonthBounds)
    );
    // A day's bounds, and a year's.
    assert_eq!(
        bounds(NOVEMBER_START, NOVEMBER_START + DAY),
        Err(InvalidMonthBounds)
    );
    assert_eq!(
        bounds(NOVEMBER_START, NOVEMBER_START + 365 * DAY),
        Err(InvalidMonthBounds)
    );
}

#[test]
fn bounds_that_overflow_the_span_are_refused_without_panicking() {
    assert_eq!(bounds(i64::MIN, i64::MAX), Err(InvalidMonthBounds));
    assert_eq!(bounds(i64::MAX, i64::MIN), Err(InvalidMonthBounds));
    assert!(bounds(i64::MAX - 30 * DAY, i64::MAX).is_ok());
    assert!(bounds(i64::MIN, i64::MIN + 30 * DAY).is_ok());
}

/// Months before 1970 have negative moments; January 1970 starts at 0 in UTC.
#[test]
fn accepts_months_that_start_before_1970() {
    let december_1969 = bounds(-31 * DAY, 0).unwrap();
    assert!(december_1969.contains(EpochMillis(-1)));
    assert!(!december_1969.contains(EpochMillis(0)));
}
