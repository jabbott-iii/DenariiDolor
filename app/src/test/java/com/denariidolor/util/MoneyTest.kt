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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parsesCommonInputsToCents() {
        assertEquals(1_200L, Money.parseToCents("12"))
        assertEquals(1_250L, Money.parseToCents("12.5"))
        assertEquals(1_205L, Money.parseToCents(" 12.05 "))
        assertEquals(123_456L, Money.parseToCents("$1,234.56"))
        assertEquals(-2_550L, Money.parseToCents("-25.50"))
        assertEquals(1_200L, Money.parseToCents("12.000"))
    }

    @Test
    fun rejectsInvalidInputs() {
        assertNull(Money.parseToCents(""))
        assertNull(Money.parseToCents("abc"))
        assertNull(Money.parseToCents("1.005"))
        assertNull(Money.parseToCents("1e3"))
        assertNull(Money.parseToCents("99999999999"))
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
        val tenCents = Money.parseToCents("0.10")!!
        assertEquals(Money.parseToCents("0.30"), tenCents * 3)
    }
}
