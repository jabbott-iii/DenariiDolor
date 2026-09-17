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
        return SearchFilters(
            description = description.trim().takeIf { it.isNotEmpty() },
            categoryId = parseOptionalLong(categoryId, "category"),
            minAmount = parseOptionalDouble(minAmount, "minimum amount"),
            maxAmount = parseOptionalDouble(maxAmount, "maximum amount"),
            startDateEpochMillis = startDate.trim().takeIf { it.isNotEmpty() }?.let { DateUtils.parseIsoDateToStartOfDayEpochMillis(it, zoneId) },
            endDateEpochMillis = endDate.trim().takeIf { it.isNotEmpty() }?.let { DateUtils.parseIsoDateToEndOfDayEpochMillis(it, zoneId) }
        )
    }

    private fun parseOptionalLong(value: String, fieldName: String): Long? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.toLongOrNull() ?: throw IllegalArgumentException("Invalid $fieldName")
    }

    private fun parseOptionalDouble(value: String, fieldName: String): Double? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid $fieldName")
    }
}
