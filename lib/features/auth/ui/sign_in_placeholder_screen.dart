/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/features/auth/state/sign_in_placeholder_view_model.dart';
import 'package:denarii_dolor/l10n/app_localizations.dart';
import 'package:denarii_dolor/l10n/money_format.dart';
import 'package:denarii_dolor/platform/money_parser.dart';
import 'package:material_ui/material_ui.dart';

/// Stands in for the sign-in flow until M3.5. Nothing here unlocks the vault, so until then the
/// app can't reach the signed-in tree.
///
/// Its amount field shows that the bridge works (M1.2): under it are the cents that the Rust core
/// returned for the typed text, written as money in [currencyCode] and as a number (M1.6), or why
/// the core rejected the text.
class SignInPlaceholderScreen extends StatelessWidget {
  const SignInPlaceholderScreen({
    super.key,
    required this.state,
    required this.currencyCode,
    required this.onAmountChanged,
  });

  final SignInPlaceholderState state;

  /// The ISO 4217 code of the currency the amount is written in.
  final String currencyCode;

  final ValueChanged<String> onAmountChanged;

  @override
  Widget build(BuildContext context) {
    final textTheme = TextTheme.of(context);
    final l10n = AppLocalizations.of(context);
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsetsDirectional.all(16),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Semantics(
                  header: true,
                  child: Text(l10n.appName, style: textTheme.headlineMedium),
                ),
                const SizedBox(height: 16),
                TextField(
                  decoration: InputDecoration(
                    labelText: l10n.transactionAmountHint,
                  ),
                  keyboardType: const TextInputType.numberWithOptions(
                    signed: true,
                    decimal: true,
                  ),
                  onChanged: onAmountChanged,
                ),
                const SizedBox(height: 16),
                // One live region for the result's lines, so a screen reader reads them together.
                MergeSemantics(
                  child: Semantics(
                    liveRegion: true,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        for (final line in _resultLines(
                          l10n,
                          Localizations.localeOf(context),
                        ))
                          Text(line, style: textTheme.bodyLarge),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// The cents Rust returned, as money and as a number, or the code name of its error.
  List<String> _resultLines(AppLocalizations l10n, Locale locale) =>
      switch (state.parsed) {
        null => const [],
        AmountInCents(:final cents) => [
          formatMoney(cents, currencyCode: currencyCode, locale: locale),
          l10n.placeholderAmountInCents(cents),
        ],
        RejectedAmount(:final error) => [
          l10n.placeholderAmountRejected(error.name),
        ],
      };
}
