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

package com.denariidolor.domain.usecase

import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.model.ReportRow
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.model.toDomainTransaction
import com.denariidolor.domain.report.ReportText
import com.denariidolor.util.DateUtils
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.first

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
                amountCents = transaction.amountCents,
                paymentMethod = transaction.transferAccountId?.let { "$source → ${accountName(it)}" } ?: source
            )
        }
        return MonthlyReport(
            title = ReportText.TITLE,
            period = period,
            generatedAtEpochMillis = clock.millis(),
            totalIncomeCents = rows.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents },
            totalExpenseCents = rows.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents },
            netCents = transactions.sumOf { it.toDomainTransaction().balanceImpact() },
            rows = rows
        )
    }
}
