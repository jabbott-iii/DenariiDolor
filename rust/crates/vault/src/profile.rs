/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The security profile (CS-15): the database key wrapped under the PIN and under the
//! normalized answer, the setup and recovery rules, and the shared lockout (CS-13). It ports v1's
//! `SecurityProfileService` (`docs/specs/android-v1-formats.md`, section 3).

use std::sync::{Mutex, MutexGuard};

use denarii_core::is_blank;
use zeroize::Zeroizing;

use crate::base64;
use crate::crypto::{self, KEY_LEN, NONCE_LEN};
use crate::error::VaultError;
use crate::key::DatabaseKey;
use crate::lockout::{self, MAX_FREE_ATTEMPTS, MonotonicTime, Timer};
use crate::random::{OsRandom, RandomSource};
use crate::rules::{SetupProblem, is_valid_pin, normalize_answer, validate_setup};
use crate::storage::{DeviceKey, ProfileStore};
use crate::stored::{
    CURRENT_VERSION, FAILED_ATTEMPTS, LOCK_BOOT_COUNT, LOCK_DURATION, LOCK_STARTED_AT,
    PROFILE_VERSION, ProfileValues, SECURITY_QUESTION,
};

/// The length of each slot's salt, in bytes.
const SALT_LEN: usize = 16;

/// A PBKDF2 iteration count that v1 accepted in a stored slot: 10,000 to 1,000,000.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct Iterations(u32);

impl Iterations {
    /// The fewest iterations a stored slot may have.
    pub const MIN: Self = Self(10_000);
    /// The most iterations a stored slot may have.
    pub const MAX: Self = Self(1_000_000);
    /// The iterations a new slot gets: 210,000, as v1 wrote them.
    pub const DEFAULT: Self = Self(210_000);

    /// `count` as an iteration count, or `None` when it is outside [`Iterations::MIN`] to
    /// [`Iterations::MAX`].
    #[must_use]
    pub const fn new(count: u32) -> Option<Self> {
        if count >= Self::MIN.0 && count <= Self::MAX.0 {
            Some(Self(count))
        } else {
            None
        }
    }

    /// The count.
    #[must_use]
    pub const fn get(self) -> u32 {
        self.0
    }
}

/// Whether the profile is set up, and the question to ask when it is.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum ProfileState {
    /// No profile is set up: none is stored, or what is stored isn't a whole version 2 profile.
    /// v1 then offered setup, and setup deleted the database as an orphan
    /// (`docs/specs/android-v1-formats.md`, open question 7).
    NeedsSetup,
    /// A profile is set up, and sign-in can start.
    Configured {
        /// The security question, as it was stored.
        security_question: String,
    },
}

/// The result of [`SecurityProfile::set_up`].
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Setup {
    /// The new profile is stored around a new random database key. The database itself is
    /// created at the first sign-in.
    Saved,
    /// A profile is already set up; nothing changed.
    AlreadyConfigured,
    /// The input breaks a setup rule; nothing changed.
    Invalid(SetupProblem),
}

/// The result of [`SecurityProfile::unlock_with_pin`].
#[derive(Debug)]
pub enum SignIn {
    /// The PIN opened its slot.
    Unlocked(DatabaseKey),
    /// The PIN isn't 6 to 12 ASCII digits (CS-10). It wasn't checked, and it isn't counted as a
    /// failure, as v1's sign-in screen refused it before it reached the profile.
    InvalidPinFormat,
    /// The PIN is wrong.
    Rejected {
        /// How many more wrong PINs or answers start a lockout.
        attempts_before_lockout: u32,
    },
    /// Sign-in is locked, by this failure or an earlier one, and the PIN wasn't checked if the
    /// lockout was already running.
    LockedOut {
        /// How long the lockout has left, in milliseconds.
        remaining_millis: u64,
    },
}

