/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
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
