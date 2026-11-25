/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The search screen's input, turned into the filters a store search takes.
//!
//! This ports v1's `SearchFilterParser` (`app/.../presentation/ui/search/SearchFilterParser.kt`
//! at `6d57526`). v1 parsed every field from text; here the category comes from a picker as an
//! ID, and the dates come from a date picker as moments that Dart computes in the user's time
//! zone (ADR-0019), so only the description and the amounts are text.

use denarii_core::{
    CategoryId, Cents, DomainError, EpochMillis, NumberSymbols, SearchFilters, is_blank,
    is_valid_search_range, is_whitespace, parse_cents,
};

use crate::error::ReportError;

/// What the user entered on the search screen. A field left empty, or `None`, filters nothing.
#[derive(Debug, Clone, PartialEq, Eq, Hash, Default)]
pub struct SearchInput {
    /// Text the description must contain.
    pub description: String,
    /// The category picked, or `None` for every category.
    pub category_id: Option<CategoryId>,
    /// The smallest amount, as typed.
    pub min_amount: String,
    /// The largest amount, as typed.
    pub max_amount: String,
    /// The first moment of the start date, in the user's time zone.
    pub start_date: Option<EpochMillis>,
    /// The first moment of the day after the end date, in the user's time zone. The search runs
    /// to one millisecond before it, as v1's did.
    pub day_after_end_date: Option<EpochMillis>,
}

/// The filters `input` asks for, as v1's search screen parsed them.
///
/// The description is trimmed with Kotlin's whitespace, and left out if nothing is left. An
/// amount left blank is left out; any other is read with [`parse_cents`] and `symbols`, the
/// user's locale's. The end date becomes the moment before `day_after_end_date`.
///
/// # Errors
///
/// The first of these that applies, in v1's order:
///
/// 1. [`ReportError::InvalidMinAmount`], then [`ReportError::InvalidMaxAmount`], with why
///    [`parse_cents`] refused the amount.
/// 2. [`ReportError::Overflow`] if `day_after_end_date` is the smallest moment there is, which
///    has no moment before it.
/// 3. [`ReportError::Domain`] with [`DomainError::InvalidSearchRange`] if the start date is after
///    the end date, or the minimum amount above the maximum.
///
/// # Examples
///
/// ```
/// use denarii_core::{Cents, EpochMillis, NumberSymbols};
/// use denarii_report::{SearchInput, parse_search_input};
///
/// # fn main() -> Result<(), Box<dyn std::error::Error>> {
/// let english = NumberSymbols {
///     decimal_separator: '.',
///     currency_marks: vec!["$".to_owned(), "$".to_owned(), "USD".to_owned()],
/// };
/// let input = SearchInput {
///     description: " Groceries ".to_owned(),
///     min_amount: "10.5".to_owned(),
///     // September 30, 2024 in UTC: its day after starts at 2024-10-01T00:00:00Z.
///     day_after_end_date: Some(EpochMillis(1_727_740_800_000)),
///     ..SearchInput::default()
/// };
/// let filters = parse_search_input(&input, &english)?;
/// assert_eq!(filters.description.as_deref(), Some("Groceries"));
/// assert_eq!(filters.min_amount, Some(Cents(1_050)));
/// assert_eq!(filters.end_date, Some(EpochMillis(1_727_740_799_999)));
/// # Ok(())
/// # }
/// ```
pub fn parse_search_input(
    input: &SearchInput,
    symbols: &NumberSymbols,
) -> Result<SearchFilters, ReportError> {
    let description = input.description.trim_matches(is_whitespace);
    let min_amount =
        optional_cents(&input.min_amount, symbols).map_err(ReportError::InvalidMinAmount)?;
    let max_amount =
        optional_cents(&input.max_amount, symbols).map_err(ReportError::InvalidMaxAmount)?;
    let end_date = input
        .day_after_end_date
        .map(|next_day| {
            next_day
                .0
                .checked_sub(1)
                .map(EpochMillis)
                .ok_or(ReportError::Overflow)
        })
        .transpose()?;
    let filters = SearchFilters {
        description: (!description.is_empty()).then(|| description.to_owned()),
        category_id: input.category_id,
        min_amount,
        max_amount,
        start_date: input.start_date,
        end_date,
    };
    if is_valid_search_range(&filters) {
        Ok(filters)
    } else {
        Err(ReportError::Domain(DomainError::InvalidSearchRange))
    }
}

