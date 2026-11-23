/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Amounts of money, held as whole cents, and the conversions between them and plain text.
//!
//! Every amount the app stores or adds up is a [`Cents`] value, never a floating-point number,
//! so 10 cents added three times is exactly 30 cents. [`parse_cents`] reads what a user typed with
//! v1's rules (`Money.parseToCents` in v1's `Money.kt`), so text that v1 accepted gives the same
//! number of cents here, and text it rejected is rejected here too. The user's locale enters only
//! through [`NumberSymbols`], which the caller fills from the platform's locale data.
//!
//! Writing an amount the way a locale writes money, such as `$1,234.50` or `1 234,50 €`, is the
//! user interface's job; this module writes only the plain forms that CSV files and input fields
//! use ([`Cents::to_plain`] and [`Cents::to_input`]).

use std::cmp::Reverse;
use std::fmt;

use crate::error::ArithmeticOverflow;
use crate::text;

/// Cents per whole unit of a currency. Only currencies with two decimals are allowed.
const CENTS_PER_UNIT: i64 = 100;

/// Digits after the decimal mark.
const SCALE: usize = 2;

/// Digits in one thousands group.
const GROUP_DIGITS: usize = 3;

/// The most digits before the decimal mark that [`parse_cents`] accepts, after leading zeros.
pub const MAX_WHOLE_DIGITS: usize = 10;

/// The two characters either of which can be the decimal mark, or a thousands separator.
const DECIMAL_MARKS: [char; 2] = ['.', ','];

/// Characters that only ever separate thousands: a space, a no-break space, a narrow no-break
/// space, and two apostrophes, as in French `1 234,5` or Swiss German `1'234.5`.
const GROUP_ONLY_MARKS: [char; 5] = [' ', '\u{A0}', '\u{202F}', '\'', '\u{2019}'];

const ARABIC_DECIMAL_SEPARATOR: char = '\u{066B}';
const ARABIC_THOUSANDS_SEPARATOR: char = '\u{066C}';
const MINUS_SIGN: char = '\u{2212}';

/// The left-to-right mark, the right-to-left mark, and the Arabic letter mark, which formatted
/// Arabic and Hebrew amounts contain.
const DIRECTION_MARKS: [char; 3] = ['\u{200E}', '\u{200F}', '\u{061C}'];

/// An amount of money in cents, the hundredths of a currency's unit.
///
/// The value can be negative: an expense's effect on a balance is negative, for example. All
/// arithmetic is checked and returns [`ArithmeticOverflow`] instead of wrapping.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord, Default)]
pub struct Cents(pub i64);

impl Cents {
    /// No money.
    pub const ZERO: Self = Self(0);

    /// The largest amount [`parse_cents`] accepts: 9,999,999,999.99.
    pub const MAX_PARSED: Self = Self(999_999_999_999);

    /// Whether the amount is more than zero.
    #[must_use]
    pub const fn is_positive(self) -> bool {
        self.0 > 0
    }

    /// The sum of two amounts.
    ///
    /// # Errors
    ///
    /// Returns [`ArithmeticOverflow`] if the sum doesn't fit in an `i64`.
    pub fn checked_add(self, other: Self) -> Result<Self, ArithmeticOverflow> {
        self.0
            .checked_add(other.0)
            .map(Self)
            .ok_or(ArithmeticOverflow)
    }

    /// The difference of two amounts.
    ///
    /// # Errors
    ///
    /// Returns [`ArithmeticOverflow`] if the difference doesn't fit in an `i64`.
    pub fn checked_sub(self, other: Self) -> Result<Self, ArithmeticOverflow> {
        self.0
            .checked_sub(other.0)
            .map(Self)
            .ok_or(ArithmeticOverflow)
    }

    /// The amount with its sign flipped.
    ///
    /// # Errors
    ///
    /// Returns [`ArithmeticOverflow`] for `i64::MIN` cents, whose negation doesn't fit.
    pub fn checked_neg(self) -> Result<Self, ArithmeticOverflow> {
        self.0.checked_neg().map(Self).ok_or(ArithmeticOverflow)
    }

    /// The amount as a plain decimal with two decimals, such as `1234.50` or `-0.05`, which CSV
    /// files use.
    #[must_use]
    pub fn to_plain(self) -> String {
        let sign = if self.0 < 0 { "-" } else { "" };
        // `unsigned_abs` can't overflow, even for `i64::MIN`, and both divisions truncate toward
        // zero, as Kotlin's do.
        let units = (self.0 / CENTS_PER_UNIT).unsigned_abs();
        let rest = (self.0 % CENTS_PER_UNIT).unsigned_abs();
        format!("{sign}{units}.{rest:02}")
    }

