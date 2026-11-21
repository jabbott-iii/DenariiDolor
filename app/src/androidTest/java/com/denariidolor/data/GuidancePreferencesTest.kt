/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.denariidolor.data.local.preferences.GuidancePreferences
import com.denariidolor.data.local.preferences.RatePromptPolicy
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a test-only prefs file, never the app's own `ui_prefs`. */
@RunWith(AndroidJUnit4::class)
class GuidancePreferencesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun preferences() = GuidancePreferences(context, PREFS_NAME)

    // commit(), not apply(): it waits for earlier writes, so no test sees another test's state.
    @Before
    @After
    fun clearPrefs() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun existingInstallsSeeNoTourOrChecklist() {
        val preferences = preferences()

        assertFalse(preferences.tourPending.value)
        assertFalse(preferences.checklistActive.value)
    }

    @Test
    fun onboardingStateSurvivesARestart() {
        preferences().apply {
            startOnboarding()
            finishTour()
        }

        val reopened = preferences()

        assertFalse(reopened.tourPending.value)
        assertTrue(reopened.checklistActive.value)
    }

    @Test
    fun ratePromptCountsSavesAcrossRestartsAndStopsWhenFinished() {
        repeat(RatePromptPolicy.FIRST_ASK_AFTER_SAVES - 1) { preferences().recordSave() }
        val reopened = preferences()

        reopened.recordSave()
        assertTrue(reopened.promptVisible.value)
        reopened.finish()
        assertFalse(reopened.promptVisible.value)

        val afterFinish = preferences()
        repeat(RatePromptPolicy.SECOND_ASK_AFTER_SAVES) { afterFinish.recordSave() }
        assertFalse(afterFinish.promptVisible.value)
    }

    private companion object {
        const val PREFS_NAME = "guidance_prefs_test"
    }
}
