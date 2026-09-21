/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.denariidolor.presentation.ui.dashboard

import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.model.toDomainTransaction
import com.denariidolor.presentation.ui.common.CategoryIcons
import com.denariidolor.presentation.ui.common.TransactionRow
import java.time.YearMonth

data class DashboardSummary(
    val incomeCents: Long = 0L,
    val expenseCents: Long = 0L,
    val netCents: Long = 0L
)

data class AccountBalance(val id: Long, val name: String, val balanceCents: Long)

/** One bar in the spending chart. `categoryId == null` is the folded "Other" bucket. */
data class CategorySpend(val categoryId: Long?, val name: String?, val iconKey: String, val amountCents: Long)

enum class BudgetStatus { OK, WARNING, OVER }

data class BudgetProgress(
    val categoryId: Long,
    val categoryName: String,
    val iconKey: String,
    val spentCents: Long,
    val limitCents: Long,
    val warningPercent: Int
) {
    val fraction: Float get() = if (limitCents > 0) (spentCents.toDouble() / limitCents).toFloat() else 0f
    val status: BudgetStatus get() = budgetStatus(spentCents, limitCents, warningPercent)
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
    val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
    val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
    val net = transactions.sumOf { it.toDomainTransaction().balanceImpact() }
    return DashboardSummary(income, expense, net)
}

/** Integer math: `spent * 100 >= limit * percent` avoids rounding at the threshold. */
fun budgetStatus(spentCents: Long, limitCents: Long, warningPercent: Int): BudgetStatus = when {
    spentCents > limitCents -> BudgetStatus.OVER
    spentCents * 100 >= limitCents * warningPercent -> BudgetStatus.WARNING
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
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.categoryId }
        .map { (categoryId, items) ->
            val category = categoriesById[categoryId]
            CategorySpend(categoryId, category?.name ?: "#$categoryId", category?.iconName ?: CategoryIcons.DEFAULT_KEY, items.sumOf { it.amountCents })
        }
        .sortedWith(compareByDescending<CategorySpend> { it.amountCents }.thenBy { it.name })
    if (totals.size <= maxBars) return totals
    val other = totals.drop(maxBars).sumOf { it.amountCents }
    return totals.take(maxBars) + CategorySpend(null, null, CategoryIcons.DEFAULT_KEY, other)
}

fun budgetProgress(
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    monthTransactions: List<TransactionEntity>
): List<BudgetProgress> {
    val categoriesById = categories.associateBy { it.id }
    val spentByCategory = monthTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.categoryId }
        .mapValues { (_, items) -> items.sumOf { it.amountCents } }
    return budgets
        .map { budget ->
            val category = categoriesById[budget.categoryId]
            BudgetProgress(
                categoryId = budget.categoryId,
                categoryName = category?.name ?: "#${budget.categoryId}",
                iconKey = category?.iconName ?: CategoryIcons.DEFAULT_KEY,
                spentCents = spentByCategory[budget.categoryId] ?: 0L,
                limitCents = budget.monthlyLimitCents,
                warningPercent = budget.warningThresholdPercent
            )
        }
        .sortedByDescending { it.fraction }
}
