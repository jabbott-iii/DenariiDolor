package com.denariidolor.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.denariidolor.data.local.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(category: CategoryEntity): Long

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories WHERE LOWER(name) = LOWER(:name) AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Long): Int

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("UPDATE categories SET name = :name, iconName = :iconName WHERE id = :id")
    suspend fun update(id: Long, name: String, iconName: String): Int

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
