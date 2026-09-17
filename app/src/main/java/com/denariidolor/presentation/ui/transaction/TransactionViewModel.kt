package com.denariidolor.presentation.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.domain.model.Expense
import com.denariidolor.domain.model.Income
import com.denariidolor.domain.model.Transaction
import com.denariidolor.domain.model.Transfer
import com.denariidolor.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    fun addTransaction(
        type: String,
        description: String,
        amount: Double,
        categoryId: Long,
        accountId: Long,
        transferAccountId: Long?,
        dateEpochMillis: Long
    ) {
        viewModelScope.launch {
            val result = addTransactionUseCase(
                buildTransaction(
                    type = type,
                    description = description,
                    amount = amount,
                    categoryId = categoryId,
                    accountId = accountId,
                    transferAccountId = transferAccountId,
                    dateEpochMillis = dateEpochMillis
                )
            )
            _status.value = if (result.isSuccess) "Saved" else (result.exceptionOrNull()?.message ?: "Error")
        }
    }

    private fun buildTransaction(
        type: String,
        description: String,
        amount: Double,
        categoryId: Long,
        accountId: Long,
        transferAccountId: Long?,
        dateEpochMillis: Long
    ): Transaction {
        return when (type) {
            "INCOME" -> Income(description = description, amount = amount, categoryId = categoryId, accountId = accountId, dateEpochMillis = dateEpochMillis)
            "TRANSFER" -> Transfer(
                description = description,
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                transferAccountId = transferAccountId,
                dateEpochMillis = dateEpochMillis
            )
            else -> Expense(description = description, amount = amount, categoryId = categoryId, accountId = accountId, dateEpochMillis = dateEpochMillis)
        }
    }
}
