/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
