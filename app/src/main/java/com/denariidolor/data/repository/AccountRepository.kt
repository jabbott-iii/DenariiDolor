package com.denariidolor.data.repository

import com.denariidolor.data.local.db.dao.AccountDao
import com.denariidolor.data.local.db.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface AccountRepository {
    suspend fun add(account: AccountEntity): Long
    fun getAll(): Flow<List<AccountEntity>>
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {
    override suspend fun add(account: AccountEntity): Long = accountDao.insert(account)

    override fun getAll(): Flow<List<AccountEntity>> = accountDao.getAll()
}
