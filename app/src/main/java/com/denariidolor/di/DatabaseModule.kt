package com.denariidolor.di

import android.content.Context
import androidx.room.Room
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.dao.AccountDao
import com.denariidolor.data.local.db.dao.BudgetDao
import com.denariidolor.data.local.db.dao.CategoryDao
import com.denariidolor.data.local.db.dao.TransactionDao
import com.denariidolor.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import androidx.sqlite.db.SupportSQLiteDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, Constants.APP_DB_NAME)
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Constants.DEFAULT_ACCOUNTS.forEach { account ->
                        db.execSQL(
                            "INSERT OR IGNORE INTO accounts (id, name, balance) VALUES (?, ?, ?)",
                            arrayOf(account.id, account.name, account.balance)
                        )
                    }
                    Constants.DEFAULT_CATEGORIES.forEach { category ->
                        db.execSQL(
                            "INSERT OR IGNORE INTO categories (id, name, iconName) VALUES (?, ?, ?)",
                            arrayOf(category.id, category.name, category.iconName)
                        )
                    }
                }
            })
            .build()
    }

    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
}
