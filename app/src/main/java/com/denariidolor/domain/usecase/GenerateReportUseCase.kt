package com.denariidolor.domain.usecase

import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.model.ReportRow
import com.denariidolor.domain.model.toDomainTransaction
import com.denariidolor.domain.report.ReportText
import com.denariidolor.util.DateUtils
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

class GenerateReportUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(period: YearMonth): MonthlyReport {
        val (start, end) = DateUtils.monthRangeEpochMillis(period.year, period.monthValue, clock.zone)
        val transactions = transactionRepository.getByDateRange(start, end)
            .sortedWith(compareBy({ it.dateEpochMillis }, { it.id }))
        val categoryNames = categoryRepository.getAll().first().associate { it.id to it.name }
        val accountNames = accountRepository.getAll().first().associate { it.id to it.name }
        fun accountName(id: Long) = accountNames[id] ?: "#$id"

        val rows = transactions.map { transaction ->
            val source = accountName(transaction.accountId)
            ReportRow(
                transactionId = transaction.id,
                dateEpochMillis = transaction.dateEpochMillis,
                type = transaction.type,
                categoryName = categoryNames[transaction.categoryId] ?: "#${transaction.categoryId}",
                description = transaction.description,
                amount = transaction.amount,
                paymentMethod = transaction.transferAccountId?.let { "$source → ${accountName(it)}" } ?: source
            )
        }
        return MonthlyReport(
            title = ReportText.TITLE,
            period = period,
            generatedAtEpochMillis = clock.millis(),
            totalIncome = rows.filter { it.type == "INCOME" }.sumOf { it.amount },
            totalExpense = rows.filter { it.type == "EXPENSE" }.sumOf { it.amount },
            net = transactions.sumOf { it.toDomainTransaction().balanceImpact() },
            rows = rows
        )
    }
}
