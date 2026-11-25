/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Text measures that match how v1's Kotlin code read text, so the port accepts and rejects the
//! same input.
//!
//! Three things differ from Rust's defaults:
//!
//! - **Whitespace.** Kotlin's `trim()` and `isBlank()` on Android treat the characters Rust's
//!   [`char::is_whitespace`] does as whitespace, except U+0085 (next line), plus U+001C to U+001F
//!   (the information separators) and U+180E (the Mongolian vowel separator). [`is_whitespace`]
//!   is that set.
//! - **Length.** Kotlin's `String.length` counts UTF-16 code units, so an emoji outside the Basic
//!   Multilingual Plane counts as two. [`utf16_len`] counts the same way.
//! - **Digits and currency signs.** Kotlin looked at one UTF-16 code unit at a time, so it
//!   recognized decimal digits and currency signs only up to U+FFFF. The tables in
//!   `unicode_tables.rs` cover that range.

mod unicode_tables;

use unicode_tables::{CURRENCY_SYMBOLS, DECIMAL_ZEROS};

/// Whether Kotlin's `Char.isWhitespace()` is true for `c` on Android.
pub(crate) fn is_whitespace(c: char) -> bool {
    // Kotlin/JVM's rule is Java's `Character.isWhitespace(c) || Character.isSpaceChar(c)`. Android's
    // `Character.isWhitespace` still counts U+180E, which Unicode 6.3 stopped calling a space; v1
    // trimmed it on API 26 and API 36 (fixture F7, in rust/crates/vault/tests/fixtures).
    matches!(c, '\u{1C}'..='\u{1F}' | '\u{180E}') || (c.is_whitespace() && c != '\u{85}')
}

/// `text` without leading and trailing whitespace, as Kotlin's `trim()` removes it.
pub(crate) fn trim(text: &str) -> &str {
    text.trim_matches(is_whitespace)
}

/// Whether `text` is empty or holds only whitespace, as Kotlin's `isBlank()` decides.
pub(crate) fn is_blank(text: &str) -> bool {
    text.chars().all(is_whitespace)
}

/// The length of `text` in UTF-16 code units, which is what Kotlin's `String.length` returns.
pub(crate) fn utf16_len(text: &str) -> usize {
    text.encode_utf16().count()
}

/// The value of `c` if it is a decimal digit in the Basic Multilingual Plane, such as `٣` (3).
pub(crate) fn decimal_digit_value(c: char) -> Option<u32> {
    let code = u32::from(c);
    let run = DECIMAL_ZEROS.partition_point(|&zero| zero <= code);
    let zero = DECIMAL_ZEROS.get(run.checked_sub(1)?)?;
    let value = code.checked_sub(*zero)?;
    (value < 10).then_some(value)
}

/// Whether `c` is a currency symbol in the Basic Multilingual Plane, such as `$`, `€`, or `₹`.
pub(crate) fn is_currency_symbol(c: char) -> bool {
    let code = u32::from(c);
    let range = CURRENCY_SYMBOLS.partition_point(|&(start, _)| start <= code);
    range
        .checked_sub(1)
        .and_then(|index| CURRENCY_SYMBOLS.get(index))
        .is_some_and(|&(_, end)| code <= end)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn whitespace_matches_kotlin_not_rust() {
        assert!(is_whitespace(' '));
        assert!(is_whitespace('\u{A0}'));
        assert!(is_whitespace('\u{202F}'));
        assert!(is_whitespace('\u{1C}'));
        assert!(is_whitespace('\u{180E}'));
        assert!(!is_whitespace('\u{85}'));
        assert!(!is_whitespace('\u{200B}'));
    }

    #[test]
    fn trim_and_blank_use_kotlin_whitespace() {
        assert_eq!(trim("\u{1F} Rent \u{A0}"), "Rent");
        assert_eq!(trim("\u{85}Rent"), "\u{85}Rent");
        assert!(is_blank(""));
        assert!(is_blank(" \t\u{3000}"));
        assert!(!is_blank("\u{85}"));
    }

    #[test]
    fn length_counts_utf16_code_units() {
        assert_eq!(utf16_len("Rent"), 4);
        assert_eq!(utf16_len("é"), 1);
        assert_eq!(utf16_len("💰"), 2);
    }

    #[test]
    fn decimal_digits_in_the_basic_multilingual_plane_have_values() {
        assert_eq!(decimal_digit_value('0'), Some(0));
        assert_eq!(decimal_digit_value('٣'), Some(3));
        assert_eq!(decimal_digit_value('۹'), Some(9));
        assert_eq!(decimal_digit_value('५'), Some(5));
        assert_eq!(decimal_digit_value('７'), Some(7));
        assert_eq!(decimal_digit_value('a'), None);
        assert_eq!(decimal_digit_value('\u{0669}'), Some(9));
        assert_eq!(decimal_digit_value('\u{066A}'), None);
        assert_eq!(decimal_digit_value('\u{1D7CE}'), None);
    }

    #[test]
    fn currency_symbols_are_recognized() {
        for sign in ['$', '€', '£', '¥', '₹', '₺', '﷼', '＄'] {
            assert!(is_currency_symbol(sign), "{sign}");
        }
        for other in ['a', '-', '.', '\u{20C1}', '\u{11FDD}'] {
            assert!(!is_currency_symbol(other), "{other}");
        }
    }
}
