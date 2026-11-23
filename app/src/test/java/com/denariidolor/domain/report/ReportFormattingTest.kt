/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.report

import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.model.ReportRow
import com.denariidolor.domain.model.TransactionType
import java.util.Locale
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportFormattingTest {
    private val utc = TimeZone.of("UTC")
    private val report = MonthlyReport(
        title = ReportText.TITLE,
        period = YearMonth(2026, 9),
        generatedAtEpochMillis = 1_789_992_000_000L,
        timeZone = TimeZone.of("UTC"),
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
        assertEquals("September 2026", ReportText.periodLabel(YearMonth(2026, 9), Locale.US))
        assertEquals("spending-report-2026-09.pdf", ReportText.fileName(YearMonth(2026, 9), "pdf"))
    }

    @Test
    fun generatedLabelFormatsInZone() {
        assertEquals("1970-01-01 00:00 UTC", ReportText.generatedLabel(0L, utc))
    }

    @Test
    fun generatedLabelNamesTheZoneAsTheDeviceDoes() {
        val newYork = TimeZone.of("America/New_York")

        assertEquals("2026-07-01 08:00 EDT", ReportText.generatedLabel(1_782_907_200_000L, newYork))
        assertEquals("2026-01-01 07:00 EST", ReportText.generatedLabel(1_767_268_800_000L, newYork))
        assertEquals("2026-09-01 05:30 IST", ReportText.generatedLabel(1_788_220_800_000L, TimeZone.of("Asia/Kolkata")))
    }

    @Test
    fun csvContainsMetadataHeaderAndRows() {
        val lines = ReportCsvFormatter.format(report, Locale.US).split("\r\n")

        assertEquals("title,Monthly Spending Report", lines[0])
        assertEquals("period,September 2026", lines[1])
        assertEquals("generated_at,2026-09-21 12:00 UTC", lines[2])
        assertEquals("total_expense,900.50", lines[4])
        assertEquals("date,type,category,description,amount,payment_method", lines[7])
        assertEquals("2026-09-01,EXPENSE,Dining,\"Lunch, \"\"team\"\"\",12.50,Cash", lines[8])
    }

    @Test
    fun csvWritesTimesAndDatesInTheReportsZone() {
        // 12:00 UTC on Sep 21 is 05:00 in Phoenix, and midnight UTC on Sep 1 is still Aug 31 there.
        val lines = ReportCsvFormatter.format(report.copy(timeZone = TimeZone.of("America/Phoenix")), Locale.US).split("\r\n")

        assertTrue(lines[2], lines[2].startsWith("generated_at,2026-09-21 05:00 "))
        assertTrue(lines[8], lines[8].startsWith("2026-08-31,EXPENSE,Dining,"))
    }

    @Test
    fun csvNeutralizesFormulaInjection() {
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", ReportCsvFormatter.escape("=HYPERLINK(\"x\")"))
        assertEquals("\"'@cmd\"", ReportCsvFormatter.escape("@cmd"))
        assertEquals("-12.50", ReportCsvFormatter.escape("-12.50"))
        assertEquals("plain", ReportCsvFormatter.escape("plain"))
    }

    @Test
    fun csvNeutralizesFormulasAfterWhitespaceSemicolonsAndTabs() {
        assertEquals("\"x;'=1+1\"", ReportCsvFormatter.escape("x;=1+1"))
        assertEquals("\"x\t'=1+1\"", ReportCsvFormatter.escape("x\t=1+1"))
        assertEquals("\"' =1+1\"", ReportCsvFormatter.escape(" =1+1"))
        assertEquals("\"Lunch; '@cmd; ok\"", ReportCsvFormatter.escape("Lunch; @cmd; ok"))
        assertEquals("\"Refund;-5\"", ReportCsvFormatter.escape("Refund;-5"))
        assertEquals("\"a;b\"", ReportCsvFormatter.escape("a;b"))
    }
}
