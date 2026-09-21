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

package com.denariidolor.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.denariidolor.data.local.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE (:description IS NULL OR description LIKE '%' || :description || '%')
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:minAmountCents IS NULL OR amountCents >= :minAmountCents)
          AND (:maxAmountCents IS NULL OR amountCents <= :maxAmountCents)
          AND (:startDate IS NULL OR dateEpochMillis >= :startDate)
          AND (:endDate IS NULL OR dateEpochMillis <= :endDate)
        ORDER BY dateEpochMillis DESC
        """
    )
    fun search(
        description: String?,
        categoryId: Long?,
        minAmountCents: Long?,
        maxAmountCents: Long?,
        startDate: Long?,
        endDate: Long?
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE dateEpochMillis BETWEEN :startInclusive AND :endInclusive
        ORDER BY dateEpochMillis DESC
        """
    )
    suspend fun getByDateRange(startInclusive: Long, endInclusive: Long): List<TransactionEntity>

    @Query(
        """
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions
        WHERE type = 'EXPENSE' AND categoryId = :categoryId
          AND dateEpochMillis BETWEEN :startInclusive AND :endInclusive
          AND id != :excludeTransactionId
        """
    )
    suspend fun getExpenseTotalForCategory(
        categoryId: Long,
        startInclusive: Long,
        endInclusive: Long,
        excludeTransactionId: Long
    ): Long

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Update
    suspend fun update(transaction: TransactionEntity): Int

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId OR transferAccountId = :accountId")
    suspend fun countByAccount(accountId: Long): Int
}
