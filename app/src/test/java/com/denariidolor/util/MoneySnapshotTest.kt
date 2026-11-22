/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.report.ReportText
import java.util.Currency
import java.util.Locale
import kotlinx.datetime.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Money and month labels exactly as each of the app's languages shows them today, in the region currency and in US dollars.
 * The JVM's locale data, not a device's: Android's can differ (Indian grouping, for one), so these pin the app's code, and
 * an iOS formatter has to match them. Invisible characters are escaped: U+00A0 and U+202F are no-break spaces, U+200E and
 * U+200F direction marks and U+061C the Arabic letter mark.
 */
class MoneySnapshotTest {
    @Test
    fun moneyIsShownAsEachLanguageShowsIt() {
        MONEY.forEach { (key, expected) ->
            val (tag, code) = key.split(' ')
            val locale = Locale.forLanguageTag(tag)
            val currency = Currency.getInstance(code)
            val actual = AMOUNTS.map { formatMoney(it, currency, locale) } +
                formatMoney(PLUS_AMOUNT, currency, locale, showPlus = true) +
                TransactionType.entries.map { formatSignedAmount(it, SIGNED_AMOUNT, currency, locale) }

            assertEquals(key, expected, actual)
        }
    }

    @Test
    fun monthLabelsAreShownAsEachLanguageShowsThem() {
        PERIODS.forEach { (tag, expected) ->
            val locale = Locale.forLanguageTag(tag)

            assertEquals(tag, expected, listOf(YearMonth(2026, 5), YearMonth(2026, 9)).map { ReportText.periodLabel(it, locale) })
        }
    }

