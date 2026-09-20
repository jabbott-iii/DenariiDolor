package com.denariidolor.presentation.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.util.Constants
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
                    pin = "",
                    signInEnabled = false,
                    biometricAvailable = false,
                    isFirstTimeSetup = false,
                    securityQuestionPrompt = null,
                    isSubmitting = false,
                    onPinChange = {},
                    onSignIn = {},
                    onSetup = { _, _, _ -> },
                    onBiometricLogin = {},
                    onRecoverPin = { _, _, _ -> false },
                    onWipeDataConfirmed = {}
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
    fun loginScreenShowsSetupFieldsDuringFirstTimeSetup() {
        composeRule.setContent {
            DenariiDolorTheme {
                LoginScreen(
                    pin = "",
                    signInEnabled = true,
                    biometricAvailable = false,
                    isFirstTimeSetup = true,
                    securityQuestionPrompt = null,
                    isSubmitting = false,
                    onPinChange = {},
                    onSignIn = {},
                    onSetup = { _, _, _ -> },
                    onBiometricLogin = {},
                    onRecoverPin = { _, _, _ -> false },
                    onWipeDataConfirmed = {}
                )
            }
        }

        composeRule.onNodeWithText("Confirm PIN").assertIsDisplayed()
        composeRule.onNodeWithText("Security question").assertIsDisplayed()
        composeRule.onNodeWithText("Security answer").assertIsDisplayed()
    }

    @Test
    fun loginScreenTogglesRecoveryPanelAndShowsWipeDialog() {
        composeRule.setContent {
            DenariiDolorTheme {
                LoginScreen(
                    pin = "",
                    signInEnabled = true,
                    biometricAvailable = false,
                    isFirstTimeSetup = false,
                    securityQuestionPrompt = "City?",
                    isSubmitting = false,
                    onPinChange = {},
                    onSignIn = {},
                    onSetup = { _, _, _ -> },
                    onBiometricLogin = {},
                    onRecoverPin = { _, _, _ -> false },
                    onWipeDataConfirmed = {}
                )
            }
        }

        composeRule.onNodeWithText("Recover PIN").performClick()
        composeRule.onNodeWithText("Hide PIN Recovery")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "PIN recovery options expanded"
                )
            )
        composeRule.onNodeWithText("New PIN").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Warning: permanently delete all app data")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Wipe All Data").performClick()
        composeRule.onNodeWithText("Delete all app data?").assertIsDisplayed()
    }
}
