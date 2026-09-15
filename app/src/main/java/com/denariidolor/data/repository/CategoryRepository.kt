package com.denariidolor.data.repository

import com.denariidolor.data.local.db.dao.CategoryDao
import com.denariidolor.data.local.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface CategoryRepository {
    suspend fun add(category: CategoryEntity): Long
    fun getAll(): Flow<List<CategoryEntity>>
    suspend fun isDuplicateName(name: String): Boolean
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override suspend fun add(category: CategoryEntity): Long = categoryDao.insert(category)

    override fun getAll(): Flow<List<CategoryEntity>> = categoryDao.getAll()

    override suspend fun isDuplicateName(name: String): Boolean = categoryDao.countByName(name) > 0
}
