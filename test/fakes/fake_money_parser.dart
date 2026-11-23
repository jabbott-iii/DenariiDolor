/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'dart:async';

import 'package:denarii_dolor/platform/money_parser.dart';

/// A [MoneyParser] whose answers a test sets, and can hold until it completes them.
final class FakeMoneyParser implements MoneyParser {
  /// The answer for each text. A text with no answer is rejected as malformed.
  final Map<String, ParsedAmount> answers = {};

  /// When a text has an entry here, its call waits for the entry to complete.
  final Map<String, Completer<void>> pending = {};

  /// Each call's text and symbols, in order.
  final List<(String, NumberSymbols)> calls = [];

  @override
  Future<ParsedAmount> parseCents(String text, NumberSymbols symbols) async {
    calls.add((text, symbols));
    final hold = pending[text];
    if (hold != null) {
      await hold.future;
    }
    return answers[text] ?? const RejectedAmount(AmountError.malformed);
  }
}
