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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.denariidolor.R
import com.denariidolor.data.local.preferences.SecurityProfileService
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.presentation.ui.common.PickerOption
import com.denariidolor.util.Constants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val DIALOG_TIMEOUT_MILLIS = 5_000L

class ComposeScreensTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int, vararg args: Any) = composeRule.activity.getString(id, *args)

    @Composable
    private fun TestLoginScreen(mode: LoginScreenMode, showWipeConfirmation: Boolean = false, onConfirmWipeData: () -> Unit = {}) {
        DenariiDolorTheme {
            LoginScreen(
                mode = mode,
                pin = "",
                pinConfirmation = "",
                securityQuestion = "",
                securityAnswer = "",
                recoveryQuestion = null,
                signInEnabled = true,
                biometricAvailable = false,
                showWipeConfirmation = showWipeConfirmation,
                feedbackMessage = null,
                onPinChange = {},
                onPinConfirmationChange = {},
                onSecurityQuestionChange = {},
                onSecurityAnswerChange = {},
                onPrimaryAction = {},
                onForgotPin = {},
                onBackToSignIn = {},
                onBiometricLogin = {},
                onRequestWipeData = {},
                onCancelWipeData = {},
                onConfirmWipeData = onConfirmWipeData
            )
        }
    }

    @Test
    fun loginScreenWipeNeedsTypedWordAndCountdown() {
        var confirmed = false
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TestLoginScreen(mode = LoginScreenMode.SIGN_IN, showWipeConfirmation = true, onConfirmWipeData = { confirmed = true })
        }

        composeRule.onNodeWithText(text(R.string.wipe_data_confirm_countdown, WIPE_COUNTDOWN_SECONDS)).assertIsDisplayed()
        composeRule.onNodeWithTag(WIPE_CONFIRM_BUTTON_TAG).assertIsNotEnabled()

        composeRule.onNodeWithTag(WIPE_CONFIRMATION_FIELD_TAG).performTextReplacement("wipe")
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag(WIPE_CONFIRM_BUTTON_TAG).assertIsNotEnabled()

        composeRule.mainClock.advanceTimeBy(WIPE_COUNTDOWN_SECONDS * 1_000L)
        composeRule.onNodeWithText(text(R.string.wipe_data_confirm)).assertIsDisplayed()
        composeRule.onNodeWithTag(WIPE_CONFIRM_BUTTON_TAG).assertIsEnabled().performClick()
        composeRule.runOnIdle { assertTrue(confirmed) }
    }

    @Test
    fun loginScreenWipeStaysDisabledForTheWrongWord() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { TestLoginScreen(mode = LoginScreenMode.SIGN_IN, showWipeConfirmation = true) }

        composeRule.onNodeWithTag(WIPE_CONFIRMATION_FIELD_TAG).performTextReplacement("delete")
        composeRule.mainClock.advanceTimeBy(WIPE_COUNTDOWN_SECONDS * 1_000L)

        composeRule.onNodeWithTag(WIPE_CONFIRM_BUTTON_TAG).assertIsNotEnabled()
    }

    @Test
    fun loginScreenSetupShowsRecoveryAnswerRules() {
        composeRule.setContent { TestLoginScreen(mode = LoginScreenMode.SETUP) }

        composeRule.onNodeWithText(text(R.string.security_answer_rules, SecurityProfileService.MIN_ANSWER_LENGTH)).assertIsDisplayed()
    }

    @Test
    fun loginScreenUpgradeModeAsksForNewQuestionWithoutPin() {
        composeRule.setContent { TestLoginScreen(mode = LoginScreenMode.UPGRADE_RECOVERY) }

        composeRule.onNodeWithText(text(R.string.upgrade_recovery_intro)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.security_question_hint)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.security_answer_rules, SecurityProfileService.MIN_ANSWER_LENGTH)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.save_and_continue)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.back_to_sign_in)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.pin_hint)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.wipe_all_data)).assertDoesNotExist()
    }

    @Test
    fun loginScreenStorageErrorOffersRetryAndWipeOnly() {
        composeRule.setContent { TestLoginScreen(mode = LoginScreenMode.STORAGE_ERROR) }

        composeRule.onNodeWithText(text(R.string.storage_error_message)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.try_again)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.wipe_all_data)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.pin_hint)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.forgot_pin)).assertDoesNotExist()
    }

    @Test
    fun loginScreenHidesBiometricButtonWhenUnavailable() {
        composeRule.setContent {
            DenariiDolorTheme {
                LoginScreen(
                    mode = LoginScreenMode.SIGN_IN,
                    pin = "",
                    pinConfirmation = "",
                    securityQuestion = "",
                    securityAnswer = "",
                    recoveryQuestion = null,
                    signInEnabled = false,
                    biometricAvailable = false,
                    showWipeConfirmation = false,
                    feedbackMessage = null,
                    onPinChange = {},
                    onPinConfirmationChange = {},
                    onSecurityQuestionChange = {},
                    onSecurityAnswerChange = {},
                    onPrimaryAction = {},
                    onForgotPin = {},
                    onBackToSignIn = {},
                    onBiometricLogin = {},
                    onRequestWipeData = {},
                    onCancelWipeData = {},
                    onConfirmWipeData = {}
                )
            }
        }

        composeRule.onNodeWithTag(LOGIN_BUTTON_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(BIOMETRIC_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun addTransactionScreenShowsTransferAccountFieldForTransfers() {
        composeRule.setContent {
            DenariiDolorTheme {
                AddTransactionScreen(
                    onSave = { _, _, _, _, _, _, _ -> },
                    onShowMessage = {},
                    accounts = listOf(
                        PickerOption(Constants.DEFAULT_CASH_ACCOUNT_ID, "Cash"),
                        PickerOption(Constants.DEFAULT_SAVINGS_ACCOUNT_ID, "Savings")
                    )
                )
            }
        }

        composeRule.onNodeWithTag(TRANSFER_ACCOUNT_FIELD_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(TRANSACTION_TYPE_FIELD_TAG).performClick()
        composeRule.onNodeWithText("TRANSFER").performClick()
        composeRule.onNodeWithTag(TRANSFER_ACCOUNT_FIELD_TAG)
            .assertIsDisplayed()
            .assertTextContains("Savings")
    }

    @Test
    fun loginScreenWipeDialogCancelDoesNotTriggerConfirmAction() {
        var cancelTriggered = false
        var confirmTriggered = false

        composeRule.setContent {
            DenariiDolorTheme {
                LoginScreen(
                    mode = LoginScreenMode.SIGN_IN,
                    pin = "",
                    pinConfirmation = "",
                    securityQuestion = "",
                    securityAnswer = "",
                    recoveryQuestion = null,
                    signInEnabled = true,
                    biometricAvailable = false,
                    showWipeConfirmation = true,
                    feedbackMessage = null,
                    onPinChange = {},
                    onPinConfirmationChange = {},
                    onSecurityQuestionChange = {},
                    onSecurityAnswerChange = {},
                    onPrimaryAction = {},
                    onForgotPin = {},
                    onBackToSignIn = {},
                    onBiometricLogin = {},
                    onRequestWipeData = {},
                    onCancelWipeData = { cancelTriggered = true },
                    onConfirmWipeData = { confirmTriggered = true }
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.wipe_data_cancel)).performClick()
        composeRule.runOnIdle {
            assertTrue(cancelTriggered)
            assertFalse(confirmTriggered)
        }
    }

    @Test
    fun loginScreenSetupModeShowsSetupFieldsOnly() {
        composeRule.setContent {
            DenariiDolorTheme {
                LoginScreen(
                    mode = LoginScreenMode.SETUP,
                    pin = "",
                    pinConfirmation = "",
                    securityQuestion = "",
                    securityAnswer = "",
                    recoveryQuestion = null,
                    signInEnabled = true,
                    biometricAvailable = false,
                    showWipeConfirmation = false,
                    feedbackMessage = null,
                    onPinChange = {},
                    onPinConfirmationChange = {},
                    onSecurityQuestionChange = {},
                    onSecurityAnswerChange = {},
                    onPrimaryAction = {},
                    onForgotPin = {},
                    onBackToSignIn = {},
                    onBiometricLogin = {},
                    onRequestWipeData = {},
                    onCancelWipeData = {},
                    onConfirmWipeData = {}
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.security_question_hint)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.security_answer_hint)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.forgot_pin)).assertDoesNotExist()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.wipe_all_data)).assertDoesNotExist()
    }

    @Test
    fun loginScreenRecoveryModeShowsQuestionAndBackAction() {
        val recoveryPrompt = "What is your favorite color?"
        composeRule.setContent {
            DenariiDolorTheme {
                LoginScreen(
                    mode = LoginScreenMode.RECOVER_PIN,
                    pin = "",
                    pinConfirmation = "",
                    securityQuestion = "",
                    securityAnswer = "",
                    recoveryQuestion = recoveryPrompt,
                    signInEnabled = true,
                    biometricAvailable = false,
                    showWipeConfirmation = false,
                    feedbackMessage = null,
                    onPinChange = {},
                    onPinConfirmationChange = {},
                    onSecurityQuestionChange = {},
                    onSecurityAnswerChange = {},
                    onPrimaryAction = {},
                    onForgotPin = {},
                    onBackToSignIn = {},
                    onBiometricLogin = {},
                    onRequestWipeData = {},
                    onCancelWipeData = {},
                    onConfirmWipeData = {}
                )
            }
        }

        composeRule.onNodeWithText(recoveryPrompt).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.back_to_sign_in)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.forgot_pin)).assertDoesNotExist()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.wipe_all_data)).assertDoesNotExist()
    }

    @Test
    fun loginScreenPrimaryActionLabelMatchesMode() {
        var mode by mutableStateOf(LoginScreenMode.SIGN_IN)
        composeRule.setContent {
            DenariiDolorTheme {
                LoginScreen(
                    mode = mode,
                    pin = "",
                    pinConfirmation = "",
                    securityQuestion = "",
                    securityAnswer = "",
                    recoveryQuestion = "Recovery prompt",
                    signInEnabled = true,
                    biometricAvailable = false,
                    showWipeConfirmation = false,
                    feedbackMessage = null,
                    onPinChange = {},
                    onPinConfirmationChange = {},
                    onSecurityQuestionChange = {},
                    onSecurityAnswerChange = {},
                    onPrimaryAction = {},
                    onForgotPin = {},
                    onBackToSignIn = {},
                    onBiometricLogin = {},
                    onRequestWipeData = {},
                    onCancelWipeData = {},
                    onConfirmWipeData = {}
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.sign_in)).assertIsDisplayed()

        mode = LoginScreenMode.SETUP
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.create_security_profile)).assertIsDisplayed()

        mode = LoginScreenMode.RECOVER_PIN
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.reset_pin)).assertIsDisplayed()
    }

    @Test
    fun loginScreenPinVisibilityToggleSwitchesLabel() {
        composeRule.setContent {
            DenariiDolorTheme {
                LoginScreen(
                    mode = LoginScreenMode.SIGN_IN,
                    pin = "123456",
                    pinConfirmation = "",
                    securityQuestion = "",
                    securityAnswer = "",
                    recoveryQuestion = null,
                    signInEnabled = true,
                    biometricAvailable = false,
                    showWipeConfirmation = false,
                    feedbackMessage = null,
                    onPinChange = {},
                    onPinConfirmationChange = {},
                    onSecurityQuestionChange = {},
                    onSecurityAnswerChange = {},
                    onPrimaryAction = {},
                    onForgotPin = {},
                    onBackToSignIn = {},
                    onBiometricLogin = {},
                    onRequestWipeData = {},
                    onCancelWipeData = {},
                    onConfirmWipeData = {}
                )
            }
        }

        val showLabel = composeRule.activity.getString(R.string.show_pin)
        val hideLabel = composeRule.activity.getString(R.string.hide_pin)
        composeRule.onNodeWithText(showLabel).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(hideLabel).assertIsDisplayed()
    }

    @Test
    fun loginScreenWipeDialogConfirmTriggersDestructiveActionOnly() {
        var cancelTriggered = false
        var confirmTriggered = false

        composeRule.setContent {
            DenariiDolorTheme {
                LoginScreen(
                    mode = LoginScreenMode.SIGN_IN,
                    pin = "",
                    pinConfirmation = "",
                    securityQuestion = "",
                    securityAnswer = "",
                    recoveryQuestion = null,
                    signInEnabled = true,
                    biometricAvailable = false,
                    showWipeConfirmation = true,
                    feedbackMessage = null,
                    onPinChange = {},
                    onPinConfirmationChange = {},
                    onSecurityQuestionChange = {},
                    onSecurityAnswerChange = {},
                    onPrimaryAction = {},
                    onForgotPin = {},
                    onBackToSignIn = {},
                    onBiometricLogin = {},
                    onRequestWipeData = {},
                    onCancelWipeData = { cancelTriggered = true },
                    onConfirmWipeData = { confirmTriggered = true }
                )
            }
        }

        composeRule.onNodeWithTag(WIPE_CONFIRMATION_FIELD_TAG).performTextReplacement("WIPE")
        composeRule.mainClock.advanceTimeBy(WIPE_COUNTDOWN_SECONDS * 1_000L)
        composeRule.onNodeWithTag(WIPE_CONFIRM_BUTTON_TAG).assertIsEnabled().performClick()
        composeRule.runOnIdle {
            assertTrue(confirmTriggered)
            assertFalse(cancelTriggered)
        }
    }

    @Test
    fun loginScreenWipeDialogDismissRequestTriggersCancelCallback() {
        var cancelTriggered = false

        composeRule.setContent {
            var showDialog by rememberSaveable { mutableStateOf(true) }
            DenariiDolorTheme {
                LoginScreen(
                    mode = LoginScreenMode.SIGN_IN,
                    pin = "",
                    pinConfirmation = "",
                    securityQuestion = "",
                    securityAnswer = "",
                    recoveryQuestion = null,
                    signInEnabled = true,
                    biometricAvailable = false,
                    showWipeConfirmation = showDialog,
                    feedbackMessage = null,
                    onPinChange = {},
                    onPinConfirmationChange = {},
                    onSecurityQuestionChange = {},
                    onSecurityAnswerChange = {},
                    onPrimaryAction = {},
                    onForgotPin = {},
                    onBackToSignIn = {},
                    onBiometricLogin = {},
                    onRequestWipeData = {},
                    onCancelWipeData = {
                        cancelTriggered = true
                        showDialog = false
                    },
                    onConfirmWipeData = {}
                )
            }
        }

        // A real system back key goes to the focused window, which is the dialog (not the activity behind it). UiAutomator
        // doesn't wait for Compose, so wait until the dialog window is on screen first; the API 26 emulator is slow to show it.
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeRule.onNodeWithTag(WIPE_CONFIRM_BUTTON_TAG).assertIsDisplayed()
        assertTrue(device.wait(Until.hasObject(By.text(text(R.string.wipe_data_title))), DIALOG_TIMEOUT_MILLIS))
        // Android 8.x gives a new window initial focus even in touch mode, so the dialog's "type WIPE" field is focused
        // and the keyboard opens; the first Back would only close the keyboard. Close it first (a no-op when none is
        // shown) so that the Back below is the dialog's dismiss request.
        Espresso.closeSoftKeyboard()
        device.waitForIdle()
        device.pressBack()
        composeRule.runOnIdle {
            assertTrue("Back did not reach the wipe dialog's onDismissRequest", cancelTriggered)
        }
        composeRule.onNodeWithTag(WIPE_CONFIRM_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun recoveryModeBackButtonAndSystemBackReturnToSignIn() {
        var mode by mutableStateOf(LoginScreenMode.RECOVER_PIN)
        var backCalls = 0
        composeRule.setContent {
            DenariiDolorTheme {
                LoginScreen(
                    mode = mode,
                    pin = "",
                    pinConfirmation = "",
                    securityQuestion = "",
                    securityAnswer = "",
                    recoveryQuestion = "Recovery prompt",
                    signInEnabled = true,
                    biometricAvailable = false,
                    showWipeConfirmation = false,
                    feedbackMessage = null,
                    onPinChange = {},
                    onPinConfirmationChange = {},
                    onSecurityQuestionChange = {},
                    onSecurityAnswerChange = {},
                    onPrimaryAction = {},
                    onForgotPin = { mode = LoginScreenMode.RECOVER_PIN },
                    onBackToSignIn = {
                        backCalls++
                        mode = LoginScreenMode.SIGN_IN
                    },
                    onBiometricLogin = {},
                    onRequestWipeData = {},
                    onCancelWipeData = {},
                    onConfirmWipeData = {}
                )
            }
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.back_to_sign_in)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.sign_in)).assertIsDisplayed()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.forgot_pin)).performClick()
        // Wait until recovery mode is composed so its BackHandler is registered and enabled;
        // otherwise the back press reaches the activity and finishes it.
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.back_to_sign_in)).assertIsDisplayed()
        composeRule.waitForIdle()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.sign_in)).assertIsDisplayed()
        composeRule.runOnIdle { assertTrue(backCalls == 2) }
    }
}
