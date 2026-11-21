/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.viewmodel

import com.denariidolor.data.local.preferences.RatePromptCounters
import com.denariidolor.data.local.preferences.RatePromptPolicy
import com.denariidolor.presentation.ui.guide.GuidanceViewModel
import com.denariidolor.testutil.FakeOnboardingStore
import com.denariidolor.testutil.FakeRatePromptStore
import com.denariidolor.testutil.MainDispatcherRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GuidanceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val onboarding = FakeOnboardingStore(tourPending = true, checklistActive = true)

    // One save short of the first prompt.
    private val ratePrompt = FakeRatePromptStore(RatePromptCounters(saves = RatePromptPolicy.FIRST_ASK_AFTER_SAVES - 1))
    private val viewModel = GuidanceViewModel(onboarding, ratePrompt)

    @Test
    fun finishingTheTourKeepsTheChecklist() {
        viewModel.finishTour()

        assertFalse(viewModel.tourPending.value)
        assertTrue(onboarding.checklistActive.value)
    }

    @Test
    fun showTourAgainRestartsTourAndChecklist() {
        viewModel.finishTour()
        onboarding.dismissChecklist()

        viewModel.showTourAgain()

        assertTrue(viewModel.tourPending.value)
        assertTrue(onboarding.checklistActive.value)
    }

    @Test
    fun notNowHidesThePromptButAllowsOneMoreAsk() {
        ratePrompt.recordSave()
        assertTrue(viewModel.ratePromptVisible.value)

        viewModel.postponeRating()

        assertFalse(viewModel.ratePromptVisible.value)
        assertFalse(ratePrompt.counters.finished)
    }

    @Test
    fun stopAskingHidesThePromptForGood() {
        ratePrompt.recordSave()

        viewModel.stopAskingToRate()
        repeat(RatePromptPolicy.SECOND_ASK_AFTER_SAVES) { ratePrompt.recordSave() }

        assertFalse(viewModel.ratePromptVisible.value)
        assertTrue(ratePrompt.counters.finished)
    }
}
