package com.denariidolor.presentation.ui.search

import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.util.DateUtils
import java.time.ZoneId

object SearchFilterParser {
    fun parse(
        description: String,
        categoryId: String,
        minAmount: String,
        maxAmount: String,
        startDate: String,
        endDate: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SearchFilters {
        val filters = SearchFilters(
            description = description.trim().takeIf { it.isNotEmpty() },
            categoryId = parseOptionalLong(categoryId, "category ID must be a whole number"),
            minAmount = parseOptionalDouble(minAmount, "minimum amount must be a valid number"),
            maxAmount = parseOptionalDouble(maxAmount, "maximum amount must be a valid number"),
            startDateEpochMillis = startDate.trim().takeIf { it.isNotEmpty() }?.let { DateUtils.parseIsoDateToStartOfDayEpochMillis(it, zoneId) },
            endDateEpochMillis = endDate.trim().takeIf { it.isNotEmpty() }?.let { DateUtils.parseIsoDateToEndOfDayEpochMillis(it, zoneId) }
        )
        if (filters.startDateEpochMillis != null && filters.endDateEpochMillis != null &&
            filters.startDateEpochMillis > filters.endDateEpochMillis
        ) {
            throw IllegalArgumentException("Start date must be on or before the end date")
        }
        return filters
    }

    private fun parseOptionalLong(value: String, fieldName: String): Long? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.toLongOrNull() ?: throw IllegalArgumentException(fieldName)
    }

    private fun parseOptionalDouble(value: String, fieldName: String): Double? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.toDoubleOrNull() ?: throw IllegalArgumentException(fieldName)
    }
}
