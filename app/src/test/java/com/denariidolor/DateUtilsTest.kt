/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.denariidolor

import com.denariidolor.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class DateUtilsTest {
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun parseIsoDateToStartOfDayEpochMillisUsesInclusiveStart() {
        assertEquals(1_725_148_800_000L, DateUtils.parseIsoDateToStartOfDayEpochMillis("2024-09-01", zoneId))
    }

    @Test
    fun parseIsoDateToEndOfDayEpochMillisUsesInclusiveEnd() {
        assertEquals(1_725_235_199_999L, DateUtils.parseIsoDateToEndOfDayEpochMillis("2024-09-01", zoneId))
    }

    @Test
    fun formatLocalDateUsesZone() {
        assertEquals("2024-09-01", DateUtils.formatLocalDate(1_725_148_800_000L, zoneId))
        assertEquals("2024-08-31", DateUtils.formatLocalDate(1_725_148_800_000L, ZoneId.of("America/Phoenix")))
    }
}
