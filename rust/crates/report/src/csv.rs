/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The monthly report as CSV text, with v1's quoting and formula guard (CS-20).
//!
//! This ports v1's `ReportCsvFormatter` (`shared/.../domain/report/ReportCsvFormatter.kt` at
//! `6d57526`). People open these files in spreadsheets, and a spreadsheet runs a cell that starts
//! with `=`, `+`, `-`, or `@` as a formula, which can fetch a URL or run a command. So a cell that
//! would run as one gets an apostrophe in front, which spreadsheets show as text, unless it
//! reads as a number, such as `-12.50`. The same goes for each part of a cell after a `;` or a
//! tab, because a spreadsheet that splits cells on those starts a new cell there.

use denarii_core::is_whitespace as is_kotlin_whitespace;

use crate::monthly::MonthlyReport;

/// The text of a report's CSV export that the user's language and time zone decide, which Dart
/// formats.
///
/// v1 wrote each of them as `docs/specs/android-v1-formats.md`, section 8, says: the title
/// translated, the period as the month and year in the phone's language with the pattern
/// `MMMM yyyy`, and the moment it was made as `yyyy-MM-dd HH:mm z` in US English, in the
/// phone's time zone.
#[derive(Debug, Clone, PartialEq, Eq, Hash, Default)]
pub struct CsvLabels {
    /// The report's title, translated, such as `Monthly Spending Report`.
    pub title: String,
    /// The month and year, such as `October 2026` or `أكتوبر 2026`.
    pub period: String,
    /// When the report was made, such as `2026-11-02 10:15 EET`.
    pub generated_at: String,
}

/// The formats a report is exported in.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum ReportFormat {
    /// Comma-separated values, which [`MonthlyReport::to_csv`] writes.
    Csv,
    /// A PDF document, which the app draws (M4.5).
    Pdf,
}

impl ReportFormat {
    /// The file name extension: `csv` or `pdf`.
    #[must_use]
    pub const fn extension(self) -> &'static str {
        match self {
            Self::Csv => "csv",
            Self::Pdf => "pdf",
        }
    }

    /// The media type the share sheet and the file picker get: `text/csv` or `application/pdf`.
    #[must_use]
    pub const fn mime_type(self) -> &'static str {
        match self {
            Self::Csv => "text/csv",
            Self::Pdf => "application/pdf",
        }
    }
}

impl MonthlyReport {
    /// The report as CSV text, as v1 exported it.
    ///
    /// Six key-value rows (`title`, `period`, `generated_at`, `total_income`, `total_expense`,
    /// and `net`), an empty line, a header row, and one row for each transaction, every line
    /// ending in CRLF. The keys, the header, and the type names stay in English in every
    /// language, and amounts are plain, such as `-0.05`. Every cell goes through
    /// [`escape_cell`]. Encode the text as UTF-8 with no byte order mark, as v1 wrote it.
    #[must_use]
    pub fn to_csv(&self, labels: &CsvLabels) -> String {
        let totals = self.totals();
        let mut csv = String::new();
        push_row(&mut csv, &["title", &labels.title]);
        push_row(&mut csv, &["period", &labels.period]);
        push_row(&mut csv, &["generated_at", &labels.generated_at]);
        push_row(&mut csv, &["total_income", &totals.income.to_plain()]);
        push_row(&mut csv, &["total_expense", &totals.expense.to_plain()]);
        push_row(&mut csv, &["net", &totals.net.to_plain()]);
        csv.push_str("\r\n");
        push_row(
            &mut csv,
            &[
                "date",
                "type",
                "category",
                "description",
                "amount",
                "payment_method",
            ],
        );
        for row in self.rows() {
            push_row(
                &mut csv,
                &[
                    &row.local_date.to_string(),
                    row.transaction_type.stored_name(),
                    &row.category_name,
                    &row.description,
                    &row.amount.to_plain(),
                    &row.payment_method,
                ],
            );
        }
        csv
    }
}

