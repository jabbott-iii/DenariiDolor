/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The error every operation of the report crate returns.

use std::fmt;

use denarii_core::{DomainError, EpochMillis, MoneyParseError, TransactionId};

/// Why a report or a search's filters couldn't be built.
///
/// The `Display` text is for tests and debugging; the bridge will turn each variant into a stable
/// code for the user interface.
#[derive(Debug, Clone, PartialEq, Eq)]
#[non_exhaustive]
pub enum ReportError {
    /// The year, the month, or the first moments of its days that the caller passed don't
    /// describe a calendar month (ADR-0021). This is the caller's bug, never the user's.
    InvalidMonthDays,
    /// A transaction the caller passed isn't dated in the report's month, so the caller read the
    /// rows of another month. This is the caller's bug, never the user's.
    DateOutsideMonth {
        /// The transaction's ID.
        id: TransactionId,
        /// Its date.
        date: EpochMillis,
    },
    /// A total, or the moment before a search's day after its end date, would go past the range
    /// of a 64-bit number. Only stored data that no validated write could leave, or a moment no
    /// calendar date stands for, causes it.
    Overflow,
    /// The search's minimum amount isn't an amount. v1's search screen showed its "invalid
    /// filters" message.
    InvalidMinAmount(MoneyParseError),
    /// The search's maximum amount isn't an amount. v1's search screen showed its "invalid
    /// filters" message.
    InvalidMaxAmount(MoneyParseError),
    /// The search breaks a business rule: [`DomainError::InvalidSearchRange`] for a start date
    /// after the end date, or a minimum amount above the maximum.
    Domain(DomainError),
}

impl fmt::Display for ReportError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::InvalidMonthDays => f.write_str(
                "the day starts don't describe the calendar month: each day needs its first \
                 moment, in order, spanning 0 to 48 hours",
            ),
            Self::DateOutsideMonth { id, date } => write!(
                f,
                "transaction {} is dated {}, outside the report's month",
                id.0, date.0
            ),
            Self::Overflow => f.write_str("a total went past the range of 64-bit cents"),
            Self::InvalidMinAmount(error) => write!(f, "the minimum amount is invalid: {error}"),
            Self::InvalidMaxAmount(error) => write!(f, "the maximum amount is invalid: {error}"),
            Self::Domain(error) => write!(f, "{error}"),
        }
    }
}

impl std::error::Error for ReportError {
    fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
        match self {
            Self::InvalidMinAmount(error) | Self::InvalidMaxAmount(error) => Some(error),
            Self::Domain(error) => Some(error),
            Self::InvalidMonthDays | Self::DateOutsideMonth { .. } | Self::Overflow => None,
        }
    }
}

#[cfg(test)]
mod tests {
    use std::error::Error;

    use denarii_core::{DomainError, EpochMillis, MoneyParseError, TransactionId};

    use super::ReportError;

    #[test]
    fn each_error_says_what_went_wrong_in_lowercase() {
        let cases = [
            (
                ReportError::InvalidMonthDays,
                "the day starts don't describe the calendar month: each day needs its first \
                 moment, in order, spanning 0 to 48 hours",
            ),
            (
                ReportError::DateOutsideMonth {
                    id: TransactionId(12),
                    date: EpochMillis(1_793_484_000_000),
                },
                "transaction 12 is dated 1793484000000, outside the report's month",
            ),
            (
                ReportError::Overflow,
                "a total went past the range of 64-bit cents",
            ),
        ];
        for (error, text) in cases {
            assert_eq!(error.to_string(), text);
            assert!(error.source().is_none());
        }
    }

    #[test]
    fn a_rejected_amount_or_rule_keeps_its_cause_as_the_source() {
        let min = ReportError::InvalidMinAmount(MoneyParseError::Malformed);
        assert!(
            min.to_string()
                .starts_with("the minimum amount is invalid: ")
        );
        assert_eq!(
            min.source().unwrap().to_string(),
            MoneyParseError::Malformed.to_string()
        );

        let max = ReportError::InvalidMaxAmount(MoneyParseError::TooManyDecimals);
        assert!(
            max.to_string()
                .starts_with("the maximum amount is invalid: ")
        );
        assert_eq!(
            max.source().unwrap().to_string(),
            MoneyParseError::TooManyDecimals.to_string()
        );

        let range = ReportError::Domain(DomainError::InvalidSearchRange);
        assert_eq!(
            range.to_string(),
            DomainError::InvalidSearchRange.to_string()
        );
        assert_eq!(
            range.source().unwrap().to_string(),
            DomainError::InvalidSearchRange.to_string()
        );
    }
}
