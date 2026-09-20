package com.denariidolor.presentation.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.denariidolor.R
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
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
                    statusMessage = null,
                    onSave = { _, _, _, _, _, _, _ -> },
                    onShowMessage = {}
                )
            }
        }

        composeRule.onNodeWithTag(TransferAccountFieldTag).assertDoesNotExist()
        composeRule.onNodeWithTag(TransactionTypeFieldTag).performClick()
        composeRule.onNodeWithText("TRANSFER").performClick()
        composeRule.onNodeWithTag(TransferAccountFieldTag)
            .assertIsDisplayed()
            .assertTextEquals(Constants.DEFAULT_SAVINGS_ACCOUNT_ID.toString())
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
}
