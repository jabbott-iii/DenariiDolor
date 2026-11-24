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
    /// The file's schema version is newer than any this build reads: a later build of the app
    /// wrote it. Nothing in the file was changed.
    NewerSchema {
        /// The file's schema version, from `PRAGMA user_version`.
        version: i32,
    },
    /// The file opened with the key, but its schema isn't one this build knows: a negative
    /// schema version, a version 0 file that held at least one byte before it opened, or a
    /// version 2 file without version 2's Room identity hash (ADR-0018). Nothing in the file was
    /// changed.
    UnrecognizedSchema {
        /// The file's schema version, from `PRAGMA user_version`.
        version: i32,
    },
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
            Self::NewerSchema { version } => write!(
                f,
                "the database's schema version, {version}, is newer than this build reads"
            ),
            Self::UnrecognizedSchema { version } => write!(
                f,
                "the database's schema isn't one this build knows (schema version {version})"
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
            Self::CipherUnavailable
            | Self::KeyRejected
            | Self::NewerSchema { .. }
            | Self::UnrecognizedSchema { .. }
            | Self::Poisoned => None,
        }
    }
}

/// Wraps an error from SQLite with what the store was doing when it happened.
pub(crate) fn sqlite(operation: &'static str, source: rusqlite::Error) -> StoreError {
    StoreError::Sqlite { operation, source }
}
