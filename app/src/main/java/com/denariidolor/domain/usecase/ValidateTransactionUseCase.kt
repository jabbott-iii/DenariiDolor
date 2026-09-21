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

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.data.repository.BudgetRepository
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.util.DateUtils
import com.denariidolor.util.Validators
import com.denariidolor.util.runSuspendCatching
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class ValidateTransactionUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: TransactionEntity): Result<Unit> {
        if (!Validators.isValidAmount(transaction.amountCents)) return failure("Invalid amount")
        if (!Validators.isValidDescription(transaction.description)) return failure("Description cannot be blank")
        if (!Validators.isValidDateEpoch(transaction.dateEpochMillis)) return failure("Invalid date")
        if (transaction.accountId <= 0L) return failure("Invalid account")
        if (transaction.categoryId <= 0L) return failure("Invalid category")
        if (transaction.type == TransactionType.TRANSFER) {
            val transferAccountId = transaction.transferAccountId ?: return failure("Transfer destination is required")
            if (transferAccountId == transaction.accountId) return failure("Transfer destination must be different")
        }
        return runSuspendCatching { checkReferencesAndBudget(transaction) }.getOrElse { Result.failure(it) }
    }

    private suspend fun checkReferencesAndBudget(transaction: TransactionEntity): Result<Unit> {
        if (categoryRepository.getById(transaction.categoryId) == null) return failure("Category not found")
        if (accountRepository.getById(transaction.accountId) == null) return failure("Account not found")
        val transferAccountId = transaction.transferAccountId
        if (transaction.type == TransactionType.TRANSFER && transferAccountId != null && accountRepository.getById(transferAccountId) == null) {
            return failure("Transfer destination account not found")
        }
        if (transaction.type == TransactionType.EXPENSE) {
            val budget = budgetRepository.getByCategoryId(transaction.categoryId)
            if (budget != null) {
                val (start, end) = monthBounds(transaction.dateEpochMillis)
                val spent = transactionRepository.getExpenseTotalForCategory(
                    categoryId = transaction.categoryId,
                    startInclusive = start,
                    endInclusive = end,
                    excludeTransactionId = transaction.id
                )
                if (spent + transaction.amountCents > budget.monthlyLimitCents) {
                    return Result.failure(IllegalStateException("Budget threshold violated"))
                }
            }
        }
        return Result.success(Unit)
    }

    private fun failure(message: String): Result<Unit> = Result.failure(IllegalArgumentException(message))

    private fun monthBounds(epochMillis: Long): Pair<Long, Long> {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return DateUtils.monthRangeEpochMillis(date.year, date.monthValue)
    }
}
