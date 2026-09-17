package com.denariidolor.domain.model

abstract class Transaction(
    open val id: Long = 0,
    open val description: String,
    open val amount: Double,
    open val categoryId: Long,
    open val accountId: Long,
    open val dateEpochMillis: Long
) {
    abstract fun balanceImpact(): Double
}
