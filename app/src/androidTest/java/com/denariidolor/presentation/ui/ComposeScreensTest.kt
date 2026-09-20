package com.denariidolor.presentation.ui

import androidx.activity.ComponentActivity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
import com.denariidolor.R
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.util.Constants
import org.junit.Rule
import org.junit.Test

class ComposeScreensTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

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
        val confirmPinLabel = context.getString(R.string.confirm_pin_hint)
        val securityQuestionLabel = context.getString(R.string.security_question_hint)
        val securityAnswerLabel = context.getString(R.string.security_answer_hint)
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

        composeRule.onNodeWithText(confirmPinLabel).assertIsDisplayed()
        composeRule.onNodeWithText(securityQuestionLabel).assertIsDisplayed()
        composeRule.onNodeWithText(securityAnswerLabel).assertIsDisplayed()
    }

    @Test
    fun loginScreenTogglesRecoveryPanelAndShowsWipeDialog() {
        val recoverPinLabel = context.getString(R.string.recover_pin)
        val hideRecoveryLabel = context.getString(R.string.hide_recovery)
        val recoveryExpanded = context.getString(R.string.pin_recovery_expanded)
        val newPinLabel = context.getString(R.string.new_pin_hint)
        val wipeWarningLabel = context.getString(R.string.wipe_all_data_warning_label)
        val wipeButtonLabel = context.getString(R.string.wipe_all_data)
        val wipeDialogTitle = context.getString(R.string.wipe_data_confirmation_title)
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

        composeRule.onNodeWithText(recoverPinLabel).performClick()
        composeRule.onNodeWithText(hideRecoveryLabel)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    recoveryExpanded
                )
            )
        composeRule.onNodeWithText(newPinLabel).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(wipeWarningLabel)
            .assertIsDisplayed()
        composeRule.onNodeWithText(wipeButtonLabel).performClick()
        composeRule.onNodeWithText(wipeDialogTitle).assertIsDisplayed()
    }
}
