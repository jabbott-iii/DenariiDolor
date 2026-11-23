/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import com.denariidolor.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoneyTest {
    private val germany = localeOf("de-DE")
    private val swiss = localeOf("de-CH")

    private val usd = Currencies.DEFAULT
    private val euro = currencyOf("EUR")
    private val rupee = currencyOf("INR")
    private val india = localeOf("hi-IN")
    private val saudiArabia = localeOf("ar-SA")

    private fun us(text: String) = Money.parseToCents(text, localeOf("en-US"), usd)

    @Test
    fun parsesCommonInputsToCents() {
        assertEquals(1_200L, us("12"))
        assertEquals(1_250L, us("12.5"))
        assertEquals(1_205L, us(" 12.05 "))
        assertEquals(123_456L, us("$1,234.56"))
        assertEquals(-2_550L, us("-25.50"))
        assertEquals(-500L, us("-$5"))
        assertEquals(-500L, us("$-5"))
        assertEquals(1_200L, us("12.000"))
        assertEquals(50L, us(".5"))
        assertEquals(999_999_999_999L, us("9999999999.99"))
    }

    @Test
    fun acceptsCommaAsDecimalMarkInAnyLocale() {
        assertEquals(1_250L, us("12,50"))
        assertEquals(1_250L, us("12,5"))
        assertEquals(1_250L, Money.parseToCents("12,50", germany))
        assertEquals(1_250L, Money.parseToCents("12.50", germany))
        assertEquals(123_456L, us("1.234,56"))
        assertEquals(123_456L, Money.parseToCents("1.234,56", germany))
    }

    @Test
    fun localeSettlesOneMarkFollowedByThreeDigits() {
        assertEquals(123_400L, us("1,234"))
        assertNull(us("1.234"))
        assertEquals(123_400L, Money.parseToCents("1.234", germany))
        assertNull(Money.parseToCents("1,234", germany))
        assertEquals(1_200_000L, Money.parseToCents("12.000", germany))
    }

    @Test
    fun acceptsWellFormedThousandsGrouping() {
        assertEquals(123_456_789L, us("1,234,567.89"))
        assertEquals(123_456_700L, Money.parseToCents("1.234.567", germany))
        assertEquals(123_456L, Money.parseToCents("1 234,56", localeOf("fr-FR")))
        assertEquals(123_450L, Money.parseToCents("1 234,5", localeOf("fr-FR")))
        assertEquals(123_450L, Money.parseToCents("1 234,5", localeOf("fr-FR")))
        assertEquals(123_450L, Money.parseToCents("1'234.50", swiss))
        assertEquals(123_450L, Money.parseToCents("1’234.50", swiss))
    }

    @Test
    fun rejectsInvalidInputs() {
        assertNull(us(""))
        assertNull(us("$"))
        assertNull(us("abc"))
        assertNull(us("1.005"))
        assertNull(us("1e3"))
        assertNull(us("99999999999"))
        assertNull(us("1,2,3"))
        assertNull(us("12,345,67"))
        assertNull(us("12 34"))
        assertNull(us("1..2"))
        assertNull(us("5."))
        assertNull(us(","))
        assertNull(us("--5"))
        assertNull(us("+5"))
        assertNull(us("1,234.5.6"))
        assertNull(us("1 234,567.89"))
    }

    @Test
    fun formatsCents() {
        assertEquals("12.50", Money.toPlain(1_250))
        assertEquals("4.5", Money.toInput(450))
        assertEquals("250", Money.toInput(25_000))
        assertEquals("$0.05", formatMoney(5, usd, localeOf("en-US")))
        assertEquals("-$1.00", formatMoney(-100, usd, localeOf("en-US")))
        assertEquals("$1,234.50", formatMoney(123_450, usd, localeOf("en-US")))
        assertEquals("+$12.00", formatMoney(1_200, usd, localeOf("en-US"), showPlus = true))
    }

    @Test
    fun formatsInTheSavedCurrencyTheLocaleWay() {
        // French puts the symbol last and groups with a narrow no-break space; compare with plain spaces.
        assertEquals("1 234,50 €", formatMoney(123_450, euro, localeOf("fr-FR")).replace('\u202F', ' ').replace('\u00A0', ' '))
        assertEquals("€1,234.50", formatMoney(123_450, euro, localeOf("en-US")))
        assertTrue(formatMoney(1_250, rupee, india).contains("₹"))
        assertEquals("-$4.50", formatSignedAmount(TransactionType.EXPENSE, 450, usd, localeOf("en-US")))
        assertEquals("+$1,200.00", formatSignedAmount(TransactionType.INCOME, 120_000, usd, localeOf("en-US")))
        assertEquals("$80.00", formatSignedAmount(TransactionType.TRANSFER, 8_000, usd, localeOf("en-US")))
    }

    @Test
    fun acceptsTheCurrencySymbolOrCodeOnEitherSide() {
        assertEquals(1_250L, Money.parseToCents("12,50 €", localeOf("fr-FR"), euro))
        assertEquals(1_250L, Money.parseToCents("€12.50", localeOf("en-US"), euro))
        assertEquals(1_250L, Money.parseToCents("12.50 EUR", localeOf("en-US"), euro))
        assertEquals(123_456L, Money.parseToCents("R$ 1.234,56", localeOf("pt-BR"), currencyOf("BRL")))
        assertEquals(-500L, Money.parseToCents("-₹5", india, rupee))
        assertNull(Money.parseToCents("€", localeOf("en-US"), euro))
    }

    @Test
    fun acceptsArabicIndicAndDevanagariDigits() {
        assertEquals("12.50", Money.normalizeDigits("١٢٫٥٠"))
        assertEquals("-5", Money.normalizeDigits("\u200F\u22125"))
        assertEquals(1_250L, Money.parseToCents("١٢٫٥", saudiArabia, usd))
        assertEquals(123_456L, Money.parseToCents("١٬٢٣٤٫٥٦", saudiArabia, usd))
        assertEquals(1_250L, Money.parseToCents("۱۲.۵", saudiArabia, usd))
        assertEquals(1_250L, Money.parseToCents("१२.५०", india, usd))
        // What the app displays in Arabic parses back, direction marks and all.
        val riyal = currencyOf("SAR")
        assertEquals(1_250L, Money.parseToCents(formatMoney(1_250, riyal, saudiArabia), saudiArabia, riyal))
    }

    @Test
    fun centsSumExactly() {
        val tenCents = us("0.10")!!
        assertEquals(us("0.30"), tenCents * 3)
    }
}