fn push_row(csv: &mut String, cells: &[&str]) {
    for (column, cell) in cells.iter().enumerate() {
        if column > 0 {
            csv.push(',');
        }
        csv.push_str(&escape_cell(cell));
    }
    csv.push_str("\r\n");
}

/// The characters that make a spreadsheet run a cell as a formula.
const FORMULA_TRIGGERS: [char; 4] = ['=', '+', '-', '@'];

/// The characters that RFC 4180 needs a cell quoted for, with `;` and the tab, which some
/// spreadsheets split cells on.
const QUOTE_TRIGGERS: [char; 6] = [',', '"', '\n', '\r', ';', '\t'];

/// A CSV cell for `value`, with v1's formula guard and RFC 4180 quoting (CS-20).
///
/// - A value that would run as a formula gets an apostrophe in front: one that starts with `=`,
///   `+`, `-`, `@`, a tab, or a carriage return, or that starts with one of the first four after
///   leading whitespace, as Kotlin's `trimStart()` decides it. A value that reads as a number
///   once trimmed, as Java's `Double.parseDouble` reads one, is left alone, so `-12.50` and
///   `-1e3` stay numbers.
/// - Each part after a `;` or a tab that starts with one of the four, after any whitespace as
///   Unicode's `White_Space` property decides it, gets the same apostrophe, unless it reads as a
///   number up to the next `;` or tab.
/// - The result is quoted, with each `"` doubled, if it got an apostrophe at its start or holds a
///   `,`, `"`, line feed, carriage return, `;`, or tab.
///
/// The whitespace after a `;` or tab is what Android's regular expressions matched with `\s`,
/// where v1 ran, rather than the six ASCII characters a desktop Java matches: on Android, v1
/// guarded `a;\u{A0}=1` (`docs/specs/report.md`, "Differences from v1").
///
/// # Examples
///
/// ```
/// use denarii_report::escape_cell;
///
/// assert_eq!(escape_cell("=HYPERLINK(\"x\")"), "\"'=HYPERLINK(\"\"x\"\")\"");
/// assert_eq!(escape_cell("Lunch; @cmd; ok"), "\"Lunch; '@cmd; ok\"");
/// assert_eq!(escape_cell("-12.50"), "-12.50");
/// assert_eq!(escape_cell("plain"), "plain");
/// ```
#[must_use]
pub fn escape_cell(value: &str) -> String {
    let starts_like_formula = value
        .chars()
        .next()
        .is_some_and(|c| FORMULA_TRIGGERS.contains(&c) || c == '\t' || c == '\r')
        || value
            .trim_start_matches(is_kotlin_whitespace)
            .chars()
            .next()
            .is_some_and(|c| FORMULA_TRIGGERS.contains(&c));
    let looks_like_formula =
        starts_like_formula && !is_java_double(value.trim_matches(is_kotlin_whitespace));
    let inner = guard_inner_cells(value);
    let safe = if looks_like_formula {
        format!("'{inner}")
    } else {
        inner
    };
    if looks_like_formula || safe.contains(QUOTE_TRIGGERS) {
        format!("\"{}\"", safe.replace('"', "\"\""))
    } else {
        safe
    }
}

