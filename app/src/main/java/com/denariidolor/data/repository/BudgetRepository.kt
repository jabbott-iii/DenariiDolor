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
}

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {
    override suspend fun upsert(budget: BudgetEntity): Long = budgetDao.upsert(budget)

    override fun getAll(): Flow<List<BudgetEntity>> = budgetDao.getAll()

    override suspend fun getByCategoryId(categoryId: Long): BudgetEntity? = budgetDao.getByCategoryId(categoryId)
}
