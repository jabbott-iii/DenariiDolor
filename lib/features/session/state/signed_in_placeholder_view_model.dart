/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/session_lock.dart';
import 'package:flutter/foundation.dart';

/// What the placeholder signed-in screen shows.
@immutable
final class SignedInPlaceholderState {
  const SignedInPlaceholderState({this.isLocking = false});

  /// A lock is in flight, so the sign-out button is disabled.
  final bool isLocking;

  SignedInPlaceholderState copyWith({bool? isLocking}) =>
      SignedInPlaceholderState(isLocking: isLocking ?? this.isLocking);

  @override
  bool operator ==(Object other) =>
      other is SignedInPlaceholderState && other.isLocking == isLocking;

  @override
  int get hashCode => isLocking.hashCode;
}

/// The view model of the placeholder signed-in screen, which M4 replaces. It has the shape every
/// view model follows (ADR-0010): immutable state, services through the constructor, and one action
/// at a time.
final class SignedInPlaceholderViewModel extends ChangeNotifier {
  SignedInPlaceholderViewModel({required this._sessionLock});

  final SessionLock _sessionLock;
  SignedInPlaceholderState _state = const SignedInPlaceholderState();
  bool _isDisposed = false;

  SignedInPlaceholderState get state => _state;

  /// Signs out by locking the vault. A call while a lock is in flight does nothing.
  Future<void> lock() async {
    if (_state.isLocking) {
      return;
    }
    _update(_state.copyWith(isLocking: true));
    try {
      await _sessionLock.lock();
    } finally {
      // Locking disposes the signed-in tree, and this view model with it, possibly before this runs.
      _update(_state.copyWith(isLocking: false));
    }
  }

  void _update(SignedInPlaceholderState next) {
    if (_isDisposed || next == _state) {
      return;
    }
    _state = next;
    notifyListeners();
  }

  @override
  void dispose() {
    _isDisposed = true;
    super.dispose();
  }
}
