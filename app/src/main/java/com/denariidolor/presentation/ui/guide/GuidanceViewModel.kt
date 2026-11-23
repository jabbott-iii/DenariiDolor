/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.guide

import androidx.lifecycle.ViewModel
import com.denariidolor.data.local.preferences.OnboardingStore
import com.denariidolor.data.local.preferences.RatePromptStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.StateFlow

/** The welcome tour and the rate prompt, both shown above every screen of `MainActivity`. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class GuidanceViewModel(private val onboarding: OnboardingStore, private val ratePrompt: RatePromptStore) : ViewModel() {
    val tourPending: StateFlow<Boolean> = onboarding.tourPending
    val ratePromptVisible: StateFlow<Boolean> = ratePrompt.promptVisible

    fun finishTour() = onboarding.finishTour()

    fun showTourAgain() = onboarding.startOnboarding()

    fun postponeRating() = ratePrompt.postpone()

    /** "Rate now", "Don't ask again", or Rate this app in Settings: the prompt never shows again. */
    fun stopAskingToRate() = ratePrompt.finish()
}
