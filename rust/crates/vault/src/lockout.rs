/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The lockout's schedule and its timer (CS-13), as v1's `SecurityProfileService` ran them
//! (`docs/specs/android-v1-formats.md`, section 3).
//!
//! Failed PIN and answer checks share one counter. The first four only count; from the fifth on,
//! each locks sign-in for 30 seconds, doubling with each further failure up to 15 minutes. The
//! timer runs on time since boot, which changing the date can't move, so this crate reads no clock:
//! each call takes a [`MonotonicTime`] from its caller.

/// Failures that only count; the next one starts a lockout.
pub const MAX_FREE_ATTEMPTS: u32 = 5;
/// The first lockout, after the fifth failure, in milliseconds.
pub const BASE_LOCKOUT_MILLIS: u64 = 30_000;
/// The longest lockout, in milliseconds: 15 minutes.
pub const MAX_LOCKOUT_MILLIS: u64 = 900_000;
/// The most times the lockout doubles; the cap stops it first, at the tenth failure.
const MAX_DOUBLINGS: u32 = 5;

/// The time that the lockout runs on, which the caller reads when the user acts.
///
/// On Android it is `SystemClock.elapsedRealtime()`, the milliseconds since boot including deep
/// sleep, and `Settings.Global.BOOT_COUNT`, as v1 read them (CS-13). Both must come from there,
/// never from the wall clock, or changing the date would shorten a lockout. v1 stored them in the
/// profile, so a lockout started by v1 carries on after the update.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct MonotonicTime {
    /// Milliseconds since the device booted.
    pub elapsed_millis: i64,
    /// How many times the device has booted, or `None` when the platform doesn't say. v1 stored
    /// an unknown count as −1, so a count of −1 also reads as unknown.
    pub boot_count: Option<i32>,
}

impl MonotonicTime {
    /// The boot count as v1 stored it, with −1 for unknown.
    pub(crate) fn stored_boot_count(self) -> i32 {
        self.boot_count.unwrap_or(UNKNOWN_BOOT)
    }
}

/// The boot count v1 stored when the platform didn't give one.
pub(crate) const UNKNOWN_BOOT: i32 = -1;

/// How long `failures` failed checks in a row lock sign-in, in milliseconds: none for the first
/// four, then 30 seconds, doubling with each further failure, capped at 15 minutes.
///
/// # Examples
///
/// ```
/// use denarii_vault::lockout_duration_millis;
///
/// assert_eq!(lockout_duration_millis(4), 0);
/// assert_eq!(lockout_duration_millis(5), 30_000);
/// assert_eq!(lockout_duration_millis(9), 480_000);
/// assert_eq!(lockout_duration_millis(10), 900_000);
/// ```
#[must_use]
pub fn lockout_duration_millis(failures: u32) -> u64 {
    let Some(past_free) = failures.checked_sub(MAX_FREE_ATTEMPTS) else {
        return 0;
    };
    let doublings = past_free.min(MAX_DOUBLINGS);
    (BASE_LOCKOUT_MILLIS << doublings).min(MAX_LOCKOUT_MILLIS)
}

/// What the stored lockout timer says at `now`.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum Timer {
    /// No lockout is stored.
    Off,
    /// The lockout has `remaining` milliseconds left.
    Running { remaining: u64 },
    /// The lockout has been served; its timer can be cleared.
    Served,
    /// The device rebooted since the lockout started, so the time served is unknown, and the whole
    /// lockout of `duration` milliseconds starts again now.
    Restart { duration: u64 },
}

