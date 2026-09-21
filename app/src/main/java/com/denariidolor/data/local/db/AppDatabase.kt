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
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao
}
