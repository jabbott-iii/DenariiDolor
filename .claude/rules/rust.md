---
paths:
  - "**/*.rs"
  - "**/Cargo.toml"
  - "Cargo.lock"
  - "rust-toolchain.toml"
  - "clippy.toml"
  - "rustfmt.toml"
  - "deny.toml"
  - ".cargo/**"
---

# Rust standards

These rules cover the Rust in this repository: crates, build scripts, tests, benchmarks, manifests,
and toolchain files. They describe how a senior engineer writes Rust. Doc comments, error messages,
and other text follow `writing.md` in this folder.

Apply them to code you write or edit. Don't rewrite existing code only to make it conform; fix a
passage when you are already changing it, and mention anything else you notice.

## Project details

A repository fills in this section when it adopts the rule, and the rest of the rule refers to
each value by its bold name. If a value still reads `[FILL IN: …]`, the rule isn't set up here
yet. Work out that value from the repository itself, and tell the user which values are missing
so they can fill them in.

- **Crates:** the workspace in `rust/`. `crates/core` (`denarii-core`) holds the domain rules:
  money in cents, transactions, balance changes, and validation. `crates/bridge`
  (`denarii-bridge`) is the API Dart calls through `flutter_rust_bridge` (ADR-0005), with its
  generated glue in `src/frb_generated.rs`. `crates/store` (`denarii-store`) opens the SQLCipher
  database with `rusqlite` (ADR-0006, ADR-0017), and runs its writes and reads. `crates/report`
  (`denarii-report`) builds the monthly report, its CSV text, and the search filters from rows the
  store reads (ADR-0021). The planned crates (`vault` and `backup`) and the tasks that build them
  are in `docs/01-architecture.md`
- **Toolchain:** edition 2024, Rust 1.100.0 pinned in `rust/rust-toolchain.toml`, and
  `rust-version = "1.100"` in `[workspace.package]`
- **Check commands:** run from `rust/`: `cargo fmt --all --check`, `cargo clippy --workspace
  --all-targets --locked -- -D warnings`, `cargo test --workspace --locked`, and `RUSTDOCFLAGS="-D
  warnings" cargo doc --workspace --no-deps --locked`
- **Lint configuration:** `[workspace.lints]` in `rust/Cargo.toml`, plus `rust/clippy.toml`.
  `crates/bridge/Cargo.toml` repeats those lints in a `[lints]` table of its own, with
  `unsafe_code` at `deny`, because Cargo can't relax one inherited lint (ADR-0014); change both
  tables together
- **Dependency policy:** ask first, and record each decision in `docs/02-dependency-policy.md`.
  `denarii-core` uses `std` only
- **Unsafe policy:** forbidden in every crate (`unsafe_code = "forbid"`). The one exception is
  the bridge's generated foreign function interface code, `crates/bridge/src/frb_generated.rs`,
  which `src/lib.rs` exempts with `#[expect(…, reason = "…")]` on its `mod` line (ADR-0005,
  ADR-0014). Hand-written code in `bridge` has `unsafe_code` at `deny`, and no `unsafe`
- **Untrusted input:** money text a user types (`core::money`, limited by `Cents::MAX_PARSED`);
  later, backup files (`backup`, M5.1), the v1 database and profile read during the upgrade
  (`store` and `vault`, M2 and M3), and every value that crosses the bridge from Dart. Format
  limits are in `docs/specs/android-v1-formats.md`
- **Errors:** one error enum per crate, implementing `Display` and `std::error::Error`.
  `core::DomainError` names each broken business rule, and its `code()` gives the stable name the
  Dart UI maps to a localized message; users never see `Display` text
- **Logging:** none. No crate prints or logs, because shipped code may not log (`CLAUDE.md`,
  "Security posture")
