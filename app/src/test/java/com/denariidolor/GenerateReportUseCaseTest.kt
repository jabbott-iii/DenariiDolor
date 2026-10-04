/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.report.ReportText
import com.denariidolor.domain.usecase.GenerateReportUseCase
import com.denariidolor.testutil.FakeAccountRepository
import com.denariidolor.testutil.FakeCategoryRepository
import com.denariidolor.testutil.FakeTransactionRepository
import com.denariidolor.testutil.TestData
import com.denariidolor.util.DateUtils
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GenerateReportUseCaseTest {
    private val zone = ZoneId.of("UTC")
    private val clock = Clock.fixed(Instant.parse("2026-09-20T12:00:00Z"), zone)
    private val september = YearMonth.of(2026, 9)
    private val start = DateUtils.monthRangeEpochMillis(2026, 9, zone).first

    private val transactions = FakeTransactionRepository(
        listOf(
            TransactionEntity(
                id = 1,
                type = TransactionType.EXPENSE,
                description = "Rent",
                amountCents = 90_000,
                categoryId = 4,
                accountId = 1,
                dateEpochMillis =
                start + 2_000
            ),
            TransactionEntity(
                id = 2,
                type = TransactionType.INCOME,
                description = "Salary",
                amountCents = 200_000,
                categoryId = 2,
                accountId = 1,
                dateEpochMillis =
                start + 1_000
            ),
            TransactionEntity(
                id = 3,
                type = TransactionType.TRANSFER,
                description = "Savings",
                amountCents = 30_000,
                categoryId = 3,
                accountId = 1,
                transferAccountId = 2,
                dateEpochMillis =
                start + 3_000
            ),
            TransactionEntity(
                id = 4,
                type = TransactionType.EXPENSE,
                description = "Old",
                amountCents = 5_000,
                categoryId = 99,
                accountId = 1,
                dateEpochMillis =
                start - 1
            )
        )
    )
    private val useCase = GenerateReportUseCase(
        transactions,
        FakeCategoryRepository(TestData.categories),
        FakeAccountRepository(TestData.accounts),
        clock
    )

    @Test
    fun reportHasTitlePeriodTimestampAndTotals() = runBlocking<Unit> {
        val report = useCase(september)

        assertEquals(ReportText.TITLE, report.title)
        assertEquals(september, report.period)
        assertEquals(clock.millis(), report.generatedAtEpochMillis)
        assertEquals(200_000L, report.totalIncomeCents)
        assertEquals(90_000L, report.totalExpenseCents)
        assertEquals(110_000L, report.netCents)
    }

    @Test
    fun rowsAreChronologicalWithNamesAndPaymentMethod() = runBlocking<Unit> {
        val rows = useCase(september).rows

        assertEquals(listOf(2L, 1L, 3L), rows.map { it.transactionId })
        assertEquals("Groceries", rows[1].categoryName)
        assertEquals("Cash", rows[1].paymentMethod)
        assertEquals("Cash → Savings", rows[2].paymentMethod)
    }

    @Test
    fun unknownCategoryFallsBackToId() = runBlocking<Unit> {
        val august = useCase(YearMonth.of(2026, 8)).rows.single()

        assertEquals("#99", august.categoryName)
    }
}
