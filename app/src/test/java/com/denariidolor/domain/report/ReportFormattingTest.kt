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

package com.denariidolor.domain.report

import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.model.ReportRow
import com.denariidolor.domain.model.TransactionType
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportFormattingTest {
    private val utc = ZoneId.of("UTC")
    private val report = MonthlyReport(
        title = ReportText.TITLE,
        period = YearMonth.of(2026, 9),
        generatedAtEpochMillis = 1_789_992_000_000L,
        totalIncomeCents = 200_000,
        totalExpenseCents = 90_050,
        netCents = 109_950,
        rows = listOf(
            ReportRow(1, 1_788_220_800_000L, TransactionType.EXPENSE, "Dining", "Lunch, \"team\"", 1_250, "Cash"),
            ReportRow(2, 1_788_220_800_000L, TransactionType.EXPENSE, "Misc", "=HYPERLINK(\"x\")", 100, "Visa")
        )
    )

    @Test
    fun labelsAndFileName() {
        assertEquals("September 2026", ReportText.periodLabel(YearMonth.of(2026, 9), Locale.US))
        assertEquals("spending-report-2026-09.pdf", ReportText.fileName(YearMonth.of(2026, 9), "pdf"))
    }

    @Test
    fun generatedLabelFormatsInZone() {
        assertEquals("1970-01-01 00:00 UTC", ReportText.generatedLabel(0L, utc))
    }

    @Test
    fun csvContainsMetadataHeaderAndRows() {
        val lines = ReportCsvFormatter.format(report, utc, Locale.US).split("\r\n")

        assertEquals("title,Monthly Spending Report", lines[0])
        assertEquals("period,September 2026", lines[1])
        assertTrue(lines[2].startsWith("generated_at,"))
        assertEquals("total_expense,900.50", lines[4])
        assertEquals("date,type,category,description,amount,payment_method", lines[7])
        assertEquals("2026-09-01,EXPENSE,Dining,\"Lunch, \"\"team\"\"\",12.50,Cash", lines[8])
    }

    @Test
    fun csvNeutralizesFormulaInjection() {
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", ReportCsvFormatter.escape("=HYPERLINK(\"x\")"))
        assertEquals("\"'@cmd\"", ReportCsvFormatter.escape("@cmd"))
        assertEquals("-12.50", ReportCsvFormatter.escape("-12.50"))
        assertEquals("plain", ReportCsvFormatter.escape("plain"))
    }
}
