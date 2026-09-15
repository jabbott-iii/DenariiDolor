package com.denariidolor.data.repository

import com.denariidolor.data.local.db.dao.TransactionDao
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.SearchFilters
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionRepository {
    suspend fun add(transaction: TransactionEntity): Long
    fun getAll(): Flow<List<TransactionEntity>>
    fun search(filters: SearchFilters): Flow<List<TransactionEntity>>
    suspend fun getByDateRange(startInclusive: Long, endInclusive: Long): List<TransactionEntity>
    suspend fun getExpenseTotalForCategory(categoryId: Long, startInclusive: Long, endInclusive: Long): Double
}

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {
    override suspend fun add(transaction: TransactionEntity): Long = transactionDao.insert(transaction)

    override fun getAll(): Flow<List<TransactionEntity>> = transactionDao.getAll()

    override fun search(filters: SearchFilters): Flow<List<TransactionEntity>> {
        return transactionDao.search(
            description = filters.description,
            categoryId = filters.categoryId,
            minAmount = filters.minAmount,
            maxAmount = filters.maxAmount,
            startDate = filters.startDateEpochMillis,
            endDate = filters.endDateEpochMillis
        )
    }

    override suspend fun getByDateRange(startInclusive: Long, endInclusive: Long): List<TransactionEntity> {
        return transactionDao.getByDateRange(startInclusive, endInclusive)
    }

    override suspend fun getExpenseTotalForCategory(
        categoryId: Long,
        startInclusive: Long,
        endInclusive: Long
    ): Double {
        return transactionDao.getExpenseTotalForCategory(categoryId, startInclusive, endInclusive)
    }
}
