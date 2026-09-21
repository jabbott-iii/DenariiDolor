package com.denariidolor.data

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.data.export.ReportPdfRenderer
import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.model.ReportRow
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
        totalIncome = 0.0,
        totalExpense = rowCount.toDouble(),
        net = -rowCount.toDouble(),
        rows = List(rowCount) { index ->
            ReportRow(index.toLong(), 0L, "EXPENSE", "Category $index", "A long description that must be ellipsized $index", 1.0, "Cash")
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
