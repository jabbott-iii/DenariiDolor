/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.util.Validators
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test
    fun amountAndDescriptionValidation() {
        assertTrue(Validators.isValidAmount(1_000L))
        assertFalse(Validators.isValidAmount(0L))
        assertFalse(Validators.isValidAmount(-1L))
        assertTrue(Validators.isValidDescription("Rent"))
        assertFalse(Validators.isValidDescription("   "))
    }

    @Test
    fun rangeValidationRejectsInvertedRanges() {
        assertFalse(Validators.isValidSearchRange(SearchFilters(minAmountCents = 10_000, maxAmountCents = 5_000)))
        assertFalse(Validators.isValidSearchRange(SearchFilters(startDateEpochMillis = 2L, endDateEpochMillis = 1L)))
        assertTrue(Validators.isValidSearchRange(SearchFilters(minAmountCents = 100, maxAmountCents = 200)))
    }

    @Test
    fun nameAndPercentValidation() {
        assertTrue(Validators.isValidName("Groceries"))
        assertFalse(Validators.isValidName("   "))
        assertFalse(Validators.isValidName("x".repeat(Validators.MAX_NAME_LENGTH + 1)))
        assertTrue(Validators.isValidPercent(80))
        assertFalse(Validators.isValidPercent(0))
        assertFalse(Validators.isValidPercent(101))
    }

    @Test
    fun descriptionIsTrimmedAndCapped() {
        assertTrue(Validators.isValidDescription("x".repeat(Validators.MAX_DESCRIPTION_LENGTH)))
        assertTrue(Validators.isValidDescription("  " + "x".repeat(Validators.MAX_DESCRIPTION_LENGTH) + "  "))
        assertFalse(Validators.isValidDescription("x".repeat(Validators.MAX_DESCRIPTION_LENGTH + 1)))
    }
}
