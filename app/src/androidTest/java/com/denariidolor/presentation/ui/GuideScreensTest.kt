/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.presentation.ui.dashboard.DashboardScreen
import com.denariidolor.presentation.ui.dashboard.DashboardUiState
import com.denariidolor.presentation.ui.dashboard.GETTING_STARTED_CARD_TAG
import com.denariidolor.presentation.ui.dashboard.GETTING_STARTED_HIDE_TAG
import com.denariidolor.presentation.ui.dashboard.GETTING_STARTED_STEP_TAG_PREFIX
import com.denariidolor.presentation.ui.dashboard.GettingStarted
import com.denariidolor.presentation.ui.dashboard.GettingStartedStep
import com.denariidolor.presentation.ui.guide.RATE_PROMPT_LATER_TAG
import com.denariidolor.presentation.ui.guide.RATE_PROMPT_NEVER_TAG
import com.denariidolor.presentation.ui.guide.RATE_PROMPT_RATE_TAG
import com.denariidolor.presentation.ui.guide.RatePromptDialog
import com.denariidolor.presentation.ui.guide.WELCOME_TOUR_NEXT_TAG
import com.denariidolor.presentation.ui.guide.WELCOME_TOUR_SKIP_TAG
import com.denariidolor.presentation.ui.guide.WelcomeTour
import com.denariidolor.presentation.ui.help.FaqEntry
import com.denariidolor.presentation.ui.help.HELP_ANSWER_TAG_PREFIX
import com.denariidolor.presentation.ui.help.HELP_FEEDBACK_TAG
import com.denariidolor.presentation.ui.help.HELP_QUESTION_TAG_PREFIX
import com.denariidolor.presentation.ui.help.HelpScreen
import com.denariidolor.presentation.ui.settings.SettingsScreenState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GuideScreensTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tourWalksThroughEveryPageThenFinishes() {
        var finished = 0
        composeRule.setContent { DenariiDolorTheme { WelcomeTour(onFinish = { finished++ }) } }

        repeat(3) { composeRule.onNodeWithTag(WELCOME_TOUR_NEXT_TAG).assertTextContains("Next").performClick() }

        composeRule.onNodeWithTag(WELCOME_TOUR_SKIP_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(WELCOME_TOUR_NEXT_TAG).assertTextContains("Get started").performClick()
        assertEquals(1, finished)
    }

    @Test
    fun tourCanBeSkippedFromTheFirstPage() {
        var finished = 0
        composeRule.setContent { DenariiDolorTheme { WelcomeTour(onFinish = { finished++ }) } }

        composeRule.onNodeWithTag(WELCOME_TOUR_SKIP_TAG).performClick()

        assertEquals(1, finished)
    }

    @Test
    fun ratePromptReportsEachChoice() {
        val choices = mutableListOf<String>()
        composeRule.setContent {
            DenariiDolorTheme {
                RatePromptDialog(onRate = { choices += "rate" }, onLater = { choices += "later" }, onNever = { choices += "never" })
            }
        }

        composeRule.onNodeWithTag(RATE_PROMPT_LATER_TAG).performClick()
        composeRule.onNodeWithTag(RATE_PROMPT_NEVER_TAG).performClick()
        composeRule.onNodeWithTag(RATE_PROMPT_RATE_TAG).performClick()

        assertEquals(listOf("later", "never", "rate"), choices)
    }

    @Test
    fun helpAnswersOpenOnTapAndFeedbackIsOneTapAway() {
        var feedback = 0
        val entries = listOf(FaqEntry("one", "Question one?", "Answer one."), FaqEntry("two", "Question two?", "Answer two."))
        composeRule.setContent { DenariiDolorTheme { HelpScreen(entries = entries, onBack = {}, onSendFeedback = { feedback++ }) } }

        composeRule.onNodeWithTag(HELP_ANSWER_TAG_PREFIX + "one").assertDoesNotExist()
        composeRule.onNodeWithTag(HELP_QUESTION_TAG_PREFIX + "one").performClick()
        composeRule.onNodeWithTag(HELP_ANSWER_TAG_PREFIX + "one").assertIsDisplayed().assertTextContains("Answer one.")
        composeRule.onNodeWithTag(HELP_ANSWER_TAG_PREFIX + "two").assertDoesNotExist()

        composeRule.onNodeWithTag(HELP_FEEDBACK_TAG).performClick()
        assertEquals(1, feedback)
    }

    @Test
    fun settingsHelpRowsCallTheirActions() {
        val calls = mutableListOf<String>()
        composeRule.setContent {
            DenariiDolorTheme {
                SettingsScreen(
                    state = SettingsScreenState(sessionTimeoutMinutes = 5, pinConfigured = true, darkMode = false),
                    onSignOut = {},
                    onHelp = { calls += "help" },
                    onShowTour = { calls += "tour" },
                    onSendFeedback = { calls += "feedback" },
                    onRateApp = { calls += "rate" },
                    onShareApp = { calls += "share" }
                )
            }
        }

        listOf(SETTINGS_HELP_TAG, SETTINGS_TOUR_TAG, SETTINGS_FEEDBACK_TAG, SETTINGS_RATE_TAG, SETTINGS_SHARE_TAG).forEach { tag ->
            composeRule.onNodeWithTag(tag).performScrollTo().performClick()
        }

        assertEquals(listOf("help", "tour", "feedback", "rate", "share"), calls)
    }

    @Test
    fun gettingStartedCardOpensStepsAndHides() {
        val steps = mutableListOf<GettingStartedStep>()
        var hidden = 0
        composeRule.setContent {
            DenariiDolorTheme {
                DashboardScreen(
                    state = DashboardUiState(gettingStarted = GettingStarted(setOf(GettingStartedStep.ADD_ACCOUNT))),
                    onEditTransaction = {},
                    onDeleteTransaction = {},
                    onGettingStartedStep = { steps += it },
                    onHideGettingStarted = { hidden++ }
                )
            }
        }

        composeRule.onNodeWithTag(GETTING_STARTED_CARD_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("1 of 3 done").assertIsDisplayed()
        composeRule.onNodeWithTag(GETTING_STARTED_STEP_TAG_PREFIX + GettingStartedStep.SET_BUDGET.name).performClick()
        composeRule.onNodeWithTag(GETTING_STARTED_HIDE_TAG).performClick()

        assertEquals(listOf(GettingStartedStep.SET_BUDGET), steps)
        assertEquals(1, hidden)
    }
}