/// The result of [`SecurityProfile::recover_pin`], in the order v1 checked.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Recovery {
    /// The answer opened its slot, and the new PIN now wraps the same database key. The answer's
    /// slot is unchanged.
    Recovered,
    /// No profile is set up.
    NotConfigured,
    /// Sign-in is locked, by this failure or an earlier one.
    LockedOut {
        /// How long the lockout has left, in milliseconds.
        remaining_millis: u64,
    },
    /// The new PIN isn't 6 to 12 ASCII digits.
    InvalidPinFormat,
    /// The confirmation isn't the same as the new PIN.
    PinMismatch,
    /// The answer is empty once normalized.
    SecurityAnswerRequired,
    /// The answer is wrong. It counts toward the lockout that wrong PINs start.
    InvalidSecurityAnswer,
}

/// The outcome of a check that runs under the shared lockout.
enum Attempt<T> {
    Accepted(T),
    Rejected { attempts_before_lockout: u32 },
    LockedOut { remaining_millis: u64 },
}

/// A slot that wraps the database key under one secret.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum Slot {
    Pin,
    Answer,
}

impl Slot {
    const ALL: [Self; 2] = [Self::Pin, Self::Answer];

    const fn wrap_key(self) -> &'static str {
        match self {
            Self::Pin => "pin_wrap",
            Self::Answer => "answer_wrap",
        }
    }

    const fn salt_key(self) -> &'static str {
        match self {
            Self::Pin => "pin_salt",
            Self::Answer => "answer_salt",
        }
    }

    const fn iterations_key(self) -> &'static str {
        match self {
            Self::Pin => "pin_iterations",
            Self::Answer => "answer_iterations",
        }
    }

    /// The slot's context, the prefix of its device-key input and its AES-GCM associated data.
    const fn context(self) -> &'static [u8] {
        match self {
            Self::Pin => b"denarii-dolor:pin:v2",
            Self::Answer => b"denarii-dolor:answer:v2",
        }
    }
}

/// The database key sealed into one slot, ready to store.
struct Sealed {
    slot: Slot,
    salt: [u8; SALT_LEN],
    iterations: Iterations,
    /// The IV followed by AES-GCM's ciphertext and tag.
    wrapped: Vec<u8>,
}

impl Sealed {
    fn store_in(&self, values: &mut ProfileValues) {
        values.put_string(self.slot.wrap_key(), &base64::encode(&self.wrapped));
        values.put_string(self.slot.salt_key(), &base64::encode(&self.salt));
        values.put_long(self.slot.iterations_key(), i64::from(self.iterations.get()));
    }
}

/// What the lock guards: the profile's values as last read or written, and the random source.
struct State<R> {
    cache: Option<ProfileValues>,
    random: R,
}

/// The single user's security profile (CS-15).
///
/// The database key exists on disk only wrapped: once under the PIN and once under the
/// normalized security answer, each with AES-256-GCM under a key-encryption key that PBKDF2 and
/// the [`DeviceKey`] make from the secret. Failed PIN and answer checks share an escalating
/// lockout on a [`MonotonicTime`] (CS-13).
///
/// Every method holds one lock for its whole call, PBKDF2 included, as v1's methods were
/// `@Synchronized`, so two checks never run at once to get around the lockout. Each change to
/// the profile writes the whole of it through the [`ProfileStore`], and changes the copy this
/// type keeps only once the write succeeded.
pub struct SecurityProfile<D, S, R = OsRandom> {
    device_key: D,
    store: S,
    iterations: Iterations,
    state: Mutex<State<R>>,
}

impl<D: DeviceKey, S: ProfileStore> SecurityProfile<D, S> {
    /// The profile that `store` holds, with `device_key`, the operating system's random bytes,
    /// and [`Iterations::DEFAULT`] for each new slot. The app makes it with
    /// [`Keystore::vault`](denarii_keystore::Keystore::vault) for both.
    #[must_use]
    pub fn new(device_key: D, store: S) -> Self {
        Self::with_random(device_key, store, OsRandom, Iterations::DEFAULT)
    }
}

