/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The database key, the one secret the profile guards.

use std::fmt;

use zeroize::Zeroizing;

/// The 32 random bytes that encrypt the database (CS-15), as a PIN or an answer unwrapped them.
///
/// `Debug` never shows the bytes, and dropping the key overwrites them with zeros. The wipe is
/// best effort: copies that the compiler makes when the key moves aren't wiped. The caller hands
/// the bytes to `denarii_store::DatabaseKey`, which wipes its own copy.
pub struct DatabaseKey(Zeroizing<[u8; DatabaseKey::LEN]>);

impl DatabaseKey {
    /// The key's length in bytes.
    pub const LEN: usize = 32;

    pub(crate) fn from_bytes(bytes: [u8; Self::LEN]) -> Self {
        Self(Zeroizing::new(bytes))
    }

    /// The key's bytes.
    #[must_use]
    pub fn as_bytes(&self) -> &[u8; Self::LEN] {
        &self.0
    }
}

impl fmt::Debug for DatabaseKey {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("DatabaseKey(..)")
    }
}

#[cfg(test)]
mod tests {
    use super::DatabaseKey;

    #[test]
    fn debug_output_hides_the_key() {
        let key = DatabaseKey::from_bytes([0xA5; DatabaseKey::LEN]);
        assert_eq!(format!("{key:?}"), "DatabaseKey(..)");
        assert_eq!(key.as_bytes(), &[0xA5; DatabaseKey::LEN]);
    }
}
