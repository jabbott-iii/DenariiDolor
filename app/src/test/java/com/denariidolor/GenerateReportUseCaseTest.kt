package com.denariidolor

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.report.ReportText
import com.denariidolor.domain.usecase.GenerateReportUseCase
import com.denariidolor.testutil.FakeAccountRepository
import com.denariidolor.testutil.FakeCategoryRepository
import com.denariidolor.testutil.FakeTransactionRepository
import com.denariidolor.testutil.TestData
import com.denariidolor.util.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class GenerateReportUseCaseTest {
    private val zone = ZoneId.of("UTC")
    private val clock = Clock.fixed(Instant.parse("2026-09-20T12:00:00Z"), zone)
    private val september = YearMonth.of(2026, 9)
    private val start = DateUtils.monthRangeEpochMillis(2026, 9, zone).first

    private val transactions = FakeTransactionRepository(
        listOf(
            TransactionEntity(id = 1, type = "EXPENSE", description = "Rent", amount = 900.0, categoryId = 4, accountId = 1, dateEpochMillis = start + 2_000),
            TransactionEntity(id = 2, type = "INCOME", description = "Salary", amount = 2000.0, categoryId = 2, accountId = 1, dateEpochMillis = start + 1_000),
            TransactionEntity(id = 3, type = "TRANSFER", description = "Savings", amount = 300.0, categoryId = 3, accountId = 1, transferAccountId = 2, dateEpochMillis = start + 3_000),
            TransactionEntity(id = 4, type = "EXPENSE", description = "Old", amount = 50.0, categoryId = 99, accountId = 1, dateEpochMillis = start - 1)
        )
    )
    private val useCase = GenerateReportUseCase(
        transactions,
        FakeCategoryRepository(TestData.categories),
        FakeAccountRepository(TestData.accounts),
        clock
    )

    @Test
    fun reportHasTitlePeriodTimestampAndTotals() = runBlocking {
        val report = useCase(september)

        assertEquals(ReportText.TITLE, report.title)
        assertEquals(september, report.period)
        assertEquals(clock.millis(), report.generatedAtEpochMillis)
        assertEquals(2000.0, report.totalIncome, 0.0001)
        assertEquals(900.0, report.totalExpense, 0.0001)
        assertEquals(1100.0, report.net, 0.0001)
    }

    @Test
    fun rowsAreChronologicalWithNamesAndPaymentMethod() = runBlocking {
        val rows = useCase(september).rows

        assertEquals(listOf(2L, 1L, 3L), rows.map { it.transactionId })
        assertEquals("Groceries", rows[1].categoryName)
        assertEquals("Cash", rows[1].paymentMethod)
        assertEquals("Cash → Savings", rows[2].paymentMethod)
    }

    @Test
    fun unknownCategoryFallsBackToId() = runBlocking {
        val august = useCase(YearMonth.of(2026, 8)).rows.single()

        assertEquals("#99", august.categoryName)
    }
}
