/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The profile's plaintext: the JSON object that v1's `KeystoreProfileStore` sealed, with its keys
//! (`docs/specs/android-v1-formats.md`, section 3).
//!
//! v1 wrote it with Android's `org.json`, putting only strings and whole numbers. The getters read
//! those types and nothing else: a value of another type reads as absent, where `org.json` would
//! have converted it, but v1 never wrote one. Keys this crate doesn't know are kept on every edit,
//! as v1 kept them.

use serde_json::{Map, Value};
use zeroize::Zeroizing;

/// `profile_version`: 2 for this format.
pub(crate) const PROFILE_VERSION: &str = "profile_version";
/// `security_question`: the question, trimmed.
pub(crate) const SECURITY_QUESTION: &str = "security_question";
/// `failed_attempts`: failed PIN and answer checks since the last success.
pub(crate) const FAILED_ATTEMPTS: &str = "failed_attempts";
/// `lock_duration_millis`: the length of the current lockout.
pub(crate) const LOCK_DURATION: &str = "lock_duration_millis";
/// `lock_started_at_elapsed_millis`: the time since boot when the lockout started.
pub(crate) const LOCK_STARTED_AT: &str = "lock_started_at_elapsed_millis";
/// `lock_boot_count`: the boot count when the lockout started, or −1.
pub(crate) const LOCK_BOOT_COUNT: &str = "lock_boot_count";

/// The format version this crate reads and writes.
pub(crate) const CURRENT_VERSION: i32 = 2;

/// The profile's values, keyed as v1 keyed them.
#[derive(Debug, Clone, Default, PartialEq)]
pub(crate) struct ProfileValues(Map<String, Value>);

impl ProfileValues {
    /// The values `json` holds, or `None` when it isn't a JSON object.
    pub(crate) fn parse(json: &[u8]) -> Option<Self> {
        match serde_json::from_slice(json) {
            Ok(Value::Object(values)) => Some(Self(values)),
            Ok(_) | Err(_) => None,
        }
    }

    /// The values as a JSON object, wiped when dropped.
    #[expect(
        clippy::expect_used,
        reason = "a JSON object with string keys, which serde_json parsed or this type built, always serializes"
    )]
    pub(crate) fn to_json(&self) -> Zeroizing<Vec<u8>> {
        let mut json = Vec::new();
        serde_json::to_writer(&mut json, &self.0).expect("JSON values always serialize");
        Zeroizing::new(json)
    }

    /// The text at `key`, or `None` when it is absent or isn't a string.
    pub(crate) fn string(&self, key: &str) -> Option<&str> {
        self.0.get(key)?.as_str()
    }

    /// The Kotlin `Int` at `key`, or `None` when it is absent, isn't a whole number, or doesn't fit.
    pub(crate) fn int(&self, key: &str) -> Option<i32> {
        i32::try_from(self.long(key)?).ok()
    }

    /// The Kotlin `Long` at `key`, or `None` when it is absent, isn't a whole number, or doesn't
    /// fit.
    pub(crate) fn long(&self, key: &str) -> Option<i64> {
        self.0.get(key)?.as_i64()
    }

    pub(crate) fn put_string(&mut self, key: &str, value: &str) {
        self.0.insert(key.to_owned(), Value::from(value));
    }

    pub(crate) fn put_int(&mut self, key: &str, value: i32) {
        self.0.insert(key.to_owned(), Value::from(value));
    }

    pub(crate) fn put_long(&mut self, key: &str, value: i64) {
        self.0.insert(key.to_owned(), Value::from(value));
    }

    pub(crate) fn remove(&mut self, key: &str) {
        self.0.remove(key);
    }

    /// Removes every value, as setup did before it wrote a new profile.
    pub(crate) fn clear(&mut self) {
        self.0.clear();
    }
}

#[cfg(test)]
mod tests {
    use super::ProfileValues;

    #[test]
    fn reads_the_types_v1_wrote() {
        let values = ProfileValues::parse(
            concat!(
                r#"{"profile_version":2,"security_question":"Pet\/dog \u00e9?","#,
                r#""lock_started_at_elapsed_millis":9007199254740993,"lock_boot_count":-1}"#,
            )
            .as_bytes(),
        )
        .unwrap();
        assert_eq!(values.int("profile_version"), Some(2));
        assert_eq!(values.string("security_question"), Some("Pet/dog é?"));
        assert_eq!(
            values.long("lock_started_at_elapsed_millis"),
            Some(9_007_199_254_740_993)
        );
        assert_eq!(values.int("lock_boot_count"), Some(-1));
        assert_eq!(values.int("missing"), None);
        assert_eq!(values.string("missing"), None);
    }

    #[test]
    fn other_types_read_as_absent() {
        let values = ProfileValues::parse(
            br#"{"a":"2","b":2.0,"c":2147483648,"d":true,"e":null,"f":[2],"g":{"h":2},"i":18446744073709551615}"#,
        )
        .unwrap();
        for key in ["a", "b", "c", "d", "e", "f", "g", "i"] {
            assert_eq!(values.int(key), None, "{key}");
        }
        for key in ["b", "d", "e", "f", "g", "i"] {
            assert_eq!(values.long(key), None, "{key}");
            assert_eq!(values.string(key), None, "{key}");
        }
        assert_eq!(values.long("c"), Some(2_147_483_648));
    }

    #[test]
    fn refuses_anything_but_a_json_object() {
        for json in [
            &b""[..],
            b"[]",
            b"2",
            b"\"profile\"",
            b"null",
            b"{",
            b"{\"a\":1,}",
            b"\xff",
        ] {
            assert_eq!(ProfileValues::parse(json), None, "{json:?}");
        }
    }

    #[test]
    fn edits_keep_unknown_keys_and_write_valid_json() {
        let mut values =
            ProfileValues::parse(br#"{"biometric_wrap":"AAEC","failed_attempts":3}"#).unwrap();
        values.put_int("failed_attempts", 4);
        values.put_long("lock_duration_millis", 30_000);
        values.put_string("security_question", "Quote \" and \\ and \u{1}?");
        values.remove("absent");
        let written = values.to_json();
        let read = ProfileValues::parse(&written).unwrap();
        assert_eq!(read, values);
        assert_eq!(read.string("biometric_wrap"), Some("AAEC"));
        assert_eq!(read.int("failed_attempts"), Some(4));

        values.clear();
        assert_eq!(values.to_json().as_slice(), b"{}");
    }
}