    /// The amount as an input field shows it for editing, without trailing zeros: `4.5`, `250`.
    #[must_use]
    pub fn to_input(self) -> String {
        let plain = self.to_plain();
        let trimmed = plain.trim_end_matches('0');
        trimmed.strip_suffix('.').unwrap_or(trimmed).to_owned()
    }
}

/// What parsing needs from the user's locale: its decimal separator, and the ways it writes the
/// currency.
///
/// v1 filled it from `java.text.DecimalFormatSymbols` and `java.util.Currency`; the Flutter app
/// fills it from `package:intl` (task M1.6).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NumberSymbols {
    /// The locale's decimal separator: `.` in English, `,` in German, `٫` in Arabic.
    pub decimal_separator: char,
    /// The currency's symbol in the user's locale, its symbol in the default locale, and its
    /// ISO 4217 code, in that order, as v1 passed them; for example `["€", "€", "EUR"]`.
    pub currency_marks: Vec<String>,
}

/// Why [`parse_cents`] rejected a text.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[non_exhaustive]
pub enum MoneyParseError {
    /// The text holds no number: it is blank, or only a sign or a currency mark.
    Empty,
    /// The text holds a character that isn't part of a number, or its decimal mark or thousands
    /// grouping is malformed.
    Malformed,
    /// The amount has more than two decimals once trailing zeros are removed.
    TooManyDecimals,
    /// The amount is larger than [`Cents::MAX_PARSED`].
    TooLarge,
}

impl MoneyParseError {
    /// A stable name for the error, which doesn't change when the message is reworded.
    #[must_use]
    pub const fn code(self) -> &'static str {
        match self {
            Self::Empty => "EMPTY",
            Self::Malformed => "MALFORMED",
            Self::TooManyDecimals => "TOO_MANY_DECIMALS",
            Self::TooLarge => "TOO_LARGE",
        }
    }
}

impl fmt::Display for MoneyParseError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(match self {
            Self::Empty => "the text holds no amount",
            Self::Malformed => "the text isn't a well-formed amount",
            Self::TooManyDecimals => "the amount has more than two decimals",
            Self::TooLarge => "the amount is larger than 9,999,999,999.99",
        })
    }
}

impl std::error::Error for MoneyParseError {}

/// Parses an amount a user typed, such as `12`, `12.5`, `$1,234.56`, `12,50 €`, `1.234,56`,
/// `1 234,56`, or `١٢٫٥`, into cents.
///
/// The rules are v1's:
///
/// - The currency's symbol or code may come before or after the number, and a minus sign may come
///   before or after a leading symbol (`-$5` and `$-5`). A plus sign isn't accepted.
/// - Any decimal digit in the Basic Multilingual Plane counts, such as Arabic-Indic `٣` or
///   Devanagari `३` (see [`normalize_digits`]).
/// - Either `.` or `,` can be the decimal mark. When both appear, the last one is. A single mark
///   followed by one or two digits always is. A single mark followed by exactly three digits, as
///   in `1,234`, is the decimal mark only if it is the locale's decimal separator; otherwise it
///   separates thousands.
/// - Thousands groups must be well formed: one to three digits, then groups of exactly three.
///
/// # Errors
///
/// - [`MoneyParseError::Empty`] when no number is left after the sign and the currency mark.
/// - [`MoneyParseError::Malformed`] for any other character, malformed grouping, or a decimal mark
///   with no digits after it.
/// - [`MoneyParseError::TooManyDecimals`] for more than two decimals, trailing zeros aside.
/// - [`MoneyParseError::TooLarge`] for more than [`Cents::MAX_PARSED`].
///
/// # Examples
///
/// ```
/// use denarii_core::{Cents, NumberSymbols, parse_cents};
///
/// # fn main() -> Result<(), Box<dyn std::error::Error>> {
/// let german = NumberSymbols {
///     decimal_separator: ',',
///     currency_marks: vec!["€".to_owned(), "€".to_owned(), "EUR".to_owned()],
/// };
/// assert_eq!(parse_cents("1.234,56 €", &german)?, Cents(123_456));
/// assert_eq!(parse_cents("12.000", &german)?, Cents(1_200_000));
/// # Ok(())
/// # }
/// ```
pub fn parse_cents(text: &str, symbols: &NumberSymbols) -> Result<Cents, MoneyParseError> {
    let normalized = normalize_digits(text);
    let (negative, body) = split_sign(&normalized, &symbols.currency_marks)?;
    let separator = symbols.decimal_separator;
    let locale_decimal = normalize_char(separator).unwrap_or(separator);
    let (whole, fraction) = parse_unsigned(body, locale_decimal)?;
    let cents = to_cents(&whole, &fraction)?;
    if negative {
        cents
            .checked_neg()
            .map_err(|ArithmeticOverflow| MoneyParseError::TooLarge)
    } else {
        Ok(cents)
    }
}

