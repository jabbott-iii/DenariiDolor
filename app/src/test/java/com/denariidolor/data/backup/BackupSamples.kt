/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.backup

internal val DEFAULT_ACCOUNT_IDS = setOf(1L, 2L)
internal val DEFAULT_CATEGORY_IDS = setOf(1L, 2L, 3L)

/** A snapshot with every kind of row, a transfer, a malformed transfer (BUG-06), and text in several scripts. */
internal fun sampleSnapshot() = BackupSnapshot(
    createdAtEpochMillis = 1_791_150_000_000L,
    currencyCode = "INR",
    accounts = listOf(
        BackupAccount(1, "Cash", 12_345),
        BackupAccount(2, "Savings", 0),
        BackupAccount(7, "Carte de crédit", -98_765)
    ),
    categories = listOf(
        BackupCategory(1, "General Expense", "ic_category_default"),
        BackupCategory(2, "General Income", "income"),
        BackupCategory(3, "Transfer", "transfer"),
        BackupCategory(9, "مطاعم 🍜", "dining")
    ),
    budgets = listOf(BackupBudget(4, 9, 20_000, 80)),
    transactions = listOf(
        BackupTransaction(1, "EXPENSE", "Café au lait", 450, 9, 1, null, 1_790_000_000_000L, 1_790_000_000_500L),
        BackupTransaction(2, "INCOME", "工资", 500_000, 2, 7, null, 1_790_100_000_000L, 1_790_100_000_500L),
        BackupTransaction(3, "TRANSFER", "To savings", 10_000, 3, 1, 2, 1_790_200_000_000L, 1_790_200_000_500L),
        BackupTransaction(5, "TRANSFER", "Before BUG-06", 1_000, 3, 1, null, 1_790_300_000_000L, 1_790_300_000_500L)
    )
)
