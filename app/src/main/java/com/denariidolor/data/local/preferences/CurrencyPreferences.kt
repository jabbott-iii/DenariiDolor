/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import android.content.Context
import com.denariidolor.util.Currencies
import com.denariidolor.util.Money
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The currency amounts are shown in. It is only a label: amounts are never converted. It is saved, so changing the device's
 * language or region later doesn't relabel existing amounts. Not sensitive, so it lives in the plain `ui_prefs` file, which is
 * excluded from backups like everything else.
 */
@Singleton
class CurrencyPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Installs from before this setting have nothing saved; they showed US dollars, so that is what they keep.
    private val _currency = MutableStateFlow(Currencies.fromCode(preferences.getString(KEY_CURRENCY, null)) ?: Currencies.DEFAULT)
    val currency: StateFlow<Currency> = _currency.asStateFlow()

    /** Makes [Money] format with the saved currency; called once at app start. */
    fun applySaved() {
        Money.currency = _currency.value
    }

    fun setCurrency(currency: Currency) {
        require(Currencies.isSupported(currency)) { "Unsupported currency ${currency.currencyCode}" }
        preferences.edit().putString(KEY_CURRENCY, currency.currencyCode).apply()
        _currency.value = currency
        Money.currency = currency
    }

    /** For a new security profile: the currency of the device's region, which the user can change in Settings. */
    fun resetToRegionDefault(locale: Locale = Locale.getDefault()) = setCurrency(Currencies.regionDefault(locale))

    private companion object {
        const val PREFS_NAME = "ui_prefs"
        const val KEY_CURRENCY = "currency"
    }
}
