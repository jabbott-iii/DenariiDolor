/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.repository

import com.denariidolor.data.local.db.dao.BudgetDao
import com.denariidolor.data.local.db.entity.BudgetEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    suspend fun upsert(budget: BudgetEntity): Long
    fun getAll(): Flow<List<BudgetEntity>>
    suspend fun getByCategoryId(categoryId: Long): BudgetEntity?
    suspend fun deleteByCategoryId(categoryId: Long): Boolean
}

class BudgetRepositoryImpl @Inject constructor(private val budgetDao: BudgetDao) : BudgetRepository {
    override suspend fun upsert(budget: BudgetEntity): Long = budgetDao.upsert(budget)

    override fun getAll(): Flow<List<BudgetEntity>> = budgetDao.getAll()

    override suspend fun getByCategoryId(categoryId: Long): BudgetEntity? = budgetDao.getByCategoryId(categoryId)

    override suspend fun deleteByCategoryId(categoryId: Long): Boolean = budgetDao.deleteByCategoryId(categoryId) > 0
}
