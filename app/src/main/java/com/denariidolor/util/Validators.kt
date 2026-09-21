package com.denariidolor.util

import com.denariidolor.domain.model.SearchFilters

object Validators {
    fun isValidAmount(amount: Double): Boolean = amount > 0.0 && amount.isFinite()

    fun isValidDescription(description: String): Boolean = description.isNotBlank()

    fun isValidDateEpoch(epochMillis: Long): Boolean = epochMillis > 0

    const val MAX_NAME_LENGTH = 50

    fun isValidName(name: String): Boolean = name.trim().length in 1..MAX_NAME_LENGTH

    fun isValidPercent(percent: Int): Boolean = percent in 1..100

    fun isValidBalance(balance: Double): Boolean = balance.isFinite()

    fun isValidSearchRange(filters: SearchFilters): Boolean {
        val amountOk = (filters.minAmount == null || filters.maxAmount == null || filters.minAmount <= filters.maxAmount)
        val dateOk = (filters.startDateEpochMillis == null || filters.endDateEpochMillis == null ||
            filters.startDateEpochMillis <= filters.endDateEpochMillis)
        return amountOk && dateOk
    }
}
