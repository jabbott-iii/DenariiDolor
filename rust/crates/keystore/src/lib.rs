/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Calls from Rust to the Android Keystore code in the app's Java runtime, for the vault of
//! Denarii Dolor.
//!
//! v1, the Kotlin app, keeps its vault keys in the Android Keystore, which never lets them leave
//! the phone (`docs/specs/android-v1-formats.md`, section 2). Only code in the app's Java runtime
//! can use them, so the Kotlin object `com.denariidolor.VaultKeystore` runs each operation, and
//! this crate calls it through the Java Native Interface (JNI), with `jni-sys`'s declarations of
//! `jni.h` (ADR-0007, ADR-0022: hardware-key operations stay in platform code, which Rust reaches
//! through `jni-sys` in this crate). [`Keystore`] is the safe API. All the `unsafe` code is in one
//! private module, compiled only for Android; on any other platform every call returns
//! [`KeystoreError::Unavailable`]. Its design is in `docs/specs/keystore.md`.
//!
//! # The parts
//!
//! - [`Keystore`]: the names of one set of keys and one profile file, with
//!   [`Keystore::vault`] for v1's and [`Keystore::new`] for a test's.
//! - The device key, which every PIN and answer guess passes through (CS-15):
//!   [`Keystore::ensure_device_key`] and [`Keystore::device_hmac`].
//! - The profile, sealed under the storage key in a preferences file:
//!   [`Keystore::read_profile`], [`Keystore::write_profile`], and [`Keystore::delete_profile`].
//! - [`Keystore::delete_keys`], which deletes all three of the vault's keys (CS-17).
//!
//! # Example
//!
//! ```
//! use denarii_keystore::{Keystore, KeystoreError};
//!
//! # fn main() -> Result<(), KeystoreError> {
//! let keystore = Keystore::new("denarii_test_example_", "denarii_test_example_profile")?;
//! // Off Android, there is no Keystore to reach.
//! assert_eq!(keystore.read_profile(), Err(KeystoreError::Unavailable));
//! # Ok(())
//! # }
//! ```
//!
//! # What callers must know
//!
//! - `MainActivity` loads the bridge's library with `System.loadLibrary` before Dart starts, so
//!   the Java runtime runs this crate's `JNI_OnLoad`, which finds the Kotlin code. Before that, or
//!   if it failed, every call returns [`KeystoreError::Unavailable`].
//! - Each call blocks its thread while the Keystore works, about a millisecond on the emulators
//!   (ADR-0007). Call it from a bridge worker thread.
//! - Calls may run on several threads at once. The Kotlin side creates keys, writes the profile,
//!   and deletes, one caller at a time.
//! - What [`Keystore::device_hmac`] and [`Keystore::read_profile`] return is secret. Overwrite it
//!   with zeros once it is used. This crate overwrites every Java array it passes or receives, and
//!   every copy of its own.

use std::fmt;

#[cfg(target_os = "android")]
#[expect(
    unsafe_code,
    reason = "the JNI calls into the app's Java runtime, the only hand-written unsafe code (ADR-0022)"
)]
mod jni;
#[cfg(target_os = "android")]
use jni as platform;

#[cfg(not(target_os = "android"))]
mod unavailable;
#[cfg(not(target_os = "android"))]
use unavailable as platform;

/// The names of one set of Keystore keys and one profile file: the alias prefix of the keys, and
/// the name of the preferences file that holds the sealed profile.
///
/// The keys are the prefix followed by `storage`, `device`, and `biometric`, as in v1
/// (`docs/specs/android-v1-formats.md`, section 2), and the file is
/// `shared_prefs/<profile_file>.xml` in the app's data folder.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Keystore {
    alias_prefix: String,
    profile_file: String,
}

impl Keystore {
    /// The alias prefix of v1's vault keys: `denarii_vault_storage`, `denarii_vault_device`, and
    /// `denarii_vault_biometric`.
    pub const VAULT_ALIAS_PREFIX: &str = "denarii_vault_";

    /// The preferences file of v1's sealed profile, `shared_prefs/vault_profile.xml`.
    pub const VAULT_PROFILE_FILE: &str = "vault_profile";

    /// The longest name [`Keystore::new`] accepts, in bytes.
    pub const MAX_NAME_BYTES: usize = 64;

    /// v1's preferences files (`docs/specs/android-v1-formats.md`, section 1), which only
    /// [`Keystore::vault`] may name.
    const V1_FILES: [&str; 4] = ["vault_profile", "secure_prefs", "db_key_prefs", "ui_prefs"];

    /// v1's keys and profile file, the ones that hold a user's data. Only the vault uses them; a
    /// test never does (`.claude/rules/security.md`).
    #[must_use]
    pub fn vault() -> Self {
        Self {
            alias_prefix: Self::VAULT_ALIAS_PREFIX.to_owned(),
            profile_file: Self::VAULT_PROFILE_FILE.to_owned(),
        }
    }

