/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
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
import com.denariidolor.domain.usecase.BudgetOverage
import com.denariidolor.domain.usecase.DomainError
import com.denariidolor.domain.usecase.DomainException
import com.denariidolor.domain.usecase.UpdateTransactionUseCase
import com.denariidolor.presentation.ui.common.PickerOption
import com.denariidolor.presentation.ui.common.UiMessage
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
    data class Failed(val message: UiMessage) : TransactionEvent
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

    private val _isSaving = MutableStateFlow(false)

    /** True while a save is in flight, and after a successful edit while the screen closes (BUG-02). */
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _overBudget = MutableStateFlow<BudgetOverage?>(null)

    /** Set while the form asks whether to save an expense that goes past its category's budget. */
    val overBudget: StateFlow<BudgetOverage?> = _overBudget.asStateFlow()

    // The transaction waiting on that answer; kept here so the dialog survives a configuration change.
    private var pendingOverBudget: Transaction? = null

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
        if (_isSaving.value || _overBudget.value != null) return
        _isSaving.value = true
        viewModelScope.launch {
            val transaction = runSuspendCatching {
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
            }.getOrElse { error ->
                finishSave(Result.failure(error))
                return@launch
            }
            val result = persist(transaction, allowOverBudget = false)
            val overage = result.exceptionOrNull()?.budgetOverage()
            if (overage == null) {
                finishSave(result)
            } else {
                // Ask instead of failing: the user may really have spent it (a budget is a target, not a hard limit).
                pendingOverBudget = transaction
                _overBudget.value = overage
                _isSaving.value = false
            }
        }
    }

    /** Saves the expense the over-budget prompt asked about. */
    fun confirmOverBudget() {
        val transaction = pendingOverBudget ?: return
        if (_isSaving.value) return
        pendingOverBudget = null
        _overBudget.value = null
        _isSaving.value = true
        viewModelScope.launch { finishSave(persist(transaction, allowOverBudget = true)) }
    }

    /** Leaves the form as it was, so the amount or category can be changed. */
    fun dismissOverBudget() {
        pendingOverBudget = null
        _overBudget.value = null
    }

    private suspend fun persist(transaction: Transaction, allowOverBudget: Boolean): Result<Unit> = if (transactionId == null) {
        addTransactionUseCase(transaction, allowOverBudget).map { }
    } else {
        updateTransactionUseCase(transaction, allowOverBudget)
    }

    private suspend fun finishSave(result: Result<Unit>) {
        if (result.isFailure || !isEditMode) _isSaving.value = false
        _events.send(result.exceptionOrNull()?.let { TransactionEvent.Failed(UiMessage.fromError(it)) } ?: TransactionEvent.Saved)
    }

    private fun Throwable.budgetOverage(): BudgetOverage? =
        (this as? DomainException)?.takeIf { it.error == DomainError.BUDGET_EXCEEDED }?.arg as? BudgetOverage

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
            transferAccountId = transferAccountId
                ?: throw DomainException(DomainError.TRANSFER_DESTINATION_REQUIRED, "Transfer destination is required"),
            dateEpochMillis = dateEpochMillis
        )
    }

    companion object {
        const val ARG_TRANSACTION_ID = "transactionId"
    }
}
