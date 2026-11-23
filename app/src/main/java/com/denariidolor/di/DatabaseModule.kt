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
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/** The open database and its DAOs, only in [SessionGraph] (rule 4). */
@ContributesTo(SessionScope::class)
@BindingContainer
object DatabaseModule {
    /**
     * The database opened at the latest sign-in. Unscoped on purpose: each request reads [DatabaseHolder] again, so after the
     * vault locks this throws instead of handing out data.
     */
    @Provides
    fun provideAppDatabase(holder: DatabaseHolder): AppDatabase = holder.database

    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()
}
