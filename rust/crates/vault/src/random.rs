/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Where the profile's random bytes come from: the database key, each salt, and each IV.
//!
//! A trait of its own, as v1's `CryptoPrimitives.randomBytes` was, so that the golden tests can
//! hand out known bytes and pin a whole profile byte for byte.

use std::fmt;

/// A source of random bytes for keys, salts, and IVs.
///
/// [`OsRandom`] is the only one the app uses. Any other source must be as unpredictable as the
/// operating system's, or the keys it makes can be guessed.
pub trait RandomSource {
    /// Fills `bytes` with random bytes.
    ///
    /// # Errors
    ///
    /// [`RandomUnavailable`] when the source can't give them.
    fn fill(&mut self, bytes: &mut [u8]) -> Result<(), RandomUnavailable>;
}

/// The operating system's cryptographically secure random source, through the `getrandom` crate:
/// the `getrandom` system call on Android and Linux, or `/dev/urandom` on a kernel without it,
/// and `CCRandomGenerateBytes` on iOS.
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Hash)]
pub struct OsRandom;

impl RandomSource for OsRandom {
    fn fill(&mut self, bytes: &mut [u8]) -> Result<(), RandomUnavailable> {
        getrandom::fill(bytes).map_err(|_| RandomUnavailable)
    }
}

/// A [`RandomSource`] couldn't give random bytes.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct RandomUnavailable;

impl fmt::Display for RandomUnavailable {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("the random source failed")
    }
}

impl std::error::Error for RandomUnavailable {}

#[cfg(test)]
mod tests {
    use super::{OsRandom, RandomSource};

    #[test]
    fn os_random_fills_with_different_bytes_each_time() {
        let mut first = [0_u8; 32];
        let mut second = [0_u8; 32];
        OsRandom.fill(&mut first).unwrap();
        OsRandom.fill(&mut second).unwrap();
        assert_ne!(first, [0; 32]);
        assert_ne!(first, second);
    }

    #[test]
    fn unavailable_says_what_failed() {
        assert_eq!(
            super::RandomUnavailable.to_string(),
            "the random source failed"
        );
    }
}
