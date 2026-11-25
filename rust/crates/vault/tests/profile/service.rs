/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! v1's `SecurityProfileServiceTest`: setup, sign-in, recovery, and the shared lockout (CS-10,
//! CS-13, CS-14, CS-15), on the fewest iterations to keep the tests fast. The time is passed in,
//! so it moves only when a test moves it.

use denarii_keystore::KeystoreError;
use denarii_vault::{
    MonotonicTime, ProfileState, Recovery, SecurityProfile, Setup, SetupProblem, VaultError,
    normalize_answer,
};
use serde_json::Value;

use crate::support::{
    FakeDeviceKey, MemoryStore, Outcome, START, base64, later, outcome, profile, unlocked,
};

const PIN: &str = "123456";
const QUESTION: &str = "Name of my first school?";
const ANSWER: &str = "Lincoln Elementary";
const WRONG_PIN: &str = "000000";

struct Fixture {
    device_key: FakeDeviceKey,
    store: MemoryStore,
    profile: SecurityProfile<FakeDeviceKey, MemoryStore>,
}

impl Fixture {
    fn new() -> Self {
        let device_key = FakeDeviceKey::v1();
        let store = MemoryStore::default();
        let profile = profile(&device_key, &store);
        Self {
            device_key,
            store,
            profile,
        }
    }

    fn configured() -> Self {
        let fixture = Self::new();
        assert_eq!(
            fixture.profile.set_up(PIN, PIN, QUESTION, ANSWER),
            Ok(Setup::Saved)
        );
        fixture
    }

    fn sign_in(&self, pin: &str, now: MonotonicTime) -> Outcome {
        outcome(self.profile.unlock_with_pin(pin, now))
    }

    fn key(&self, pin: &str) -> Option<[u8; 32]> {
        unlocked(self.profile.unlock_with_pin(pin, START))
    }

    fn recover(
        &self,
        answer: &str,
        new_pin: &str,
        now: MonotonicTime,
    ) -> Result<Recovery, VaultError> {
        self.profile.recover_pin(answer, new_pin, new_pin, now)
    }

    fn lockout(&self, now: MonotonicTime) -> Result<u64, VaultError> {
        self.profile.lockout_remaining_millis(now)
    }

    /// The failure count as stored.
    fn failures(&self) -> i64 {
        self.store
            .json()
            .get("failed_attempts")
            .and_then(Value::as_i64)
            .unwrap_or(0)
    }

    fn fail(&self, times: usize) {
        for _ in 0..times {
            assert!(self.profile.unlock_with_pin(WRONG_PIN, START).is_ok());
        }
    }
}

#[test]
fn setup_succeeds_once_and_configures_sign_in() {
    let fixture = Fixture::configured();

    assert_eq!(
        fixture.profile.state(),
        Ok(ProfileState::Configured {
            security_question: QUESTION.to_owned()
        })
    );
    assert_eq!(
        fixture.profile.set_up(PIN, PIN, QUESTION, ANSWER),
        Ok(Setup::AlreadyConfigured)
    );
    assert_eq!(fixture.device_key.times_prepared(), 1);
}

#[test]
fn a_new_profile_needs_setup() {
    let fixture = Fixture::new();

    assert_eq!(fixture.profile.state(), Ok(ProfileState::NeedsSetup));
    assert_eq!(fixture.store.plaintext(), None);
}

#[test]
fn the_question_is_stored_trimmed() {
    let fixture = Fixture::new();
    assert_eq!(
        fixture
            .profile
            .set_up(PIN, PIN, "\u{A0} Name of my first school?\t\u{1F}", ANSWER),
        Ok(Setup::Saved)
    );

    assert_eq!(
        fixture.profile.state(),
        Ok(ProfileState::Configured {
            security_question: QUESTION.to_owned()
        })
    );
}

