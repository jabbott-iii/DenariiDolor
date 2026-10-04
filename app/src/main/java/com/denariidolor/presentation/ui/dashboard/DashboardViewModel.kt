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

package com.denariidolor.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.R
import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.data.repository.BudgetRepository
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.usecase.DeleteTransactionUseCase
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.presentation.ui.common.buildTransactionRows
import com.denariidolor.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val clock: Clock
) : ViewModel() {
    private val period = MutableStateFlow(currentPeriod())

    /** Loads only the current month and the latest rows; a failure shows an error instead of crashing (BUG-06, BUG-10). */
    val uiState: StateFlow<DashboardUiState> = period
        .flatMapLatest { period -> observe(period).catch { emit(DashboardUiState(period = period.month, failed = true)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    /** Called on resume, so the Dashboard rolls over to a new month (or time zone) without waiting for a data change. */
    fun refreshPeriod() {
        period.value = currentPeriod()
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            _messages.send(UiMessage.fromResult(deleteTransactionUseCase(id), R.string.transaction_deleted))
        }
    }

    private fun currentPeriod() = Period(YearMonth.now(clock), clock.zone)

    private fun observe(period: Period): Flow<DashboardUiState> {
        val (start, end) = DateUtils.monthRangeEpochMillis(period.month.year, period.month.monthValue, period.zone)
        return combine(
            transactionRepository.observeByDateRange(start, end),
            transactionRepository.observeRecent(RECENT_LIMIT),
            categoryRepository.getAll(),
            accountRepository.getAll(),
            budgetRepository.getAll()
        ) { monthTransactions, recent, categories, accounts, budgets ->
            DashboardUiState(
                period = period.month,
                summary = summarize(monthTransactions),
                spending = spendingByCategory(monthTransactions, categories),
                budgets = budgetProgress(budgets, categories, monthTransactions),
                balances = accounts.map { AccountBalance(it.id, it.name, it.balanceCents) },
                recent = buildTransactionRows(recent, categories, accounts, RECENT_LIMIT, period.zone)
            )
        }
    }

    private data class Period(val month: YearMonth, val zone: ZoneId)

    private companion object {
        const val RECENT_LIMIT = 20
    }
}
