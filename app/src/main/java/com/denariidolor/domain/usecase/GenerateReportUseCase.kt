package com.denariidolor.domain.usecase

import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.model.ReportRow
import com.denariidolor.domain.model.toDomainTransaction
import com.denariidolor.util.DateUtils
import java.time.YearMonth
import javax.inject.Inject

class GenerateReportUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(year: Int, month: Int): MonthlyReport {
        val (start, end) = DateUtils.monthRangeEpochMillis(year, month)
        val transactions = transactionRepository.getByDateRange(start, end)
        val rows = transactions.map {
            ReportRow(
                description = it.description,
                categoryId = it.categoryId,
                type = it.type,
                amount = it.amount,
                dateEpochMillis = it.dateEpochMillis
            )
        }
        val income = rows.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = rows.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val net = transactions.sumOf { it.toDomainTransaction().balanceImpact() }
        val generatedAt = System.currentTimeMillis()
        val csv = buildString {
            appendLine("generated_at,${DateUtils.formatIso(generatedAt)}")
            appendLine("description,category_id,type,amount,date")
            rows.forEach {
                appendLine("${escape(it.description)},${it.categoryId},${it.type},${it.amount},${DateUtils.formatIso(it.dateEpochMillis)}")
            }
        }

        return MonthlyReport(
            monthLabel = YearMonth.of(year, month).toString(),
            generatedAtEpochMillis = generatedAt,
            totalIncome = income,
            totalExpense = expense,
            net = net,
            rows = rows,
            csv = csv
        )
    }

    private fun escape(input: String): String = '"' + input.replace("\"", "\"\"") + '"'
}
