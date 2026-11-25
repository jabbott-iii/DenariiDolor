/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Stand-ins for the Keystore and the random source, as v1's `SecurityProfileFakes.kt` had them.
//! Each is a handle: clones share one state, so a test keeps one and gives the profile the other.

use std::sync::atomic::{AtomicBool, AtomicU32, Ordering};
use std::sync::{Arc, Mutex, MutexGuard, PoisonError};

use denarii_keystore::KeystoreError;
use denarii_vault::{
    DeviceKey, Iterations, MonotonicTime, OsRandom, ProfileStore, RandomSource, RandomUnavailable,
    SecurityProfile, SignIn, VaultError,
};
use hmac::{Hmac, KeyInit, Mac};
use serde_json::Value;
use sha2::Sha256;
use zeroize::Zeroizing;

/// v1's `FakeDeviceKeyMixer` key: 32 bytes of 1.
pub(crate) const V1_DEVICE_KEY: [u8; 32] = [1; 32];

/// A software HMAC-SHA256 standing in for the Keystore's device key.
#[derive(Clone)]
pub(crate) struct FakeDeviceKey(Arc<DeviceState>);

struct DeviceState {
    key: [u8; 32],
    available: AtomicBool,
    can_create: AtomicBool,
    prepared: AtomicU32,
}

impl FakeDeviceKey {
    pub(crate) fn new(key: [u8; 32]) -> Self {
        Self(Arc::new(DeviceState {
            key,
            available: AtomicBool::new(true),
            can_create: AtomicBool::new(true),
            prepared: AtomicU32::new(0),
        }))
    }

    pub(crate) fn v1() -> Self {
        Self::new(V1_DEVICE_KEY)
    }

    /// Makes every later HMAC fail as the Keystore does when the key is gone.
    pub(crate) fn lose(&self) {
        self.0.available.store(false, Ordering::SeqCst);
    }

    /// Makes every later attempt to create the key fail, as the Keystore can.
    pub(crate) fn fail_to_create(&self) {
        self.0.can_create.store(false, Ordering::SeqCst);
    }

    /// How many times the profile asked for the key to be created.
    pub(crate) fn times_prepared(&self) -> u32 {
        self.0.prepared.load(Ordering::SeqCst)
    }
}

impl DeviceKey for FakeDeviceKey {
    fn prepare(&self) -> Result<(), KeystoreError> {
        self.0.prepared.fetch_add(1, Ordering::SeqCst);
        if self.0.can_create.load(Ordering::SeqCst) {
            Ok(())
        } else {
            Err(KeystoreError::Failed)
        }
    }

    fn mix(&self, message: &[u8]) -> Result<Zeroizing<[u8; 32]>, KeystoreError> {
        if !self.0.available.load(Ordering::SeqCst) {
            return Err(KeystoreError::KeyMissing);
        }
        hmac_sha256(&self.0.key, &[message])
            .map(Zeroizing::new)
            .ok_or(KeystoreError::Unexpected)
    }
}

/// HMAC-SHA256 of the concatenated `parts` under `key`.
pub(crate) fn hmac_sha256(key: &[u8], parts: &[&[u8]]) -> Option<[u8; 32]> {
    let mut mac = <Hmac<Sha256> as KeyInit>::new_from_slice(key).ok()?;
    for part in parts {
        mac.update(part);
    }
    Some(mac.finalize().into_bytes().into())
}

/// The sealed profile, kept as plaintext in memory.
#[derive(Clone, Default)]
pub(crate) struct MemoryStore(Arc<Mutex<StoreState>>);

#[derive(Default)]
struct StoreState {
    plaintext: Option<Vec<u8>>,
    read_error: Option<KeystoreError>,
    write_error: Option<KeystoreError>,
    writes: u32,
}

impl MemoryStore {
    pub(crate) fn holding(plaintext: &[u8]) -> Self {
        let store = Self::default();
        store.state().plaintext = Some(plaintext.to_vec());
        store
    }

    pub(crate) fn holding_json(value: &Value) -> Self {
        Self::holding(value.to_string().as_bytes())
    }

    /// The stored plaintext.
    pub(crate) fn plaintext(&self) -> Option<Vec<u8>> {
        self.state().plaintext.clone()
    }

    /// The stored plaintext parsed as JSON, or `null` when there is none or it isn't JSON.
    pub(crate) fn json(&self) -> Value {
        self.plaintext()
            .and_then(|plaintext| serde_json::from_slice(&plaintext).ok())
            .unwrap_or(Value::Null)
    }

    /// Makes every later read fail with `error`.
    pub(crate) fn fail_reads(&self, error: KeystoreError) {
        self.state().read_error = Some(error);
    }

    /// Makes every later write fail with `error`, or succeed again with `None`.
    pub(crate) fn fail_writes(&self, error: Option<KeystoreError>) {
        self.state().write_error = error;
    }

