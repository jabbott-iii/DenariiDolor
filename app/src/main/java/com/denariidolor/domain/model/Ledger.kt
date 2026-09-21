package com.denariidolor.domain.model

object Ledger {
    fun balanceDeltas(previous: Transaction?, current: Transaction?): Map<Long, Double> {
        val deltas = mutableMapOf<Long, Double>()
        previous?.accountImpacts()?.forEach { (accountId, impact) -> deltas.merge(accountId, -impact) { a, b -> a + b } }
        current?.accountImpacts()?.forEach { (accountId, impact) -> deltas.merge(accountId, impact) { a, b -> a + b } }
        return deltas.filterValues { it != 0.0 }
    }
}
