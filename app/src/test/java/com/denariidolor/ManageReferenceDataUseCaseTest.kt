package com.denariidolor

import com.denariidolor.domain.usecase.AccountUseCases
import com.denariidolor.domain.usecase.BudgetUseCases
import com.denariidolor.domain.usecase.CategoryUseCases
import com.denariidolor.testutil.FakeAccountRepository
import com.denariidolor.testutil.FakeBudgetRepository
import com.denariidolor.testutil.FakeCategoryRepository
import com.denariidolor.testutil.FakeTransactionRepository
import com.denariidolor.testutil.TestData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageReferenceDataUseCaseTest {
    private val transactions = FakeTransactionRepository(listOf(TestData.expense(id = 1, categoryId = 4, accountId = 3)))
    private val categoryRepo = FakeCategoryRepository(TestData.categories)
    private val accountRepo = FakeAccountRepository(TestData.accounts)
    private val budgetRepo = FakeBudgetRepository()
    private val categories = CategoryUseCases(categoryRepo, transactions)
    private val accounts = AccountUseCases(accountRepo, transactions)
    private val budgets = BudgetUseCases(budgetRepo, categoryRepo)

    @Test
    fun categoryAddTrimsAndRejectsDuplicatesCaseInsensitively() = runBlocking {
        assertTrue(categories.add("  Dining  ").isSuccess)
        assertEquals("Dining", categoryRepo.items.last().name)
        assertTrue(categories.add("groceries").isFailure)
        assertTrue(categories.add("   ").isFailure)
    }

    @Test
    fun categoryRenameAllowsKeepingOwnName() = runBlocking {
        assertTrue(categories.update(4, "GROCERIES").isSuccess)
        assertTrue(categories.update(4, "Transfer").isFailure)
        assertTrue(categories.update(99, "Other").exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun categoryRenamePreservesIcon() = runBlocking {
        val id = categories.add("Pets", "ic_pets").getOrThrow()
        categories.update(id, "Pet Care")

        assertEquals("ic_pets", categoryRepo.items.single { it.id == id }.iconName)
    }

    @Test
    fun categoryDeleteBlockedWhenDefaultOrInUse() = runBlocking {
        assertEquals("Default categories cannot be deleted", categories.delete(1).exceptionOrNull()?.message)
        assertEquals("Category is used by 1 transaction(s)", categories.delete(4).exceptionOrNull()?.message)
        val id = categories.add("Temp").getOrThrow()
        assertTrue(categories.delete(id).isSuccess)
    }

    @Test
    fun accountAddValidatesNameAndBalance() = runBlocking {
        assertTrue(accounts.add("Credit Card", -250.0).isSuccess)
        assertTrue(accounts.add("cash").isFailure)
        assertTrue(accounts.add("Broken", Double.POSITIVE_INFINITY).isFailure)
    }

    @Test
    fun accountDeleteBlockedWhenDefaultOrInUse() = runBlocking {
        assertTrue(accounts.delete(1).isFailure)
        assertEquals("Account is used by 1 transaction(s)", accounts.delete(3).exceptionOrNull()?.message)
        val id = accounts.add("Temp").getOrThrow()
        assertTrue(accounts.delete(id).isSuccess)
    }

    @Test
    fun budgetSetValidatesAndReplacesExisting() = runBlocking {
        val firstId = budgets.set(4, 200.0).getOrThrow()
        val secondId = budgets.set(4, 300.0, 90).getOrThrow()

        assertEquals(firstId, secondId)
        assertEquals(300.0, budgetRepo.items.single().monthlyLimit, 0.0001)
        assertEquals(90, budgetRepo.items.single().warningThresholdPercent)
        assertTrue(budgets.set(4, 0.0).isFailure)
        assertTrue(budgets.set(4, 100.0, 0).isFailure)
        assertTrue(budgets.set(99, 100.0).isFailure)
    }

    @Test
    fun budgetDelete() = runBlocking {
        budgets.set(4, 200.0)
        assertTrue(budgets.delete(4).isSuccess)
        assertTrue(budgets.delete(4).isFailure)
    }
}
