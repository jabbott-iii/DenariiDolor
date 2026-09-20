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
    companion object {
        const val STATUS_SAVED = "Saved"
    }

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
            val result = runCatching {
                buildTransaction(
                    type = type.trim().uppercase(),
                    description = description,
                    amount = amount,
                    categoryId = categoryId,
                    accountId = accountId,
                    transferAccountId = transferAccountId,
                    dateEpochMillis = dateEpochMillis
                )
            }.fold(
                onSuccess = { transaction -> addTransactionUseCase(transaction) },
                onFailure = { Result.failure(it) }
            )
            _status.value = if (result.isSuccess) STATUS_SAVED else (result.exceptionOrNull()?.message ?: "Error")
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
                transferAccountId = transferAccountId
                    ?: throw IllegalArgumentException("Transfer destination is required"),
                dateEpochMillis = dateEpochMillis
            )
            "EXPENSE" -> Expense(description = description, amount = amount, categoryId = categoryId, accountId = accountId, dateEpochMillis = dateEpochMillis)
            else -> throw IllegalArgumentException("Unsupported transaction type")
        }
    }
}
