/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.model

abstract class Transaction(
    open val id: Long = 0,
    open val description: String,
    open val amountCents: Long,
    open val categoryId: Long,
    open val accountId: Long,
    open val dateEpochMillis: Long
) {
    abstract val type: TransactionType

    /** Net effect on total wealth, in cents. */
    abstract fun balanceImpact(): Long

    /** Effect on each account's balance, in cents. */
    open fun accountImpacts(): Map<Long, Long> = mapOf(accountId to balanceImpact())
}
