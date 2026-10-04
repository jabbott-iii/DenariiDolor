/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.db

import android.content.Context
import androidx.room.withTransaction
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the default accounts and categories into the unlocked database; runs after every sign-in and is idempotent.
 * Their names are written in the device's language at that moment (new installs and after a wipe); existing names stay as they are.
 */
@Singleton
class DefaultDataInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseHolder: DatabaseHolder
) {
    suspend fun seedDefaults() {
        val appDatabase = databaseHolder.database
        val accountDao = appDatabase.accountDao()
        val categoryDao = appDatabase.categoryDao()
        appDatabase.withTransaction {
            if (accountDao.count() == 0) {
                Constants.DEFAULT_ACCOUNTS.forEach { account ->
                    accountDao.insertOrIgnore(
                        AccountEntity(
                            id = account.id,
                            name = context.getString(account.nameRes),
                            balanceCents = account.balanceCents
                        )
                    )
                }
            }

            if (categoryDao.count() == 0) {
                Constants.DEFAULT_CATEGORIES.forEach { category ->
                    categoryDao.insertOrIgnore(
                        CategoryEntity(
                            id = category.id,
                            name = context.getString(category.nameRes),
                            iconName = category.iconName
                        )
                    )
                }
            }
        }
    }
}
