/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The security profile of Denarii Dolor: the database key wrapped under the PIN and under the
//! security answer, the rules for both, and the lockout that guards them.
//!
//! v1, the Kotlin app, stored this profile on every phone, and the new app must open it as v1
//! left it (ADR-0002, existing installs upgrade in place). This crate ports v1's
//! `SecurityProfileService` byte for byte (`docs/specs/android-v1-formats.md`, section 3), with
//! PBKDF2, HMAC, and AES-GCM from the RustCrypto crates (ADR-0023). It reaches the Keystore's
//! device key and sealed profile through `denarii-keystore`, and Kotlin's whitespace through
//! `denarii-core`. Its design is in `docs/specs/vault.md`.
//!
//! # The parts
//!
//! - [`SecurityProfile`]: setup ([`SecurityProfile::set_up`]), sign-in
//!   ([`SecurityProfile::unlock_with_pin`]), PIN recovery ([`SecurityProfile::recover_pin`]),
//!   and the lockout ([`SecurityProfile::lockout_remaining_millis`]), with their results
//!   [`ProfileState`], [`Setup`], [`SignIn`], and [`Recovery`], and [`DatabaseKey`], the key a
//!   PIN unwraps.
//! - What the profile needs from the platform: a [`DeviceKey`] and a [`ProfileStore`], which
//!   [`Keystore`](denarii_keystore::Keystore) implements with v1's keys, and a
//!   [`RandomSource`], [`OsRandom`] in the app.
//! - The rules (CS-10, CS-14): [`is_valid_pin`], [`normalize_answer`], [`validate_setup`], and
//!   [`SetupProblem`].
//! - The lockout (CS-13): [`MonotonicTime`], the time it runs on, and
//!   [`lockout_duration_millis`].
//! - [`Iterations`], the PBKDF2 iteration counts a slot may have, and [`VaultError`].
//!
//! # Example
//!
//! On Android, the app makes the profile over v1's Keystore keys and signs in with the time since
//! boot. Off Android the Keystore is unreachable, so this example only compiles:
//!
//! ```no_run
//! use denarii_keystore::Keystore;
//! use denarii_vault::{MonotonicTime, ProfileState, SecurityProfile, SignIn};
//!
//! # fn main() -> Result<(), Box<dyn std::error::Error>> {
//! let profile = SecurityProfile::new(Keystore::vault(), Keystore::vault());
//! if let ProfileState::Configured { security_question } = profile.state()? {
//!     let now = MonotonicTime { elapsed_millis: 86_400_000, boot_count: Some(12) };
//!     match profile.unlock_with_pin("135790", now)? {
//!         SignIn::Unlocked(key) => { /* open the database with key.as_bytes() */ }
//!         SignIn::Rejected { attempts_before_lockout } => { /* ask again */ }
//!         SignIn::LockedOut { remaining_millis } => { /* wait */ }
//!         SignIn::InvalidPinFormat => { /* 6 to 12 digits */ }
//!     }
//! }
//! # Ok(())
//! # }
//! ```
//!
//! # What callers must know
//!
//! - Nothing here reads a clock. Each call that runs under the lockout takes a [`MonotonicTime`],
//!   which on Android must be `SystemClock.elapsedRealtime()` and `Settings.Global.BOOT_COUNT`
//!   (CS-13), never the wall clock.
//! - A [`SecurityProfile`] is `Send` and `Sync` when its parts are, and each call holds its lock
//!   for the whole call: PBKDF2 at 210,000 iterations takes a noticeable time, so call it off the
//!   UI thread.
//! - PINs, answers, and keys are wiped from memory once used, with `zeroize`, as far as this crate
//!   holds them. The caller's own strings are the caller's to wipe.
//! - No call panics on any input or stored value. A stored profile that is damaged is an error,
//!   never a reason to start over: deleting the database is the caller's decision.

mod base64;
mod crypto;
mod error;
mod key;
mod lockout;
mod profile;
mod random;
mod rules;
mod storage;
mod stored;

pub use error::VaultError;
pub use key::DatabaseKey;
pub use lockout::{
    BASE_LOCKOUT_MILLIS, MAX_FREE_ATTEMPTS, MAX_LOCKOUT_MILLIS, MonotonicTime,
    lockout_duration_millis,
};
pub use profile::{Iterations, ProfileState, Recovery, SecurityProfile, Setup, SignIn};
pub use random::{OsRandom, RandomSource, RandomUnavailable};
pub use rules::{
    MAX_ANSWER_UNITS, MAX_PIN_DIGITS, MAX_QUESTION_UNITS, MIN_ANSWER_CHARS, MIN_PIN_DIGITS,
    SetupProblem, is_valid_pin, normalize_answer, validate_setup,
};
pub use storage::{DeviceKey, ProfileStore};
