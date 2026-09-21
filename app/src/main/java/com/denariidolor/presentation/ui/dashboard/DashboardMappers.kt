package com.denariidolor.presentation.ui.dashboard

import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.toDomainTransaction
import com.denariidolor.util.DateUtils
import java.time.ZoneId
import java.util.Locale

data class DashboardSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0
)

data class AccountBalance(val id: Long, val name: String, val balance: Double)

data class TransactionRow(
    val id: Long,
    val description: String,
    val type: String,
    val amountText: String,
    val categoryName: String,
    val accountLabel: String,
    val dateText: String
)

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

fun buildTransactionRows(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    limit: Int = Int.MAX_VALUE,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<TransactionRow> {
    val categoryNames = categories.associate { it.id to it.name }
    val accountNames = accounts.associate { it.id to it.name }
    fun accountName(id: Long) = accountNames[id] ?: "#$id"

    return transactions
        .sortedWith(compareByDescending<TransactionEntity> { it.dateEpochMillis }.thenByDescending { it.id })
        .take(limit.coerceAtLeast(0))
        .map { transaction ->
            val source = accountName(transaction.accountId)
            TransactionRow(
                id = transaction.id,
                description = transaction.description,
                type = transaction.type,
                amountText = formatSignedAmount(transaction.type, transaction.amount),
                categoryName = categoryNames[transaction.categoryId] ?: "#${transaction.categoryId}",
                accountLabel = transaction.transferAccountId?.let { "$source → ${accountName(it)}" } ?: source,
                dateText = DateUtils.formatLocalDate(transaction.dateEpochMillis, zoneId)
            )
        }
}

fun formatSignedAmount(type: String, amount: Double): String {
    val sign = when (type) {
        "EXPENSE" -> "-"
        "INCOME" -> "+"
        else -> ""
    }
    return sign + String.format(Locale.US, "$%.2f", amount)
}