impl<D: DeviceKey, S: ProfileStore, R: RandomSource> SecurityProfile<D, S, R> {
    /// The profile that `store` holds, with random bytes from `random` and `iterations` for each
    /// new slot. Stored slots open with their own iteration counts.
    ///
    /// v1's golden tests used a source that counts up, and 10,000 iterations, so that a whole
    /// profile has known bytes; the app uses [`SecurityProfile::new`].
    #[must_use]
    pub fn with_random(device_key: D, store: S, random: R, iterations: Iterations) -> Self {
        Self {
            device_key,
            store,
            iterations,
            state: Mutex::new(State {
                cache: None,
                random,
            }),
        }
    }

    /// Whether a profile is set up, and its question.
    ///
    /// # Errors
    ///
    /// [`VaultError::Keystore`] when the stored profile can't be read,
    /// [`VaultError::ProfileUnreadable`] when it isn't a JSON object, or [`VaultError::Poisoned`].
    pub fn state(&self) -> Result<ProfileState, VaultError> {
        let mut state = self.lock()?;
        let values = self.values(&mut state)?;
        Ok(
            match (is_configured(values), values.string(SECURITY_QUESTION)) {
                (true, Some(question)) => ProfileState::Configured {
                    security_question: question.to_owned(),
                },
                _ => ProfileState::NeedsSetup,
            },
        )
    }

    /// Sets up a new profile around a new random database key, after the setup rules
    /// ([`validate_setup`]). The question is stored trimmed, and the answer's slot opens with the
    /// normalized answer.
    ///
    /// # Errors
    ///
    /// [`VaultError::Keystore`] when the device key can't be created or the profile can't be
    /// read or written, [`VaultError::ProfileUnreadable`], [`VaultError::RandomUnavailable`], or
    /// [`VaultError::Poisoned`]. Nothing is stored then.
    pub fn set_up(
        &self,
        pin: &str,
        confirmation: &str,
        question: &str,
        answer: &str,
    ) -> Result<Setup, VaultError> {
        let mut state = self.lock()?;
        if is_configured(self.values(&mut state)?) {
            return Ok(Setup::AlreadyConfigured);
        }
        if let Err(problem) = validate_setup(pin, confirmation, question, answer) {
            return Ok(Setup::Invalid(problem));
        }
        let mut key = [0; DatabaseKey::LEN];
        random_bytes(&mut state.random, &mut key)?;
        let key = DatabaseKey::from_bytes(key);
        self.device_key.prepare()?;
        let pin_slot = self.seal(&mut state, Slot::Pin, pin, &key)?;
        let answer_slot = self.seal(&mut state, Slot::Answer, &normalize_answer(answer), &key)?;
        self.edit(&mut state, |values| {
            values.clear();
            values.put_int(PROFILE_VERSION, CURRENT_VERSION);
            values.put_string(
                SECURITY_QUESTION,
                question.trim_matches(denarii_core::is_whitespace),
            );
            pin_slot.store_in(values);
            answer_slot.store_in(values);
        })?;
        Ok(Setup::Saved)
    }

    /// Unwraps the database key with `pin`, under the shared lockout. A wrong PIN counts as a
    /// failure; one that isn't 6 to 12 digits is refused without a check.
    ///
    /// # Errors
    ///
    /// - [`VaultError::NotConfigured`] when no profile is set up.
    /// - [`VaultError::Keystore`] when the device key or the profile fails, for example
    ///   [`KeystoreError::KeyMissing`](denarii_keystore::KeystoreError::KeyMissing) when the
    ///   device key is gone. That isn't counted as a wrong PIN.
    /// - [`VaultError::SlotDamaged`], [`VaultError::ProfileUnreadable`], or
    ///   [`VaultError::Poisoned`].
    pub fn unlock_with_pin(&self, pin: &str, now: MonotonicTime) -> Result<SignIn, VaultError> {
        if !is_valid_pin(pin) {
            return Ok(SignIn::InvalidPinFormat);
        }
        let mut state = self.lock()?;
        if !is_configured(self.values(&mut state)?) {
            return Err(VaultError::NotConfigured);
        }
        Ok(
            match self.attempt(&mut state, now, |profile, state| {
                profile.open(state, Slot::Pin, pin)
            })? {
                Attempt::Accepted(key) => SignIn::Unlocked(key),
                Attempt::Rejected {
                    attempts_before_lockout,
                } => SignIn::Rejected {
                    attempts_before_lockout,
                },
                Attempt::LockedOut { remaining_millis } => SignIn::LockedOut { remaining_millis },
            },
        )
    }