/// `value` with an apostrophe before each part that follows a `;` or a tab and would run as a
/// formula.
///
/// This is v1's `INNER_FORMULA` replacement, `(?<=[;\t])(\s*)([=+\-@][^;\t]*)`, run as Android's
/// regular expressions ran it: from the start, each match is the leftmost one that begins after
/// the previous match ends, and `\s` is Unicode's `White_Space`, which [`char::is_whitespace`]
/// tests.
fn guard_inner_cells(value: &str) -> String {
    let chars: Vec<char> = value.chars().collect();
    let mut guarded = String::with_capacity(value.len());
    let mut at = 0;
    let mut previous = None;
    while let Some(&c) = chars.get(at) {
        if matches!(previous, Some(';' | '\t')) {
            let rest = chars.get(at..).unwrap_or_default();
            let space_len = rest.iter().take_while(|c| c.is_whitespace()).count();
            let after_space = rest.get(space_len..).unwrap_or_default();
            if after_space
                .first()
                .is_some_and(|c| FORMULA_TRIGGERS.contains(c))
            {
                // The part runs to the next `;` or tab, and holds at least its trigger.
                let cell_len = after_space
                    .iter()
                    .take_while(|c| !matches!(c, ';' | '\t'))
                    .count();
                let cell: String = after_space.iter().take(cell_len).collect();
                guarded.extend(rest.iter().take(space_len));
                if !is_java_double(cell.trim_matches(is_kotlin_whitespace)) {
                    guarded.push('\'');
                }
                guarded.push_str(&cell);
                at = at.saturating_add(space_len).saturating_add(cell_len);
                previous = cell.chars().last();
                continue;
            }
        }
        guarded.push(c);
        previous = Some(c);
        at = at.saturating_add(1);
    }
    guarded
}

/// Whether Java's `Double.parseDouble` reads `text` as a number, which is when v1's Kotlin
/// `toDoubleOrNull()` returned one.
///
/// Java's grammar, from the documentation of `Double.valueOf(String)`: optional characters from
/// U+0000 to U+0020 at both ends, an optional sign, and then `NaN`, `Infinity`, a decimal number
/// with an optional exponent, or a hexadecimal number with a binary exponent, either of the last
/// two with an optional `f`, `F`, `d`, or `D` after it. Digits are ASCII only.
fn is_java_double(text: &str) -> bool {
    let body = text.trim_matches(|c: char| c <= '\u{20}');
    let unsigned = body.strip_prefix(['+', '-']).unwrap_or(body).as_bytes();
    if unsigned == b"NaN" || unsigned == b"Infinity" {
        return true;
    }
    let number = unsigned
        .strip_suffix(b"f")
        .or_else(|| unsigned.strip_suffix(b"F"))
        .or_else(|| unsigned.strip_suffix(b"d"))
        .or_else(|| unsigned.strip_suffix(b"D"))
        .unwrap_or(unsigned);
    match number
        .strip_prefix(b"0x")
        .or_else(|| number.strip_prefix(b"0X"))
    {
        Some(hex) => is_hex_number(hex),
        None => is_decimal_number(number),
    }
}

/// `Digits . Digits? Exp?`, `Digits Exp?`, or `. Digits Exp?`, where `Exp` is `[eE][+-]?Digits`.
fn is_decimal_number(text: &[u8]) -> bool {
    let (whole, rest) = split_digits(text, u8::is_ascii_digit);
    let (fraction, exponent) = match rest.strip_prefix(b".") {
        Some(after_point) => split_digits(after_point, u8::is_ascii_digit),
        None => (&[][..], rest),
    };
    (!whole.is_empty() || !fraction.is_empty())
        && (exponent.is_empty() || is_exponent(exponent, b"eE"))
}

/// What follows `0x`: `HexDigits .?` or `HexDigits? . HexDigits`, then a binary exponent,
/// `[pP][+-]?Digits`, which a hexadecimal number needs.
fn is_hex_number(text: &[u8]) -> bool {
    let (whole, rest) = split_digits(text, u8::is_ascii_hexdigit);
    let (fraction, exponent) = match rest.strip_prefix(b".") {
        Some(after_point) => split_digits(after_point, u8::is_ascii_hexdigit),
        None => (&[][..], rest),
    };
    (!whole.is_empty() || !fraction.is_empty()) && is_exponent(exponent, b"pP")
}

/// `[<marks>][+-]?Digits`, the whole of `text`.
fn is_exponent(text: &[u8], marks: &[u8]) -> bool {
    let Some((mark, rest)) = text.split_first() else {
        return false;
    };
    let digits = rest
        .strip_prefix(b"+")
        .or_else(|| rest.strip_prefix(b"-"))
        .unwrap_or(rest);
    marks.contains(mark) && !digits.is_empty() && digits.iter().all(u8::is_ascii_digit)
}

