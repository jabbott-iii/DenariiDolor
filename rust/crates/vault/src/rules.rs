/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The rules for a PIN, a security question, and its answer (CS-10, CS-14), as v1's
//! `SecurityProfileService` checked them (`docs/specs/android-v1-formats.md`, section 3).
//!
//! Text is measured as v1's Kotlin measured it: whitespace is Kotlin's, the answer's minimum
//! counts code points, and the two maximums count UTF-16 code units.

use std::fmt;

use denarii_core::is_whitespace;
use unicode_normalization::UnicodeNormalization;
use zeroize::Zeroizing;

/// The fewest digits a PIN has.
pub const MIN_PIN_DIGITS: usize = 6;
/// The most digits a PIN has.
pub const MAX_PIN_DIGITS: usize = 12;
/// The fewest characters (code points) a normalized answer has.
pub const MIN_ANSWER_CHARS: usize = 6;
/// The most UTF-16 code units a normalized answer has.
pub const MAX_ANSWER_UNITS: usize = 100;
/// The most UTF-16 code units a trimmed question has.
pub const MAX_QUESTION_UNITS: usize = 200;

/// Whether `pin` is 6 to 12 ASCII digits, v1's `^[0-9]{6,12}$` (CS-10), which setup, recovery,
/// and sign-in all check. Other decimal digits, such as `٣`, aren't PIN digits.
#[must_use]
pub fn is_valid_pin(pin: &str) -> bool {
    (MIN_PIN_DIGITS..=MAX_PIN_DIGITS).contains(&pin.len())
        && pin.bytes().all(|byte| byte.is_ascii_digit())
}

/// `answer` in the form v1 compared and stored answers in, so that it opens however it is typed
/// (CS-14). The steps are v1's `normalizeAnswer`, in order:
///
/// 1. Unicode Normalization Form KC (NFKC), which folds fullwidth letters, ligatures, and
///    compatibility forms into the characters people mean.
/// 2. Kotlin's `trim()`, with Kotlin's whitespace on Android ([`denarii_core::is_whitespace`]).
/// 3. Each run of whitespace, by Unicode's `White_Space` property, becomes one space. v1 ran the
///    regular expression `\s+` on Android, whose `\s` is `White_Space`.
/// 4. Lowercase, as Android's `String.toLowerCase(Locale.ROOT)` does: Unicode's mapping in every
///    locale, but with a simpler rule for the Greek final sigma (`ς`) in most strings.
///
/// Fixture F7 checks every step against what v1 made of answers on API 26 and API 36.
///
/// The result is secret, so it is wiped when dropped, as is each step's text. The wipe is best
/// effort: a string that grows while it is built leaves its old buffer behind.
///
/// # Examples
///
/// ```
/// use denarii_vault::normalize_answer;
///
/// assert_eq!(*normalize_answer("  Cafe\u{301}   ＭÜLLER "), "café müller");
/// ```
#[must_use]
pub fn normalize_answer(answer: &str) -> Zeroizing<String> {
    let mut compatible = Zeroizing::new(String::with_capacity(
        answer.nfkc().map(char::len_utf8).sum(),
    ));
    compatible.extend(answer.nfkc());
    let trimmed = compatible.trim_matches(is_whitespace);
    let mut spaced = Zeroizing::new(String::with_capacity(trimmed.len()));
    let mut in_run = false;
    for c in trimmed.chars() {
        if c.is_whitespace() {
            if !in_run {
                spaced.push(' ');
            }
            in_run = true;
        } else {
            spaced.push(c);
            in_run = false;
        }
    }
    lowercase_as_android(&spaced)
}

