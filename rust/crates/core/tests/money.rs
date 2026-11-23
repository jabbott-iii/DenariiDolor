/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Money parsing and plain text. Every case of v1's `MoneyTest.kt` (at commit 6d57526) is here
//! with v1's expected result; the locale symbols are what `java.text` gave v1 for each locale.

use denarii_core::{Cents, MoneyParseError, NumberSymbols, normalize_digits, parse_cents};

fn symbols(decimal_separator: char, marks: &[&str]) -> NumberSymbols {
    NumberSymbols {
        decimal_separator,
        currency_marks: marks.iter().map(|&mark| mark.to_owned()).collect(),
    }
}

/// US English with US dollars, v1's defaults.
fn us(text: &str) -> Result<Cents, MoneyParseError> {
    parse_cents(text, &symbols('.', &["$", "$", "USD"]))
}

/// German with US dollars.
fn germany(text: &str) -> Result<Cents, MoneyParseError> {
    parse_cents(text, &symbols(',', &["$", "$", "USD"]))
}

#[test]
fn parses_common_inputs_to_cents() {
    assert_eq!(us("12"), Ok(Cents(1_200)));
    assert_eq!(us("12.5"), Ok(Cents(1_250)));
    assert_eq!(us(" 12.05 "), Ok(Cents(1_205)));
    assert_eq!(us("$1,234.56"), Ok(Cents(123_456)));
    assert_eq!(us("-25.50"), Ok(Cents(-2_550)));
    assert_eq!(us("-$5"), Ok(Cents(-500)));
    assert_eq!(us("$-5"), Ok(Cents(-500)));
    assert_eq!(us("12.000"), Ok(Cents(1_200)));
    assert_eq!(us(".5"), Ok(Cents(50)));
    assert_eq!(us("9999999999.99"), Ok(Cents(999_999_999_999)));
}

#[test]
fn accepts_comma_as_decimal_mark_in_any_locale() {
    assert_eq!(us("12,50"), Ok(Cents(1_250)));
    assert_eq!(us("12,5"), Ok(Cents(1_250)));
    assert_eq!(germany("12,50"), Ok(Cents(1_250)));
    assert_eq!(germany("12.50"), Ok(Cents(1_250)));
    assert_eq!(us("1.234,56"), Ok(Cents(123_456)));
    assert_eq!(germany("1.234,56"), Ok(Cents(123_456)));
}

#[test]
fn locale_settles_one_mark_followed_by_three_digits() {
    assert_eq!(us("1,234"), Ok(Cents(123_400)));
    assert_eq!(us("1.234"), Err(MoneyParseError::TooManyDecimals));
    assert_eq!(germany("1.234"), Ok(Cents(123_400)));
    assert_eq!(germany("1,234"), Err(MoneyParseError::TooManyDecimals));
    assert_eq!(germany("12.000"), Ok(Cents(1_200_000)));
}

#[test]
fn accepts_well_formed_thousands_grouping() {
    let french = symbols(',', &["$US", "$", "USD"]);
    let swiss = symbols('.', &["$", "$", "USD"]);
    assert_eq!(us("1,234,567.89"), Ok(Cents(123_456_789)));
    assert_eq!(germany("1.234.567"), Ok(Cents(123_456_700)));
    for space in [' ', '\u{A0}', '\u{202F}'] {
        assert_eq!(
            parse_cents(&format!("1{space}234,56"), &french),
            Ok(Cents(123_456))
        );
        assert_eq!(
            parse_cents(&format!("1{space}234,5"), &french),
            Ok(Cents(123_450))
        );
    }
    assert_eq!(parse_cents("1'234.50", &swiss), Ok(Cents(123_450)));
    assert_eq!(parse_cents("1\u{2019}234.50", &swiss), Ok(Cents(123_450)));
}

#[test]
fn rejects_invalid_inputs() {
    assert_eq!(us(""), Err(MoneyParseError::Empty));
    assert_eq!(us("$"), Err(MoneyParseError::Empty));
    assert_eq!(us("-"), Err(MoneyParseError::Empty));
    assert_eq!(us("abc"), Err(MoneyParseError::Malformed));
    assert_eq!(us("1.005"), Err(MoneyParseError::TooManyDecimals));
    assert_eq!(us("1e3"), Err(MoneyParseError::Malformed));
    assert_eq!(us("99999999999"), Err(MoneyParseError::TooLarge));
    assert_eq!(us("1,2,3"), Err(MoneyParseError::Malformed));
    assert_eq!(us("12,345,67"), Err(MoneyParseError::Malformed));
    assert_eq!(us("12 34"), Err(MoneyParseError::Malformed));
    assert_eq!(us("1..2"), Err(MoneyParseError::Malformed));
    assert_eq!(us("5."), Err(MoneyParseError::Malformed));
    assert_eq!(us(","), Err(MoneyParseError::Malformed));
    assert_eq!(us("--5"), Err(MoneyParseError::Malformed));
    assert_eq!(us("+5"), Err(MoneyParseError::Malformed));
    assert_eq!(us("1,234.5.6"), Err(MoneyParseError::Malformed));
    assert_eq!(us("1 234,567.89"), Err(MoneyParseError::Malformed));
}

