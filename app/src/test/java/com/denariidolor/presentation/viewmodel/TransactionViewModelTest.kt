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

package com.denariidolor.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.denariidolor.domain.usecase.AddTransactionUseCase
import com.denariidolor.domain.usecase.UpdateTransactionUseCase
import com.denariidolor.domain.usecase.ValidateTransactionUseCase
import com.denariidolor.presentation.ui.transaction.TransactionEvent
import com.denariidolor.presentation.ui.transaction.TransactionFormState
import com.denariidolor.presentation.ui.transaction.TransactionViewModel
import com.denariidolor.testutil.FakeAccountRepository
import com.denariidolor.testutil.FakeBudgetRepository
import com.denariidolor.testutil.FakeCategoryRepository
import com.denariidolor.testutil.FakeTransactionRepository
import com.denariidolor.testutil.MainDispatcherRule
import com.denariidolor.testutil.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TransactionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val transactions = FakeTransactionRepository(listOf(TestData.expense(id = 7, amountCents = 450)))

    private fun viewModel(transactionId: Long? = null): TransactionViewModel {
        val categories = FakeCategoryRepository(TestData.categories)
        val accounts = FakeAccountRepository(TestData.accounts)
        val validate = ValidateTransactionUseCase(categories, accounts, FakeBudgetRepository(), transactions)
        return TransactionViewModel(
            AddTransactionUseCase(validate, transactions),
            UpdateTransactionUseCase(validate, transactions),
            transactions,
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

        assertEquals(TransactionEvent.Failed("Transfer destination is required"), vm.events.first())
    }
}
