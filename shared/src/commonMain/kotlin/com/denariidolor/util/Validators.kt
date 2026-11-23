/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import com.denariidolor.domain.model.SearchFilters

object Validators {
    fun isValidAmount(amountCents: Long): Boolean = amountCents > 0

    fun isValidDescription(description: String): Boolean = description.trim().length in 1..MAX_DESCRIPTION_LENGTH

    fun isValidDateEpoch(epochMillis: Long): Boolean = epochMillis > 0

    const val MAX_NAME_LENGTH = 50
    const val MAX_DESCRIPTION_LENGTH = 200
    private const val MAX_PERCENT = 100

    fun isValidName(name: String): Boolean = name.trim().length in 1..MAX_NAME_LENGTH

    fun isValidPercent(percent: Int): Boolean = percent in 1..MAX_PERCENT

    fun isValidSearchRange(filters: SearchFilters): Boolean {
        val amountOk = (
            filters.minAmountCents == null ||
                filters.maxAmountCents == null ||
                filters.minAmountCents <= filters.maxAmountCents
            )
        val dateOk = (
            filters.startDateEpochMillis == null ||
                filters.endDateEpochMillis == null ||
                filters.startDateEpochMillis <= filters.endDateEpochMillis
            )
        return amountOk && dateOk
    }
}
