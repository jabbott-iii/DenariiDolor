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
import com.denariidolor.data.local.preferences.RatePromptPolicy
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
import com.denariidolor.testutil.FakeRatePromptStore
import com.denariidolor.testutil.FakeTransactionRepository
import com.denariidolor.testutil.MainDispatcherRule
import com.denariidolor.testutil.TestData
import com.denariidolor.util.asZonedClock
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toKotlinTimeZone
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
        budgets: FakeBudgetRepository = FakeBudgetRepository(),
        ratePrompt: FakeRatePromptStore = FakeRatePromptStore(),
        clock: Clock = Clock.systemDefaultZone()
    ): TransactionViewModel {
        val categories = FakeCategoryRepository(TestData.categories)
        val accounts = FakeAccountRepository(TestData.accounts)
        val validate = ValidateTransactionUseCase(categories, accounts, budgets, repository, clock.asZonedClock())
        return TransactionViewModel(
            AddTransactionUseCase(validate, repository),
            UpdateTransactionUseCase(validate, repository),
            repository,
            categories,
            accounts,
            ratePrompt,
            clock.asZonedClock(),
            SavedStateHandle(transactionId?.let { mapOf(TransactionViewModel.ARG_TRANSACTION_ID to it) } ?: emptyMap())
        )
    }

    @Test
    fun aTransactionSavedWithoutADateIsDatedNowByTheInjectedClock() = runTest {
        val clock = Clock.fixed(Instant.parse("2026-09-20T12:00:00Z"), ZoneOffset.UTC)
        val vm = viewModel(clock = clock)

        vm.saveTransaction("EXPENSE", "Lunch", 1_299, categoryId = 4, accountId = 1, transferAccountId = null, dateEpochMillis = null)

        assertEquals(TransactionEvent.Saved, vm.events.first())
        assertEquals(clock.millis(), transactions.items.last().dateEpochMillis)
    }

    @Test
    fun theDatePickerOpensOnTodayInTheInjectedClocksZone() {
        // 23:30 UTC is already the next day at UTC+14 and still the same day at UTC-7.
        val instant = Instant.parse("2026-09-20T23:30:00Z")

        assertEquals(LocalDate(2026, 9, 21), viewModel(clock = Clock.fixed(instant, ZoneId.of("Pacific/Kiritimati"))).today())
        assertEquals(LocalDate(2026, 9, 20), viewModel(clock = Clock.fixed(instant, ZoneId.of("America/Los_Angeles"))).today())
    }

    @Test
    fun editModeShowsTheStoredDateInTheClocksZone() = runTest {
        // 23:30 UTC on Sep 20 is already Sep 21 at UTC+14.
        val stored = TestData.expense(id = 7).copy(dateEpochMillis = Instant.parse("2026-09-20T23:30:00Z").toEpochMilli())
        val kiritimati = ZoneId.of("Pacific/Kiritimati")
        val vm = viewModel(
            transactionId = 7,
            repository = FakeTransactionRepository(listOf(stored)),
            clock = Clock.fixed(Instant.EPOCH, kiritimati)
        )

        val state = vm.formState.first { it !is TransactionFormState.Loading } as TransactionFormState.Ready

        assertEquals("2026-09-21", state.initial?.dateText)
        assertEquals(kiritimati.toKotlinTimeZone(), vm.timeZone())
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
    fun tenthNewTransactionShowsRatePrompt() = runTest {
        val ratePrompt = FakeRatePromptStore()

        repeat(RatePromptPolicy.FIRST_ASK_AFTER_SAVES) {
            assertFalse(ratePrompt.promptVisible.value)
            val vm = viewModel(ratePrompt = ratePrompt)
            vm.saveTransaction("EXPENSE", "Lunch", 1_299, categoryId = 4, accountId = 1, transferAccountId = null, dateEpochMillis = 1L)
            assertEquals(TransactionEvent.Saved, vm.events.first())
        }

        assertTrue(ratePrompt.promptVisible.value)
    }

    @Test
    fun editsAndFailedSavesDoNotCountTowardRatePrompt() = runTest {
        val ratePrompt = FakeRatePromptStore()

        val edit = viewModel(transactionId = 7, ratePrompt = ratePrompt)
        edit.formState.first { it !is TransactionFormState.Loading }
        edit.saveTransaction("EXPENSE", "Coffee", 500, categoryId = 4, accountId = 1, transferAccountId = null, dateEpochMillis = 1L)
        assertEquals(TransactionEvent.Saved, edit.events.first())
        val invalid = viewModel(ratePrompt = ratePrompt)
        invalid.saveTransaction("TRANSFER", "Move", 100, categoryId = 3, accountId = 1, transferAccountId = null, dateEpochMillis = 1L)
        invalid.events.first()

        assertEquals(0, ratePrompt.counters.saves)
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
