/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.usecase

import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.model.ReportRow
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.model.toDomainTransactionOrNull
import com.denariidolor.domain.report.ReportText
import com.denariidolor.util.DateUtils
import com.denariidolor.util.ZonedClock
import com.denariidolor.util.transferRoute
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

@Inject
class GenerateReportUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val clock: ZonedClock
) {
    suspend operator fun invoke(period: YearMonth): MonthlyReport {
        val timeZone = clock.timeZone
        val (start, end) = DateUtils.monthRangeEpochMillis(period.year, period.month.number, timeZone)
        val transactions = transactionRepository.observeByDateRange(start, end).first()
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
                amountCents = transaction.amountCents,
                paymentMethod = transaction.transferAccountId?.let { transferRoute(source, accountName(it)) } ?: source
            )
        }
        return MonthlyReport(
            title = ReportText.TITLE,
            period = period,
            generatedAtEpochMillis = clock.now().toEpochMilliseconds(),
            timeZone = timeZone,
            totalIncomeCents = rows.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents },
            totalExpenseCents = rows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents },
            netCents = transactions.mapNotNull { it.toDomainTransactionOrNull() }.sumOf { it.balanceImpact() },
            rows = rows
        )
    }
}
