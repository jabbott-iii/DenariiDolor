package com.denariidolor.domain.usecase

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.BudgetRepository
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.util.DateUtils
import com.denariidolor.util.Validators
import javax.inject.Inject

class ValidateTransactionUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: TransactionEntity): Result<Unit> {
        if (!Validators.isValidAmount(transaction.amount)) return Result.failure(IllegalArgumentException("Invalid amount"))
        if (!Validators.isValidDescription(transaction.description)) return Result.failure(IllegalArgumentException("Description cannot be blank"))
        if (!Validators.isValidDateEpoch(transaction.dateEpochMillis)) return Result.failure(IllegalArgumentException("Invalid date"))

        if (transaction.type == "EXPENSE") {
            val budget = budgetRepository.getByCategoryId(transaction.categoryId)
            if (budget != null) {
                val (start, end) = monthBounds(transaction.dateEpochMillis)
                val spent = transactionRepository.getExpenseTotalForCategory(transaction.categoryId, start, end)
                if ((spent + transaction.amount) > budget.monthlyLimit) {
                    return Result.failure(IllegalStateException("Budget threshold violated"))
                }
            }
        }

        return Result.success(Unit)
    }

    private fun monthBounds(epochMillis: Long): Pair<Long, Long> {
        val date = java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return DateUtils.monthRangeEpochMillis(date.year, date.monthValue)
    }
}
