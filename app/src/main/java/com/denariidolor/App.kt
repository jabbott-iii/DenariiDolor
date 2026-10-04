/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import android.app.Application
import com.denariidolor.data.local.preferences.CurrencyPreferences
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var currencyPreferences: CurrencyPreferences

    override fun onCreate() {
        super.onCreate()
        currencyPreferences.applySaved()
    }
}
