/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'dart:async';

import 'package:denarii_dolor/app/app.dart';
import 'package:denarii_dolor/app/app_scope.dart';
import 'package:denarii_dolor/app/session_lock.dart';
import 'package:denarii_dolor/app/signed_in_tree.dart';
import 'package:denarii_dolor/features/auth/ui/sign_in_placeholder_screen.dart';
import 'package:denarii_dolor/features/session/ui/signed_in_placeholder_screen.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:material_ui/material_ui.dart';

import '../fakes/fake_money_parser.dart';
import '../fakes/fake_session_lock.dart';

void main() {
  Future<void> pumpApp(WidgetTester tester, SessionLock sessionLock) async {
    await tester.pumpWidget(
      DenariiDolorApp(
        services: AppServices(
          sessionLock: sessionLock,
          moneyParser: FakeMoneyParser(),
        ),
      ),
    );
  }

  /// Pushes a [_DisposeProbe] on the signed-in tree's Navigator, as a later screen would be.
  Future<_DisposeProbeRecord> pushProbe(WidgetTester tester) async {
    final record = _DisposeProbeRecord();
    final navigator = Navigator.of(
      tester.element(find.byType(SignedInPlaceholderScreen)),
    );
    unawaited(
      navigator.push(
        MaterialPageRoute<void>(builder: (_) => _DisposeProbe(record)),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byType(_DisposeProbe), findsOneWidget);
    return record;
  }

  testWidgets('the app starts on the sign-in placeholder, locked', (
    tester,
  ) async {
    final sessionLock = LockedSessionLock();
    await pumpApp(tester, sessionLock);

    expect(find.byType(SignInPlaceholderScreen), findsOneWidget);
    expect(find.text('Denarii Dolor'), findsOneWidget);
    expect(find.byType(SignedInTree), findsNothing);

    await sessionLock.lock();
    await tester.pump();
    expect(sessionLock.isUnlocked, isFalse);
    expect(find.byType(SignedInTree), findsNothing);
  });

  testWidgets('unlocking shows the signed-in tree in place of sign-in', (
    tester,
  ) async {
    final sessionLock = FakeSessionLock();
    await pumpApp(tester, sessionLock);

    sessionLock.unlock();
    await tester.pumpAndSettle();

    expect(find.byType(SignedInPlaceholderScreen), findsOneWidget);
    expect(find.byType(SignInPlaceholderScreen), findsNothing);
  });

  testWidgets('locking disposes every screen in the signed-in tree', (
    tester,
  ) async {
    final sessionLock = FakeSessionLock(isUnlocked: true);
    await pumpApp(tester, sessionLock);
    final probe = await pushProbe(tester);

    await sessionLock.lock();
    await tester.pumpAndSettle();

    expect(probe.isDisposed, isTrue);
    expect(find.byType(SignedInTree), findsNothing);
    expect(find.byType(_DisposeProbe), findsNothing);
    expect(find.byType(SignInPlaceholderScreen), findsOneWidget);
  });

  testWidgets('sign-out locks once and is disabled while locking', (
    tester,
  ) async {
    final sessionLock = FakeSessionLock(isUnlocked: true);
    final pending = Completer<void>();
    sessionLock.pendingLock = pending;
    await pumpApp(tester, sessionLock);

    await tester.tap(find.byTooltip('Sign Out'));
    await tester.pump();
    final button = tester.widget<IconButton>(find.byType(IconButton));
    expect(button.onPressed, isNull);
    await tester.tap(find.byTooltip('Sign Out'), warnIfMissed: false);
    await tester.pump();
    expect(sessionLock.lockCalls, 1);

    pending.complete();
    await tester.pumpAndSettle();
    expect(find.byType(SignInPlaceholderScreen), findsOneWidget);
  });

  testWidgets('Back closes a pushed signed-in screen and stays signed in', (
    tester,
  ) async {
    final sessionLock = FakeSessionLock(isUnlocked: true);
    await pumpApp(tester, sessionLock);
    final probe = await pushProbe(tester);

    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();

    expect(probe.isDisposed, isTrue);
    expect(find.byType(SignedInPlaceholderScreen), findsOneWidget);
    expect(sessionLock.isUnlocked, isTrue);
  });

  for (final brightness in Brightness.values) {
    testWidgets('both placeholders meet the accessibility guidelines, '
        '${brightness.name} theme', (tester) async {
      tester.platformDispatcher.platformBrightnessTestValue = brightness;
      addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
      final handle = tester.ensureSemantics();
      final sessionLock = FakeSessionLock();
      await pumpApp(tester, sessionLock);

      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(iOSTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));

      sessionLock.unlock();
      await tester.pumpAndSettle();
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(iOSTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));
      handle.dispose();
    });
  }
}

/// Records whether a [_DisposeProbe] was disposed.
final class _DisposeProbeRecord {
  bool isDisposed = false;
}

/// A screen that records its own disposal.
class _DisposeProbe extends StatefulWidget {
  const _DisposeProbe(this.record);

  final _DisposeProbeRecord record;

  @override
  State<_DisposeProbe> createState() => _DisposeProbeState();
}

class _DisposeProbeState extends State<_DisposeProbe> {
  @override
  void dispose() {
    widget.record.isDisposed = true;
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => const Placeholder();
}
