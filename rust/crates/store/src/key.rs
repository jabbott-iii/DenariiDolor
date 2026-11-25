/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The database key, and the raw-key text SQLCipher reads it from.
//!
//! The key is a module of its own because it is the one secret this crate holds: it never
//! appears in `Debug` output or an error, and every copy of it is wiped once it is used.

use std::fmt;
use std::hint::black_box;

/// The 32 random bytes that encrypt the database (CS-15).
///
/// The vault (`denarii-vault`) unwraps the key at sign-in, and the bridge passes it to
/// [`Store::open`] (M3.5). SQLCipher uses it as the encryption key directly, with no key
/// derivation, because it already holds 256 bits of randomness (section 5 of
/// `docs/specs/android-v1-formats.md`).
///
/// `Debug` never shows the bytes, and dropping the key overwrites them with zeros. The wipe is
/// best effort: copies the compiler makes when a key moves, and the caller's own array, aren't
/// wiped. The vault wipes its own copies with `zeroize` (ADR-0023); this crate keeps its own wipe.
///
/// [`Store::open`]: crate::Store::open
pub struct DatabaseKey([u8; DatabaseKey::LEN]);

impl DatabaseKey {
    /// The key's length in bytes.
    pub const LEN: usize = 32;

    /// Wraps the key's bytes.
    #[must_use]
    pub fn from_bytes(bytes: [u8; Self::LEN]) -> Self {
        Self(bytes)
    }

    fn as_bytes(&self) -> &[u8; Self::LEN] {
        &self.0
    }
}

impl fmt::Debug for DatabaseKey {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("DatabaseKey(..)")
    }
}

impl Drop for DatabaseKey {
    fn drop(&mut self) {
        wipe(&mut self.0);
    }
}

/// The statement that hands a key to SQLCipher, built once and wiped when dropped.
///
/// SQLCipher's raw-key syntax is the text `x'<64 lowercase hex digits>'`, which v1 passed as its
/// passphrase (`DatabaseKeys.toRawKeyPassphrase` at `6d57526`). The statement is
/// `PRAGMA key = "x'<hex>'";`, the form SQLCipher's documentation gives.
pub(crate) struct KeyPragma(String);

impl KeyPragma {
    const PREFIX: &'static str = "PRAGMA key = \"x'";
    const SUFFIX: &'static str = "'\";";
    /// The statement's length: the prefix (16 bytes), two hex digits per key byte (64), and the
    /// suffix (3). The string is allocated with exactly this capacity, so it never reallocates
    /// and leaves no unwiped copy of the key behind.
    pub(crate) const LEN: usize = 83;

    pub(crate) fn new(key: &DatabaseKey) -> Self {
        let mut sql = String::with_capacity(Self::LEN);
        sql.push_str(Self::PREFIX);
        for &byte in key.as_bytes() {
            sql.push(char::from(hex_digit(byte >> 4)));
            sql.push(char::from(hex_digit(byte)));
        }
        sql.push_str(Self::SUFFIX);
        Self(sql)
    }

    pub(crate) fn as_str(&self) -> &str {
        &self.0
    }
}

impl Drop for KeyPragma {
    fn drop(&mut self) {
        let mut bytes = std::mem::take(&mut self.0).into_bytes();
        wipe(&mut bytes);
    }
}

/// The lowercase hex digit for the low four bits of `nibble`.
#[expect(
    clippy::indexing_slicing,
    reason = "the mask keeps the index below 16, the table's length"
)]
fn hex_digit(nibble: u8) -> u8 {
    b"0123456789abcdef"[usize::from(nibble & 0x0f)]
}

/// Overwrites `bytes` with zeros. `black_box` keeps the compiler from dropping the writes as
/// dead stores before the memory is freed; it is a hint, not a guarantee.
fn wipe(bytes: &mut [u8]) {
    bytes.fill(0);
    black_box(bytes);
}

#[cfg(test)]
mod tests {
    use super::{DatabaseKey, KeyPragma};

    fn counting_key() -> DatabaseKey {
        DatabaseKey::from_bytes(std::array::from_fn(|i| u8::try_from(i).unwrap()))
    }

    #[test]
    fn writes_v1_raw_key_syntax_in_lowercase_hex() {
        let pragma = KeyPragma::new(&counting_key());
        assert_eq!(
            pragma.as_str(),
            "PRAGMA key = \"x'000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f'\";"
        );
    }

    #[test]
    fn fills_its_capacity_exactly_so_it_never_reallocates() {
        let pragma = KeyPragma::new(&DatabaseKey::from_bytes([0xff; DatabaseKey::LEN]));
        assert_eq!(pragma.as_str().len(), KeyPragma::LEN);
        assert_eq!(pragma.0.capacity(), KeyPragma::LEN);
        assert!(pragma.as_str().contains(&"ff".repeat(DatabaseKey::LEN)));
    }

    #[test]
    fn debug_output_hides_the_key() {
        assert_eq!(format!("{:?}", counting_key()), "DatabaseKey(..)");
    }
}
