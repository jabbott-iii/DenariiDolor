package com.denariidolor.presentation.ui

import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.presentation.ui.dashboard.buildTransactionRows
import com.denariidolor.presentation.ui.dashboard.formatSignedAmount
import com.denariidolor.presentation.ui.dashboard.summarize
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
    }

    @Test
    fun summaryTreatsTransfersAsNetNeutral() {
        val summary = summarize(listOf(txn(1, "INCOME", 0), txn(2, "EXPENSE", 0), txn(3, "TRANSFER", 0, transferTo = 2)))

        assertEquals(10.0, summary.income, 0.0001)
        assertEquals(10.0, summary.expense, 0.0001)
        assertEquals(0.0, summary.net, 0.0001)
    }
}
