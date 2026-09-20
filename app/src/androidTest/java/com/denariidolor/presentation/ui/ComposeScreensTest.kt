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
                    onPinChange = {},
                    onLogin = {},
                    onBiometricLogin = {}
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
}
