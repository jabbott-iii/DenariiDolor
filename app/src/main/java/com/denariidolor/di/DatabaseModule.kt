package com.denariidolor.di

import android.content.Context
import androidx.room.Room
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.dao.AccountDao
import com.denariidolor.data.local.db.dao.BudgetDao
import com.denariidolor.data.local.db.dao.CategoryDao
import com.denariidolor.data.local.db.dao.TransactionDao
import com.denariidolor.data.local.db.security.DatabaseKeyProvider
import com.denariidolor.data.local.db.security.DatabaseKeys
import com.denariidolor.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        keyProvider: DatabaseKeyProvider
    ): AppDatabase {
        System.loadLibrary("sqlcipher")
        val key = keyProvider.getOrCreate()
        if (DatabaseKeys.shouldDiscard(context.getDatabasePath(Constants.APP_DB_NAME), key.newlyCreated)) {
            context.deleteDatabase(Constants.APP_DB_NAME)
        }
        return Room.databaseBuilder(context, AppDatabase::class.java, Constants.APP_DB_NAME)
            .openHelperFactory(SupportOpenHelperFactory(key.passphrase))
            .build()
    }

    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
}
