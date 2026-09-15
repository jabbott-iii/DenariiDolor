package com.denariidolor.domain.model

data class MonthlyReport(
    val monthLabel: String,
    val generatedAtEpochMillis: Long,
    val totalIncome: Double,
    val totalExpense: Double,
    val net: Double,
    val rows: List<ReportRow>,
    val csv: String
)

data class ReportRow(
    val description: String,
    val categoryId: Long,
    val type: String,
    val amount: Double,
    val dateEpochMillis: Long
)
