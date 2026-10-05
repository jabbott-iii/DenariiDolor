/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.backup

import androidx.room.withTransaction
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.TransactionType

/** Copies every table into a [BackupSnapshot], and replaces every table with one; each runs as a single database transaction. */
class BackupStore(private val database: AppDatabase) {
    private val dao = database.backupDao()

    suspend fun read(createdAtEpochMillis: Long, currencyCode: String): BackupSnapshot = database.withTransaction {
        BackupSnapshot(
            createdAtEpochMillis = createdAtEpochMillis,
            currencyCode = currencyCode,
            accounts = dao.accounts().map { BackupAccount(it.id, it.name, it.balanceCents) },
            categories = dao.categories().map { BackupCategory(it.id, it.name, it.iconName) },
            budgets = dao.budgets().map { BackupBudget(it.id, it.categoryId, it.monthlyLimitCents, it.warningThresholdPercent) },
            transactions = dao.transactions().map { it.toBackup() }
        )
    }

    /**
     * Deletes every row, then inserts the snapshot's with their IDs, so references and stored balances come back exactly as they
     * were. Stored balances are copied, not recomputed: opening balances live only there. If anything fails, the transaction
     * rolls back and the database is unchanged. Check [BackupSnapshot.isConsistent] first.
     */
    suspend fun replaceAll(snapshot: BackupSnapshot) = database.withTransaction {
        // Children before parents, so no foreign key is ever left dangling.
        dao.deleteTransactions()
        dao.deleteBudgets()
        dao.deleteCategories()
        dao.deleteAccounts()
        dao.insertAll(
            accounts = snapshot.accounts.map { AccountEntity(id = it.id, name = it.name, balanceCents = it.balanceCents) },
            categories = snapshot.categories.map { CategoryEntity(id = it.id, name = it.name, iconName = it.iconName) },
            budgets = snapshot.budgets.map {
                BudgetEntity(
                    id = it.id,
                    categoryId = it.categoryId,
                    monthlyLimitCents = it.monthlyLimitCents,
                    warningThresholdPercent = it.warningThresholdPercent
                )
            },
            transactions = snapshot.transactions.map { it.toEntity() }
        )
    }

    private fun TransactionEntity.toBackup() = BackupTransaction(
        id = id,
        type = type.name,
        description = description,
        amountCents = amountCents,
        categoryId = categoryId,
        accountId = accountId,
        transferAccountId = transferAccountId,
        dateEpochMillis = dateEpochMillis,
        createdAtEpochMillis = createdAtEpochMillis
    )

    private fun BackupTransaction.toEntity() = TransactionEntity(
        id = id,
        type = TransactionType.valueOf(type),
        description = description,
        amountCents = amountCents,
        categoryId = categoryId,
        accountId = accountId,
        transferAccountId = transferAccountId,
        dateEpochMillis = dateEpochMillis,
        createdAtEpochMillis = createdAtEpochMillis
    )
}
