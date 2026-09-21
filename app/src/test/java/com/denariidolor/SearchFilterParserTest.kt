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

import com.denariidolor.presentation.ui.search.SearchFilterParser
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchFilterParserTest {
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun parseBuildsFiltersFromOptionalInputs() {
        val filters = SearchFilterParser.parse(
            description = " Groceries ",
            categoryId = "1",
            minAmount = "10.5",
            maxAmount = "20.5",
            startDate = "2024-09-01",
            endDate = "2024-09-30",
            zoneId = zoneId
        )

        assertEquals("Groceries", filters.description)
        assertEquals(1L, filters.categoryId)
        assertEquals(1_050L, filters.minAmountCents)
        assertEquals(2_050L, filters.maxAmountCents)
        assertEquals(1_725_148_800_000L, filters.startDateEpochMillis)
        assertEquals(1_727_740_799_999L, filters.endDateEpochMillis)
    }

    @Test(expected = java.time.format.DateTimeParseException::class)
    fun parseRejectsInvalidDates() {
        SearchFilterParser.parse(
            description = "",
            categoryId = "",
            minAmount = "",
            maxAmount = "",
            startDate = "09/01/2024",
            endDate = "",
            zoneId = zoneId
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseRejectsInvalidCategoryFilter() {
        SearchFilterParser.parse(
            description = "",
            categoryId = "abc",
            minAmount = "",
            maxAmount = "",
            startDate = "",
            endDate = "",
            zoneId = zoneId
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseRejectsInvalidMinimumAmount() {
        SearchFilterParser.parse(
            description = "",
            categoryId = "",
            minAmount = "1.2.3",
            maxAmount = "",
            startDate = "",
            endDate = "",
            zoneId = zoneId
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseRejectsInvalidMaximumAmount() {
        SearchFilterParser.parse(
            description = "",
            categoryId = "",
            minAmount = "",
            maxAmount = "4.5.6",
            startDate = "",
            endDate = "",
            zoneId = zoneId
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseRejectsInvertedDateRange() {
        SearchFilterParser.parse(
            description = "",
            categoryId = "",
            minAmount = "",
            maxAmount = "",
            startDate = "2024-09-30",
            endDate = "2024-09-01",
            zoneId = zoneId
        )
    }

    @Test
    fun parseLeavesBlankInputsUnset() {
        val filters = SearchFilterParser.parse(
            description = "   ",
            categoryId = "",
            minAmount = "",
            maxAmount = "",
            startDate = "",
            endDate = "",
            zoneId = zoneId
        )

        assertNull(filters.description)
        assertNull(filters.categoryId)
        assertNull(filters.minAmountCents)
        assertNull(filters.maxAmountCents)
        assertNull(filters.startDateEpochMillis)
        assertNull(filters.endDateEpochMillis)
    }
}
