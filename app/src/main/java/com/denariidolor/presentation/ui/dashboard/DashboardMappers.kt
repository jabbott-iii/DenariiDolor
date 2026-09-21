package com.denariidolor.presentation.ui.dashboard

import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.toDomainTransaction
import com.denariidolor.presentation.ui.common.CategoryIcons
import com.denariidolor.presentation.ui.common.TransactionRow
import java.time.YearMonth

data class DashboardSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0
)

data class AccountBalance(val id: Long, val name: String, val balance: Double)

/** One bar in the spending chart. `categoryId == null` is the folded "Other" bucket. */
data class CategorySpend(val categoryId: Long?, val name: String?, val iconKey: String, val amount: Double)

enum class BudgetStatus { OK, WARNING, OVER }

data class BudgetProgress(
    val categoryId: Long,
    val categoryName: String,
    val iconKey: String,
    val spent: Double,
    val limit: Double,
    val warningPercent: Int
) {
    val fraction: Float get() = if (limit > 0) (spent / limit).toFloat() else 0f
    val status: BudgetStatus get() = budgetStatus(spent, limit, warningPercent)
}

data class DashboardUiState(
    val period: YearMonth? = null,
    val summary: DashboardSummary = DashboardSummary(),
    val spending: List<CategorySpend> = emptyList(),
    val budgets: List<BudgetProgress> = emptyList(),
    val balances: List<AccountBalance> = emptyList(),
    val recent: List<TransactionRow> = emptyList()
) {
    val budgetAlerts: Int get() = budgets.count { it.status != BudgetStatus.OK }
}

fun summarize(transactions: List<TransactionEntity>): DashboardSummary {
    val income = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val net = transactions.sumOf { it.toDomainTransaction().balanceImpact() }
    return DashboardSummary(income, expense, net)
}

fun budgetStatus(spent: Double, limit: Double, warningPercent: Int): BudgetStatus = when {
    spent > limit -> BudgetStatus.OVER
    spent >= limit * warningPercent / 100.0 -> BudgetStatus.WARNING
    else -> BudgetStatus.OK
}

/** Expense totals per category, largest first; anything past [maxBars] folds into one "Other" bar. */
fun spendingByCategory(
    monthTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    maxBars: Int = 5
): List<CategorySpend> {
    val categoriesById = categories.associateBy { it.id }
    val totals = monthTransactions
        .filter { it.type == "EXPENSE" }
        .groupBy { it.categoryId }
        .map { (categoryId, items) ->
            val category = categoriesById[categoryId]
            CategorySpend(categoryId, category?.name ?: "#$categoryId", category?.iconName ?: CategoryIcons.DEFAULT_KEY, items.sumOf { it.amount })
        }
        .sortedWith(compareByDescending<CategorySpend> { it.amount }.thenBy { it.name })
    if (totals.size <= maxBars) return totals
    val other = totals.drop(maxBars).sumOf { it.amount }
    return totals.take(maxBars) + CategorySpend(null, null, CategoryIcons.DEFAULT_KEY, other)
}

fun budgetProgress(
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    monthTransactions: List<TransactionEntity>
): List<BudgetProgress> {
    val categoriesById = categories.associateBy { it.id }
    val spentByCategory = monthTransactions
        .filter { it.type == "EXPENSE" }
        .groupBy { it.categoryId }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
    return budgets
        .map { budget ->
            val category = categoriesById[budget.categoryId]
            BudgetProgress(
                categoryId = budget.categoryId,
                categoryName = category?.name ?: "#${budget.categoryId}",
                iconKey = category?.iconName ?: CategoryIcons.DEFAULT_KEY,
                spent = spentByCategory[budget.categoryId] ?: 0.0,
                limit = budget.monthlyLimit,
                warningPercent = budget.warningThresholdPercent
            )
        }
        .sortedByDescending { it.fraction }
}