/// The text with every decimal digit in the Basic Multilingual Plane written as an ASCII digit,
/// the Arabic decimal and thousands separators written as `.` and `,`, the minus sign `−` written
/// as `-`, and direction marks removed.
///
/// # Examples
///
/// ```
/// assert_eq!(denarii_core::normalize_digits("١٢٫٥٠"), "12.50");
/// assert_eq!(denarii_core::normalize_digits("\u{200F}\u{2212}५"), "-5");
/// ```
#[must_use]
pub fn normalize_digits(text: &str) -> String {
    text.chars().filter_map(normalize_char).collect()
}

/// What [`normalize_digits`] makes of one character; `None` drops it.
fn normalize_char(c: char) -> Option<char> {
    if c.is_ascii_digit() {
        return Some(c);
    }
    if let Some(digit) = text::decimal_digit_value(c) {
        return char::from_digit(digit, 10);
    }
    match c {
        ARABIC_DECIMAL_SEPARATOR => Some('.'),
        ARABIC_THOUSANDS_SEPARATOR => Some(','),
        MINUS_SIGN => Some('-'),
        _ if DIRECTION_MARKS.contains(&c) => None,
        _ => Some(c),
    }
}

/// Whether the amount is negative, and the text left once the sign and the currency are removed.
/// Accepts `-12`, `-$12`, `$-12`, `$12`, and `12 €`.
fn split_sign<'a>(
    text: &'a str,
    currency_marks: &[String],
) -> Result<(bool, &'a str), MoneyParseError> {
    let trimmed = text::trim(text);
    let (leading_minus, rest) = match trimmed.strip_prefix('-') {
        Some(rest) => (true, rest),
        None => (false, trimmed),
    };
    let body = strip_currency(rest, currency_marks);
    let (minus_after_symbol, body) = match body.strip_prefix('-') {
        Some(rest) if !leading_minus => (true, rest),
        _ => (false, body),
    };
    if body.is_empty() {
        Err(MoneyParseError::Empty)
    } else {
        Ok((leading_minus || minus_after_symbol, body))
    }
}

/// The text without the currency's symbol or code at either end, such as `R$`, `US$`, `EUR`, or
/// `ر.س.`, and without any currency sign or whitespace at either end.
fn strip_currency<'a>(text: &'a str, currency_marks: &[String]) -> &'a str {
    let mut marks: Vec<String> = Vec::with_capacity(currency_marks.len());
    for mark in currency_marks {
        let mark = normalize_digits(mark);
        if !text::is_blank(&mark) && !marks.contains(&mark) {
            marks.push(mark);
        }
    }
    // Longest first, so `US$` goes before `$`. The sort is stable, as v1's was.
    marks.sort_by_key(|mark| Reverse(text::utf16_len(mark)));
    let mut body = text::trim(text);
    for mark in &marks {
        let without_prefix = body.strip_prefix(mark.as_str()).unwrap_or(body);
        let without_suffix = without_prefix
            .strip_suffix(mark.as_str())
            .unwrap_or(without_prefix);
        body = text::trim(without_suffix);
    }
    body.trim_matches(|c| text::is_whitespace(c) || text::is_currency_symbol(c))
}

/// The whole-number digits and the fraction digits of an unsigned amount.
fn parse_unsigned(body: &str, locale_decimal: char) -> Result<(String, String), MoneyParseError> {
    let allowed =
        |c: &char| c.is_ascii_digit() || DECIMAL_MARKS.contains(c) || GROUP_ONLY_MARKS.contains(c);
    if !body.chars().all(|c| allowed(&c)) {
        return Err(MoneyParseError::Malformed);
    }
    // Every character is now in the Basic Multilingual Plane, so `char` positions are the
    // UTF-16 positions v1 counted.
    let chars: Vec<char> = body.chars().collect();
    let (whole, fraction) = match decimal_mark_index(&chars, locale_decimal) {
        None => (chars.as_slice(), None),
        Some(index) => {
            let (whole, rest) = chars
                .split_at_checked(index)
                .ok_or(MoneyParseError::Malformed)?;
            let fraction = rest.split_first().map_or(&[][..], |(_, digits)| digits);
            (whole, Some(fraction))
        }
    };
    let whole = ungroup(whole).ok_or(MoneyParseError::Malformed)?;
    let fraction = match fraction {
        None => String::new(),
        Some(digits) if !digits.is_empty() && digits.iter().all(char::is_ascii_digit) => {
            digits.iter().collect()
        }
        Some(_) => return Err(MoneyParseError::Malformed),
    };
    if whole.is_empty() && fraction.is_empty() {
        return Err(MoneyParseError::Malformed);
    }
    Ok((whole, fraction))
}

