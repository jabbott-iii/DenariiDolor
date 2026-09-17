package com.denariidolor.data.local.db

import com.denariidolor.data.local.db.dao.AccountDao
import com.denariidolor.data.local.db.dao.CategoryDao
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultDataInitializer @Inject constructor(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) {
    suspend fun seedDefaults() {
        if (accountDao.count() == 0) {
            Constants.DEFAULT_ACCOUNTS.forEach { account ->
                accountDao.insert(
                    AccountEntity(
                        id = account.id,
                        name = account.name,
                        balance = account.balance
                    )
                )
            }
        }

        if (categoryDao.count() == 0) {
            Constants.DEFAULT_CATEGORIES.forEach { category ->
                categoryDao.insert(
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
