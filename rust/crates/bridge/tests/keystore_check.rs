/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The bridge's Keystore check refuses every prefix that isn't a test's before it calls anything,
//! and off Android it reports each check as `Unavailable`. `integration_test/keystore_test.dart`
//! runs it on an emulator, where each check has v1's outcome.

#![cfg(not(target_os = "android"))]

use denarii_bridge::api::keystore_check::{KeystoreCheckError, keystore_self_check};

/// The checks, in the order the function runs them.
const CHECKS: [&str; 35] = [
    "clean_start",
    "read_without_entry",
    "hmac_without_device_key",
    "ensure_device_key",
    "hmac",
    "hmac_repeats",
    "hmac_differs_by_message",
    "hmac_of_empty_message",
    "ensure_device_key_again",
    "hmac_after_ensuring_again",
    "other_ensure_device_key",
    "hmac_differs_by_device_key",
    "write",
    "read",
    "rewrite",
    "read_rewritten",
    "write_empty",
    "read_empty",
    "write_64_kib",
    "read_64_kib",
    "other_write",
    "read_with_other_storage_key",
    "delete_keys",
    "read_without_storage_key",
    "write_without_storage_key",
    "read_after_refused_write",
    "hmac_after_delete_keys",
    "delete_keys_again",
    "delete_profile",
    "read_after_delete_profile",
    "delete_profile_again",
    "write_after_delete_profile",
    "read_with_new_storage_key",
    "clean_end",
    "read_after_clean_end",
];

#[test]
fn reports_every_check_as_unavailable_off_android() {
    let expected: Vec<String> = CHECKS
        .iter()
        .map(|name| format!("{name}: Unavailable"))
        .collect();
    assert_eq!(
        keystore_self_check("denarii_test_host_".to_owned()),
        Ok(expected)
    );
}

#[test]
fn refuses_v1s_alias_prefix() {
    assert_eq!(
        keystore_self_check("denarii_vault_".to_owned()),
        Err(KeystoreCheckError::NotATestPrefix)
    );
}

#[test]
fn refuses_every_prefix_that_isnt_a_tests() {
    for prefix in [
        "",
        "denarii_",
        "denarii_test",
        "Denarii_test_",
        "x_denarii_test_",
        "denarii_testing",
    ] {
        assert_eq!(
            keystore_self_check(prefix.to_owned()),
            Err(KeystoreCheckError::NotATestPrefix),
            "{prefix:?}"
        );
    }
}

#[test]
fn refuses_a_test_prefix_that_makes_invalid_names() {
    for prefix in [
        "denarii_test_A",
        "denarii_test_-",
        "denarii_test_/../",
        "denarii_test_é",
    ] {
        assert_eq!(
            keystore_self_check(prefix.to_owned()),
            Err(KeystoreCheckError::InvalidPrefix),
            "{prefix:?}"
        );
    }
}

#[test]
fn takes_test_prefixes_up_to_51_bytes() {
    // The longest name made from the prefix is `<prefix>other_profile`, 13 bytes longer, and a
    // name may be 64 bytes long.
    let longest = format!("denarii_test_{}", "a".repeat(38));
    assert_eq!(longest.len(), 51);
    assert!(keystore_self_check(longest.clone()).is_ok());
    assert_eq!(
        keystore_self_check(format!("{longest}a")),
        Err(KeystoreCheckError::InvalidPrefix)
    );
}
