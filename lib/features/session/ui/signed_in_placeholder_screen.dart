/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/features/session/state/signed_in_placeholder_view_model.dart';
import 'package:denarii_dolor/l10n/app_localizations.dart';
import 'package:material_ui/material_ui.dart';

/// Stands in for the signed-in screens until M4: the app's name and a sign-out button.
class SignedInPlaceholderScreen extends StatelessWidget {
  const SignedInPlaceholderScreen({
    super.key,
    required this.state,
    required this.onSignOut,
  });

  final SignedInPlaceholderState state;
  final VoidCallback onSignOut;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.appName),
        actions: [
          IconButton(
            tooltip: l10n.signOut,
            icon: const Icon(Icons.logout),
            onPressed: state.isLocking ? null : onSignOut,
          ),
        ],
      ),
    );
  }
}
