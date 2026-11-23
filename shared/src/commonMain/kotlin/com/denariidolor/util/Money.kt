/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import com.denariidolor.domain.model.TransactionType
import kotlin.concurrent.Volatile
import kotlin.math.abs

/** What parsing needs from a locale: its decimal separator, and the symbols and code it writes a currency with. */
data class NumberSymbols(val decimalSeparator: Char, val currencyMarks: List<String>)

/**
 * Money is stored and computed as whole cents (`Long`); these helpers are the only conversions to and from text. The rules are
 * plain Kotlin; only what a locale calls its marks, and how it writes money, come from the platform (PlatformFormat.kt).
 */
object Money {
    private const val SCALE = 2
    private const val CENTS_PER_UNIT = 100L
    private const val GROUP_DIGITS = 3

    // At most 9,999,999,999.99.
    private const val MAX_WHOLE_DIGITS = 10
    private val DECIMAL_MARKS = setOf('.', ',')
    private const val ARABIC_DECIMAL_SEPARATOR = '\u066B'
    private const val ARABIC_THOUSANDS_SEPARATOR = '\u066C'
    private const val MINUS_SIGN = '\u2212'

    // Left-to-right, right-to-left and Arabic letter marks, which formatted Arabic and Hebrew amounts contain.
    private val DIRECTION_MARKS = setOf('\u200E', '\u200F', '\u061C')

    /** The currency amounts are shown in. `CurrencyPreferences` keeps it in step with the saved setting. */
    @Volatile
    var currency: PlatformCurrency = Currencies.DEFAULT

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
    fun parseToCents(text: String, locale: PlatformLocale = defaultFormatLocale(), currency: PlatformCurrency = this.currency): Long? =
        parseToCents(text, localeNumberSymbols(locale, currency))

    /** [parseToCents] with [symbols] in place of a locale and currency. */
    fun parseToCents(text: String, symbols: NumberSymbols): Long? {
        val (negative, body) = splitSign(normalizeDigits(text), symbols.currencyMarks) ?: return null
        val localeDecimal = normalizeDigits(symbols.decimalSeparator.toString()).single()
        val (whole, fraction) = parseUnsigned(body, localeDecimal) ?: return null
        val cents = toCents(whole, fraction) ?: return null
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
                char.isDigit() -> append('0' + char.digitToInt())
                char == ARABIC_DECIMAL_SEPARATOR -> append('.')
                char == ARABIC_THOUSANDS_SEPARATOR -> append(',')
                char == MINUS_SIGN -> append('-')
                char !in DIRECTION_MARKS -> append(char)
            }
        }
    }

    /** Accepts `-12`, `-$12`, `$-12`, `$12` and `12 €`; returns whether the amount is negative and the unsigned text. */
    private fun splitSign(text: String, currencyMarks: List<String>): Pair<Boolean, String>? {
        var body = text.trim()
        val leadingMinus = body.startsWith('-')
        if (leadingMinus) body = body.drop(1)
        body = stripCurrency(body, currencyMarks)
        val minusAfterSymbol = !leadingMinus && body.startsWith('-')
        if (minusAfterSymbol) body = body.drop(1)
        return if (body.isEmpty()) null else (leadingMinus || minusAfterSymbol) to body
    }

    /** Removes the currency's symbol or code (`R$`, `US$`, `EUR`, `ر.س.`) and any currency sign from either end. */
    private fun stripCurrency(text: String, currencyMarks: List<String>): String {
        val marks = currencyMarks
            .map(::normalizeDigits)
            .filter(String::isNotBlank)
            .distinct()
            .sortedByDescending(String::length)
        var body = text.trim()
        marks.forEach { mark -> body = body.removePrefix(mark).removeSuffix(mark).trim() }
        return body.trim { it.isWhitespace() || it.category == CharCategory.CURRENCY_SYMBOL }
    }

    /** The whole-number and fraction digits, or null when [body] isn't a number. */
    private fun parseUnsigned(body: String, localeDecimal: Char): Pair<String, String>? {
        if (body.any { !it.isAsciiDigit() && it !in DECIMAL_MARKS && it !in GROUP_ONLY_MARKS }) return null
        val decimalIndex = decimalMarkIndex(body, localeDecimal)
        val whole = ungroup(if (decimalIndex < 0) body else body.substring(0, decimalIndex)) ?: return null
        val fraction = if (decimalIndex < 0) "" else body.substring(decimalIndex + 1)
        val fractionOk = decimalIndex < 0 || (fraction.isNotEmpty() && fraction.all { it.isAsciiDigit() })
        return if (fractionOk && (whole + fraction).isNotEmpty()) whole to fraction else null
    }

    /** Null for more than 2 decimals (trailing zeros aside) or more than 9,999,999,999.99. */
    private fun toCents(whole: String, fraction: String): Long? {
        val units = whole.trimStart('0')
        val decimals = fraction.trimEnd('0')
        if (decimals.length > SCALE || units.length > MAX_WHOLE_DIGITS) return null
        return units.ifEmpty { "0" }.toLong() * CENTS_PER_UNIT + decimals.padEnd(SCALE, '0').toLong()
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
    fun toPlain(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val units = abs(cents / CENTS_PER_UNIT)
        val rest = abs(cents % CENTS_PER_UNIT)
        return "$sign$units." + rest.toString().padStart(SCALE, '0')
    }

    /** Editable text without trailing zeros, e.g. `4.5`, `250`. */
    fun toInput(cents: Long): String = toPlain(cents).trimEnd('0').removeSuffix(".")

    fun toDouble(cents: Long): Double = cents / CENTS_PER_UNIT.toDouble()
}

/**
 * [cents] in [currency], written the way [locale] writes money: `$1,234.50` (en-US), `1 234,50 €` (fr-FR), `₹1,23,456.50`
 * (hi-IN). [showPlus] adds a `+` to positive amounts.
 */
fun formatMoney(
    cents: Long,
    currency: PlatformCurrency = Money.currency,
    locale: PlatformLocale = defaultFormatLocale(),
    showPlus: Boolean = false
): String = localeMoneyText(cents, currency, locale, showPlus)

/** An amount as a row shows it: expenses negative, income with a `+`, transfers unsigned. */
fun formatSignedAmount(
    type: TransactionType,
    cents: Long,
    currency: PlatformCurrency = Money.currency,
    locale: PlatformLocale = defaultFormatLocale()
): String = when (type) {
    TransactionType.EXPENSE -> formatMoney(-cents, currency, locale)
    TransactionType.INCOME -> formatMoney(cents, currency, locale, showPlus = true)
    TransactionType.TRANSFER -> formatMoney(cents, currency, locale)
}
