package com.denariidolor.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.denariidolor.data.local.db.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(account: AccountEntity): Long

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAll(): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts WHERE LOWER(name) = LOWER(:name) AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Long): Int

    @Query("UPDATE accounts SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String): Int

    @Query("UPDATE accounts SET balance = balance + :delta WHERE id = :id")
    suspend fun adjustBalance(id: Long, delta: Double): Int

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
