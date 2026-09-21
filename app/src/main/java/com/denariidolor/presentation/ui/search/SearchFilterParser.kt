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
            startDateEpochMillis = startDate.trim().takeIf { it.isNotEmpty() }?.let { DateUtils.parseIsoDateToStartOfDayEpochMillis(it, zoneId) },
            endDateEpochMillis = endDate.trim().takeIf { it.isNotEmpty() }?.let { DateUtils.parseIsoDateToEndOfDayEpochMillis(it, zoneId) }
        )
        if (filters.startDateEpochMillis != null && filters.endDateEpochMillis != null &&
            filters.startDateEpochMillis > filters.endDateEpochMillis
        ) {
            throw IllegalArgumentException("Start date must be on or before the end date")
        }
        if (filters.minAmountCents != null && filters.maxAmountCents != null && filters.minAmountCents > filters.maxAmountCents) {
            throw IllegalArgumentException("Minimum amount must be less than or equal to the maximum amount")
        }
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