/// The position of the decimal mark, or `None` when every `.` and `,` separates thousands.
fn decimal_mark_index(chars: &[char], locale_decimal: char) -> Option<usize> {
    let marks: Vec<(usize, char)> = chars
        .iter()
        .copied()
        .enumerate()
        .filter(|(_, c)| DECIMAL_MARKS.contains(c))
        .collect();
    let &(last, last_mark) = marks.last()?;
    let digits_after = chars.iter().skip(last).skip(1).count();
    if marks.iter().any(|&(_, mark)| mark != last_mark) {
        Some(last)
    } else if marks.len() > 1 || (digits_after == GROUP_DIGITS && last_mark != locale_decimal) {
        None
    } else {
        Some(last)
    }
}

/// The whole-number digits with well-formed thousands grouping removed, or `None` when the
/// grouping is malformed.
fn ungroup(whole: &[char]) -> Option<String> {
    let mut separators: Vec<char> = Vec::new();
    for &c in whole {
        if !c.is_ascii_digit() && !separators.contains(&c) {
            separators.push(c);
        }
    }
    let Some(&separator) = separators.first() else {
        return Some(whole.iter().collect());
    };
    if separators.len() > 1 {
        return None;
    }
    let groups: Vec<&[char]> = whole.split(|&c| c == separator).collect();
    let (first, rest) = groups.split_first()?;
    let well_formed = (1..=GROUP_DIGITS).contains(&first.len())
        && rest.iter().all(|group| group.len() == GROUP_DIGITS);
    well_formed.then(|| groups.concat().into_iter().collect())
}

/// The amount in cents from its ASCII whole-number and fraction digits.
fn to_cents(whole: &str, fraction: &str) -> Result<Cents, MoneyParseError> {
    let units = whole.trim_start_matches('0');
    let decimals = fraction.trim_end_matches('0');
    if decimals.len() > SCALE {
        return Err(MoneyParseError::TooManyDecimals);
    }
    if units.len() > MAX_WHOLE_DIGITS {
        return Err(MoneyParseError::TooLarge);
    }
    let units: i64 = if units.is_empty() {
        0
    } else {
        units.parse().map_err(|_| MoneyParseError::Malformed)?
    };
    let decimals: i64 = format!("{decimals:0<SCALE$}")
        .parse()
        .map_err(|_| MoneyParseError::Malformed)?;
    units
        .checked_mul(CENTS_PER_UNIT)
        .and_then(|cents| cents.checked_add(decimals))
        .map(Cents)
        .ok_or(MoneyParseError::TooLarge)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn decimal_mark_is_the_last_of_mixed_marks() {
        let chars: Vec<char> = "1.234,56".chars().collect();
        assert_eq!(decimal_mark_index(&chars, '.'), Some(5));
    }

    #[test]
    fn repeated_marks_only_group_thousands() {
        let chars: Vec<char> = "1,234,567".chars().collect();
        assert_eq!(decimal_mark_index(&chars, ','), None);
    }

    #[test]
    fn ungroup_rejects_short_and_long_groups() {
        let chars = |text: &str| text.chars().collect::<Vec<char>>();
        assert_eq!(ungroup(&chars("1,234")), Some("1234".to_owned()));
        assert_eq!(ungroup(&chars("1234,567")), None);
        assert_eq!(ungroup(&chars("1,23")), None);
        assert_eq!(ungroup(&chars(",123")), None);
        assert_eq!(ungroup(&chars("1 234,567")), None);
    }

    #[test]
    fn longer_currency_marks_are_removed_first() {
        let marks = ["$".to_owned(), "US$".to_owned(), "USD".to_owned()];
        assert_eq!(strip_currency("US$ 12", &marks), "12");
        assert_eq!(strip_currency("12 USD", &marks), "12");
    }

    #[test]
    fn to_cents_counts_digits_after_removing_padding_zeros() {
        assert_eq!(to_cents("0009999999999", "990"), Ok(Cents::MAX_PARSED));
        assert_eq!(to_cents("99999999999", ""), Err(MoneyParseError::TooLarge));
        assert_eq!(to_cents("1", "005"), Err(MoneyParseError::TooManyDecimals));
        assert_eq!(to_cents("", "5"), Ok(Cents(50)));
    }
}
