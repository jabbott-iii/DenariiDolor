/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * The system clock in the device's *current* time zone. [Clock.systemDefaultZone] fixes the zone when it is created, so after a
 * time-zone change, budget checks and the Dashboard could place a transaction near midnight in different months (BUG-09).
 */
object DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()

    override fun millis(): Long = System.currentTimeMillis()
}
