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
    fun categoryAddTrimsAndRejectsDuplicatesCaseInsensitively() = runBlocking<Unit> {
        assertTrue(categories.add("  Dining  ").isSuccess)
        assertEquals("Dining", categoryRepo.items.last().name)
        assertTrue(categories.add("groceries").isFailure)
        assertTrue(categories.add("   ").isFailure)
    }

    @Test
    fun categoryRenameAllowsKeepingOwnName() = runBlocking<Unit> {
        assertTrue(categories.update(4, "GROCERIES").isSuccess)
        assertTrue(categories.update(4, "Transfer").isFailure)
        assertTrue(categories.update(99, "Other").exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun categoryRenamePreservesIcon() = runBlocking<Unit> {
        val id = categories.add("Pets", "ic_pets").getOrThrow()
        categories.update(id, "Pet Care")

        assertEquals("ic_pets", categoryRepo.items.single { it.id == id }.iconName)
    }

    @Test
    fun categoryDeleteBlockedWhenDefaultOrInUse() = runBlocking<Unit> {
        assertEquals("Default categories cannot be deleted", categories.delete(1).exceptionOrNull()?.message)
        assertEquals("Category is used by 1 transaction(s)", categories.delete(4).exceptionOrNull()?.message)
        val id = categories.add("Temp").getOrThrow()
        assertTrue(categories.delete(id).isSuccess)
    }

    @Test
    fun accountAddValidatesNameAndBalance() = runBlocking<Unit> {
        assertTrue(accounts.add("Credit Card", -25_000).isSuccess)
        assertEquals(-25_000L, accountRepo.items.last().balanceCents)
        assertTrue(accounts.add("cash").isFailure)
    }

    @Test
    fun accountDeleteBlockedWhenDefaultOrInUse() = runBlocking<Unit> {
        assertTrue(accounts.delete(1).isFailure)
        assertEquals("Account is used by 1 transaction(s)", accounts.delete(3).exceptionOrNull()?.message)
        val id = accounts.add("Temp").getOrThrow()
        assertTrue(accounts.delete(id).isSuccess)
    }

    @Test
    fun budgetSetValidatesAndReplacesExisting() = runBlocking<Unit> {
        val firstId = budgets.set(4, 20_000).getOrThrow()
        val secondId = budgets.set(4, 30_000, 90).getOrThrow()

        assertEquals(firstId, secondId)
        assertEquals(30_000L, budgetRepo.items.single().monthlyLimitCents)
        assertEquals(90, budgetRepo.items.single().warningThresholdPercent)
        assertTrue(budgets.set(4, 0).isFailure)
        assertTrue(budgets.set(4, 10_000, 0).isFailure)
        assertTrue(budgets.set(99, 10_000).isFailure)
    }

    @Test
    fun budgetDelete() = runBlocking<Unit> {
        budgets.set(4, 20_000)
        assertTrue(budgets.delete(4).isSuccess)
        assertTrue(budgets.delete(4).isFailure)
    }
}
