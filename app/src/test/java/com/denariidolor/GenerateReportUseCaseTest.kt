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
