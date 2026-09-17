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
            categoryId = categoryId.toLongOrNull(),
            minAmount = minAmount.toDoubleOrNull(),
            maxAmount = maxAmount.toDoubleOrNull(),
            startDateEpochMillis = startDate.trim().takeIf { it.isNotEmpty() }?.let { DateUtils.parseIsoDateToStartOfDayEpochMillis(it, zoneId) },
            endDateEpochMillis = endDate.trim().takeIf { it.isNotEmpty() }?.let { DateUtils.parseIsoDateToEndOfDayEpochMillis(it, zoneId) }
        )
    }
}
