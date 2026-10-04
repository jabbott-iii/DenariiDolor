/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
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
