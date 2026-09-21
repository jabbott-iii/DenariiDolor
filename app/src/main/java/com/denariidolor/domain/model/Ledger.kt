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

object Ledger {
    /** Per-account balance changes (cents) for add (`null → new`), edit (`old → new`) and delete (`old → null`). */
    fun balanceDeltas(previous: Transaction?, current: Transaction?): Map<Long, Long> {
        val deltas = mutableMapOf<Long, Long>()
        previous?.accountImpacts()?.forEach { (accountId, impact) -> deltas.merge(accountId, -impact) { a, b -> a + b } }
        current?.accountImpacts()?.forEach { (accountId, impact) -> deltas.merge(accountId, impact) { a, b -> a + b } }
        return deltas.filterValues { it != 0L }
    }
}
