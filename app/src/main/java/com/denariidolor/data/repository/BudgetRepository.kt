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

import com.denariidolor.data.local.db.dao.BudgetDao
import com.denariidolor.data.local.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface BudgetRepository {
    suspend fun upsert(budget: BudgetEntity): Long
    fun getAll(): Flow<List<BudgetEntity>>
    suspend fun getByCategoryId(categoryId: Long): BudgetEntity?
    suspend fun deleteByCategoryId(categoryId: Long): Boolean
}

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {
    override suspend fun upsert(budget: BudgetEntity): Long = budgetDao.upsert(budget)

    override fun getAll(): Flow<List<BudgetEntity>> = budgetDao.getAll()

    override suspend fun getByCategoryId(categoryId: Long): BudgetEntity? = budgetDao.getByCategoryId(categoryId)

    override suspend fun deleteByCategoryId(categoryId: Long): Boolean = budgetDao.deleteByCategoryId(categoryId) > 0
}
