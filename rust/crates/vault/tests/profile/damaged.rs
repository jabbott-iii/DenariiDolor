/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Stored profiles that aren't what setup writes: incomplete, damaged, or left by v1 in the middle
//! of a lockout, and stores that fail. A damaged profile must be an error, never a reason to offer
//! setup in more cases than v1 did, because setup deletes the database
//! (`docs/specs/android-v1-formats.md`, open question 7).

use aes_gcm::Aes256Gcm;
use aes_gcm::aead::{AeadInOut, KeyInit};
use denarii_keystore::KeystoreError;
use denarii_vault::{Iterations, MonotonicTime, ProfileState, SecurityProfile, Setup, VaultError};
use serde_json::{Value, json};
use sha2::Sha256;

use crate::support::{
    FailingRandom, FakeDeviceKey, MemoryStore, Outcome, START, V1_DEVICE_KEY, base64, hmac_sha256,
    later, outcome, profile, unlocked,
};

const PIN: &str = "123456";
const QUESTION: &str = "Name of my first school?";
const ANSWER: &str = "Lincoln Elementary";

/// A profile that setup wrote, as JSON.
fn set_up_profile() -> Value {
    let store = MemoryStore::default();
    let profile = profile(&FakeDeviceKey::v1(), &store);
    assert_eq!(profile.set_up(PIN, PIN, QUESTION, ANSWER), Ok(Setup::Saved));
    store.json()
}

/// The set-up profile with `key` changed to `value`, or removed when `value` is `None`.
fn with(key: &str, value: Option<Value>) -> MemoryStore {
    let mut stored = set_up_profile();
    if let Some(values) = stored.as_object_mut() {
        match value {
            Some(value) => values.insert(key.to_owned(), value),
            None => values.remove(key),
        };
    }
    MemoryStore::holding_json(&stored)
}

fn state_of(store: &MemoryStore) -> Result<ProfileState, VaultError> {
    profile(&FakeDeviceKey::v1(), store).state()
}

#[test]
fn only_a_whole_version_2_profile_is_set_up() {
    let needs_setup = [
        ("profile_version", Some(json!(1))),
        ("profile_version", Some(json!(3))),
        ("profile_version", Some(json!("2"))),
        ("profile_version", None),
        ("security_question", Some(json!(" \u{3000}\t"))),
        ("security_question", None),
        ("pin_wrap", Some(json!(""))),
        ("pin_wrap", None),
        ("answer_salt", Some(json!("  "))),
        ("answer_salt", None),
        ("pin_iterations", Some(json!(9_999))),
        ("pin_iterations", Some(json!(1_000_001))),
        ("pin_iterations", Some(json!(-210_000))),
        ("answer_iterations", Some(json!("210000"))),
        ("answer_iterations", Some(json!(210_000.0))),
        ("answer_iterations", None),
    ];
    for (key, value) in needs_setup {
        assert_eq!(
            state_of(&with(key, value.clone())),
            Ok(ProfileState::NeedsSetup),
            "{key} {value:?}"
        );
    }
}

#[test]
fn stored_iteration_counts_at_the_limits_are_honored() {
    for count in [Iterations::MIN.get(), Iterations::MAX.get()] {
        assert!(
            matches!(
                state_of(&with("answer_iterations", Some(json!(count)))),
                Ok(ProfileState::Configured { .. })
            ),
            "{count}"
        );
    }
}

#[test]
fn a_plaintext_that_isnt_a_json_object_is_an_error_not_a_new_setup() {
    for plaintext in [
        &b"[]"[..],
        b"",
        b"{\"profile_version\":2",
        b"\xef\xbb\xbf{}",
    ] {
        let store = MemoryStore::holding(plaintext);
        let profile = profile(&FakeDeviceKey::v1(), &store);
        assert_eq!(
            profile.state(),
            Err(VaultError::ProfileUnreadable),
            "{plaintext:?}"
        );
        assert_eq!(
            profile.set_up(PIN, PIN, QUESTION, ANSWER),
            Err(VaultError::ProfileUnreadable)
        );
        assert_eq!(store.plaintext().as_deref(), Some(plaintext));
    }
}

