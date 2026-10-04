/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
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
import com.denariidolor.data.repository.escapeLike
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

    private fun txn(
        id: Long,
        type: TransactionType,
        description: String,
        cents: Long,
        categoryId: Long,
        date: Long,
        transferTo: Long? = null
    ) = TransactionEntity(id, type, description, cents, categoryId, 1, transferTo, date, createdAtEpochMillis = 0)

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

    @Test
    fun searchMatchesWildcardsLiterally() = runTest {
        dao.insert(txn(5, TransactionType.EXPENSE, "Tip 50% off", 100, categoryId = 1, date = 5_000))
        dao.insert(txn(6, TransactionType.EXPENSE, "snake_case", 100, categoryId = 1, date = 6_000))

        assertEquals(listOf(5L), search(description = escapeLike("50%")))
        assertEquals(listOf(6L), search(description = escapeLike("_")))
    }

    @Test
    fun dashboardQueriesReturnTheMonthAndTheLatestRows() = runTest {
        assertEquals(listOf(3L, 2L), dao.observeByDateRange(2_000, 3_000).first().map { it.id })
        assertEquals(listOf(4L, 3L), dao.observeRecent(2).first().map { it.id })
    }
}
