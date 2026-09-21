package com.denariidolor

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.usecase.GenerateReportUseCase
import com.denariidolor.testutil.FakeTransactionRepository
import com.denariidolor.util.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateReportUseCaseTest {
    @Test
    fun reportCalculatesIncomeExpenseAndCsv() = runBlocking {
        val date = DateUtils.monthRangeEpochMillis(2026, 9).first
        val repo = FakeTransactionRepository(
            listOf(
                TransactionEntity(id = 1, type = "INCOME", description = "Salary", amount = 2000.0, categoryId = 1, accountId = 1, dateEpochMillis = date),
                TransactionEntity(id = 2, type = "EXPENSE", description = "Rent", amount = 900.0, categoryId = 2, accountId = 1, dateEpochMillis = date),
                TransactionEntity(id = 3, type = "TRANSFER", description = "Savings", amount = 300.0, categoryId = 3, accountId = 1, transferAccountId = 2, dateEpochMillis = date)
            )
        )

        val report = GenerateReportUseCase(repo).invoke(2026, 9)

        assertEquals(2000.0, report.totalIncome, 0.0001)
        assertEquals(900.0, report.totalExpense, 0.0001)
        assertEquals(1100.0, report.net, 0.0001)
        assertTrue(report.csv.contains("Salary"))
        assertTrue(report.csv.contains("Rent"))
        assertTrue(report.csv.contains("Savings"))
    }
}