#[test]
fn a_keystore_failure_is_reported_as_it_happened() {
    let store = MemoryStore::holding_json(&set_up_profile());
    store.fail_reads(KeystoreError::Unreadable);

    assert_eq!(
        state_of(&store),
        Err(VaultError::Keystore(KeystoreError::Unreadable))
    );
}

/// A wrap or salt that isn't Base64, or a wrap with no room after its IV, can't come from setup.
/// It is damage, not a wrong PIN, so it isn't counted.
#[test]
fn a_damaged_slot_is_an_error_not_a_wrong_pin() {
    let damaged = [
        ("pin_wrap", json!("not base64!")),
        ("pin_wrap", json!("AAECAwQFBgcICQoL")),
        ("pin_salt", json!("AAECA")),
        ("pin_salt", json!("AAEC\u{e9}")),
    ];
    for (key, value) in damaged {
        let store = with(key, Some(value.clone()));
        let profile = profile(&FakeDeviceKey::v1(), &store);
        assert_eq!(
            profile.unlock_with_pin(PIN, START).unwrap_err(),
            VaultError::SlotDamaged,
            "{key} {value}"
        );
        assert_eq!(store.json().get("failed_attempts"), None);
    }
}

/// A wrap too short for a tag fails AES-GCM's check, as Java's cipher did, so v1 counted it as a
/// wrong PIN; the port does the same.
#[test]
fn a_wrap_shorter_than_an_iv_and_a_tag_reads_as_a_wrong_pin() {
    let store = with(
        "pin_wrap",
        Some(json!("AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBka")),
    );
    let profile = profile(&FakeDeviceKey::v1(), &store);

    assert_eq!(
        outcome(profile.unlock_with_pin(PIN, START)),
        Outcome::Rejected(4)
    );
}

/// A slot sealed here, outside the crate, with the format spec's derivation (invariant 4), opens
/// with its PIN; one that holds a key of the wrong length is damage.
#[test]
fn a_slot_sealed_by_the_spec_opens_and_its_key_length_is_checked() {
    let salt = [7_u8; 16];
    let iv = [9_u8; 12];
    for (length, opens) in [(32, true), (31, false), (33, false)] {
        let key: Vec<u8> = (0..length).collect();
        let wrapped = seal_pin_slot(PIN, &salt, 10_000, &iv, &key).unwrap();
        let mut stored = set_up_profile();
        stored["pin_wrap"] = json!(base64(&wrapped));
        stored["pin_salt"] = json!(base64(&salt));
        stored["pin_iterations"] = json!(10_000);
        let store = MemoryStore::holding_json(&stored);
        let result = profile(&FakeDeviceKey::v1(), &store).unlock_with_pin(PIN, START);
        if opens {
            assert_eq!(unlocked(result).map(Vec::from), Some(key));
        } else {
            assert_eq!(
                result.unwrap_err(),
                VaultError::SlotDamaged,
                "{length} bytes"
            );
        }
    }
}

/// v1 kept its lockout in the profile, so one that v1 started carries on after the update.
#[test]
fn a_lockout_that_v1_started_carries_on() {
    let mut stored = set_up_profile();
    stored["failed_attempts"] = json!(6);
    stored["lock_duration_millis"] = json!(60_000);
    stored["lock_started_at_elapsed_millis"] = json!(START.elapsed_millis - 15_000);
    stored["lock_boot_count"] = json!(7);
    let store = MemoryStore::holding_json(&stored);
    let profile = profile(&FakeDeviceKey::v1(), &store);

    assert_eq!(
        outcome(profile.unlock_with_pin(PIN, START)),
        Outcome::LockedOut(45_000)
    );
    assert_eq!(
        outcome(profile.unlock_with_pin("000000", later(START, 45_000))),
        Outcome::LockedOut(120_000)
    );
}

