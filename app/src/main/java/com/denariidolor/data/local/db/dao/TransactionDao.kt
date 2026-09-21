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
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
          AND (:startDate IS NULL OR dateEpochMillis >= :startDate)
          AND (:endDate IS NULL OR dateEpochMillis <= :endDate)
        ORDER BY dateEpochMillis DESC
        """
    )
    fun search(
        description: String?,
        categoryId: Long?,
        minAmount: Double?,
        maxAmount: Double?,
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
        SELECT COALESCE(SUM(amount), 0) FROM transactions
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
    ): Double

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
