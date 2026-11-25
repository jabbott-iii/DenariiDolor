/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! v1's `SecurityProfileGoldenTest`: the stored profile pinned byte for byte, with values computed
//! outside the app with `hashlib` and OpenSSL, so that every profile on a phone still opens. The
//! device key is the fake's, so only the format is pinned here; a phone's own key never leaves
//! its Keystore. Never change these values to make a change pass.

use denarii_vault::{Iterations, Recovery, SecurityProfile, Setup};
use serde_json::{Value, json};

use crate::support::{
    CountingRandom, FakeDeviceKey, MemoryStore, Outcome, START, outcome, unlocked,
};

const PIN: &str = "135790";
const NEW_PIN: &str = "246801";
const QUESTION: &str = "  Where did you first work?  ";
/// A decomposed é and a fullwidth Ｍ: stored as `café müller`.
const ANSWER: &str = "  Cafe\u{301}   \u{FF2D}\u{FC}ller ";
const ITERATIONS: u32 = 10_000;

/// The first 32 bytes that `CountingRandom` hands out; then come the PIN's salt and IV, then the
/// answer's.
fn database_key() -> [u8; 32] {
    let mut key = [0; 32];
    for (byte, value) in key.iter_mut().zip(0..) {
        *byte = value;
    }
    key
}

fn golden_profile() -> Value {
    json!({
        "profile_version": 2,
        "security_question": "Where did you first work?",
        "pin_wrap": "MDEyMzQ1Njc4OTo70uWPgrZgoUIUfXebxIDKe1H5cqj8jvxFL+A2KBDHGDskUSRwqd5TZFu5DhcKJ402",
        "pin_salt": "ICEiIyQlJicoKSorLC0uLw==",
        "pin_iterations": ITERATIONS,
        "answer_wrap": "TE1OT1BRUlNUVVZX8dywEEsjR39KbgztRv23mdc3s28PojP36mQQXe8/a6eidECrd1aHbtlp52VoZHBj",
        "answer_salt": "PD0+P0BBQkNERUZHSElKSw==",
        "answer_iterations": ITERATIONS,
    })
}

/// The golden profile, opened with the app's settings: the system's random bytes and 210,000
/// iterations for a new slot.
fn golden_service(store: &MemoryStore) -> SecurityProfile<FakeDeviceKey, MemoryStore> {
    SecurityProfile::new(FakeDeviceKey::v1(), store.clone())
}

#[test]
fn a_new_profile_is_stored_exactly_as_before() {
    let store = MemoryStore::default();
    let profile = SecurityProfile::with_random(
        FakeDeviceKey::v1(),
        store.clone(),
        CountingRandom::default(),
        Iterations::new(ITERATIONS).unwrap(),
    );

    assert_eq!(profile.set_up(PIN, PIN, QUESTION, ANSWER), Ok(Setup::Saved));
    assert_eq!(store.json(), golden_profile());
}

#[test]
fn a_stored_profile_still_unlocks_with_its_pin() {
    let store = MemoryStore::holding_json(&golden_profile());
    let profile = golden_service(&store);

    assert_eq!(
        unlocked(profile.unlock_with_pin(PIN, START)),
        Some(database_key())
    );
    assert_eq!(
        outcome(profile.unlock_with_pin("135791", START)),
        Outcome::Rejected(4)
    );
}

/// The answer was stored normalized, so it opens however it is typed now.
#[test]
fn a_stored_profile_still_recovers_with_its_answer() {
    let store = MemoryStore::holding_json(&golden_profile());
    let profile = golden_service(&store);

    assert_eq!(
        profile.recover_pin("CAFÉ MÜLLER", NEW_PIN, NEW_PIN, START),
        Ok(Recovery::Recovered)
    );
    assert_eq!(
        unlocked(profile.unlock_with_pin(NEW_PIN, START)),
        Some(database_key())
    );

    // The new PIN's slot has the app's iteration count, and the answer's slot is unchanged.
    let stored = store.json();
    assert_eq!(stored["pin_iterations"], json!(Iterations::DEFAULT.get()));
    assert_ne!(stored["pin_wrap"], golden_profile()["pin_wrap"]);
    for key in [
        "answer_wrap",
        "answer_salt",
        "answer_iterations",
        "security_question",
    ] {
        assert_eq!(stored[key], golden_profile()[key], "{key}");
    }
}
