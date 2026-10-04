/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.report

import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.util.DateUtils
import com.denariidolor.util.Money
import java.time.ZoneId
import java.util.Locale

object ReportCsvFormatter {
    private val FORMULA_TRIGGERS = setOf('=', '+', '-', '@')
    private val FORMULA_PREFIXES = FORMULA_TRIGGERS + setOf('\t', '\r')
    private val QUOTE_TRIGGERS = setOf(',', '"', '\n', '\r', ';', '\t')

    // A spreadsheet that splits on `;` or tab starts a new cell after one, so a trigger there needs the same guard (CS-20).
    private val INNER_FORMULA = Regex("(?<=[;\t])(\\s*)([=+\\-@][^;\t]*)")

    fun format(report: MonthlyReport, zoneId: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): String = buildString {
        appendRow("title", report.title)
        appendRow("period", ReportText.periodLabel(report.period, locale))
        appendRow("generated_at", ReportText.generatedLabel(report.generatedAtEpochMillis, zoneId))
        appendRow("total_income", Money.toPlain(report.totalIncomeCents))
        appendRow("total_expense", Money.toPlain(report.totalExpenseCents))
        appendRow("net", Money.toPlain(report.netCents))
        append("\r\n")
        appendRow("date", "type", "category", "description", "amount", "payment_method")
        report.rows.forEach { row ->
            appendRow(
                DateUtils.formatLocalDate(row.dateEpochMillis, zoneId),
                row.type.name,
                row.categoryName,
                row.description,
                Money.toPlain(row.amountCents),
                row.paymentMethod
            )
        }
    }

    /**
     * RFC 4180 quoting plus an apostrophe before text that spreadsheets would evaluate as a formula: at the start of the value,
     * after leading whitespace, and after a `;` or tab inside it.
     */
    fun escape(value: String): String {
        val startsLikeFormula = value.firstOrNull() in FORMULA_PREFIXES || value.trimStart().firstOrNull() in FORMULA_TRIGGERS
        val looksLikeFormula = startsLikeFormula && value.trim().toDoubleOrNull() == null
        val inner = INNER_FORMULA.replace(value) { match ->
            val (space, cell) = match.destructured
            if (cell.trim().toDoubleOrNull() == null) "$space'$cell" else match.value
        }
        val safe = if (looksLikeFormula) "'$inner" else inner
        return if (looksLikeFormula || safe.any { it in QUOTE_TRIGGERS }) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else {
            safe
        }
    }

    private fun StringBuilder.appendRow(vararg cells: String) {
        append(cells.joinToString(",") { escape(it) })
        append("\r\n")
    }
}
