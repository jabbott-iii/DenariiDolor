/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.di

import android.app.Application
import android.content.Context
import com.denariidolor.App
import com.denariidolor.MainActivity
import com.denariidolor.presentation.ui.auth.LoginActivity
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/**
 * The app's dependency graph, created once in [App.onCreate]: process-wide singletons and what sign-in needs. The database
 * and everything that reads it are only in [SessionGraph], which this graph can create but can't see into (rule 4).
 */
@DependencyGraph(AppScope::class)
interface AppGraph :
    ViewModelGraph,
    SessionGraph.Factory {
    // Android creates these itself, so the graph fills their @Inject fields instead of calling their constructors.
    fun inject(app: App)

    fun inject(activity: MainActivity)

    fun inject(activity: LoginActivity)

    @Provides
    @ApplicationContext
    fun provideApplicationContext(application: Application): Context = application

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AppGraph
    }
}
