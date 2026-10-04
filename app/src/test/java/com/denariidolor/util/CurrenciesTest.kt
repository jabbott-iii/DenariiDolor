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

import java.util.Currency
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrenciesTest {
    @Test
    fun regionDefaultFollowsTheRegion() {
        assertEquals("INR", Currencies.regionDefault(Locale("hi", "IN")).currencyCode)
        assertEquals("CNY", Currencies.regionDefault(Locale.CHINA).currencyCode)
        assertEquals("MXN", Currencies.regionDefault(Locale("es", "MX")).currencyCode)
        assertEquals("EUR", Currencies.regionDefault(Locale.FRANCE).currencyCode)
        assertEquals("BRL", Currencies.regionDefault(Locale("pt", "BR")).currencyCode)
        assertEquals("SAR", Currencies.regionDefault(Locale("ar", "SA")).currencyCode)
    }

    @Test
    fun regionDefaultFallsBackToUsDollarsWhenCentsDontFit() {
        assertEquals(Currencies.DEFAULT, Currencies.regionDefault(Locale.JAPAN)) // yen: no decimals
        assertEquals(Currencies.DEFAULT, Currencies.regionDefault(Locale("ar", "KW"))) // Kuwaiti dinar: 3 decimals
        assertEquals(Currencies.DEFAULT, Currencies.regionDefault(Locale("ar"))) // no region
    }

    @Test
    fun fromCodeAcceptsOnlySupportedCurrencies() {
        assertEquals("EUR", Currencies.fromCode("EUR")?.currencyCode)
        assertNull(Currencies.fromCode("JPY"))
        assertNull(Currencies.fromCode("nope"))
        assertNull(Currencies.fromCode(null))
    }

    @Test
    fun optionsIncludeTheCurrentCurrencyOnce() {
        val peso = Currency.getInstance("UYU") // Uruguayan peso: supported, but not one of the common choices
        val options = Currencies.options(peso)
        assertTrue(peso in options)
        assertEquals(options.size, options.toSet().size)
        assertTrue(options.all(Currencies::isSupported))
        assertEquals(Currencies.options(Currencies.DEFAULT).size + 1, options.size)
    }
}
