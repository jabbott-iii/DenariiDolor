/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [Money] once parsed and scaled with BigDecimal and java.lang.Character. These compare it with that code, kept below as
 * [BigDecimalMoney], over many generated inputs in the app's locales, so no amount a user types or sees changes.
 */
class MoneyCompatibilityTest {
    @Test
    fun typedAmountsParseAsTheyDid() {
        val random = Random(SEED)
        cases("USD", "EUR").forEach { (locale, currency) ->
            val inputs = FIXED_INPUTS + List(GENERATED_PER_CASE) { numberLike(random, locale, currency) } +
                List(GENERATED_PER_CASE) { noise(random) }
            inputs.forEach { text -> assertParsesAsBefore(text, locale, currency) }
        }
    }

    @Test
    fun shownAmountsParseBackAsTheyDid() {
        cases("USD").forEach { (locale, currency) ->
            val shown = AMOUNTS.flatMap { listOf(formatMoney(it, currency, locale), formatMoney(it, currency, locale, showPlus = true)) }
            shown.forEach { text -> assertParsesAsBefore(text, locale, currency) }
        }
    }

    /** Each locale with its region currency and [others]. */
    private fun cases(vararg others: String): List<Pair<Locale, Currency>> = LOCALES.flatMap { (tag, code) ->
        (listOf(code) + others).distinct().map { Locale.forLanguageTag(tag) to Currency.getInstance(it) }
    }

    private fun assertParsesAsBefore(text: String, locale: Locale, currency: Currency) {
        assertEquals(
            "${locale.toLanguageTag()} ${currency.currencyCode} '$text'",
            BigDecimalMoney.parseToCents(text, locale, currency),
            Money.parseToCents(text, locale, currency)
        )
    }

    @Test
    fun plainAndInputTextMatchBigDecimal() {
        val random = Random(SEED)
        val values = AMOUNTS + listOf(Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE + 1) + (-1_000L..1_000L) +
            List(10_000) { random.nextLong() } + List(10_000) { random.nextLong(-10_000_000, 10_000_000) }
        values.forEach { cents ->
            assertEquals("$cents", BigDecimal.valueOf(cents, 2).toPlainString(), Money.toPlain(cents))
            assertEquals("$cents", BigDecimal.valueOf(cents, 2).stripTrailingZeros().toPlainString(), Money.toInput(cents))
        }
    }

    @Test
    fun everyCharacterNormalizesAsItDid() {
        for (code in 0..Char.MAX_VALUE.code) {
            val text = "1${code.toChar()}2"
            assertEquals("U+%04X".format(code), BigDecimalMoney.normalizeDigits(text), Money.normalizeDigits(text))
        }
    }

    /** Digits grouped well or badly, a decimal mark, a sign and the currency's marks, in the shapes people type. */
    private fun numberLike(random: Random, locale: Locale, currency: Currency): String = buildString {
        val digits = if (random.nextInt(4) == 0) NATIVE_DIGITS.random(random) else ASCII_DIGITS
        val group = GROUP_MARKS.random(random)
        if (random.nextInt(5) == 0) append(SIGNS.random(random))
        if (random.nextInt(3) ==
            0
        ) {
            append(listOf(currency.getSymbol(locale), currency.symbol, currency.currencyCode, "$", "€").random(random))
        }
        if (random.nextBoolean()) append(' ')
        if (random.nextInt(5) == 0) append(SIGNS.random(random))
        val whole = random.nextInt(0, 13)
        for (i in 0 until whole) {
            if (i > 0 && (whole - i) % 3 == 0 && random.nextInt(3) == 0) append(group)
            append(digits[random.nextInt(10)])
        }
        if (random.nextInt(3) > 0) {
            append(DECIMAL_MARKS.random(random))
            repeat(random.nextInt(0, 5)) { append(digits[random.nextInt(10)]) }
        }
        if (random.nextBoolean()) append(' ')
        if (random.nextInt(3) == 0) append(listOf(currency.getSymbol(locale), currency.currencyCode, "‏", "؜").random(random))
    }

    private fun noise(random: Random): String = String(CharArray(random.nextInt(0, 9)) { NOISE[random.nextInt(NOISE.length)] })

    /** Money's parsing as it was before it became plain Kotlin. */
    private object BigDecimalMoney {
        private const val SCALE = 2
        private const val GROUP_DIGITS = 3
        private val MAX_INPUT = BigDecimal("9999999999.99")
        private val DECIMAL_MARKS = setOf('.', ',')
        private const val ARABIC_DECIMAL_SEPARATOR = '٫'
        private const val ARABIC_THOUSANDS_SEPARATOR = '٬'
        private const val MINUS_SIGN = '−'
        private const val DECIMAL_RADIX = 10
        private val DIRECTION_MARKS = setOf('‎', '‏', '؜')
        private val GROUP_ONLY_MARKS = setOf(' ', ' ', ' ', '\'', '’')