/// Reads v1's stored timer: a lockout of `duration` milliseconds that started at `started`
/// milliseconds since boot, on boot `stored_boot` (−1 when unknown).
///
/// Elapsed time starts again at each boot. So when `now` is earlier than the start, or both boot
/// counts are known and differ, the time already served is unknown, and v1 served the whole
/// lockout again from now.
pub(crate) fn read_timer(
    duration: i64,
    started: i64,
    stored_boot: i32,
    now: MonotonicTime,
) -> Timer {
    let Ok(duration) = u64::try_from(duration) else {
        return Timer::Off;
    };
    if duration == 0 {
        return Timer::Off;
    }
    let boot = now.stored_boot_count();
    let rebooted = boot != UNKNOWN_BOOT && stored_boot != UNKNOWN_BOOT && boot != stored_boot;
    if now.elapsed_millis < started || rebooted {
        return Timer::Restart { duration };
    }
    // In 128 bits, the sum and difference of two 64-bit values are exact, so they never saturate.
    let remaining = i128::from(started)
        .saturating_add(i128::from(duration))
        .saturating_sub(i128::from(now.elapsed_millis));
    match u64::try_from(remaining) {
        Ok(0) | Err(_) => Timer::Served,
        Ok(remaining) => Timer::Running { remaining },
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const fn at(elapsed_millis: i64, boot_count: Option<i32>) -> MonotonicTime {
        MonotonicTime {
            elapsed_millis,
            boot_count,
        }
    }

    #[test]
    fn duration_escalates_and_caps() {
        assert_eq!(lockout_duration_millis(0), 0);
        assert_eq!(lockout_duration_millis(4), 0);
        assert_eq!(lockout_duration_millis(5), 30_000);
        assert_eq!(lockout_duration_millis(6), 60_000);
        assert_eq!(lockout_duration_millis(7), 120_000);
        assert_eq!(lockout_duration_millis(8), 240_000);
        assert_eq!(lockout_duration_millis(9), 480_000);
        assert_eq!(lockout_duration_millis(10), 900_000);
        assert_eq!(lockout_duration_millis(1_000), 900_000);
        assert_eq!(lockout_duration_millis(u32::MAX), 900_000);
    }

    #[test]
    fn no_stored_duration_is_no_lockout() {
        assert_eq!(read_timer(0, 1_000, 7, at(1_000, Some(7))), Timer::Off);
        assert_eq!(
            read_timer(-30_000, 1_000, 7, at(1_000, Some(7))),
            Timer::Off
        );
    }

    #[test]
    fn a_running_lockout_counts_down_to_served() {
        assert_eq!(
            read_timer(30_000, 1_000, 7, at(1_000, Some(7))),
            Timer::Running { remaining: 30_000 }
        );
        assert_eq!(
            read_timer(30_000, 1_000, 7, at(30_999, Some(7))),
            Timer::Running { remaining: 1 }
        );
        assert_eq!(
            read_timer(30_000, 1_000, 7, at(31_000, Some(7))),
            Timer::Served
        );
        assert_eq!(
            read_timer(30_000, 1_000, 7, at(i64::MAX, Some(7))),
            Timer::Served
        );
    }

    #[test]
    fn time_before_the_start_or_another_boot_restarts_the_lockout() {
        let restart = Timer::Restart { duration: 30_000 };
        assert_eq!(read_timer(30_000, 1_000, 7, at(999, Some(7))), restart);
        assert_eq!(read_timer(30_000, 1_000, 7, at(5_000, Some(8))), restart);
        assert_eq!(
            read_timer(30_000, 1_000, UNKNOWN_BOOT, at(999, None)),
            restart
        );
    }

    #[test]
    fn an_unknown_boot_count_on_either_side_isnt_a_reboot() {
        let running = Timer::Running { remaining: 26_000 };
        assert_eq!(
            read_timer(30_000, 1_000, UNKNOWN_BOOT, at(5_000, Some(8))),
            running
        );
        assert_eq!(read_timer(30_000, 1_000, 7, at(5_000, None)), running);
        assert_eq!(
            read_timer(30_000, 1_000, 7, at(5_000, Some(UNKNOWN_BOOT))),
            running
        );
    }

    #[test]
    fn extreme_stored_values_never_overflow() {
        assert_eq!(
            read_timer(i64::MAX, i64::MAX, 7, at(i64::MAX, Some(7))),
            Timer::Running {
                remaining: u64::try_from(i64::MAX).unwrap()
            }
        );
        assert_eq!(
            read_timer(i64::MAX, i64::MIN, 7, at(i64::MAX, Some(7))),
            Timer::Served
        );
    }
}
