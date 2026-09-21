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

import com.denariidolor.data.local.db.entity.BudgetEntity
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

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
}
