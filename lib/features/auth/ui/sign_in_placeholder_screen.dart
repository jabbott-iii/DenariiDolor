/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/app_name.dart';
import 'package:denarii_dolor/features/auth/state/sign_in_placeholder_view_model.dart';
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
                  child: Text(appName, style: textTheme.headlineMedium),
                ),
                const SizedBox(height: 16),
                TextField(
                  // English until M1.4 moves the placeholder's text to the ARB files.
                  decoration: const InputDecoration(labelText: 'Amount'),
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
                    _resultText(state.parsed),
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

  /// The cents Rust returned, or the name of its error; English until M1.4.
  static String _resultText(ParsedAmount? parsed) => switch (parsed) {
    null => '',
    AmountInCents(:final cents) => '$cents cents',
    RejectedAmount(:final error) => 'Rejected: ${error.name}',
  };
}
