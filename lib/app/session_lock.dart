/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:flutter/foundation.dart';

/// Whether the vault is unlocked, which decides whether the app shows the sign-in flow or the
/// signed-in tree (ADR-0011).
///
/// M3.5 implements it over the vault, which unlocks on sign-in, and locks on sign-out, on the
/// session timeout (CS-01), and on wipe.
abstract interface class SessionLock implements Listenable {
  /// Whether the vault holds the database key, so the signed-in tree may exist.
  bool get isUnlocked;

  /// Locks the vault. Listeners are notified once [isUnlocked] is false.
  Future<void> lock();
}

/// The lock until the vault exists (M3.5): always locked, with no way to unlock.
final class LockedSessionLock extends ChangeNotifier implements SessionLock {
  @override
  bool get isUnlocked => false;

  @override
  Future<void> lock() async {}
}
