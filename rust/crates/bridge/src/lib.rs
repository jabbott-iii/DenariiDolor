/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

//! The API the Flutter app of Denarii Dolor calls: thin functions that check what Dart sends, call
//! the domain crates, and turn their errors into values that Dart can match on.
//!
//! `flutter_rust_bridge_codegen` reads the [`api`] module and writes the Dart bindings in
//! `lib/platform/rust/` and this crate's glue code in `src/frb_generated.rs`. Flutter's
//! native-assets build hook (`hook/build.dart`) then compiles the crate into
//! `libdenarii_bridge.so` for each Android ABI (ADR-0005 and ADR-0014: Dart calls Rust through
//! `flutter_rust_bridge`, built by that hook). The crate depends on [`denarii_core`], and no crate
//! depends on it. Its design is in `docs/specs/bridge.md`.
//!
//! # The parts
//!
//! - Money: [`api::money::parse_cents`] reads an amount the user typed, with the locale's
//!   [`api::money::NumberSymbols`], and names a rejected amount with an
//!   [`api::money::AmountError`].
//!
//! # Example
//!
//! ```
//! use denarii_bridge::api::money::{NumberSymbols, parse_cents};
//!
//! # fn main() -> Result<(), Box<dyn std::error::Error>> {
//! let english = NumberSymbols {
//!     decimal_separator: ".".to_owned(),
//!     currency_marks: vec!["$".to_owned(), "$".to_owned(), "USD".to_owned()],
//! };
//! assert_eq!(parse_cents("$1,234.56".to_owned(), english)?, 123_456);
//! # Ok(())
//! # }
//! ```
//!
//! # What callers must know
//!
//! - Every value from Dart is untrusted input. This crate checks it before it reaches the domain
//!   crates, and no function panics on any input.
//! - Dart calls each function asynchronously, and the generated code runs it on a worker thread of
//!   `flutter_rust_bridge`'s thread pool, off Flutter's UI thread. The functions themselves are
//!   synchronous and start no threads.
//! - Errors cross the bridge as enums, never as `Display` text, so the app shows its own localized
//!   message for each variant.

pub mod api;

// The generated glue is the only unsafe code in the workspace (ADR-0005), and it is never edited
// by hand (ADR-0012), so it alone is exempt from these lints (ADR-0014). Its decoders read only
// bytes that the generated Dart code wrote, and the bridge catches a panic in a call and returns
// it to Dart as an exception, so the `unwrap` and `unimplemented!` calls here never end the app.
// `expect` keeps the list honest: a lint that stops firing after regeneration fails the build. The
// codegen formats the file itself, and rustfmt skips it, as ADR-0012 says for generated paths.
#[rustfmt::skip]
#[expect(
    unsafe_code,
    unreachable_pub,
    clippy::as_conversions,
    clippy::cast_lossless,
    clippy::cast_possible_truncation,
    clippy::cast_possible_wrap,
    clippy::cast_sign_loss,
    clippy::semicolon_if_nothing_returned,
    clippy::unimplemented,
    clippy::unreadable_literal,
    clippy::unwrap_used,
    reason = "flutter_rust_bridge's generated glue, which is never edited by hand (ADR-0014)"
)]
// Clippy doesn't check wildcard imports in a test build, so this one is expected outside tests only.
#[cfg_attr(
    not(test),
    expect(
        clippy::wildcard_imports,
        reason = "flutter_rust_bridge's generated glue, which is never edited by hand (ADR-0014)"
    )
)]
mod frb_generated;