#[test]
fn edits_keep_keys_this_crate_doesnt_know() {
    let mut stored = set_up_profile();
    stored["biometric_wrap"] = json!("AAECAwQFBgcICQoLDA0ODxAR");
    let store = MemoryStore::holding_json(&stored);
    let profile = profile(&FakeDeviceKey::v1(), &store);

    assert_eq!(
        outcome(profile.unlock_with_pin("000000", START)),
        Outcome::Rejected(4)
    );
    assert_eq!(store.json()["biometric_wrap"], stored["biometric_wrap"]);
    assert_eq!(store.json()["failed_attempts"], 1);
}

/// v1's setup cleared whatever an incomplete profile held before it wrote the new one.
#[test]
fn setup_replaces_an_incomplete_profile_whole() {
    let store = MemoryStore::holding_json(&json!({"profile_version": 1, "biometric_wrap": "AAEC"}));
    let profile = profile(&FakeDeviceKey::v1(), &store);

    assert_eq!(profile.set_up(PIN, PIN, QUESTION, ANSWER), Ok(Setup::Saved));
    let mut keys: Vec<String> = store.json().as_object().unwrap().keys().cloned().collect();
    keys.sort();
    assert_eq!(
        keys,
        [
            "answer_iterations",
            "answer_salt",
            "answer_wrap",
            "pin_iterations",
            "pin_salt",
            "pin_wrap",
            "profile_version",
            "security_question"
        ]
    );
}

#[test]
fn a_failed_write_changes_nothing() {
    let store = MemoryStore::default();
    let profile = profile(&FakeDeviceKey::v1(), &store);
    store.fail_writes(Some(KeystoreError::Failed));

    assert_eq!(
        profile.set_up(PIN, PIN, QUESTION, ANSWER),
        Err(VaultError::Keystore(KeystoreError::Failed))
    );
    assert_eq!(profile.state(), Ok(ProfileState::NeedsSetup));

    store.fail_writes(None);
    assert_eq!(profile.set_up(PIN, PIN, QUESTION, ANSWER), Ok(Setup::Saved));
    store.fail_writes(Some(KeystoreError::Failed));
    assert_eq!(
        profile.unlock_with_pin("000000", START).unwrap_err(),
        VaultError::Keystore(KeystoreError::Failed)
    );

    // The failure that couldn't be saved wasn't counted, here or in the copy the profile keeps.
    store.fail_writes(None);
    assert_eq!(
        outcome(profile.unlock_with_pin("000000", START)),
        Outcome::Rejected(4)
    );
    assert_eq!(store.writes(), 2);
}

#[test]
fn a_failed_random_source_stores_nothing() {
    let device_key = FakeDeviceKey::v1();
    let store = MemoryStore::default();
    let profile = SecurityProfile::with_random(
        device_key.clone(),
        store.clone(),
        FailingRandom,
        Iterations::MIN,
    );

    assert_eq!(
        profile.set_up(PIN, PIN, QUESTION, ANSWER),
        Err(VaultError::RandomUnavailable)
    );
    assert_eq!(store.plaintext(), None);
    assert_eq!(device_key.times_prepared(), 0);
}

#[test]
fn a_device_key_that_cant_be_created_stores_nothing() {
    let device_key = FakeDeviceKey::v1();
    device_key.fail_to_create();
    let store = MemoryStore::default();

    assert_eq!(
        profile(&device_key, &store).set_up(PIN, PIN, QUESTION, ANSWER),
        Err(VaultError::Keystore(KeystoreError::Failed))
    );
    assert_eq!(store.plaintext(), None);
}

