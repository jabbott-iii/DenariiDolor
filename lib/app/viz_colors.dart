/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

import 'package:material_ui/material_ui.dart';

/// The colors of charts and budget meters, which Material's `ColorScheme` has no roles for (v1's
/// `VizColors.kt`). Each theme picks its own values rather than inverting the other's.
///
/// Read them with `Theme.of(context).extension<VizColors>()`.
@immutable
final class VizColors extends ThemeExtension<VizColors> {
  const VizColors({
    required this.series1,
    required this.meterTrack,
    required this.statusGood,
    required this.statusWarning,
    required this.statusCritical,
  });

  /// The spending chart's columns, and a budget within its limit.
  final Color series1;

  /// The unfilled part of a budget's progress bar.
  final Color meterTrack;

  /// A good status. v1 defined it, but no v1 screen used it.
  final Color statusGood;

  /// A budget near its limit, and its warning icon.
  final Color statusWarning;

  /// A budget over its limit.
  final Color statusCritical;

  @override
  VizColors copyWith({
    Color? series1,
    Color? meterTrack,
    Color? statusGood,
    Color? statusWarning,
    Color? statusCritical,
  }) => VizColors(
    series1: series1 ?? this.series1,
    meterTrack: meterTrack ?? this.meterTrack,
    statusGood: statusGood ?? this.statusGood,
    statusWarning: statusWarning ?? this.statusWarning,
    statusCritical: statusCritical ?? this.statusCritical,
  );

  /// Blends toward [other] while `MaterialApp` animates a change of theme.
  @override
  VizColors lerp(VizColors? other, double t) {
    if (other == null) {
      return this;
    }
    return VizColors(
      series1: _lerp(series1, other.series1, t),
      meterTrack: _lerp(meterTrack, other.meterTrack, t),
      statusGood: _lerp(statusGood, other.statusGood, t),
      statusWarning: _lerp(statusWarning, other.statusWarning, t),
      statusCritical: _lerp(statusCritical, other.statusCritical, t),
    );
  }

  // Color.lerp returns null only when both colors are null.
  static Color _lerp(Color a, Color b, double t) => Color.lerp(a, b, t) ?? b;

  @override
  bool operator ==(Object other) =>
      other is VizColors &&
      other.series1 == series1 &&
      other.meterTrack == meterTrack &&
      other.statusGood == statusGood &&
      other.statusWarning == statusWarning &&
      other.statusCritical == statusCritical;

  @override
  int get hashCode => Object.hash(
    series1,
    meterTrack,
    statusGood,
    statusWarning,
    statusCritical,
  );
}
