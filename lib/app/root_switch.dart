/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/app_scope.dart';
import 'package:denarii_dolor/app/signed_in_tree.dart';
import 'package:denarii_dolor/features/auth/ui/sign_in_placeholder_screen.dart';
import 'package:flutter/widgets.dart';

/// Shows the sign-in flow while the vault is locked, and the signed-in tree while it's unlocked
/// (ADR-0011).
///
/// Switching replaces the whole subtree, so no signed-in screen or view model outlives the session
/// (CS-02). M3.5 puts the sign-in screens it pushes on a `Navigator` inside the sign-in flow, so that
/// unlocking replaces them too.
class RootSwitch extends StatelessWidget {
  const RootSwitch({super.key});

  @override
  Widget build(BuildContext context) {
    final sessionLock = AppScope.of(context).sessionLock;
    return ListenableBuilder(
      listenable: sessionLock,
      builder: (context, _) => sessionLock.isUnlocked
          ? const SignedInTree()
          : const SignInPlaceholderScreen(),
    );
  }
}
