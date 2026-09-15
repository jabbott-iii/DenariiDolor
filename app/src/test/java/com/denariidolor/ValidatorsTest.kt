package com.denariidolor

import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.util.Validators
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test
    fun amountAndDescriptionValidation() {
        assertTrue(Validators.isValidAmount(10.0))
        assertFalse(Validators.isValidAmount(0.0))
        assertTrue(Validators.isValidDescription("Rent"))
        assertFalse(Validators.isValidDescription("   "))
    }

    @Test
    fun rangeValidationRejectsInvertedRanges() {
        assertFalse(Validators.isValidSearchRange(SearchFilters(minAmount = 100.0, maxAmount = 50.0)))
        assertFalse(Validators.isValidSearchRange(SearchFilters(startDateEpochMillis = 2L, endDateEpochMillis = 1L)))
        assertTrue(Validators.isValidSearchRange(SearchFilters(minAmount = 1.0, maxAmount = 2.0)))
    }
}
