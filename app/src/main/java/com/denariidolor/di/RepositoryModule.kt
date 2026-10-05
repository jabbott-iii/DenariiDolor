/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.di

import com.denariidolor.data.backup.BackupManager
import com.denariidolor.data.backup.BackupService
import com.denariidolor.data.repository.AccountRepository
import com.denariidolor.data.repository.AccountRepositoryImpl
import com.denariidolor.data.repository.BudgetRepository
import com.denariidolor.data.repository.BudgetRepositoryImpl
import com.denariidolor.data.repository.CategoryRepository
import com.denariidolor.data.repository.CategoryRepositoryImpl
import com.denariidolor.data.repository.TransactionRepository
import com.denariidolor.data.repository.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    abstract fun bindBackupService(impl: BackupManager): BackupService
}
