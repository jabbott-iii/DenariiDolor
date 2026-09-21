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
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    budgetRepository: BudgetRepository,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val clock: Clock
) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.getAll(),
        categoryRepository.getAll(),
        accountRepository.getAll(),
        budgetRepository.getAll()
    ) { transactions, categories, accounts, budgets ->
        val period = YearMonth.now(clock)
        val (start, end) = DateUtils.monthRangeEpochMillis(period.year, period.monthValue, clock.zone)
        val monthTransactions = transactions.filter { it.dateEpochMillis in start..end }
        DashboardUiState(
            period = period,
            summary = summarize(monthTransactions),
            spending = spendingByCategory(monthTransactions, categories),
            budgets = budgetProgress(budgets, categories, monthTransactions),
            balances = accounts.map { AccountBalance(it.id, it.name, it.balanceCents) },
            recent = buildTransactionRows(transactions, categories, accounts, RECENT_LIMIT, clock.zone)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            _messages.send(UiMessage.fromResult(deleteTransactionUseCase(id), R.string.transaction_deleted))
        }
    }

    private companion object {
        const val RECENT_LIMIT = 20
    }
}