/// `None` for blank text, as Kotlin's `isBlank()` decides, and otherwise the amount it reads as.
fn optional_cents(
    text: &str,
    symbols: &NumberSymbols,
) -> Result<Option<Cents>, denarii_core::MoneyParseError> {
    if is_blank(text) {
        Ok(None)
    } else {
        parse_cents(text, symbols).map(Some)
    }
}

#[cfg(test)]
mod tests {
    use denarii_core::{
        CategoryId, Cents, DomainError, EpochMillis, MoneyParseError, NumberSymbols, SearchFilters,
    };

    use super::{SearchInput, parse_search_input};
    use crate::error::ReportError;

    /// 2024-09-01T00:00:00Z and 2024-10-01T00:00:00Z, the moments v1's test parsed its dates to
    /// in UTC.
    const SEPTEMBER_1: i64 = 1_725_148_800_000;
    const OCTOBER_1: i64 = 1_727_740_800_000;

    fn english() -> NumberSymbols {
        NumberSymbols {
            decimal_separator: '.',
            currency_marks: vec!["$".to_owned(), "$".to_owned(), "USD".to_owned()],
        }
    }

    fn parse(input: &SearchInput) -> Result<SearchFilters, ReportError> {
        parse_search_input(input, &english())
    }

    #[test]
    fn parse_builds_filters_from_optional_inputs() {
        // v1's SearchFilterParserTest.parseBuildsFiltersFromOptionalInputs, with the dates as
        // Dart passes them.
        let input = SearchInput {
            description: " Groceries ".to_owned(),
            category_id: Some(CategoryId(1)),
            min_amount: "10.5".to_owned(),
            max_amount: "20.5".to_owned(),
            start_date: Some(EpochMillis(SEPTEMBER_1)),
            day_after_end_date: Some(EpochMillis(OCTOBER_1)),
        };

        assert_eq!(
            parse(&input),
            Ok(SearchFilters {
                description: Some("Groceries".to_owned()),
                category_id: Some(CategoryId(1)),
                min_amount: Some(Cents(1_050)),
                max_amount: Some(Cents(2_050)),
                start_date: Some(EpochMillis(1_725_148_800_000)),
                end_date: Some(EpochMillis(1_727_740_799_999)),
            })
        );
    }

    #[test]
    fn parse_leaves_blank_inputs_unset() {
        // v1's SearchFilterParserTest.parseLeavesBlankInputsUnset, with Kotlin's whitespace.
        let input = SearchInput {
            description: " \t\u{3000}\u{1F}".to_owned(),
            min_amount: "  ".to_owned(),
            max_amount: "\u{A0}".to_owned(),
            ..SearchInput::default()
        };

        assert_eq!(parse(&input), Ok(SearchFilters::default()));
        assert_eq!(parse(&SearchInput::default()), Ok(SearchFilters::default()));
    }

    #[test]
    fn the_description_is_trimmed_with_kotlins_whitespace() {
        let input = SearchInput {
            description: "\u{1F} café ☕\u{A0}".to_owned(),
            ..SearchInput::default()
        };
        assert_eq!(
            parse(&input).unwrap().description.as_deref(),
            Some("café ☕")
        );

        // U+0085 isn't whitespace to Kotlin, so it stays.
        let input = SearchInput {
            description: "\u{85}".to_owned(),
            ..SearchInput::default()
        };
        assert_eq!(
            parse(&input).unwrap().description.as_deref(),
            Some("\u{85}")
        );
    }

    #[test]
    fn parse_rejects_invalid_amounts_with_why() {
        // v1's SearchFilterParserTest.parseRejectsInvalidMinimumAmount and
        // parseRejectsInvalidMaximumAmount.
        let min = SearchInput {
            min_amount: "1.2.3".to_owned(),
            ..SearchInput::default()
        };
        assert_eq!(
            parse(&min),
            Err(ReportError::InvalidMinAmount(MoneyParseError::Malformed))
        );
        let max = SearchInput {
            max_amount: "4.5.6".to_owned(),
            ..SearchInput::default()
        };
        assert_eq!(
            parse(&max),
            Err(ReportError::InvalidMaxAmount(MoneyParseError::Malformed))
        );
        let too_precise = SearchInput {
            max_amount: "4.555".to_owned(),
            ..SearchInput::default()
        };
        assert_eq!(
            parse(&too_precise),
            Err(ReportError::InvalidMaxAmount(
                MoneyParseError::TooManyDecimals
            ))
        );
    }

    #[test]
    fn the_minimum_is_read_before_the_maximum() {
        let both = SearchInput {
            min_amount: "abc".to_owned(),
            max_amount: "def".to_owned(),
            ..SearchInput::default()
        };
        assert!(matches!(
            parse(&both),
            Err(ReportError::InvalidMinAmount(_))
        ));
    }

