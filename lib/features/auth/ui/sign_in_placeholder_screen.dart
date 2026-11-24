/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/features/auth/state/sign_in_placeholder_view_model.dart';
import 'package:denarii_dolor/l10n/app_localizations.dart';
import 'package:denarii_dolor/platform/money_parser.dart';
import 'package:material_ui/material_ui.dart';

/// Stands in for the sign-in flow until M3.5. Nothing here unlocks the vault, so until then the
/// app can't reach the signed-in tree.
///
/// Its amount field shows that the bridge works (M1.2): the line under it shows the cents that the
/// Rust core returned for the typed text, or why it rejected it.
class SignInPlaceholderScreen extends StatelessWidget {
  const SignInPlaceholderScreen({
    super.key,
    required this.state,
    required this.onAmountChanged,
  });

  final SignInPlaceholderState state;
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
                Semantics(
                  liveRegion: true,
                  child: Text(
                    _resultText(l10n, state.parsed),
                    style: textTheme.bodyLarge,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// The cents Rust returned, or the code name of its error.
  static String _resultText(AppLocalizations l10n, ParsedAmount? parsed) =>
      switch (parsed) {
        null => '',
        AmountInCents(:final cents) => l10n.placeholderAmountInCents(cents),
        RejectedAmount(:final error) => l10n.placeholderAmountRejected(
          error.name,
        ),
      };
}
