package com.denariidolor.data.repository

import androidx.room.withTransaction
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.dao.AccountDao
import com.denariidolor.data.local.db.dao.TransactionDao
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.Ledger
import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.domain.model.toDomainTransaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionRepository {
    suspend fun add(transaction: TransactionEntity): Long
    suspend fun update(transaction: TransactionEntity): Boolean
    suspend fun delete(id: Long): Boolean
    suspend fun getById(id: Long): TransactionEntity?
    fun getAll(): Flow<List<TransactionEntity>>
    fun search(filters: SearchFilters): Flow<List<TransactionEntity>>
    suspend fun getByDateRange(startInclusive: Long, endInclusive: Long): List<TransactionEntity>
    suspend fun getExpenseTotalForCategory(
        categoryId: Long,
        startInclusive: Long,
        endInclusive: Long,
        excludeTransactionId: Long = 0L
    ): Double
    suspend fun countByCategory(categoryId: Long): Int
    suspend fun countByAccount(accountId: Long): Int
}

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : TransactionRepository {
    override suspend fun add(transaction: TransactionEntity): Long = database.withTransaction {
        val id = transactionDao.insert(transaction)
        applyBalanceDeltas(Ledger.balanceDeltas(previous = null, current = transaction.toDomainTransaction()))
        id
    }

    override suspend fun update(transaction: TransactionEntity): Boolean = database.withTransaction {
        val previous = transactionDao.getById(transaction.id) ?: return@withTransaction false
        transactionDao.update(transaction.copy(createdAtEpochMillis = previous.createdAtEpochMillis))
        applyBalanceDeltas(Ledger.balanceDeltas(previous.toDomainTransaction(), transaction.toDomainTransaction()))
        true
    }

    override suspend fun delete(id: Long): Boolean = database.withTransaction {
        val previous = transactionDao.getById(id) ?: return@withTransaction false
        transactionDao.deleteById(id)
        applyBalanceDeltas(Ledger.balanceDeltas(previous = previous.toDomainTransaction(), current = null))
        true
    }

    override suspend fun getById(id: Long): TransactionEntity? = transactionDao.getById(id)

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
        endInclusive: Long,
        excludeTransactionId: Long
    ): Double {
        return transactionDao.getExpenseTotalForCategory(categoryId, startInclusive, endInclusive, excludeTransactionId)
    }

    override suspend fun countByCategory(categoryId: Long): Int = transactionDao.countByCategory(categoryId)

    override suspend fun countByAccount(accountId: Long): Int = transactionDao.countByAccount(accountId)

    private suspend fun applyBalanceDeltas(deltas: Map<Long, Double>) {
        deltas.forEach { (accountId, delta) ->
            check(accountDao.adjustBalance(accountId, delta) == 1) { "Account $accountId not found" }
        }
    }
}
