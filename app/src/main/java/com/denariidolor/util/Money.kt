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
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Money is stored and computed as whole cents (`Long`); these helpers are the only conversions to and from text. */
object Money {
    private const val SCALE = 2
    private const val CENTS_PER_UNIT = 100.0
    private const val GROUP_DIGITS = 3
    private val MAX_INPUT = BigDecimal("9999999999.99")
    private val DECIMAL_MARKS = setOf('.', ',')

    // Spaces (regular, no-break, narrow no-break) and apostrophes only ever group thousands, e.g. fr `1 234,5`, de-CH `1'234.5`.
    private val GROUP_ONLY_MARKS = setOf(' ', ' ', ' ', '\'', '’')

    /**
     * Parses user input such as `12`, `12.5`, `$1,234.56`, `12,50`, `1.234,56` or `1 234,56` into cents.
     *
     * Either `.` or `,` can be the decimal mark. With both present the last one is; a single mark followed by one or two
     * digits always is. Only a single mark followed by exactly three digits is ambiguous (`1,234`): it is the decimal mark
     * if it is [locale]'s decimal separator, and a thousands separator otherwise.
     * Returns null for blanks, malformed grouping, more than 2 decimals, or out-of-range values.
     */
    fun parseToCents(text: String, locale: Locale = Locale.getDefault(Locale.Category.FORMAT)): Long? {
        val (negative, body) = splitSign(text) ?: return null
        val value = parseUnsigned(body, DecimalFormatSymbols.getInstance(locale).decimalSeparator) ?: return null
        if (value.stripTrailingZeros().scale() > SCALE || value > MAX_INPUT) return null
        val cents = value.movePointRight(SCALE).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
        return if (negative) -cents else cents
    }

    /** Accepts `-12`, `-$12`, `$-12` and `$12`; returns whether the amount is negative and the unsigned text. */
    private fun splitSign(text: String): Pair<Boolean, String>? {
        var body = text.trim()
        val leadingMinus = body.startsWith('-')
        if (leadingMinus) body = body.drop(1)
        body = body.removePrefix("$")
        val minusAfterSymbol = !leadingMinus && body.startsWith('-')
        if (minusAfterSymbol) body = body.drop(1)
        return if (body.isEmpty()) null else (leadingMinus || minusAfterSymbol) to body
    }

    private fun parseUnsigned(body: String, localeDecimal: Char): BigDecimal? {
        if (body.any { !it.isAsciiDigit() && it !in DECIMAL_MARKS && it !in GROUP_ONLY_MARKS }) return null
        val decimalIndex = decimalMarkIndex(body, localeDecimal)
        val whole = ungroup(if (decimalIndex < 0) body else body.substring(0, decimalIndex)) ?: return null
        val fraction = if (decimalIndex < 0) "" else body.substring(decimalIndex + 1)
        val fractionOk = decimalIndex < 0 || (fraction.isNotEmpty() && fraction.all { it.isAsciiDigit() })
        return if (fractionOk && (whole + fraction).isNotEmpty()) {
            BigDecimal(whole.ifEmpty { "0" } + if (fraction.isEmpty()) "" else ".$fraction")
        } else {
            null
        }
    }

    /** Index of the decimal mark in [body], or -1 when every `.`/`,` in it groups thousands. */
    private fun decimalMarkIndex(body: String, localeDecimal: Char): Int {
        val marks = body.indices.filter { body[it] in DECIMAL_MARKS }
        if (marks.isEmpty()) return -1
        val last = marks.last()
        return when {
            marks.any { body[it] != body[last] } -> last
            marks.size > 1 -> -1
            body.length - last - 1 == GROUP_DIGITS && body[last] != localeDecimal -> -1
            else -> last
        }
    }

    /** The whole-number digits with valid thousands grouping removed, or null when the grouping is malformed. */
    private fun ungroup(whole: String): String? {
        val separators = whole.filterNot { it.isAsciiDigit() }.toSet()
        if (separators.isEmpty()) return whole
        val groups = whole.split(separators.first())
        val wellFormed = separators.size == 1 &&
            groups.first().length in 1..GROUP_DIGITS &&
            groups.drop(1).all { it.length == GROUP_DIGITS }
        return if (wellFormed) groups.joinToString("") else null
    }

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

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
