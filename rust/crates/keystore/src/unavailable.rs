/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The platform calls off Android, where there is no Java runtime and no Keystore: each returns
//! [`KeystoreError::Unavailable`]. The `jni` module has the same functions for Android.

use crate::KeystoreError;

pub(crate) fn ensure_device_key(_alias_prefix: &str) -> Result<(), KeystoreError> {
    Err(KeystoreError::Unavailable)
}

pub(crate) fn device_hmac(_alias_prefix: &str, _message: &[u8]) -> Result<Vec<u8>, KeystoreError> {
    Err(KeystoreError::Unavailable)
}

pub(crate) fn read_profile(
    _alias_prefix: &str,
    _profile_file: &str,
) -> Result<Option<Vec<u8>>, KeystoreError> {
    Err(KeystoreError::Unavailable)
}

pub(crate) fn write_profile(
    _alias_prefix: &str,
    _profile_file: &str,
    _plaintext: &[u8],
) -> Result<(), KeystoreError> {
    Err(KeystoreError::Unavailable)
}

pub(crate) fn delete_profile(_profile_file: &str) -> Result<(), KeystoreError> {
    Err(KeystoreError::Unavailable)
}

pub(crate) fn delete_keys(_alias_prefix: &str) -> Result<(), KeystoreError> {
    Err(KeystoreError::Unavailable)
}
