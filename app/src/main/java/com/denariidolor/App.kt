/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import android.app.Application
import android.content.Context
import com.denariidolor.data.local.preferences.CurrencyPreferences
import com.denariidolor.di.AppGraph
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.createGraphFactory

class App : Application() {
    /** The dependency graph. It is created first in [onCreate], before anything can ask for it. */
    lateinit var graph: AppGraph
        private set

    @Inject
    lateinit var currencyPreferences: CurrencyPreferences

    override fun onCreate() {
        super.onCreate()
        graph = createGraphFactory<AppGraph.Factory>().create(this)
        graph.inject(this)
        currencyPreferences.applySaved()
    }
}

/** The app's dependency graph, for the components Android creates itself. */
val Context.appGraph: AppGraph get() = (applicationContext as App).graph
