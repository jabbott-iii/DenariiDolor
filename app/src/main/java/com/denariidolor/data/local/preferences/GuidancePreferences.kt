/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Onboarding and rate-prompt state. It says nothing about the user's finances (only how many transactions were saved, for
 * the rate prompt), so it lives in the plain app-private `ui_prefs` file next to the theme and currency, excluded from backups.
 * An install from before these features has no keys, so its existing users see neither the tour nor the checklist.
 */
@Singleton
class GuidancePreferences internal constructor(context: Context, prefsName: String) :
    OnboardingStore,
    RatePromptStore {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, PREFS_NAME)

    private val preferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private val _tourPending = MutableStateFlow(preferences.getBoolean(KEY_TOUR_PENDING, false))
    override val tourPending: StateFlow<Boolean> = _tourPending.asStateFlow()

    private val _checklistActive = MutableStateFlow(preferences.getBoolean(KEY_CHECKLIST_ACTIVE, false))
    override val checklistActive: StateFlow<Boolean> = _checklistActive.asStateFlow()

    private val _promptVisible = MutableStateFlow(false)
    override val promptVisible: StateFlow<Boolean> = _promptVisible.asStateFlow()

    override fun startOnboarding() {
        preferences.edit {
            putBoolean(KEY_TOUR_PENDING, true)
            putBoolean(KEY_CHECKLIST_ACTIVE, true)
        }
        _tourPending.value = true
        _checklistActive.value = true
    }

    override fun finishTour() {
        preferences.edit { putBoolean(KEY_TOUR_PENDING, false) }
        _tourPending.value = false
    }

    override fun dismissChecklist() {
        preferences.edit { putBoolean(KEY_CHECKLIST_ACTIVE, false) }
        _checklistActive.value = false
    }

    @Synchronized
    override fun recordSave() {
        val decision = RatePromptPolicy.afterSave(readCounters())
        writeCounters(decision.counters)
        if (decision.ask) _promptVisible.value = true
    }

    override fun postpone() {
        _promptVisible.value = false
    }

    @Synchronized
    override fun finish() {
        writeCounters(readCounters().copy(finished = true))
        _promptVisible.value = false
    }

    private fun readCounters() = RatePromptCounters(
        saves = preferences.getInt(KEY_RATE_SAVES, 0),
        timesAsked = preferences.getInt(KEY_RATE_TIMES_ASKED, 0),
        finished = preferences.getBoolean(KEY_RATE_FINISHED, false)
    )

    private fun writeCounters(counters: RatePromptCounters) {
        preferences.edit {
            putInt(KEY_RATE_SAVES, counters.saves)
            putInt(KEY_RATE_TIMES_ASKED, counters.timesAsked)
            putBoolean(KEY_RATE_FINISHED, counters.finished)
        }
    }

    private companion object {
        const val PREFS_NAME = "ui_prefs"
        const val KEY_TOUR_PENDING = "tour_pending"
        const val KEY_CHECKLIST_ACTIVE = "checklist_active"
        const val KEY_RATE_SAVES = "rate_prompt_saves"
        const val KEY_RATE_TIMES_ASKED = "rate_prompt_times_asked"
        const val KEY_RATE_FINISHED = "rate_prompt_finished"
    }
}
