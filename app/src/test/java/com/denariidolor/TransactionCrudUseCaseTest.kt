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
    private val transactions = FakeTransactionRepository(listOf(TestData.expense(id = 1, amountCents = 1_000)))
    private val validate = ValidateTransactionUseCase(
        FakeCategoryRepository(TestData.categories),
        FakeAccountRepository(TestData.accounts),
        FakeBudgetRepository(),
        transactions
    )
    private val add = AddTransactionUseCase(validate, transactions)
    private val update = UpdateTransactionUseCase(validate, transactions)
    private val delete = DeleteTransactionUseCase(transactions)

    private fun expense(id: Long, amountCents: Long) = Expense(
        id = id,
        description = "Coffee",
        amountCents = amountCents,
        categoryId = 4,
        accountId = 1,
        dateEpochMillis = System.currentTimeMillis()
    )

    @Test
    fun addIgnoresCallerSuppliedId() = runBlocking {
        val id = add(expense(id = 1, amountCents = 500)).getOrThrow()

        assertEquals(2L, id)
        assertEquals(2, transactions.items.size)
    }

    @Test
    fun updateReplacesExistingTransaction() = runBlocking {
        val result =
            update(Income(id = 1, description = "Refund", amountCents = 3_000, categoryId = 2, accountId = 1, dateEpochMillis = 1L))

        assertTrue(result.isSuccess)
        assertEquals(TransactionType.INCOME, transactions.items.single().type)
        assertEquals(3_000L, transactions.items.single().amountCents)
    }

    @Test
    fun updateRejectsMissingIdAndUnknownTransaction() = runBlocking {
        assertEquals("Transaction ID is required", update(expense(id = 0, amountCents = 500)).exceptionOrNull()?.message)
        assertTrue(update(expense(id = 42, amountCents = 500)).exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun updateRunsValidation() = runBlocking {
        assertEquals("Invalid amount", update(expense(id = 1, amountCents = -100)).exceptionOrNull()?.message)
        assertEquals(1_000L, transactions.items.single().amountCents)
    }

    @Test
    fun deleteRemovesTransactionAndReportsMissing() = runBlocking {
        assertTrue(delete(1).isSuccess)
        assertTrue(transactions.items.isEmpty())
        assertTrue(delete(1).exceptionOrNull() is NoSuchElementException)
        assertTrue(delete(0).isFailure)
    }
}
