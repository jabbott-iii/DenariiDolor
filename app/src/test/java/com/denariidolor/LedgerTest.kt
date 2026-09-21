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

import com.denariidolor.domain.model.Expense
import com.denariidolor.domain.model.Income
import com.denariidolor.domain.model.Ledger
import com.denariidolor.domain.model.Transfer
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerTest {
    private val expense = Expense(id = 1, description = "Food", amountCents = 4_500, categoryId = 1, accountId = 1, dateEpochMillis = 1L)

    @Test
    fun addAppliesImpact() {
        assertEquals(mapOf(1L to -4_500L), Ledger.balanceDeltas(previous = null, current = expense))
    }

    @Test
    fun deleteReversesImpact() {
        assertEquals(mapOf(1L to 4_500L), Ledger.balanceDeltas(previous = expense, current = null))
    }

    @Test
    fun editAmountAppliesOnlyTheDifference() {
        assertEquals(mapOf(1L to -500L), Ledger.balanceDeltas(expense, expense.copy(amountCents = 5_000)))
    }

    @Test
    fun editAccountMovesImpactBetweenAccounts() {
        assertEquals(mapOf(1L to 4_500L, 2L to -4_500L), Ledger.balanceDeltas(expense, expense.copy(accountId = 2)))
    }

    @Test
    fun unchangedEditProducesNoDeltas() {
        assertEquals(emptyMap<Long, Long>(), Ledger.balanceDeltas(expense, expense.copy(description = "Lunch")))
    }

    @Test
    fun changingTypeFromExpenseToIncomeDoublesSwing() {
        val income = Income(id = 1, description = "Refund", amountCents = 4_500, categoryId = 2, accountId = 1, dateEpochMillis = 1L)
        assertEquals(mapOf(1L to 9_000L), Ledger.balanceDeltas(expense, income))
    }

    @Test
    fun transferMovesMoneyBetweenAccounts() {
        val transfer = Transfer(description = "Save", amountCents = 10_000, categoryId = 3, accountId = 1, transferAccountId = 2, dateEpochMillis = 1L)
        assertEquals(mapOf(1L to -10_000L, 2L to 10_000L), Ledger.balanceDeltas(null, transfer))
        assertEquals(mapOf(1L to 10_000L, 2L to -10_000L), Ledger.balanceDeltas(transfer, null))
    }

    @Test
    fun centsAvoidFloatingPointDrift() {
        val tenCents = expense.copy(amountCents = 10)
        val total = (1..3).fold(0L) { acc, _ -> acc + Ledger.balanceDeltas(null, tenCents).getValue(1L) }
        assertEquals(-30L, total)
    }
}
