# Subsystem: `core`

`denarii-core` (`rust/crates/core`) holds Denarii Dolor's domain rules: money as whole cents, the
three kinds of transaction, the change each write makes to stored account balances, and the
field checks every write passes. It guarantees that text v1 accepted as an amount gives the same
cents, that every balance change is exact or an error, and that no input makes it panic. It is the
bottom layer of ADR-0004 (Proposed: Rust owns the rules and the data) and uses only `std`. Built in
M0.4–M0.7 as described here, with months and `Transaction::budgeted_amount` added in M2.4, and
`validate_name`, `is_blank`, and the values that five `DomainError` variants carry added in
M2.5, and `StoredTransaction`, `totals`, and `is_whitespace` added in M2.7; the API reference is
`cargo doc -p denarii-core --open`.

## Why this design

v1 kept these rules in plain Kotlin (`Money.kt`, `Ledger.kt`, `Validators.kt`, the transaction
classes, and `ValidateTransactionUseCase.kt`) so JVM tests could cover them. The rewrite needs the
same rules on Android and iOS, and users' stored balances were computed by v1's rules, so the
port must agree with v1 on every input v1 could produce. Two choices follow:

- **Port, don't redesign.** Parsing, validation order, and limits are v1's, including its quirks:
  lengths in UTF-16 code units and Kotlin's set of whitespace characters (`text` module). A
  cleaner rule would reject or accept text that v1 handled the other way, which users would see
  as the app changing under them.
- **Make bad states unrepresentable where v1 only checked them.** A transfer's destination lives
  inside `TransactionKind::Transfer`, so a transfer without one, or an expense with one, can't
  be built. IDs and amounts are newtypes, so an account ID can't be passed as a category ID.

What it trades away: the crate carries Unicode tables generated from Unicode 15.0 instead of using
a Unicode crate, and its text rules aren't Rust's defaults, so contributors must use the `text`
helpers rather than `str::trim` and `str::len`.

## Money

```rust
pub struct Cents(pub i64);
pub struct NumberSymbols { pub decimal_separator: char, pub currency_marks: Vec<String> }
pub fn parse_cents(text: &str, symbols: &NumberSymbols) -> Result<Cents, MoneyParseError>;
pub fn normalize_digits(text: &str) -> String;
impl Cents { pub fn to_plain(self) -> String; pub fn to_input(self) -> String; /* checked_add, checked_sub, checked_neg */ }
```

- `parse_cents` follows v1's `Money.parseToCents` step by step: normalize digits, take the sign
  and the currency marks off, find the decimal mark, check the thousands grouping, then convert.
  Its rules are in its doc comment.
- The caller supplies `NumberSymbols`: the locale's decimal separator, then the currency's symbol
  in the user's locale, its symbol in the default locale, and its ISO 4217 code, as v1's
  `localeNumberSymbols` built them. The Flutter app fills them with `moneyParsingSymbols` in
  `lib/l10n/money_format.dart` (ADR-0016): `intl`'s decimal separator for the locale, then the
  currency's symbol in that locale and its code. v1's symbol in the default locale is left out,
  because on a phone the default locale was the user's.
- Digits and currency signs are recognized only in the Basic Multilingual Plane (U+0000 to
  U+FFFF), because v1 read text one UTF-16 code unit at a time. The tables in
  `src/text/unicode_tables.rs` come from `tools/unicode_tables.py`.
- The largest accepted amount is `Cents::MAX_PARSED`, 9,999,999,999.99.
- Formatting for display (`$1,234.50`) isn't here; Dart does it with `formatMoney` in
  `lib/l10n/money_format.dart` (ADR-0016). The core reads back what `formatMoney` writes, except
  Indian grouping of a lakh (`1,00,000`) or more: `parse_cents`, like v1, accepts only groups of
  three digits.

## Transactions and balance changes

```rust
pub enum TransactionKind { Expense, Income, Transfer { to_account: AccountId } }
pub struct Transaction { pub id: Option<TransactionId>, pub description: String, pub amount: Cents,
                         pub category_id: CategoryId, pub account_id: AccountId, pub date: EpochMillis,
                         pub kind: TransactionKind }
pub fn balance_deltas(previous: Option<&Transaction>, current: Option<&Transaction>)
    -> Result<BTreeMap<AccountId, Cents>, ArithmeticOverflow>;
pub struct StoredTransaction { pub id: TransactionId, pub transaction_type: TransactionType,
                               pub description: String, pub amount: Cents, pub category_id: CategoryId,
                               pub account_id: AccountId, pub transfer_account_id: Option<AccountId>,
                               pub date: EpochMillis }
pub struct Totals { pub income: Cents, pub expense: Cents, pub net: Cents }
pub fn totals<'a>(transactions: impl IntoIterator<Item = &'a Transaction>) -> Result<Totals, ArithmeticOverflow>;
```

