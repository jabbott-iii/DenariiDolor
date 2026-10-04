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

import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.util.Constants
import com.denariidolor.util.Validators
import com.denariidolor.util.runSuspendCatching
import javax.inject.Inject

class CategoryUseCases @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend fun add(name: String, iconName: String = DEFAULT_ICON): Result<Long> = runSuspendCatching {
        val validName = requireValidName(name, excludeId = 0L)
        categoryRepository.add(CategoryEntity(name = validName, iconName = iconName.ifBlank { DEFAULT_ICON }))
    }

    suspend fun update(id: Long, name: String, iconName: String? = null): Result<Unit> = runSuspendCatching {
        if (id <= 0L) domainFailure(DomainError.ID_REQUIRED, "Category ID is required")
        val existing = categoryRepository.getById(id) ?: throw NoSuchElementException("Category not found")
        val validName = requireValidName(name, excludeId = id)
        val updated = categoryRepository.update(
            existing.copy(name = validName, iconName = iconName?.takeIf { it.isNotBlank() } ?: existing.iconName)
        )
        if (!updated) throw NoSuchElementException("Category not found")
    }

    suspend fun delete(id: Long): Result<Unit> = runSuspendCatching {
        if (id in PROTECTED_IDS) domainFailure(DomainError.DEFAULT_CATEGORY, "Default categories cannot be deleted")
        val usage = transactionRepository.countByCategory(id)
        if (usage != 0) domainFailure(DomainError.CATEGORY_IN_USE, "Category is used by $usage transaction(s)", usage)
        if (!categoryRepository.delete(id)) throw NoSuchElementException("Category not found")
    }

    private suspend fun requireValidName(name: String, excludeId: Long): String {
        val trimmed = name.trim()
        if (!Validators.isValidName(trimmed)) {
            domainFailure(DomainError.INVALID_NAME, "Name must be 1-${Validators.MAX_NAME_LENGTH} characters", Validators.MAX_NAME_LENGTH)
        }
        if (categoryRepository.isDuplicateName(trimmed, excludeId)) {
            domainFailure(DomainError.DUPLICATE_CATEGORY_NAME, "A category named \"$trimmed\" already exists", trimmed)
        }
        return trimmed
    }

    private companion object {
        const val DEFAULT_ICON = "ic_category_default"
        val PROTECTED_IDS = Constants.DEFAULT_CATEGORIES.map { it.id }.toSet()
    }
}
