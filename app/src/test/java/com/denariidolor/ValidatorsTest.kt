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
}