        fun parseToCents(text: String, locale: Locale, currency: Currency): Long? {
            val (negative, body) = splitSign(normalizeDigits(text), locale, currency) ?: return null
            val localeDecimal = normalizeDigits(DecimalFormatSymbols.getInstance(locale).decimalSeparator.toString()).single()
            val value = parseUnsigned(body, localeDecimal) ?: return null
            if (value.stripTrailingZeros().scale() > SCALE || value > MAX_INPUT) return null
            val cents = value.movePointRight(SCALE).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
            return if (negative) -cents else cents
        }

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

        private fun splitSign(text: String, locale: Locale, currency: Currency): Pair<Boolean, String>? {
            var body = text.trim()
            val leadingMinus = body.startsWith('-')
            if (leadingMinus) body = body.drop(1)
            body = stripCurrency(body, locale, currency)
            val minusAfterSymbol = !leadingMinus && body.startsWith('-')
            if (minusAfterSymbol) body = body.drop(1)
            return if (body.isEmpty()) null else (leadingMinus || minusAfterSymbol) to body
        }

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
            if (body.any { it !in '0'..'9' && it !in DECIMAL_MARKS && it !in GROUP_ONLY_MARKS }) return null
            val decimalIndex = decimalMarkIndex(body, localeDecimal)
            val whole = ungroup(if (decimalIndex < 0) body else body.substring(0, decimalIndex)) ?: return null
            val fraction = if (decimalIndex < 0) "" else body.substring(decimalIndex + 1)
            val fractionOk = decimalIndex < 0 || (fraction.isNotEmpty() && fraction.all { it in '0'..'9' })
            return if (fractionOk && (whole + fraction).isNotEmpty()) {
                BigDecimal(whole.ifEmpty { "0" } + if (fraction.isEmpty()) "" else ".$fraction")
            } else {
                null
            }
        }

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

        private fun ungroup(whole: String): String? {
            val separators = whole.filterNot { it in '0'..'9' }.toSet()
            if (separators.isEmpty()) return whole
            val groups = whole.split(separators.first())
            val wellFormed = separators.size == 1 &&
                groups.first().length in 1..GROUP_DIGITS &&
                groups.drop(1).all { it.length == GROUP_DIGITS }
            return if (wellFormed) groups.joinToString("") else null
        }
    }

    private companion object {
        const val SEED = 20_261_122
        const val GENERATED_PER_CASE = 3_000

        val LOCALES = listOf(
            "en-US" to "USD", "en-IN" to "INR", "zh-CN" to "CNY", "hi-IN" to "INR", "es-ES" to "EUR", "es-MX" to "MXN",
            "ar-SA" to "SAR", "ar-EG" to "EGP", "ar-MA" to "MAD", "fr-FR" to "EUR", "fr-CA" to "CAD", "fr-CH" to "CHF",
            "de-DE" to "EUR", "de-CH" to "CHF", "pt-BR" to "BRL", "pt-PT" to "EUR"
        )
        val AMOUNTS = listOf(0L, 1L, 5L, 99L, 100L, 1_250L, 123_450L, 123_456_789L, 999_999_999_999L).flatMap { listOf(it, -it) }
        const val ASCII_DIGITS = "0123456789"
        val NATIVE_DIGITS = listOf("٠١٢٣٤٥٦٧٨٩", "۰۱۲۳۴۵۶۷۸۹", "०१२३४५६७८९", "０１２３４５６７８９")
        val GROUP_MARKS = listOf(",", ".", " ", " ", " ", "'", "’", "٬")
        val DECIMAL_MARKS = listOf(".", ",", "٫")
        val SIGNS = listOf("-", "−", "+", "‏-", "؜-")
        const val NOISE = "0123456789.,-+   '’−٫٬‏؜$€₹eE١٥१x"

        val FIXED_INPUTS = listOf(
            "", " ", "$", "-", "--5", "+5", "-0", "0", "00", "0.0", ".5", "5.", ",", "1e3", "12", "12.5", " 12.05 ", "$1,234.56",
            "-25.50", "-$5", "$-5", "12.000", "12.500", "12.505", "9999999999.99", "9999999999.995", "10000000000", "99999999999",
            "0009999999999.99", "1,2,3", "12,345,67", "12 34", "1..2", "1,234.5.6", "1 234,567.89", "1,234", "1.234", "12,50",
            "1.234,56", "1.234.567", "1 234,56", "1'234.50", "1’234.50", "١٢٫٥", "١٬٢٣٤٫٥٦", "۱۲.۵", "१२.५०", "‏−5",
            "12,50 €", "€12.50", "12.50 EUR", "R$ 1.234,56", "-₹5", "€", "US$ 5", "5 US$", "\$US 5", "CHF 5.50", "5.50 CHF"
        )
    }
}
