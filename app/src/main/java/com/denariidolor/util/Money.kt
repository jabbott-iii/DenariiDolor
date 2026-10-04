/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import com.denariidolor.domain.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Money is stored and computed as whole cents (`Long`); these helpers are the only conversions to and from text. */
object Money {
    private const val SCALE = 2
    private const val CENTS_PER_UNIT = 100.0
    private const val GROUP_DIGITS = 3
    private val MAX_INPUT = BigDecimal("9999999999.99")
    private val DECIMAL_MARKS = setOf('.', ',')
    private const val ARABIC_DECIMAL_SEPARATOR = '\u066B'
    private const val ARABIC_THOUSANDS_SEPARATOR = '\u066C'
    private const val MINUS_SIGN = '\u2212'
    private const val DECIMAL_RADIX = 10

    // Left-to-right, right-to-left and Arabic letter marks, which formatted Arabic and Hebrew amounts contain.
    private val DIRECTION_MARKS = setOf('\u200E', '\u200F', '\u061C')

    /** The currency amounts are shown in. `CurrencyPreferences` keeps it in step with the saved setting. */
    @Volatile
    var currency: Currency = Currencies.DEFAULT

    // Spaces (regular, no-break, narrow no-break) and apostrophes only ever group thousands, e.g. fr `1 234,5`, de-CH `1'234.5`.
    private val GROUP_ONLY_MARKS = setOf(' ', ' ', ' ', '\'', '’')

    /**
     * Parses user input such as `12`, `12.5`, `$1,234.56`, `12,50 €`, `1.234,56`, `1 234,56` or `١٢٫٥` into cents.
     * The currency's symbol or code may come before or after the number, and any Unicode digits count (see [normalizeDigits]).
     *
     * Either `.` or `,` can be the decimal mark. With both present the last one is; a single mark followed by one or two
     * digits always is. Only a single mark followed by exactly three digits is ambiguous (`1,234`): it is the decimal mark
     * if it is [locale]'s decimal separator, and a thousands separator otherwise.
     * Returns null for blanks, malformed grouping, more than 2 decimals, or out-of-range values.
     */
    fun parseToCents(text: String, locale: Locale = Locale.getDefault(Locale.Category.FORMAT), currency: Currency = this.currency): Long? {
        val (negative, body) = splitSign(normalizeDigits(text), locale, currency) ?: return null
        val localeDecimal = normalizeDigits(DecimalFormatSymbols.getInstance(locale).decimalSeparator.toString()).single()
        val value = parseUnsigned(body, localeDecimal) ?: return null
        if (value.stripTrailingZeros().scale() > SCALE || value > MAX_INPUT) return null
        val cents = value.movePointRight(SCALE).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
        return if (negative) -cents else cents
    }

    /**
     * ASCII digits for any Unicode decimal digits (Arabic-Indic `٣`, Devanagari `३`, …), `.` and `,` for the Arabic decimal and
     * thousands separators, `-` for the minus sign, and no direction marks.
     */
    fun normalizeDigits(text: String): String = buildString(text.length) {
        text.forEach { char ->
            when {
                char in '0'..'9' -> append(char)
                Character.isDigit(char) -> append('0' + Character.digit(char, DECIMAL_RADIX))
                char == ARABIC_DECIMAL_SEPARATOR -> append('.')
                char == ARABIC_THOUSANDS_SEPARATOR -> append(',')
                char == MINUS_SIGN -> append('-')
                char !in DIRECTION_MARKS -> append(char)
            }
        }
    }

    /** Accepts `-12`, `-$12`, `$-12`, `$12` and `12 €`; returns whether the amount is negative and the unsigned text. */
    private fun splitSign(text: String, locale: Locale, currency: Currency): Pair<Boolean, String>? {
        var body = text.trim()
        val leadingMinus = body.startsWith('-')
        if (leadingMinus) body = body.drop(1)
        body = stripCurrency(body, locale, currency)
        val minusAfterSymbol = !leadingMinus && body.startsWith('-')
        if (minusAfterSymbol) body = body.drop(1)
        return if (body.isEmpty()) null else (leadingMinus || minusAfterSymbol) to body
    }

    /** Removes the currency's symbol or code (`R$`, `US$`, `EUR`, `ر.س.`) and any currency sign from either end. */
    private fun stripCurrency(text: String, locale: Locale, currency: Currency): String {
        val marks = listOf(currency.getSymbol(locale), currency.symbol, currency.currencyCode)
            .map(::normalizeDigits)
            .filter(String::isNotBlank)
            .distinct()
            .sortedByDescending(String::length)
        var body = text.trim()
        marks.forEach { mark -> body = body.removePrefix(mark).removeSuffix(mark).trim() }
        return body.trim { it.isWhitespace() || Character.getType(it) == Character.CURRENCY_SYMBOL.toInt() }
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

/**
 * [cents] in [currency], written the way [locale] writes money: `$1,234.50` (en-US), `1 234,50 €` (fr-FR), `₹1,23,456.50`
 * (hi-IN). [showPlus] adds a `+` to positive amounts.
 */
fun formatMoney(
    cents: Long,
    currency: Currency = Money.currency,
    locale: Locale = Locale.getDefault(Locale.Category.FORMAT),
    showPlus: Boolean = false
): String {
    val format = NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = currency
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    if (showPlus && cents > 0 && format is DecimalFormat) format.positivePrefix = "+" + format.positivePrefix
    return format.format(BigDecimal.valueOf(cents, 2))
}

/** An amount as a row shows it: expenses negative, income with a `+`, transfers unsigned. */
fun formatSignedAmount(
    type: TransactionType,
    cents: Long,
    currency: Currency = Money.currency,
    locale: Locale = Locale.getDefault(Locale.Category.FORMAT)
): String = when (type) {
    TransactionType.EXPENSE -> formatMoney(-cents, currency, locale)
    TransactionType.INCOME -> formatMoney(cents, currency, locale, showPlus = true)
    TransactionType.TRANSFER -> formatMoney(cents, currency, locale)
}
