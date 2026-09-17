package com.denariidolor.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.toDomainTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    transactionRepository: TransactionRepository
) : ViewModel() {
    val summary = transactionRepository.getAll()
        .map { transactions ->
            val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val net = transactions.sumOf { it.toDomainTransaction().balanceImpact() }
            DashboardSummary(income, expense, net)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardSummary())
}

data class DashboardSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0
)
