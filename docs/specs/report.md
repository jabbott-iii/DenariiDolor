# Subsystem: `report`

`denarii-report` (`rust/crates/report`) builds a month's report, its CSV export, and the search
screen's filters from rows its caller reads. It guarantees that a report lists every transaction
of its month in v1's order with v1's names and totals, that its CSV text is the bytes v1 wrote
for the same rows and labels, with v1's formula guard (CS-20) as it ran on phones, and that no
input makes it panic. It depends on `denarii-core` alone and opens no database: `denarii-store`
runs the reads (`docs/specs/store.md`, "Reading the ledger"). ADR-0019 (Dart passes each month's
bounds, and Rust reads no time zone) and ADR-0021 (the store runs a report's reads, and Dart
passes the first moment of each of its days) shaped it. Section 8 of
`docs/specs/android-v1-formats.md` describes the CSV it must write. Built in M2.7 as described
here; the API reference is `cargo doc -p denarii-report --open`, run from `rust/`.

## Why this design

v1's `GenerateReportUseCase` read the month's rows itself, and its `ReportCsvFormatter` read the
phone's time zone and language. The port splits that work three ways:

- **`store` reads, `report` decides.** The layer table in `docs/01-architecture.md` lets
  `report` call only `core`, so the store runs the queries and this crate works on plain values.
  Its tests need no database, and the same rows can come from a fixture.
- **Dart supplies the calendar and the language.** Rust reads no time zone (ADR-0019), but each
  CSV row carries its date on the phone's calendar. Dart passes the first moment of each day of
  the month, so Rust can place every moment on its day, including on a 25-hour day (ADR-0021).
  Dart also formats the title, the month, and the time made, and says which way the language is
  written.
- **Rust writes the whole CSV.** The quoting and the formula guard stay in one place, and
  fixture F8, which v1's own code wrote, checks every byte.

What it trades away: the bridge must call the store and this crate in turn, and Dart must format
the labels as v1 did for the files to match. Rust can't check that the day starts come from the
right zone.

## The month

```rust
pub struct LocalDate { pub year: i32, pub month: u8, pub day: u8 }   // displays as YYYY-MM-DD
pub struct MonthDays { /* year, month, day_starts, bounds */ }
impl MonthDays {
    pub const MAX_DAY_SPAN_MILLIS: i64; // 48 hours
    pub fn new(year: i32, month: u8, day_starts: Vec<EpochMillis>, next_month_start: EpochMillis)
        -> Result<MonthDays, ReportError>;
    pub const fn year(&self) -> i32;
    pub const fn month(&self) -> u8;
    pub const fn bounds(&self) -> MonthBounds;
    pub fn date_of(&self, moment: EpochMillis) -> Option<LocalDate>;
}
```

- Dart computes `day_starts[d - 1]` as `DateTime(year, month, d)` and `next_month_start` as
  `DateTime(year, month + 1, 1)`, in the phone's current zone (ADR-0021).
- `new` refuses them with `InvalidMonthDays` unless the year is 1 to 9999, the month 1 to 12,
  there is one start for each day of the month in the Gregorian calendar, the first start and
  `next_month_start` make a `MonthBounds` (27 to 32 days), and each day lasts 0 to 48 hours. A
  day lasts 0 hours where a date was skipped, as Samoa skipped December 30, 2011, and 48 where a
  place repeated a date.
- `date_of` gives the last day whose first moment is at or before the moment, so the moments of
  a skipped date belong to the next one. It gives `None` outside the month.
- `bounds` is what `Store::transactions_in_month` takes.

## The report

```rust
pub enum TextDirection { LeftToRight, RightToLeft }
pub struct ReportRow { pub transaction_id: TransactionId, pub date: EpochMillis, pub local_date: LocalDate,
                       pub transaction_type: TransactionType, pub category_name: String,
                       pub description: String, pub amount: Cents, pub payment_method: String }
pub struct MonthlyReport { /* year, month, totals, rows */ }
impl MonthlyReport {
    pub fn new(days: &MonthDays, transactions: &[StoredTransaction], categories: &[Category],
               accounts: &[Account], direction: TextDirection) -> Result<MonthlyReport, ReportError>;
    pub const fn year(&self) -> i32;
    pub const fn month(&self) -> u8;
    pub const fn totals(&self) -> Totals;
    pub fn rows(&self) -> &[ReportRow];
    pub fn file_name(&self, format: ReportFormat) -> String;
}
```

`new` ports what v1's use case did with the rows it read:

1. Each transaction must fall in the month; the first that doesn't, in the order given, fails
   with `DateOutsideMonth`. Its date on the calendar is `days.date_of`.
