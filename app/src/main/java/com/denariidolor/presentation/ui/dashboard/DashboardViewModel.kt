package com.denariidolor.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.R
import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.usecase.DeleteTransactionUseCase
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

@HiltViewModel
class DashboardViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.getAll(),
        categoryRepository.getAll(),
        accountRepository.getAll()
    ) { transactions, categories, accounts ->
        DashboardUiState(
            summary = summarize(transactions),
            balances = accounts.map { AccountBalance(it.id, it.name, it.balance) },
            recent = buildTransactionRows(transactions, categories, accounts, RECENT_LIMIT)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            _messages.send(UiMessage.fromResult(deleteTransactionUseCase(id), R.string.transaction_deleted))
        }
    }

    private companion object {
        const val RECENT_LIMIT = 20
    }
}