    /// Other keys and another profile file, such as a test's.
    ///
    /// # Errors
    ///
    /// - [`KeystoreError::InvalidName`] when either name is empty, longer than
    ///   [`Keystore::MAX_NAME_BYTES`], or holds a character other than a lowercase ASCII letter,
    ///   an ASCII digit, or `_`.
    /// - [`KeystoreError::ReservedName`] when the prefix starts with `denarii_vault_`, or the file
    ///   is one of v1's four preferences files, so that nothing but [`Keystore::vault`] reaches a
    ///   user's keys or files.
    pub fn new(alias_prefix: &str, profile_file: &str) -> Result<Self, KeystoreError> {
        if !is_valid_name(alias_prefix) || !is_valid_name(profile_file) {
            return Err(KeystoreError::InvalidName);
        }
        if alias_prefix.starts_with(Self::VAULT_ALIAS_PREFIX)
            || Self::V1_FILES.contains(&profile_file)
        {
            return Err(KeystoreError::ReservedName);
        }
        Ok(Self {
            alias_prefix: alias_prefix.to_owned(),
            profile_file: profile_file.to_owned(),
        })
    }

    /// The alias prefix of the keys.
    #[must_use]
    pub fn alias_prefix(&self) -> &str {
        &self.alias_prefix
    }

    /// The name of the profile's preferences file, without `.xml`.
    #[must_use]
    pub fn profile_file(&self) -> &str {
        &self.profile_file
    }

    /// Creates the device key, an HMAC-SHA256 key that never leaves the Keystore, unless it
    /// already exists. v1 did this before it wrote a new profile.
    ///
    /// # Errors
    ///
    /// [`KeystoreError::Unavailable`], [`KeystoreError::Failed`], or
    /// [`KeystoreError::Unexpected`], as [`KeystoreError`] describes them.
    pub fn ensure_device_key(&self) -> Result<(), KeystoreError> {
        platform::ensure_device_key(&self.alias_prefix)
    }

    /// HMAC-SHA256 of `message` under the device key: v1's key-encryption key for a PIN or answer
    /// slot, when `message` is the slot's context followed by the stretched secret (CS-15,
    /// `docs/specs/android-v1-formats.md`, section 3). It never creates the key.
    ///
    /// # Errors
    ///
    /// - [`KeystoreError::KeyMissing`] when the device key doesn't exist.
    /// - [`KeystoreError::Unavailable`], [`KeystoreError::Failed`], or
    ///   [`KeystoreError::Unexpected`], as [`KeystoreError`] describes them. A result that isn't
    ///   32 bytes long is [`KeystoreError::Unexpected`].
    pub fn device_hmac(&self, message: &[u8]) -> Result<[u8; 32], KeystoreError> {
        hmac_bytes(platform::device_hmac(&self.alias_prefix, message)?)
    }

    /// The profile's plaintext, opened from the `profile` entry of the profile file with the
    /// storage key, or `None` when the file has no entry. v1 stored it as Base64 of a 12-byte IV,
    /// the ciphertext, and a 16-byte tag, sealed with AES-256-GCM and the associated data
    /// `denarii-dolor:profile:v2` (`docs/specs/android-v1-formats.md`, section 3).
    ///
    /// # Errors
    ///
    /// - [`KeystoreError::KeyMissing`] when there is an entry but no storage key.
    /// - [`KeystoreError::Unreadable`] when the entry isn't Base64, is too short to hold an IV and
    ///   a tag, or fails AES-GCM's check under the storage key.
    /// - [`KeystoreError::Unavailable`], [`KeystoreError::Failed`], or
    ///   [`KeystoreError::Unexpected`], as [`KeystoreError`] describes them.
    pub fn read_profile(&self) -> Result<Option<Vec<u8>>, KeystoreError> {
        platform::read_profile(&self.alias_prefix, &self.profile_file)
    }

    /// Seals `plaintext` with the storage key and a new IV, and saves it as the profile file's
    /// `profile` entry, synchronously, as v1 did. It creates the storage key only when the file
    /// has no entry, so it never replaces a key that a stored profile needs.
    ///
    /// # Errors
    ///
    /// - [`KeystoreError::KeyMissing`] when the file has an entry but there is no storage key.
    ///   The entry stays as it was.
    /// - [`KeystoreError::Failed`] when the Keystore fails or the file can't be saved.
    /// - [`KeystoreError::Unavailable`] or [`KeystoreError::Unexpected`], as [`KeystoreError`]
    ///   describes them. A `plaintext` longer than 2,147,483,647 bytes, a Java array's limit, is
    ///   [`KeystoreError::Unexpected`].
    pub fn write_profile(&self, plaintext: &[u8]) -> Result<(), KeystoreError> {
        platform::write_profile(&self.alias_prefix, &self.profile_file, plaintext)
    }

    /// Empties the profile file and deletes it, as v1 did. A file that doesn't exist counts as
    /// deleted. The keys stay; [`Keystore::delete_keys`] deletes them.
    ///
    /// # Errors
    ///
    /// [`KeystoreError::Failed`] when the file can't be emptied or deleted, or
    /// [`KeystoreError::Unavailable`] or [`KeystoreError::Unexpected`], as [`KeystoreError`]
    /// describes them.
    pub fn delete_profile(&self) -> Result<(), KeystoreError> {
        platform::delete_profile(&self.profile_file)
    }

