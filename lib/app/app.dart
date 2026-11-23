/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:denarii_dolor/app/app_name.dart';
import 'package:denarii_dolor/app/app_scope.dart';
import 'package:denarii_dolor/app/root_switch.dart';
import 'package:material_ui/material_ui.dart';

/// The app: the app scope above a [MaterialApp] whose home is the [RootSwitch].
class DenariiDolorApp extends StatelessWidget {
  const DenariiDolorApp({super.key, required this.services});

  final AppServices services;

  @override
  Widget build(BuildContext context) {
    return AppScope(
      services: services,
      child: MaterialApp(
        title: appName,
        // Material 3's defaults, following the device's theme, until M1.5 ports v1's colors.
        theme: ThemeData(),
        darkTheme: ThemeData(brightness: Brightness.dark),
        home: const RootSwitch(),
      ),
    );
  }
}
