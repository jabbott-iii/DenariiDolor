/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.testutil

import com.denariidolor.data.local.preferences.OnboardingStore
import com.denariidolor.data.local.preferences.RatePromptCounters
import com.denariidolor.data.local.preferences.RatePromptPolicy
import com.denariidolor.data.local.preferences.RatePromptStore
import kotlinx.coroutines.flow.MutableStateFlow

class FakeOnboardingStore(tourPending: Boolean = false, checklistActive: Boolean = false) : OnboardingStore {
    override val tourPending = MutableStateFlow(tourPending)
    override val checklistActive = MutableStateFlow(checklistActive)

    override fun startOnboarding() {
        tourPending.value = true
        checklistActive.value = true
    }

    override fun finishTour() {
        tourPending.value = false
    }

    override fun dismissChecklist() {
        checklistActive.value = false
    }
}

/** Runs the real [RatePromptPolicy] over in-memory counters. */
class FakeRatePromptStore(var counters: RatePromptCounters = RatePromptCounters()) : RatePromptStore {
    override val promptVisible = MutableStateFlow(false)

    override fun recordSave() {
        val decision = RatePromptPolicy.afterSave(counters)
        counters = decision.counters
        if (decision.ask) promptVisible.value = true
    }

    override fun postpone() {
        promptVisible.value = false
    }

    override fun finish() {
        counters = counters.copy(finished = true)
        promptVisible.value = false
    }
}
