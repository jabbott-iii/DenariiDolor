package com.denariidolor.presentation.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.R
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.domain.usecase.CategoryUseCases
import com.denariidolor.presentation.ui.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryUseCases: CategoryUseCases,
    categoryRepository: CategoryRepository
) : ViewModel() {
    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    fun add(name: String) = report(R.string.category_saved) { categoryUseCases.add(name) }

    fun rename(id: Long, name: String) = report(R.string.category_saved) { categoryUseCases.update(id, name) }

    fun delete(id: Long) = report(R.string.category_deleted) { categoryUseCases.delete(id) }

    private fun report(successResId: Int, action: suspend () -> Result<*>) {
        viewModelScope.launch { _messages.send(UiMessage.fromResult(action(), successResId)) }
    }
}