/// The run of digits at the start of `text`, and what follows it.
fn split_digits(text: &[u8], is_digit: fn(&u8) -> bool) -> (&[u8], &[u8]) {
    let run = text.iter().take_while(|b| is_digit(b)).count();
    text.split_at_checked(run).unwrap_or((text, &[]))
}

#[cfg(test)]
mod tests {
    use denarii_core::{
        Account, AccountId, Category, CategoryId, Cents, EpochMillis, StoredTransaction,
        TransactionId, TransactionType,
    };

    use super::{CsvLabels, ReportFormat, escape_cell, is_java_double};
    use crate::calendar::MonthDays;
    use crate::monthly::{MonthlyReport, TextDirection};

    #[test]
    fn csv_neutralizes_formula_injection() {
        // v1's ReportFormattingTest.csvNeutralizesFormulaInjection.
        assert_eq!(
            escape_cell("=HYPERLINK(\"x\")"),
            "\"'=HYPERLINK(\"\"x\"\")\""
        );
        assert_eq!(escape_cell("@cmd"), "\"'@cmd\"");
        assert_eq!(escape_cell("-12.50"), "-12.50");
        assert_eq!(escape_cell("plain"), "plain");
    }

    #[test]
    fn csv_neutralizes_formulas_after_whitespace_semicolons_and_tabs() {
        // v1's ReportFormattingTest.csvNeutralizesFormulasAfterWhitespaceSemicolonsAndTabs.
        assert_eq!(escape_cell("x;=1+1"), "\"x;'=1+1\"");
        assert_eq!(escape_cell("x\t=1+1"), "\"x\t'=1+1\"");
        assert_eq!(escape_cell(" =1+1"), "\"' =1+1\"");
        assert_eq!(escape_cell("Lunch; @cmd; ok"), "\"Lunch; '@cmd; ok\"");
        assert_eq!(escape_cell("Refund;-5"), "\"Refund;-5\"");
        assert_eq!(escape_cell("a;b"), "\"a;b\"");
    }

    #[test]
    fn a_tab_or_carriage_return_at_the_start_needs_the_guard() {
        assert_eq!(escape_cell("\t1"), "\"\t1\"");
        assert_eq!(escape_cell("\r=1"), "\"'\r=1\"");
        assert_eq!(escape_cell("\t"), "\"'\t\"");
        // The leading tab guards the cell, and the `=` after it guards its inner part too.
        assert_eq!(escape_cell("\t=1"), "\"'\t'=1\"");
    }

    #[test]
    fn leading_whitespace_is_kotlins_and_the_inner_whitespace_androids() {
        // U+00A0 is whitespace to Kotlin's trimStart and to Android's `\s`; U+001C only to
        // Kotlin, and U+0085 only to Android.
        assert_eq!(escape_cell("\u{A0}=1"), "\"'\u{A0}=1\"");
        assert_eq!(escape_cell("\u{1C}=1"), "\"'\u{1C}=1\"");
        assert_eq!(escape_cell("\u{85}=1"), "\u{85}=1");
        assert_eq!(escape_cell("a;\u{A0}=1"), "\"a;\u{A0}'=1\"");
        assert_eq!(escape_cell("a;\u{85}=1"), "\"a;\u{85}'=1\"");
        assert_eq!(escape_cell("a;\u{1C}=1"), "\"a;\u{1C}=1\"");
        assert_eq!(escape_cell("a;\u{3000}@x"), "\"a;\u{3000}'@x\"");
    }