/// `text` lowercased as Android's `String.toLowerCase(Locale.ROOT)` lowercases it, which v1's
/// `lowercase()` called (libcore's `CaseMapper`).
///
/// A string with an `İ` (U+0130) or a character outside the Basic Multilingual Plane goes to ICU,
/// whose mapping is Unicode's, as [`str::to_lowercase`]'s is. Android lowercases any other string
/// one character at a time, and makes a `Σ` final only when the character just before it is a
/// cased letter and the one just after isn't. Unicode's rule skips apostrophes, soft hyphens,
/// combining marks, and other case-ignorable characters between them, so `Α'Σ` ends in `ς` there
/// and in `σ` on Android.
fn lowercase_as_android(text: &str) -> Zeroizing<String> {
    if text
        .chars()
        .any(|c| c == '\u{130}' || u32::from(c) > 0xFFFF)
    {
        return Zeroizing::new(text.to_lowercase());
    }
    // Lowercasing a BMP character other than U+0130 at most turns two UTF-8 bytes into three.
    let mut lower = Zeroizing::new(String::with_capacity(
        text.len().saturating_add(text.len() / 2),
    ));
    let mut previous = None;
    let mut rest = text.chars().peekable();
    while let Some(c) = rest.next() {
        if c == 'Σ' {
            let is_final = previous.is_some_and(is_cased_on_android)
                && !rest.peek().copied().is_some_and(is_cased_on_android);
            lower.push(if is_final { 'ς' } else { 'σ' });
        } else {
            lower.extend(c.to_lowercase());
        }
        previous = Some(c);
    }
    lower
}

/// Whether Android's `Character.isLowerCase`, `isUpperCase`, or `isTitleCase` is true for `c`,
/// for a character of text that NFKC has normalized. Android asks ICU for the general categories
/// Ll, Lu, and Lt, where Rust's [`char::is_lowercase`] and [`char::is_uppercase`] also count
/// Unicode's `Other_Lowercase` and `Other_Uppercase`. NFKC leaves six such characters in the Basic
/// Multilingual Plane, and 27 Greek titlecase letters, which this lists.
fn is_cased_on_android(c: char) -> bool {
    match c {
        '\u{2C0}' | '\u{2C1}' | '\u{345}' | '\u{1D2F}' | '\u{1D3B}' | '\u{1D4E}' => false,
        '\u{1F88}'..='\u{1F8F}'
        | '\u{1F98}'..='\u{1F9F}'
        | '\u{1FA8}'..='\u{1FAF}'
        | '\u{1FBC}'
        | '\u{1FCC}'
        | '\u{1FFC}' => true,
        _ => c.is_lowercase() || c.is_uppercase(),
    }
}

#[cfg(test)]
mod tests {
    use super::{is_cased_on_android, lowercase_as_android};

    #[test]
    fn sigma_is_final_only_after_a_cased_letter_and_before_none() {
        for (text, lower) in [
            ("Σ", "σ"),
            ("ΑΣ", "ας"),
            ("ΑΣ.", "ας."),
            ("ΑΣΑ", "ασα"),
            ("ΑΣ1", "ας1"),
            ("1Σ", "1σ"),
            ("Α'Σ", "α'σ"),
            ("Α\u{AD}Σ", "α\u{AD}σ"),
            ("ΑΣ\u{345}", "ας\u{345}"),
            ("\u{345}Σ", "\u{345}σ"),
            ("\u{1F88}Σ", "\u{1F80}ς"),
            ("ΑΣ\u{1F88}", "ασ\u{1F80}"),
        ] {
            assert_eq!(*lowercase_as_android(text), lower, "{text}");
        }
    }

    /// With an `İ` or a character outside the BMP, the whole string takes Unicode's rule.
    #[test]
    fn an_i_with_a_dot_or_a_supplementary_character_takes_unicodes_rule() {
        assert_eq!(*lowercase_as_android("Α'Σ \u{10400}"), "α'ς \u{10428}");
        assert_eq!(*lowercase_as_android("Α'Σ \u{130}"), "α'ς i\u{307}");
        assert_eq!(*lowercase_as_android("İSTANBUL"), "i\u{307}stanbul");
    }

    #[test]
    fn cased_means_androids_categories() {
        for c in ['a', 'A', 'σ', 'ß', '\u{1F88}', '\u{1FFC}'] {
            assert!(is_cased_on_android(c), "{c:?}");
        }
        for c in [
            '1', '\'', '\u{AD}', '\u{2C0}', '\u{2C1}', '\u{345}', '\u{1D2F}', '\u{1D3B}',
            '\u{1D4E}',
        ] {
            assert!(!is_cased_on_android(c), "{c:?}");
        }
    }
}

