/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! A check of the whole Keystore path on a device, for `integration_test/keystore_test.dart`
//! only: Dart → this crate → `denarii-keystore` → JNI → Kotlin → the Android Keystore (ADR-0022).
//! The app itself never calls it. It works only under an alias prefix that starts with
//! `denarii_test_`, so it can't reach a user's keys or profile, and it returns one line for each
//! check, never key bytes.

use std::fmt::Debug;

use denarii_keystore::{Keystore, KeystoreError};

/// Every alias prefix [`keystore_self_check`] accepts starts with this.
const TEST_PREFIX: &str = "denarii_test_";

/// Why [`keystore_self_check`] refused to run.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum KeystoreCheckError {
    /// The alias prefix doesn't start with `denarii_test_`, so the check could reach keys that
    /// aren't the test's.
    NotATestPrefix,
    /// The prefix starts with `denarii_test_`, but the keystore rejects a name made from it: the
    /// prefix holds a character other than a lowercase ASCII letter, an ASCII digit, or `_`, or it
    /// is longer than 51 bytes, so a file name made from it would be longer than 64.
    InvalidPrefix,
}

impl std::fmt::Display for KeystoreCheckError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(match self {
            Self::NotATestPrefix => "the alias prefix doesn't start with denarii_test_",
            Self::InvalidPrefix => "the alias prefix can't make valid keystore names",
        })
    }
}

impl std::error::Error for KeystoreCheckError {}

/// Runs a fixed list of checks of the Keystore calls under `alias_prefix`, and returns one
/// `name: outcome` line for each, in order. It uses two sets of test keys, `alias_prefix` and
/// `alias_prefix` followed by `other_`, with the profile files `<alias_prefix>profile` and
/// `<alias_prefix>other_profile`, and deletes both sets at its start and at its end.
///
/// On a device, every line has the outcome that v1's Keystore code would give; off Android,
/// each call is `Unavailable` (`rust/crates/bridge/tests/keystore_check.rs` lists both).
///
/// # Errors
///
/// - [`KeystoreCheckError::NotATestPrefix`] when `alias_prefix` doesn't start with
///   `denarii_test_`.
/// - [`KeystoreCheckError::InvalidPrefix`] when the keystore rejects a name made from it.
#[expect(
    clippy::needless_pass_by_value,
    reason = "the generated code passes each argument from Dart by value"
)]
pub fn keystore_self_check(alias_prefix: String) -> Result<Vec<String>, KeystoreCheckError> {
    if !alias_prefix.starts_with(TEST_PREFIX) {
        return Err(KeystoreCheckError::NotATestPrefix);
    }
    let other_prefix = format!("{alias_prefix}other_");
    let keystore = Keystore::new(&alias_prefix, &format!("{alias_prefix}profile"));
    let other = Keystore::new(&other_prefix, &format!("{alias_prefix}other_profile"));
    // The other storage key, over the first set's profile file.
    let crossed = Keystore::new(&other_prefix, &format!("{alias_prefix}profile"));
    let (Ok(keystore), Ok(other), Ok(crossed)) = (keystore, other, crossed) else {
        return Err(KeystoreCheckError::InvalidPrefix);
    };
    Ok(run_checks(&keystore, &other, &crossed))
}

/// The checks, in order. `crossed` has `other`'s keys and `keystore`'s profile file.
fn run_checks(keystore: &Keystore, other: &Keystore, crossed: &Keystore) -> Vec<String> {
    let first = br#"{"profile_version":2,"security_question":"First pet?"}"#;
    let second = br#"{"profile_version":2,"security_question":"First street?"}"#;
    let large = vec![0x5A_u8; 64 * 1024];
    let mut message = b"denarii-dolor:pin:v2".to_vec();
    message.extend_from_slice(&[0x11; 32]);
    let mut other_message = message.clone();
    other_message.extend_from_slice(b"!");

    let mut report = Vec::new();
    let mut check = |name: &str, outcome: String| report.push(format!("{name}: {outcome}"));

    check("clean_start", deleted_all(keystore, other));
    check("read_without_entry", read(&keystore.read_profile(), first));
    check(
        "hmac_without_device_key",
        hmac(&keystore.device_hmac(&message)),
    );
    check("ensure_device_key", done(keystore.ensure_device_key()));
    let hmac_first = keystore.device_hmac(&message);
    check("hmac", hmac(&hmac_first));
    check(
        "hmac_repeats",
        same(&hmac_first, &keystore.device_hmac(&message)),
    );
    check(
        "hmac_differs_by_message",
        same(&hmac_first, &keystore.device_hmac(&other_message)),
    );
    check("hmac_of_empty_message", hmac(&keystore.device_hmac(&[])));
    check(
        "ensure_device_key_again",
        done(keystore.ensure_device_key()),
    );
    check(
        "hmac_after_ensuring_again",
        same(&hmac_first, &keystore.device_hmac(&message)),
    );
    check("other_ensure_device_key", done(other.ensure_device_key()));
    check(
        "hmac_differs_by_device_key",
        same(&hmac_first, &other.device_hmac(&message)),
    );

    check("write", done(keystore.write_profile(first)));
    check("read", read(&keystore.read_profile(), first));
    check("rewrite", done(keystore.write_profile(second)));
    check("read_rewritten", read(&keystore.read_profile(), second));
    check("write_empty", done(keystore.write_profile(&[])));
    check("read_empty", read(&keystore.read_profile(), &[]));
    check("write_64_kib", done(keystore.write_profile(&large)));
    check("read_64_kib", read(&keystore.read_profile(), &large));
    check("other_write", done(other.write_profile(first)));
    check(
        "read_with_other_storage_key",
        read(&crossed.read_profile(), &large),
    );

    check("delete_keys", done(keystore.delete_keys()));
    check(
        "read_without_storage_key",
        read(&keystore.read_profile(), &large),
    );
    check(
        "write_without_storage_key",
        done(keystore.write_profile(first)),
    );
    check(
        "read_after_refused_write",
        read(&keystore.read_profile(), &large),
    );
    check(
        "hmac_after_delete_keys",
        hmac(&keystore.device_hmac(&message)),
    );
    check("delete_keys_again", done(keystore.delete_keys()));
    check("delete_profile", done(keystore.delete_profile()));
    check(
        "read_after_delete_profile",
        read(&keystore.read_profile(), first),
    );
    check("delete_profile_again", done(keystore.delete_profile()));
    check(
        "write_after_delete_profile",
        done(keystore.write_profile(first)),
    );
    check(
        "read_with_new_storage_key",
        read(&keystore.read_profile(), first),
    );

    check("clean_end", deleted_all(keystore, other));
    check(
        "read_after_clean_end",
        read(&keystore.read_profile(), first),
    );
    report
}

