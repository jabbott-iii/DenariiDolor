/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.data.backup.BackupAccount
import com.denariidolor.data.backup.BackupFile
import com.denariidolor.data.backup.BackupStore
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private lateinit var store: BackupStore

    private fun inMemoryDatabase() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Before
    fun setUp() = runBlocking<Unit> {
        db = inMemoryDatabase()
        store = BackupStore(db)
        db.accountDao().insert(AccountEntity(id = 1, name = "Cash", balanceCents = 10_000))
        db.accountDao().insert(AccountEntity(id = 2, name = "Savings", balanceCents = 2_500))
        db.accountDao().insert(AccountEntity(id = 7, name = "Card", balanceCents = -4_200))
        db.categoryDao().insert(CategoryEntity(id = 1, name = "General Expense"))
        db.categoryDao().insert(CategoryEntity(id = 2, name = "General Income", iconName = "income"))
        db.categoryDao().insert(CategoryEntity(id = 3, name = "Transfer", iconName = "transfer"))
        db.categoryDao().insert(CategoryEntity(id = 9, name = "Dining", iconName = "dining"))
        db.budgetDao().upsert(BudgetEntity(id = 4, categoryId = 9, monthlyLimitCents = 20_000, warningThresholdPercent = 75))
        db.transactionDao().insert(transaction(id = 11, type = TransactionType.EXPENSE, categoryId = 9, accountId = 7))
        db.transactionDao().insert(
            transaction(id = 12, type = TransactionType.TRANSFER, categoryId = 3, accountId = 1, transferAccountId = 2)
        )
    }

    @After
    fun tearDown() = db.close()

    private fun transaction(id: Long, type: TransactionType, categoryId: Long, accountId: Long, transferAccountId: Long? = null) =
        TransactionEntity(
            id = id,
            type = type,
            description = "Row $id",
            amountCents = id * 100,
            categoryId = categoryId,
            accountId = accountId,
            transferAccountId = transferAccountId,
            dateEpochMillis = 1_790_000_000_000L + id,
            createdAtEpochMillis = 1_790_000_100_000L + id
        )

    @Test
    fun aSnapshotHoldsEveryRowAndRestoresExactly() = runBlocking<Unit> {
        val snapshot = store.read(createdAtEpochMillis = 42L, currencyCode = "EUR")
        assertEquals(listOf(1L, 2L, 7L), snapshot.accounts.map { it.id })
        assertEquals(-4_200L, snapshot.accounts.last().balanceCents)
        assertEquals(listOf(11L, 12L), snapshot.transactions.map { it.id })
        assertEquals(2L, snapshot.transactions.last().transferAccountId)
        assertTrue(snapshot.isConsistent(setOf(1L, 2L), setOf(1L, 2L, 3L)))

        // Change everything, then restore.
        db.transactionDao().deleteById(11)
        db.budgetDao().deleteByCategoryId(9)
        db.accountDao().rename(7, "Renamed")
        db.accountDao().insert(AccountEntity(id = 20, name = "Added later", balanceCents = 1))
        store.replaceAll(snapshot)

        assertEquals(snapshot, store.read(createdAtEpochMillis = 42L, currencyCode = "EUR"))
    }

    @Test
    fun aFailedRestoreChangesNothing() = runBlocking<Unit> {
        val before = store.read(createdAtEpochMillis = 1L, currencyCode = "USD")
        // Two accounts with one name break the unique index halfway through the restore.
        val broken = before.copy(accounts = before.accounts + BackupAccount(30, "Cash", 0))

        val failed = runCatching { store.replaceAll(broken) }

        assertTrue(failed.isFailure)
        assertEquals(before, store.read(createdAtEpochMillis = 1L, currencyCode = "USD"))
    }

    @Test
    fun anEncryptedBackupRestoresIntoAnotherDatabase() = runBlocking<Unit> {
        val snapshot = store.read(createdAtEpochMillis = 7L, currencyCode = "BRL")
        val file = BackupFile.write(snapshot, PASSPHRASE, iterations = 100_000)
        val newPhone = inMemoryDatabase()
        try {
            val restoredStore = BackupStore(newPhone)
            restoredStore.replaceAll(BackupFile.read(file, PASSPHRASE))

            assertEquals(snapshot, restoredStore.read(createdAtEpochMillis = 7L, currencyCode = "BRL"))
        } finally {
            newPhone.close()
        }
    }

    private companion object {
        const val PASSPHRASE = "correct horse battery staple"
    }
}
