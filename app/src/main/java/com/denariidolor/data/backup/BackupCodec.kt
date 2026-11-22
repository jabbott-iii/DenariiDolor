/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.backup

/**
 * The snapshot as bytes, before encryption: values in `java.io.DataOutput`'s layout ([BigEndianWriter]) in a fixed order. After
 * the creation time and the currency code come the accounts, categories, budgets and transactions, each list as a count followed
 * by its records. A change to this layout needs a new [BackupFile.FORMAT_VERSION], and `decode` must keep reading every earlier
 * version.
 */
internal object BackupCodec {
    fun encode(snapshot: BackupSnapshot): ByteArray = with(BigEndianWriter()) {
        writeLong(snapshot.createdAtEpochMillis)
        writeUtf(snapshot.currencyCode)
        writeList(snapshot.accounts) {
            writeLong(it.id)
            writeUtf(it.name)
            writeLong(it.balanceCents)
        }
        writeList(snapshot.categories) {
            writeLong(it.id)
            writeUtf(it.name)
            writeUtf(it.iconName)
        }
        writeList(snapshot.budgets) {
            writeLong(it.id)
            writeLong(it.categoryId)
            writeLong(it.monthlyLimitCents)
            writeInt(it.warningThresholdPercent)
        }
        writeList(snapshot.transactions, ::writeTransaction)
        finish()
    }

    /** Throws [BackupException] with [BackupError.DAMAGED] unless [bytes] hold exactly one snapshot. */
    fun decode(bytes: ByteArray): BackupSnapshot {
        val input = BigEndianReader(bytes)
        val snapshot = BackupSnapshot(
            createdAtEpochMillis = input.readLong(),
            currencyCode = input.readUtf(),
            accounts = input.readList { BackupAccount(id = readLong(), name = readUtf(), balanceCents = readLong()) },
            categories = input.readList { BackupCategory(id = readLong(), name = readUtf(), iconName = readUtf()) },
            budgets = input.readList {
                BackupBudget(
                    id = readLong(),
                    categoryId = readLong(),
                    monthlyLimitCents = readLong(),
                    warningThresholdPercent = readInt()
                )
            },
            transactions = input.readList { readTransaction(this) }
        )
        if (!input.isExhausted) throw BackupException(BackupError.DAMAGED)
        return snapshot
    }

    private fun writeTransaction(out: BigEndianWriter, transaction: BackupTransaction) = with(out) {
        writeLong(transaction.id)
        writeUtf(transaction.type)
        writeUtf(transaction.description)
        writeLong(transaction.amountCents)
        writeLong(transaction.categoryId)
        writeLong(transaction.accountId)
        writeBoolean(transaction.transferAccountId != null)
        transaction.transferAccountId?.let { writeLong(it) }
        writeLong(transaction.dateEpochMillis)
        writeLong(transaction.createdAtEpochMillis)
    }

    private fun readTransaction(input: BigEndianReader) = with(input) {
        BackupTransaction(
            id = readLong(),
            type = readUtf(),
            description = readUtf(),
            amountCents = readLong(),
            categoryId = readLong(),
            accountId = readLong(),
            transferAccountId = if (readBoolean()) readLong() else null,
            dateEpochMillis = readLong(),
            createdAtEpochMillis = readLong()
        )
    }

    private inline fun <T> BigEndianWriter.writeList(items: List<T>, write: BigEndianWriter.(T) -> Unit) {
        writeInt(items.size)
        items.forEach { write(it) }
    }

    // No capacity is reserved from the count: a damaged count runs into the end of the data instead of allocating memory.
    private inline fun <T> BigEndianReader.readList(readItem: BigEndianReader.() -> T): List<T> {
        val count = readInt()
        if (count < 0) throw BackupException(BackupError.DAMAGED)
        val items = ArrayList<T>()
        repeat(count) { items += readItem() }
        return items
    }
}
