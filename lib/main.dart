/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/app.dart';
import 'package:denarii_dolor/app/app_scope.dart';
import 'package:denarii_dolor/app/session_lock.dart';
import 'package:flutter/widgets.dart';

void main() {
  runApp(
    DenariiDolorApp(services: AppServices(sessionLock: LockedSessionLock())),
  );
}
