/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The error every operation of the store returns.

use std::fmt;

use denarii_core::{AccountId, CategoryId, DomainError, EpochMillis, MonthBounds, TransactionId};

/// Why the store couldn't open the database or run an operation on it.
///
/// No variant carries the key. The `Display` text is for tests and debugging; the bridge will
/// turn each variant into a stable code for the user interface. A write that fails with any
/// variant leaves the database as it was.
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
    /// The request breaks a business rule, such as an expense past its category's budget that
    /// the user hasn't confirmed. The user interface shows the message that
    /// [`DomainError::code`] names.
    Domain(DomainError),
    /// The transaction to edit or delete has no row. v1 showed its "item not found" message.
    TransactionNotFound {
        /// The ID the request named.
        id: TransactionId,
    },
    /// The category to edit or delete has no row. v1 showed its "item not found" message, unlike
    /// [`DomainError::CategoryNotFound`], which names a missing category that a transaction or a
    /// budget would refer to.
    CategoryNotFound {
        /// The ID the request named.
        id: CategoryId,
    },
    /// The account to rename or delete has no row. v1 showed its "item not found" message,
    /// unlike [`DomainError::AccountNotFound`], which names a missing account that a transaction
    /// would use.
    AccountNotFound {
        /// The ID the request named.
        id: AccountId,
    },
    /// The category has no budget to delete. v1 showed its "item not found" message.
    BudgetNotFound {
        /// The category the request named.
        category: CategoryId,
    },
    /// The transaction's date isn't in the month the caller passed, so the caller worked out the
    /// bounds of another month (ADR-0019). This is the caller's bug, never the user's.
    DateOutsideMonth {
        /// The transaction's date.
        date: EpochMillis,
        /// The bounds the caller passed.
        month: MonthBounds,
    },
    /// A balance, or a month's expenses in a category, would go past the range of a 64-bit
    /// number of cents. Only stored data that no validated write could leave causes it.
    Overflow,
    /// A balance change names an account that has no row. v1's checks never leave such data: it
    /// takes a damaged file, such as one holding a transfer whose destination was deleted.
    MissingAccount {
        /// The account the balance change names.
        id: AccountId,
    },
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
            Self::Domain(error) => {
                write!(f, "the request breaks a business rule ({})", error.code())
            }
            Self::TransactionNotFound { id } => write!(f, "no transaction has the ID {}", id.0),
            Self::CategoryNotFound { id } => write!(f, "no category has the ID {}", id.0),
            Self::AccountNotFound { id } => write!(f, "no account has the ID {}", id.0),
            Self::BudgetNotFound { category } => {
                write!(f, "category {} has no budget", category.0)
            }
            Self::DateOutsideMonth { .. } => {
                f.write_str("the transaction's date isn't in the month the caller passed")
            }
            Self::Overflow => f.write_str(
                "a balance or a month's expenses went past the range of a 64-bit number of cents",
            ),
            Self::MissingAccount { id } => write!(
                f,
                "a balance change names account {}, which has no row",
                id.0
            ),
            Self::Sqlite { operation, .. } => write!(f, "failed to {operation}"),
        }
    }
}

impl std::error::Error for StoreError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Self::Sqlite { source, .. } => Some(source),
            Self::Domain(error) => Some(error),
            Self::CipherUnavailable
            | Self::KeyRejected
            | Self::NewerSchema { .. }
            | Self::UnrecognizedSchema { .. }
            | Self::Poisoned
            | Self::TransactionNotFound { .. }
            | Self::CategoryNotFound { .. }
            | Self::AccountNotFound { .. }
            | Self::BudgetNotFound { .. }
            | Self::DateOutsideMonth { .. }
            | Self::Overflow
            | Self::MissingAccount { .. } => None,
        }
    }
}

/// Wraps an error from SQLite with what the store was doing when it happened.
pub(crate) fn sqlite(operation: &'static str, source: rusqlite::Error) -> StoreError {
    StoreError::Sqlite { operation, source }
}

#[cfg(test)]
mod tests {
    use std::error::Error;

    use denarii_core::{AccountId, CategoryId};

    use super::StoreError;

    /// The `Display` text is for tests and debugging, never for users, and names the row the
    /// request named. None of these errors has a source.
    #[test]
    fn a_missing_category_account_or_budget_displays_the_id() {
        let cases = [
            (
                StoreError::CategoryNotFound { id: CategoryId(9) },
                "no category has the ID 9",
            ),
            (
                StoreError::AccountNotFound { id: AccountId(7) },
                "no account has the ID 7",
            ),
            (
                StoreError::BudgetNotFound {
                    category: CategoryId(4),
                },
                "category 4 has no budget",
            ),
        ];
        for (error, text) in cases {
            assert_eq!(error.to_string(), text);
            assert!(error.source().is_none(), "{error:?}");
        }
    }
}
