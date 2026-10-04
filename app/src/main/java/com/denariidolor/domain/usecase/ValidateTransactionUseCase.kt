/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
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
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** How far an expense would take its category past the monthly budget; the `arg` of a [DomainError.BUDGET_EXCEEDED] failure. */
data class BudgetOverage(val categoryName: String, val overByCents: Long, val limitCents: Long)

class ValidateTransactionUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val clock: Clock
) {
    /**
     * Checks [transaction] before it is written. An expense that would take its category past the monthly budget fails with
     * [DomainError.BUDGET_EXCEEDED] and a [BudgetOverage], unless [allowOverBudget] says the user has confirmed it.
     * Every other rule still applies when it does.
     */
    suspend operator fun invoke(transaction: TransactionEntity, allowOverBudget: Boolean = false): Result<Unit> {
        if (!Validators.isValidAmount(transaction.amountCents)) return failure(DomainError.INVALID_AMOUNT, "Invalid amount")
        if (transaction.description.isBlank()) return failure(DomainError.BLANK_DESCRIPTION, "Description cannot be blank")
        if (!Validators.isValidDescription(transaction.description)) {
            return failure(
                DomainError.DESCRIPTION_TOO_LONG,
                "Description must be at most ${Validators.MAX_DESCRIPTION_LENGTH} characters",
                Validators.MAX_DESCRIPTION_LENGTH
            )
        }
        if (!Validators.isValidDateEpoch(transaction.dateEpochMillis)) return failure(DomainError.INVALID_DATE, "Invalid date")
        if (transaction.accountId <= 0L) return failure(DomainError.INVALID_ACCOUNT, "Invalid account")
        if (transaction.categoryId <= 0L) return failure(DomainError.INVALID_CATEGORY, "Invalid category")
        if (transaction.type == TransactionType.TRANSFER) {
            val transferAccountId = transaction.transferAccountId ?: return failure(
                DomainError.TRANSFER_DESTINATION_REQUIRED,
                "Transfer destination is required"
            )
            if (transferAccountId == transaction.accountId) {
                return failure(DomainError.TRANSFER_DESTINATION_SAME, "Transfer destination must be different")
            }
        }
        return runSuspendCatching { checkReferencesAndBudget(transaction, allowOverBudget) }.getOrElse { Result.failure(it) }
    }

    private suspend fun checkReferencesAndBudget(transaction: TransactionEntity, allowOverBudget: Boolean): Result<Unit> {
        val category = categoryRepository.getById(transaction.categoryId)
            ?: return failure(DomainError.CATEGORY_NOT_FOUND, "Category not found")
        if (accountRepository.getById(transaction.accountId) == null) return failure(DomainError.ACCOUNT_NOT_FOUND, "Account not found")
        val transferAccountId = transaction.transferAccountId
        if (transaction.type == TransactionType.TRANSFER &&
            transferAccountId != null &&
            accountRepository.getById(transferAccountId) == null
        ) {
            return failure(DomainError.TRANSFER_DESTINATION_NOT_FOUND, "Transfer destination account not found")
        }
        if (transaction.type == TransactionType.EXPENSE && !allowOverBudget) {
            val budget = budgetRepository.getByCategoryId(transaction.categoryId)
            if (budget != null) {
                val (start, end) = monthBounds(transaction.dateEpochMillis)
                val spent = transactionRepository.getExpenseTotalForCategory(
                    categoryId = transaction.categoryId,
                    startInclusive = start,
                    endInclusive = end,
                    excludeTransactionId = transaction.id
                )
                val overByCents = spent + transaction.amountCents - budget.monthlyLimitCents
                if (overByCents > 0) {
                    val overage = BudgetOverage(category.name, overByCents, budget.monthlyLimitCents)
                    return failure(DomainError.BUDGET_EXCEEDED, "Budget threshold violated", overage)
                }
            }
        }
        return Result.success(Unit)
    }

    private fun failure(error: DomainError, message: String, arg: Any? = null): Result<Unit> =
        Result.failure(DomainException(error, message, arg))

    private fun monthBounds(epochMillis: Long): Pair<Long, Long> {
        val zone = clock.zone
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return DateUtils.monthRangeEpochMillis(date.year, date.monthValue, zone)
    }
}
