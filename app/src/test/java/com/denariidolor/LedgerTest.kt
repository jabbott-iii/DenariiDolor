package com.denariidolor

import com.denariidolor.domain.model.Expense
import com.denariidolor.domain.model.Income
import com.denariidolor.domain.model.Ledger
import com.denariidolor.domain.model.Transfer
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerTest {
    private val expense = Expense(id = 1, description = "Food", amount = 45.0, categoryId = 1, accountId = 1, dateEpochMillis = 1L)

    @Test
    fun addAppliesImpact() {
        assertEquals(mapOf(1L to -45.0), Ledger.balanceDeltas(previous = null, current = expense))
    }

    @Test
    fun deleteReversesImpact() {
        assertEquals(mapOf(1L to 45.0), Ledger.balanceDeltas(previous = expense, current = null))
    }

    @Test
    fun editAmountAppliesOnlyTheDifference() {
        assertEquals(mapOf(1L to -5.0), Ledger.balanceDeltas(expense, expense.copy(amount = 50.0)))
    }

    @Test
    fun editAccountMovesImpactBetweenAccounts() {
        assertEquals(mapOf(1L to 45.0, 2L to -45.0), Ledger.balanceDeltas(expense, expense.copy(accountId = 2)))
    }

    @Test
    fun unchangedEditProducesNoDeltas() {
        assertEquals(emptyMap<Long, Double>(), Ledger.balanceDeltas(expense, expense.copy(description = "Lunch")))
    }

    @Test
    fun changingTypeFromExpenseToIncomeDoublesSwing() {
        val income = Income(id = 1, description = "Refund", amount = 45.0, categoryId = 2, accountId = 1, dateEpochMillis = 1L)
        assertEquals(mapOf(1L to 90.0), Ledger.balanceDeltas(expense, income))
    }

    @Test
    fun transferMovesMoneyBetweenAccounts() {
        val transfer = Transfer(description = "Save", amount = 100.0, categoryId = 3, accountId = 1, transferAccountId = 2, dateEpochMillis = 1L)
        assertEquals(mapOf(1L to -100.0, 2L to 100.0), Ledger.balanceDeltas(null, transfer))
        assertEquals(mapOf(1L to 100.0, 2L to -100.0), Ledger.balanceDeltas(transfer, null))
    }
}
