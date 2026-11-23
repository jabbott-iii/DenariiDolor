/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'dart:async';

import 'package:denarii_dolor/features/session/state/signed_in_placeholder_view_model.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../../fakes/fake_session_lock.dart';

void main() {
  late FakeSessionLock sessionLock;
  late SignedInPlaceholderViewModel viewModel;

  setUp(() {
    sessionLock = FakeSessionLock(isUnlocked: true);
    viewModel = SignedInPlaceholderViewModel(sessionLock: sessionLock);
  });

  test('starts with no lock in flight', () {
    expect(viewModel.state, const SignedInPlaceholderState());
    expect(viewModel.state.isLocking, isFalse);
    viewModel.dispose();
  });

  test('lock locks the session', () async {
    await viewModel.lock();

    expect(sessionLock.lockCalls, 1);
    expect(sessionLock.isUnlocked, isFalse);
    expect(viewModel.state.isLocking, isFalse);
    viewModel.dispose();
  });

  test('isLocking is true while a lock is in flight', () async {
    final pending = Completer<void>();
    sessionLock.pendingLock = pending;
    final seen = <bool>[];
    viewModel.addListener(() => seen.add(viewModel.state.isLocking));

    final lock = viewModel.lock();
    expect(viewModel.state.isLocking, isTrue);
    pending.complete();
    await lock;

    expect(seen, [true, false]);
    viewModel.dispose();
  });

  test('a second lock while one is in flight is ignored', () async {
    final pending = Completer<void>();
    sessionLock.pendingLock = pending;

    final first = viewModel.lock();
    final second = viewModel.lock();
    pending.complete();
    await Future.wait([first, second]);

    expect(sessionLock.lockCalls, 1);
    viewModel.dispose();
  });

  test('a failed lock clears isLocking and passes the error on', () async {
    sessionLock.failure = Exception('lock failed');

    await expectLater(viewModel.lock(), throwsException);

    expect(viewModel.state.isLocking, isFalse);
    expect(sessionLock.isUnlocked, isTrue);
    viewModel.dispose();
  });

  test('a lock that ends after dispose notifies no one', () async {
    final pending = Completer<void>();
    sessionLock.pendingLock = pending;
    var notifications = 0;
    viewModel.addListener(() => notifications += 1);

    final lock = viewModel.lock();
    expect(notifications, 1);
    viewModel.dispose();
    pending.complete();
    await lock;

    expect(notifications, 1);
    expect(sessionLock.isUnlocked, isFalse);
  });
}
