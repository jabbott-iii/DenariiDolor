/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import java.util.Currency
import kotlinx.coroutines.flow.StateFlow

/** The part of [CurrencyPreferences] that sign-in and Settings use; an interface, so their ViewModels can be tested. */
interface CurrencySettings {
    /** The saved currency amounts are shown in. */
    val currency: StateFlow<Currency>

    /** Saves a supported [currency] and shows amounts in it from now on; only the label changes, amounts aren't converted. */
    fun setCurrency(currency: Currency)

    /** For a new security profile: the currency of the device's region, which the user can change in Settings. */
    fun resetToRegionDefault()
}
