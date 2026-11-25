/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! A calendar month in the user's time zone, as the first moment of each of its days.
//!
//! Rust reads no clock and no time zone (ADR-0019), but a report writes each transaction's date
//! as the phone's calendar shows it. So the caller passes the first moment of every day of the
//! month, which Dart computes with `DateTime(year, month, day)` for each day, the same way it
//! computes a month's bounds (ADR-0021). This module checks that the moments can describe the
//! month, and turns a moment into its date.

use std::fmt;

use denarii_core::{EpochMillis, MonthBounds};

use crate::error::ReportError;

/// A date on the user's calendar.
///
/// It displays as `YYYY-MM-DD`, which is how v1's CSV exports wrote each transaction's date
/// (kotlinx-datetime's `LocalDate.toString`).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct LocalDate {
    /// The year, from 1 to 9999.
    pub year: i32,
    /// The month, from 1 to 12.
    pub month: u8,
    /// The day of the month, from 1.
    pub day: u8,
}

impl fmt::Display for LocalDate {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{:04}-{:02}-{:02}", self.year, self.month, self.day)
    }
}

/// A calendar month in the user's time zone, with the first moment of each of its days.
///
/// Day `d` of the month holds every moment from its first moment, inclusive, to the next day's
/// first moment, exclusive; the last day ends where the next month starts. A day is usually 24
/// hours long, but not where the clocks change: in `Asia/Beirut`, October 24, 2026 lasts 25
/// hours, because the clocks go back from midnight to 23:00.
///
/// # Examples
///
/// ```
/// use denarii_core::EpochMillis;
/// use denarii_report::{LocalDate, MonthDays};
///
/// # fn main() -> Result<(), Box<dyn std::error::Error>> {
/// // February 2026 in UTC: 28 days of 24 hours from 2026-02-01T00:00:00Z.
/// const DAY: i64 = 86_400_000;
/// let starts = (0..28).map(|d| EpochMillis(1_769_904_000_000 + d * DAY)).collect();
/// let february = MonthDays::new(2026, 2, starts, EpochMillis(1_769_904_000_000 + 28 * DAY))?;
///
/// let date = february.date_of(EpochMillis(1_769_904_000_000 + 27 * DAY));
/// assert_eq!(date, Some(LocalDate { year: 2026, month: 2, day: 28 }));
/// assert_eq!(february.date_of(february.bounds().next_start()), None);
/// # Ok(())
/// # }
/// ```
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct MonthDays {
    year: i32,
    month: u8,
    day_starts: Vec<EpochMillis>,
    bounds: MonthBounds,
}

impl MonthDays {
    /// The longest day accepted, 48 hours in milliseconds.
    ///
    /// A day lasts 24 hours, plus or minus a change of the clocks. The largest change in the
    /// time-zone database since 1970 is a day's: a place that moved across the date line
    /// repeats a date, and that date lasts 48 hours. A date skipped the other way lasts 0
    /// hours, so its first moment is the next date's.
    pub const MAX_DAY_SPAN_MILLIS: i64 = 172_800_000;

    /// The month `month` of `year`, whose day `d` starts at `day_starts[d - 1]`, and which ends
    /// where the next month starts, at `next_month_start`.
    ///
    /// # Errors
    ///
    /// Returns [`ReportError::InvalidMonthDays`] unless all of these hold:
    ///
    /// - `year` is from 1 to 9999 and `month` from 1 to 12;
    /// - `day_starts` holds one moment for each day of that month in the Gregorian calendar;
    /// - the first day's start and `next_month_start` make a [`MonthBounds`], which spans 27 to
    ///   32 days;
    /// - each day, the last included, ends 0 to [`MAX_DAY_SPAN_MILLIS`](Self::MAX_DAY_SPAN_MILLIS)
    ///   after it starts, so no day starts before the day before it.
    pub fn new(
        year: i32,
        month: u8,
        day_starts: Vec<EpochMillis>,
        next_month_start: EpochMillis,
    ) -> Result<Self, ReportError> {
        let days = days_in_month(year, month).ok_or(ReportError::InvalidMonthDays)?;
        if day_starts.len() != usize::from(days) {
            return Err(ReportError::InvalidMonthDays);
        }
        let first = *day_starts.first().ok_or(ReportError::InvalidMonthDays)?;
        let bounds =
            MonthBounds::new(first, next_month_start).map_err(|_| ReportError::InvalidMonthDays)?;
        let ends = day_starts.iter().skip(1).chain([&next_month_start]);
        for (start, end) in day_starts.iter().zip(ends) {
            let span = end
                .0
                .checked_sub(start.0)
                .ok_or(ReportError::InvalidMonthDays)?;
            if !(0..=Self::MAX_DAY_SPAN_MILLIS).contains(&span) {
                return Err(ReportError::InvalidMonthDays);
            }
        }
        Ok(Self {
            year,
            month,
            day_starts,
            bounds,
        })
    }

