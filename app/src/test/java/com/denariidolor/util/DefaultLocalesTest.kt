/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.report.ReportCsvFormatter
import com.denariidolor.domain.report.ReportText
import java.util.Currency
import java.util.Locale
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Which default locale each helper falls back to when the caller passes none. A device can set its number format apart from
 * its language: amounts follow the format locale, while transfer arrows and month names follow the language.
 */
class DefaultLocalesTest {
    private lateinit var savedDefault: Locale
    private lateinit var savedFormat: Locale

    @Before
    fun setLocales() {
        savedDefault = Locale.getDefault()
        savedFormat = Locale.getDefault(Locale.Category.FORMAT)
        // Locale.setDefault(Locale) sets every category, so the format locale goes second.
        Locale.setDefault(Locale.forLanguageTag("ar-SA"))
        Locale.setDefault(Locale.Category.FORMAT, Locale.FRANCE)
    }

    @After
    fun restoreLocales() {
        Locale.setDefault(savedDefault)
        Locale.setDefault(Locale.Category.FORMAT, savedFormat)
    }

    @Test
    fun amountsFollowTheFormatLocale() {
        val euro = Currency.getInstance("EUR")

        assertEquals(formatMoney(123_450, euro, Locale.FRANCE), formatMoney(123_450, euro))
        assertEquals(formatMoney(-500, euro, Locale.FRANCE), formatSignedAmount(TransactionType.EXPENSE, 500, euro))
        // In French `,` is the decimal mark, so three digits after it are too many decimals; in English it groups thousands.
        assertNull(Money.parseToCents("1,234", currency = euro))
        assertEquals(123_400L, Money.parseToCents("1,234", Locale.US, euro))
    }

    @Test
    fun transferArrowsAndMonthNamesFollowTheLanguage() {
        val period = YearMonth(2026, 9)

        assertEquals("Cash ← Savings", transferRoute("Cash", "Savings"))
        assertEquals(ReportText.periodLabel(period, Locale.forLanguageTag("ar-SA")), ReportText.periodLabel(period))
        val report = MonthlyReport("Title", period, 0L, TimeZone.UTC, 0, 0, 0, emptyList())
        val periodRow = ReportCsvFormatter.format(report).split("\r\n")[1]
        assertEquals(ReportCsvFormatter.format(report, Locale.forLanguageTag("ar-SA")).split("\r\n")[1], periodRow)
    }
}