    #[test]
    fn amounts_are_read_in_the_users_locale() {
        let french = NumberSymbols {
            decimal_separator: ',',
            currency_marks: vec!["€".to_owned(), "€".to_owned(), "EUR".to_owned()],
        };
        let input = SearchInput {
            min_amount: "10,5 €".to_owned(),
            max_amount: "١٢".to_owned(),
            ..SearchInput::default()
        };
        let filters = parse_search_input(&input, &french).unwrap();
        assert_eq!(filters.min_amount, Some(Cents(1_050)));
        assert_eq!(filters.max_amount, Some(Cents(1_200)));
    }

    #[test]
    fn parse_rejects_an_inverted_date_range() {
        // v1's SearchFilterParserTest.parseRejectsInvertedDateRange: from September 30 to
        // September 1.
        let input = SearchInput {
            start_date: Some(EpochMillis(OCTOBER_1 - 86_400_000)),
            day_after_end_date: Some(EpochMillis(SEPTEMBER_1 + 86_400_000)),
            ..SearchInput::default()
        };
        assert_eq!(
            parse(&input),
            Err(ReportError::Domain(DomainError::InvalidSearchRange))
        );
    }

    #[test]
    fn a_one_day_search_runs_to_the_moment_before_the_next_day() {
        let input = SearchInput {
            start_date: Some(EpochMillis(SEPTEMBER_1)),
            day_after_end_date: Some(EpochMillis(SEPTEMBER_1 + 86_400_000)),
            ..SearchInput::default()
        };
        let filters = parse(&input).unwrap();
        assert_eq!(filters.start_date, Some(EpochMillis(SEPTEMBER_1)));
        assert_eq!(
            filters.end_date,
            Some(EpochMillis(SEPTEMBER_1 + 86_399_999))
        );

        // A day after that starts where the start date does leaves an end before the start.
        let empty = SearchInput {
            day_after_end_date: Some(EpochMillis(SEPTEMBER_1)),
            ..input
        };
        assert_eq!(
            parse(&empty),
            Err(ReportError::Domain(DomainError::InvalidSearchRange))
        );
    }

    #[test]
    fn parse_rejects_a_minimum_above_the_maximum_and_accepts_them_equal() {
        let above = SearchInput {
            min_amount: "20.01".to_owned(),
            max_amount: "20".to_owned(),
            ..SearchInput::default()
        };
        assert_eq!(
            parse(&above),
            Err(ReportError::Domain(DomainError::InvalidSearchRange))
        );
        let equal = SearchInput {
            min_amount: "20".to_owned(),
            max_amount: "20.00".to_owned(),
            ..SearchInput::default()
        };
        let filters = parse(&equal).unwrap();
        assert_eq!(
            (filters.min_amount, filters.max_amount),
            (Some(Cents(2_000)), Some(Cents(2_000)))
        );
    }

    #[test]
    fn an_amount_error_comes_before_a_range_error() {
        let input = SearchInput {
            max_amount: "1.2.3".to_owned(),
            start_date: Some(EpochMillis(OCTOBER_1)),
            day_after_end_date: Some(EpochMillis(SEPTEMBER_1)),
            ..SearchInput::default()
        };
        assert_eq!(
            parse(&input),
            Err(ReportError::InvalidMaxAmount(MoneyParseError::Malformed))
        );
    }

    #[test]
    fn the_smallest_moment_has_no_moment_before_it() {
        let input = SearchInput {
            day_after_end_date: Some(EpochMillis(i64::MIN)),
            ..SearchInput::default()
        };
        assert_eq!(parse(&input), Err(ReportError::Overflow));
        let largest = SearchInput {
            day_after_end_date: Some(EpochMillis(i64::MAX)),
            ..SearchInput::default()
        };
        assert_eq!(
            parse(&largest).unwrap().end_date,
            Some(EpochMillis(i64::MAX - 1))
        );
    }

    #[test]
    fn an_open_ended_search_keeps_its_one_bound() {
        let only_start = SearchInput {
            start_date: Some(EpochMillis(OCTOBER_1)),
            min_amount: "5".to_owned(),
            ..SearchInput::default()
        };
        let filters = parse(&only_start).unwrap();
        assert_eq!(filters.start_date, Some(EpochMillis(OCTOBER_1)));
        assert_eq!(filters.end_date, None);
        assert_eq!(filters.min_amount, Some(Cents(500)));
        assert_eq!(filters.max_amount, None);
    }
}
