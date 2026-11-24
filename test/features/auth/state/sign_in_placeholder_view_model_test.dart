/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'dart:async';

import 'package:denarii_dolor/features/auth/state/sign_in_placeholder_view_model.dart';
import 'package:denarii_dolor/platform/money_parser.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../../fakes/fake_money_parser.dart';

void main() {
  late FakeMoneyParser moneyParser;
  late SignInPlaceholderViewModel viewModel;

  setUp(() {
    moneyParser = FakeMoneyParser();
    viewModel = SignInPlaceholderViewModel(moneyParser: moneyParser);
  });

  test('starts with nothing parsed', () {
    expect(viewModel.state, const SignInPlaceholderState());
    expect(viewModel.state.parsed, isNull);
    viewModel.dispose();
  });

  test('shows the cents the parser returned', () async {
    moneyParser.answers['1,234.56'] = const AmountInCents(123456);

    await viewModel.onAmountChanged('1,234.56', _symbols);

    expect(viewModel.state.parsed, const AmountInCents(123456));
    viewModel.dispose();
  });

  test('shows why the parser rejected an amount', () async {
    moneyParser.answers['1.005'] = const RejectedAmount(
      AmountError.tooManyDecimals,
    );

    await viewModel.onAmountChanged('1.005', _symbols);

    expect(
      viewModel.state.parsed,
      const RejectedAmount(AmountError.tooManyDecimals),
    );
    viewModel.dispose();
  });

  test('sends the symbols it is given with the typed text', () async {
    await viewModel.onAmountChanged('12', _symbols);

    expect(moneyParser.calls, [('12', _symbols)]);
    viewModel.dispose();
  });

  test('keeps only the latest text when results arrive out of order', () async {
    final slow = Completer<void>();
    moneyParser.pending['1'] = slow;
    moneyParser.answers['1'] = const AmountInCents(100);
    moneyParser.answers['12'] = const AmountInCents(1200);
    final seen = <ParsedAmount?>[];
    viewModel.addListener(() => seen.add(viewModel.state.parsed));

    final first = viewModel.onAmountChanged('1', _symbols);
    await viewModel.onAmountChanged('12', _symbols);
    slow.complete();
    await first;

    expect(viewModel.state.parsed, const AmountInCents(1200));
    expect(seen, [const AmountInCents(1200)]);
    viewModel.dispose();
  });

  test('a result that arrives after dispose notifies no one', () async {
    final slow = Completer<void>();
    moneyParser.pending['5'] = slow;
    var notifications = 0;
    viewModel.addListener(() => notifications += 1);

    final parse = viewModel.onAmountChanged('5', _symbols);
    viewModel.dispose();
    slow.complete();
    await parse;

    expect(notifications, 0);
  });
}

/// US English's symbols for US dollars, as `moneyParsingSymbols` gives them.
const NumberSymbols _symbols = NumberSymbols(
  decimalSeparator: '.',
  currencyMarks: ['\$', 'USD'],
);
