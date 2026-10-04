/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
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
import java.time.Clock
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
        transactions,
        Clock.systemDefaultZone()
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
    fun addIgnoresCallerSuppliedId() = runBlocking<Unit> {
        val id = add(expense(id = 1, amountCents = 500)).getOrThrow()

        assertEquals(2L, id)
        assertEquals(2, transactions.items.size)
    }

    @Test
    fun updateReplacesExistingTransaction() = runBlocking<Unit> {
        val result =
            update(Income(id = 1, description = "Refund", amountCents = 3_000, categoryId = 2, accountId = 1, dateEpochMillis = 1L))

        assertTrue(result.isSuccess)
        assertEquals(TransactionType.INCOME, transactions.items.single().type)
        assertEquals(3_000L, transactions.items.single().amountCents)
    }

    @Test
    fun updateRejectsMissingIdAndUnknownTransaction() = runBlocking<Unit> {
        assertEquals("Transaction ID is required", update(expense(id = 0, amountCents = 500)).exceptionOrNull()?.message)
        assertTrue(update(expense(id = 42, amountCents = 500)).exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun updateRunsValidation() = runBlocking<Unit> {
        assertEquals("Invalid amount", update(expense(id = 1, amountCents = -100)).exceptionOrNull()?.message)
        assertEquals(1_000L, transactions.items.single().amountCents)
    }

    @Test
    fun deleteRemovesTransactionAndReportsMissing() = runBlocking<Unit> {
        assertTrue(delete(1).isSuccess)
        assertTrue(transactions.items.isEmpty())
        assertTrue(delete(1).exceptionOrNull() is NoSuchElementException)
        assertTrue(delete(0).isFailure)
    }

    @Test
    fun addAndUpdateTrimTheDescription() = runBlocking<Unit> {
        val id = add(expense(id = 0, amountCents = 500).copy(description = "  Lunch  ")).getOrThrow()
        assertEquals("Lunch", transactions.items.single { it.id == id }.description)

        update(expense(id = id, amountCents = 500).copy(description = " Dinner ")).getOrThrow()
        assertEquals("Dinner", transactions.items.single { it.id == id }.description)
    }
}
