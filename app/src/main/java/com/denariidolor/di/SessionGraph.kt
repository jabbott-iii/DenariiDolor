/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.di

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import kotlin.reflect.KClass

/** The signed-in app: what exists only while the vault is unlocked. */
abstract class SessionScope private constructor()

/**
 * Everything that reads the database (rule 4): the database and its DAOs, the repositories, the use cases, and the signed-in
 * app's screens. It extends [AppGraph], which can't see any of it, so the compiler refuses a singleton or anything sign-in
 * uses that asks for the database. MainActivity creates one after its session check; it goes away with the activity.
 */
@GraphExtension(SessionScope::class)
interface SessionGraph {
    val viewModelFactory: SessionViewModelFactory

    @GraphExtension.Factory
    interface Factory {
        fun createSessionGraph(): SessionGraph
    }
}

/**
 * Creates the signed-in app's screens. Like [AppViewModelFactory] it holds providers, not ViewModels, so each screen's
 * dependencies are resolved, and the database read, when the screen is opened.
 */
@SingleIn(SessionScope::class)
@Inject
class SessionViewModelFactory(
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    override val assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    override val manualAssistedFactoryProviders: Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>
) : MetroViewModelFactory()
