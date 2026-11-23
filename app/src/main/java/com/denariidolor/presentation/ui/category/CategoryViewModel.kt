/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.di.SessionScope
import com.denariidolor.domain.usecase.CategoryUseCases
import com.denariidolor.presentation.ui.common.StringKey
import com.denariidolor.presentation.ui.common.UiMessage
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@ViewModelKey
@ContributesIntoMap(SessionScope::class)
@Inject
class CategoryViewModel(private val categoryUseCases: CategoryUseCases, categoryRepository: CategoryRepository) : ViewModel() {
    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    fun add(name: String, iconKey: String) = report(StringKey.CATEGORY_SAVED) { categoryUseCases.add(name, iconKey) }

    fun update(id: Long, name: String, iconKey: String) = report(StringKey.CATEGORY_SAVED) { categoryUseCases.update(id, name, iconKey) }

    fun delete(id: Long) = report(StringKey.CATEGORY_DELETED) { categoryUseCases.delete(id) }

    private fun report(success: StringKey, action: suspend () -> Result<*>) {
        viewModelScope.launch { _messages.send(UiMessage.fromResult(action(), success)) }
    }
}