- **Concurrency:** none in the hand-written code: every API is synchronous, and no crate starts a
  thread itself. `flutter_rust_bridge`'s generated handler runs each bridge call on a pool of
  worker threads, so two calls can run at once, and its `rust-async` feature starts an idle
  `tokio` runtime (ADR-0014, `docs/specs/bridge.md`). `denarii-store`'s `Store` keeps its one
  connection behind a `std::sync::Mutex`, so those calls take turns on the database (ADR-0017)
- **Public API:** none published. The `bridge` crate's API is what Dart calls, so a change to it
  regenerates the Dart bindings in the same change
- **Published crates:** none
- **Design docs:** `docs/specs/<crate>.md`: `docs/specs/core.md`, `docs/specs/bridge.md`,
  `docs/specs/store.md`, and `docs/specs/report.md`
- **Test layout:** unit tests in a `#[cfg(test)] mod tests` beside the code; integration tests
  that use only the public API in each crate's `tests/`; compatibility fixtures, such as a v1
  database and one backup per `FORMAT_VERSION`, in `tests/fixtures/` of the crate that reads them
  (from M2.1 and M5.1)

## Decisions that are the user's

Stop and ask the user before you add a dependency, change a public API or a file format, write new
`unsafe`, change the toolchain or edition, or weaken a lint or a test.

## Making a change

- **Read first.** Read the code around a change and the tests that cover it. Check the **Crates** to
  find which crate a new piece of code belongs in.
- **Check each step.** Make each step compile, and run `cargo check` after it; it is much faster
  than a full build.
- **Fix the cause, not the symptom.** For a bug, first write a test that fails, then fix the bug and
  show the test passing. Don't add a bounds check that hides an index computed wrong upstream.
- **Don't silence the compiler.** Never add `.clone()`, `'static`, `unsafe`, an `#[allow]`, or
  `Rc<RefCell<_>>` only to make an error go away. Work out what the compiler is saying, then fix the
  ownership or the design. If a clone is the right fix, say why.

## Design with types

- Make invalid states unrepresentable:
  - Use an enum instead of a `bool` plus an `Option` that is set only when the `bool` is true.
  - Use newtypes (a struct that wraps one value) for IDs, units, and validated values, so the
    compiler rejects a `UserId` passed where an `OrderId` belongs.
- Parse at the boundary. Turn raw input into validated types once, where it enters the program,
  so code inside never checks it again.
- Replace `bool` parameters with enums: `open(path, Mode::ReadOnly)` says what
  `open(path, true)` hides.
- Use a builder, or a config struct that implements `Default`, when a constructor would take many
  optional arguments.
- Avoid `_ =>` catch-all arms when you match on an enum this crate owns. Without them, adding a
  variant makes the compiler list every place that must handle it.
- Use generics or `impl Trait` (static dispatch) by default. Use `dyn Trait` when one collection
  must hold different types, or to reduce compile time and binary size.
- When iteration order reaches output (files, printed lists, test expectations), use a
  `BTreeMap` or sort first. A `HashMap`'s iteration order changes from one run to the next.
- Organize modules by what they do (`parser`, `cache`), not by kind (`utils`, `helpers`). Keep
  functions short enough that none needs comments to separate its parts.

## Ownership and borrowing

- In parameters, borrow the most general type: `&str`, `&[T]`, and `&Path` (or
  `impl AsRef<Path>`), not `&String`, `&Vec<T>`, or `&PathBuf`. Take ownership only when the
  function stores or consumes the value.
- Own data in struct fields. Give a struct a lifetime parameter only when it borrows for a clear,
  short-lived purpose, such as a parser over a borrowed buffer.
- Fix a borrow-checker error by restructuring: shorten the borrow, split a struct so its fields
  borrow separately, move a value out with `std::mem::take`, or store indices instead of
  references.
- Use `Rc` or `Arc` only for ownership that is truly shared, and `RefCell` or `Mutex` only for
  mutation that is truly shared. `Rc<RefCell<T>>` all over a design usually means its ownership
  needs rethinking.
