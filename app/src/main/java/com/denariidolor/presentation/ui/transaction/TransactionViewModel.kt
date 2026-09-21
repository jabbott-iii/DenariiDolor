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

package com.denariidolor.presentation.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.Expense
import com.denariidolor.domain.model.Income
import com.denariidolor.domain.model.Transaction
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.model.Transfer
import com.denariidolor.domain.usecase.AddTransactionUseCase
import com.denariidolor.domain.usecase.UpdateTransactionUseCase
import com.denariidolor.presentation.ui.common.PickerOption
import com.denariidolor.util.runSuspendCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TransactionFormState {
    data object Loading : TransactionFormState
    data object NotFound : TransactionFormState
    data class Ready(val initial: TransactionFormInput?) : TransactionFormState
}

sealed interface TransactionEvent {
    data object Saved : TransactionEvent
    data class Failed(val message: String?) : TransactionEvent
}

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val transactionId: Long? = savedStateHandle.get<Long>(ARG_TRANSACTION_ID)?.takeIf { it > 0L }

    val isEditMode: Boolean = transactionId != null

    val categoryOptions: StateFlow<List<PickerOption>> = categoryRepository.getAll()
        .map { categories -> categories.map { PickerOption(it.id, it.name) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accountOptions: StateFlow<List<PickerOption>> = accountRepository.getAll()
        .map { accounts -> accounts.map { PickerOption(it.id, it.name) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _formState = MutableStateFlow<TransactionFormState>(
        if (transactionId == null) TransactionFormState.Ready(null) else TransactionFormState.Loading
    )
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    private val _events = Channel<TransactionEvent>(Channel.BUFFERED)
    val events: Flow<TransactionEvent> = _events.receiveAsFlow()

    init {
        transactionId?.let { id ->
            viewModelScope.launch {
                val entity = runSuspendCatching { transactionRepository.getById(id) }.getOrNull()
                _formState.value = entity?.let { TransactionFormState.Ready(TransactionFormInput.from(it)) }
                    ?: TransactionFormState.NotFound
            }
        }
    }

    fun saveTransaction(
        type: String,
        description: String,
        amountCents: Long,
        categoryId: Long,
        accountId: Long,
        transferAccountId: Long?,
        dateEpochMillis: Long
    ) {
        viewModelScope.launch {
            val result = runSuspendCatching {
                buildTransaction(
                    id = transactionId ?: 0L,
                    type = TransactionType.parse(type),
                    description = description,
                    amountCents = amountCents,
                    categoryId = categoryId,
                    accountId = accountId,
                    transferAccountId = transferAccountId,
                    dateEpochMillis = dateEpochMillis
                )
            }.fold(
                onSuccess = { transaction ->
                    if (transactionId == null) addTransactionUseCase(transaction).map { } else updateTransactionUseCase(transaction)
                },
                onFailure = { Result.failure(it) }
            )
            _events.send(
                if (result.isSuccess) TransactionEvent.Saved else TransactionEvent.Failed(result.exceptionOrNull()?.message)
            )
        }
    }

    @Suppress("LongParameterList")
    private fun buildTransaction(
        id: Long,
        type: TransactionType,
        description: String,
        amountCents: Long,
        categoryId: Long,
        accountId: Long,
        transferAccountId: Long?,
        dateEpochMillis: Long
    ): Transaction = when (type) {
        TransactionType.INCOME -> Income(id, description, amountCents, categoryId, accountId, dateEpochMillis)
        TransactionType.EXPENSE -> Expense(id, description, amountCents, categoryId, accountId, dateEpochMillis)
        TransactionType.TRANSFER -> Transfer(
            id = id,
            description = description,
            amountCents = amountCents,
            categoryId = categoryId,
            accountId = accountId,
            transferAccountId = transferAccountId ?: throw IllegalArgumentException("Transfer destination is required"),
            dateEpochMillis = dateEpochMillis
        )
    }

    companion object {
        const val ARG_TRANSACTION_ID = "transactionId"
    }
}
