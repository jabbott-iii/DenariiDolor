/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.auth

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** CS-11 on API 26–30. On API 31+ overlays are hidden instead, which no public API reports; check that with `dumpsys window`. */
@RunWith(AndroidJUnit4::class)
class OverlayGuardTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    // SdkSuppress, not an assumption: AGP 9.4's test engine reports a failed assumption as a test failure.
    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.R)
    fun windowDropsTouchesThroughOverlaysBeforeApi31() {
        activityRule.scenario.onActivity { activity ->
            activity.window.guardAgainstOverlays()
            assertTrue(activity.window.decorView.filterTouchesWhenObscured)
        }
    }
}
