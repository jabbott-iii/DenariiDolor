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

package com.denariidolor.data

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.data.export.ReportPdfRenderer
import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.model.ReportRow
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.report.ReportText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class ReportPdfRendererTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun report(rowCount: Int) = MonthlyReport(
        title = ReportText.TITLE,
        period = YearMonth.of(2026, 9),
        generatedAtEpochMillis = 0L,
        totalIncomeCents = 0L,
        totalExpenseCents = rowCount * 100L,
        netCents = -rowCount * 100L,
        rows = List(rowCount) { index ->
            ReportRow(index.toLong(), 0L, TransactionType.EXPENSE, "Category $index", "A long description that must be ellipsized $index", 100L, "Cash")
        }
    )

    private fun renderPageCount(rowCount: Int): Int {
        val file = File(context.cacheDir, "report-test.pdf")
        file.outputStream().use { ReportPdfRenderer.render(report(rowCount), it) }
        assertTrue(file.readBytes().copyOf(4).contentEquals("%PDF".toByteArray()))
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { it.pageCount }
        }.also { file.delete() }
    }

    @Test
    fun emptyReportRendersSinglePage() {
        assertEquals(1, renderPageCount(0))
    }

    @Test
    fun longReportPaginates() {
        assertTrue(renderPageCount(150) >= 4)
    }
}
