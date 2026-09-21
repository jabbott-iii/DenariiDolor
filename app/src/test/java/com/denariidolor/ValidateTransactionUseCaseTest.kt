package com.denariidolor

import com.denariidolor.data.local.db.entity.BudgetEntity
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
    private val budgets = FakeBudgetRepository(listOf(BudgetEntity(id = 1, categoryId = 4, monthlyLimit = 100.0)))
    private val useCase = ValidateTransactionUseCase(
        FakeCategoryRepository(TestData.categories),
        FakeAccountRepository(TestData.accounts),
        budgets,
        transactions
    )

    @Test
    fun expenseFailsWhenBudgetExceeded() = runBlocking {
        transactions.expenseTotal = 90.0

        val result = useCase(TestData.expense(amount = 20.0))

        assertTrue(result.isFailure)
        assertEquals("Budget threshold violated", result.exceptionOrNull()?.message)
    }

    @Test
    fun editedExpenseExcludesItsOwnPreviousAmountFromBudget() = runBlocking {
        transactions.expenseTotal = 80.0

        val result = useCase(TestData.expense(id = 7, amount = 20.0))

        assertTrue(result.isSuccess)
        assertEquals(7L, transactions.lastExcludedTransactionId)
    }

    @Test
    fun transferFailsWhenDestinationMatchesSource() = runBlocking {
        val result = useCase(
            TestData.expense().copy(type = "TRANSFER", categoryId = 3, accountId = 1, transferAccountId = 1)
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun transferFailsWhenDestinationAccountMissing() = runBlocking {
        val result = useCase(
            TestData.expense().copy(type = "TRANSFER", categoryId = 3, accountId = 1, transferAccountId = 99)
        )

        assertEquals("Transfer destination account not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun failsWhenCategoryOrAccountMissing() = runBlocking {
        assertEquals("Category not found", useCase(TestData.expense(categoryId = 99)).exceptionOrNull()?.message)
        assertEquals("Account not found", useCase(TestData.expense(accountId = 99)).exceptionOrNull()?.message)
    }
}
