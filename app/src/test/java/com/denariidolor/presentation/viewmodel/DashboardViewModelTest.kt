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
import com.denariidolor.presentation.ui.dashboard.GettingStartedStep
import com.denariidolor.testutil.FakeAccountRepository
import com.denariidolor.testutil.FakeBudgetRepository
import com.denariidolor.testutil.FakeCategoryRepository
import com.denariidolor.testutil.FakeOnboardingStore
import com.denariidolor.testutil.FakeTransactionRepository
import com.denariidolor.testutil.MainDispatcherRule
import com.denariidolor.testutil.TestData
import com.denariidolor.util.DateUtils
import com.denariidolor.util.asZonedClock
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-09-20T12:00:00Z"), ZoneOffset.UTC)
    private val september = DateUtils.monthRangeEpochMillis(2026, 9, TimeZone.UTC).first

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
            FakeOnboardingStore(),
            clock.asZonedClock()
        )
        val collector = launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.first { it.period != null }

        assertEquals(YearMonth(2026, 9), state.period)
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

        assertEquals(YearMonth(2026, 9), state.period)
        collector.cancel()
    }

    @Test
    fun theMonthAndItsTransactionsFollowTheClocksZone() = runTest {
        // At 12:00 UTC on September 30 it is already October 1 in Kiritimati (UTC+14) and still September 30 in Los Angeles.
        // The expense at 11:00 UTC falls in Kiritimati's October, though it is still September in UTC.
        val now = Instant.parse("2026-09-30T12:00:00Z")
        val expense = TestData.expense(
            id = 1,
            amountCents = 9_000
        ).copy(dateEpochMillis = Instant.parse("2026-09-30T11:00:00Z").toEpochMilli())
        val kiritimati = viewModel(FakeTransactionRepository(listOf(expense)), Clock.fixed(now, ZoneId.of("Pacific/Kiritimati")))
        val losAngeles = viewModel(FakeTransactionRepository(listOf(expense)), Clock.fixed(now, ZoneId.of("America/Los_Angeles")))
        val collectors = listOf(launch { kiritimati.uiState.collect {} }, launch { losAngeles.uiState.collect {} })

        val october = kiritimati.uiState.first { it.period != null }
        val september = losAngeles.uiState.first { it.period != null }

        assertEquals(YearMonth(2026, 10), october.period)
        assertEquals(9_000L, october.summary.expenseCents)
        assertEquals(YearMonth(2026, 9), september.period)
        assertEquals(9_000L, september.summary.expenseCents)
        collectors.forEach { it.cancel() }
    }

    @Test
    fun refreshPeriodRollsOverToTheNewMonth() = runTest {
        val october = DateUtils.monthRangeEpochMillis(2026, 10, TimeZone.UTC).first
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

        val state = viewModel.uiState.first { it.period == YearMonth(2026, 10) }
        assertEquals(1_000L, state.summary.expenseCents)
        collector.cancel()
    }

    @Test
    fun showsGettingStartedWhileActiveAndHidesItWhenDismissed() = runTest {
        val onboarding = FakeOnboardingStore(checklistActive = true)
        val viewModel = viewModel(FakeTransactionRepository(), clock, onboarding)
        val collector = launch { viewModel.uiState.collect {} }

        // TestData.accounts has "Checking" besides the defaults; there are no transactions or budgets yet.
        val checklist = viewModel.uiState.first { it.period != null }.gettingStarted
        assertEquals(setOf(GettingStartedStep.ADD_ACCOUNT), checklist?.done)

        viewModel.dismissGettingStarted()

        assertNull(viewModel.uiState.first { it.gettingStarted == null }.gettingStarted)
        assertFalse(onboarding.checklistActive.value)
        collector.cancel()
    }

    @Test
    fun noGettingStartedForProfilesThatDidNotStartOnboarding() = runTest {
        val viewModel = viewModel(FakeTransactionRepository(), clock)
        val collector = launch { viewModel.uiState.collect {} }

        assertNull(viewModel.uiState.first { it.period != null }.gettingStarted)
        collector.cancel()
    }

    private fun viewModel(transactions: TransactionRepository, clock: Clock, onboarding: FakeOnboardingStore = FakeOnboardingStore()) =
        DashboardViewModel(
            transactions,
            FakeCategoryRepository(TestData.categories),
            FakeAccountRepository(TestData.accounts),
            FakeBudgetRepository(),
            DeleteTransactionUseCase(transactions),
            onboarding,
            clock.asZonedClock()
        )

    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now
    }
}
