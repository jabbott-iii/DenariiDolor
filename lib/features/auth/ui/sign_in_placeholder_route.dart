/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'dart:async';

import 'package:denarii_dolor/app/app_scope.dart';
import 'package:denarii_dolor/features/auth/state/sign_in_placeholder_view_model.dart';
import 'package:denarii_dolor/features/auth/ui/sign_in_placeholder_screen.dart';
import 'package:flutter/widgets.dart';

/// Creates the [SignInPlaceholderViewModel], gives its state to the screen, and disposes it with
/// itself (ADR-0010).
class SignInPlaceholderRoute extends StatefulWidget {
  const SignInPlaceholderRoute({super.key});

  @override
  State<SignInPlaceholderRoute> createState() => _SignInPlaceholderRouteState();
}

class _SignInPlaceholderRouteState extends State<SignInPlaceholderRoute> {
  late final SignInPlaceholderViewModel _viewModel;

  @override
  void initState() {
    super.initState();
    _viewModel = SignInPlaceholderViewModel(
      moneyParser: AppScope.read(context).moneyParser,
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
      builder: (context, _) => SignInPlaceholderScreen(
        state: _viewModel.state,
        onAmountChanged: (text) => unawaited(_viewModel.onAmountChanged(text)),
      ),
    );
  }
}
