package com.denariidolor.presentation.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.domain.usecase.SearchTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchTransactionUseCase: SearchTransactionUseCase
) : ViewModel() {
    private val _results = MutableStateFlow<List<String>>(emptyList())
    val results: StateFlow<List<String>> = _results.asStateFlow()

    fun search(filters: SearchFilters) {
        viewModelScope.launch {
            searchTransactionUseCase(filters).collect { entities ->
                _results.value = entities.map { "${it.description}: ${it.amount}" }
            }
        }
    }
}
