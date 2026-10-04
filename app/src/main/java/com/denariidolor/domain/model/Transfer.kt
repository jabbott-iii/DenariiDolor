/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.model

data class Transfer(
    override val id: Long = 0,
    override val description: String,
    override val amountCents: Long,
    override val categoryId: Long,
    override val accountId: Long,
    val transferAccountId: Long,
    override val dateEpochMillis: Long
) : Transaction(id, description, amountCents, categoryId, accountId, dateEpochMillis) {
    override val type: TransactionType get() = TransactionType.TRANSFER

    override fun balanceImpact(): Long = 0L

    override fun accountImpacts(): Map<Long, Long> = mapOf(accountId to -amountCents, transferAccountId to amountCents)
}
