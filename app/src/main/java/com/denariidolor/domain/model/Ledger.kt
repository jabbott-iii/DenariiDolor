/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.model

object Ledger {
    /** Per-account balance changes (cents) for add (`null → new`), edit (`old → new`) and delete (`old → null`). */
    fun balanceDeltas(previous: Transaction?, current: Transaction?): Map<Long, Long> {
        val deltas = mutableMapOf<Long, Long>()
        previous?.accountImpacts()?.forEach { (accountId, impact) -> deltas.merge(accountId, -impact) { a, b -> a + b } }
        current?.accountImpacts()?.forEach { (accountId, impact) -> deltas.merge(accountId, impact) { a, b -> a + b } }
        return deltas.filterValues { it != 0L }
    }
}
