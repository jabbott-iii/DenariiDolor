/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'dart:async';

import 'package:denarii_dolor/app/app_scope.dart';
import 'package:denarii_dolor/features/session/state/signed_in_placeholder_view_model.dart';
import 'package:denarii_dolor/features/session/ui/signed_in_placeholder_screen.dart';
import 'package:flutter/widgets.dart';

/// Creates the [SignedInPlaceholderViewModel], gives its state to the screen, and disposes it with
/// itself (ADR-0010).
class SignedInPlaceholderRoute extends StatefulWidget {
  const SignedInPlaceholderRoute({super.key});

  @override
  State<SignedInPlaceholderRoute> createState() =>
      _SignedInPlaceholderRouteState();
}

class _SignedInPlaceholderRouteState extends State<SignedInPlaceholderRoute> {
  late final SignedInPlaceholderViewModel _viewModel;

  @override
  void initState() {
    super.initState();
    _viewModel = SignedInPlaceholderViewModel(
      sessionLock: AppScope.read(context).sessionLock,
    );
  }

  @override
  void dispose() {
    _viewModel.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: _viewModel,
      builder: (context, _) => SignedInPlaceholderScreen(
        state: _viewModel.state,
        onSignOut: () => unawaited(_viewModel.lock()),
      ),
    );
  }
}
