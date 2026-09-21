package com.denariidolor.domain.report

import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.util.DateUtils
import java.time.ZoneId
import java.util.Locale

object ReportCsvFormatter {
    private val FORMULA_PREFIXES = charArrayOf('=', '+', '-', '@', '\t', '\r')

    fun format(report: MonthlyReport, zoneId: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String =
        buildString {
            appendRow("title", report.title)
            appendRow("period", ReportText.periodLabel(report.period, locale))
            appendRow("generated_at", ReportText.generatedLabel(report.generatedAtEpochMillis, zoneId))
            appendRow("total_income", amount(report.totalIncome))
            appendRow("total_expense", amount(report.totalExpense))
            appendRow("net", amount(report.net))
            append("\r\n")
            appendRow("date", "type", "category", "description", "amount", "payment_method")
            report.rows.forEach { row ->
                appendRow(
                    DateUtils.formatLocalDate(row.dateEpochMillis, zoneId),
                    row.type,
                    row.categoryName,
                    row.description,
                    amount(row.amount),
                    row.paymentMethod
                )
            }
        }

    /** RFC 4180 quoting plus a leading apostrophe on text that spreadsheets would evaluate as a formula. */
    fun escape(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in FORMULA_PREFIXES && value.toDoubleOrNull() == null) "'$value" else value
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' } || safe != value) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else {
            safe
        }
    }

    private fun amount(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun StringBuilder.appendRow(vararg cells: String) {
        append(cells.joinToString(",") { escape(it) })
        append("\r\n")
    }
}