#[test]
fn accepts_the_largest_amount_and_rejects_one_cent_more() {
    assert_eq!(us("9,999,999,999.99"), Ok(Cents::MAX_PARSED));
    assert_eq!(us("-9999999999.99"), Ok(Cents(-999_999_999_999)));
    assert_eq!(us("0009999999999.990"), Ok(Cents::MAX_PARSED));
    assert_eq!(us("10000000000"), Err(MoneyParseError::TooLarge));
    assert_eq!(us("10000000000.00"), Err(MoneyParseError::TooLarge));
}

#[test]
fn writes_plain_and_input_text() {
    assert_eq!(Cents(1_250).to_plain(), "12.50");
    assert_eq!(Cents(5).to_plain(), "0.05");
    assert_eq!(Cents(-5).to_plain(), "-0.05");
    assert_eq!(Cents(-123_456).to_plain(), "-1234.56");
    assert_eq!(Cents(i64::MIN).to_plain(), "-92233720368547758.08");
    assert_eq!(Cents(450).to_input(), "4.5");
    assert_eq!(Cents(25_000).to_input(), "250");
    assert_eq!(Cents(1_000).to_input(), "10");
    assert_eq!(Cents(0).to_input(), "0");
}

#[test]
fn plain_text_parses_back_to_the_same_cents() {
    for value in [1, 99, 100, 1_250, 123_456, 999_999_999_999] {
        assert_eq!(us(&Cents(value).to_plain()), Ok(Cents(value)));
        assert_eq!(us(&Cents(value).to_input()), Ok(Cents(value)));
    }
}

#[test]
fn accepts_the_currency_symbol_or_code_on_either_side() {
    let french_euro = symbols(',', &["€", "€", "EUR"]);
    let english_euro = symbols('.', &["€", "€", "EUR"]);
    let brazil = symbols(',', &["R$", "R$", "BRL"]);
    let india = symbols('.', &["₹", "₹", "INR"]);
    assert_eq!(parse_cents("12,50 €", &french_euro), Ok(Cents(1_250)));
    assert_eq!(parse_cents("€12.50", &english_euro), Ok(Cents(1_250)));
    assert_eq!(parse_cents("12.50 EUR", &english_euro), Ok(Cents(1_250)));
    assert_eq!(parse_cents("R$ 1.234,56", &brazil), Ok(Cents(123_456)));
    assert_eq!(parse_cents("-₹5", &india), Ok(Cents(-500)));
    assert_eq!(parse_cents("€", &english_euro), Err(MoneyParseError::Empty));
}

#[test]
fn strips_any_currency_sign_left_at_either_end() {
    assert_eq!(us("£12"), Ok(Cents(1_200)));
    assert_eq!(us("12 ¥"), Ok(Cents(1_200)));
}

#[test]
fn accepts_arabic_indic_and_devanagari_digits() {
    let saudi_dollars = symbols('\u{066B}', &["US$", "$", "USD"]);
    let saudi_riyals = symbols('\u{066B}', &["ر.س.\u{200F}", "SAR", "SAR"]);
    let india_dollars = symbols('.', &["$", "$", "USD"]);
    assert_eq!(normalize_digits("١٢٫٥٠"), "12.50");
    assert_eq!(normalize_digits("\u{200F}\u{2212}5"), "-5");
    assert_eq!(parse_cents("١٢٫٥", &saudi_dollars), Ok(Cents(1_250)));
    assert_eq!(parse_cents("١٬٢٣٤٫٥٦", &saudi_dollars), Ok(Cents(123_456)));
    assert_eq!(parse_cents("۱۲.۵", &saudi_dollars), Ok(Cents(1_250)));
    assert_eq!(parse_cents("१२.५०", &india_dollars), Ok(Cents(1_250)));
    // What v1 displayed in Arabic parses back, direction marks and all (v1's MoneySnapshotTest).
    assert_eq!(
        parse_cents("١٢٫٥٠\u{A0}ر.س.\u{200F}", &saudi_riyals),
        Ok(Cents(1_250))
    );
    assert_eq!(
        parse_cents("\u{061C}-١٬٢٣٤٬٥٦٧٫٨٩\u{A0}ر.س.\u{200F}", &saudi_riyals),
        Ok(Cents(-123_456_789))
    );
}

#[test]
fn ignores_digits_outside_the_basic_multilingual_plane_as_v1_did() {
    // MATHEMATICAL BOLD DIGIT ONE and TWO: Kotlin saw two UTF-16 surrogates each, not digits.
    assert_eq!(us("\u{1D7CF}\u{1D7D0}"), Err(MoneyParseError::Malformed));
}

#[test]
fn cents_sum_exactly() {
    let ten_cents = us("0.10").unwrap();
    let thirty = ten_cents
        .checked_add(ten_cents)
        .and_then(|sum| sum.checked_add(ten_cents));
    assert_eq!(thirty.map_err(|_| MoneyParseError::TooLarge), us("0.30"));
}

#[test]
fn checked_arithmetic_reports_overflow() {
    assert!(Cents(i64::MAX).checked_add(Cents(1)).is_err());
    assert!(Cents(i64::MIN).checked_sub(Cents(1)).is_err());
    assert!(Cents(i64::MIN).checked_neg().is_err());
    assert_eq!(Cents(-5).checked_neg(), Ok(Cents(5)));
}
