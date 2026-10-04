/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.viewmodel

import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.usecase.DeleteTransactionUseCase
import com.denariidolor.presentation.ui.dashboard.BudgetStatus
import com.denariidolor.presentation.ui.dashboard.DashboardViewModel
import com.denariidolor.testutil.FakeAccountRepository
import com.denariidolor.testutil.FakeBudgetRepository
import com.denariidolor.testutil.FakeCategoryRepository
import com.denariidolor.testutil.FakeTransactionRepository
import com.denariidolor.testutil.MainDispatcherRule
import com.denariidolor.testutil.TestData
import com.denariidolor.util.DateUtils
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-09-20T12:00:00Z"), ZoneOffset.UTC)
    private val september = DateUtils.monthRangeEpochMillis(2026, 9, ZoneOffset.UTC).first

    @Test
    fun summarizesCurrentMonthOnlyAndFlagsBudgets() = runTest {
        val transactions = FakeTransactionRepository(
            listOf(
                TestData.expense(id = 1, amountCents = 9_000).copy(dateEpochMillis = september + 1),
                TestData.expense(id = 2, amountCents = 50_000).copy(dateEpochMillis = september - 1)
            )
        )
        val viewModel = DashboardViewModel(
            transactions,
            FakeCategoryRepository(TestData.categories),
            FakeAccountRepository(TestData.accounts),
            FakeBudgetRepository(listOf(BudgetEntity(id = 1, categoryId = 4, monthlyLimitCents = 10_000))),
            DeleteTransactionUseCase(transactions),
            clock
        )
        val collector = launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.first { it.period != null }

        assertEquals(YearMonth.of(2026, 9), state.period)
        assertEquals(9_000L, state.summary.expenseCents)
        assertEquals(-9_000L, state.summary.netCents)
        assertEquals(BudgetStatus.WARNING, state.budgets.single().status)
        assertEquals(1, state.budgetAlerts)
        assertEquals(2, state.recent.size)
        collector.cancel()
    }

    @Test
    fun loadErrorShowsErrorStateInsteadOfCrashing() = runTest {
        val failing = object : TransactionRepository by FakeTransactionRepository() {
            override fun observeByDateRange(startInclusive: Long, endInclusive: Long): Flow<List<TransactionEntity>> =
                flow { error("malformed row") }
        }
        val viewModel = viewModel(failing, clock)
        val collector = launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.first { it.failed }

        assertEquals(YearMonth.of(2026, 9), state.period)
        collector.cancel()
    }

    @Test
    fun refreshPeriodRollsOverToTheNewMonth() = runTest {
        val october = DateUtils.monthRangeEpochMillis(2026, 10, ZoneOffset.UTC).first
        val transactions = FakeTransactionRepository(
            listOf(
                TestData.expense(id = 1, amountCents = 9_000).copy(dateEpochMillis = september + 1),
                TestData.expense(id = 2, amountCents = 1_000).copy(dateEpochMillis = october + 1)
            )
        )
        val movingClock = MutableClock(Instant.parse("2026-09-30T23:59:00Z"))
        val viewModel = viewModel(transactions, movingClock)
        val collector = launch { viewModel.uiState.collect {} }
        assertEquals(9_000L, viewModel.uiState.first { it.period != null }.summary.expenseCents)

        movingClock.now = Instant.parse("2026-10-01T00:01:00Z")
        viewModel.refreshPeriod()

        val state = viewModel.uiState.first { it.period == YearMonth.of(2026, 10) }
        assertEquals(1_000L, state.summary.expenseCents)
        collector.cancel()
    }

    private fun viewModel(transactions: TransactionRepository, clock: Clock) = DashboardViewModel(
        transactions,
        FakeCategoryRepository(TestData.categories),
        FakeAccountRepository(TestData.accounts),
        FakeBudgetRepository(),
        DeleteTransactionUseCase(transactions),
        clock
    )

    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now
    }
}
