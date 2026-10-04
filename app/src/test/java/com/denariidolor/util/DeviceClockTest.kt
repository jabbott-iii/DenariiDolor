/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceClockTest {
    @Test
    fun zoneFollowsTheDeviceTimeZone() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            assertEquals(ZoneId.of("Asia/Tokyo"), DeviceClock.zone)
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            assertEquals(ZoneId.of("America/New_York"), DeviceClock.zone)
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
