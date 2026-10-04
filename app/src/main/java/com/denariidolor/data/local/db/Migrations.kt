/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: money columns move from REAL dollars to INTEGER cents
 * (`accounts.balance` → `balanceCents`, `budgets.monthlyLimit` → `monthlyLimitCents`,
 * `transactions.amount` → `amountCents`).
 *
 * Tables are rebuilt rather than altered because `ALTER TABLE … DROP COLUMN` needs SQLite 3.35+.
 * Room runs migrations before enabling foreign keys, so parent tables can be swapped safely.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `accounts_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `balanceCents` INTEGER NOT NULL)"
        )
        db.execSQL(
            "INSERT INTO `accounts_new` (`id`, `name`, `balanceCents`) " +
                "SELECT `id`, `name`, ${toCents("balance")} FROM `accounts`"
        )
        db.execSQL("DROP TABLE `accounts`")
        db.execSQL("ALTER TABLE `accounts_new` RENAME TO `accounts`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_name` ON `accounts` (`name`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `budgets_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`categoryId` INTEGER NOT NULL, `monthlyLimitCents` INTEGER NOT NULL, " +
                "`warningThresholdPercent` INTEGER NOT NULL, " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "INSERT INTO `budgets_new` (`id`, `categoryId`, `monthlyLimitCents`, `warningThresholdPercent`) " +
                "SELECT `id`, `categoryId`, ${toCents("monthlyLimit")}, `warningThresholdPercent` FROM `budgets`"
        )
        db.execSQL("DROP TABLE `budgets`")
        db.execSQL("ALTER TABLE `budgets_new` RENAME TO `budgets`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transactions_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`type` TEXT NOT NULL, `description` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, " +
                "`categoryId` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `transferAccountId` INTEGER, " +
                "`dateEpochMillis` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )"
        )
        db.execSQL(
            "INSERT INTO `transactions_new` (`id`, `type`, `description`, `amountCents`, `categoryId`, `accountId`, " +
                "`transferAccountId`, `dateEpochMillis`, `createdAtEpochMillis`) " +
                "SELECT `id`, `type`, `description`, ${toCents("amount")}, `categoryId`, `accountId`, " +
                "`transferAccountId`, `dateEpochMillis`, `createdAtEpochMillis` FROM `transactions`"
        )
        db.execSQL("DROP TABLE `transactions`")
        db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_dateEpochMillis` ON `transactions` (`dateEpochMillis`)")
    }

    private fun toCents(column: String) = "CAST(ROUND(`$column` * 100) AS INTEGER)"
}
