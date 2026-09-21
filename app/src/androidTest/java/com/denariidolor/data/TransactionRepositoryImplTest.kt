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

package com.denariidolor.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.TransactionRepositoryImpl
import com.denariidolor.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionRepositoryImplTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
        repository = TransactionRepositoryImpl(db, db.transactionDao(), db.accountDao())
        db.accountDao().insert(AccountEntity(id = 1, name = "Cash", balanceCents = 10_000))
        db.accountDao().insert(AccountEntity(id = 2, name = "Savings", balanceCents = 0))
        db.categoryDao().insert(CategoryEntity(id = 1, name = "General"))
        Unit
    }

    @After
    fun tearDown() = db.close()

    private suspend fun balance(id: Long) = db.accountDao().getById(id)!!.balanceCents

    private fun expense(amountCents: Long, accountId: Long = 1) = TransactionEntity(
        type = TransactionType.EXPENSE,
        description = "Coffee",
        amountCents = amountCents,
        categoryId = 1,
        accountId = accountId,
        dateEpochMillis = 1_000L
    )

    @Test
    fun addUpdateDeleteKeepBalancesInSync() = runBlocking {
        val id = repository.add(expense(3_000))
        assertEquals(7_000L, balance(1))

        assertTrue(repository.update(expense(5_000, accountId = 2).copy(id = id)))
        assertEquals(10_000L, balance(1))
        assertEquals(-5_000L, balance(2))

        assertTrue(repository.delete(id))
        assertEquals(0L, balance(2))
        assertNull(repository.getById(id))
    }

    @Test
    fun transferDebitsSourceAndCreditsDestination() = runBlocking {
        repository.add(expense(4_000).copy(type = TransactionType.TRANSFER, transferAccountId = 2))

        assertEquals(6_000L, balance(1))
        assertEquals(4_000L, balance(2))
    }

    @Test
    fun updatePreservesCreatedAt() = runBlocking {
        val id = repository.add(expense(1_000).copy(createdAtEpochMillis = 5L))
        repository.update(expense(1_200).copy(id = id, createdAtEpochMillis = 999L))

        assertEquals(5L, repository.getById(id)!!.createdAtEpochMillis)
    }

    @Test
    fun missingAccountRollsBackWholeOperation() = runBlocking {
        val result = runCatching { repository.add(expense(1_000).copy(type = TransactionType.TRANSFER, transferAccountId = 99)) }

        assertTrue(result.isFailure)
        assertEquals(0, db.transactionDao().getByDateRange(0L, Long.MAX_VALUE).size)
        assertEquals(10_000L, balance(1))
    }

    @Test
    fun updateAndDeleteReturnFalseForUnknownId() = runBlocking {
        assertFalse(repository.update(expense(100).copy(id = 42)))
        assertFalse(repository.delete(42))
    }
}
