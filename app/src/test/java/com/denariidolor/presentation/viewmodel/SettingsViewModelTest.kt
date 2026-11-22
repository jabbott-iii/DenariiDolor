/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.viewmodel

import com.denariidolor.R
import com.denariidolor.data.local.preferences.SecureStorageException
import com.denariidolor.data.local.vault.SettingsVault
import com.denariidolor.data.local.vault.VaultState
import com.denariidolor.presentation.platform.BiometricPromptResult
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.presentation.ui.settings.SettingsEvent
import com.denariidolor.presentation.ui.settings.SettingsScreenState
import com.denariidolor.presentation.ui.settings.SettingsUiState
import com.denariidolor.presentation.ui.settings.SettingsViewModel
import com.denariidolor.testutil.FakeBiometricCapability
import com.denariidolor.testutil.FakeCurrencySettings
import com.denariidolor.testutil.FakeThemeSettings
import com.denariidolor.testutil.MainDispatcherRule
import java.util.Currency
import javax.crypto.Cipher
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** What Settings did in MainActivity (biometric sign-in, dark mode, currency), now in [SettingsViewModel]. */
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vault = FakeSettingsVault()
    private val biometrics = FakeBiometricCapability()
    private val theme = FakeThemeSettings()
    private val currency = FakeCurrencySettings()

    private fun viewModel() = SettingsViewModel(vault, biometrics, theme, currency)

    /** Opens Settings once [setUp] has arranged the fakes, and runs [block] with the state collected, as on screen. */
    private fun runOnScreen(setUp: () -> Unit = {}, block: suspend TestScope.(SettingsViewModel) -> Unit) = runTest {
        setUp()
        val viewModel = viewModel()
        val screen = launch { viewModel.state.collect {} }
        yield()
        block(viewModel)
        screen.cancel()
    }

    /** The first state that matches; fails fast instead of waiting forever for one that never comes. */
    private suspend fun SettingsViewModel.stateWhen(predicate: (SettingsUiState) -> Boolean): SettingsUiState =
        withTimeout(1.seconds) { state.first(predicate) }

    /** The next message, or null when none comes. */
    private suspend fun SettingsViewModel.nextMessage(): UiMessage? = withTimeoutOrNull(1.seconds) { messages.first() }

    private suspend fun SettingsViewModel.nextEvent(): SettingsEvent? = withTimeoutOrNull(1.seconds) { events.first() }

    /** Turns the switch on, as a tap does, and returns the cipher the enrollment prompt was asked to authorize. */
    private suspend fun SettingsViewModel.startEnrollment(): Cipher {
        onBiometricChange(true)
        stateWhen { it.biometricChanging }
        return (nextEvent() as SettingsEvent.ShowEnrollmentPrompt).cipher
    }

    private fun message(resId: Int) = UiMessage.Resource(resId)

    @Test
    fun showsBiometricSignInAsTheVaultHasIt() = runTest {
        vault.enrolled = true

        val state = viewModel().stateWhen { it.biometricEnabled }

        assertTrue(state.biometricAvailable)
        assertFalse(state.biometricChanging)
    }

    @Test
    fun offersTheSwitchWhenTheDeviceCanUseBiometricsOrWhileItIsOn() = runTest {
        biometrics.available = false
        assertFalse(viewModel().stateWhen { true }.biometricAvailable)

        // Still shown after the device loses its biometrics, so it can be turned off.
        vault.enrolled = true
        assertTrue(viewModel().stateWhen { it.biometricEnabled }.biometricAvailable)

        biometrics.available = true
        vault.enrolled = false
        assertTrue(viewModel().stateWhen { true }.biometricAvailable)
    }

    @Test
    fun anUnreadableVaultShowsBiometricSignInOff() = runOnScreen(
        setUp = {
            vault.enrolled = true
            vault.stateFailure = SecureStorageException("unreadable")
        }
    ) { viewModel ->
        assertEquals(listOf("state"), vault.calls)
        assertFalse(viewModel.stateWhen { true }.biometricEnabled)
    }

    @Test
    fun turningBiometricSignInOnShowsTheEnrollmentPromptWithTheVaultsCipher() = runOnScreen { viewModel ->
        assertSame(vault.enrollmentCipher, viewModel.startEnrollment())
        assertEquals(listOf("prepare"), vault.calls.filterNot { it == "state" })
    }

    @Test
    fun tapsAreIgnoredUntilThePromptAnswers() = runOnScreen { viewModel ->
        viewModel.startEnrollment()

        viewModel.onBiometricChange(true)
        viewModel.onBiometricChange(false)

        assertEquals(listOf("prepare"), vault.calls.filterNot { it == "state" })
        assertNull(viewModel.nextEvent())
        assertTrue(viewModel.stateWhen { true }.biometricChanging)
    }

    @Test
    fun theAuthorizedCipherTurnsBiometricSignInOn() = runOnScreen { viewModel ->
        val cipher = viewModel.startEnrollment()

        viewModel.onEnrollmentPromptResult(BiometricPromptResult.Authorized(cipher))

        assertEquals(message(R.string.biometric_enabled), viewModel.nextMessage())
        assertSame(cipher, vault.authorizedCipher)
        assertFalse(viewModel.stateWhen { it.biometricEnabled }.biometricChanging)
    }

    @Test
    fun anEnrollmentTheVaultRefusesSaysItFailed() = runOnScreen { viewModel ->
        vault.completeResult = false

        viewModel.onEnrollmentPromptResult(BiometricPromptResult.Authorized(viewModel.startEnrollment()))

        assertEquals(message(R.string.biometric_enable_failed), viewModel.nextMessage())
        assertFalse(viewModel.stateWhen { !it.biometricChanging }.biometricEnabled)
    }

    @Test
    fun aVaultErrorWhileEnrollingSaysItFailed() = runOnScreen { viewModel ->
        vault.completeFailure = SecureStorageException("Couldn't wrap the key")

        viewModel.onEnrollmentPromptResult(BiometricPromptResult.Authorized(viewModel.startEnrollment()))

        assertEquals(message(R.string.biometric_enable_failed), viewModel.nextMessage())
        assertFalse(viewModel.stateWhen { !it.biometricChanging }.biometricEnabled)
    }

    @Test
    fun aVaultErrorBeforeThePromptSaysItFailedWithoutAPrompt() = runOnScreen { viewModel ->
        vault.prepareFailure = IllegalStateException("The vault is locked")

        viewModel.onBiometricChange(true)

        assertEquals(message(R.string.biometric_enable_failed), viewModel.nextMessage())
        assertNull(viewModel.nextEvent())
        assertFalse(viewModel.stateWhen { !it.biometricChanging }.biometricEnabled)
    }

    @Test
    fun aPromptThatFailsSaysSo() = runOnScreen { viewModel ->
        viewModel.startEnrollment()

        viewModel.onEnrollmentPromptResult(BiometricPromptResult.Failed)

        assertEquals(message(R.string.biometric_enable_failed), viewModel.nextMessage())
        assertFalse(viewModel.stateWhen { !it.biometricChanging }.biometricEnabled)
        assertFalse("complete" in vault.calls)
    }

    @Test
    fun aCanceledPromptLeavesBiometricSignInOffQuietly() = runOnScreen { viewModel ->
        viewModel.startEnrollment()

        viewModel.onEnrollmentPromptResult(BiometricPromptResult.Canceled)

        assertFalse(viewModel.stateWhen { !it.biometricChanging }.biometricEnabled)
        assertNull(viewModel.nextMessage())
        assertFalse("complete" in vault.calls)
    }

    @Test
    fun aPromptAnswerWithNoEnrollmentWaitingIsIgnored() = runOnScreen { viewModel ->
        viewModel.onEnrollmentPromptResult(BiometricPromptResult.Authorized(vault.enrollmentCipher))

        assertFalse("complete" in vault.calls)
        assertNull(viewModel.nextMessage())
    }

    @Test
    fun turningBiometricSignInOffRemovesItsKey() = runOnScreen(setUp = { vault.enrolled = true }) { viewModel ->
        viewModel.stateWhen { it.biometricEnabled }

        viewModel.onBiometricChange(false)

        assertTrue("disable" in vault.calls)
        val state = viewModel.stateWhen { !it.biometricEnabled }
        assertFalse(state.biometricChanging)
        assertTrue(state.biometricAvailable)
        assertNull(viewModel.nextMessage())
    }

    @Test
    fun aFailureTurningItOffShowsTheSwitchAsTheVaultHasIt() = runOnScreen(
        setUp = {
            vault.enrolled = true
            vault.disableFailure = SecureStorageException("unwritable")
        }
    ) { viewModel ->
        viewModel.stateWhen { it.biometricEnabled }

        viewModel.onBiometricChange(false)

        assertTrue("disable" in vault.calls)
        assertTrue(viewModel.stateWhen { !it.biometricChanging }.biometricEnabled)
        assertNull(viewModel.nextMessage())
    }

    @Test
    fun theDarkModeChoiceIsSavedAndShown() = runOnScreen { viewModel ->
        viewModel.onDarkModeChange(true)

        assertEquals(true, theme.darkModeOverride.value)
        assertEquals(true, viewModel.stateWhen { it.darkModeOverride != null }.darkModeOverride)
    }

    @Test
    fun choosingAnotherCurrencySavesItAndStartsTheAppOver() = runOnScreen { viewModel ->
        viewModel.onCurrencyChange("EUR")

        assertEquals(listOf(Currency.getInstance("EUR")), currency.saved)
        assertEquals(SettingsEvent.CurrencyChanged, viewModel.nextEvent())
        assertEquals("EUR", viewModel.stateWhen { it.currencyCode == "EUR" }.currencyCode)
    }

    @Test
    fun choosingTheSavedOrAnUnsupportedCurrencyChangesNothing() = runOnScreen { viewModel ->
        viewModel.onCurrencyChange("USD")
        viewModel.onCurrencyChange("JPY")
        viewModel.onCurrencyChange("not a code")

        assertEquals(emptyList<Currency>(), currency.saved)
        assertNull(viewModel.nextEvent())
    }

    @Test
    fun theSavedCurrencyIsAlwaysOffered() = runTest {
        currency.currency.value = Currency.getInstance("NZD")

        val state = viewModel().stateWhen { true }

        assertEquals("NZD", state.currencyCode)
        assertEquals("USD", state.currencyOptions.first())
        assertEquals("NZD", state.currencyOptions.last())
    }

    @Test
    fun theScreenFollowsTheDeviceThemeUntilTheUserChoosesOne() {
        val state = SettingsUiState(biometricAvailable = true, biometricEnabled = true, biometricChanging = true, currencyCode = "INR")

        assertEquals(
            SettingsScreenState(
                sessionTimeoutMinutes = 5,
                pinConfigured = true,
                darkMode = true,
                biometricAvailable = true,
                biometricEnabled = true,
                biometricChanging = true,
                currencyCode = "INR"
            ),
            state.toScreenState(systemDarkMode = true)
        )
        assertFalse(state.copy(darkModeOverride = false).toScreenState(systemDarkMode = true).darkMode)
        assertTrue(state.copy(darkModeOverride = true).toScreenState(systemDarkMode = false).darkMode)
    }

    /** Answers from its fields and records each call. */
    private class FakeSettingsVault : SettingsVault {
        var enrolled = false
        var stateFailure: Throwable? = null
        val enrollmentCipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
        var prepareFailure: Throwable? = null
        var completeResult = true
        var completeFailure: Throwable? = null
        var authorizedCipher: Cipher? = null
        var disableFailure: Throwable? = null
        val calls = mutableListOf<String>()

        override suspend fun state(): VaultState {
            calls += "state"
            stateFailure?.let { throw it }
            return VaultState.Configured("Where did you first work?", biometricEnrolled = enrolled, upgradeStarted = false)
        }

        override suspend fun prepareBiometricEnrollment(): Cipher {
            calls += "prepare"
            prepareFailure?.let { throw it }
            return enrollmentCipher
        }

        override suspend fun completeBiometricEnrollment(cipher: Cipher): Boolean {
            calls += "complete"
            authorizedCipher = cipher
            completeFailure?.let { throw it }
            if (completeResult) enrolled = true
            return completeResult
        }

        override suspend fun disableBiometric() {
            calls += "disable"
            disableFailure?.let { throw it }
            enrolled = false
        }
    }
}