/// Checks a new profile's PIN, question, and answer against v1's setup rules, in v1's order.
///
/// # Errors
///
/// The first [`SetupProblem`] that the input has, in the order its variants are listed.
///
/// # Examples
///
/// ```
/// use denarii_vault::{SetupProblem, validate_setup};
///
/// assert_eq!(validate_setup("135790", "135790", "First pet?", "Rex the dog"), Ok(()));
/// assert_eq!(
///     validate_setup("135790", "135790", "First pet?", "Rex"),
///     Err(SetupProblem::SecurityAnswerTooShort)
/// );
/// ```
pub fn validate_setup(
    pin: &str,
    confirmation: &str,
    question: &str,
    answer: &str,
) -> Result<(), SetupProblem> {
    let question = question.trim_matches(is_whitespace);
    let answer = normalize_answer(answer);
    if !is_valid_pin(pin) {
        Err(SetupProblem::InvalidPinFormat)
    } else if pin != confirmation {
        Err(SetupProblem::PinMismatch)
    } else if question.is_empty() {
        Err(SetupProblem::SecurityQuestionRequired)
    } else if question.encode_utf16().count() > MAX_QUESTION_UNITS {
        Err(SetupProblem::SecurityQuestionTooLong)
    } else if answer.is_empty() {
        Err(SetupProblem::SecurityAnswerRequired)
    } else if answer.chars().count() < MIN_ANSWER_CHARS {
        Err(SetupProblem::SecurityAnswerTooShort)
    } else if answer.encode_utf16().count() > MAX_ANSWER_UNITS {
        Err(SetupProblem::SecurityAnswerTooLong)
    } else if normalize_answer(question).contains(answer.as_str()) {
        Err(SetupProblem::SecurityAnswerInQuestion)
    } else if *answer == pin {
        Err(SetupProblem::SecurityAnswerMatchesPin)
    } else {
        Ok(())
    }
}

/// A setup rule that a new profile's input breaks. Each variant's [`SetupProblem::code`] is v1's
/// name for it, which the screens map to a localized message.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[non_exhaustive]
pub enum SetupProblem {
    /// The PIN isn't 6 to 12 ASCII digits.
    InvalidPinFormat,
    /// The confirmation isn't the same as the PIN.
    PinMismatch,
    /// The question is empty once trimmed.
    SecurityQuestionRequired,
    /// The trimmed question is longer than [`MAX_QUESTION_UNITS`].
    SecurityQuestionTooLong,
    /// The answer is empty once normalized.
    SecurityAnswerRequired,
    /// The normalized answer is shorter than [`MIN_ANSWER_CHARS`].
    SecurityAnswerTooShort,
    /// The normalized answer is longer than [`MAX_ANSWER_UNITS`].
    SecurityAnswerTooLong,
    /// The normalized question contains the normalized answer.
    SecurityAnswerInQuestion,
    /// The normalized answer is the PIN.
    SecurityAnswerMatchesPin,
}

impl SetupProblem {
    /// The stable name of the problem, v1's `SetupProfileResult` name, such as
    /// `"INVALID_PIN_FORMAT"`.
    #[must_use]
    pub const fn code(&self) -> &'static str {
        match self {
            Self::InvalidPinFormat => "INVALID_PIN_FORMAT",
            Self::PinMismatch => "PIN_MISMATCH",
            Self::SecurityQuestionRequired => "SECURITY_QUESTION_REQUIRED",
            Self::SecurityQuestionTooLong => "SECURITY_QUESTION_TOO_LONG",
            Self::SecurityAnswerRequired => "SECURITY_ANSWER_REQUIRED",
            Self::SecurityAnswerTooShort => "SECURITY_ANSWER_TOO_SHORT",
            Self::SecurityAnswerTooLong => "SECURITY_ANSWER_TOO_LONG",
            Self::SecurityAnswerInQuestion => "SECURITY_ANSWER_IN_QUESTION",
            Self::SecurityAnswerMatchesPin => "SECURITY_ANSWER_MATCHES_PIN",
        }
    }
}

impl fmt::Display for SetupProblem {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "the setup input breaks a rule ({})", self.code())
    }
}

impl std::error::Error for SetupProblem {}
