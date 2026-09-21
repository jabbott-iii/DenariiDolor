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
