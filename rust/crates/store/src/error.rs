/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The error every operation of the store returns.

use std::fmt;

/// Why the store couldn't open the database or run an operation on it.
///
/// No variant carries the key. The `Display` text is for tests and debugging; the bridge will
/// turn each variant into a stable code for the user interface.
#[derive(Debug)]
#[non_exhaustive]
pub enum StoreError {
    /// The SQLite library in this build has no SQLCipher codec, so it would ignore the key and
    /// write the database unencrypted. This is a build error, never a user's.
    CipherUnavailable,
    /// The file didn't open with this key: the key is wrong, or the file isn't a SQLCipher
    /// database (a plaintext SQLite file, a damaged file, or another kind of file). Nothing in the
    /// file was changed.
    KeyRejected,
    /// An earlier call panicked while it held the connection, so the connection's state is
    /// unknown and the store refuses to use it.
    Poisoned,
    /// SQLite or SQLCipher reported an error.
    Sqlite {
        /// What the store was doing, such as `"open the database file"`.
        operation: &'static str,
        /// The error SQLite reported.
        source: rusqlite::Error,
    },
}

impl fmt::Display for StoreError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::CipherUnavailable => {
                f.write_str("the SQLite library in this build has no SQLCipher codec")
            }
            Self::KeyRejected => f.write_str(
                "the database didn't open with this key: the key is wrong, or the file isn't a \
                 SQLCipher database",
            ),
            Self::Poisoned => {
                f.write_str("an earlier call panicked while it held the database connection")
            }
            Self::Sqlite { operation, .. } => write!(f, "failed to {operation}"),
        }
    }
}

impl std::error::Error for StoreError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Self::Sqlite { source, .. } => Some(source),
            Self::CipherUnavailable | Self::KeyRejected | Self::Poisoned => None,
        }
    }
}
