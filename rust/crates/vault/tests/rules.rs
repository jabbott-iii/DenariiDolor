/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The PIN, question, and answer rules (CS-10, CS-14), with v1's cases from
//! `SecurityProfileServiceTest` and the edges of each limit.

use denarii_vault::{
    MAX_ANSWER_UNITS, MAX_QUESTION_UNITS, MIN_ANSWER_CHARS, SetupProblem, is_valid_pin,
    normalize_answer, validate_setup,
};

const PIN: &str = "123456";
const QUESTION: &str = "Name of my first school?";
const ANSWER: &str = "Lincoln Elementary";

fn check(question: &str, answer: &str) -> Result<(), SetupProblem> {
    validate_setup(PIN, PIN, question, answer)
}

#[test]
fn a_pin_is_six_to_twelve_ascii_digits() {
    for pin in ["123456", "000000", "123456789012"] {
        assert!(is_valid_pin(pin), "{pin}");
    }
    for pin in [
        "",
        "12345",
        "1234567890123",
        "12345a",
        "12 456",
        "123456\n",
        "\n123456",
        "-12345",
        "\u{661}\u{662}\u{663}\u{664}\u{665}\u{666}",
        "１２３４５６",
        "123456\u{0}",
    ] {
        assert!(!is_valid_pin(pin), "{pin:?}");
    }
}

#[test]
fn recovery_answers_must_be_hard_to_guess() {
    assert_eq!(
        check("   ", ANSWER),
        Err(SetupProblem::SecurityQuestionRequired)
    );
    assert_eq!(
        check(&"q".repeat(201), ANSWER),
        Err(SetupProblem::SecurityQuestionTooLong)
    );
    assert_eq!(
        check(QUESTION, "   "),
        Err(SetupProblem::SecurityAnswerRequired)
    );
    assert_eq!(
        check(QUESTION, "Rex"),
        Err(SetupProblem::SecurityAnswerTooShort)
    );
    assert_eq!(
        check(QUESTION, "  a   b  c "),
        Err(SetupProblem::SecurityAnswerTooShort)
    );
    assert_eq!(
        check(QUESTION, &"a".repeat(101)),
        Err(SetupProblem::SecurityAnswerTooLong)
    );
    assert_eq!(
        check("Is it Rex the dog?", "REX the   dog"),
        Err(SetupProblem::SecurityAnswerInQuestion)
    );
    assert_eq!(
        check(QUESTION, PIN),
        Err(SetupProblem::SecurityAnswerMatchesPin)
    );
    assert_eq!(check(QUESTION, "abcdef"), Ok(()));
    assert_eq!(check(QUESTION, &"🐶".repeat(6)), Ok(()));
}

#[test]
fn the_rules_are_checked_in_v1s_order() {
    assert_eq!(
        validate_setup("12345", "54321", "", ""),
        Err(SetupProblem::InvalidPinFormat)
    );
    assert_eq!(
        validate_setup(PIN, "654321", "", ""),
        Err(SetupProblem::PinMismatch)
    );
    assert_eq!(
        validate_setup(PIN, PIN, "", ""),
        Err(SetupProblem::SecurityQuestionRequired)
    );
    assert_eq!(
        validate_setup(PIN, PIN, &"q".repeat(201), ""),
        Err(SetupProblem::SecurityQuestionTooLong)
    );
    assert_eq!(
        validate_setup(PIN, PIN, QUESTION, ""),
        Err(SetupProblem::SecurityAnswerRequired)
    );
}

/// The question's limit counts UTF-16 code units after Kotlin's trim, as v1's `String.length` did.
#[test]
fn the_question_limit_counts_utf16_units_after_trimming() {
    let longest = "q".repeat(MAX_QUESTION_UNITS);
    assert_eq!(check(&format!(" \u{A0}{longest}\u{1F}\t"), ANSWER), Ok(()));
    let emoji_pairs = "💰".repeat(MAX_QUESTION_UNITS / 2);
    assert_eq!(check(&emoji_pairs, ANSWER), Ok(()));
    assert_eq!(
        check(&format!("{emoji_pairs}q"), ANSWER),
        Err(SetupProblem::SecurityQuestionTooLong)
    );
}

/// The answer's minimum counts code points, and its maximum UTF-16 code units, both after
/// normalization: v1's one inconsistency (`docs/specs/android-v1-formats.md`, section 3).
#[test]
fn the_answer_limits_count_code_points_and_utf16_units() {
    assert_eq!(
        check(QUESTION, &"a".repeat(MIN_ANSWER_CHARS - 1)),
        Err(SetupProblem::SecurityAnswerTooShort)
    );
    assert_eq!(check(QUESTION, &"a".repeat(MIN_ANSWER_CHARS)), Ok(()));
    assert_eq!(
        check(QUESTION, &"🐶".repeat(MIN_ANSWER_CHARS - 1)),
        Err(SetupProblem::SecurityAnswerTooShort)
    );
    assert_eq!(check(QUESTION, &"a".repeat(MAX_ANSWER_UNITS)), Ok(()));
    assert_eq!(check(QUESTION, &"🐶".repeat(MAX_ANSWER_UNITS / 2)), Ok(()));
    assert_eq!(
        check(QUESTION, &format!("{}a", "🐶".repeat(MAX_ANSWER_UNITS / 2))),
        Err(SetupProblem::SecurityAnswerTooLong)
    );
    // NFKC folds the ligature into two letters before the length is counted.
    assert_eq!(check(QUESTION, "\u{FB01}\u{FB01}ab"), Ok(()));
    assert_eq!(
        check(QUESTION, &"\u{FB01}".repeat(MAX_ANSWER_UNITS / 2 + 1)),
        Err(SetupProblem::SecurityAnswerTooLong)
    );
}