- `Transaction::balance_impact`, `Transaction::account_impacts`,
  `Transaction::budgeted_amount`, and `totals` are the only code that matches on the kind to
  compute money; balances, the budget check, and reports call them, as v1's invariant required.
  `budgeted_amount` is an expense's amount, and `None` for income and transfers, which no budget
  limits.
- `balance_deltas` subtracts the previous version's impacts, adds the current one's, and drops the
  accounts whose change is zero. The storage layer applies the result in the same database
  transaction as the row (`docs/specs/store.md`, "Transaction writes").
- `TransactionKind::from_stored` maps a row's `type` and `transfer_account_id`; a transfer with no
  destination gives `None`, which the reader treats as having no balance impact (BUG-06).
- `StoredTransaction` is a row as the storage layer reads it for reports and searches, which list
  a malformed transfer too, with its `transfer_account_id` column as stored.
  `StoredTransaction::transaction` gives the `Transaction` the row stands for, with its kind from
  `from_stored`, or `None` for a malformed transfer. Added in M2.7.
- `totals` adds up income amounts, expense amounts, and every transaction's balance impact, as
  v1's `GenerateReportUseCase` did for its monthly report; a transfer counts in neither of the
  first two and adds zero to the net. The caller leaves out malformed transfers, which map to no
  transaction. A stored amount keeps its sign, as v1's sums kept it. Added in M2.7, for
  `denarii-report` (`docs/specs/report.md`).
- `TransactionType::stored_name` gives `EXPENSE`, `INCOME`, and `TRANSFER`, the values in the
  database and in CSV files.

## Validation

```rust
pub struct TransactionDraft { /* the form's fields, with transaction_type and transfer_account_id */ }
pub fn validate_transaction(draft: &TransactionDraft) -> Result<Transaction, DomainError>;
pub fn budget_overage(category_name: &str, spent_this_month: Cents, amount: Cents, monthly_limit: Cents)
    -> Result<Option<BudgetOverage>, ArithmeticOverflow>;
pub fn validate_name(name: &str) -> Result<&str, DomainError>;
pub fn is_blank(text: &str) -> bool;
pub fn is_whitespace(c: char) -> bool;
```

- `validate_transaction` checks v1's field rules in v1's order and returns the transaction with
  its description trimmed, which is what v1 stored (BUG-08).
- The checks that need stored rows (category and accounts exist, and the monthly budget) run in
  the storage layer (`docs/specs/store.md`, "Transaction writes"). `budget_overage` is the
  budget's arithmetic: what was spent this month, plus the amount, minus the limit.
- `validate_name` trims a category or account name with Kotlin's whitespace and returns it, or
  `InvalidName` when it isn't 1 to `MAX_NAME_UNITS` (50) UTF-16 code units long; the storage
  layer stores the trimmed name, as v1 did. `is_blank` is Kotlin's `isBlank()`, which decides
  whether a category's icon was given (`docs/specs/store.md`, "Categories, accounts, and
  budgets"). `is_whitespace` is Kotlin's `Char.isWhitespace()` as it ran on Android, which
  `trim()`, `trimStart()`, and `isBlank()` used; `denarii-report` trims with it as v1's search
  screen and CSV formatter trimmed (M2.7). Android's `Character.isWhitespace` also counts U+180E,
  which desktop Java dropped with Unicode 6.3; fixture F7 showed v1 trimming it on API 26 and
  API 36, so `is_whitespace` counts it too.
- `DomainError` has v1's 23 variants, and `DomainError::code` returns v1's names, such as
  `BUDGET_EXCEEDED`, which name the localized messages. v1 passed a value with some of them,
  which the message shows, and the variants carry the same value: `DescriptionTooLong` and
  `InvalidName` the limit, `BudgetExceeded` the overage, `DuplicateCategoryName` and
  `DuplicateAccountName` the trimmed name, and `CategoryInUse` and `AccountInUse` how many
  transactions use the row.
- `defaults` holds the seeded account IDs 1–2 and category IDs 1–3, which can't be deleted, and
  the categories' icon keys.

## Months

Built in M2.4 as described here.

```rust
pub struct MonthBounds { /* start, next_start */ }
impl MonthBounds {
    pub const MIN_SPAN_MILLIS: i64; // 27 days
    pub const MAX_SPAN_MILLIS: i64; // 32 days
    pub fn new(start: EpochMillis, next_start: EpochMillis) -> Result<MonthBounds, InvalidMonthBounds>;
    pub const fn start(self) -> EpochMillis;
    pub const fn next_start(self) -> EpochMillis;
    pub const fn contains(self, date: EpochMillis) -> bool;
}
```

