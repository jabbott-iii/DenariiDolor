package com.denariidolor.domain.model

data class SearchFilters(
    val description: String? = null,
    val categoryId: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val startDateEpochMillis: Long? = null,
    val endDateEpochMillis: Long? = null
)