    #[test]
    fn every_inner_part_is_guarded_from_left_to_right() {
        assert_eq!(escape_cell("=1;=2"), "\"'=1;'=2\"");
        assert_eq!(escape_cell("a;;=1"), "\"a;;'=1\"");
        assert_eq!(escape_cell("a;\t;=1"), "\"a;\t;'=1\"");
        assert_eq!(escape_cell("a\t\t=1"), "\"a\t\t'=1\"");
        assert_eq!(escape_cell("a; \t=1"), "\"a; \t'=1\"");
        assert_eq!(escape_cell("a;+1;+x"), "\"a;+1;'+x\"");
        assert_eq!(escape_cell("a; -5 ; =x"), "\"a; -5 ; '=x\"");
        assert_eq!(escape_cell("-5;=1"), "\"'-5;'=1\"");
        assert_eq!(escape_cell("a;="), "\"a;'=\"");
    }

    #[test]
    fn a_cell_that_reads_as_a_number_keeps_its_sign() {
        for number in [
            "-5",
            "+5",
            "-1e3",
            "-.5",
            "-5.",
            "-0x1p3",
            "-1.5f",
            "-NaN",
            "-Infinity",
        ] {
            assert_eq!(escape_cell(number), number);
        }
        assert_eq!(escape_cell(" -5"), " -5");
        assert_eq!(escape_cell("-5\u{1}"), "-5\u{1}");
        assert_eq!(escape_cell("a;-5e2"), "\"a;-5e2\"");
    }

    #[test]
    fn quoting_doubles_each_quote_and_covers_rfc_4180s_characters() {
        assert_eq!(escape_cell("a,b"), "\"a,b\"");
        assert_eq!(escape_cell("a\"b"), "\"a\"\"b\"");
        assert_eq!(escape_cell("a\nb"), "\"a\nb\"");
        assert_eq!(escape_cell("a\rb"), "\"a\rb\"");
        assert_eq!(escape_cell("\"=1\""), "\"\"\"=1\"\"\"");
        assert_eq!(escape_cell(""), "");
        assert_eq!(escape_cell("💰=1"), "💰=1");
        assert_eq!(escape_cell(";=💰"), "\";'=💰\"");
    }

    #[test]
    fn javas_number_grammar_is_read_exactly() {
        let numbers = [
            "0",
            "12",
            "-12.50",
            "+5",
            ".5",
            "5.",
            "1e3",
            "1E+3",
            "1e-3",
            "1.5e3",
            ".5e1",
            "1.5f",
            "1.5F",
            "1d",
            "1D",
            "NaN",
            "-NaN",
            "Infinity",
            "+Infinity",
            "0x1p3",
            "0X1P3",
            "0x1.8p1",
            "0x.8p-1",
            "0x8.p1",
            "0xAbp0",
            "0x1p3d",
            " 5 ",
            "\u{0}5\u{20}",
            "\u{1F}-5",
        ];
        for number in numbers {
            assert!(is_java_double(number), "{number:?}");
        }
        let words = [
            "",
            " ",
            "-",
            "+",
            ".",
            "e5",
            "1e",
            "1e+",
            "1.5ff",
            "NaNf",
            "Infinityd",
            "inf",
            "nan",
            "infinity",
            "0x",
            "0x1",
            "0x1p",
            "0x.p1",
            "0xg1p1",
            "0x1.8",
            "1_000",
            "١٢",
            "--5",
            "+-5",
            "- 5",
            "5 5",
            "1.2.3",
            "1e3.5",
            "0x1p3.5",
            "5\u{A0}",
            "\u{21}5",
        ];
        for word in words {
            assert!(!is_java_double(word), "{word:?}");
        }
    }

    #[test]
    fn report_formats_have_v1s_extensions_and_media_types() {
        assert_eq!(ReportFormat::Csv.extension(), "csv");
        assert_eq!(ReportFormat::Pdf.extension(), "pdf");
        assert_eq!(ReportFormat::Csv.mime_type(), "text/csv");
        assert_eq!(ReportFormat::Pdf.mime_type(), "application/pdf");
    }

