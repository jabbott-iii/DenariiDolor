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
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.TransactionRepositoryImpl
import com.denariidolor.domain.model.TransactionType
import kotlinx.coroutines.flow.first
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
    fun setUp() = runBlocking<Unit> {
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
    fun addUpdateDeleteKeepBalancesInSync() = runBlocking<Unit> {
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
    fun transferDebitsSourceAndCreditsDestination() = runBlocking<Unit> {
        repository.add(expense(4_000).copy(type = TransactionType.TRANSFER, transferAccountId = 2))

        assertEquals(6_000L, balance(1))
        assertEquals(4_000L, balance(2))
    }

    @Test
    fun updatePreservesCreatedAt() = runBlocking<Unit> {
        val id = repository.add(expense(1_000).copy(createdAtEpochMillis = 5L))
        repository.update(expense(1_200).copy(id = id, createdAtEpochMillis = 999L))

        assertEquals(5L, repository.getById(id)!!.createdAtEpochMillis)
    }

    @Test
    fun missingAccountRollsBackWholeOperation() = runBlocking<Unit> {
        val result = runCatching { repository.add(expense(1_000).copy(type = TransactionType.TRANSFER, transferAccountId = 99)) }

        assertTrue(result.isFailure)
        assertEquals(0, db.transactionDao().observeByDateRange(0L, Long.MAX_VALUE).first().size)
        assertEquals(10_000L, balance(1))
    }

    @Test
    fun updateAndDeleteReturnFalseForUnknownId() = runBlocking<Unit> {
        assertFalse(repository.update(expense(100).copy(id = 42)))
        assertFalse(repository.delete(42))
    }

    @Test
    fun malformedTransferCanBeDeletedWithoutTouchingBalances() = runBlocking<Unit> {
        val id = db.transactionDao().insert(expense(500).copy(type = TransactionType.TRANSFER, transferAccountId = null))

        assertTrue(repository.delete(id))
        assertEquals(10_000L, balance(1))
    }
}