#[test]
fn an_answer_compared_with_the_pin_is_normalized() {
    assert_eq!(check(QUESTION, " ０００１２３ "), Ok(()));
    assert_eq!(
        validate_setup("000123", "000123", QUESTION, " ０００１２３ "),
        Err(SetupProblem::SecurityAnswerMatchesPin)
    );
}

#[test]
fn answers_ignore_case_spacing_and_unicode_form() {
    assert_eq!(
        *normalize_answer("ＬＩＮＣＯＬＮ Elementary"),
        "lincoln elementary"
    );
    assert_eq!(
        *normalize_answer("  lincoln   ELEMENTARY "),
        "lincoln elementary"
    );
}

/// NFKC folds ligatures, numerals, Arabic presentation forms, and halfwidth kana into the letters
/// people mean.
#[test]
fn answers_are_compared_in_compatibility_form() {
    let cases = [
        ("\u{FB01}rst \u{FB02}at \u{2116}\u{2075}", "first flat no5"),
        ("\u{2460}\u{2461}\u{2462} \u{216B}", "123 xii"),
        (
            "\u{FEFB} \u{FDF2}",
            "\u{644}\u{627} \u{627}\u{644}\u{644}\u{647}",
        ),
        (
            "\u{92E}\u{947}\u{930}\u{93E}   \u{92A}\u{939}\u{932}\u{93E}",
            "\u{92E}\u{947}\u{930}\u{93E} \u{92A}\u{939}\u{932}\u{93E}",
        ),
        (
            "\u{FF76}\u{FF9E}\u{FF6F}\u{FF7A}\u{FF73}",
            "\u{30AC}\u{30C3}\u{30B3}\u{30A6}",
        ),
        (
            "  Cafe\u{301}   \u{FF2D}\u{FC}ller ",
            "caf\u{E9} m\u{FC}ller",
        ),
    ];
    for (typed, normalized) in cases {
        assert_eq!(*normalize_answer(typed), normalized, "{typed}");
    }
}

/// Kotlin's `trim()` on Android and Android's `\s` disagree about six characters, and
/// normalization keeps v1's quirks: U+001C to U+001F and U+180E are trimmed but not collapsed, and
/// U+0085 is collapsed but not trimmed. NFKC turns most other spaces into U+0020 first.
#[test]
fn whitespace_follows_kotlin_to_trim_and_white_space_to_collapse() {
    assert_eq!(*normalize_answer("\u{1F}rex\u{1C}"), "rex");
    assert_eq!(*normalize_answer("rex\u{1E}dog"), "rex\u{1E}dog");
    assert_eq!(*normalize_answer("\u{180E}rex\u{180E}"), "rex");
    assert_eq!(*normalize_answer("rex\u{180E}dog"), "rex\u{180E}dog");
    assert_eq!(*normalize_answer("\u{85}rex\u{85}"), " rex ");
    assert_eq!(*normalize_answer("rex\u{85}\u{2028}\t\u{B}dog"), "rex dog");
    assert_eq!(
        *normalize_answer("rex\u{A0}\u{3000}\u{2029}\u{1680}dog"),
        "rex dog"
    );
    assert_eq!(*normalize_answer("rex\u{200B}dog"), "rex\u{200B}dog");
}

/// Lowercase is Android's: Unicode's mapping in every locale, with a final sigma after a cased
/// letter. Fixture F7 (`tests/f7.rs`) checks every case against phones.
#[test]
fn lowercase_is_androids() {
    assert_eq!(*normalize_answer("İSTANBUL"), "i\u{307}stanbul");
    assert_eq!(*normalize_answer("ISPARTA"), "isparta");
    assert_eq!(
        *normalize_answer(
            "\u{39F}\u{394}\u{39F}\u{3A3} \u{3A3}\u{39F}\u{3A6}\u{399}\u{391}\u{3A3}"
        ),
        "\u{3BF}\u{3B4}\u{3BF}\u{3C2} \u{3C3}\u{3BF}\u{3C6}\u{3B9}\u{3B1}\u{3C2}"
    );
}

#[test]
fn empty_and_blank_answers_normalize_to_nothing() {
    for answer in ["", " ", "\t\n\u{3000}", "\u{1C}\u{1D}"] {
        assert_eq!(*normalize_answer(answer), "", "{answer:?}");
    }
}

#[test]
fn each_problem_has_v1s_code() {
    let codes = [
        (SetupProblem::InvalidPinFormat, "INVALID_PIN_FORMAT"),
        (SetupProblem::PinMismatch, "PIN_MISMATCH"),
        (
            SetupProblem::SecurityQuestionRequired,
            "SECURITY_QUESTION_REQUIRED",
        ),
        (
            SetupProblem::SecurityQuestionTooLong,
            "SECURITY_QUESTION_TOO_LONG",
        ),
        (
            SetupProblem::SecurityAnswerRequired,
            "SECURITY_ANSWER_REQUIRED",
        ),
        (
            SetupProblem::SecurityAnswerTooShort,
            "SECURITY_ANSWER_TOO_SHORT",
        ),
        (
            SetupProblem::SecurityAnswerTooLong,
            "SECURITY_ANSWER_TOO_LONG",
        ),
        (
            SetupProblem::SecurityAnswerInQuestion,
            "SECURITY_ANSWER_IN_QUESTION",
        ),
        (
            SetupProblem::SecurityAnswerMatchesPin,
            "SECURITY_ANSWER_MATCHES_PIN",
        ),
    ];
    for (problem, code) in codes {
        assert_eq!(problem.code(), code);
        assert_eq!(
            problem.to_string(),
            format!("the setup input breaks a rule ({code})")
        );
    }
}
