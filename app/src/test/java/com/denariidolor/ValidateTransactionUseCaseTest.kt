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
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.usecase.ValidateTransactionUseCase
import com.denariidolor.testutil.FakeAccountRepository
import com.denariidolor.testutil.FakeBudgetRepository
import com.denariidolor.testutil.FakeCategoryRepository
import com.denariidolor.testutil.FakeTransactionRepository
import com.denariidolor.testutil.TestData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun rejectsZeroOrNegativeAmount() = runBlocking<Unit> {
        assertEquals("Invalid amount", messageFor(TestData.expense(amountCents = 0)))
        assertEquals("Invalid amount", messageFor(TestData.expense(amountCents = -1)))
    }

    @Test
    fun rejectsBlankDescription() = runBlocking<Unit> {
        assertEquals("Description cannot be blank", messageFor(TestData.expense().copy(description = "   ")))
    }

    @Test
    fun rejectsNonPositiveDate() = runBlocking<Unit> {
        assertEquals("Invalid date", messageFor(TestData.expense().copy(dateEpochMillis = 0)))
    }

    @Test
    fun rejectsNonPositiveAccountOrCategoryIds() = runBlocking<Unit> {
        assertEquals("Invalid account", messageFor(TestData.expense(accountId = 0)))
        assertEquals("Invalid category", messageFor(TestData.expense(categoryId = 0)))
    }

    @Test
    fun transferRequiresDestination() = runBlocking<Unit> {
        val transfer = TestData.expense().copy(type = TransactionType.TRANSFER, categoryId = 3, transferAccountId = null)

        assertEquals("Transfer destination is required", messageFor(transfer))
    }

    @Test
    fun validTransferPasses() = runBlocking<Unit> {
        val transfer = TestData.expense().copy(type = TransactionType.TRANSFER, categoryId = 3, accountId = 1, transferAccountId = 2)

        assertTrue(useCase(transfer).isSuccess)
    }

    @Test
    fun budgetOnlyLimitsExpenses() = runBlocking<Unit> {
        transactions.expenseTotalCents = 9_000
        val income = TestData.expense(amountCents = 5_000).copy(type = TransactionType.INCOME)

        assertTrue(useCase(income).isSuccess)
    }

    @Test
    fun expenseWithoutBudgetIsNotLimited() = runBlocking<Unit> {
        transactions.expenseTotalCents = 1_000_000

        assertTrue(useCase(TestData.expense(amountCents = 500_000, categoryId = 1)).isSuccess)
    }

    @Test
    fun repositoryErrorBecomesFailureInsteadOfCrash() = runBlocking<Unit> {
        val failingCategories = object : CategoryRepository by FakeCategoryRepository(TestData.categories) {
            override suspend fun getById(id: Long): CategoryEntity? = throw IllegalStateException("database closed")
        }
        val failingUseCase = ValidateTransactionUseCase(failingCategories, FakeAccountRepository(TestData.accounts), budgets, transactions)

        val result = failingUseCase(TestData.expense())

        assertFalse(result.isSuccess)
        assertEquals("database closed", result.exceptionOrNull()?.message)
    }

    private suspend fun messageFor(transaction: TransactionEntity): String? = useCase(transaction).exceptionOrNull()?.message
}
