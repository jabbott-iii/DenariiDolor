/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.denariidolor.R
import com.denariidolor.data.local.preferences.CurrencySettings
import com.denariidolor.data.local.preferences.ThemeSettings
import com.denariidolor.data.local.vault.SettingsVault
import com.denariidolor.data.local.vault.VaultState
import com.denariidolor.presentation.platform.BiometricPromptResult
import com.denariidolor.presentation.platform.BiometricPromptText
import com.denariidolor.presentation.platform.BiometricPrompter
import com.denariidolor.presentation.ui.auth.BiometricCapability
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.presentation.ui.settings.SettingsViewModel
import com.denariidolor.util.Currencies
import java.util.Currency
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** SettingsRoute shows the enrollment prompt the ViewModel asks for (CS-07) and passes currency and theme choices on. */
@RunWith(AndroidJUnit4::class)
class SettingsRouteTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val vault = FakeSettingsVault()
    private val prompter = RecordingPrompter()
    private val theme = InMemoryThemeSettings()
    private val currency = InMemoryCurrencySettings()
    private val restarts = AtomicInteger()

    private fun showSettings() {
        val viewModel = SettingsViewModel(vault, BiometricCapability { true }, theme, currency)
        composeRule.setContent {
            DenariiDolorTheme {
                SettingsRoute(
                    onSignOut = {},
                    onCurrencyChanged = { restarts.incrementAndGet() },
                    viewModel = viewModel,
                    biometricPrompter = prompter
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun text(resId: Int): String = composeRule.activity.getString(resId)

    private fun tapBiometricSwitchAndWaitForThePrompt(): RecordingPrompter.Request {
        composeRule.onNodeWithTag(BIOMETRIC_SWITCH_TAG).assertIsOff().performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) { prompter.requests.isNotEmpty() }
        return prompter.requests.single()
    }

    @Test
    fun turningBiometricSignInOnAsksTheEnrollmentPromptAndWaitsForIt() {
        showSettings()

        val request = tapBiometricSwitchAndWaitForThePrompt()

        assertSame(vault.enrollmentCipher, request.cipher)
        assertEquals(
            BiometricPromptText(text(R.string.biometric_enroll_title), text(R.string.biometric_enroll_subtitle), text(R.string.cancel)),
            request.text
        )
        // A second enrollment would replace the key this prompt is using, so the switch waits for the answer.
        composeRule.onNodeWithTag(BIOMETRIC_SWITCH_TAG).assertIsNotEnabled().performClick()
        composeRule.runOnIdle {
            assertEquals(1, vault.prepares.get())
            assertEquals(1, prompter.requests.size)
        }

        composeRule.runOnIdle { request.onResult(BiometricPromptResult.Authorized(request.cipher)) }

        composeRule.onNodeWithTag(BIOMETRIC_SWITCH_TAG).assertIsOn().assertIsEnabled()
        assertSame(request.cipher, vault.authorizedCipher)
    }

    @Test
    fun aCanceledPromptLeavesBiometricSignInOffAndTheSwitchUsable() {
        showSettings()

        val request = tapBiometricSwitchAndWaitForThePrompt()
        composeRule.runOnIdle { request.onResult(BiometricPromptResult.Canceled) }

        composeRule.onNodeWithTag(BIOMETRIC_SWITCH_TAG).assertIsOff().assertIsEnabled()
        assertEquals(null, vault.authorizedCipher)
    }

    @Test
    fun choosingAnotherCurrencyStartsTheAppOver() {
        showSettings()

        composeRule.onNodeWithTag(CURRENCY_SETTING_TAG).performClick()
        composeRule.onNodeWithTag(CURRENCY_OPTION_TAG_PREFIX + "EUR").performClick()

        composeRule.waitUntil(TIMEOUT_MILLIS) { restarts.get() == 1 }
        assertEquals("EUR", currency.currency.value.currencyCode)
    }

    @Test
    fun theDarkModeSwitchSavesTheChoice() {
        showSettings()

        composeRule.onNodeWithTag(DARK_MODE_SWITCH_TAG).performClick()

        composeRule.onNodeWithTag(DARK_MODE_SWITCH_TAG).assertIsOn()
        assertEquals(true, theme.darkModeOverride.value)
    }

    /** Records each prompt instead of showing the system one, which a test can't answer. */
    private class RecordingPrompter : BiometricPrompter {
        class Request(val cipher: Cipher, val text: BiometricPromptText, val onResult: (BiometricPromptResult) -> Unit)

        val requests = CopyOnWriteArrayList<Request>()

        override fun authenticate(cipher: Cipher, text: BiometricPromptText, onResult: (BiometricPromptResult) -> Unit) {
            requests += Request(cipher, text, onResult)
        }
    }

    private class FakeSettingsVault : SettingsVault {
        val enrollmentCipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val prepares = AtomicInteger()

        @Volatile
        var enrolled = false

        @Volatile
        var authorizedCipher: Cipher? = null

        override suspend fun state(): VaultState =
            VaultState.Configured("Where did you first work?", biometricEnrolled = enrolled, upgradeStarted = false)

        override suspend fun prepareBiometricEnrollment(): Cipher {
            prepares.incrementAndGet()
            return enrollmentCipher
        }

        override suspend fun completeBiometricEnrollment(cipher: Cipher): Boolean {
            authorizedCipher = cipher
            enrolled = true
            return true
        }

        override suspend fun disableBiometric() {
            enrolled = false
        }
    }

    private class InMemoryThemeSettings : ThemeSettings {
        override val darkModeOverride = MutableStateFlow<Boolean?>(null)

        override fun setDarkMode(enabled: Boolean) {
            darkModeOverride.value = enabled
        }
    }

    private class InMemoryCurrencySettings : CurrencySettings {
        override val currency = MutableStateFlow(Currencies.DEFAULT)

        override fun setCurrency(currency: Currency) {
            this.currency.value = currency
        }

        override fun resetToRegionDefault() = Unit
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
