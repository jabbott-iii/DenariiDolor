package com.denariidolor.presentation.ui

import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
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
    private val accounts = listOf(AccountEntity(id = 1, name = "Cash", balance = 0.0), AccountEntity(id = 2, name = "Savings", balance = 0.0))

    private fun txn(id: Long, type: String, date: Long, transferTo: Long? = null, categoryId: Long = 1) = TransactionEntity(
        id = id,
        type = type,
        description = "T$id",
        amount = 10.0,
        categoryId = categoryId,
        accountId = 1,
        transferAccountId = transferTo,
        dateEpochMillis = date
    )

    @Test
    fun rowsAreNewestFirstAndLimited() {
        val rows = buildTransactionRows(
            listOf(txn(1, "EXPENSE", 1_000), txn(2, "EXPENSE", 3_000), txn(3, "INCOME", 2_000)),
            categories,
            accounts,
            limit = 2,
            zoneId = utc
        )

        assertEquals(listOf(2L, 3L), rows.map { it.id })
    }

    @Test
    fun rowsResolveNamesAndTransferLabel() {
        val row = buildTransactionRows(listOf(txn(1, "TRANSFER", 0, transferTo = 2, categoryId = 3)), categories, accounts, zoneId = utc).single()

        assertEquals("Transfer", row.categoryName)
        assertEquals("Cash → Savings", row.accountLabel)
        assertEquals("1970-01-01", row.dateText)
        assertEquals("$10.00", row.amountText)
    }

    @Test
    fun missingReferencesFallBackToIds() {
        val row = buildTransactionRows(listOf(txn(1, "EXPENSE", 0, categoryId = 99)), emptyList(), emptyList(), zoneId = utc).single()

        assertEquals("#99", row.categoryName)
        assertEquals("#1", row.accountLabel)
    }

    @Test
    fun signedAmountsReflectType() {
        assertEquals("-$4.50", formatSignedAmount("EXPENSE", 4.5))
        assertEquals("+$1200.00", formatSignedAmount("INCOME", 1200.0))
        assertEquals("$80.00", formatSignedAmount("TRANSFER", 80.0))
        assertEquals("$0.50", formatMoney(0.5))
    }

    @Test
    fun summaryTreatsTransfersAsNetNeutral() {
        val summary = summarize(listOf(txn(1, "INCOME", 0), txn(2, "EXPENSE", 0), txn(3, "TRANSFER", 0, transferTo = 2)))

        assertEquals(10.0, summary.income, 0.0001)
        assertEquals(10.0, summary.expense, 0.0001)
        assertEquals(0.0, summary.net, 0.0001)
    }

    @Test
    fun rowsCarryCategoryIcon() {
        val cats = listOf(CategoryEntity(id = 1, name = "Dining", iconName = "dining"))
        val row = buildTransactionRows(listOf(txn(1, "EXPENSE", 0)), cats, accounts, zoneId = utc).single()

        assertEquals("dining", row.categoryIcon)
    }

    @Test
    fun budgetStatusThresholds() {
        assertEquals(BudgetStatus.OK, budgetStatus(79.0, 100.0, 80))
        assertEquals(BudgetStatus.WARNING, budgetStatus(80.0, 100.0, 80))
        assertEquals(BudgetStatus.WARNING, budgetStatus(100.0, 100.0, 80))
        assertEquals(BudgetStatus.OVER, budgetStatus(100.01, 100.0, 80))
    }

    @Test
    fun spendingSortsDescendingAndFoldsOther() {
        val cats = (1L..7L).map { CategoryEntity(id = it, name = "C$it") }
        val expenses = (1L..7L).map { txn(it, "EXPENSE", 0, categoryId = it).copy(amount = it * 10.0) } + txn(99, "INCOME", 0)

        val spending = spendingByCategory(expenses, cats, maxBars = 5)

        assertEquals(listOf(7L, 6L, 5L, 4L, 3L, null), spending.map { it.categoryId })
        assertEquals(30.0, spending.last().amount, 0.0001)
    }

    @Test
    fun spendingWithoutFoldWhenFewCategories() {
        val spending = spendingByCategory(listOf(txn(1, "EXPENSE", 0), txn(2, "EXPENSE", 0)), categories)

        assertEquals(1, spending.size)
        assertEquals(20.0, spending.single().amount, 0.0001)
    }

    @Test
    fun budgetProgressSumsMonthExpensesAndSortsByFraction() {
        val budgets = listOf(
            BudgetEntity(id = 1, categoryId = 1, monthlyLimit = 100.0),
            BudgetEntity(id = 2, categoryId = 3, monthlyLimit = 10.0, warningThresholdPercent = 50)
        )
        val progress = budgetProgress(budgets, categories, listOf(txn(1, "EXPENSE", 0), txn(2, "EXPENSE", 0), txn(3, "INCOME", 0)))

        assertEquals(listOf(1L, 3L), progress.map { it.categoryId })
        assertEquals(20.0, progress[0].spent, 0.0001)
        assertEquals(0.0, progress[1].spent, 0.0001)
        assertEquals(BudgetStatus.OK, progress[0].status)
    }

    @Test
    fun compactMoneyAxisLabels() {
        assertEquals("$5", compactMoney(5f))
        assertEquals("$1.5K", compactMoney(1_500f))
        assertEquals("$2.0M", compactMoney(2_000_000f))
    }
}