- Return `Cow<'_, str>` when a function usually returns borrowed data but sometimes must
  allocate.
- Use iterator adapters when they read clearly; a plain `for` loop beats a chain the reader has
  to decode. Don't `collect` into a `Vec` only to iterate over it again.
- Allocate deliberately: use `with_capacity` when the size is known (and bounded, for input), and
  reuse buffers in hot loops. Don't optimize without measuring; a performance claim needs a
  benchmark, with numbers.

## Errors and panics

- Follow the **Errors** pattern.
- Library crates return typed errors: an enum that implements `Display` and
  `std::error::Error`, whose `source()` returns the underlying error, so callers can match on
  what went wrong. An application can gather errors into one type, such as
  `Box<dyn Error + Send + Sync>`, or a crate for it if the **Dependency policy** allows one.
- Add context where an error crosses a layer: the path, the operation, or the position in the
  input. Keep the original error as the source; never discard it.
- Write error messages in lowercase with no trailing period ("failed to read the config file"),
  because callers chain them with `: ` into one line.
- Propagate with `?`, converting with `From` impls or `map_err`.
- No `unwrap` outside tests. Use `expect("…")` only for a true invariant, with a message that
  says why it can't fail, and never on a value derived from input.
- Panic only for bugs, that is, a broken invariant that means the program itself is wrong. A
  missing file, bad input, or a network failure is a `Result`.
- Don't discard a `Result` (`let _ = …`) unless ignoring it is correct, and then add a comment
  that says why, for example a best-effort cleanup.

## Logging and output

- Follow the **Logging** setup. Library code never prints to stdout or stderr: it returns errors
  and logs through the project's logging.
- Only binary crates print, from their entry points, and only where the **Logging** value allows
  it.

## Untrusted input and arithmetic

Treat every byte from outside the program as possibly malformed or hostile, especially in the
**Untrusted input** modules.

- Never panic on input:
  - Don't index or slice with offsets derived from input; use `get`, or check first.
  - Don't `unwrap` or `expect` values derived from input.
  - Don't do unchecked arithmetic on input lengths, counts, or offsets.
- Check before allocating. Compare every claimed size against a limit and against the input that
  remains before `Vec::with_capacity`, `resize`, or a loop that allocates for each item.
- Bound recursion with an explicit depth limit. A stack overflow aborts the whole process; it is
  not a panic that can be caught.
- Bound loops that follow links inside the data (chains, trees, offsets), so a cycle in damaged
  input is an error, not a hang.
- Make limits configuration with named defaults, not constants buried in code, and test that
  exceeding each limit returns its documented error.
- Treat paths from input as hostile. Reject absolute paths and `..` components before joining
  them to a root directory.
- Integer overflow panics in debug builds but wraps silently in release builds, unless
  `overflow-checks` is on. Wherever overflow is possible, state what you want with
  `checked_*`, `saturating_*`, or `wrapping_*`.
- Convert numbers with `From` and `TryFrom`, not `as`, which truncates or changes the sign
  silently: `300_u32 as u8` is 44.

## Concurrency and async

- Follow the **Concurrency** model. Don't add threads or an async runtime it doesn't include.
- Prefer moving data to sharing it: channels, `std::thread::scope`, or moving data into a task.
  When state must be shared, hold each lock as briefly as possible, and document the order when
  code takes more than one.
- Never hold a lock guard or a `RefCell` borrow across an `.await`. Never block inside async
  code (blocking I/O, `std::thread::sleep`, long computation); move that work to a blocking
  thread, such as `tokio::task::spawn_blocking`.
- For an async function that keeps state across `.await` points, document what happens if the
  future is dropped at each one (its cancellation safety).
- Bound every queue and every group of spawned tasks. An unbounded channel turns a slow consumer
  into unbounded memory use.
- Give every spawned thread or task an owner that joins or cancels it, and handle the panic or
  error it returns.