2. Its category's name, or `#` and the ID when no category has it, such as `#99`.
3. Its payment method: its account's name, or `#` and the ID. When the row's stored
   `transfer_account_id` names an account, the method is `source → destination`, or
   `source ← destination` in a right-to-left language, where the source is read first, on the
   right. v1 read that column whatever the type, so an expense that names a destination shows a
   route, and a malformed transfer, which names none, shows its account alone.
4. The rows are sorted by date, then by ID.
5. The totals are `core::totals` over every row that maps to a transaction. A malformed transfer
   is listed and counts in no total (BUG-06). A total past the range of an `i64` fails with
   `Overflow`.

`file_name` gives `spending-report-<YYYY-MM>.csv` or `.pdf`, v1's names.

## The CSV

```rust
pub struct CsvLabels { pub title: String, pub period: String, pub generated_at: String }
pub enum ReportFormat { Csv, Pdf }           // extension(), mime_type()
impl MonthlyReport { pub fn to_csv(&self, labels: &CsvLabels) -> String; }
pub fn escape_cell(value: &str) -> String;
```

`to_csv` writes section 8 of the format spec: six key-value rows (`title`, `period`,
`generated_at`, `total_income`, `total_expense`, `net`), an empty line, the header
`date,type,category,description,amount,payment_method`, and one row for each transaction, every
line ending in CRLF. Dates are `YYYY-MM-DD`, types `EXPENSE`, `INCOME`, or `TRANSFER`, and
amounts plain (`Cents::to_plain`). The caller encodes the text as UTF-8 with no byte order mark.
The labels are Dart's: v1 wrote the title translated, the period with Java's `MMMM yyyy` in the
phone's language, and the time made with `yyyy-MM-dd HH:mm z` in US English, in the phone's zone.

`escape_cell` runs on every cell, keys and header included. It ports v1's `escape`:

1. **A cell that starts like a formula** gets an apostrophe in front, which spreadsheets show as
   text: one that starts with `=`, `+`, `-`, `@`, a tab, or a carriage return, or with one of the
   first four after leading whitespace as Kotlin's `trimStart()` decides it
   (`core::is_whitespace`). A cell that reads as a number once trimmed is spared, so `-12.50`
   stays a number.
2. **Each part after a `;` or a tab** that starts with one of the four, after any whitespace,
   gets the same apostrophe unless it reads as a number up to the next `;` or tab. A spreadsheet
   that splits cells on `;` or tabs starts a new cell there (CS-20). This is v1's regular
   expression `(?<=[;\t])(\s*)([=+\-@][^;\t]*)`, applied from left to right.
3. **Quoting** follows RFC 4180: the cell is wrapped in `"`, with each `"` doubled, if step 1
   guarded it, or if it holds a `,`, `"`, line feed, carriage return, `;`, or tab.

"Reads as a number" is Java's `Double.parseDouble` grammar, which v1's Kotlin `toDoubleOrNull()`
followed: characters up to U+0020 at both ends, an optional sign, then `NaN`, `Infinity`, a
decimal with an optional exponent, or a hexadecimal number with a binary exponent (`0x1p3`), the
last two with an optional `f`, `F`, `d`, or `D`. Only ASCII digits count, so `-١٢` is guarded.

The whitespace in step 2 is Unicode's `White_Space` property (`char::is_whitespace`), which is
what Android's ICU-based `\s` matched when v1 ran on a phone. A desktop Java's `\s` is six ASCII
characters, which is what v1's unit tests saw. Fixture F8 holds the cells escaped both ways: they
differ only where a no-break space follows a `;` or a tab, and on API 26 and API 36 alike the
phone guarded those cells. The port follows the phones, which guard more cells: the maintainer
chose it on 2026-11-25 over the desktop Java's six characters.

## The search

```rust
pub struct SearchInput { pub description: String, pub category_id: Option<CategoryId>,
                         pub min_amount: String, pub max_amount: String,
                         pub start_date: Option<EpochMillis>, pub day_after_end_date: Option<EpochMillis> }
pub fn parse_search_input(input: &SearchInput, symbols: &NumberSymbols) -> Result<SearchFilters, ReportError>;
```

It ports v1's `SearchFilterParser`, whose result `Store::search_transactions` takes:

- The description is trimmed with Kotlin's whitespace, and left out when nothing remains.
- A blank amount, as Kotlin's `isBlank()` decides, is left out. Any other goes through
  `core::parse_cents` with the locale's symbols; a refusal is `InvalidMinAmount` or
  `InvalidMaxAmount`, with `parse_cents`'s reason, the minimum checked first.
- The category is the picker's ID, or `None` for every category.
- The dates are first moments that Dart computes from the date picker's dates in the phone's
  zone: the start date's, and the day after the end date's. The end is the moment before the
  latter, as v1's `parseIsoDateToEndOfDayEpochMillis` gave.
- Then a start after the end, or a minimum above the maximum, fails with
  `Domain(InvalidSearchRange)`.

## Errors

`ReportError` has six variants:

