# Subsystem: `core`

`denarii-core` (`rust/crates/core`) holds Denarii Dolor's domain rules: money as whole cents, the
three kinds of transaction, the change each write makes to stored account balances, and the
field checks every write passes. It guarantees that text v1 accepted as an amount gives the same
cents, that every balance change is exact or an error, and that no input makes it panic. It is the
bottom layer of ADR-0004 (Proposed: Rust owns the rules and the data) and uses only `std`. Built in
M0.4–M0.7 as described here; the API reference is `cargo doc -p denarii-core --open`.

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
  `localeNumberSymbols` built them. The Flutter app fills them from `package:intl` (M1.6).
- Digits and currency signs are recognized only in the Basic Multilingual Plane (U+0000 to
  U+FFFF), because v1 read text one UTF-16 code unit at a time. The tables in
  `src/text/unicode_tables.rs` come from `tools/unicode_tables.py`.
- The largest accepted amount is `Cents::MAX_PARSED`, 9,999,999,999.99.
- Formatting for display (`$1,234.50`) isn't here; Dart does it (M1.6).

## Transactions and balance changes

```rust
pub enum TransactionKind { Expense, Income, Transfer { to_account: AccountId } }
pub struct Transaction { pub id: Option<TransactionId>, pub description: String, pub amount: Cents,
                         pub category_id: CategoryId, pub account_id: AccountId, pub date: EpochMillis,
                         pub kind: TransactionKind }
pub fn balance_deltas(previous: Option<&Transaction>, current: Option<&Transaction>)
    -> Result<BTreeMap<AccountId, Cents>, ArithmeticOverflow>;
```

- `Transaction::balance_impact` and `Transaction::account_impacts` are the only code that matches
  on the kind; totals and balances call them, as v1's invariant required.
- `balance_deltas` subtracts the previous version's impacts, adds the current one's, and drops the
  accounts whose change is zero. The storage layer applies the result in the same database
  transaction as the row (M2.4).
- `TransactionKind::from_stored` maps a row's `type` and `transfer_account_id`; a transfer with no
  destination gives `None`, which the reader treats as having no balance impact (BUG-06).
- `TransactionType::stored_name` gives `EXPENSE`, `INCOME`, and `TRANSFER`, the values in the
  database and in CSV files.

## Validation

```rust
pub struct TransactionDraft { /* the form's fields, with transaction_type and transfer_account_id */ }
pub fn validate_transaction(draft: &TransactionDraft) -> Result<Transaction, DomainError>;
pub fn budget_overage(category_name: &str, spent_this_month: Cents, amount: Cents, monthly_limit: Cents)
    -> Result<Option<BudgetOverage>, ArithmeticOverflow>;
```

- `validate_transaction` checks v1's field rules in v1's order and returns the transaction with
  its description trimmed, which is what v1 stored (BUG-08).
- The checks that need stored rows (category and accounts exist, and the monthly budget) belong to
  the storage layer (M2.4). `budget_overage` is their arithmetic: what was spent this month, plus
  the amount, minus the limit.
- `DomainError` has v1's 23 variants, and `DomainError::code` returns v1's names, such as
  `BUDGET_EXCEEDED`, which name the localized messages.
- `defaults` holds the seeded account IDs 1–2 and category IDs 1–3, which can't be deleted, and
  the categories' icon keys.

## Differences from v1

Each difference below affects only input that v1's validation never let into the database, or a
detail no user sees.

| Case | v1 | `core` |
| :- | :- | :- |
| Arithmetic past the range of a 64-bit number of cents | Wrapped silently | Returns `ArithmeticOverflow` |
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

## Tests

| Invariant | Checked by |
| :- | :- |
| 2, 3 | `tests/money.rs`, which ports every case of v1's `MoneyTest.kt` and adds the limit and round-trip cases |
| 4, 5 | `tests/ledger.rs`, which ports v1's `LedgerTest.kt` and `TransactionModelTest.kt` and adds the overflow cases |
| 6, 7 | `tests/validation.rs`, which ports v1's `ValidatorsTest.kt` and checks the order of errors |
| Kotlin text rules | Unit tests in `src/text.rs` |
| 1, 8 | `rust/crates/core/Cargo.toml` has no `[dependencies]`; review checks for floating point |

Run them with `cargo test --workspace --locked` from `rust/`.