- Use atomics only for simple flags and counters. Choose each memory ordering deliberately, and
  say in a comment why it is strong enough.

## `unsafe`

- Follow the **Unsafe policy**. If it doesn't allow `unsafe` where you need it, stop, and tell
  the user which safe approach you tried and why it can't work.
- Use `std` or an existing safe abstraction first.
- Where `unsafe` is allowed:
  - Put one unsafe operation in each block, preceded by a `// SAFETY:` comment that says why it
    is sound.
  - Give every `pub unsafe fn` a `# Safety` doc section that lists what the caller must uphold.
  - Deny `unsafe_op_in_unsafe_fn`, so an `unsafe fn` body still needs `unsafe` blocks.
  - Keep `unsafe` in small modules behind a safe API, and wrap every raw resource in a type whose
    `Drop` releases it. Document who owns and frees each handle.
  - Run the tests under Miri (an interpreter that detects undefined behavior) where it supports
    the code: `cargo +nightly miri test`.
- For a foreign function interface (FFI):
  - Write `unsafe extern` blocks (required in edition 2024), and name the header or document
    each declaration comes from.
  - Assert the `size_of` and `align_of` of every `#[repr(C)]` type against the values in that
    header or document, not against what the compiler produces.
  - Don't let a panic reach an `extern "C"` boundary; since Rust 1.81 it aborts the process.
    Catch it with `std::panic::catch_unwind` and return an error code instead.

## Public APIs

- Keep every API small. Make items `pub(crate)` unless other crates need them, and turn on the
  `unreachable_pub` lint.
