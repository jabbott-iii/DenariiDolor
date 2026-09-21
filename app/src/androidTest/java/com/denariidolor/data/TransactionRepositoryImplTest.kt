package com.denariidolor.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.repository.TransactionRepositoryImpl
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
        db.accountDao().insert(AccountEntity(id = 1, name = "Cash", balance = 100.0))
        db.accountDao().insert(AccountEntity(id = 2, name = "Savings", balance = 0.0))
        db.categoryDao().insert(CategoryEntity(id = 1, name = "General"))
        Unit
    }

    @After
    fun tearDown() = db.close()

    private suspend fun balance(id: Long) = db.accountDao().getById(id)!!.balance

    private fun expense(amount: Double, accountId: Long = 1) = TransactionEntity(
        type = "EXPENSE",
        description = "Coffee",
        amount = amount,
        categoryId = 1,
        accountId = accountId,
        dateEpochMillis = 1_000L
    )

    @Test
    fun addUpdateDeleteKeepBalancesInSync() = runBlocking {
        val id = repository.add(expense(30.0))
        assertEquals(70.0, balance(1), 0.0001)

        assertTrue(repository.update(expense(50.0, accountId = 2).copy(id = id)))
        assertEquals(100.0, balance(1), 0.0001)
        assertEquals(-50.0, balance(2), 0.0001)

        assertTrue(repository.delete(id))
        assertEquals(0.0, balance(2), 0.0001)
        assertNull(repository.getById(id))
    }

    @Test
    fun transferDebitsSourceAndCreditsDestination() = runBlocking {
        repository.add(expense(40.0).copy(type = "TRANSFER", transferAccountId = 2))

        assertEquals(60.0, balance(1), 0.0001)
        assertEquals(40.0, balance(2), 0.0001)
    }

    @Test
    fun updatePreservesCreatedAt() = runBlocking {
        val id = repository.add(expense(10.0).copy(createdAtEpochMillis = 5L))
        repository.update(expense(12.0).copy(id = id, createdAtEpochMillis = 999L))

        assertEquals(5L, repository.getById(id)!!.createdAtEpochMillis)
    }

    @Test
    fun missingAccountRollsBackWholeOperation() = runBlocking {
        val result = runCatching { repository.add(expense(10.0).copy(type = "TRANSFER", transferAccountId = 99)) }

        assertTrue(result.isFailure)
        assertEquals(0, db.transactionDao().getByDateRange(0L, Long.MAX_VALUE).size)
        assertEquals(100.0, balance(1), 0.0001)
    }

    @Test
    fun updateAndDeleteReturnFalseForUnknownId() = runBlocking {
        assertFalse(repository.update(expense(1.0).copy(id = 42)))
        assertFalse(repository.delete(42))
    }
}
