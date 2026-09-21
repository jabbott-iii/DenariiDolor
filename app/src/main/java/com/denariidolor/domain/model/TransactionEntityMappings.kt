/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    type = type,
    description = description,
    amountCents = amountCents,
    categoryId = categoryId,
    accountId = accountId,
    transferAccountId = (this as? Transfer)?.transferAccountId,
    dateEpochMillis = dateEpochMillis
)