    const DAY: i64 = 86_400_000;
    /// 2026-09-01T00:00:00Z, the moment of both rows of v1's `ReportFormattingTest`.
    const SEPTEMBER_1_UTC: i64 = 1_788_220_800_000;
    /// 2026-08-01T07:00:00Z, midnight on August 1 in `America/Phoenix` (UTC-7, no daylight
    /// saving).
    const AUGUST_1_PHOENIX: i64 = 1_785_567_600_000;

    /// `days` days of 24 hours from `start`, then the next month's start.
    fn month(year: i32, month: u8, start: i64, days: usize) -> MonthDays {
        let mut moments =
            std::iter::successors(Some(start), |s| s.checked_add(DAY)).map(EpochMillis);
        let starts = moments.by_ref().take(days).collect();
        MonthDays::new(year, month, starts, moments.next().unwrap()).unwrap()
    }

    /// The rows and names of v1's `ReportFormattingTest`, in `days`.
    fn v1_report(days: &MonthDays) -> MonthlyReport {
        let row = |id, category, description: &str, amount, account| StoredTransaction {
            id: TransactionId(id),
            transaction_type: TransactionType::Expense,
            description: description.to_owned(),
            amount: Cents(amount),
            category_id: CategoryId(category),
            account_id: AccountId(account),
            transfer_account_id: None,
            date: EpochMillis(SEPTEMBER_1_UTC),
        };
        let categories = [(1, "Dining"), (2, "Misc")].map(|(id, name)| Category {
            id: Some(CategoryId(id)),
            ..Category::new(name)
        });
        let accounts = [(1, "Cash"), (2, "Visa")].map(|(id, name)| Account {
            id: Some(AccountId(id)),
            name: name.to_owned(),
            balance: Cents::ZERO,
        });
        MonthlyReport::new(
            days,
            &[
                row(1, 1, "Lunch, \"team\"", 1_250, 1),
                row(2, 2, "=HYPERLINK(\"x\")", 100, 2),
            ],
            &categories,
            &accounts,
            TextDirection::LeftToRight,
        )
        .unwrap()
    }

    #[test]
    fn csv_contains_metadata_header_and_rows() {
        // v1's ReportFormattingTest.csvContainsMetadataHeaderAndRows. v1's test set its totals
        // by hand; here the two rows give them.
        let september = month(2026, 9, SEPTEMBER_1_UTC, 30);
        let labels = CsvLabels {
            title: "Monthly Spending Report".to_owned(),
            period: "September 2026".to_owned(),
            generated_at: "2026-09-21 12:00 UTC".to_owned(),
        };
        let csv = v1_report(&september).to_csv(&labels);

        let lines: Vec<&str> = csv.split("\r\n").collect();
        assert_eq!(
            lines,
            [
                "title,Monthly Spending Report",
                "period,September 2026",
                "generated_at,2026-09-21 12:00 UTC",
                "total_income,0.00",
                "total_expense,13.50",
                "net,-13.50",
                "",
                "date,type,category,description,amount,payment_method",
                "2026-09-01,EXPENSE,Dining,\"Lunch, \"\"team\"\"\",12.50,Cash",
                "2026-09-01,EXPENSE,Misc,\"'=HYPERLINK(\"\"x\"\")\",1.00,Visa",
                "",
            ]
        );
    }

    #[test]
    fn csv_writes_dates_in_the_reports_zone() {
        // v1's ReportFormattingTest.csvWritesTimesAndDatesInTheReportsZone: midnight UTC on
        // September 1 is still August 31 in Phoenix. The time made is Dart's label now.
        let august = month(2026, 8, AUGUST_1_PHOENIX, 31);
        let csv = v1_report(&august).to_csv(&CsvLabels::default());

        let lines: Vec<&str> = csv.split("\r\n").collect();
        assert!(
            lines[8].starts_with("2026-08-31,EXPENSE,Dining,"),
            "{}",
            lines[8]
        );
        assert!(
            lines[9].starts_with("2026-08-31,EXPENSE,Misc,"),
            "{}",
            lines[9]
        );
    }
}
