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
import com.denariidolor.data.local.db.dao.TransactionDao
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        dao = db.transactionDao()
        db.categoryDao().insert(CategoryEntity(id = 1, name = "Dining"))
        db.categoryDao().insert(CategoryEntity(id = 2, name = "Rent"))
        db.accountDao().insert(AccountEntity(id = 1, name = "Cash", balanceCents = 0))
        db.accountDao().insert(AccountEntity(id = 2, name = "Savings", balanceCents = 0))
        listOf(
            txn(1, TransactionType.EXPENSE, "Coffee beans", 1_250, categoryId = 1, date = 1_000),
            txn(2, TransactionType.EXPENSE, "Rent September", 150_000, categoryId = 2, date = 2_000),
            txn(3, TransactionType.INCOME, "Coffee refund", 500, categoryId = 1, date = 3_000),
            txn(4, TransactionType.TRANSFER, "To savings", 10_000, categoryId = 1, date = 4_000, transferTo = 2)
        ).forEach { dao.insert(it) }
    }

    @After
    fun tearDown() = db.close()

    private fun txn(id: Long, type: TransactionType, description: String, cents: Long, categoryId: Long, date: Long, transferTo: Long? = null) =
        TransactionEntity(id, type, description, cents, categoryId, 1, transferTo, date, createdAtEpochMillis = 0)

    private suspend fun search(
        description: String? = null,
        categoryId: Long? = null,
        min: Long? = null,
        max: Long? = null,
        start: Long? = null,
        end: Long? = null
    ) = dao.search(description, categoryId, min, max, start, end).first().map { it.id }

    @Test
    fun searchFiltersCombineAndSortNewestFirst() = runTest {
        assertEquals(listOf(4L, 3L, 2L, 1L), search())
        assertEquals(listOf(3L, 1L), search(description = "coffee"))
        assertEquals(listOf(4L, 3L, 1L), search(categoryId = 1))
        assertEquals(listOf(4L, 1L), search(min = 1_000, max = 10_000))
        assertEquals(listOf(3L, 2L), search(start = 2_000, end = 3_000))
    }

    @Test
    fun searchWithoutMatchesIsEmpty() = runTest {
        assertEquals(emptyList<Long>(), search(description = "zzz"))
        assertEquals(emptyList<Long>(), search(min = 200_000))
    }

    @Test
    fun expenseTotalCountsOnlyExpensesAndHonoursExclusion() = runTest {
        assertEquals(1_250L, dao.getExpenseTotalForCategory(1, 0, 10_000, excludeTransactionId = 0))
        assertEquals(0L, dao.getExpenseTotalForCategory(1, 0, 10_000, excludeTransactionId = 1))
    }

    @Test
    fun countByAccountIncludesTransferDestinations() = runTest {
        assertEquals(1, dao.countByAccount(2))
        assertEquals(4, dao.countByAccount(1))
    }
}
