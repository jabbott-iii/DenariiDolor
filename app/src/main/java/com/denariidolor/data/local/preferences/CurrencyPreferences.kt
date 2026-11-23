/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import android.content.Context
import com.denariidolor.di.ApplicationContext
import com.denariidolor.util.Currencies
import com.denariidolor.util.Money
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The currency amounts are shown in. It is only a label: amounts are never converted. It is saved, so changing the device's
 * language or region later doesn't relabel existing amounts. Not sensitive, so it lives in the plain `ui_prefs` file, which is
 * excluded from backups like everything else.
 */
@SingleIn(AppScope::class)
@Inject
class CurrencyPreferences(@ApplicationContext context: Context) : CurrencySettings {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Installs from before this setting have nothing saved; they showed US dollars, so that is what they keep.
    private val _currency = MutableStateFlow(Currencies.fromCode(preferences.getString(KEY_CURRENCY, null)) ?: Currencies.DEFAULT)
    override val currency: StateFlow<Currency> = _currency.asStateFlow()

    /** Makes [Money] format with the saved currency; called once at app start. */
    fun applySaved() {
        Money.currency = _currency.value
    }

    override fun setCurrency(currency: Currency) {
        require(Currencies.isSupported(currency)) { "Unsupported currency ${currency.currencyCode}" }
        preferences.edit().putString(KEY_CURRENCY, currency.currencyCode).apply()
        _currency.value = currency
        Money.currency = currency
    }

    override fun resetToRegionDefault() = setCurrency(Currencies.regionDefault(Locale.getDefault()))

    private companion object {
        const val PREFS_NAME = "ui_prefs"
        const val KEY_CURRENCY = "currency"
    }
}
