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

import androidx.room.withTransaction
import com.denariidolor.data.local.db.dao.AccountDao
import com.denariidolor.data.local.db.dao.CategoryDao
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultDataInitializer @Inject constructor(
    private val appDatabase: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) {
    suspend fun seedDefaults() {
        appDatabase.withTransaction {
            if (accountDao.count() == 0) {
                Constants.DEFAULT_ACCOUNTS.forEach { account ->
                    accountDao.insertOrIgnore(
                        AccountEntity(
                            id = account.id,
                            name = account.name,
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
                            name = category.name,
                            iconName = category.iconName
                        )
                    )
                }
            }
        }
    }
}
