package com.denariidolor

import com.denariidolor.domain.model.Expense
import com.denariidolor.domain.model.Income
import com.denariidolor.domain.model.Transfer
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionModelTest {
    @Test
    fun transactionSubclassesReportExpectedBalanceImpact() {
        assertEquals(-45.0, Expense(description = "Groceries", amount = 45.0, categoryId = 1, accountId = 1, dateEpochMillis = 1L).balanceImpact(), 0.0001)
        assertEquals(1250.0, Income(description = "Salary", amount = 1250.0, categoryId = 2, accountId = 1, dateEpochMillis = 1L).balanceImpact(), 0.0001)
        assertEquals(0.0, Transfer(description = "Move", amount = 80.0, categoryId = 3, accountId = 1, transferAccountId = 2, dateEpochMillis = 1L).balanceImpact(), 0.0001)
    }

    @Test
    fun transactionSubclassesReportPerAccountImpacts() {
        assertEquals(mapOf(1L to -45.0), Expense(description = "Groceries", amount = 45.0, categoryId = 1, accountId = 1, dateEpochMillis = 1L).accountImpacts())
        assertEquals(mapOf(1L to 1250.0), Income(description = "Salary", amount = 1250.0, categoryId = 2, accountId = 1, dateEpochMillis = 1L).accountImpacts())
        assertEquals(
            mapOf(1L to -80.0, 2L to 80.0),
            Transfer(description = "Move", amount = 80.0, categoryId = 3, accountId = 1, transferAccountId = 2, dateEpochMillis = 1L).accountImpacts()
        )
    }
}