/// Deletes both sets of keys and both profile files, trying each even when one fails.
fn deleted_all(keystore: &Keystore, other: &Keystore) -> String {
    let results = [
        keystore.delete_keys(),
        keystore.delete_profile(),
        other.delete_keys(),
        other.delete_profile(),
    ];
    done(results.into_iter().collect())
}

fn done(result: Result<(), KeystoreError>) -> String {
    outcome(&result, |()| "ok".to_owned())
}

fn hmac(result: &Result<[u8; 32], KeystoreError>) -> String {
    outcome(result, |_| "32 bytes".to_owned())
}

/// Whether a read returned `expected`, without the bytes themselves.
fn read(result: &Result<Option<Vec<u8>>, KeystoreError>, expected: &[u8]) -> String {
    outcome(result, |bytes| match bytes {
        None => "none".to_owned(),
        Some(bytes) if bytes.as_slice() == expected => "matches".to_owned(),
        Some(_) => "differs".to_owned(),
    })
}

/// Whether two HMAC results are the same, or the first error.
fn same(left: &Result<[u8; 32], KeystoreError>, right: &Result<[u8; 32], KeystoreError>) -> String {
    match (left, right) {
        (Ok(left), Ok(right)) => if left == right { "same" } else { "different" }.to_owned(),
        (Err(error), _) | (_, Err(error)) => format!("{error:?}"),
    }
}

fn outcome<T>(result: &Result<T, impl Debug>, describe: impl FnOnce(&T) -> String) -> String {
    match result {
        Ok(value) => describe(value),
        Err(error) => format!("{error:?}"),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn done_names_success_and_each_error() {
        assert_eq!(done(Ok(())), "ok");
        assert_eq!(done(Err(KeystoreError::KeyMissing)), "KeyMissing");
        assert_eq!(done(Err(KeystoreError::Failed)), "Failed");
    }

    #[test]
    fn hmac_names_the_length_never_the_bytes() {
        assert_eq!(hmac(&Ok([0xAB; 32])), "32 bytes");
        assert_eq!(hmac(&Err(KeystoreError::Unexpected)), "Unexpected");
    }

    #[test]
    fn read_compares_without_showing_the_bytes() {
        assert_eq!(read(&Ok(None), b"x"), "none");
        assert_eq!(read(&Ok(Some(b"x".to_vec())), b"x"), "matches");
        assert_eq!(read(&Ok(Some(Vec::new())), &[]), "matches");
        assert_eq!(read(&Ok(Some(b"y".to_vec())), b"x"), "differs");
        assert_eq!(read(&Err(KeystoreError::Unreadable), b"x"), "Unreadable");
    }

    #[test]
    fn same_compares_two_results() {
        assert_eq!(same(&Ok([1; 32]), &Ok([1; 32])), "same");
        assert_eq!(same(&Ok([1; 32]), &Ok([2; 32])), "different");
        assert_eq!(
            same(&Err(KeystoreError::KeyMissing), &Ok([1; 32])),
            "KeyMissing"
        );
        assert_eq!(same(&Ok([1; 32]), &Err(KeystoreError::Failed)), "Failed");
        assert_eq!(
            same(
                &Err(KeystoreError::Unavailable),
                &Err(KeystoreError::Failed)
            ),
            "Unavailable"
        );
    }

    #[test]
    fn each_error_describes_itself() {
        assert_eq!(
            KeystoreCheckError::NotATestPrefix.to_string(),
            "the alias prefix doesn't start with denarii_test_"
        );
        assert_eq!(
            KeystoreCheckError::InvalidPrefix.to_string(),
            "the alias prefix can't make valid keystore names"
        );
    }
}
