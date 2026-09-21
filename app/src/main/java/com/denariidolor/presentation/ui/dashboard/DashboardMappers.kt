package com.denariidolor.presentation.ui.dashboard

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.toDomainTransaction
import com.denariidolor.presentation.ui.common.TransactionRow

data class DashboardSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0
)

data class AccountBalance(val id: Long, val name: String, val balance: Double)

data class DashboardUiState(
    val summary: DashboardSummary = DashboardSummary(),
    val balances: List<AccountBalance> = emptyList(),
    val recent: List<TransactionRow> = emptyList()
)

fun summarize(transactions: List<TransactionEntity>): DashboardSummary {
    val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val net = transactions.sumOf { it.toDomainTransaction().balanceImpact() }
    return DashboardSummary(income, expense, net)
}
