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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val rows: StateFlow<List<BudgetRow>> = combine(categoryRepository.getAll(), budgetRepository.getAll()) { categories, budgets -> buildBudgetRows(categories, budgets) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    fun setBudget(categoryId: Long, limitText: String, warningPercentText: String) {
        val limit = limitText.trim().toDoubleOrNull()
        val warningPercent = warningPercentText.trim().toIntOrNull()
        viewModelScope.launch {
            val message = if (limit == null || warningPercent == null) {
                UiMessage.Resource(R.string.invalid_budget_input)
            } else {
                UiMessage.fromResult(budgetUseCases.set(categoryId, limit, warningPercent), R.string.budget_saved)
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
