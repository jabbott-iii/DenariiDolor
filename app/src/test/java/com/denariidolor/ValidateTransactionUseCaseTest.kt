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

import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.usecase.ValidateTransactionUseCase
import com.denariidolor.testutil.FakeAccountRepository
import com.denariidolor.testutil.FakeBudgetRepository
import com.denariidolor.testutil.FakeCategoryRepository
import com.denariidolor.testutil.FakeTransactionRepository
import com.denariidolor.testutil.TestData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateTransactionUseCaseTest {
    private val transactions = FakeTransactionRepository()
    private val budgets = FakeBudgetRepository(listOf(BudgetEntity(id = 1, categoryId = 4, monthlyLimitCents = 10_000)))
    private val useCase = ValidateTransactionUseCase(
        FakeCategoryRepository(TestData.categories),
        FakeAccountRepository(TestData.accounts),
        budgets,
        transactions
    )

    @Test
    fun expenseFailsWhenBudgetExceeded() = runBlocking<Unit> {
        transactions.expenseTotalCents = 9_000

        val result = useCase(TestData.expense(amountCents = 2_000))

        assertTrue(result.isFailure)
        assertEquals("Budget threshold violated", result.exceptionOrNull()?.message)
    }

    @Test
    fun editedExpenseExcludesItsOwnPreviousAmountFromBudget() = runBlocking<Unit> {
        transactions.expenseTotalCents = 8_000

        val result = useCase(TestData.expense(id = 7, amountCents = 2_000))

        assertTrue(result.isSuccess)
        assertEquals(7L, transactions.lastExcludedTransactionId)
    }

    @Test
    fun transferFailsWhenDestinationMatchesSource() = runBlocking<Unit> {
        val result = useCase(
            TestData.expense().copy(type = TransactionType.TRANSFER, categoryId = 3, accountId = 1, transferAccountId = 1)
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun transferFailsWhenDestinationAccountMissing() = runBlocking<Unit> {
        val result = useCase(
            TestData.expense().copy(type = TransactionType.TRANSFER, categoryId = 3, accountId = 1, transferAccountId = 99)
        )

        assertEquals("Transfer destination account not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun failsWhenCategoryOrAccountMissing() = runBlocking<Unit> {
        assertEquals("Category not found", useCase(TestData.expense(categoryId = 99)).exceptionOrNull()?.message)
        assertEquals("Account not found", useCase(TestData.expense(accountId = 99)).exceptionOrNull()?.message)
    }

    @Test
    fun expenseExactlyAtLimitIsAllowed() = runBlocking<Unit> {
        transactions.expenseTotalCents = 8_000

        assertTrue(useCase(TestData.expense(amountCents = 2_000)).isSuccess)
        assertTrue(useCase(TestData.expense(amountCents = 2_001)).isFailure)
    }
}
