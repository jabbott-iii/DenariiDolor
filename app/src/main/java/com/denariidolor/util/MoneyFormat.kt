package com.denariidolor.util

import java.util.Locale

fun formatMoney(amount: Double): String = String.format(Locale.US, "$%.2f", amount)

fun formatSignedAmount(type: String, amount: Double): String {
    val sign = when (type) {
        "EXPENSE" -> "-"
        "INCOME" -> "+"
        else -> ""
    }
    return sign + formatMoney(amount)
}
