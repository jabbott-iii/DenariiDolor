/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.denariidolor.data.local.db.dao.AccountDao
import com.denariidolor.data.local.db.dao.BudgetDao
import com.denariidolor.data.local.db.dao.CategoryDao
import com.denariidolor.data.local.db.dao.TransactionDao
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, BudgetEntity::class, AccountEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao
}
