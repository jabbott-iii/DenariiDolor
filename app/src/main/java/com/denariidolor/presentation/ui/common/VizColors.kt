/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.common

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Data-visualization roles. Values follow the validated reference palette; light and dark are selected, not flipped. */
@Immutable
data class VizColors(val series1: Color, val meterTrack: Color, val statusGood: Color, val statusWarning: Color, val statusCritical: Color)
