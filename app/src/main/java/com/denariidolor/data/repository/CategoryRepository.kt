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

package com.denariidolor.data.repository

import com.denariidolor.data.local.db.dao.CategoryDao
import com.denariidolor.data.local.db.entity.CategoryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    suspend fun add(category: CategoryEntity): Long
    suspend fun update(category: CategoryEntity): Boolean
    suspend fun delete(id: Long): Boolean
    suspend fun getById(id: Long): CategoryEntity?
    fun getAll(): Flow<List<CategoryEntity>>
    suspend fun isDuplicateName(name: String, excludeId: Long = 0L): Boolean
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(private val categoryDao: CategoryDao) : CategoryRepository {
    override suspend fun add(category: CategoryEntity): Long = categoryDao.insert(category)

    override suspend fun update(category: CategoryEntity): Boolean = categoryDao.update(category.id, category.name, category.iconName) > 0

    override suspend fun delete(id: Long): Boolean = categoryDao.deleteById(id) > 0

    override suspend fun getById(id: Long): CategoryEntity? = categoryDao.getById(id)

    override fun getAll(): Flow<List<CategoryEntity>> = categoryDao.getAll()

    override suspend fun isDuplicateName(name: String, excludeId: Long): Boolean = categoryDao.countByName(name, excludeId) > 0
}
