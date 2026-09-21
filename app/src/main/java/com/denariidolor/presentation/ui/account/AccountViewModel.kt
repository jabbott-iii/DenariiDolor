package com.denariidolor.presentation.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.R
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.domain.usecase.AccountUseCases
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
class AccountViewModel @Inject constructor(
    private val accountUseCases: AccountUseCases,
    accountRepository: AccountRepository
) : ViewModel() {
    val accounts: StateFlow<List<AccountEntity>> = accountRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    fun add(name: String, openingBalanceText: String) {
        val openingBalance = parseOpeningBalance(openingBalanceText)
        if (openingBalance == null) {
            viewModelScope.launch { _messages.send(UiMessage.Resource(R.string.invalid_opening_balance)) }
            return
        }
        report(R.string.account_saved) { accountUseCases.add(name, openingBalance) }
    }

    fun rename(id: Long, name: String) = report(R.string.account_saved) { accountUseCases.rename(id, name) }

    fun delete(id: Long) = report(R.string.account_deleted) { accountUseCases.delete(id) }

    private fun report(successResId: Int, action: suspend () -> Result<*>) {
        viewModelScope.launch { _messages.send(UiMessage.fromResult(action(), successResId)) }
    }

    companion object {
        fun parseOpeningBalance(text: String): Double? =
            text.trim().ifEmpty { "0" }.toDoubleOrNull()?.takeIf { it.isFinite() }
    }
}
