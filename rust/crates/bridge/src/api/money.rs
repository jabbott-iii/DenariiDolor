/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Reading an amount the user typed, with v1's rules, through [`denarii_core::parse_cents`].

use std::fmt;

use denarii_core::MoneyParseError;

/// What parsing needs from the user's locale: its decimal separator, and the ways it writes the
/// currency. It carries what [`denarii_core::NumberSymbols`] holds, in types that cross the bridge.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NumberSymbols {
    /// The locale's decimal separator, which must be exactly one character: `.` in English, `,` in
    /// German, `٫` in Arabic. It crosses the bridge as text, because a Dart `String` can hold any
    /// number of characters, and this crate checks the count itself.
    pub decimal_separator: String,
    /// The currency's symbol in the user's locale, its symbol in the default locale, and its
    /// ISO 4217 code, in that order; for example `["€", "€", "EUR"]`. It may be empty.
    pub currency_marks: Vec<String>,
}

/// Why [`parse_cents`] rejected an amount. The first four variants are
/// [`denarii_core::MoneyParseError`]'s; the last is a mistake in the caller's
/// [`NumberSymbols`].
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum AmountError {
    /// The text holds no number: it is blank, or only a sign or a currency mark.
    Empty,
    /// The text holds a character that isn't part of a number, or its decimal mark or thousands
    /// grouping is malformed.
    Malformed,
    /// The amount has more than two decimals once trailing zeros are removed.
    TooManyDecimals,
    /// The amount is larger than [`denarii_core::Cents::MAX_PARSED`], 9,999,999,999.99.
    TooLarge,
    /// [`NumberSymbols::decimal_separator`] isn't exactly one character. The caller built the
    /// symbols wrong; the user's typing can't cause it.
    InvalidDecimalSeparator,
}

impl fmt::Display for AmountError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(match self {
            Self::Empty => "the text holds no amount",
            Self::Malformed => "the text isn't a well-formed amount",
            Self::TooManyDecimals => "the amount has more than two decimals",
            Self::TooLarge => "the amount is larger than 9,999,999,999.99",
            Self::InvalidDecimalSeparator => "the decimal separator isn't exactly one character",
        })
    }
}

impl std::error::Error for AmountError {}

/// Parses an amount a user typed, such as `12.5`, `$1,234.56`, or `١٢٫٥`, into a whole number of
/// cents, with v1's rules. [`denarii_core::parse_cents`] describes them.
///
/// # Errors
///
/// - [`AmountError::InvalidDecimalSeparator`] when `symbols.decimal_separator` isn't exactly one
///   character. It is checked before the text is read.
/// - [`AmountError::Empty`], [`AmountError::Malformed`], [`AmountError::TooManyDecimals`], or
///   [`AmountError::TooLarge`] when [`denarii_core::parse_cents`] rejects the text with the
///   [`denarii_core::MoneyParseError`] variant of the same name.
///
/// # Examples
///
/// ```
/// use denarii_bridge::api::money::{AmountError, NumberSymbols, parse_cents};
///
/// let german = NumberSymbols {
///     decimal_separator: ",".to_owned(),
///     currency_marks: vec!["€".to_owned(), "€".to_owned(), "EUR".to_owned()],
/// };
/// assert_eq!(parse_cents("1.234,56 €".to_owned(), german.clone()), Ok(123_456));
/// assert_eq!(parse_cents("12,345".to_owned(), german), Err(AmountError::TooManyDecimals));
/// ```
#[expect(
    clippy::needless_pass_by_value,
    reason = "the generated code passes each argument from Dart by value"
)]
pub fn parse_cents(text: String, symbols: NumberSymbols) -> Result<i64, AmountError> {
    let mut separators = symbols.decimal_separator.chars();
    let (Some(decimal_separator), None) = (separators.next(), separators.next()) else {
        return Err(AmountError::InvalidDecimalSeparator);
    };
    let symbols = denarii_core::NumberSymbols {
        decimal_separator,
        currency_marks: symbols.currency_marks,
    };
    denarii_core::parse_cents(&text, &symbols)
        .map(|cents| cents.0)
        .map_err(amount_error)
}

#[expect(
    clippy::match_same_arms,
    reason = "a variant added to MoneyParseError later is rejected as Malformed on purpose"
)]
fn amount_error(error: MoneyParseError) -> AmountError {
    match error {
        MoneyParseError::Empty => AmountError::Empty,
        MoneyParseError::Malformed => AmountError::Malformed,
        MoneyParseError::TooManyDecimals => AmountError::TooManyDecimals,
        MoneyParseError::TooLarge => AmountError::TooLarge,
        // `MoneyParseError` is `#[non_exhaustive]`, so another crate's match needs this arm. A
        // variant added later still means the amount was rejected; give it its own `AmountError`
        // variant when it is added.
        _ => AmountError::Malformed,
    }
}
