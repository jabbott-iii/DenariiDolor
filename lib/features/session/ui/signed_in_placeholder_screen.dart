/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/app_name.dart';
import 'package:denarii_dolor/features/session/state/signed_in_placeholder_view_model.dart';
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
    return Scaffold(
      appBar: AppBar(
        title: const Text(appName),
        actions: [
          IconButton(
            // v1's English `sign_out` string, until M1.4 moves it to the ARB files.
            tooltip: 'Sign Out',
            icon: const Icon(Icons.logout),
            onPressed: state.isLocking ? null : onSignOut,
          ),
        ],
      ),
    );
  }
}
