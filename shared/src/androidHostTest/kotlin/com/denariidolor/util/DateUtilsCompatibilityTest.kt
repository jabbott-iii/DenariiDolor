/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [DateUtils] once ran on java.time. These compare it with that code, kept below as [JavaTimeDateUtils], in zones with unusual
 * offsets and daylight-saving rules, so no date the app stores, shows or searches by moves.
 */
class DateUtilsCompatibilityTest {
    @Test
    fun monthRangesMatchJavaTime() {
        ZONES.forEach { id ->
            for (year in FIRST_YEAR..LAST_YEAR) {
                for (month in 1..12) {
                    assertEquals(
                        "$id $year-$month",
                        JavaTimeDateUtils.monthRangeEpochMillis(year, month, ZoneId.of(id)),
                        DateUtils.monthRangeEpochMillis(year, month, zone(id))
                    )
                }
            }
        }
    }

    @Test
    fun startAndEndOfDayMatchJavaTime() {
        ZONES.forEach { id ->
            var day = LocalDate.of(FIRST_YEAR, 1, 1)
            while (day.year <= LAST_YEAR) {
                val text = day.toString()
                assertEquals(
                    "$id $text",
                    JavaTimeDateUtils.startOfDay(text, ZoneId.of(id)),
                    DateUtils.parseIsoDateToStartOfDayEpochMillis(text, zone(id))
                )
                assertEquals(
                    "$id $text",
                    JavaTimeDateUtils.endOfDay(text, ZoneId.of(id)),
                    DateUtils.parseIsoDateToEndOfDayEpochMillis(text, zone(id))
                )
                day = day.plusDays(1)
            }
        }
    }

    /** Every 7 hours plus a millisecond, and a millisecond either side of each offset change, so every hour of day comes up. */
    @Test
    fun localDatesMatchJavaTime() {
        val first = Instant.parse("$FIRST_YEAR-01-01T00:00:00Z").toEpochMilli()
        val last = Instant.parse("${LAST_YEAR + 1}-01-01T00:00:00Z").toEpochMilli()
        ZONES.forEach { id ->
            val rules = ZoneId.of(id).rules
            val instants = generateSequence(first) { it + 7 * HOUR_MILLIS + 1 }.takeWhile { it < last }.toMutableList()
            var transition = rules.nextTransition(Instant.ofEpochMilli(first))
            while (transition != null && transition.instant.toEpochMilli() < last) {
                val at = transition.instant.toEpochMilli()
                instants += listOf(at - 1, at, at + 1)
                transition = rules.nextTransition(transition.instant)
            }
            instants.forEach { millis ->
                assertEquals(
                    "$id $millis",
                    JavaTimeDateUtils.formatLocalDate(millis, ZoneId.of(id)),
                    DateUtils.formatLocalDate(millis, zone(id))
                )
            }
        }
    }

    @Test
    fun typedDatesAreAcceptedOrRejectedAsJavaTimeDid() {
        TYPED_DATES.forEach { text ->
            val expected = runCatching { JavaTimeDateUtils.parseIsoDate(text).let { Triple(it.year, it.monthValue, it.dayOfMonth) } }
            val actual = runCatching { parsedDate(text) }

            assertEquals("'$text'", expected.getOrNull(), actual.getOrNull())
            assertEquals("'$text'", expected.isSuccess, actual.isSuccess)
            if (expected.isSuccess) {
                assertEquals(
                    "'$text'",
                    JavaTimeDateUtils.startOfDay(text, ZoneId.of("Asia/Kolkata")),
                    DateUtils.parseIsoDateToStartOfDayEpochMillis(text, zone("Asia/Kolkata"))
                )
            }
        }
    }

    @Test
    fun isoInstantsMatchJavaTime() {
        listOf(0L, 1L, 999L, 1_000L, 1_725_148_800_000L, 1_725_148_800_120L, -1L, -86_400_001L, 253_402_300_799_999L).forEach { millis ->
            assertEquals("$millis", Instant.ofEpochMilli(millis).toString(), DateUtils.formatIso(millis))
        }
    }

    // The two lines below are the whole difference between running these against java.time and against kotlinx-datetime.
    private fun zone(id: String) = TimeZone.of(id)

    private fun parsedDate(text: String): Triple<Int, Int, Int> = DateUtils.parseIsoDate(text).let {
        Triple(it.year, it.month.number, it.day)
    }

    /** DateUtils as it was before kotlinx-datetime, with its default zones made explicit. */
    private object JavaTimeDateUtils {
        fun monthRangeEpochMillis(year: Int, month: Int, zoneId: ZoneId): Pair<Long, Long> {
            val ym = YearMonth.of(year, month)
            val start = ym.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = ym.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
            return start to end
        }

        fun formatLocalDate(epochMillis: Long, zoneId: ZoneId): String =
            Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().toString()

        fun parseIsoDate(value: String): LocalDate = LocalDate.parse(Money.normalizeDigits(value).trim())

        fun startOfDay(value: String, zoneId: ZoneId): Long = parseIsoDate(value).atStartOfDay(zoneId).toInstant().toEpochMilli()

        fun endOfDay(value: String, zoneId: ZoneId): Long =
            parseIsoDate(value).plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
    }

    private companion object {
        const val FIRST_YEAR = 2008
        const val LAST_YEAR = 2032
        const val HOUR_MILLIS = 3_600_000L

        /** Half-hour and 45-minute offsets, both hemispheres, midnight transitions, a skipped day (Apia, 2011-12-30), +14:00. */
        val ZONES = listOf(
            "UTC",
            "America/New_York",
            "America/Los_Angeles",
            "America/Phoenix",
            "America/Sao_Paulo",
            "America/Santiago",
            "America/Havana",
            "Europe/London",
            "Africa/Casablanca",
            "Asia/Tehran",
            "Asia/Kolkata",
            "Asia/Kathmandu",
            "Australia/Lord_Howe",
            "Pacific/Chatham",
            "Pacific/Apia",
            "Pacific/Kiritimati"
        )

        val TYPED_DATES = listOf(
            "2024-09-01",
            " 2024-09-01 ",
            "\t2024-09-01\n",
            "2024-02-29",
            "2000-02-29",
            "1900-02-28",
            "0001-01-01",
            "0000-01-01",
            "9999-12-31",
            "٢٠٢٤-٠٩-٠١",
            "२०२४-०९-०१",
            "‏٢٠٢٤-٠٩-٠١",
            "2023-02-29",
            "1900-02-29",
            "2024-04-31",
            "2024-13-01",
            "2024-00-10",
            "2024-09-00",
            "2024-9-1",
            "2024-09-1",
            "24-09-01",
            "02024-09-01",
            "10000-01-01",
            "+10000-01-01",
            "+2024-09-01",
            "-2024-09-01",
            "-0001-01-01",
            "2024/09/01",
            "20240901",
            "2024-09-01T00:00",
            "2024-09-01Z",
            "2024 -09-01",
            "2024-09-01 x",
            "",
            " ",
            "-",
            "2024-09",
            "2024"
        )
    }
}