    /// The year.
    #[must_use]
    pub const fn year(&self) -> i32 {
        self.year
    }

    /// The month, from 1 to 12.
    #[must_use]
    pub const fn month(&self) -> u8 {
        self.month
    }

    /// The moments the month starts and the next one starts, which a store read takes.
    #[must_use]
    pub const fn bounds(&self) -> MonthBounds {
        self.bounds
    }

    /// The date on the user's calendar of `moment`, or `None` if the month doesn't hold it.
    ///
    /// Where a date was skipped, so that its first moment is the next date's, every moment
    /// belongs to the later date.
    #[must_use]
    pub fn date_of(&self, moment: EpochMillis) -> Option<LocalDate> {
        if !self.bounds.contains(moment) {
            return None;
        }
        // The days that start at or before the moment; the last of them holds it. The month's
        // first moment starts day 1, so at least one does.
        let started = self.day_starts.partition_point(|start| *start <= moment);
        let day = u8::try_from(started).ok()?;
        Some(LocalDate {
            year: self.year,
            month: self.month,
            day,
        })
    }
}

/// The number of days in `month` of `year` in the Gregorian calendar, or `None` for a year
/// outside 1 to 9999 or a month outside 1 to 12.
fn days_in_month(year: i32, month: u8) -> Option<u8> {
    if !(1..=9999).contains(&year) {
        return None;
    }
    let leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    match month {
        1 | 3 | 5 | 7 | 8 | 10 | 12 => Some(31),
        4 | 6 | 9 | 11 => Some(30),
        2 if leap => Some(29),
        2 => Some(28),
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use denarii_core::EpochMillis;

    use super::{LocalDate, MonthDays, days_in_month};
    use crate::error::ReportError;

    const DAY: i64 = 86_400_000;
    const HOUR: i64 = 3_600_000;
    /// 2026-02-01T00:00:00Z.
    const FEBRUARY_2026: i64 = 1_769_904_000_000;
    /// 2024-02-01T00:00:00Z.
    const FEBRUARY_2024: i64 = 1_706_745_600_000;

    /// `count` days of 24 hours from `start`, and the moment the last one ends.
    fn utc_days(start: i64, count: usize) -> (Vec<EpochMillis>, EpochMillis) {
        let mut moments =
            std::iter::successors(Some(start), |s| s.checked_add(DAY)).map(EpochMillis);
        let starts = moments.by_ref().take(count).collect();
        (starts, moments.next().unwrap())
    }

    fn february_2026() -> MonthDays {
        let (starts, next) = utc_days(FEBRUARY_2026, 28);
        MonthDays::new(2026, 2, starts, next).unwrap()
    }

    fn date(year: i32, month: u8, day: u8) -> LocalDate {
        LocalDate { year, month, day }
    }

    #[test]
    fn a_date_displays_as_v1s_csv_wrote_it() {
        assert_eq!(date(2026, 10, 1).to_string(), "2026-10-01");
        assert_eq!(date(2026, 12, 31).to_string(), "2026-12-31");
        assert_eq!(date(1, 1, 9).to_string(), "0001-01-09");
        assert_eq!(date(9999, 9, 30).to_string(), "9999-09-30");
    }

    #[test]
    fn each_moment_falls_on_the_day_whose_first_moment_it_follows() {
        let february = february_2026();

        assert_eq!(
            february.date_of(EpochMillis(FEBRUARY_2026)),
            Some(date(2026, 2, 1))
        );
        assert_eq!(
            february.date_of(EpochMillis(FEBRUARY_2026 + DAY - 1)),
            Some(date(2026, 2, 1))
        );
        assert_eq!(
            february.date_of(EpochMillis(FEBRUARY_2026 + DAY)),
            Some(date(2026, 2, 2))
        );
        assert_eq!(
            february.date_of(EpochMillis(FEBRUARY_2026 + 28 * DAY - 1)),
            Some(date(2026, 2, 28))
        );
    }

    #[test]
    fn a_moment_outside_the_month_has_no_date() {
        let february = february_2026();

        assert_eq!(february.date_of(EpochMillis(FEBRUARY_2026 - 1)), None);
        assert_eq!(
            february.date_of(EpochMillis(FEBRUARY_2026 + 28 * DAY)),
            None
        );
        assert_eq!(february.date_of(EpochMillis(i64::MIN)), None);
        assert_eq!(february.date_of(EpochMillis(i64::MAX)), None);
    }

    #[test]
    fn the_month_keeps_its_year_month_and_bounds() {
        let february = february_2026();

        assert_eq!(february.year(), 2026);
        assert_eq!(february.month(), 2);
        assert_eq!(february.bounds().start(), EpochMillis(FEBRUARY_2026));
        assert_eq!(
            february.bounds().next_start(),
            EpochMillis(FEBRUARY_2026 + 28 * DAY)
        );
    }

    #[test]
    fn a_25_hour_day_holds_the_hour_the_clocks_repeat() {
        // October 2026 in Asia/Beirut, from fixture F8: day 24 lasts 25 hours.
        let starts: Vec<EpochMillis> = (0..31)
            .map(|d| {
                let start = 1_790_802_000_000 + d * DAY;
                EpochMillis(if d >= 24 { start + HOUR } else { start })
            })
            .collect();
        let october = MonthDays::new(2026, 10, starts, EpochMillis(1_793_484_000_000)).unwrap();

        // 23:30 EEST and 23:30 EET on the 24th, then midnight EET on the 25th.
        assert_eq!(
            october.date_of(EpochMillis(1_792_873_800_000)),
            Some(date(2026, 10, 24))
        );
        assert_eq!(
            october.date_of(EpochMillis(1_792_877_400_000)),
            Some(date(2026, 10, 24))
        );
        assert_eq!(
            october.date_of(EpochMillis(1_792_879_200_000)),
            Some(date(2026, 10, 25))
        );
        assert_eq!(
            october.date_of(EpochMillis(1_793_483_999_999)),
            Some(date(2026, 10, 31))
        );
    }

    #[test]
    fn a_skipped_dates_moments_belong_to_the_next_date() {
        // Samoa skipped December 30, 2011: Dart's DateTime for it is the 31st's first moment.
        let mut starts: Vec<EpochMillis> = (0..31).map(|d| EpochMillis(d * DAY)).collect();
        starts[29] = starts[30];
        let december = MonthDays::new(2011, 12, starts, EpochMillis(31 * DAY)).unwrap();

        assert_eq!(
            december.date_of(EpochMillis(29 * DAY - 1)),
            Some(date(2011, 12, 29))
        );
        assert_eq!(
            december.date_of(EpochMillis(30 * DAY)),
            Some(date(2011, 12, 31))
        );
        assert_eq!(
            december.date_of(EpochMillis(31 * DAY - 1)),
            Some(date(2011, 12, 31))
        );
    }

    #[test]
    fn a_day_starting_before_the_day_before_it_is_refused() {
        let (mut starts, next) = utc_days(FEBRUARY_2024, 29);
        // Day 3 starts with day 2, so day 2 lasts 0 hours and day 3 48.
        starts[2] = starts[1];
        assert!(MonthDays::new(2024, 2, starts.clone(), next).is_ok());
        // One millisecond earlier, day 3 starts before day 2 does.
        starts[2] = EpochMillis(starts[1].0 - 1);
        assert_eq!(
            MonthDays::new(2024, 2, starts, next),
            Err(ReportError::InvalidMonthDays)
        );
    }

    #[test]
    fn a_48_hour_day_is_accepted_and_a_longer_one_refused() {
        let (mut starts, next) = utc_days(FEBRUARY_2024, 29);
        starts[1] = EpochMillis(starts[0].0 + 48 * HOUR);
        starts[2] = EpochMillis(starts[1].0);
        assert!(MonthDays::new(2024, 2, starts.clone(), next).is_ok());
        starts[1] = EpochMillis(starts[0].0 + 48 * HOUR + 1);
        starts[2] = EpochMillis(starts[1].0);
        assert_eq!(
            MonthDays::new(2024, 2, starts, next),
            Err(ReportError::InvalidMonthDays)
        );
    }

    #[test]
    fn the_last_day_ends_where_the_next_month_starts() {
        let (starts, _) = utc_days(FEBRUARY_2026, 28);
        let last = starts[27].0;
        assert!(MonthDays::new(2026, 2, starts.clone(), EpochMillis(last + 48 * HOUR)).is_ok());
        assert_eq!(
            MonthDays::new(2026, 2, starts.clone(), EpochMillis(last + 48 * HOUR + 1)),
            Err(ReportError::InvalidMonthDays)
        );
        assert_eq!(
            MonthDays::new(2026, 2, starts.clone(), EpochMillis(last - 1)),
            Err(ReportError::InvalidMonthDays)
        );
        // A last day of 0 hours: the next month starts with it.
        assert!(MonthDays::new(2026, 2, starts, EpochMillis(last)).is_ok());
    }

    #[test]
    fn days_out_of_order_are_refused() {
        let (mut starts, next) = utc_days(FEBRUARY_2026, 28);
        starts.swap(3, 4);
        assert_eq!(
            MonthDays::new(2026, 2, starts, next),
            Err(ReportError::InvalidMonthDays)
        );
    }

    #[test]
    fn the_number_of_days_must_be_the_months() {
        for (count, valid) in [(27, false), (28, true), (29, false), (0, false)] {
            let (starts, _) = utc_days(FEBRUARY_2026, count);
            let next = EpochMillis(FEBRUARY_2026 + 28 * DAY);
            assert_eq!(
                MonthDays::new(2026, 2, starts, next).is_ok(),
                valid,
                "{count} days"
            );
        }
        let (starts, next) = utc_days(FEBRUARY_2024, 29);
        assert!(MonthDays::new(2024, 2, starts, next).is_ok());
    }

    #[test]
    fn a_span_that_cant_be_a_month_is_refused() {
        // Thirty days of 24 hours make a month of September, but a February of 28 days whose
        // days are all one hour long spans too little.
        let starts: Vec<EpochMillis> = (0..28)
            .map(|d| EpochMillis(FEBRUARY_2026 + d * HOUR))
            .collect();
        assert_eq!(
            MonthDays::new(2026, 2, starts, EpochMillis(FEBRUARY_2026 + 28 * HOUR)),
            Err(ReportError::InvalidMonthDays)
        );
    }

    #[test]
    fn moments_at_the_ends_of_the_range_dont_overflow() {
        let mut starts = vec![EpochMillis(i64::MIN); 28];
        starts[27] = EpochMillis(i64::MAX);
        assert_eq!(
            MonthDays::new(2026, 2, starts, EpochMillis(i64::MAX)),
            Err(ReportError::InvalidMonthDays)
        );
        let starts = vec![EpochMillis(i64::MAX - 1); 28];
        assert_eq!(
            MonthDays::new(2026, 2, starts, EpochMillis(i64::MIN)),
            Err(ReportError::InvalidMonthDays)
        );
        // A real month whose second day starts at the smallest moment: the first day's span
        // can't be computed, and is refused rather than wrapped.
        let (mut starts, next) = utc_days(FEBRUARY_2026, 28);
        starts[1] = EpochMillis(i64::MIN);
        assert_eq!(
            MonthDays::new(2026, 2, starts, next),
            Err(ReportError::InvalidMonthDays)
        );
    }

    #[test]
    fn a_year_or_month_outside_the_calendar_is_refused() {
        for (year, month) in [(0, 1), (10_000, 1), (-1, 6), (2026, 0), (2026, 13)] {
            let (starts, next) = utc_days(FEBRUARY_2026, 31);
            assert_eq!(
                MonthDays::new(year, month, starts, next),
                Err(ReportError::InvalidMonthDays),
                "{year}-{month}"
            );
        }
    }

    #[test]
    fn months_have_the_gregorian_calendars_days() {
        let lengths: Vec<Option<u8>> = (1..=12).map(|m| days_in_month(2026, m)).collect();
        let expected = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31].map(Some);
        assert_eq!(lengths, expected);
        assert_eq!(days_in_month(2024, 2), Some(29));
        assert_eq!(days_in_month(2000, 2), Some(29));
        assert_eq!(days_in_month(1900, 2), Some(28));
        assert_eq!(days_in_month(2100, 2), Some(28));
        assert_eq!(days_in_month(1, 1), Some(31));
        assert_eq!(days_in_month(9999, 12), Some(31));
        assert_eq!(days_in_month(0, 1), None);
        assert_eq!(days_in_month(10_000, 1), None);
        assert_eq!(days_in_month(2026, 0), None);
        assert_eq!(days_in_month(2026, 13), None);
    }
}
