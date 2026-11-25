/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The one error type of the crate.

use std::fmt;

use denarii_keystore::KeystoreError;

/// Why a [`SecurityProfile`] call failed, as opposed to a result such as a wrong PIN, which each
/// call returns as a value.
///
/// v1 reported every Keystore, storage, and damaged-profile failure as one
/// `SecureStorageException`, and its sign-in screen showed one message for all of them: that the
/// device's secure storage can't be read (BUG-05). [`VaultError::Keystore`],
/// [`VaultError::ProfileUnreadable`], and [`VaultError::SlotDamaged`] are those failures; the
/// others are bugs in the caller or the platform. Users never see this type's `Display` text.
///
/// [`SecurityProfile`]: crate::SecurityProfile
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[non_exhaustive]
pub enum VaultError {
    /// The device key or the sealed profile failed in the Keystore, for the reason the
    /// [`KeystoreError`] gives: for example [`KeystoreError::KeyMissing`] when the device key is
    /// gone, which is not a wrong PIN.
    Keystore(KeystoreError),
    /// The profile opened, but its plaintext isn't a JSON object.
    ProfileUnreadable,
    /// A set-up profile's slot can't be opened: its wrap or salt is missing or isn't Base64, its
    /// iteration count is outside 10,000 to 1,000,000, its wrap is too short to hold an IV, or it
    /// opened to a key that isn't 32 bytes long.
    SlotDamaged,
    /// The call needs a set-up profile, and there is none. The caller should have asked for the
    /// profile's state first.
    NotConfigured,
    /// The operating system's source of random bytes failed.
    RandomUnavailable,
    /// An earlier call panicked while it held the profile, so its state is unknown.
    Poisoned,
}

impl fmt::Display for VaultError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Keystore(_) => f.write_str("the Keystore failed"),
            Self::ProfileUnreadable => f.write_str("the security profile isn't a JSON object"),
            Self::SlotDamaged => f.write_str("a slot of the security profile is damaged"),
            Self::NotConfigured => f.write_str("there is no security profile"),
            Self::RandomUnavailable => f.write_str("the system's random source failed"),
            Self::Poisoned => {
                f.write_str("an earlier call panicked while it held the security profile")
            }
        }
    }
}

impl std::error::Error for VaultError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Self::Keystore(error) => Some(error),
            Self::ProfileUnreadable
            | Self::SlotDamaged
            | Self::NotConfigured
            | Self::RandomUnavailable
            | Self::Poisoned => None,
        }
    }
}

impl From<KeystoreError> for VaultError {
    fn from(error: KeystoreError) -> Self {
        Self::Keystore(error)
    }
}

#[cfg(test)]
mod tests {
    use std::error::Error;

    use denarii_keystore::KeystoreError;

    use super::VaultError;

    #[test]
    fn each_error_says_what_failed_and_keeps_the_keystore_error_as_its_source() {
        let cases = [
            (
                VaultError::Keystore(KeystoreError::KeyMissing),
                "the Keystore failed",
            ),
            (
                VaultError::ProfileUnreadable,
                "the security profile isn't a JSON object",
            ),
            (
                VaultError::SlotDamaged,
                "a slot of the security profile is damaged",
            ),
            (VaultError::NotConfigured, "there is no security profile"),
            (
                VaultError::RandomUnavailable,
                "the system's random source failed",
            ),
            (
                VaultError::Poisoned,
                "an earlier call panicked while it held the security profile",
            ),
        ];
        for (error, message) in cases {
            assert_eq!(error.to_string(), message);
        }
        let source = VaultError::from(KeystoreError::KeyMissing)
            .source()
            .map(ToString::to_string);
        assert_eq!(source, Some(KeystoreError::KeyMissing.to_string()));
        assert!(VaultError::SlotDamaged.source().is_none());
    }
}
