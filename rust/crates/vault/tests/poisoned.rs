/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! A panic while the profile is locked leaves its state unknown, so every later call fails with
//! [`VaultError::Poisoned`] instead of trusting it. This file is its own test process, because it
//! silences the panic message, which is a setting for the whole process.

use std::panic::{self, AssertUnwindSafe};
use std::sync::{Mutex, MutexGuard, PoisonError};

use denarii_keystore::KeystoreError;
use denarii_vault::{
    DeviceKey, Iterations, MonotonicTime, OsRandom, ProfileStore, SecurityProfile, VaultError,
};
use zeroize::Zeroizing;

/// A device key whose HMAC panics, standing in for a bug below the profile.
struct PanickingDeviceKey;

impl DeviceKey for PanickingDeviceKey {
    fn prepare(&self) -> Result<(), KeystoreError> {
        Ok(())
    }

    #[expect(
        clippy::panic,
        reason = "the test needs a call that panics while the profile holds its lock"
    )]
    fn mix(&self, _: &[u8]) -> Result<Zeroizing<[u8; 32]>, KeystoreError> {
        panic!("the device key panicked");
    }
}

#[derive(Default)]
struct Store(Mutex<Option<Vec<u8>>>);

impl Store {
    fn plaintext(&self) -> MutexGuard<'_, Option<Vec<u8>>> {
        self.0.lock().unwrap_or_else(PoisonError::into_inner)
    }
}

impl ProfileStore for Store {
    fn read(&self) -> Result<Option<Zeroizing<Vec<u8>>>, KeystoreError> {
        Ok(self.plaintext().clone().map(Zeroizing::new))
    }

    fn write(&self, plaintext: &[u8]) -> Result<(), KeystoreError> {
        *self.plaintext() = Some(plaintext.to_vec());
        Ok(())
    }
}

#[test]
fn a_panic_inside_a_call_poisons_every_later_call() {
    let profile = SecurityProfile::with_random(
        PanickingDeviceKey,
        Store::default(),
        OsRandom,
        Iterations::MIN,
    );
    let now = MonotonicTime {
        elapsed_millis: 1_000,
        boot_count: Some(1),
    };

    panic::set_hook(Box::new(|_| {}));
    let panicked = panic::catch_unwind(AssertUnwindSafe(|| {
        profile.set_up("123456", "123456", "First pet?", "Rex the dog")
    }));
    drop(panic::take_hook());
    assert!(panicked.is_err());

    assert_eq!(profile.state(), Err(VaultError::Poisoned));
    assert_eq!(
        profile.set_up("123456", "123456", "First pet?", "Rex the dog"),
        Err(VaultError::Poisoned)
    );
    assert_eq!(
        profile.unlock_with_pin("123456", now).unwrap_err(),
        VaultError::Poisoned
    );
    assert_eq!(
        profile.recover_pin("Rex the dog", "654321", "654321", now),
        Err(VaultError::Poisoned)
    );
    assert_eq!(
        profile.lockout_remaining_millis(now),
        Err(VaultError::Poisoned)
    );
}
