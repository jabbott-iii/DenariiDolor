/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! Fixture F8: CSV exports and cells that v1's own `GenerateReportUseCase` and
//! `ReportCsvFormatter` wrote (`tests/fixtures/README.md`). The rows below are the ones the
//! fixture's writer gave v1, and the report must come out byte for byte the same.

use std::collections::BTreeSet;
use std::error::Error;
use std::fs;

use denarii_core::{
    Account, AccountId, Category, CategoryId, Cents, EpochMillis, StoredTransaction, Totals,
    TransactionId, TransactionType,
};
use denarii_report::{
    CsvLabels, MonthDays, MonthlyReport, ReportFormat, TextDirection, escape_cell,
};

type TestResult<T> = Result<T, Box<dyn Error>>;

// The writer's moments, in milliseconds since 1970, with their time in UTC and in Beirut.
/// 2026-09-30T20:59:59.999Z, 23:59:59.999 EEST on September 30.
const SEPTEMBER_LAST: i64 = 1_790_801_999_999;
/// 2026-09-30T21:00:00Z, midnight EEST on October 1.
const OCTOBER_START: i64 = 1_790_802_000_000;
/// 2026-10-03T09:00:00Z.
const OCTOBER_3: i64 = 1_791_018_000_000;
/// 2026-10-05T06:30:00Z.
const OCTOBER_5: i64 = 1_791_181_800_000;
/// 2026-10-10T09:00:00Z.
const OCTOBER_10: i64 = 1_791_622_800_000;
/// 2026-10-12T09:00:00Z.
const OCTOBER_12: i64 = 1_791_795_600_000;
/// 2026-10-24T20:30:00Z, the first 23:30 on October 24, in EEST.
const OCTOBER_24_FIRST_2330: i64 = 1_792_873_800_000;
/// 2026-10-24T21:30:00Z, the second 23:30 on October 24, in EET.
const OCTOBER_24_SECOND_2330: i64 = 1_792_877_400_000;
/// 2026-10-24T22:00:00Z, midnight EET on October 25.
const OCTOBER_25_START: i64 = 1_792_879_200_000;
/// 2026-10-31T21:59:59.999Z, the month's last moment.
const OCTOBER_LAST: i64 = 1_793_483_999_999;
/// 2026-10-31T22:00:00Z, midnight EET on November 1.
const NOVEMBER_START: i64 = 1_793_484_000_000;

fn fixture(name: &str) -> TestResult<Vec<u8>> {
    let path = format!("{}/tests/fixtures/{name}", env!("CARGO_MANIFEST_DIR"));
    Ok(fs::read(path)?)
}

fn text_fixture(name: &str) -> TestResult<String> {
    Ok(String::from_utf8(fixture(name)?)?)
}

/// October 2026 in `Asia/Beirut`, from the day starts v1's Java wrote: 31 first moments, then
/// November's.
fn october() -> TestResult<MonthDays> {
    let moments = text_fixture("v1-f8-day-starts.txt")?
        .lines()
        .map(|line| line.parse().map(EpochMillis))
        .collect::<Result<Vec<_>, _>>()?;
    let (next, days) = moments.split_last().ok_or("the file holds no moment")?;
    Ok(MonthDays::new(2026, 10, days.to_vec(), *next)?)
}

fn txn(
    id: i64,
    (transaction_type, description, cents): (TransactionType, &str, i64),
    (category, account, transfer_to): (i64, i64, Option<i64>),
    date: i64,
) -> StoredTransaction {
    StoredTransaction {
        id: TransactionId(id),
        transaction_type,
        description: description.to_owned(),
        amount: Cents(cents),
        category_id: CategoryId(category),
        account_id: AccountId(account),
        transfer_account_id: transfer_to.map(AccountId),
        date: EpochMillis(date),
    }
}

/// The writer's rows dated in October, in the order it gave them.
fn october_rows() -> Vec<StoredTransaction> {
    use TransactionType::{Expense, Income, Transfer};
    vec![
        txn(1, (Income, "راتب", 250_000), (2, 1, None), OCTOBER_START),
        txn(
            2,
            (Expense, "=HYPERLINK(\"http://x\")", 1_250),
            (4, 1, None),
            OCTOBER_3,
        ),
        txn(3, (Transfer, "ادخار", 30_000), (3, 1, Some(2)), OCTOBER_5),
        txn(
            4,
            (Expense, "Lunch, \"team\"; @cmd", 4_599),
            (1, 3, None),
            OCTOBER_24_FIRST_2330,
        ),
        txn(
            5,
            (Expense, " +1 refund\ttab-=2", 99),
            (99, 1, None),
            OCTOBER_24_SECOND_2330,
        ),
        txn(6, (Expense, "Coffee", 375), (1, 7, None), OCTOBER_25_START),
        txn(
            7,
            (Transfer, "تحويل\nناقص", 10_000),
            (3, 2, None),
            OCTOBER_10,
        ),
        txn(8, (Expense, "Odd row", 500), (1, 1, Some(2)), OCTOBER_12),
        txn(10, (Income, "-5", 500), (2, 1, None), OCTOBER_LAST),
        txn(9, (Expense, "Refund;-5", 200), (1, 1, None), OCTOBER_LAST),
    ]
}

fn categories() -> Vec<Category> {
    [
        (1, "مصروفات عامة"),
        (2, "دخل عام"),
        (3, "تحويل"),
        (4, "@Groceries"),
    ]
    .into_iter()
    .map(|(id, name)| Category {
        id: Some(CategoryId(id)),
        ..Category::new(name)
    })
    .collect()
}

