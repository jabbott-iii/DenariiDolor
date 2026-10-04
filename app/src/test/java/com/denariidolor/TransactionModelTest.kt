/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import com.denariidolor.domain.model.Expense
import com.denariidolor.domain.model.Income
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.model.Transfer
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionModelTest {
    private val expense = Expense(description = "Groceries", amountCents = 4_500, categoryId = 1, accountId = 1, dateEpochMillis = 1L)
    private val income = Income(description = "Salary", amountCents = 125_000, categoryId = 2, accountId = 1, dateEpochMillis = 1L)
    private val transfer =
        Transfer(description = "Move", amountCents = 8_000, categoryId = 3, accountId = 1, transferAccountId = 2, dateEpochMillis = 1L)

    @Test
    fun transactionSubclassesReportExpectedBalanceImpact() {
        assertEquals(-4_500L, expense.balanceImpact())
        assertEquals(125_000L, income.balanceImpact())
        assertEquals(0L, transfer.balanceImpact())
    }

    @Test
    fun transactionSubclassesReportPerAccountImpacts() {
        assertEquals(mapOf(1L to -4_500L), expense.accountImpacts())
        assertEquals(mapOf(1L to 125_000L), income.accountImpacts())
        assertEquals(mapOf(1L to -8_000L, 2L to 8_000L), transfer.accountImpacts())
    }

    @Test
    fun subclassesExposeTheirType() {
        assertEquals(TransactionType.EXPENSE, expense.type)
        assertEquals(TransactionType.INCOME, income.type)
        assertEquals(TransactionType.TRANSFER, transfer.type)
    }

    @Test
    fun typeParsingIsCaseInsensitiveAndStrict() {
        assertEquals(TransactionType.INCOME, TransactionType.parse(" income "))
        assertEquals(true, runCatching { TransactionType.parse("REFUND") }.isFailure)
    }
}
