/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import kotlin.time.Clock
import kotlinx.datetime.TimeZone

/**
 * The time now and the zone to read it in: what code shared across platforms injects instead of `java.time.Clock` (rule 5).
 * [timeZone] is read on every use, so a time-zone change applies at once (BUG-09). On Android it is [DeviceClock], adapted by
 * [asZonedClock].
 */
interface ZonedClock : Clock {
    val timeZone: TimeZone
}
