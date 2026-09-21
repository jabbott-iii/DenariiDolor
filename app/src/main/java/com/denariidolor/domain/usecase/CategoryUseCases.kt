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
        require(id > 0L) { "Category ID is required" }
        val existing = categoryRepository.getById(id) ?: throw NoSuchElementException("Category not found")
        val validName = requireValidName(name, excludeId = id)
        val updated = categoryRepository.update(
            existing.copy(name = validName, iconName = iconName?.takeIf { it.isNotBlank() } ?: existing.iconName)
        )
        if (!updated) throw NoSuchElementException("Category not found")
    }

    suspend fun delete(id: Long): Result<Unit> = runSuspendCatching {
        check(id !in PROTECTED_IDS) { "Default categories cannot be deleted" }
        val usage = transactionRepository.countByCategory(id)
        check(usage == 0) { "Category is used by $usage transaction(s)" }
        if (!categoryRepository.delete(id)) throw NoSuchElementException("Category not found")
    }

    private suspend fun requireValidName(name: String, excludeId: Long): String {
        val trimmed = name.trim()
        require(Validators.isValidName(trimmed)) { "Name must be 1-${Validators.MAX_NAME_LENGTH} characters" }
        require(!categoryRepository.isDuplicateName(trimmed, excludeId)) { "A category named \"$trimmed\" already exists" }
        return trimmed
    }

    private companion object {
        const val DEFAULT_ICON = "ic_category_default"
        val PROTECTED_IDS = Constants.DEFAULT_CATEGORIES.map { it.id }.toSet()
    }
}
