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

package com.denariidolor.util

import com.denariidolor.domain.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

/** Money is stored and computed as whole cents (`Long`); these helpers are the only conversions to and from text. */
object Money {
    private const val SCALE = 2
    private const val CENTS_PER_UNIT = 100.0
    private val MAX_INPUT = BigDecimal("9999999999.99")
    private val DECIMAL = Regex("^-?\\d+(\\.\\d+)?$")

    /** Parses user input like `12`, `12.5`, `$1,234.56`; returns null for blanks, >2 decimals, or out-of-range values. */
    fun parseToCents(text: String): Long? {
        val cleaned = text.trim().replace(",", "").let { if (it.startsWith("-$")) "-" + it.drop(2) else it.removePrefix("$") }
        if (!DECIMAL.matches(cleaned)) return null
        val value = cleaned.toBigDecimal()
        if (value.stripTrailingZeros().scale() > SCALE || value.abs() > MAX_INPUT) return null
        return value.movePointRight(SCALE).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    }

    /** Plain decimal for CSV and input fields, e.g. `1234.50`. */
    fun toPlain(cents: Long): String = BigDecimal.valueOf(cents, SCALE).toPlainString()

    /** Editable text without trailing zeros, e.g. `4.5`, `250`. */
    fun toInput(cents: Long): String = BigDecimal.valueOf(cents, SCALE).stripTrailingZeros().toPlainString()

    fun toDouble(cents: Long): Double = cents / CENTS_PER_UNIT
}

fun formatMoney(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    return sign + "$" + Money.toPlain(kotlin.math.abs(cents))
}

fun formatSignedAmount(type: TransactionType, cents: Long): String {
    val sign = when (type) {
        TransactionType.EXPENSE -> "-"
        TransactionType.INCOME -> "+"
        TransactionType.TRANSFER -> ""
    }
    return sign + formatMoney(cents)
}
