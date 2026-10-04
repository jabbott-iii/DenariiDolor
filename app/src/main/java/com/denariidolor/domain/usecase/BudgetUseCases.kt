/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

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
    suspend fun set(categoryId: Long, monthlyLimitCents: Long, warningThresholdPercent: Int = DEFAULT_WARNING_PERCENT): Result<Long> =
        runSuspendCatching {
            if (!Validators.isValidAmount(monthlyLimitCents)) {
                domainFailure(DomainError.INVALID_BUDGET_LIMIT, "Monthly limit must be greater than zero")
            }
            if (!Validators.isValidPercent(warningThresholdPercent)) {
                domainFailure(DomainError.INVALID_WARNING_PERCENT, "Warning threshold must be between 1 and 100")
            }
            categoryRepository.getById(categoryId) ?: domainFailure(DomainError.CATEGORY_NOT_FOUND, "Category not found")
            val existingId = budgetRepository.getByCategoryId(categoryId)?.id ?: 0L
            budgetRepository.upsert(
                BudgetEntity(
                    id = existingId,
                    categoryId = categoryId,
                    monthlyLimitCents = monthlyLimitCents,
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