| Variant | When |
| :- | :- |
| `InvalidMonthDays` | The year, month, or day starts don't describe a calendar month: the caller's bug |
| `DateOutsideMonth { id, date }` | A transaction given to `MonthlyReport::new` isn't in the month: the caller read another month |
| `Overflow` | A total would leave the range of an `i64`, or a search's day after its end is the smallest moment there is |
| `InvalidMinAmount(MoneyParseError)` | The search's minimum isn't an amount |
| `InvalidMaxAmount(MoneyParseError)` | The search's maximum isn't an amount |
| `Domain(DomainError)` | The search's range is inverted: `INVALID_SEARCH_RANGE` |

v1's search screen showed its "invalid filters" message for every search error; M4.3 maps the
variants.

## Differences from v1

| Case | v1 | `report` |
| :- | :- | :- |
| The guard after a `;` or tab, where whitespace before the formula isn't ASCII | Android's `\s` matched Unicode's `White_Space`; v1's unit tests, on a desktop Java, matched six ASCII characters | Unicode's `White_Space`, as on phones |
| A report's title, month, and time made | Part of `MonthlyReport`, from the clock and `ReportText` | `CsvLabels` that Dart formats, at export |
| Which way a transfer's arrow points | The language of `Locale.getDefault()`, in a list of 11 right-to-left languages | The `TextDirection` Dart passes |
| A transaction outside the report's month | Couldn't happen: the use case read the month itself | `DateOutsideMonth` |
| A total past the range of 64-bit cents | Kotlin's sum wrapped around | `Overflow` |
| The search's category | Text from the picker, refused unless a whole number | The picker's ID |
| The search's dates | `YYYY-MM-DD` text from the date picker, parsed in the phone's zone, other text refused | First moments that Dart computes |
| Why a search was refused | An `IllegalArgumentException` with English text | A `ReportError` variant |

## Invariants

1. `MonthDays::new` accepts exactly the year, month, and day starts its rules describe, and
   `date_of` gives the last day whose first moment is at or before a moment, or `None` outside
   the month.
2. A report lists every transaction it was given, once, by date and then by ID, with its
   category's and account's names or `#` and the ID, and a route for every row that names a
   destination, with the arrow of the text direction.
3. A report's totals are `core::totals` over the rows that map to a transaction; a malformed
   transfer is listed and counted in no total.
4. A transaction outside the month fails with `DateOutsideMonth`, and a total out of range with
   `Overflow`.
5. For fixture F8's rows and labels, `to_csv` gives the bytes v1 wrote, in Arabic and in
   English.
6. `escape_cell` gives what v1's `escape` gave on Android for each of F8's 2,121 cells, and the
   guard after a `;` fires for exactly the characters it fired for on Android.
7. `parse_search_input` gives v1's filters for every case of v1's `SearchFilterParserTest` that
   still applies, checks in v1's order, and ends a search one millisecond before the day after
   its end date.
8. No function panics on any input, and no sum wraps.

## Tests

| Invariant | Checked by |
| :- | :- |
| 1 | `src/calendar.rs`: every day of a UTC February, the moments either side of the month, the 25-hour October 24 of F8, a skipped date, 0- and 48-hour days and one millisecond past, days out of order, too few and too many days, a leap year, spans too short, moments at the ends of `i64`, and years and months outside the calendar |
| 2, 3, 4 | `src/monthly.rs`: every case of v1's `GenerateReportUseCaseTest`, a right-to-left arrow, rows at one moment given out of order, a malformed transfer, an expense that names a destination, an empty month, a category and account with no row, a name with no ID, rows outside the month, and a total past `i64::MAX` |
| 5 | `tests/f8.rs`: both exports compared byte for byte with `tests/fixtures/v1-f8-report-ar.csv` and `v1-f8-report-en.csv`, the totals, the file names, and the two rows outside the month; `tests/fixtures/README.md` describes the fixture. `src/csv.rs`: v1's `ReportFormattingTest` cases for the CSV's lines and for dates in another zone |
| 6 | `tests/f8.rs`: every cell of `v1-f8-escape-android.tsv`, the five cells where the desktop Java's `v1-f8-escape.tsv` differs, and every character of the Basic Multilingual Plane after `a;` against `v1-f8-inner-space-android.txt`; `src/csv.rs`: every case of v1's `ReportFormattingTest` for `escape`, and Java's number grammar case by case |
| 7 | `src/search.rs`: every case of v1's `SearchFilterParserTest` but the two that parse a category or a date from text, Kotlin's whitespace, the order of the errors, a French and an Arabic-Indic amount, a one-day search, and the smallest and largest moments |
| 8 | The tests above, at the edges of `i64` and on malformed input; no fuzz target exists yet |

Run them with `cargo test --locked -p denarii-report` from `rust/`, or only F8's with
`cargo test --locked -p denarii-report --test f8`.
