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
import com.denariidolor.domain.model.Transfer
import com.denariidolor.domain.usecase.AddTransactionUseCase
import com.denariidolor.domain.usecase.UpdateTransactionUseCase
import com.denariidolor.presentation.ui.common.PickerOption
import com.denariidolor.util.runSuspendCatching
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

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
        amount: Double,
        categoryId: Long,
        accountId: Long,
        transferAccountId: Long?,
        dateEpochMillis: Long
    ) {
        viewModelScope.launch {
            val result = runSuspendCatching {
                buildTransaction(
                    id = transactionId ?: 0L,
                    type = type.trim().uppercase(),
                    description = description,
                    amount = amount,
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

    private fun buildTransaction(
        id: Long,
        type: String,
        description: String,
        amount: Double,
        categoryId: Long,
        accountId: Long,
        transferAccountId: Long?,
        dateEpochMillis: Long
    ): Transaction {
        return when (type) {
            "INCOME" -> Income(id, description, amount, categoryId, accountId, dateEpochMillis)
            "EXPENSE" -> Expense(id, description, amount, categoryId, accountId, dateEpochMillis)
            "TRANSFER" -> Transfer(
                id = id,
                description = description,
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                transferAccountId = transferAccountId ?: throw IllegalArgumentException("Transfer destination is required"),
                dateEpochMillis = dateEpochMillis
            )
            else -> throw IllegalArgumentException("Unsupported transaction type")
        }
    }

    companion object {
        const val ARG_TRANSACTION_ID = "transactionId"
    }
}
