/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSnapshotTest {
    private val sample = sampleSnapshot()

    private fun BackupSnapshot.consistent() = isConsistent(DEFAULT_ACCOUNT_IDS, DEFAULT_CATEGORY_IDS)

    @Test
    fun aSnapshotOfAValidDatabaseIsConsistent() {
        assertTrue(sample.consistent())
    }

    @Test
    fun theSeededDefaultsMustBePresent() {
        assertFalse(sample.copy(accounts = sample.accounts.filter { it.id != 2L }).consistent())
        assertFalse(sample.copy(categories = sample.categories.filter { it.id != 3L }, transactions = emptyList()).consistent())
    }

    @Test
    fun idsMustBePositiveAndUnique() {
        assertFalse(sample.copy(accounts = sample.accounts + BackupAccount(7, "Another", 0)).consistent())
        assertFalse(sample.copy(transactions = sample.transactions + sample.transactions.first()).consistent())
        assertFalse(sample.copy(budgets = listOf(BackupBudget(0, 9, 1_000, 80))).consistent())
    }

    @Test
    fun namesMustBeUniqueLikeTheDatabaseRequires() {
        assertFalse(sample.copy(accounts = sample.accounts + BackupAccount(8, "Cash", 0)).consistent())
        assertFalse(sample.copy(categories = sample.categories + BackupCategory(10, "Transfer", "transfer")).consistent())
        // The unique indexes compare exactly, so names differing only in case can both be restored.
        assertTrue(sample.copy(accounts = sample.accounts + BackupAccount(8, "cash", 0)).consistent())
    }

    @Test
    fun everyReferenceMustPointAtARowOfTheSnapshot() {
        val first = sample.transactions.first()
        listOf(first.copy(categoryId = 99), first.copy(accountId = 99)).forEach { dangling ->
            assertFalse(dangling.toString(), sample.copy(transactions = listOf(dangling)).consistent())
        }
        assertFalse(sample.copy(budgets = listOf(BackupBudget(4, 99, 1_000, 80))).consistent())
    }

    /** `transferAccountId` has no foreign key, so a database can hold such a row, and its backup must restore (BUG-06). */
    @Test
    fun aTransferWhoseDestinationIsGoneStillRestores() {
        val orphan = sample.transactions.first().copy(type = "TRANSFER", transferAccountId = 99)

        assertTrue(sample.copy(transactions = sample.transactions + orphan.copy(id = 50)).consistent())
    }

    @Test
    fun aCategoryHasAtMostOneBudget() {
        assertFalse(sample.copy(budgets = sample.budgets + BackupBudget(5, 9, 5_000, 90)).consistent())
    }

    @Test
    fun transactionTypesMustBeKnown() {
        assertFalse(sample.copy(transactions = listOf(sample.transactions.first().copy(type = "expense"))).consistent())
        assertFalse(sample.copy(transactions = listOf(sample.transactions.first().copy(type = "REFUND"))).consistent())
    }
}
