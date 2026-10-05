/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity

/** Whole-table reads and writes for backup and restore. Call them inside `withTransaction` (see `BackupStore`). */
@Dao
interface BackupDao {
    @Query("SELECT * FROM accounts ORDER BY id")
    suspend fun accounts(): List<AccountEntity>

    @Query("SELECT * FROM categories ORDER BY id")
    suspend fun categories(): List<CategoryEntity>

    @Query("SELECT * FROM budgets ORDER BY id")
    suspend fun budgets(): List<BudgetEntity>

    @Query("SELECT * FROM transactions ORDER BY id")
    suspend fun transactions(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteTransactions()

    @Query("DELETE FROM budgets")
    suspend fun deleteBudgets()

    @Query("DELETE FROM categories")
    suspend fun deleteCategories()

    @Query("DELETE FROM accounts")
    suspend fun deleteAccounts()

    /** Inserts in parameter order, so every row's parents exist before it does. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        budgets: List<BudgetEntity>,
        transactions: List<TransactionEntity>
    )
}
