/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The bridge's money call: it gives `denarii_core::parse_cents`'s result for well-formed symbols,
//! names each rejection, and checks the decimal separator that Dart sends before it reads the text.

use denarii_bridge::api::money::{AmountError, NumberSymbols, parse_cents};

fn symbols(decimal_separator: &str, marks: &[&str]) -> NumberSymbols {
    NumberSymbols {
        decimal_separator: decimal_separator.to_owned(),
        currency_marks: marks.iter().map(|&mark| mark.to_owned()).collect(),
    }
}

/// US English with US dollars, v1's defaults.
fn us(text: &str) -> Result<i64, AmountError> {
    parse_cents(text.to_owned(), symbols(".", &["$", "$", "USD"]))
}

#[test]
fn returns_the_cents_core_parses() {
    assert_eq!(us("12"), Ok(1_200));
    assert_eq!(us("$1,234.56"), Ok(123_456));
    assert_eq!(us("-$5"), Ok(-500));
    assert_eq!(us("9999999999.99"), Ok(999_999_999_999));
}

#[test]
fn uses_the_locale_symbols_from_dart() {
    let german = symbols(",", &["€", "€", "EUR"]);
    assert_eq!(parse_cents("1.234,56 €".to_owned(), german), Ok(123_456));

    let arabic = symbols("٫", &["SAR", "SAR", "SAR"]);
    assert_eq!(parse_cents("١٢٫٥".to_owned(), arabic), Ok(1_250));

    let no_currency = symbols(".", &[]);
    assert_eq!(parse_cents("12.50".to_owned(), no_currency), Ok(1_250));
}

#[test]
fn names_each_rejection_as_core_does() {
    assert_eq!(us(""), Err(AmountError::Empty));
    assert_eq!(us("$"), Err(AmountError::Empty));
    assert_eq!(us("abc"), Err(AmountError::Malformed));
    assert_eq!(us("1,2,3"), Err(AmountError::Malformed));
    assert_eq!(us("1.005"), Err(AmountError::TooManyDecimals));
    assert_eq!(us("99999999999"), Err(AmountError::TooLarge));
    assert_eq!(us("10000000000"), Err(AmountError::TooLarge));
}

#[test]
fn rejects_a_decimal_separator_that_isnt_one_character() {
    for separator in ["", "..", "٫٫", ". "] {
        assert_eq!(
            parse_cents("12.50".to_owned(), symbols(separator, &[])),
            Err(AmountError::InvalidDecimalSeparator),
            "separator {separator:?}",
        );
    }
}

#[test]
fn checks_the_separator_before_the_text() {
    assert_eq!(
        parse_cents(String::new(), symbols("", &[])),
        Err(AmountError::InvalidDecimalSeparator),
    );
}

#[test]
fn accepts_any_single_character_as_the_separator() {
    // One Unicode scalar value, even outside the Basic Multilingual Plane, is one separator; core
    // then decides whether the text uses it.
    assert_eq!(
        parse_cents("12.50".to_owned(), symbols("𝟎", &[])),
        Ok(1_250)
    );
}

#[test]
fn reads_long_text_without_panicking() {
    let long = "9".repeat(100_000);
    assert_eq!(us(&long), Err(AmountError::TooLarge));
    let noise = "$,.-٫ ".repeat(10_000);
    assert!(us(&noise).is_err());
}
