package com.denariidolor.domain.model

data class Budget(
    val id: Long = 0,
    val categoryId: Long,
    val monthlyLimit: Double,
    val warningThresholdPercent: Int = 80
)
