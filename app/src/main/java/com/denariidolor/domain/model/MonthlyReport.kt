package com.denariidolor.domain.model

import java.time.YearMonth

data class MonthlyReport(
    val title: String,
    val period: YearMonth,
    val generatedAtEpochMillis: Long,
    val totalIncome: Double,
    val totalExpense: Double,
    val net: Double,
    val rows: List<ReportRow>
)

data class ReportRow(
    val transactionId: Long,
    val dateEpochMillis: Long,
    val type: String,
    val categoryName: String,
    val description: String,
    val amount: Double,
    val paymentMethod: String
)
