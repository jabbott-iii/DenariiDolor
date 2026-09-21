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

package com.denariidolor.presentation.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.domain.usecase.SearchTransactionUseCase
import com.denariidolor.presentation.ui.common.PickerOption
import com.denariidolor.presentation.ui.common.TransactionRow
import com.denariidolor.presentation.ui.common.buildTransactionRows
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchTransactionUseCase: SearchTransactionUseCase,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository
) : ViewModel() {
    private val filters = MutableStateFlow<SearchFilters?>(null)

    val categoryOptions: StateFlow<List<PickerOption>> = categoryRepository.getAll()
        .map { categories -> categories.map { PickerOption(it.id, it.name) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** `null` until the first search; afterwards a live, auto-updating result list. */
    val results: StateFlow<List<TransactionRow>?> = combine(
        filters.flatMapLatest { current ->
            if (current == null) {
                flowOf<List<TransactionEntity>?>(null)
            } else {
                flow { emitAll(searchTransactionUseCase(current)) }.catch { emit(emptyList()) }
            }
        },
        categoryRepository.getAll(),
        accountRepository.getAll()
    ) { transactions, categories, accounts ->
        transactions?.let { buildTransactionRows(it, categories, accounts) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun search(newFilters: SearchFilters) {
        filters.value = newFilters
    }
}