    /// Replaces the PIN, when `answer` opens the answer's slot under the shared lockout. The
    /// database key and the answer's slot stay the same.
    ///
    /// # Errors
    ///
    /// [`VaultError::Keystore`], [`VaultError::SlotDamaged`], [`VaultError::ProfileUnreadable`],
    /// [`VaultError::RandomUnavailable`], or [`VaultError::Poisoned`], as
    /// [`SecurityProfile::unlock_with_pin`] and [`SecurityProfile::set_up`] describe them. A wrong
    /// answer is [`Recovery::InvalidSecurityAnswer`], not an error.
    pub fn recover_pin(
        &self,
        answer: &str,
        new_pin: &str,
        confirmation: &str,
        now: MonotonicTime,
    ) -> Result<Recovery, VaultError> {
        let answer = normalize_answer(answer);
        let mut state = self.lock()?;
        if !is_configured(self.values(&mut state)?) {
            return Ok(Recovery::NotConfigured);
        }
        let remaining_millis = self.lockout_remaining(&mut state, now)?;
        if remaining_millis > 0 {
            return Ok(Recovery::LockedOut { remaining_millis });
        }
        if !is_valid_pin(new_pin) {
            return Ok(Recovery::InvalidPinFormat);
        }
        if new_pin != confirmation {
            return Ok(Recovery::PinMismatch);
        }
        if answer.is_empty() {
            return Ok(Recovery::SecurityAnswerRequired);
        }
        match self.attempt(&mut state, now, |profile, state| {
            profile.open(state, Slot::Answer, &answer)
        })? {
            Attempt::Accepted(key) => {
                let pin_slot = self.seal(&mut state, Slot::Pin, new_pin, &key)?;
                self.edit(&mut state, |values| pin_slot.store_in(values))?;
                Ok(Recovery::Recovered)
            }
            Attempt::Rejected { .. } => Ok(Recovery::InvalidSecurityAnswer),
            Attempt::LockedOut { remaining_millis } => Ok(Recovery::LockedOut { remaining_millis }),
        }
    }

    /// How long the lockout has left at `now`, in milliseconds, or 0 when there is none.
    ///
    /// Like v1, this also updates the stored timer: after a reboot it restarts the whole lockout
    /// from `now`, and once the lockout is served it clears the timer. The failure count stays
    /// until a check succeeds, so the next failure locks for twice as long.
    ///
    /// # Errors
    ///
    /// [`VaultError::Keystore`] when the profile can't be read or written,
    /// [`VaultError::ProfileUnreadable`], or [`VaultError::Poisoned`].
    pub fn lockout_remaining_millis(&self, now: MonotonicTime) -> Result<u64, VaultError> {
        let mut state = self.lock()?;
        self.lockout_remaining(&mut state, now)
    }

