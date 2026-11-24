/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/app.dart';
import 'package:denarii_dolor/app/app_scope.dart';
import 'package:denarii_dolor/app/session_lock.dart';
import 'package:denarii_dolor/platform/money_parser.dart';
import 'package:denarii_dolor/platform/rust/frb_generated.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // The app draws under the system bars, as v1's `enableEdgeToEdge()` made it. Flutter 3.50.1 does
  // this only from Android 10, and Android 15 and later always do it.
  await SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
  // Loads libdenarii_bridge before any screen can call it (ADR-0005).
  await RustLib.init();
  runApp(
    DenariiDolorApp(
      services: AppServices(
        sessionLock: LockedSessionLock(),
        moneyParser: const RustMoneyParser(),
      ),
    ),
  );
}