    /// Deletes the storage, device, and biometric keys. It tries all three even when one fails,
    /// because wipe deletes every key so that any copy of a wrapped key is useless (CS-17). A key
    /// that doesn't exist counts as deleted.
    ///
    /// # Errors
    ///
    /// [`KeystoreError::Failed`] when any of the three can't be deleted, or
    /// [`KeystoreError::Unavailable`] or [`KeystoreError::Unexpected`], as [`KeystoreError`]
    /// describes them.
    pub fn delete_keys(&self) -> Result<(), KeystoreError> {
        platform::delete_keys(&self.alias_prefix)
    }
}

/// Why a [`Keystore`] call failed. The vault turns each into its own error; users never see this
/// type's `Display` text.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[non_exhaustive]
pub enum KeystoreError {
    /// A name given to [`Keystore::new`] is empty, longer than [`Keystore::MAX_NAME_BYTES`], or
    /// holds a character other than a lowercase ASCII letter, an ASCII digit, or `_`.
    InvalidName,
    /// A name given to [`Keystore::new`] is one of v1's, which only [`Keystore::vault`] may use.
    ReservedName,
    /// There is no Java runtime to call: this isn't Android, or the runtime hasn't run this
    /// crate's `JNI_OnLoad`, or it failed to find the Kotlin code.
    Unavailable,
    /// The key the operation needs doesn't exist: the device key for an HMAC, or the storage key
    /// for a stored profile.
    KeyMissing,
    /// The stored profile isn't Base64, is too short to hold an IV and a tag, or fails AES-GCM's
    /// check under the storage key.
    Unreadable,
    /// The Keystore failed, or the profile file couldn't be saved or deleted.
    Failed,
    /// A JNI call failed, the Kotlin code threw something it doesn't name, such as an
    /// `OutOfMemoryError`, or it returned something it never should. Either is a bug, or a runtime
    /// the app wasn't tested on.
    Unexpected,
}

impl fmt::Display for KeystoreError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(match self {
            Self::InvalidName => {
                "a Keystore name isn't 1 to 64 lowercase ASCII letters, digits, or underscores"
            }
            Self::ReservedName => "a Keystore name is one of v1's, which only the vault may use",
            Self::Unavailable => "the app's Java runtime isn't reachable",
            Self::KeyMissing => "the Keystore key the operation needs doesn't exist",
            Self::Unreadable => "the stored profile can't be opened",
            Self::Failed => "the Keystore or the profile file failed",
            Self::Unexpected => "a JNI call failed in a way the app doesn't expect",
        })
    }
}

impl std::error::Error for KeystoreError {}

/// Whether `name` is 1 to [`Keystore::MAX_NAME_BYTES`] bytes of lowercase ASCII letters, ASCII
/// digits, and `_`. Such a name is a valid Keystore alias part and preferences file name, holds no
/// path separator, and crosses JNI unchanged, because ASCII is the same in Java's modified UTF-8.
fn is_valid_name(name: &str) -> bool {
    (1..=Keystore::MAX_NAME_BYTES).contains(&name.len())
        && name
            .bytes()
            .all(|byte| byte.is_ascii_lowercase() || byte.is_ascii_digit() || byte == b'_')
}

/// The 32 bytes of an HMAC-SHA256 result, after wiping the copy they came in.
fn hmac_bytes(mut bytes: Vec<u8>) -> Result<[u8; 32], KeystoreError> {
    let hmac = <[u8; 32]>::try_from(bytes.as_slice()).map_err(|_| KeystoreError::Unexpected);
    wipe(&mut bytes);
    hmac
}

/// Overwrites `bytes` with zeros. `black_box` keeps the compiler from removing the writes as
/// stores to memory that is about to be freed. M3.3 chooses the crate the vault wipes secrets with
/// (`docs/02-dependency-policy.md`), which may replace this.
fn wipe(bytes: &mut [u8]) {
    bytes.fill(0);
    std::hint::black_box(bytes);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn hmac_bytes_keeps_exactly_32_bytes() {
        let bytes: Vec<u8> = (1..=32).collect();
        let expected: [u8; 32] = std::array::from_fn(|index| u8::try_from(index + 1).unwrap());
        assert_eq!(hmac_bytes(bytes), Ok(expected));
    }

    #[test]
    fn hmac_bytes_rejects_any_other_length() {
        assert_eq!(hmac_bytes(Vec::new()), Err(KeystoreError::Unexpected));
        assert_eq!(hmac_bytes(vec![7; 31]), Err(KeystoreError::Unexpected));
        assert_eq!(hmac_bytes(vec![7; 33]), Err(KeystoreError::Unexpected));
    }

    #[test]
    fn wipe_overwrites_every_byte() {
        let mut bytes = vec![0xA5_u8; 100];
        wipe(&mut bytes);
        assert_eq!(bytes, vec![0; 100]);
    }
}
