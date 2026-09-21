package com.denariidolor.presentation.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.denariidolor.R
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.presentation.ui.common.PickerOption
import com.denariidolor.util.Constants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposeScreensTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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

        composeRule.onNodeWithTag(LoginButtonTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(BiometricButtonTag).assertDoesNotExist()
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

        composeRule.onNodeWithTag(TransferAccountFieldTag).assertDoesNotExist()
        composeRule.onNodeWithTag(TransactionTypeFieldTag).performClick()
        composeRule.onNodeWithText("TRANSFER").performClick()
        composeRule.onNodeWithTag(TransferAccountFieldTag)
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
        fun setLoginContent(mode: LoginScreenMode) {
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
        }

        setLoginContent(LoginScreenMode.SIGN_IN)
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.sign_in)).assertIsDisplayed()

        setLoginContent(LoginScreenMode.SETUP)
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.create_security_profile)).assertIsDisplayed()

        setLoginContent(LoginScreenMode.RECOVER_PIN)
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.reset_pin)).assertIsDisplayed()
    }

    @Test
    fun loginScreenPinVisibilityToggleSwitchesLabel() {
        composeRule.setContent {
            DenariiDolorTheme {
                LoginScreen(
                    mode = LoginScreenMode.SIGN_IN,
                    pin = "1234",
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

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.wipe_data_confirm)).performClick()
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

        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.runOnIdle {
            assertTrue(cancelTriggered)
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.wipe_data_confirm)).assertDoesNotExist()
    }
}