- Follow the [Rust API Guidelines](https://rust-lang.github.io/api-guidelines/):
  - Name conversions `as_` (cheap, borrowed), `to_` (costly), or `into_` (consumes `self`);
    name getters without `get_`; start fallible variants with `try_`.
  - Implement `Debug` for every public type, and `Clone`, `PartialEq`, `Eq`, `Hash`, and
    `Default` wherever they make sense. Implement `Display` for types users read, and `From` for
    conversions that can't fail.
- Mark public enums and structs that may grow `#[non_exhaustive]`.
- In the **Public API** crates, each of these breaks callers: removing or renaming a public item;
  adding a variant to an exhaustive enum or a public field to an exhaustive struct; changing a
  function's signature; adding a trait bound; and removing a trait implementation, including
  `Send` or `Sync` lost through a new field. Ask before you make one, and mark it as `commits.md`
  requires.

## Documentation

Three kinds of docs describe a crate, and each has its own reader:

- Doc comments (`///`) build each item's page on docs.rs.
- The crate docs (`//!` at the top of `src/lib.rs`) build the crate's front page on docs.rs.
- The crate's `README.md` is its page on crates.io.

Keep all three true. A change to behavior, a signature, an error, a panic, or a feature updates
every one of them that describes it, in the same change.

### Doc comments

- Give every public item a doc comment, and turn on the `missing_docs` lint so a missing one
  fails the checks. Start with a one-sentence summary, because docs.rs shows that sentence alone
  in module listings and search results.
- Give each module a `//!` comment that says what it holds and why it is a module of its own.
- Add each section that applies:
  - `# Errors`: each way the function can fail, by error variant.
  - `# Panics`: each condition that makes it panic.
  - `# Safety`: what the caller of an `unsafe fn` must uphold.
  - `# Examples`: code that compiles and runs as a doc test.
- State only what the code does. A claim about cost ("O(1)", "doesn't allocate"), ordering,
  thread safety, or a limit must match the code, so check it there or in a test before you write
  it. Describe planned behavior as planned, never in the present tense.
- Write examples the way a user would write them:
  - Use `?`, not `unwrap`. Wrap the example in a hidden
    `fn main() -> Result<(), Box<dyn std::error::Error>>`; lines that start with `# ` compile
    but don't appear on the page.
  - Mark an example `no_run` when it compiles but can't run in a test, for example because it
    opens a window or a network connection. Mark it `compile_fail` when it shows misuse that the
    compiler rejects.
  - Use `ignore` only with a comment that says why. An ignored example isn't even compiled, so
    it goes stale without anyone noticing.
- Explain a term once, in the module docs or on the item that defines it. A reader lands on one
  item's page, not at the top of the crate, so link to that explanation from everywhere else
  with an intra-doc link such as ``[`Parser`]`` or ``[`Parser::parse`]``.
- Link only to items that docs.rs shows. A link from a public item's docs to a private item
  appears broken there, and the `rustdoc::private_intra_doc_links` lint warns about it.
- Say which Cargo feature or target an item needs when it isn't always present.
- Write code comments (`//`) that explain why, not what, and keep them true when the code
  changes. Delete commented-out code instead of leaving it.

### Crate docs

The crate docs are the front page of the crate's API reference. Write them in this order:

1. What the crate is, in one or two sentences.
2. Where it fits: what it depends on, what uses it, and how a user reaches it, for example
   through another crate that re-exports it. Link to its write-up in the **Design docs**, if
   there is one.
3. `# The parts`: the main items, grouped by purpose, each named with an intra-doc link.
4. `# Example`: a doc test that shows typical use from start to finish.
5. What a caller must know to use the crate well: its invariants, panics, limits, and thread
   safety.

Keep the crate docs and the README as separate texts instead of making the README the crate
docs with `#![doc = include_str!("../README.md")]`. The crate docs depend on intra-doc links,
and crates.io doesn't resolve them.

### Crate READMEs

Give each of the **Published crates** a `README.md` of its own in its folder, and set
`readme = "README.md"` in its manifest. Its crates.io page is often the first thing a new user
reads, so write it for someone deciding whether to use the crate, in this shape:

1. `# <crate-name>`, then one short paragraph on what the crate is and does.
2. How it fits: the project it belongs to, and how users reach it (directly, or through another
   crate), with a link to the project.
3. `## Status`: the version and how stable the API is, for example "Version 0.4: the API will
   change before 1.0." Then, kept apart, what is built today, what is left out by design, and
   what is planned.
4. `## Usage`: the `[dependencies]` line, then examples, then what a user must know to avoid
   errors and panics.
5. `## Features`, only when the crate has Cargo features: each feature, whether it is on by
   default, and what it adds.
6. `## Platforms`, only when behavior differs between operating systems or targets.
7. `## Documentation`: links to the API reference on docs.rs, the crate's write-up in the
   **Design docs**, and the project.
8. `## License`: the license, with links to the license files in the crate's folder.

- Test the README's examples. Add this item to `src/lib.rs`, so that `cargo test` compiles and
  runs every Rust block in the README as a doc test:

  ```rust
  // The README's examples are compiled as doc tests too.
  #[cfg(doctest)]
  #[doc = include_str!("../README.md")]
  struct ReadmeDoctests;
  ```

- Tag every code block in the README with its language. Rustdoc compiles an untagged block as
  Rust, so a shell command without a tag fails the doc tests. Use `toml` for the dependency line,
  and `bash` or `text` for commands and output.
- Use absolute URLs for links that leave the crate's folder. The published package holds only
  that folder, and sites that republish the README resolve relative links differently, or not at
  all.
- Reread the README whenever the crate's behavior or API changes. A doc test catches an example
  that stops compiling, but nothing catches a sentence that stops being true.

### docs.rs and crates.io metadata

For each of the **Published crates**:

- Fill in the manifest fields that crates.io shows, and inherit the shared ones from
  `[workspace.package]`:
  - `description`: one plain-text sentence, which crates.io search results show.
  - `license`: a Software Package Data Exchange (SPDX) expression, such as
    `MIT OR Apache-2.0`.
  - `repository`, `readme`, at most five `keywords`, and `categories` taken from crates.io's
    list of category slugs.
- Make docs.rs document everything a user can turn on, in `[package.metadata.docs.rs]`:
  - List the optional features (`features = [...]`, or `all-features = true`).
  - List `targets` for a crate whose API differs by target. docs.rs builds only its default
    target, `x86_64-unknown-linux-gnu`, unless the manifest lists others, so items for other
    targets are missing.
  - Add a comment above the table that says why it is there.

## Tests

- Follow the **Test layout**: unit tests in a `#[cfg(test)] mod tests` beside the code, and
  integration tests that use only the public API.
- Give every behavior change a test. Start every bug fix with a test that fails before the fix.
- Name each test for the behavior it checks: `rejects_length_past_end_of_input`, not `test3`.
- Keep tests deterministic: seed every random number generator, don't depend on the clock, the
  network, or `HashMap` order, and give each test its own temporary directory.
- Test the edges: empty, one, the maximum, one past the maximum, and malformed. For a parser,
  include truncated and oversized input.
- Assert on error variants or codes, not on message text, so rewording a message doesn't break
  a test.
- Read a changed snapshot's diff before you accept it.

## Lints, formatting, and the toolchain

- Format with `cargo fmt`, and don't hand-format against it.
- Set lints only in the **Lint configuration**, and make every member crate inherit them with
  `[lints] workspace = true`. Never weaken a lint, a test, or a check to get something passing;
  fix the cause.
- When a lint is wrong in one place, silence it on the smallest item with
  `#[expect(clippy::lint_name, reason = "…")]`, not `#[allow]`. An `#[expect]` warns once the
  lint stops firing, so the exception can't outlive its reason. It needs Rust 1.81 or later.
- Don't leave `dbg!`, `todo!`, `unimplemented!`, or debugging `println!` calls behind.
- Keep the **Toolchain**. Don't change the edition, the pinned toolchain, or the MSRV as a side
  effect of another task; a bump is a change of its own. Before you use a recently stabilized
  `std` API, check that the MSRV has it.

## Dependencies and manifests

- Follow the **Dependency policy**. Never add a crate, a feature of an existing crate, or a dev
  dependency without asking. When you propose one, say what it does that `std` or existing code
  can't, and give its license, how well it is maintained, how many crates it pulls in, and
  whether it uses `unsafe`.
- Declare shared dependencies once, in `[workspace.dependencies]`, and refer to them with
  `workspace = true`.
- Keep Cargo features additive. Cargo merges the features every crate in the build asks for, so a
  feature that removes or changes behavior breaks other crates that depend on the same crate.
- Run builds and checks with `--locked`, so a stale `Cargo.lock` fails instead of being
  rewritten.
- Keep `build.rs` to work that must happen at build time, with no network access, and emit
  `rerun-if-changed` directives so it doesn't rerun on every build.

## Before finishing

1. Run every **Check commands** command and read its output. Fix every warning, not only the
   errors.
2. If you changed public items, doc comments, crate docs, or a README:
   1. Build the docs with warnings as errors, so that a broken intra-doc link fails:
      `RUSTDOCFLAGS="-D warnings" cargo doc --no-deps`. Add `--features …` or `--all-features`
      to match `[package.metadata.docs.rs]`.
   2. Run `cargo test --doc`. It runs every doc example and, through `ReadmeDoctests`, every
      README example.
   3. Reread the changed prose against the code.
3. If you changed what a published crate ships (its manifest, README, or license files), run
   `cargo package --list -p <crate>`. Check that the README and license files are listed, and
   that nothing private or generated is.
4. If you changed `unsafe` code, run the tests under Miri where it supports the code.
5. Review your diff as a reviewer would. Look for `unwrap`, unchecked arithmetic, missing docs or
   tests, and leftover `dbg!`, `todo!`, or commented-out code. Then report what you ran, what you
   didn't, and what remains.
