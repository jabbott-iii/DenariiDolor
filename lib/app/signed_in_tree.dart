/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'dart:async';

import 'package:denarii_dolor/features/session/ui/signed_in_placeholder_route.dart';
import 'package:material_ui/material_ui.dart';

/// Every screen a signed-in user can reach, under a `Navigator` of its own (ADR-0011), so that
/// locking removes them all at once. M3.5 puts it inside the session scope (ADR-0010).
class SignedInTree extends StatefulWidget {
  const SignedInTree({super.key});

  @override
  State<SignedInTree> createState() => _SignedInTreeState();
}

class _SignedInTreeState extends State<SignedInTree> {
  // The pop handler needs this Navigator's state to pass it the system Back gesture.
  final GlobalKey<NavigatorState> _navigatorKey = GlobalKey<NavigatorState>();

  @override
  Widget build(BuildContext context) {
    return NavigatorPopHandler<Object?>(
      onPopWithResult: (_) {
        unawaited(_navigatorKey.currentState?.maybePop());
      },
      child: Navigator(
        key: _navigatorKey,
        onGenerateInitialRoutes: (_, _) => [
          MaterialPageRoute<void>(
            builder: (_) => const SignedInPlaceholderRoute(),
          ),
        ],
      ),
    );
  }
}
