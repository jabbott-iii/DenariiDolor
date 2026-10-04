/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.di

import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.DatabaseHolder
import com.denariidolor.data.local.db.dao.AccountDao
import com.denariidolor.data.local.db.dao.BudgetDao
import com.denariidolor.data.local.db.dao.CategoryDao
import com.denariidolor.data.local.db.dao.TransactionDao
import com.denariidolor.data.local.vault.VaultConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideVaultConfig(): VaultConfig = VaultConfig()

    /**
     * The database opened at the latest sign-in. Unscoped on purpose: inject it (or a repository) only into screens shown
     * after sign-in. While the vault is locked this throws instead of handing out data.
     */
    @Provides
    fun provideAppDatabase(holder: DatabaseHolder): AppDatabase = holder.database

    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
}