fn accounts() -> Vec<Account> {
    [(1, "نقدًا"), (2, "Savings, \"main\""), (3, "-Card")]
        .into_iter()
        .map(|(id, name)| Account {
            id: Some(AccountId(id)),
            name: name.to_owned(),
            balance: Cents::ZERO,
        })
        .collect()
}

fn report(direction: TextDirection) -> TestResult<MonthlyReport> {
    let month = october()?;
    Ok(MonthlyReport::new(
        &month,
        &october_rows(),
        &categories(),
        &accounts(),
        direction,
    )?)
}

fn decode_hex(hex: &str) -> TestResult<String> {
    let bytes = (0..hex.len())
        .step_by(2)
        .map(|at| {
            let pair = hex.get(at..at.saturating_add(2)).ok_or("odd hex digits")?;
            Ok(u8::from_str_radix(pair, 16)?)
        })
        .collect::<TestResult<Vec<u8>>>()?;
    Ok(String::from_utf8(bytes)?)
}

/// Each line of an escape fixture: the cell, and what v1's `escape` made of it.
fn escape_vectors(name: &str) -> TestResult<Vec<(String, String)>> {
    text_fixture(name)?
        .lines()
        .map(|line| {
            let (input, output) = line.split_once('\t').ok_or("a line without a tab")?;
            Ok((decode_hex(input)?, decode_hex(output)?))
        })
        .collect()
}

#[test]
fn the_arabic_export_is_the_one_v1_wrote_byte_for_byte() {
    let labels = CsvLabels {
        title: "تقرير الإنفاق الشهري".to_owned(),
        period: "أكتوبر 2026".to_owned(),
        generated_at: "2026-11-02 10:15 EET".to_owned(),
    };
    let csv = report(TextDirection::RightToLeft).unwrap().to_csv(&labels);
    assert_eq!(csv.as_bytes(), fixture("v1-f8-report-ar.csv").unwrap());
}

#[test]
fn the_english_export_is_the_one_v1_wrote_byte_for_byte() {
    let labels = CsvLabels {
        title: "Monthly Spending Report".to_owned(),
        period: "October 2026".to_owned(),
        generated_at: "2026-11-02 10:15 EET".to_owned(),
    };
    let csv = report(TextDirection::LeftToRight).unwrap().to_csv(&labels);
    assert_eq!(csv.as_bytes(), fixture("v1-f8-report-en.csv").unwrap());
}

#[test]
fn the_report_has_v1s_totals_and_file_name() {
    let report = report(TextDirection::RightToLeft).unwrap();
    assert_eq!(
        report.totals(),
        Totals {
            income: Cents(250_500),
            expense: Cents(7_023),
            net: Cents(243_477),
        }
    );
    assert_eq!(
        report.file_name(ReportFormat::Csv),
        "spending-report-2026-10.csv"
    );
    assert_eq!(
        report.file_name(ReportFormat::Pdf),
        "spending-report-2026-10.pdf"
    );
}

#[test]
fn the_rows_v1_left_out_are_outside_the_month() {
    let october = october().unwrap();
    for outside in [SEPTEMBER_LAST, NOVEMBER_START] {
        assert_eq!(october.date_of(EpochMillis(outside)), None, "{outside}");
    }
    assert_eq!(october.bounds().start(), EpochMillis(OCTOBER_START));
    assert_eq!(october.bounds().next_start(), EpochMillis(NOVEMBER_START));
}

#[test]
fn every_cell_escapes_as_v1_escaped_it_on_android() {
    // The same cells on API 26 and API 36 gave the same bytes, which this file holds.
    let vectors = escape_vectors("v1-f8-escape-android.tsv").unwrap();
    assert_eq!(vectors.len(), 2_121);
    for (input, expected) in &vectors {
        assert_eq!(&escape_cell(input), expected, "{input:?}");
    }
}

#[test]
fn the_desktop_jvm_differs_from_android_only_after_a_no_break_space() {
    // v1's unit tests ran ReportCsvFormatter on a desktop Java, whose `\s` is six ASCII
    // characters. Android's is Unicode's White_Space, so the guard after a `;` or tab also
    // passes a no-break space there. The port follows the phones.
    let desktop = escape_vectors("v1-f8-escape.tsv").unwrap();
    let android = escape_vectors("v1-f8-escape-android.tsv").unwrap();
    assert_eq!(desktop.len(), android.len());

    let mut differences = Vec::new();
    for ((input, on_desktop), (same_input, on_android)) in desktop.iter().zip(&android) {
        assert_eq!(input, same_input);
        if on_desktop != on_android {
            differences.push(input.as_str());
        }
    }
    assert_eq!(
        differences,
        [
            "a;\u{A0}=1",
            ";\u{A0}\u{A0}\r+I\nN",
            "\rD;\u{A0}+,fNa",
            "\n,\t\u{A0}+@0NI@\r\u{1}",
            "@\t\u{A0}@@",
        ]
    );
}

#[test]
fn the_guard_after_a_semicolon_passes_androids_whitespace_and_nothing_else() {
    // v1 on Android guarded `a;<c>=1` for exactly these characters, the semicolon included.
    let guarded_on_android: BTreeSet<u32> = text_fixture("v1-f8-inner-space-android.txt")
        .unwrap()
        .lines()
        .map(|line| u32::from_str_radix(line, 16).unwrap())
        .collect();
    assert_eq!(guarded_on_android.len(), 26);

    let guarded_here: BTreeSet<u32> = (0..=0xFFFF_u32)
        .filter_map(char::from_u32)
        .filter(|c| escape_cell(&format!("a;{c}=1")).contains(&format!("{c}'=1")))
        .map(u32::from)
        .collect();
    assert_eq!(guarded_here, guarded_on_android);
}
