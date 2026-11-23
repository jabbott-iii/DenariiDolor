/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CurrenciesTest {
    @Test
    fun regionDefaultFollowsTheRegion() {
        assertEquals(currencyOf("INR"), Currencies.regionDefault(localeOf("hi-IN")))
        assertEquals(currencyOf("CNY"), Currencies.regionDefault(localeOf("zh-CN")))
        assertEquals(currencyOf("MXN"), Currencies.regionDefault(localeOf("es-MX")))
        assertEquals(currencyOf("EUR"), Currencies.regionDefault(localeOf("fr-FR")))
        assertEquals(currencyOf("BRL"), Currencies.regionDefault(localeOf("pt-BR")))
        assertEquals(currencyOf("SAR"), Currencies.regionDefault(localeOf("ar-SA")))
    }

    @Test
    fun regionDefaultFallsBackToUsDollarsWhenCentsDontFit() {
        assertEquals(Currencies.DEFAULT, Currencies.regionDefault(localeOf("ja-JP"))) // yen: no decimals
        assertEquals(Currencies.DEFAULT, Currencies.regionDefault(localeOf("ar-KW"))) // Kuwaiti dinar: 3 decimals
        assertEquals(Currencies.DEFAULT, Currencies.regionDefault(localeOf("ar"))) // no region
    }

    @Test
    fun fromCodeAcceptsOnlySupportedCurrencies() {
        assertEquals(currencyOf("EUR"), Currencies.fromCode("EUR"))
        assertNull(Currencies.fromCode("JPY"))
        assertNull(Currencies.fromCode("nope"))
        assertNull(Currencies.fromCode(null))
    }

    @Test
    fun codeIsTheIsoCodeThatFromCodeReads() {
        assertEquals("USD", Currencies.code(Currencies.DEFAULT))
        assertEquals("EUR", Currencies.code(currencyOf("EUR")))
        assertEquals(currencyOf("BRL"), Currencies.fromCode(Currencies.code(currencyOf("BRL"))))
    }

    @Test
    fun optionsIncludeTheCurrentCurrencyOnce() {
        val peso = currencyOf("UYU") // Uruguayan peso: supported, but not one of the common choices
        val options = Currencies.options(peso)
        assertTrue(peso in options)
        assertEquals(options.size, options.toSet().size)
        assertTrue(options.all(Currencies::isSupported))
        assertEquals(Currencies.options(Currencies.DEFAULT).size + 1, options.size)
    }
}
