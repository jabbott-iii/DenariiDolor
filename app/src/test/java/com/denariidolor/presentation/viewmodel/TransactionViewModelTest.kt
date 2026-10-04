/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.denariidolor.R
import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.usecase.AddTransactionUseCase
import com.denariidolor.domain.usecase.BudgetOverage
import com.denariidolor.domain.usecase.UpdateTransactionUseCase
import com.denariidolor.domain.usecase.ValidateTransactionUseCase
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.presentation.ui.transaction.TransactionEvent
import com.denariidolor.presentation.ui.transaction.TransactionFormState
import com.denariidolor.presentation.ui.transaction.TransactionViewModel
import com.denariidolor.testutil.FakeAccountRepository
import com.denariidolor.testutil.FakeBudgetRepository
import com.denariidolor.testutil.FakeCategoryRepository
import com.denariidolor.testutil.FakeTransactionRepository
import com.denariidolor.testutil.MainDispatcherRule
import com.denariidolor.testutil.TestData
import java.time.Clock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TransactionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val transactions = FakeTransactionRepository(listOf(TestData.expense(id = 7, amountCents = 450)))

    private fun viewModel(
        transactionId: Long? = null,
        repository: TransactionRepository = transactions,
        budgets: FakeBudgetRepository = FakeBudgetRepository()
    ): TransactionViewModel {
        val categories = FakeCategoryRepository(TestData.categories)
        val accounts = FakeAccountRepository(TestData.accounts)
        val validate = ValidateTransactionUseCase(categories, accounts, budgets, repository, Clock.systemDefaultZone())
        return TransactionViewModel(
            AddTransactionUseCase(validate, repository),
            UpdateTransactionUseCase(validate, repository),
            repository,
            categories,
            accounts,
            SavedStateHandle(transactionId?.let { mapOf(TransactionViewModel.ARG_TRANSACTION_ID to it) } ?: emptyMap())
        )
    }

    @Test
    fun editModeLoadsExistingTransaction() = runTest {
        val vm = viewModel(transactionId = 7)

        val state = vm.formState.first { it !is TransactionFormState.Loading } as TransactionFormState.Ready

        assertTrue(vm.isEditMode)
        assertEquals("4.5", state.initial?.amount)
        assertEquals("EXPENSE", state.initial?.type)
    }

    @Test
    fun unknownIdReportsNotFound() = runTest {
        assertEquals(TransactionFormState.NotFound, viewModel(transactionId = 99).formState.first { it !is TransactionFormState.Loading })
    }

    @Test
    fun addSavesInCentsAndEmitsSaved() = runTest {
        val vm = viewModel()

        vm.saveTransaction("expense", "Lunch", 1_299, categoryId = 4, accountId = 1, transferAccountId = null, dateEpochMillis = 1L)

        assertEquals(TransactionEvent.Saved, vm.events.first())
        assertEquals(1_299L, transactions.items.last().amountCents)
    }

    @Test
    fun invalidTransferEmitsFailure() = runTest {
        val vm = viewModel()

        vm.saveTransaction("TRANSFER", "Move", 100, categoryId = 3, accountId = 1, transferAccountId = null, dateEpochMillis = 1L)

        assertEquals(TransactionEvent.Failed(UiMessage.Resource(R.string.error_transfer_destination_required)), vm.events.first())
    }

    @Test
    fun saveWhileOneIsInFlightIsIgnored() = runTest {
        val release = CompletableDeferred<Unit>()
        val slowTransactions = object : TransactionRepository by transactions {
            override suspend fun add(transaction: TransactionEntity): Long {
                release.await()
                return transactions.add(transaction)
            }
        }
        val vm = viewModel(repository = slowTransactions)

        vm.saveTransaction("EXPENSE", "Lunch", 1_299, categoryId = 4, accountId = 1, transferAccountId = null, dateEpochMillis = 1L)
        assertTrue(vm.isSaving.value)
        vm.saveTransaction("EXPENSE", "Lunch", 1_299, categoryId = 4, accountId = 1, transferAccountId = null, dateEpochMillis = 1L)
        release.complete(Unit)

        assertEquals(TransactionEvent.Saved, vm.events.first())
        assertEquals(1, transactions.items.count { it.description == "Lunch" })
        assertFalse(vm.isSaving.value)
    }

    @Test
    fun successfulEditKeepsSaveDisabledWhileTheScreenCloses() = runTest {
        val vm = viewModel(transactionId = 7)
        vm.formState.first { it !is TransactionFormState.Loading }

        vm.saveTransaction("EXPENSE", "Coffee", 500, categoryId = 4, accountId = 1, transferAccountId = null, dateEpochMillis = 1L)

        assertEquals(TransactionEvent.Saved, vm.events.first())
        assertTrue(vm.isSaving.value)
    }

    @Test
    fun overBudgetExpenseAsksBeforeSaving() = runTest {
        val vm = viewModel(budgets = groceriesBudget(limitCents = 1_000))

        vm.saveTransaction("EXPENSE", "Lunch", 1_299, categoryId = 4, accountId = 1, transferAccountId = null, dateEpochMillis = 1L)

        assertEquals(BudgetOverage("Groceries", overByCents = 299, limitCents = 1_000), vm.overBudget.first { it != null })
        assertEquals(0, transactions.items.count { it.description == "Lunch" })
        assertFalse(vm.isSaving.value)
    }

    @Test
    fun confirmingOverBudgetSavesTheExpense() = runTest {
        val vm = viewModel(budgets = groceriesBudget(limitCents = 1_000))
        vm.saveTransaction("EXPENSE", "Lunch", 1_299, categoryId = 4, accountId = 1, transferAccountId = null, dateEpochMillis = 1L)
        vm.overBudget.first { it != null }

        vm.confirmOverBudget()

        assertEquals(TransactionEvent.Saved, vm.events.first())
        assertNull(vm.overBudget.value)
        assertEquals(listOf(1_299L), transactions.items.filter { it.description == "Lunch" }.map { it.amountCents })
    }

    @Test
    fun dismissingOverBudgetSavesNothingAndAsksAgainNextTime() = runTest {
        val vm = viewModel(budgets = groceriesBudget(limitCents = 1_000))
        vm.saveTransaction("EXPENSE", "Lunch", 1_299, categoryId = 4, accountId = 1, transferAccountId = null, dateEpochMillis = 1L)
        vm.overBudget.first { it != null }

        vm.dismissOverBudget()
        vm.confirmOverBudget()

        assertNull(vm.overBudget.value)
        assertFalse(vm.isSaving.value)
        assertEquals(0, transactions.items.count { it.description == "Lunch" })

        vm.saveTransaction("EXPENSE", "Lunch", 1_299, categoryId = 4, accountId = 1, transferAccountId = null, dateEpochMillis = 1L)

        assertEquals(299L, vm.overBudget.first { it != null }?.overByCents)
    }

    private fun groceriesBudget(limitCents: Long) =
        FakeBudgetRepository(listOf(BudgetEntity(id = 1, categoryId = 4, monthlyLimitCents = limitCents)))

    @Test
    fun failedSaveCanBeRetried() = runTest {
        val vm = viewModel()

        vm.saveTransaction("TRANSFER", "Move", 100, categoryId = 3, accountId = 1, transferAccountId = null, dateEpochMillis = 1L)

        assertTrue(vm.events.first() is TransactionEvent.Failed)
        assertFalse(vm.isSaving.value)
    }
}