#[test]
fn the_correct_pin_always_unwraps_the_same_database_key() {
    let fixture = Fixture::configured();

    let first = fixture.key(PIN);
    assert!(first.is_some());
    assert_eq!(fixture.key(PIN), first);
    assert_eq!(fixture.key("654321"), None);
    assert_eq!(fixture.device_key.times_prepared(), 1);
}

#[test]
fn the_stored_profile_holds_no_plain_secret_or_database_key() {
    let fixture = Fixture::configured();
    let key = fixture.key(PIN).unwrap();
    let plaintext = String::from_utf8(fixture.store.plaintext().unwrap()).unwrap();

    assert!(!plaintext.contains(PIN));
    assert!(!plaintext.contains(normalize_answer(ANSWER).as_str()));
    assert!(!plaintext.contains(&base64(&key)));
    assert!(
        !plaintext
            .as_bytes()
            .windows(key.len())
            .any(|window| window == key)
    );
}

#[test]
fn another_device_key_cant_unwrap_the_database_key() {
    let fixture = Fixture::configured();
    let other_device = profile(&FakeDeviceKey::new([2; 32]), &fixture.store);

    assert_eq!(
        outcome(other_device.unlock_with_pin(PIN, START)),
        Outcome::Rejected(4)
    );
}

#[test]
fn a_missing_device_key_is_a_storage_failure_not_a_wrong_pin() {
    let fixture = Fixture::configured();
    fixture.device_key.lose();

    assert_eq!(
        fixture.profile.unlock_with_pin(PIN, START).unwrap_err(),
        VaultError::Keystore(KeystoreError::KeyMissing)
    );
    assert_eq!(
        fixture
            .profile
            .recover_pin(ANSWER, "567890", "567890", START),
        Err(VaultError::Keystore(KeystoreError::KeyMissing))
    );
    assert_eq!(fixture.failures(), 0);
}

#[test]
fn a_pin_must_be_six_to_twelve_digits() {
    let fixture = Fixture::new();
    let set_up = |pin| fixture.profile.set_up(pin, pin, QUESTION, ANSWER);

    for pin in [
        "12345",
        "1234567890123",
        "12345a",
        "\u{661}\u{662}\u{663}\u{664}\u{665}\u{666}",
    ] {
        assert_eq!(
            set_up(pin),
            Ok(Setup::Invalid(SetupProblem::InvalidPinFormat)),
            "{pin}"
        );
    }
    assert_eq!(
        fixture.profile.set_up(PIN, "123457", QUESTION, ANSWER),
        Ok(Setup::Invalid(SetupProblem::PinMismatch))
    );
    assert_eq!(set_up("123456789012"), Ok(Setup::Saved));

    assert_eq!(
        fixture.recover(ANSWER, "12345", START),
        Ok(Recovery::InvalidPinFormat)
    );
    assert_eq!(fixture.sign_in("123456789012", START), Outcome::Unlocked);
}

/// v1's sign-in screen refused a PIN of the wrong form before it reached the profile, so it was
/// never checked or counted (CS-10).
#[test]
fn sign_in_refuses_a_pin_of_the_wrong_form_without_counting_it() {
    let fixture = Fixture::configured();

    for pin in [
        "",
        "12345",
        "1234567890123",
        "12345a",
        "123456\n",
        " 123456",
        "١٢٣٤٥٦",
    ] {
        assert_eq!(
            fixture.sign_in(pin, START),
            Outcome::InvalidPinFormat,
            "{pin:?}"
        );
    }
    assert_eq!(fixture.failures(), 0);
    assert_eq!(fixture.sign_in(WRONG_PIN, START), Outcome::Rejected(4));
}

#[test]
fn sign_in_needs_a_profile() {
    let fixture = Fixture::new();

    assert_eq!(
        fixture.profile.unlock_with_pin(PIN, START).unwrap_err(),
        VaultError::NotConfigured
    );
    assert_eq!(
        fixture.recover(ANSWER, "567890", START),
        Ok(Recovery::NotConfigured)
    );
}

