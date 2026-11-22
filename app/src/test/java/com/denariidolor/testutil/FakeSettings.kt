/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.testutil

import com.denariidolor.data.local.preferences.CurrencySettings
import com.denariidolor.data.local.preferences.ThemeSettings
import com.denariidolor.presentation.ui.auth.BiometricCapability
import com.denariidolor.util.Currencies
import java.util.Currency
import kotlinx.coroutines.flow.MutableStateFlow

/** Keeps the currency in memory and records each one saved. */
class FakeCurrencySettings(currency: Currency = Currencies.DEFAULT) : CurrencySettings {
    override val currency = MutableStateFlow(currency)
    val saved = mutableListOf<Currency>()
    var resets = 0

    override fun setCurrency(currency: Currency) {
        saved += currency
        this.currency.value = currency
    }

    override fun resetToRegionDefault() {
        resets++
    }
}

class FakeThemeSettings(darkModeOverride: Boolean? = null) : ThemeSettings {
    override val darkModeOverride = MutableStateFlow(darkModeOverride)

    override fun setDarkMode(enabled: Boolean) {
        darkModeOverride.value = enabled
    }
}

class FakeBiometricCapability(var available: Boolean = true) : BiometricCapability {
    override fun canAuthenticate(): Boolean = available
}