/// A new PIN that can't be saved leaves the old one working. The answer opened its slot, but the
/// reset of the failure count couldn't be saved either, so the earlier failure still counts.
#[test]
fn a_recovery_that_cant_be_saved_keeps_the_old_pin() {
    let store = MemoryStore::holding_json(&set_up_profile());
    let profile = profile(&FakeDeviceKey::v1(), &store);
    assert_eq!(
        outcome(profile.unlock_with_pin("000000", START)),
        Outcome::Rejected(4)
    );
    store.fail_writes(Some(KeystoreError::Failed));

    assert_eq!(
        profile.recover_pin(ANSWER, "567890", "567890", START),
        Err(VaultError::Keystore(KeystoreError::Failed))
    );

    store.fail_writes(None);
    assert_eq!(
        outcome(profile.unlock_with_pin("567890", START)),
        Outcome::Rejected(3)
    );
    assert_eq!(
        outcome(profile.unlock_with_pin(PIN, START)),
        Outcome::Unlocked
    );
}

#[test]
fn a_profile_that_cant_be_read_fails_every_call() {
    let store = MemoryStore::holding_json(&set_up_profile());
    store.fail_reads(KeystoreError::Failed);
    let profile = profile(&FakeDeviceKey::v1(), &store);
    let failed = VaultError::Keystore(KeystoreError::Failed);

    assert_eq!(profile.set_up(PIN, PIN, QUESTION, ANSWER), Err(failed));
    assert_eq!(
        outcome(profile.unlock_with_pin(PIN, START)),
        Outcome::Failed(failed)
    );
    assert_eq!(
        profile.recover_pin(ANSWER, "567890", "567890", START),
        Err(failed)
    );
    assert_eq!(profile.lockout_remaining_millis(START), Err(failed));
}

/// Clearing a served lockout and restarting one after a reboot both write the profile; when that
/// write fails, the call fails, and the timer is as it was.
#[test]
fn a_lockout_timer_that_cant_be_saved_is_an_error() {
    let mut stored = set_up_profile();
    stored["failed_attempts"] = json!(5);
    stored["lock_duration_millis"] = json!(30_000);
    stored["lock_started_at_elapsed_millis"] = json!(START.elapsed_millis);
    stored["lock_boot_count"] = json!(7);
    let store = MemoryStore::holding_json(&stored);
    let profile = profile(&FakeDeviceKey::v1(), &store);
    store.fail_writes(Some(KeystoreError::Failed));
    let failed = Err(VaultError::Keystore(KeystoreError::Failed));

    assert_eq!(
        profile.lockout_remaining_millis(later(START, 30_000)),
        failed
    );
    let rebooted = MonotonicTime {
        elapsed_millis: 1_000,
        boot_count: Some(8),
    };
    assert_eq!(profile.lockout_remaining_millis(rebooted), failed);
    assert_eq!(store.json(), stored);

    store.fail_writes(None);
    assert_eq!(
        profile.lockout_remaining_millis(later(START, 10_000)),
        Ok(20_000)
    );
}

/// The PIN slot as the format spec derives it: `iv || AES-256-GCM(kek, iv, context, key)`, with
/// `kek = HMAC-SHA256(device key, context || PBKDF2-HMAC-SHA256(pin, salt, iterations))`.
fn seal_pin_slot(
    pin: &str,
    salt: &[u8],
    iterations: u32,
    iv: &[u8; 12],
    key: &[u8],
) -> Option<Vec<u8>> {
    const CONTEXT: &[u8] = b"denarii-dolor:pin:v2";
    let stretched: [u8; 32] =
        pbkdf2::pbkdf2_hmac_array::<Sha256, 32>(pin.as_bytes(), salt, iterations);
    let kek = hmac_sha256(&V1_DEVICE_KEY, &[CONTEXT, &stretched])?;
    let mut sealed = key.to_vec();
    let tag = Aes256Gcm::new((&kek).into())
        .encrypt_inout_detached(iv.into(), CONTEXT, sealed.as_mut_slice().into())
        .ok()?;
    let mut wrapped = iv.to_vec();
    wrapped.extend_from_slice(&sealed);
    wrapped.extend_from_slice(&tag);
    Some(wrapped)
}
