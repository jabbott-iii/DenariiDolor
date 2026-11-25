/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! What the profile needs from the Keystore, as two traits: the device key that every PIN and
//! answer guess passes through, and the store of the sealed profile.
//!
//! [`Keystore`] implements both with v1's keys (`docs/specs/keystore.md`). The traits exist so
//! that the tests can stand in for the Keystore on the host, as v1's tests used a software HMAC
//! for `KeystoreDeviceKeyMixer` and a map for `KeystoreProfileStore`.

use denarii_keystore::{Keystore, KeystoreError};
use zeroize::Zeroizing;

/// A key that never leaves this device, which mixes each stretched PIN and answer, so that a
/// guess can be checked only on the device (CS-15).
pub trait DeviceKey {
    /// Creates the device key unless it exists. The profile calls it only before it writes a new
    /// profile, as v1 did.
    ///
    /// # Errors
    ///
    /// The [`KeystoreError`] that the Keystore gave.
    fn prepare(&self) -> Result<(), KeystoreError>;

    /// HMAC-SHA256 of `message` under the device key. It never creates the key.
    ///
    /// # Errors
    ///
    /// [`KeystoreError::KeyMissing`] when the device key doesn't exist, or another
    /// [`KeystoreError`] that the Keystore gave.
    fn mix(&self, message: &[u8]) -> Result<Zeroizing<[u8; 32]>, KeystoreError>;
}

/// Where the profile's plaintext is kept, sealed: on Android, the `profile` entry of
/// `shared_prefs/vault_profile.xml`, sealed under the Keystore's storage key.
pub trait ProfileStore {
    /// The plaintext, or `None` when no profile is stored.
    ///
    /// # Errors
    ///
    /// The [`KeystoreError`] that the Keystore gave, such as [`KeystoreError::Unreadable`] when
    /// the stored profile can't be opened.
    fn read(&self) -> Result<Option<Zeroizing<Vec<u8>>>, KeystoreError>;

    /// Seals `plaintext` and saves it in place of the stored profile.
    ///
    /// # Errors
    ///
    /// The [`KeystoreError`] that the Keystore gave. The stored profile is then as it was.
    fn write(&self, plaintext: &[u8]) -> Result<(), KeystoreError>;
}

/// The Keystore's device key, `<prefix>device`: [`Keystore::ensure_device_key`] and
/// [`Keystore::device_hmac`].
impl DeviceKey for Keystore {
    fn prepare(&self) -> Result<(), KeystoreError> {
        self.ensure_device_key()
    }

    fn mix(&self, message: &[u8]) -> Result<Zeroizing<[u8; 32]>, KeystoreError> {
        self.device_hmac(message).map(Zeroizing::new)
    }
}

/// The profile sealed under the Keystore's storage key: [`Keystore::read_profile`] and
/// [`Keystore::write_profile`].
impl ProfileStore for Keystore {
    fn read(&self) -> Result<Option<Zeroizing<Vec<u8>>>, KeystoreError> {
        Ok(self.read_profile()?.map(Zeroizing::new))
    }

    fn write(&self, plaintext: &[u8]) -> Result<(), KeystoreError> {
        self.write_profile(plaintext)
    }
}

#[cfg(test)]
mod tests {
    use denarii_keystore::{Keystore, KeystoreError};

    use super::{DeviceKey, ProfileStore};

    /// Off Android, each call reaches the Keystore crate, which has no Java runtime to call.
    #[test]
    fn the_keystore_is_unavailable_off_android() {
        let keystore = Keystore::new("denarii_test_vault_", "denarii_test_vault_profile").unwrap();
        assert_eq!(keystore.prepare(), Err(KeystoreError::Unavailable));
        assert_eq!(keystore.mix(b"message"), Err(KeystoreError::Unavailable));
        assert_eq!(keystore.read(), Err(KeystoreError::Unavailable));
        assert_eq!(keystore.write(b"{}"), Err(KeystoreError::Unavailable));
    }
}
