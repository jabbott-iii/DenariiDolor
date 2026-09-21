package com.denariidolor

import com.denariidolor.domain.model.Expense
import com.denariidolor.domain.model.Income
import com.denariidolor.domain.usecase.AddTransactionUseCase
import com.denariidolor.domain.usecase.DeleteTransactionUseCase
import com.denariidolor.domain.usecase.UpdateTransactionUseCase
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

class TransactionCrudUseCaseTest {
    private val transactions = FakeTransactionRepository(listOf(TestData.expense(id = 1, amount = 10.0)))
    private val validate = ValidateTransactionUseCase(
        FakeCategoryRepository(TestData.categories),
        FakeAccountRepository(TestData.accounts),
        FakeBudgetRepository(),
        transactions
    )
    private val add = AddTransactionUseCase(validate, transactions)
    private val update = UpdateTransactionUseCase(validate, transactions)
    private val delete = DeleteTransactionUseCase(transactions)

    private fun expense(id: Long, amount: Double) =
        Expense(id = id, description = "Coffee", amount = amount, categoryId = 4, accountId = 1, dateEpochMillis = System.currentTimeMillis())

    @Test
    fun addIgnoresCallerSuppliedId() = runBlocking {
        val id = add(expense(id = 1, amount = 5.0)).getOrThrow()

        assertEquals(2L, id)
        assertEquals(2, transactions.items.size)
    }

    @Test
    fun updateReplacesExistingTransaction() = runBlocking {
        val result = update(Income(id = 1, description = "Refund", amount = 30.0, categoryId = 2, accountId = 1, dateEpochMillis = 1L))

        assertTrue(result.isSuccess)
        assertEquals("INCOME", transactions.items.single().type)
        assertEquals(30.0, transactions.items.single().amount, 0.0001)
    }

    @Test
    fun updateRejectsMissingIdAndUnknownTransaction() = runBlocking {
        assertEquals("Transaction ID is required", update(expense(id = 0, amount = 5.0)).exceptionOrNull()?.message)
        assertTrue(update(expense(id = 42, amount = 5.0)).exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun updateRunsValidation() = runBlocking {
        assertEquals("Invalid amount", update(expense(id = 1, amount = -1.0)).exceptionOrNull()?.message)
        assertEquals(10.0, transactions.items.single().amount, 0.0001)
    }

    @Test
    fun deleteRemovesTransactionAndReportsMissing() = runBlocking {
        assertTrue(delete(1).isSuccess)
        assertTrue(transactions.items.isEmpty())
        assertTrue(delete(1).exceptionOrNull() is NoSuchElementException)
        assertTrue(delete(0).isFailure)
    }
}
