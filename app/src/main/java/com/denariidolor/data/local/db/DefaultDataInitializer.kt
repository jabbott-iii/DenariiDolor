/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
