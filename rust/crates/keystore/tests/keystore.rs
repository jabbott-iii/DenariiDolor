/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The keystore's names and its behavior off Android: `Keystore::new` takes only valid names
//! that aren't v1's, `Keystore::vault` names v1's, and every call returns `Unavailable` on a host
//! without the app's Java runtime. `integration_test/keystore_test.dart` checks the calls on an
//! emulator.

#![cfg(not(target_os = "android"))]

use denarii_keystore::{Keystore, KeystoreError};

#[test]
fn vault_names_v1s_keys_and_profile_file() {
    let vault = Keystore::vault();
    assert_eq!(vault.alias_prefix(), "denarii_vault_");
    assert_eq!(vault.profile_file(), "vault_profile");
    assert_eq!(Keystore::VAULT_ALIAS_PREFIX, "denarii_vault_");
    assert_eq!(Keystore::VAULT_PROFILE_FILE, "vault_profile");
}

#[test]
fn new_keeps_valid_names() {
    let keystore = Keystore::new("denarii_test_a1_", "denarii_test_a1_profile").unwrap();
    assert_eq!(keystore.alias_prefix(), "denarii_test_a1_");
    assert_eq!(keystore.profile_file(), "denarii_test_a1_profile");
    let shortest = Keystore::new("x", "y").unwrap();
    assert_eq!(
        (shortest.alias_prefix(), shortest.profile_file()),
        ("x", "y")
    );
}

#[test]
fn new_takes_names_of_exactly_64_bytes() {
    let longest = "a".repeat(Keystore::MAX_NAME_BYTES);
    assert_eq!(Keystore::MAX_NAME_BYTES, 64);
    assert!(Keystore::new(&longest, &longest).is_ok());
}

#[test]
fn new_rejects_names_of_65_bytes() {
    let too_long = "a".repeat(Keystore::MAX_NAME_BYTES + 1);
    assert_eq!(
        Keystore::new(&too_long, "profile"),
        Err(KeystoreError::InvalidName)
    );
    assert_eq!(
        Keystore::new("prefix_", &too_long),
        Err(KeystoreError::InvalidName)
    );
}

#[test]
fn new_rejects_empty_names() {
    assert_eq!(
        Keystore::new("", "profile"),
        Err(KeystoreError::InvalidName)
    );
    assert_eq!(
        Keystore::new("prefix_", ""),
        Err(KeystoreError::InvalidName)
    );
}

#[test]
fn new_rejects_characters_outside_lowercase_ascii_digits_and_underscore() {
    for bad in [
        "Upper",
        "has space",
        "dash-ed",
        "dot.ted",
        "slash/ed",
        "back\\slash",
        "../up",
        "nul\0byte",
        "tab\t",
        "café",
        "ｆｕｌｌ",
        "emoji😀",
        "$",
        "\u{7f}",
    ] {
        assert_eq!(
            Keystore::new(bad, "profile"),
            Err(KeystoreError::InvalidName),
            "{bad:?}"
        );
        assert_eq!(
            Keystore::new("prefix_", bad),
            Err(KeystoreError::InvalidName),
            "{bad:?}"
        );
    }
}

#[test]
fn new_refuses_v1s_alias_prefix_and_anything_that_starts_with_it() {
    for prefix in [
        "denarii_vault_",
        "denarii_vault_storage",
        "denarii_vault_x_",
    ] {
        assert_eq!(
            Keystore::new(prefix, "denarii_test_profile"),
            Err(KeystoreError::ReservedName),
            "{prefix}"
        );
    }
    // Only the full prefix is v1's: these name other keys.
    assert!(Keystore::new("denarii_vault", "denarii_test_profile").is_ok());
    assert!(Keystore::new("denarii_", "denarii_test_profile").is_ok());
}

#[test]
fn new_refuses_v1s_preferences_files() {
    for file in ["vault_profile", "secure_prefs", "db_key_prefs", "ui_prefs"] {
        assert_eq!(
            Keystore::new("denarii_test_", file),
            Err(KeystoreError::ReservedName),
            "{file}"
        );
    }
    assert!(Keystore::new("denarii_test_", "vault_profile_2").is_ok());
}

#[test]
fn checks_the_characters_before_the_reserved_names() {
    assert_eq!(
        Keystore::new("denarii_vault_", "Bad"),
        Err(KeystoreError::InvalidName)
    );
    assert_eq!(
        Keystore::new("Bad", "vault_profile"),
        Err(KeystoreError::InvalidName)
    );
}

#[test]
fn every_call_is_unavailable_without_the_java_runtime() {
    for keystore in [
        Keystore::vault(),
        Keystore::new("denarii_test_", "denarii_test_profile").unwrap(),
    ] {
        assert_eq!(
            keystore.ensure_device_key(),
            Err(KeystoreError::Unavailable)
        );
        assert_eq!(
            keystore.device_hmac(b"denarii-dolor:pin:v2"),
            Err(KeystoreError::Unavailable)
        );
        assert_eq!(keystore.device_hmac(&[]), Err(KeystoreError::Unavailable));
        assert_eq!(keystore.read_profile(), Err(KeystoreError::Unavailable));
        assert_eq!(
            keystore.write_profile(b"{}"),
            Err(KeystoreError::Unavailable)
        );
        assert_eq!(keystore.delete_profile(), Err(KeystoreError::Unavailable));
        assert_eq!(keystore.delete_keys(), Err(KeystoreError::Unavailable));
    }
}

#[test]
fn each_error_describes_itself() {
    let cases = [
        (
            KeystoreError::InvalidName,
            "a Keystore name isn't 1 to 64 lowercase ASCII letters, digits, or underscores",
        ),
        (
            KeystoreError::ReservedName,
            "a Keystore name is one of v1's, which only the vault may use",
        ),
        (
            KeystoreError::Unavailable,
            "the app's Java runtime isn't reachable",
        ),
        (
            KeystoreError::KeyMissing,
            "the Keystore key the operation needs doesn't exist",
        ),
        (
            KeystoreError::Unreadable,
            "the stored profile can't be opened",
        ),
        (
            KeystoreError::Failed,
            "the Keystore or the profile file failed",
        ),
        (
            KeystoreError::Unexpected,
            "a JNI call failed in a way the app doesn't expect",
        ),
    ];
    for (error, text) in cases {
        assert_eq!(error.to_string(), text);
    }
}
