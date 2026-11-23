/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.legal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.denariidolor.data.legal.LegalBlock
import com.denariidolor.data.legal.LegalDocument
import com.denariidolor.data.legal.LegalDocuments
import com.denariidolor.di.SessionScope
import com.denariidolor.util.runSuspendCatching
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

sealed interface LegalUiState {
    data object Loading : LegalUiState

    data class Loaded(val blocks: List<LegalBlock>) : LegalUiState

    data object Failed : LegalUiState
}

@AssistedInject
class LegalDocumentViewModel(@Assisted savedStateHandle: SavedStateHandle, documents: LegalDocuments) : ViewModel() {
    val document: LegalDocument = LegalDocument.valueOf(checkNotNull(savedStateHandle.get<String>(ARG_DOCUMENT)))

    val state: StateFlow<LegalUiState> = flow {
        emit(runSuspendCatching { documents.load(document) }.fold(onSuccess = LegalUiState::Loaded, onFailure = { LegalUiState.Failed }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LegalUiState.Loading)

    /** Builds the [SavedStateHandle] from the screen's creation extras, which carry the document's name. */
    @AssistedFactory
    @ViewModelAssistedFactoryKey(LegalDocumentViewModel::class)
    @ContributesIntoMap(SessionScope::class)
    fun interface Factory : ViewModelAssistedFactory {
        override fun create(extras: CreationExtras): LegalDocumentViewModel = create(extras.createSavedStateHandle())

        fun create(savedStateHandle: SavedStateHandle): LegalDocumentViewModel
    }

    companion object {
        const val ARG_DOCUMENT = "document"
    }
}