    /// How many writes succeeded.
    pub(crate) fn writes(&self) -> u32 {
        self.state().writes
    }

    fn state(&self) -> MutexGuard<'_, StoreState> {
        self.0.lock().unwrap_or_else(PoisonError::into_inner)
    }
}

impl ProfileStore for MemoryStore {
    fn read(&self) -> Result<Option<Zeroizing<Vec<u8>>>, KeystoreError> {
        let state = self.state();
        match state.read_error {
            Some(error) => Err(error),
            None => Ok(state.plaintext.clone().map(Zeroizing::new)),
        }
    }

    fn write(&self, plaintext: &[u8]) -> Result<(), KeystoreError> {
        let mut state = self.state();
        if let Some(error) = state.write_error {
            return Err(error);
        }
        state.plaintext = Some(plaintext.to_vec());
        state.writes = state.writes.saturating_add(1);
        Ok(())
    }
}

/// Hands out the bytes 00, 01, 02, … in order, so that keys, salts, and IVs are known. Never
/// random: tests only, as v1's `CountingSecureRandom`.
#[derive(Debug, Default)]
pub(crate) struct CountingRandom {
    next: u8,
}

impl RandomSource for CountingRandom {
    fn fill(&mut self, bytes: &mut [u8]) -> Result<(), RandomUnavailable> {
        for byte in bytes {
            *byte = self.next;
            self.next = self.next.wrapping_add(1);
        }
        Ok(())
    }
}

/// A random source that always fails.
#[derive(Debug, Default)]
pub(crate) struct FailingRandom;

impl RandomSource for FailingRandom {
    fn fill(&mut self, _: &mut [u8]) -> Result<(), RandomUnavailable> {
        Err(RandomUnavailable)
    }
}

/// v1's `FakeMonotonicClock` start: 1,000,000 ms after boot 7.
pub(crate) const START: MonotonicTime = MonotonicTime {
    elapsed_millis: 1_000_000,
    boot_count: Some(7),
};

/// `now`, `millis` later.
pub(crate) fn later(now: MonotonicTime, millis: i64) -> MonotonicTime {
    MonotonicTime {
        elapsed_millis: now.elapsed_millis.saturating_add(millis),
        ..now
    }
}

/// A profile over fakes with the operating system's random bytes and the fewest iterations, as
/// v1's service tests used to keep them fast.
pub(crate) fn profile(
    device_key: &FakeDeviceKey,
    store: &MemoryStore,
) -> SecurityProfile<FakeDeviceKey, MemoryStore> {
    SecurityProfile::with_random(device_key.clone(), store.clone(), OsRandom, Iterations::MIN)
}

/// The key a PIN unwrapped, or `None` for any other result.
pub(crate) fn unlocked(result: Result<SignIn, VaultError>) -> Option<[u8; 32]> {
    match result {
        Ok(SignIn::Unlocked(key)) => Some(*key.as_bytes()),
        Ok(SignIn::InvalidPinFormat | SignIn::Rejected { .. } | SignIn::LockedOut { .. })
        | Err(_) => None,
    }
}

/// What a sign-in returned, without the key, so that tests can compare it.
#[derive(Debug, PartialEq, Eq)]
pub(crate) enum Outcome {
    Unlocked,
    InvalidPinFormat,
    Rejected(u32),
    LockedOut(u64),
    Failed(VaultError),
}

pub(crate) fn outcome(result: Result<SignIn, VaultError>) -> Outcome {
    result.map_or_else(Outcome::Failed, |sign_in| match sign_in {
        SignIn::Unlocked(_) => Outcome::Unlocked,
        SignIn::InvalidPinFormat => Outcome::InvalidPinFormat,
        SignIn::Rejected {
            attempts_before_lockout,
        } => Outcome::Rejected(attempts_before_lockout),
        SignIn::LockedOut { remaining_millis } => Outcome::LockedOut(remaining_millis),
    })
}

/// `bytes` in `java.util.Base64`'s basic encoding, which v1 stored byte strings in: the standard
/// alphabet, padded. Written apart from the crate's own encoder, so that the tests check it.
pub(crate) fn base64(bytes: &[u8]) -> String {
    const ALPHABET: &[u8; 64] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    let symbol = |sextet: u32| {
        usize::try_from(sextet & 63)
            .ok()
            .and_then(|index| ALPHABET.get(index))
            .map_or('?', |&symbol| char::from(symbol))
    };
    let mut text = String::new();
    for group in bytes.chunks(3) {
        let byte = |index| group.get(index).copied().unwrap_or(0);
        let bits = u32::from_be_bytes([0, byte(0), byte(1), byte(2)]);
        for (index, shift) in [18, 12, 6, 0].into_iter().enumerate() {
            text.push(if index <= group.len() {
                symbol(bits >> shift)
            } else {
                '='
            });
        }
    }
    text
}
