package com.denariidolor.domain.model

import com.denariidolor.data.local.db.entity.TransactionEntity

fun TransactionEntity.toDomainTransaction(): Transaction {
    return when (type) {
        "EXPENSE" -> Expense(
            id = id,
            description = description,
            amount = amount,
            categoryId = categoryId,
            accountId = accountId,
            dateEpochMillis = dateEpochMillis
        )

        "INCOME" -> Income(
            id = id,
            description = description,
            amount = amount,
            categoryId = categoryId,
            accountId = accountId,
            dateEpochMillis = dateEpochMillis
        )

        "TRANSFER" -> Transfer(
            id = id,
            description = description,
            amount = amount,
            categoryId = categoryId,
            accountId = accountId,
            transferAccountId = requireNotNull(transferAccountId) { "Transfer destination is required for transaction $id" },
            dateEpochMillis = dateEpochMillis
        )

        else -> throw IllegalArgumentException("Unsupported transaction type: $type")
    }
}