    fn lock(&self) -> Result<MutexGuard<'_, State<R>>, VaultError> {
        self.state.lock().map_err(|_| VaultError::Poisoned)
    }

    /// The profile's values, read from the store the first time.
    fn values<'s>(&self, state: &'s mut State<R>) -> Result<&'s ProfileValues, VaultError> {
        let values = match state.cache.take() {
            Some(values) => values,
            None => match self.store.read()? {
                None => ProfileValues::default(),
                Some(json) => ProfileValues::parse(&json).ok_or(VaultError::ProfileUnreadable)?,
            },
        };
        Ok(state.cache.insert(values))
    }

    /// Writes the values as `change` leaves them, and keeps them once the write succeeded.
    fn edit(
        &self,
        state: &mut State<R>,
        change: impl FnOnce(&mut ProfileValues),
    ) -> Result<(), VaultError> {
        let mut values = self.values(state)?.clone();
        change(&mut values);
        self.store.write(&values.to_json())?;
        state.cache = Some(values);
        Ok(())
    }

    /// Runs `verify` under the shared lockout: `None` is a failed check, and a key resets the
    /// count. An error from `verify` isn't counted.
    fn attempt<T>(
        &self,
        state: &mut State<R>,
        now: MonotonicTime,
        verify: impl FnOnce(&Self, &mut State<R>) -> Result<Option<T>, VaultError>,
    ) -> Result<Attempt<T>, VaultError> {
        let remaining_millis = self.lockout_remaining(state, now)?;
        if remaining_millis > 0 {
            return Ok(Attempt::LockedOut { remaining_millis });
        }
        if let Some(value) = verify(self, state)? {
            self.edit(state, |values| {
                values.remove(FAILED_ATTEMPTS);
                remove_timer(values);
            })?;
            return Ok(Attempt::Accepted(value));
        }
        let failures = self.register_failure(state, now)?;
        let remaining_millis = self.lockout_remaining(state, now)?;
        if remaining_millis > 0 {
            return Ok(Attempt::LockedOut { remaining_millis });
        }
        let attempts_before_lockout = i64::from(MAX_FREE_ATTEMPTS)
            .checked_sub(i64::from(failures))
            .and_then(|left| u32::try_from(left).ok())
            .unwrap_or(0);
        Ok(Attempt::Rejected {
            attempts_before_lockout,
        })
    }

    /// Counts one more failure, and starts its lockout, if it has one. Returns the new count.
    fn register_failure(
        &self,
        state: &mut State<R>,
        now: MonotonicTime,
    ) -> Result<i32, VaultError> {
        let failures = self
            .values(state)?
            .int(FAILED_ATTEMPTS)
            .unwrap_or(0)
            .saturating_add(1);
        let duration = u32::try_from(failures).map_or(0, lockout::lockout_duration_millis);
        self.edit(state, |values| {
            values.put_int(FAILED_ATTEMPTS, failures);
            if let Ok(duration) = i64::try_from(duration)
                && duration > 0
            {
                values.put_long(LOCK_DURATION, duration);
                values.put_long(LOCK_STARTED_AT, now.elapsed_millis);
                values.put_int(LOCK_BOOT_COUNT, now.stored_boot_count());
            }
        })?;
        Ok(failures)
    }

    fn lockout_remaining(
        &self,
        state: &mut State<R>,
        now: MonotonicTime,
    ) -> Result<u64, VaultError> {
        let values = self.values(state)?;
        let timer = lockout::read_timer(
            values.long(LOCK_DURATION).unwrap_or(0),
            values.long(LOCK_STARTED_AT).unwrap_or(0),
            values.int(LOCK_BOOT_COUNT).unwrap_or(lockout::UNKNOWN_BOOT),
            now,
        );
        match timer {
            Timer::Off => Ok(0),
            Timer::Running { remaining } => Ok(remaining),
            Timer::Served => {
                self.edit(state, remove_timer)?;
                Ok(0)
            }
            Timer::Restart { duration } => {
                self.edit(state, |values| {
                    values.put_long(LOCK_STARTED_AT, now.elapsed_millis);
                    values.put_int(LOCK_BOOT_COUNT, now.stored_boot_count());
                })?;
                Ok(duration)
            }
        }
    }

    /// Wraps `key` into `slot` under `secret`, with a new salt and IV, in that order.
    fn seal(
        &self,
        state: &mut State<R>,
        slot: Slot,
        secret: &str,
        key: &DatabaseKey,
    ) -> Result<Sealed, VaultError> {
        let mut salt = [0; SALT_LEN];
        random_bytes(&mut state.random, &mut salt)?;
        let mut iv = [0; NONCE_LEN];
        random_bytes(&mut state.random, &mut iv)?;
        let kek = self.key_encryption_key(slot, secret, &salt, self.iterations)?;
        let mut wrapped = iv.to_vec();
        wrapped.extend(crypto::aes_gcm_seal(
            &kek,
            &iv,
            slot.context(),
            key.as_bytes(),
        ));
        Ok(Sealed {
            slot,
            salt,
            iterations: self.iterations,
            wrapped,
        })
    }

    /// The database key that `slot` wraps, or `None` when `secret` doesn't open it.
    fn open(
        &self,
        state: &mut State<R>,
        slot: Slot,
        secret: &str,
    ) -> Result<Option<DatabaseKey>, VaultError> {
        let values = self.values(state)?;
        let stored = |key| values.string(key).and_then(base64::decode);
        let wrapped = stored(slot.wrap_key()).ok_or(VaultError::SlotDamaged)?;
        let salt = stored(slot.salt_key()).ok_or(VaultError::SlotDamaged)?;
        let iterations = iterations(values, slot).ok_or(VaultError::SlotDamaged)?;
        let (iv, sealed) = wrapped
            .split_first_chunk::<NONCE_LEN>()
            .filter(|(_, sealed)| !sealed.is_empty())
            .ok_or(VaultError::SlotDamaged)?;
        let kek = self.key_encryption_key(slot, secret, &salt, iterations)?;
        let Some(opened) = crypto::aes_gcm_open(&kek, iv, slot.context(), sealed) else {
            return Ok(None);
        };
        let key = <[u8; DatabaseKey::LEN]>::try_from(opened.as_slice())
            .map_err(|_| VaultError::SlotDamaged)?;
        Ok(Some(DatabaseKey::from_bytes(key)))
    }

    /// `HMAC-SHA256(device key, context || PBKDF2-HMAC-SHA256(secret, salt, iterations))`, the
    /// key that seals and opens `slot` (`docs/specs/android-v1-formats.md`, invariant 4).
    fn key_encryption_key(
        &self,
        slot: Slot,
        secret: &str,
        salt: &[u8],
        iterations: Iterations,
    ) -> Result<Zeroizing<[u8; KEY_LEN]>, VaultError> {
        let stretched = crypto::pbkdf2_hmac_sha256(secret, salt, iterations.get());
        let mut message = Zeroizing::new(Vec::with_capacity(
            slot.context().len().saturating_add(KEY_LEN),
        ));
        message.extend_from_slice(slot.context());
        message.extend_from_slice(stretched.as_slice());
        Ok(self.device_key.mix(&message)?)
    }
}