#[test]
fn a_rejected_setup_stores_nothing() {
    let fixture = Fixture::new();

    assert_eq!(
        fixture.profile.set_up(PIN, PIN, QUESTION, "short"),
        Ok(Setup::Invalid(SetupProblem::SecurityAnswerTooShort))
    );
    assert_eq!(fixture.profile.state(), Ok(ProfileState::NeedsSetup));
    assert_eq!(fixture.store.plaintext(), None);
    assert_eq!(fixture.device_key.times_prepared(), 0);
}

#[test]
fn answers_ignore_case_spacing_and_unicode_form() {
    let fixture = Fixture::configured();

    assert_eq!(
        fixture.recover("  lincoln   ELEMENTARY ", "567890", START),
        Ok(Recovery::Recovered)
    );
    assert_eq!(
        fixture.recover("Ｌｉｎｃｏｌｎ Elementary", "567891", START),
        Ok(Recovery::Recovered)
    );
    assert!(fixture.key("567891").is_some());
}

#[test]
fn recovery_rejects_an_incorrect_answer_without_changing_the_pin() {
    let fixture = Fixture::configured();

    assert_eq!(
        fixture.recover("wrong answer", "567890", START),
        Ok(Recovery::InvalidSecurityAnswer)
    );
    assert_eq!(fixture.sign_in(PIN, START), Outcome::Unlocked);
    assert_eq!(fixture.sign_in("567890", START), Outcome::Rejected(4));
}

#[test]
fn recovery_checks_its_input_in_v1s_order() {
    let fixture = Fixture::configured();
    let recover = |answer, new_pin, confirmation| {
        fixture
            .profile
            .recover_pin(answer, new_pin, confirmation, START)
            .unwrap()
    };

    assert_eq!(recover("  ", "12345", "54321"), Recovery::InvalidPinFormat);
    assert_eq!(recover("  ", "567890", "567891"), Recovery::PinMismatch);
    assert_eq!(
        recover(" \u{3000} ", "567890", "567890"),
        Recovery::SecurityAnswerRequired
    );
    assert_eq!(fixture.failures(), 0);
}

#[test]
fn recovery_replaces_the_pin_and_keeps_the_database_key() {
    let fixture = Fixture::configured();
    let original = fixture.key(PIN);

    assert_eq!(
        fixture.recover(ANSWER, "567890", START),
        Ok(Recovery::Recovered)
    );

    assert_eq!(fixture.key(PIN), None);
    assert_eq!(fixture.key("567890"), original);
}

#[test]
fn the_fifth_wrong_pin_locks_out_and_blocks_the_correct_pin() {
    let fixture = Fixture::configured();

    for attempt in 0..4 {
        assert_eq!(
            fixture.sign_in(WRONG_PIN, START),
            Outcome::Rejected(4 - attempt)
        );
    }
    assert_eq!(
        fixture.sign_in(WRONG_PIN, START),
        Outcome::LockedOut(30_000)
    );

    assert_eq!(
        fixture.sign_in(PIN, later(START, 10_000)),
        Outcome::LockedOut(20_000)
    );
}

#[test]
fn the_lockout_ends_only_with_elapsed_time() {
    let fixture = Fixture::configured();
    fixture.fail(5);

    // However the device's date is changed, elapsed time doesn't move, so no PIN is checked
    // (CS-13).
    for _ in 0..25 {
        assert!(matches!(fixture.sign_in(PIN, START), Outcome::LockedOut(_)));
    }
    assert_eq!(fixture.failures(), 5);

    assert_eq!(fixture.lockout(later(START, 29_999)), Ok(1));
    assert_eq!(
        fixture.sign_in(PIN, later(START, 30_000)),
        Outcome::Unlocked
    );
}

#[test]
fn the_lockout_expires_and_the_next_failure_doubles() {
    let fixture = Fixture::configured();
    fixture.fail(5);

    assert_eq!(
        fixture.sign_in(WRONG_PIN, later(START, 30_000)),
        Outcome::LockedOut(60_000)
    );
}

