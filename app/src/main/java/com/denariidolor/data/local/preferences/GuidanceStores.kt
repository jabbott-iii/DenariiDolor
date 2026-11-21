/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import kotlinx.coroutines.flow.StateFlow

/** Whether the welcome tour and the Dashboard's getting-started checklist are due. Both start only for a new profile. */
interface OnboardingStore {
    val tourPending: StateFlow<Boolean>
    val checklistActive: StateFlow<Boolean>

    /** A new profile was set up, or the user asked to see the tour again: show the tour and the checklist. */
    fun startOnboarding()

    fun finishTour()

    fun dismissChecklist()
}

/** Asks whether the user would like to rate the app: once after a few saved transactions, and at most once more later. */
interface RatePromptStore {
    /** True while the prompt should be on screen. */
    val promptVisible: StateFlow<Boolean>

    /** Called after each new transaction is saved (not after edits); may make the prompt visible. */
    fun recordSave()

    /** "Not now": hides the prompt. It may come back once, after more saves. */
    fun postpone()

    /** "Rate now" or "Don't ask again", or the user rated from Settings: hides the prompt and never asks again. */
    fun finish()
}

data class RatePromptCounters(val saves: Int = 0, val timesAsked: Int = 0, val finished: Boolean = false)

data class RatePromptDecision(val counters: RatePromptCounters, val ask: Boolean)

object RatePromptPolicy {
    const val FIRST_ASK_AFTER_SAVES = 10
    const val SECOND_ASK_AFTER_SAVES = 30
    private const val MAX_ASKS = 2

    /** Counts one more saved transaction and decides whether to ask now. Counting stops once the user can't be asked again. */
    fun afterSave(counters: RatePromptCounters): RatePromptDecision {
        if (counters.finished || counters.timesAsked >= MAX_ASKS) return RatePromptDecision(counters, ask = false)
        val counted = counters.copy(saves = counters.saves + 1)
        val threshold = if (counted.timesAsked == 0) FIRST_ASK_AFTER_SAVES else SECOND_ASK_AFTER_SAVES
        return if (counted.saves >= threshold) {
            RatePromptDecision(counted.copy(timesAsked = counted.timesAsked + 1), ask = true)
        } else {
            RatePromptDecision(counted, ask = false)
        }
    }
}
