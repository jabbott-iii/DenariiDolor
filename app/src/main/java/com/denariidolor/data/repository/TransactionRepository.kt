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

import androidx.room.withTransaction
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.dao.AccountDao
import com.denariidolor.data.local.db.dao.TransactionDao
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.Ledger
import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.domain.model.toDomainTransaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

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
    ): Long
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

    override fun search(filters: SearchFilters): Flow<List<TransactionEntity>> = transactionDao.search(
        description = filters.description,
        categoryId = filters.categoryId,
        minAmountCents = filters.minAmountCents,
        maxAmountCents = filters.maxAmountCents,
        startDate = filters.startDateEpochMillis,
        endDate = filters.endDateEpochMillis
    )

    override suspend fun getByDateRange(startInclusive: Long, endInclusive: Long): List<TransactionEntity> =
        transactionDao.getByDateRange(startInclusive, endInclusive)

    override suspend fun getExpenseTotalForCategory(
        categoryId: Long,
        startInclusive: Long,
        endInclusive: Long,
        excludeTransactionId: Long
    ): Long = transactionDao.getExpenseTotalForCategory(categoryId, startInclusive, endInclusive, excludeTransactionId)

    override suspend fun countByCategory(categoryId: Long): Int = transactionDao.countByCategory(categoryId)

    override suspend fun countByAccount(accountId: Long): Int = transactionDao.countByAccount(accountId)

    private suspend fun applyBalanceDeltas(deltas: Map<Long, Long>) {
        deltas.forEach { (accountId, delta) ->
            check(accountDao.adjustBalance(accountId, delta) == 1) { "Account $accountId not found" }
        }
    }
}