#[test]
fn a_reboot_restarts_the_lockout() {
    let fixture = Fixture::configured();
    fixture.fail(5);
    assert_eq!(fixture.lockout(later(START, 20_000)), Ok(10_000));

    let after_reboot = MonotonicTime {
        elapsed_millis: 5_000,
        boot_count: Some(8),
    };
    assert_eq!(fixture.lockout(after_reboot), Ok(30_000));
    assert_eq!(fixture.lockout(later(after_reboot, 30_000)), Ok(0));
}

#[test]
fn a_reboot_is_detected_without_a_boot_count() {
    let fixture = Fixture::configured();
    let unknown_boot = MonotonicTime {
        boot_count: None,
        ..START
    };
    for _ in 0..5 {
        fixture
            .profile
            .unlock_with_pin(WRONG_PIN, unknown_boot)
            .unwrap();
    }

    let after_reboot = MonotonicTime {
        elapsed_millis: 1_000,
        boot_count: None,
    };
    assert_eq!(fixture.lockout(after_reboot), Ok(30_000));
    assert_eq!(fixture.store.json()["lock_boot_count"], -1);
}

#[test]
fn a_boot_count_change_restarts_the_lockout_even_after_long_uptime() {
    let fixture = Fixture::configured();
    fixture.fail(5);

    let next_boot = MonotonicTime {
        boot_count: Some(8),
        ..later(START, 25_000)
    };
    assert_eq!(fixture.lockout(next_boot), Ok(30_000));
}

#[test]
fn a_served_lockout_isnt_restarted_by_a_reboot() {
    let fixture = Fixture::configured();
    fixture.fail(5);
    assert_eq!(fixture.lockout(later(START, 30_000)), Ok(0));

    let after_reboot = MonotonicTime {
        elapsed_millis: 1_000,
        boot_count: Some(8),
    };
    assert_eq!(fixture.lockout(after_reboot), Ok(0));
    assert_eq!(
        fixture.sign_in(WRONG_PIN, after_reboot),
        Outcome::LockedOut(60_000)
    );
}

#[test]
fn success_resets_the_failure_counter() {
    let fixture = Fixture::configured();
    fixture.fail(4);

    assert_eq!(fixture.sign_in(PIN, START), Outcome::Unlocked);
    assert_eq!(fixture.sign_in(WRONG_PIN, START), Outcome::Rejected(4));
}

#[test]
fn wrong_security_answers_share_the_lockout() {
    let fixture = Fixture::configured();
    for _ in 0..4 {
        assert_eq!(
            fixture.recover("wrong answer", "567890", START),
            Ok(Recovery::InvalidSecurityAnswer)
        );
    }

    assert_eq!(
        fixture.recover("wrong answer", "567890", START),
        Ok(Recovery::LockedOut {
            remaining_millis: 30_000
        })
    );
    assert_eq!(
        fixture.recover(ANSWER, "567890", later(START, 1_000)),
        Ok(Recovery::LockedOut {
            remaining_millis: 29_000
        })
    );
    assert_eq!(fixture.sign_in(PIN, START), Outcome::LockedOut(30_000));
}

#[test]
fn a_successful_recovery_clears_the_lockout() {
    let fixture = Fixture::configured();
    fixture.fail(4);

    assert_eq!(
        fixture.recover(ANSWER, "567890", START),
        Ok(Recovery::Recovered)
    );
    assert_eq!(fixture.sign_in(WRONG_PIN, START), Outcome::Rejected(4));
}

#[test]
fn concurrent_wrong_attempts_are_all_counted() {
    let fixture = Fixture::configured();

    std::thread::scope(|scope| {
        for _ in 0..4 {
            scope.spawn(|| fixture.profile.unlock_with_pin(WRONG_PIN, START).unwrap());
        }
    });

    assert_eq!(fixture.failures(), 4);
}