    private companion object {
        val AMOUNTS = listOf(5L, -100L, 123_450L, -123_456_789L)
        const val PLUS_AMOUNT = 1_200L

        /** [TransactionType.EXPENSE], [TransactionType.INCOME] and [TransactionType.TRANSFER], in that order. */
        const val SIGNED_AMOUNT = 120_000L

        /** The four [AMOUNTS], then [PLUS_AMOUNT] with a plus sign, then [SIGNED_AMOUNT] for each transaction type. */
        val MONEY = listOf(
            "en-US USD" to listOf(
                "\$0.05",
                "-\$1.00",
                "\$1,234.50",
                "-\$1,234,567.89",
                "+\$12.00",
                "-\$1,200.00",
                "+\$1,200.00",
                "\$1,200.00"
            ),
            "en-GB GBP" to listOf(
                "£0.05",
                "-£1.00",
                "£1,234.50",
                "-£1,234,567.89",
                "+£12.00",
                "-£1,200.00",
                "+£1,200.00",
                "£1,200.00"
            ),
            "en-GB USD" to listOf(
                "US\$0.05",
                "-US\$1.00",
                "US\$1,234.50",
                "-US\$1,234,567.89",
                "+US\$12.00",
                "-US\$1,200.00",
                "+US\$1,200.00",
                "US\$1,200.00"
            ),
            "en-IN INR" to listOf(
                "₹0.05",
                "-₹1.00",
                "₹1,234.50",
                "-₹1,234,567.89",
                "+₹12.00",
                "-₹1,200.00",
                "+₹1,200.00",
                "₹1,200.00"
            ),
            "en-IN USD" to listOf(
                "\$0.05",
                "-\$1.00",
                "\$1,234.50",
                "-\$1,234,567.89",
                "+\$12.00",
                "-\$1,200.00",
                "+\$1,200.00",
                "\$1,200.00"
            ),
            "zh-CN CNY" to listOf(
                "¥0.05",
                "-¥1.00",
                "¥1,234.50",
                "-¥1,234,567.89",
                "+¥12.00",
                "-¥1,200.00",
                "+¥1,200.00",
                "¥1,200.00"
            ),
            "zh-CN USD" to listOf(
                "US\$0.05",
                "-US\$1.00",
                "US\$1,234.50",
                "-US\$1,234,567.89",
                "+US\$12.00",
                "-US\$1,200.00",
                "+US\$1,200.00",
                "US\$1,200.00"
            ),
            "hi-IN INR" to listOf(
                "₹0.05",
                "-₹1.00",
                "₹1,234.50",
                "-₹1,234,567.89",
                "+₹12.00",
                "-₹1,200.00",
                "+₹1,200.00",
                "₹1,200.00"
            ),
            "hi-IN USD" to listOf(
                "\$0.05",
                "-\$1.00",
                "\$1,234.50",
                "-\$1,234,567.89",
                "+\$12.00",
                "-\$1,200.00",
                "+\$1,200.00",
                "\$1,200.00"
            ),
            "es-ES EUR" to listOf(
                "0,05\u00A0€",
                "-1,00\u00A0€",
                "1.234,50\u00A0€",
                "-1.234.567,89\u00A0€",
                "+12,00\u00A0€",
                "-1.200,00\u00A0€",
                "+1.200,00\u00A0€",
                "1.200,00\u00A0€"
            ),
            "es-ES USD" to listOf(
                "0,05\u00A0US\$",
                "-1,00\u00A0US\$",
                "1.234,50\u00A0US\$",
                "-1.234.567,89\u00A0US\$",
                "+12,00\u00A0US\$",
                "-1.200,00\u00A0US\$",
                "+1.200,00\u00A0US\$",
                "1.200,00\u00A0US\$"
            ),
            "es-MX MXN" to listOf(
                "\$0.05",
                "-\$1.00",
                "\$1,234.50",
                "-\$1,234,567.89",
                "+\$12.00",
                "-\$1,200.00",
                "+\$1,200.00",
                "\$1,200.00"
            ),
            "es-MX USD" to listOf(
                "USD0.05",
                "-USD1.00",
                "USD1,234.50",
                "-USD1,234,567.89",
                "+USD12.00",
                "-USD1,200.00",
                "+USD1,200.00",
                "USD1,200.00"
            ),
            "ar-SA SAR" to listOf(
                "٠٫٠٥\u00A0ر.س.\u200F",
                "\u061C-١٫٠٠\u00A0ر.س.\u200F",
                "١٬٢٣٤٫٥٠\u00A0ر.س.\u200F",
                "\u061C-١٬٢٣٤٬٥٦٧٫٨٩\u00A0ر.س.\u200F",
                "+١٢٫٠٠\u00A0ر.س.\u200F",
                "\u061C-١٬٢٠٠٫٠٠\u00A0ر.س.\u200F",
                "+١٬٢٠٠٫٠٠\u00A0ر.س.\u200F",
                "١٬٢٠٠٫٠٠\u00A0ر.س.\u200F"
            ),
            "ar-SA USD" to listOf(
                "٠٫٠٥\u00A0US\$",
                "\u061C-١٫٠٠\u00A0US\$",
                "١٬٢٣٤٫٥٠\u00A0US\$",
                "\u061C-١٬٢٣٤٬٥٦٧٫٨٩\u00A0US\$",
                "+١٢٫٠٠\u00A0US\$",
                "\u061C-١٬٢٠٠٫٠٠\u00A0US\$",
                "+١٬٢٠٠٫٠٠\u00A0US\$",
                "١٬٢٠٠٫٠٠\u00A0US\$"
            ),
            "ar-EG EGP" to listOf(
                "٠٫٠٥\u00A0ج.م.\u200F",
                "\u061C-١٫٠٠\u00A0ج.م.\u200F",
                "١٬٢٣٤٫٥٠\u00A0ج.م.\u200F",
                "\u061C-١٬٢٣٤٬٥٦٧٫٨٩\u00A0ج.م.\u200F",
                "+١٢٫٠٠\u00A0ج.م.\u200F",
                "\u061C-١٬٢٠٠٫٠٠\u00A0ج.م.\u200F",
                "+١٬٢٠٠٫٠٠\u00A0ج.م.\u200F",
                "١٬٢٠٠٫٠٠\u00A0ج.م.\u200F"
            ),
            "ar-EG USD" to listOf(
                "٠٫٠٥\u00A0US\$",
                "\u061C-١٫٠٠\u00A0US\$",
                "١٬٢٣٤٫٥٠\u00A0US\$",
                "\u061C-١٬٢٣٤٬٥٦٧٫٨٩\u00A0US\$",
                "+١٢٫٠٠\u00A0US\$",
                "\u061C-١٬٢٠٠٫٠٠\u00A0US\$",
                "+١٬٢٠٠٫٠٠\u00A0US\$",
                "١٬٢٠٠٫٠٠\u00A0US\$"
            ),
            "ar-MA MAD" to listOf(
                "د.م.\u200F\u00A00,05",
                "\u200E-د.م.\u200F\u00A01,00",
                "د.م.\u200F\u00A01.234,50",
                "\u200E-د.م.\u200F\u00A01.234.567,89",
                "+د.م.\u200F\u00A012,00",
                "\u200E-د.م.\u200F\u00A01.200,00",
                "+د.م.\u200F\u00A01.200,00",
                "د.م.\u200F\u00A01.200,00"
            ),
            "ar-MA USD" to listOf(
                "US\$\u00A00,05",
                "\u200E-US\$\u00A01,00",
                "US\$\u00A01.234,50",
                "\u200E-US\$\u00A01.234.567,89",
                "+US\$\u00A012,00",
                "\u200E-US\$\u00A01.200,00",
                "+US\$\u00A01.200,00",
                "US\$\u00A01.200,00"
            ),
            "fr-FR EUR" to listOf(
                "0,05\u00A0€",
                "-1,00\u00A0€",
                "1\u202F234,50\u00A0€",
                "-1\u202F234\u202F567,89\u00A0€",
                "+12,00\u00A0€",
                "-1\u202F200,00\u00A0€",
                "+1\u202F200,00\u00A0€",
                "1\u202F200,00\u00A0€"
            ),
            "fr-FR USD" to listOf(
                "0,05\u00A0\$US",
                "-1,00\u00A0\$US",
                "1\u202F234,50\u00A0\$US",
                "-1\u202F234\u202F567,89\u00A0\$US",
                "+12,00\u00A0\$US",
                "-1\u202F200,00\u00A0\$US",
                "+1\u202F200,00\u00A0\$US",
                "1\u202F200,00\u00A0\$US"
            ),
            "fr-CA CAD" to listOf(
                "0,05\u00A0\$\u00A0CA",
                "-1,00\u00A0\$\u00A0CA",
                "1\u00A0234,50\u00A0\$\u00A0CA",
                "-1\u00A0234\u00A0567,89\u00A0\$\u00A0CA",
                "+12,00\u00A0\$\u00A0CA",
                "-1\u00A0200,00\u00A0\$\u00A0CA",
                "+1\u00A0200,00\u00A0\$\u00A0CA",
                "1\u00A0200,00\u00A0\$\u00A0CA"
            ),
            "fr-CA USD" to listOf(
                "0,05\u00A0\$\u00A0US",
                "-1,00\u00A0\$\u00A0US",
                "1\u00A0234,50\u00A0\$\u00A0US",
                "-1\u00A0234\u00A0567,89\u00A0\$\u00A0US",
                "+12,00\u00A0\$\u00A0US",
                "-1\u00A0200,00\u00A0\$\u00A0US",
                "+1\u00A0200,00\u00A0\$\u00A0US",
                "1\u00A0200,00\u00A0\$\u00A0US"
            ),
            "fr-CH CHF" to listOf(
                "0.05\u00A0CHF",
                "-1.00\u00A0CHF",
                "1\u202F234.50\u00A0CHF",
                "-1\u202F234\u202F567.89\u00A0CHF",
                "+12.00\u00A0CHF",
                "-1\u202F200.00\u00A0CHF",
                "+1\u202F200.00\u00A0CHF",
                "1\u202F200.00\u00A0CHF"
            ),
            "fr-CH USD" to listOf(
                "0.05\u00A0\$US",
                "-1.00\u00A0\$US",
                "1\u202F234.50\u00A0\$US",
                "-1\u202F234\u202F567.89\u00A0\$US",
                "+12.00\u00A0\$US",
                "-1\u202F200.00\u00A0\$US",
                "+1\u202F200.00\u00A0\$US",
                "1\u202F200.00\u00A0\$US"
            ),
            "pt-BR BRL" to listOf(
                "R\$\u00A00,05",
                "-R\$\u00A01,00",
                "R\$\u00A01.234,50",
                "-R\$\u00A01.234.567,89",
                "+R\$\u00A012,00",
                "-R\$\u00A01.200,00",
                "+R\$\u00A01.200,00",
                "R\$\u00A01.200,00"
            ),
            "pt-BR USD" to listOf(
                "US\$\u00A00,05",
                "-US\$\u00A01,00",
                "US\$\u00A01.234,50",
                "-US\$\u00A01.234.567,89",
                "+US\$\u00A012,00",
                "-US\$\u00A01.200,00",
                "+US\$\u00A01.200,00",
                "US\$\u00A01.200,00"
            ),
            "pt-PT EUR" to listOf(
                "0,05\u00A0€",
                "-1,00\u00A0€",
                "1\u00A0234,50\u00A0€",
                "-1\u00A0234\u00A0567,89\u00A0€",
                "+12,00\u00A0€",
                "-1\u00A0200,00\u00A0€",
                "+1\u00A0200,00\u00A0€",
                "1\u00A0200,00\u00A0€"
            ),
            "pt-PT USD" to listOf(
                "0,05\u00A0US\$",
                "-1,00\u00A0US\$",
                "1\u00A0234,50\u00A0US\$",
                "-1\u00A0234\u00A0567,89\u00A0US\$",
                "+12,00\u00A0US\$",
                "-1\u00A0200,00\u00A0US\$",
                "+1\u00A0200,00\u00A0US\$",
                "1\u00A0200,00\u00A0US\$"
            )
        )

        /** May and September 2026. */
        val PERIODS = listOf(
            "en-US" to listOf("May 2026", "September 2026"),
            "en-GB" to listOf("May 2026", "September 2026"),
            "en-IN" to listOf("May 2026", "September 2026"),
            "zh-CN" to listOf("五月 2026", "九月 2026"),
            "hi-IN" to listOf("मई 2026", "सितंबर 2026"),
            "es-ES" to listOf("mayo 2026", "septiembre 2026"),
            "es-MX" to listOf("mayo 2026", "septiembre 2026"),
            "ar-SA" to listOf("مايو 2026", "سبتمبر 2026"),
            "ar-EG" to listOf("مايو 2026", "سبتمبر 2026"),
            "ar-MA" to listOf("ماي 2026", "شتنبر 2026"),
            "fr-FR" to listOf("mai 2026", "septembre 2026"),
            "fr-CA" to listOf("mai 2026", "septembre 2026"),
            "fr-CH" to listOf("mai 2026", "septembre 2026"),
            "pt-BR" to listOf("maio 2026", "setembro 2026"),
            "pt-PT" to listOf("maio 2026", "setembro 2026")
        )
    }
}
