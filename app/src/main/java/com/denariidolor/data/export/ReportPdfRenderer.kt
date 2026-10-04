/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
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
import kotlin.math.ceil

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

/**
 * Renders a [MonthlyReport] as a paginated US-Letter PDF table using the platform [PdfDocument]. Nothing is cut off: dates, types
 * and amounts get columns as wide as their widest value, the other columns share the rest, and any cell, header or heading line
 * that doesn't fit wraps onto more lines. Rows grow to their tallest cell, and pages break on the actual row heights.
 */
@Suppress("MagicNumber") // Layout coordinates and font sizes in PDF points; naming each would hurt readability.
object ReportPdfRenderer {
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    private const val TABLE_BOTTOM = PAGE_HEIGHT - MARGIN
    private const val CELL_PADDING = 4f
    private const val CELL_V_PADDING = 3f

    // A safety net only: a 200-character description needs about a dozen lines even in Chinese, and 30 lines still fit a page.
    private const val MAX_CELL_LINES = 30

    // Date, type, category, description, amount, payment method. Weight 0 fits the content; the rest share what's left.
    private val COLUMN_WEIGHTS = listOf(0f, 0f, 1f, 2f, 0f, 1.2f)
    private const val AMOUNT_COLUMN = 4
    private val WHITESPACE = Regex("\\s+")

    private class Paints {
        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        val meta = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
        val header = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
        }
        val cell = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f }
        val line = Paint().apply { strokeWidth = 0.5f }
    }

    fun render(
        report: MonthlyReport,
        out: OutputStream,
        zoneId: ZoneId = ZoneId.systemDefault(),
        labels: ReportPdfLabels = ReportPdfLabels()
    ) {
        val paints = Paints()
        val rows = report.rows.map { row ->
            listOf(
                DateUtils.formatLocalDate(row.dateEpochMillis, zoneId),
                labels.type(row.type),
                row.categoryName,
                row.description,
                formatSignedAmount(row.type, row.amountCents),
                row.paymentMethod
            )
        }
        val widths = columnWidths(labels.columns, rows, paints)
        val document = PdfDocument()
        try {
            var pageNumber = 0
            var page: PdfDocument.Page? = null
            var y = 0f
            var rowsOnPage = 0

            fun finishPage() {
                page?.let { current ->
                    val pageLabel = labels.page(pageNumber)
                    val pageX = PAGE_WIDTH - MARGIN - paints.meta.measureText(pageLabel)
                    current.canvas.drawText(pageLabel, pageX, PAGE_HEIGHT - MARGIN / 2, paints.meta)
                    document.finishPage(current)
                }
            }

            fun startPage(): Canvas {
                finishPage()
                pageNumber++
                val newPage = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                page = newPage
                y = MARGIN
                if (pageNumber == 1) y = drawHeading(newPage.canvas, report, zoneId, labels, y, paints)
                y = drawRow(newPage.canvas, layoutRow(labels.columns, widths, paints.header), widths, y)
                newPage.canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paints.line)
                rowsOnPage = 0
                return newPage.canvas
            }

            var canvas = startPage()
            if (rows.isEmpty()) drawBlock(canvas, labels.empty, paints.meta, y + CELL_V_PADDING)
            rows.forEach { cells ->
                val layouts = layoutRow(cells, widths, paints.cell)
                // A row that doesn't fit moves to the next page; on a fresh page it is drawn whatever its height.
                if (y + rowHeight(layouts) > TABLE_BOTTOM && rowsOnPage > 0) canvas = startPage()
                y = drawRow(canvas, layouts, widths, y)
                rowsOnPage++
            }
            finishPage()
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    /** Fitted columns measure their widest cell; every column is at least as wide as its header's longest word. */
    private fun columnWidths(headers: List<String>, rows: List<List<String>>, paints: Paints): List<Float> {
        val specs = headers.mapIndexed { index, header ->
            val longestWord = header.split(WHITESPACE).maxOf { paints.header.measureText(it) }
            val widestCell = rows.maxOfOrNull { paints.cell.measureText(it[index]) } ?: 0f
            PdfColumnSpec(
                minWidth = ceil(longestWord) + 2 * CELL_PADDING,
                contentWidth = ceil(widestCell) + 2 * CELL_PADDING,
                weight = COLUMN_WEIGHTS[index]
            )
        }
        return PdfColumnWidths.compute(specs, CONTENT_WIDTH)
    }

    private fun layoutRow(cells: List<String>, widths: List<Float>, paint: TextPaint): List<StaticLayout> =
        cells.mapIndexed { index, text -> layoutText(text, paint, widths[index] - 2 * CELL_PADDING, alignEnd = index == AMOUNT_COLUMN) }

    /**
     * Wraps [text] to [width]. Amounts align right whatever their script; other text follows its own direction, so an Arabic
     * description aligns right in its cell.
     */
    private fun layoutText(text: String, paint: TextPaint, width: Float, alignEnd: Boolean = false): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, ceil(width).toInt().coerceAtLeast(1))
            .setAlignment(if (alignEnd) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(if (alignEnd) TextDirectionHeuristics.LTR else TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setIncludePad(false)
            .setMaxLines(MAX_CELL_LINES)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()

    private fun rowHeight(layouts: List<StaticLayout>): Float = layouts.maxOf { it.height } + 2 * CELL_V_PADDING

    private fun drawRow(canvas: Canvas, layouts: List<StaticLayout>, widths: List<Float>, top: Float): Float {
        var x = MARGIN
        layouts.forEachIndexed { index, layout ->
            canvas.save()
            canvas.translate(x + CELL_PADDING, top + CELL_V_PADDING)
            layout.draw(canvas)
            canvas.restore()
            x += widths[index]
        }
        return top + rowHeight(layouts)
    }

    /** Draws [text] across the page width, wrapped, and returns the y below it. */
    private fun drawBlock(canvas: Canvas, text: String, paint: TextPaint, top: Float): Float {
        val layout = layoutText(text, paint, CONTENT_WIDTH)
        canvas.save()
        canvas.translate(MARGIN, top)
        layout.draw(canvas)
        canvas.restore()
        return top + layout.height
    }

    private fun drawHeading(
        canvas: Canvas,
        report: MonthlyReport,
        zoneId: ZoneId,
        labels: ReportPdfLabels,
        top: Float,
        paints: Paints
    ): Float {
        var y = drawBlock(canvas, "${labels.title} — ${ReportText.periodLabel(report.period)}", paints.title, top)
        y = drawBlock(canvas, labels.generated(ReportText.generatedLabel(report.generatedAtEpochMillis, zoneId)), paints.meta, y + 4f)
        val totals = labels.totals(
            formatMoney(report.totalIncomeCents),
            formatMoney(report.totalExpenseCents),
            formatMoney(report.netCents)
        )
        return drawBlock(canvas, totals, paints.meta, y + 2f) + 12f
    }
}
