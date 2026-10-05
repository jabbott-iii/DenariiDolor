/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.backup

import com.denariidolor.domain.model.TransactionType

// The backup format has its own record types, so a change to Room's entities can't silently change the file format.

data class BackupAccount(val id: Long, val name: String, val balanceCents: Long)

data class BackupCategory(val id: Long, val name: String, val iconName: String)

data class BackupBudget(val id: Long, val categoryId: Long, val monthlyLimitCents: Long, val warningThresholdPercent: Int)

data class BackupTransaction(
    val id: Long,
    /** A [TransactionType] name. */
    val type: String,
    val description: String,
    val amountCents: Long,
    val categoryId: Long,
    val accountId: Long,
    val transferAccountId: Long?,
    val dateEpochMillis: Long,
    val createdAtEpochMillis: Long
)

/** Everything a backup holds: every table and the currency setting. The PIN, the security question and biometric sign-in never are. */
data class BackupSnapshot(
    val createdAtEpochMillis: Long,
    /** ISO 4217 code of the currency setting when the backup was made. */
    val currencyCode: String,
    val accounts: List<BackupAccount>,
    val categories: List<BackupCategory>,
    val budgets: List<BackupBudget>,
    val transactions: List<BackupTransaction>
) {
    /**
     * True when the snapshot can replace the database as is. The checks mirror the schema, so a backup of any database the app
     * could have written passes: IDs are positive and unique, names and budget categories are as unique as the indexes require,
     * the foreign keys (a transaction's category and account, a budget's category) point at rows of the snapshot, every type is
     * known, and the seeded defaults the app relies on are present. `transferAccountId` has no foreign key, so a transfer whose
     * destination is gone restores as it was; the read paths already treat it as malformed (BUG-06). Business rules such as
     * description length aren't checked again: rows from before a rule existed must still restore.
     */
    fun isConsistent(requiredAccountIds: Set<Long>, requiredCategoryIds: Set<Long>): Boolean {
        val accountIds = accounts.map(BackupAccount::id)
        val categoryIds = categories.map(BackupCategory::id)
        val budgetIds = budgets.map(BackupBudget::id)
        val transactionIds = transactions.map(BackupTransaction::id)
        val knownAccounts = accountIds.toSet()
        val knownCategories = categoryIds.toSet()
        val knownTypes = TransactionType.entries.mapTo(HashSet()) { it.name }
        val checks = sequenceOf(
            { (accountIds + categoryIds + budgetIds + transactionIds).all { it > 0 } },
            { listOf(accountIds, categoryIds, budgetIds, transactionIds).all(::isUnique) },
            { isUnique(accounts.map(BackupAccount::name)) && isUnique(categories.map(BackupCategory::name)) },
            { knownAccounts.containsAll(requiredAccountIds) && knownCategories.containsAll(requiredCategoryIds) },
            { isUnique(budgets.map(BackupBudget::categoryId)) && budgets.all { it.categoryId in knownCategories } },
            { transactions.all { it.type in knownTypes && it.categoryId in knownCategories && it.accountId in knownAccounts } }
        )
        return checks.all { check -> check() }
    }

    private fun isUnique(values: List<*>): Boolean = values.size == values.toSet().size
}
