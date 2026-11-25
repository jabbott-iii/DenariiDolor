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
/// 2. Kotlin's `trim()`, with Kotlin's whitespace ([`denarii_core::is_whitespace`]).
/// 3. Each run of whitespace, by Unicode's `White_Space` property, becomes one space. v1 ran the
///    regular expression `\s+` on Android, whose `\s` is `White_Space` (M2.7 found that on API 26
///    and API 36).
/// 4. Lowercase, by Unicode's rules in every locale.
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
    Zeroizing::new(spaced.to_lowercase())
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
