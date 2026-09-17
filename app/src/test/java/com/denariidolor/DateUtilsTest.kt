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
}
