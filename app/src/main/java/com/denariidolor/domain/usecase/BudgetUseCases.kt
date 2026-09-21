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
            require(Validators.isValidAmount(monthlyLimitCents)) { "Monthly limit must be greater than zero" }
            require(Validators.isValidPercent(warningThresholdPercent)) { "Warning threshold must be between 1 and 100" }
            requireNotNull(categoryRepository.getById(categoryId)) { "Category not found" }
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
