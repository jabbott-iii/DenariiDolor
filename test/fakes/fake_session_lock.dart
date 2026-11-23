/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'dart:async';

import 'package:denarii_dolor/app/session_lock.dart';
import 'package:flutter/foundation.dart';

/// A [SessionLock] that a test unlocks, holds, and fails by hand.
final class FakeSessionLock extends ChangeNotifier implements SessionLock {
  FakeSessionLock({this._isUnlocked = false});

  bool _isUnlocked;

  /// How many times [lock] was called.
  int lockCalls = 0;

  /// When set, [lock] waits for it to complete before locking.
  Completer<void>? pendingLock;

  /// When set, [lock] throws it instead of locking.
  Exception? failure;

  @override
  bool get isUnlocked => _isUnlocked;

  void unlock() {
    _isUnlocked = true;
    notifyListeners();
  }

  @override
  Future<void> lock() async {
    lockCalls += 1;
    final pending = pendingLock;
    if (pending != null) {
      await pending.future;
    }
    final failure = this.failure;
    if (failure != null) {
      throw failure;
    }
    _isUnlocked = false;
    notifyListeners();
  }
}
