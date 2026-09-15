package com.denariidolor.presentation.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.Expense
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

    fun addExpense(description: String, amount: Double, categoryId: Long, accountId: Long, dateEpochMillis: Long) {
        viewModelScope.launch {
            val result = addTransactionUseCase(
                Expense(
                    description = description,
                    amount = amount,
                    categoryId = categoryId,
                    accountId = accountId,
                    dateEpochMillis = dateEpochMillis
                )
            )
            _status.value = if (result.isSuccess) "Saved" else (result.exceptionOrNull()?.message ?: "Error")
        }
    }

    fun toEntity(transaction: TransactionEntity): TransactionEntity = transaction
}
