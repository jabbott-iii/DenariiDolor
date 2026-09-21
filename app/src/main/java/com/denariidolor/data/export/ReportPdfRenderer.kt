package com.denariidolor.data.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.report.ReportText
import com.denariidolor.util.DateUtils
import com.denariidolor.util.formatMoney
import com.denariidolor.util.formatSignedAmount
import java.io.OutputStream
import java.time.ZoneId

/** Renders a [MonthlyReport] as a paginated US-Letter PDF table using the platform [PdfDocument]. */
object ReportPdfRenderer {
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 36f
    private const val ROW_HEIGHT = 18f
    private const val CELL_PADDING = 4f

    private data class Column(val header: String, val width: Float, val alignRight: Boolean = false)

    private val columns = listOf(
        Column("Date", 64f),
        Column("Type", 62f),
        Column("Category", 90f),
        Column("Description", 150f),
        Column("Amount", 70f, alignRight = true),
        Column("Payment Method", 104f)
    )

    fun render(report: MonthlyReport, out: OutputStream, zoneId: ZoneId = ZoneId.systemDefault()) {
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f }
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; typeface = Typeface.DEFAULT_BOLD }
        val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f }
        val linePaint = Paint().apply { strokeWidth = 0.5f }

        val document = PdfDocument()
        try {
            var pageNumber = 0
            var page: PdfDocument.Page? = null
            var y = 0f

            fun finishPage() {
                page?.let { current ->
                    current.canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN - 40f, PAGE_HEIGHT - MARGIN / 2, metaPaint)
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
                    y = drawHeader(newPage.canvas, report, zoneId, y, titlePaint, metaPaint)
                }
                y = drawRow(newPage.canvas, columns.map { it.header }, y, headerPaint)
                newPage.canvas.drawLine(MARGIN, y - ROW_HEIGHT + 4f, PAGE_WIDTH - MARGIN, y - ROW_HEIGHT + 4f, linePaint)
            }

            startPage()
            if (report.rows.isEmpty()) {
                page!!.canvas.drawText("No transactions in this period.", MARGIN, y + ROW_HEIGHT, metaPaint)
            }
            report.rows.forEach { row ->
                if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) startPage()
                y = drawRow(
                    page!!.canvas,
                    listOf(
                        DateUtils.formatLocalDate(row.dateEpochMillis, zoneId),
                        row.type,
                        row.categoryName,
                        row.description,
                        formatSignedAmount(row.type, row.amount),
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
        startY: Float,
        titlePaint: TextPaint,
        metaPaint: TextPaint
    ): Float {
        var y = startY + titlePaint.textSize
        canvas.drawText("${report.title} — ${ReportText.periodLabel(report.period)}", MARGIN, y, titlePaint)
        y += 16f
        canvas.drawText("Generated: ${ReportText.generatedLabel(report.generatedAtEpochMillis, zoneId)}", MARGIN, y, metaPaint)
        y += 14f
        canvas.drawText(
            "Income: ${formatMoney(report.totalIncome)}   Expense: ${formatMoney(report.totalExpense)}   Net: ${formatMoney(report.net)}",
            MARGIN,
            y,
            metaPaint
        )
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