/// Whether `values` is a whole version 2 profile: the version, a question that isn't blank, and
/// for both slots a wrap and a salt that aren't blank and an iteration count in range. v1 checked
/// no more than this before it offered sign-in.
fn is_configured(values: &ProfileValues) -> bool {
    let present = |key| values.string(key).is_some_and(|text| !is_blank(text));
    values.int(PROFILE_VERSION) == Some(CURRENT_VERSION)
        && present(SECURITY_QUESTION)
        && Slot::ALL.into_iter().all(|slot| {
            present(slot.wrap_key())
                && present(slot.salt_key())
                && iterations(values, slot).is_some()
        })
}

/// The slot's stored iteration count, if it is a Kotlin `Int` in range.
fn iterations(values: &ProfileValues, slot: Slot) -> Option<Iterations> {
    let count = values.int(slot.iterations_key())?;
    Iterations::new(u32::try_from(count).ok()?)
}

fn remove_timer(values: &mut ProfileValues) {
    values.remove(LOCK_DURATION);
    values.remove(LOCK_STARTED_AT);
    values.remove(LOCK_BOOT_COUNT);
}

fn random_bytes(random: &mut impl RandomSource, bytes: &mut [u8]) -> Result<(), VaultError> {
    random
        .fill(bytes)
        .map_err(|_| VaultError::RandomUnavailable)
}
