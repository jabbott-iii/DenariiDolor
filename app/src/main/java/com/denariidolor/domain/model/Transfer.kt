package com.denariidolor.domain.model

data class Transfer(
    override val id: Long = 0,
    override val description: String,
    override val amount: Double,
    override val categoryId: Long,
    override val accountId: Long,
    val transferAccountId: Long,
    override val dateEpochMillis: Long
) : Transaction(id, description, amount, categoryId, accountId, dateEpochMillis)
