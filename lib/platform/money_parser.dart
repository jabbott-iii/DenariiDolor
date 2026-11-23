/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/platform/rust/api/money.dart' as rust;
import 'package:flutter/foundation.dart';

export 'package:denarii_dolor/platform/rust/api/money.dart'
    show AmountError, NumberSymbols;

/// What [MoneyParser.parseCents] made of a typed amount: its cents, or why it was rejected.
@immutable
sealed class ParsedAmount {
  const ParsedAmount();
}

/// An amount the Rust core accepted, as a whole number of cents.
final class AmountInCents extends ParsedAmount {
  const AmountInCents(this.cents);

  final int cents;

  @override
  bool operator ==(Object other) =>
      other is AmountInCents && other.cents == cents;

  @override
  int get hashCode => cents.hashCode;
}

/// An amount the Rust core rejected, with the reason it gave.
final class RejectedAmount extends ParsedAmount {
  const RejectedAmount(this.error);

  final rust.AmountError error;

  @override
  bool operator ==(Object other) =>
      other is RejectedAmount && other.error == error;

  @override
  int get hashCode => error.hashCode;
}

/// Reads an amount the user typed with v1's rules, which live in the Rust core (ADR-0004).
abstract interface class MoneyParser {
  /// Parses [text] with the locale's [symbols]. A rejected amount is a [RejectedAmount], not an
  /// exception.
  Future<ParsedAmount> parseCents(String text, rust.NumberSymbols symbols);
}

/// The [MoneyParser] over the Rust bridge (ADR-0005). `RustLib.init` must have completed first,
/// as `main` does before the app starts.
final class RustMoneyParser implements MoneyParser {
  const RustMoneyParser();

  @override
  Future<ParsedAmount> parseCents(
    String text,
    rust.NumberSymbols symbols,
  ) async {
    try {
      return AmountInCents(await rust.parseCents(text: text, symbols: symbols));
    } on rust.AmountError catch (error) {
      return RejectedAmount(error);
    }
  }
}
