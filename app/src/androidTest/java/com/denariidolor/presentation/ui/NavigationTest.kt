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

package com.denariidolor.presentation.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun repeatedBackPopsOnlyOnce() {
        composeRule.setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    Button(onClick = { navController.navigate("child") }) { Text("Open child") }
                }
                composable("child") {
                    val back = navController.popBackOnce()
                    // What a double tap on Back, or two Saved events, does: two pops before the screen leaves.
                    Button(onClick = {
                        back()
                        back()
                    }) { Text("Back twice") }
                }
            }
        }

        composeRule.onNodeWithText("Open child").performClick()
        composeRule.onNodeWithText("Back twice").performClick()

        composeRule.onNodeWithText("Open child").assertIsDisplayed()
    }
}
