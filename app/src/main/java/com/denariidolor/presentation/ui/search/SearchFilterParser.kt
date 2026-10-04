/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.search

import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.util.DateUtils
import com.denariidolor.util.Money
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
            minAmountCents = parseOptionalCents(minAmount, "minimum amount must be a valid number"),
            maxAmountCents = parseOptionalCents(maxAmount, "maximum amount must be a valid number"),
            startDateEpochMillis = startDate.trim().takeIf {
                it.isNotEmpty()
            }?.let { DateUtils.parseIsoDateToStartOfDayEpochMillis(it, zoneId) },
            endDateEpochMillis = endDate.trim().takeIf { it.isNotEmpty() }?.let { DateUtils.parseIsoDateToEndOfDayEpochMillis(it, zoneId) }
        )
        val start = filters.startDateEpochMillis
        val end = filters.endDateEpochMillis
        require(start == null || end == null || start <= end) { "Start date must be on or before the end date" }
        val min = filters.minAmountCents
        val max = filters.maxAmountCents
        require(min == null || max == null || min <= max) { "Minimum amount must be less than or equal to the maximum amount" }
        return filters
    }

    private fun parseOptionalLong(value: String, fieldName: String): Long? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.toLongOrNull() ?: throw IllegalArgumentException(fieldName)
    }

    private fun parseOptionalCents(value: String, fieldName: String): Long? {
        if (value.isBlank()) return null
        return Money.parseToCents(value) ?: throw IllegalArgumentException(fieldName)
    }
}
