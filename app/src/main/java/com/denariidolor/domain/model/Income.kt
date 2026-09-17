package com.denariidolor.domain.model

data class Income(
    override val id: Long = 0,
    override val description: String,
    override val amount: Double,
    override val categoryId: Long,
    override val accountId: Long,
    override val dateEpochMillis: Long
) : Transaction(id, description, amount, categoryId, accountId, dateEpochMillis) {
    override fun balanceImpact(): Double = amount
}