- A calendar month in the user's time zone, as two moments: the first moment of its first day,
  which belongs to it, and the first moment of the next month's first day, which doesn't. Rust
  reads no time zone, so Dart works the moments out with `DateTime(year, month, 1)` and passes
  them in (ADR-0019: Dart passes each month's bounds, and Rust reads no time zone).
- The first moment of a day isn't always midnight. In `America/Asuncion`, clocks went from
  midnight to 01:00 on 2023-10-01, so October 2023 started at 01:00 there. v1 took the same
  moments with kotlinx-datetime's `atStartOfDayIn`, and Dart's `DateTime` gave the same moment
  for that day when ADR-0019 checked it.
- `new` accepts a span of 27 to 32 days, inclusive. Over the 599 zones of tzdata 2026e and every
  month from 1970 to 2100, months last from 27.96 days (February 1975 in `America/Adak`) to 31.29
  days (January 1994 in `Antarctica/Vostok`). So the limits refuse swapped bounds and a day's or a
  year's, never a real month. Which month the bounds describe, and in which zone, only the caller
  can check.

## Differences from v1

Each difference below affects only input that v1's validation never let into the database, or a
detail no user sees.

| Case | v1 | `core` |
| :- | :- | :- |
| Arithmetic past the range of a 64-bit number of cents, in a balance or a report's totals | Wrapped silently | Returns `ArithmeticOverflow` |
| A transfer whose destination is its own account | `accountImpacts` gave the account `+amount`, because Kotlin's `mapOf` kept the last pair | No change, which is the correct impact; validation rejects such transfers in both |
| Why an amount was rejected | `null` | A `MoneyParseError` variant |
| Unicode data for digits and currency signs | The phone's Java runtime | Unicode 15.0, fixed at generation time |
| Case-insensitive `TransactionType::parse` | Kotlin's Unicode case folding, so `ſ` (U+017F) matched `S` | ASCII only |
| Where the description is trimmed | When the row is written | When the draft is validated; the stored text is the same |

## Invariants

1. Every amount is a whole number of cents in an `i64`; no function in the crate uses floating
   point.
2. `parse_cents` gives v1's result for every case in v1's `MoneyTest.kt`, and accepts at most
   `Cents::MAX_PARSED`.
3. `Cents::to_plain` and `Cents::to_input` parse back to the same cents.
4. `balance_deltas(Some(t), Some(t))` is empty for every transaction `t` that doesn't overflow,
   and `balance_deltas(None, Some(t))` is the negation of `balance_deltas(Some(t), None)`.
5. No function panics or wraps on any input; arithmetic out of range returns `ArithmeticOverflow`.
6. `validate_transaction` reports the first broken rule in v1's order, with v1's limits in UTF-16
   code units and Kotlin's whitespace.
7. `DomainError::code` returns v1's name for every variant.
8. `denarii-core` depends on no crate outside `std`.
9. `MonthBounds::new` accepts exactly the spans from `MIN_SPAN_MILLIS` to `MAX_SPAN_MILLIS`, and
   `contains` holds the start and not the next month's start.
10. `totals` gives the sum of the income's amounts, the sum of the expenses' amounts, and the sum
    of every balance impact, with a transfer in none of the first two, or `ArithmeticOverflow`
    when any of the three leaves the range of an `i64`.
11. `StoredTransaction::transaction` keeps every field of the row, and gives `None` exactly for a
    transfer with no destination.

## Tests

| Invariant | Checked by |
| :- | :- |
| 2, 3 | `tests/money.rs`, which ports every case of v1's `MoneyTest.kt` and adds the limit and round-trip cases |
| 4, 5 | `tests/ledger.rs`, which ports v1's `LedgerTest.kt` and `TransactionModelTest.kt` and adds the overflow cases |
| 6, 7 | `tests/validation.rs`, which ports v1's `ValidatorsTest.kt` and checks the order of errors, `validate_name` and `is_blank` with Kotlin's whitespace and UTF-16 lengths, and the codes of the reference data's errors |
| Kotlin text rules | Unit tests in `src/text.rs` |
| 1, 8 | `rust/crates/core/Cargo.toml` has no `[dependencies]`; review checks for floating point |
| 9 | `tests/month.rs`: the limits, one millisecond past each, swapped and empty bounds, a day's and a year's, spans that overflow an `i64`, and months before 1970 |
| `budgeted_amount` | `tests/ledger.rs`: each kind |
| 10, 11 | `tests/ledger.rs`: v1's `GenerateReportUseCaseTest` month, several of each kind, negative stored amounts, an empty set, each sum's overflow, the net's alone, and transfers of `i64::MAX`; a stored row of each type, with and without a destination |
| `is_whitespace` | `tests/validation.rs`: Kotlin's set against U+0085, U+200B, and control characters |

Run them with `cargo test --workspace --locked` from `rust/`.
