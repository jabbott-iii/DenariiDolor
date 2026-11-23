/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.di.SessionScope
import com.denariidolor.domain.usecase.AccountUseCases
import com.denariidolor.presentation.ui.common.StringKey
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.util.Money
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
class AccountViewModel(private val accountUseCases: AccountUseCases, accountRepository: AccountRepository) : ViewModel() {
    val accounts: StateFlow<List<AccountEntity>> = accountRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    fun add(name: String, openingBalanceText: String) {
        val openingBalance = parseOpeningBalance(openingBalanceText)
        if (openingBalance == null) {
            viewModelScope.launch { _messages.send(UiMessage.Resource(StringKey.INVALID_OPENING_BALANCE)) }
            return
        }
        report(StringKey.ACCOUNT_SAVED) { accountUseCases.add(name, openingBalance) }
    }

    fun rename(id: Long, name: String) = report(StringKey.ACCOUNT_SAVED) { accountUseCases.rename(id, name) }

    fun delete(id: Long) = report(StringKey.ACCOUNT_DELETED) { accountUseCases.delete(id) }

    private fun report(success: StringKey, action: suspend () -> Result<*>) {
        viewModelScope.launch { _messages.send(UiMessage.fromResult(action(), success)) }
    }

    companion object {
        /** Blank means zero; otherwise at most 2 decimals, negative allowed (e.g. a credit card). */
        fun parseOpeningBalance(text: String): Long? = if (text.isBlank()) 0L else Money.parseToCents(text)
    }
}
