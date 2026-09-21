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

package com.denariidolor.presentation.ui

import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.presentation.ui.common.buildTransactionRows
import com.denariidolor.presentation.ui.dashboard.BudgetStatus
import com.denariidolor.presentation.ui.dashboard.budgetProgress
import com.denariidolor.presentation.ui.dashboard.budgetStatus
import com.denariidolor.presentation.ui.dashboard.compactMoney
import com.denariidolor.presentation.ui.dashboard.spendingByCategory
import com.denariidolor.presentation.ui.dashboard.summarize
import com.denariidolor.util.formatMoney
import com.denariidolor.util.formatSignedAmount
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class DashboardMappersTest {
    private val utc = ZoneId.of("UTC")
    private val categories = listOf(CategoryEntity(id = 1, name = "Dining"), CategoryEntity(id = 3, name = "Transfer"))
    private val accounts = listOf(AccountEntity(id = 1, name = "Cash", balanceCents = 0L), AccountEntity(id = 2, name = "Savings", balanceCents = 0L))

    private fun txn(id: Long, type: TransactionType, date: Long, transferTo: Long? = null, categoryId: Long = 1) = TransactionEntity(
        id = id,
        type = type,
        description = "T$id",
        amountCents = 1_000,
        categoryId = categoryId,
        accountId = 1,
        transferAccountId = transferTo,
        dateEpochMillis = date
    )

    @Test
    fun rowsAreNewestFirstAndLimited() {
        val rows = buildTransactionRows(
            listOf(txn(1, TransactionType.EXPENSE, 1_000), txn(2, TransactionType.EXPENSE, 3_000), txn(3, TransactionType.INCOME, 2_000)),
            categories,
            accounts,
            limit = 2,
            zoneId = utc
        )

        assertEquals(listOf(2L, 3L), rows.map { it.id })
    }

    @Test
    fun rowsResolveNamesAndTransferLabel() {
        val row = buildTransactionRows(listOf(txn(1, TransactionType.TRANSFER, 0, transferTo = 2, categoryId = 3)), categories, accounts, zoneId = utc).single()

        assertEquals("Transfer", row.categoryName)
        assertEquals("Cash → Savings", row.accountLabel)
        assertEquals("1970-01-01", row.dateText)
        assertEquals("$10.00", row.amountText)
    }

    @Test
    fun missingReferencesFallBackToIds() {
        val row = buildTransactionRows(listOf(txn(1, TransactionType.EXPENSE, 0, categoryId = 99)), emptyList(), emptyList(), zoneId = utc).single()

        assertEquals("#99", row.categoryName)
        assertEquals("#1", row.accountLabel)
    }

    @Test
    fun signedAmountsReflectType() {
        assertEquals("-$4.50", formatSignedAmount(TransactionType.EXPENSE, 450))
        assertEquals("+$1200.00", formatSignedAmount(TransactionType.INCOME, 120_000))
        assertEquals("$80.00", formatSignedAmount(TransactionType.TRANSFER, 8_000))
        assertEquals("$0.50", formatMoney(50))
        assertEquals("-$12.05", formatMoney(-1_205))
    }

    @Test
    fun summaryTreatsTransfersAsNetNeutral() {
        val summary = summarize(listOf(txn(1, TransactionType.INCOME, 0), txn(2, TransactionType.EXPENSE, 0), txn(3, TransactionType.TRANSFER, 0, transferTo = 2)))

        assertEquals(1_000L, summary.incomeCents)
        assertEquals(1_000L, summary.expenseCents)
        assertEquals(0L, summary.netCents)
    }

    @Test
    fun rowsCarryCategoryIcon() {
        val cats = listOf(CategoryEntity(id = 1, name = "Dining", iconName = "dining"))
        val row = buildTransactionRows(listOf(txn(1, TransactionType.EXPENSE, 0)), cats, accounts, zoneId = utc).single()

        assertEquals("dining", row.categoryIcon)
    }

    @Test
    fun budgetStatusThresholds() {
        assertEquals(BudgetStatus.OK, budgetStatus(7_999, 10_000, 80))
        assertEquals(BudgetStatus.WARNING, budgetStatus(8_000, 10_000, 80))
        assertEquals(BudgetStatus.WARNING, budgetStatus(10_000, 10_000, 80))
        assertEquals(BudgetStatus.OVER, budgetStatus(10_001, 10_000, 80))
    }

    @Test
    fun spendingSortsDescendingAndFoldsOther() {
        val cats = (1L..7L).map { CategoryEntity(id = it, name = "C$it") }
        val expenses = (1L..7L).map { txn(it, TransactionType.EXPENSE, 0, categoryId = it).copy(amountCents = it * 1_000) } + txn(99, TransactionType.INCOME, 0)

        val spending = spendingByCategory(expenses, cats, maxBars = 5)

        assertEquals(listOf(7L, 6L, 5L, 4L, 3L, null), spending.map { it.categoryId })
        assertEquals(3_000L, spending.last().amountCents)
    }

    @Test
    fun spendingWithoutFoldWhenFewCategories() {
        val spending = spendingByCategory(listOf(txn(1, TransactionType.EXPENSE, 0), txn(2, TransactionType.EXPENSE, 0)), categories)

        assertEquals(1, spending.size)
        assertEquals(2_000L, spending.single().amountCents)
    }

    @Test
    fun budgetProgressSumsMonthExpensesAndSortsByFraction() {
        val budgets = listOf(
            BudgetEntity(id = 1, categoryId = 1, monthlyLimitCents = 10_000),
            BudgetEntity(id = 2, categoryId = 3, monthlyLimitCents = 1_000, warningThresholdPercent = 50)
        )
        val progress = budgetProgress(budgets, categories, listOf(txn(1, TransactionType.EXPENSE, 0), txn(2, TransactionType.EXPENSE, 0), txn(3, TransactionType.INCOME, 0)))

        assertEquals(listOf(1L, 3L), progress.map { it.categoryId })
        assertEquals(2_000L, progress[0].spentCents)
        assertEquals(0L, progress[1].spentCents)
        assertEquals(BudgetStatus.OK, progress[0].status)
    }

    @Test
    fun compactMoneyAxisLabels() {
        assertEquals("$5", compactMoney(5f))
        assertEquals("$12.5", compactMoney(12.5f))
        assertEquals("$1.5K", compactMoney(1_500f))
        assertEquals("$2.0M", compactMoney(2_000_000f))
    }
}
