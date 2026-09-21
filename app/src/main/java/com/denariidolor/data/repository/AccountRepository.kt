package com.denariidolor.data.repository

import com.denariidolor.data.local.db.dao.AccountDao
import com.denariidolor.data.local.db.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface AccountRepository {
    suspend fun add(account: AccountEntity): Long
    suspend fun rename(id: Long, name: String): Boolean
    suspend fun delete(id: Long): Boolean
    suspend fun getById(id: Long): AccountEntity?
    fun getAll(): Flow<List<AccountEntity>>
    suspend fun isDuplicateName(name: String, excludeId: Long = 0L): Boolean
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {
    override suspend fun add(account: AccountEntity): Long = accountDao.insert(account)

    override suspend fun rename(id: Long, name: String): Boolean = accountDao.rename(id, name) > 0

    override suspend fun delete(id: Long): Boolean = accountDao.deleteById(id) > 0

    override suspend fun getById(id: Long): AccountEntity? = accountDao.getById(id)

    override fun getAll(): Flow<List<AccountEntity>> = accountDao.getAll()

    override suspend fun isDuplicateName(name: String, excludeId: Long): Boolean =
        accountDao.countByName(name, excludeId) > 0
}
