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

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    private val germany = Locale.GERMANY
    private val swiss = Locale("de", "CH")

    private fun us(text: String) = Money.parseToCents(text, Locale.US)

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
        assertEquals(123_456L, Money.parseToCents("1 234,56", Locale.FRANCE))
        assertEquals(123_450L, Money.parseToCents("1 234,5", Locale.FRANCE))
        assertEquals(123_450L, Money.parseToCents("1 234,5", Locale.FRANCE))
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
        assertEquals("$0.05", formatMoney(5))
        assertEquals("-$1.00", formatMoney(-100))
    }

    @Test
    fun centsSumExactly() {
        val tenCents = us("0.10")!!
        assertEquals(us("0.30"), tenCents * 3)
    }
}
