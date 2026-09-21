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

package com.denariidolor.presentation.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.R
import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.repository.BudgetRepository
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.domain.usecase.BudgetUseCases
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetRow(val categoryId: Long, val categoryName: String, val budget: BudgetEntity?)

fun buildBudgetRows(categories: List<CategoryEntity>, budgets: List<BudgetEntity>): List<BudgetRow> {
    val budgetsByCategory = budgets.associateBy { it.categoryId }
    return categories.map { BudgetRow(it.id, it.name, budgetsByCategory[it.id]) }
}

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetUseCases: BudgetUseCases,
    categoryRepository: CategoryRepository,
    budgetRepository: BudgetRepository
) : ViewModel() {
    val rows: StateFlow<List<BudgetRow>> = combine(categoryRepository.getAll(), budgetRepository.getAll()) { categories, budgets ->
        buildBudgetRows(categories, budgets)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    fun setBudget(categoryId: Long, limitText: String, warningPercentText: String) {
        val limitCents = Money.parseToCents(limitText)
        val warningPercent = warningPercentText.trim().toIntOrNull()
        viewModelScope.launch {
            val message = if (limitCents == null || warningPercent == null) {
                UiMessage.Resource(R.string.invalid_budget_input)
            } else {
                UiMessage.fromResult(budgetUseCases.set(categoryId, limitCents, warningPercent), R.string.budget_saved)
            }
            _messages.send(message)
        }
    }

    fun deleteBudget(categoryId: Long) {
        viewModelScope.launch {
            _messages.send(UiMessage.fromResult(budgetUseCases.delete(categoryId), R.string.budget_deleted))
        }
    }
}
