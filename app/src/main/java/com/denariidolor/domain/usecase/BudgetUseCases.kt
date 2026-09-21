package com.denariidolor.domain.usecase

import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.repository.BudgetRepository
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.util.Validators
import com.denariidolor.util.runSuspendCatching
import javax.inject.Inject

class BudgetUseCases @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend fun set(categoryId: Long, monthlyLimit: Double, warningThresholdPercent: Int = DEFAULT_WARNING_PERCENT): Result<Long> =
        runSuspendCatching {
            require(Validators.isValidAmount(monthlyLimit)) { "Monthly limit must be greater than zero" }
            require(Validators.isValidPercent(warningThresholdPercent)) { "Warning threshold must be between 1 and 100" }
            requireNotNull(categoryRepository.getById(categoryId)) { "Category not found" }
            val existingId = budgetRepository.getByCategoryId(categoryId)?.id ?: 0L
            budgetRepository.upsert(
                BudgetEntity(
                    id = existingId,
                    categoryId = categoryId,
                    monthlyLimit = monthlyLimit,
                    warningThresholdPercent = warningThresholdPercent
                )
            )
        }

    suspend fun delete(categoryId: Long): Result<Unit> = runSuspendCatching {
        if (!budgetRepository.deleteByCategoryId(categoryId)) throw NoSuchElementException("Budget not found")
    }

    private companion object {
        const val DEFAULT_WARNING_PERCENT = 80
    }
}
