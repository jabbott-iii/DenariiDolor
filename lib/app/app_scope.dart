/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/session_lock.dart';
import 'package:denarii_dolor/platform/money_parser.dart';
import 'package:flutter/widgets.dart';

/// The services that live as long as the app (ADR-0010). Later tasks add the vault's sign-in calls,
/// the platform channels, and the settings.
@immutable
final class AppServices {
  const AppServices({required this.sessionLock, required this.moneyParser});

  final SessionLock sessionLock;

  /// Reads typed amounts through the Rust core. It touches no stored data, so it may live outside
  /// the session.
  final MoneyParser moneyParser;
}

/// Gives the widgets below it the [AppServices]. Route widgets pass them to their view models'
/// constructors (ADR-0010).
class AppScope extends InheritedWidget {
  const AppScope({super.key, required this.services, required super.child});

  final AppServices services;

  /// The services, and a dependency that rebuilds [context] if they're replaced.
  static AppServices of(BuildContext context) =>
      _servicesOf(context.dependOnInheritedWidgetOfExactType<AppScope>());

  /// The services with no dependency, for `State.initState`. They aren't replaced while the app
  /// runs, so a view model may keep them.
  static AppServices read(BuildContext context) =>
      _servicesOf(context.getInheritedWidgetOfExactType<AppScope>());

  static AppServices _servicesOf(AppScope? scope) {
    if (scope == null) {
      throw StateError('No AppScope above this widget.');
    }
    return scope.services;
  }

  @override
  bool updateShouldNotify(AppScope oldWidget) =>
      !identical(services, oldWidget.services);
}
