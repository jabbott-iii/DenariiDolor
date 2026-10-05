/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * The snapshot as bytes, before encryption: `DataOutput` values in a fixed order. After the creation time and the currency
 * code come the accounts, categories, budgets and transactions, each list as a count followed by its records. A change to
 * this layout needs a new [BackupFile.FORMAT_VERSION], and `decode` must keep reading every earlier version.
 */
internal object BackupCodec {
    fun encode(snapshot: BackupSnapshot): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeLong(snapshot.createdAtEpochMillis)
            out.writeUTF(snapshot.currencyCode)
            out.writeList(snapshot.accounts) {
                writeLong(it.id)
                writeUTF(it.name)
                writeLong(it.balanceCents)
            }
            out.writeList(snapshot.categories) {
                writeLong(it.id)
                writeUTF(it.name)
                writeUTF(it.iconName)
            }
            out.writeList(snapshot.budgets) {
                writeLong(it.id)
                writeLong(it.categoryId)
                writeLong(it.monthlyLimitCents)
                writeInt(it.warningThresholdPercent)
            }
            out.writeList(snapshot.transactions, ::writeTransaction)
        }
        return bytes.toByteArray()
    }

    /** Throws [BackupException] with [BackupError.DAMAGED] unless [bytes] hold exactly one snapshot. */
    fun decode(bytes: ByteArray): BackupSnapshot = try {
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            val snapshot = BackupSnapshot(
                createdAtEpochMillis = input.readLong(),
                currencyCode = input.readUTF(),
                accounts = input.readList { BackupAccount(id = readLong(), name = readUTF(), balanceCents = readLong()) },
                categories = input.readList { BackupCategory(id = readLong(), name = readUTF(), iconName = readUTF()) },
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
            if (input.read() != -1) throw BackupException(BackupError.DAMAGED)
            snapshot
        }
    } catch (e: IOException) {
        throw BackupException(BackupError.DAMAGED, e)
    }

    private fun writeTransaction(out: DataOutputStream, transaction: BackupTransaction) = with(out) {
        writeLong(transaction.id)
        writeUTF(transaction.type)
        writeUTF(transaction.description)
        writeLong(transaction.amountCents)
        writeLong(transaction.categoryId)
        writeLong(transaction.accountId)
        writeBoolean(transaction.transferAccountId != null)
        transaction.transferAccountId?.let { writeLong(it) }
        writeLong(transaction.dateEpochMillis)
        writeLong(transaction.createdAtEpochMillis)
    }

    private fun readTransaction(input: DataInputStream) = with(input) {
        BackupTransaction(
            id = readLong(),
            type = readUTF(),
            description = readUTF(),
            amountCents = readLong(),
            categoryId = readLong(),
            accountId = readLong(),
            transferAccountId = if (readBoolean()) readLong() else null,
            dateEpochMillis = readLong(),
            createdAtEpochMillis = readLong()
        )
    }

    private inline fun <T> DataOutputStream.writeList(items: List<T>, write: DataOutputStream.(T) -> Unit) {
        writeInt(items.size)
        items.forEach { write(it) }
    }

    // No capacity is reserved from the count: a damaged count runs into the end of the data instead of allocating memory.
    private inline fun <T> DataInputStream.readList(readItem: DataInputStream.() -> T): List<T> {
        val count = readInt()
        if (count < 0) throw BackupException(BackupError.DAMAGED)
        val items = ArrayList<T>()
        repeat(count) { items += readItem() }
        return items
    }
}
