/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

/** The part of [CurrencyPreferences] that sign-in uses; an interface, so the sign-in ViewModel can be tested. */
interface CurrencySettings {
    /** For a new security profile: the currency of the device's region, which the user can change in Settings. */
    fun resetToRegionDefault()
}
