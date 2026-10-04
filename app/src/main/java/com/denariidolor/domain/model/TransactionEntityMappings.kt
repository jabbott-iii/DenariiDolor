/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.model

import com.denariidolor.data.local.db.entity.TransactionEntity

fun TransactionEntity.toDomainTransaction(): Transaction = when (type) {
    TransactionType.EXPENSE -> Expense(id, description, amountCents, categoryId, accountId, dateEpochMillis)
    TransactionType.INCOME -> Income(id, description, amountCents, categoryId, accountId, dateEpochMillis)
    TransactionType.TRANSFER -> Transfer(
        id = id,
        description = description,
        amountCents = amountCents,
        categoryId = categoryId,
        accountId = accountId,
        transferAccountId = requireNotNull(transferAccountId) { "Transfer destination is required for transaction $id" },
        dateEpochMillis = dateEpochMillis
    )
}

/** A TRANSFER row without a destination. Validation never writes one, but the schema doesn't prevent it. */
val TransactionEntity.isMalformedTransfer: Boolean get() = type == TransactionType.TRANSFER && transferAccountId == null

/**
 * The mapping for rows read back from the database. A malformed transfer maps to null and has no balance impact, so one bad row
 * can't crash the screen or the edit that reads it (BUG-06).
 */
fun TransactionEntity.toDomainTransactionOrNull(): Transaction? = if (isMalformedTransfer) null else toDomainTransaction()

/** The mapping for writes; it also trims the description (BUG-08). */
fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    type = type,
    description = description.trim(),
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    transferAccountId = (this as? Transfer)?.transferAccountId,
    dateEpochMillis = dateEpochMillis
)
