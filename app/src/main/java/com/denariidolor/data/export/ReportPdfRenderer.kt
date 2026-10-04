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

package com.denariidolor.data.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.report.ReportText
import com.denariidolor.util.DateUtils
import com.denariidolor.util.formatMoney
import com.denariidolor.util.formatSignedAmount
import java.io.OutputStream
import java.time.ZoneId

/** The PDF's words. [ReportExporter] fills them from string resources; the defaults are the English ones. */
data class ReportPdfLabels(
    val title: String = ReportText.TITLE,
    val columns: List<String> = listOf("Date", "Type", "Category", "Description", "Amount", "Payment Method"),
    val type: (TransactionType) -> String = { it.name },
    val empty: String = "No transactions in this month.",
    val generated: (String) -> String = { "Generated $it" },
    val totals: (income: String, expense: String, net: String) -> String = { income, expense, net ->
        "Income $income · Expense $expense · Net $net"
    },
    val page: (Int) -> String = { "Page $it" }
)

/** Renders a [MonthlyReport] as a paginated US-Letter PDF table using the platform [PdfDocument]. */
@Suppress("MagicNumber") // Layout coordinates and font sizes in PDF points; naming each would hurt readability.
object ReportPdfRenderer {
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 36f
    private const val ROW_HEIGHT = 18f
    private const val CELL_PADDING = 4f

    private data class Column(val width: Float, val alignRight: Boolean = false)

    // Date, type, category, description, amount, payment method; the headers come from ReportPdfLabels.columns.
    private val columns = listOf(
        Column(64f),
        Column(62f),
        Column(90f),
        Column(150f),
        Column(70f, alignRight = true),
        Column(104f)
    )

    fun render(
        report: MonthlyReport,
        out: OutputStream,
        zoneId: ZoneId = ZoneId.systemDefault(),
        labels: ReportPdfLabels = ReportPdfLabels()
    ) {
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
        }
        val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f }
        val linePaint = Paint().apply { strokeWidth = 0.5f }

        val document = PdfDocument()
        try {
            var pageNumber = 0
            var page: PdfDocument.Page? = null
            var y = 0f

            fun finishPage() {
                page?.let { current ->
                    val pageLabel = labels.page(pageNumber)
                    val pageX = PAGE_WIDTH - MARGIN - metaPaint.measureText(pageLabel)
                    current.canvas.drawText(pageLabel, pageX, PAGE_HEIGHT - MARGIN / 2, metaPaint)
                    document.finishPage(current)
                }
            }

            fun startPage() {
                finishPage()
                pageNumber++
                val newPage = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                page = newPage
                y = MARGIN
                if (pageNumber == 1) {
                    y = drawHeader(newPage.canvas, report, zoneId, labels, y, titlePaint, metaPaint)
                }
                y = drawRow(newPage.canvas, labels.columns, y, headerPaint)
                newPage.canvas.drawLine(MARGIN, y - ROW_HEIGHT + 4f, PAGE_WIDTH - MARGIN, y - ROW_HEIGHT + 4f, linePaint)
            }

            startPage()
            if (report.rows.isEmpty()) {
                page!!.canvas.drawText(labels.empty, MARGIN, y + ROW_HEIGHT, metaPaint)
            }
            report.rows.forEach { row ->
                if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) startPage()
                y = drawRow(
                    page!!.canvas,
                    listOf(
                        DateUtils.formatLocalDate(row.dateEpochMillis, zoneId),
                        labels.type(row.type),
                        row.categoryName,
                        row.description,
                        formatSignedAmount(row.type, row.amountCents),
                        row.paymentMethod
                    ),
                    y,
                    cellPaint
                )
            }
            finishPage()
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    private fun drawHeader(
        canvas: Canvas,
        report: MonthlyReport,
        zoneId: ZoneId,
        labels: ReportPdfLabels,
        startY: Float,
        titlePaint: TextPaint,
        metaPaint: TextPaint
    ): Float {
        var y = startY + titlePaint.textSize
        canvas.drawText("${labels.title} — ${ReportText.periodLabel(report.period)}", MARGIN, y, titlePaint)
        y += 16f
        canvas.drawText(labels.generated(ReportText.generatedLabel(report.generatedAtEpochMillis, zoneId)), MARGIN, y, metaPaint)
        y += 14f
        val totals = labels.totals(
            formatMoney(report.totalIncomeCents),
            formatMoney(report.totalExpenseCents),
            formatMoney(report.netCents)
        )
        canvas.drawText(totals, MARGIN, y, metaPaint)
        return y + 20f
    }

    private fun drawRow(canvas: Canvas, cells: List<String>, y: Float, paint: TextPaint): Float {
        var x = MARGIN
        val baseline = y + ROW_HEIGHT - 6f
        cells.forEachIndexed { index, text ->
            val column = columns[index]
            val available = column.width - CELL_PADDING * 2
            val fitted = TextUtils.ellipsize(text, paint, available, TextUtils.TruncateAt.END).toString()
            val textX = if (column.alignRight) x + column.width - CELL_PADDING - paint.measureText(fitted) else x + CELL_PADDING
            canvas.drawText(fitted, textX, baseline, paint)
            x += column.width
        }
        return y + ROW_HEIGHT
    }
}
