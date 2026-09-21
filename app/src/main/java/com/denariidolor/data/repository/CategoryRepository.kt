package com.denariidolor.data.repository

import com.denariidolor.data.local.db.dao.CategoryDao
import com.denariidolor.data.local.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface CategoryRepository {
    suspend fun add(category: CategoryEntity): Long
    suspend fun update(category: CategoryEntity): Boolean
    suspend fun delete(id: Long): Boolean
    suspend fun getById(id: Long): CategoryEntity?
    fun getAll(): Flow<List<CategoryEntity>>
    suspend fun isDuplicateName(name: String, excludeId: Long = 0L): Boolean
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override suspend fun add(category: CategoryEntity): Long = categoryDao.insert(category)

    override suspend fun update(category: CategoryEntity): Boolean =
        categoryDao.update(category.id, category.name, category.iconName) > 0

    override suspend fun delete(id: Long): Boolean = categoryDao.deleteById(id) > 0

    override suspend fun getById(id: Long): CategoryEntity? = categoryDao.getById(id)

    override fun getAll(): Flow<List<CategoryEntity>> = categoryDao.getAll()

    override suspend fun isDuplicateName(name: String, excludeId: Long): Boolean =
        categoryDao.countByName(name, excludeId) > 0
}
