/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/app_name.dart';
import 'package:material_ui/material_ui.dart';

/// Stands in for the sign-in flow until M3.5. Nothing here unlocks the vault, so until then the
/// app can't reach the signed-in tree.
class SignInPlaceholderScreen extends StatelessWidget {
  const SignInPlaceholderScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: Semantics(
            header: true,
            child: Text(appName, style: TextTheme.of(context).headlineMedium),
          ),
        ),
      ),
    );
  }
}
