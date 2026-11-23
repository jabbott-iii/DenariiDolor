/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/platform/money_parser.dart';
import 'package:flutter/foundation.dart';

/// What the sign-in placeholder shows.
@immutable
final class SignInPlaceholderState {
  const SignInPlaceholderState({this.parsed});

  /// What the Rust core made of the amount typed last, or null before anything is typed.
  final ParsedAmount? parsed;

  @override
  bool operator ==(Object other) =>
      other is SignInPlaceholderState && other.parsed == parsed;

  @override
  int get hashCode => parsed.hashCode;
}

/// The view model of the sign-in placeholder, which M3.5 replaces with the real sign-in flow. Until
/// then it shows that the bridge works: it sends each typed amount to the Rust core and keeps the
/// cents that come back (M1.2).
final class SignInPlaceholderViewModel extends ChangeNotifier {
  SignInPlaceholderViewModel({required this._moneyParser});

  /// The symbols of US English with no currency, until M1.6 fills them from the user's locale with
  /// `package:intl`.
  static const NumberSymbols placeholderSymbols = NumberSymbols(
    decimalSeparator: '.',
    currencyMarks: <String>[],
  );

  final MoneyParser _moneyParser;
  SignInPlaceholderState _state = const SignInPlaceholderState();
  int _latestRequest = 0;
  bool _isDisposed = false;

  SignInPlaceholderState get state => _state;

  /// Parses [text] in Rust. Calls can finish out of order, so only the latest call's result is
  /// kept.
  Future<void> onAmountChanged(String text) async {
    final request = ++_latestRequest;
    final parsed = await _moneyParser.parseCents(text, placeholderSymbols);
    if (request != _latestRequest) {
      return;
    }
    _update(SignInPlaceholderState(parsed: parsed));
  }

  void _update(SignInPlaceholderState next) {
    if (_isDisposed || next == _state) {
      return;
    }
    _state = next;
    notifyListeners();
  }

  @override
  void dispose() {
    _isDisposed = true;
    super.dispose();
  }
}
