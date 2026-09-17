package com.denariidolor

import com.denariidolor.presentation.ui.search.SearchFilterParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

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
        assertEquals(10.5, filters.minAmount)
        assertEquals(20.5, filters.maxAmount)
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
        assertNull(filters.minAmount)
        assertNull(filters.maxAmount)
        assertNull(filters.startDateEpochMillis)
        assertNull(filters.endDateEpochMillis)
    }
}
