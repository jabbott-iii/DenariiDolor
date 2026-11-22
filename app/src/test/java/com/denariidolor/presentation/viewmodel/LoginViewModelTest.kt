/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.viewmodel

import com.denariidolor.R
import com.denariidolor.data.local.preferences.CurrencySettings
import com.denariidolor.data.local.preferences.RecoverPinResult
import com.denariidolor.data.local.preferences.SecureStorageException
import com.denariidolor.data.local.preferences.SecurityProfileService
import com.denariidolor.data.local.preferences.SetupProfileResult
import com.denariidolor.data.local.vault.BiometricSignIn
import com.denariidolor.data.local.vault.RecoveryResult
import com.denariidolor.data.local.vault.SignInResult
import com.denariidolor.data.local.vault.SignInVault
import com.denariidolor.data.local.vault.VaultState
import com.denariidolor.presentation.ui.LoginScreenMode
import com.denariidolor.presentation.ui.auth.BiometricCapability
import com.denariidolor.presentation.ui.auth.LoginEvent
import com.denariidolor.presentation.ui.auth.LoginField
import com.denariidolor.presentation.ui.auth.LoginViewModel
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.testutil.FakeOnboardingStore
import com.denariidolor.testutil.MainDispatcherRule
import com.denariidolor.util.SessionManager
import javax.crypto.Cipher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Each branch the sign-in screen had when its state lived in LoginActivity, now in [LoginViewModel]. */
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vault = FakeSignInVault()
    private val biometrics = FakeBiometricCapability()
    private val sessionManager = SessionManager()
    private val currency = FakeCurrencySettings()
    private val onboarding = FakeOnboardingStore()

    private fun viewModel() = LoginViewModel(vault, biometrics, sessionManager, currency, onboarding)

    private fun LoginViewModel.type(pin: String = "", confirmation: String = "", question: String = "", answer: String = "") {
        onInputChange(LoginField.PIN, pin)
        onInputChange(LoginField.PIN_CONFIRMATION, confirmation)
        onInputChange(LoginField.SECURITY_QUESTION, question)
        onInputChange(LoginField.SECURITY_ANSWER, answer)
    }

    private fun LoginViewModel.inputs() = state.value.run { listOf(pin, pinConfirmation, securityQuestion, securityAnswer) }

    private fun message(resId: Int, arg: Any? = null) = UiMessage.Resource(resId, arg)

    @Test
    fun aNewInstallOpensOnSetup() {
        val state = viewModel().state.value

        assertEquals(LoginScreenMode.SETUP, state.mode)
        assertTrue(state.signInEnabled)
        assertFalse(state.biometricAvailable)
        assertNull(state.recoveryQuestion)
    }

    @Test
    fun aProfileOpensOnSignInWithItsQuestionAndBiometricSignIn() {
        vault.state = configured(biometricEnrolled = true)

        val state = viewModel().state.value

        assertEquals(LoginScreenMode.SIGN_IN, state.mode)
        assertEquals(QUESTION, state.recoveryQuestion)
        assertTrue(state.biometricAvailable)
    }

    @Test
    fun biometricSignInIsOfferedOnlyWhenEnrolledAndTheDeviceCanUseIt() {
        vault.state = configured(biometricEnrolled = true)
        biometrics.available = false
        assertFalse(viewModel().state.value.biometricAvailable)

        vault.state = configured(biometricEnrolled = false)
        biometrics.available = true
        assertFalse(viewModel().state.value.biometricAvailable)
    }

    @Test
    fun anUpgradeInProgressOpensOnItsLastStep() {
        vault.state = configured(upgradeStarted = true)

        assertEquals(LoginScreenMode.UPGRADE_RECOVERY, viewModel().state.value.mode)
    }

    @Test
    fun aPinInTheWrongFormatIsRefusedWithoutAskingTheVault() {
        vault.state = configured()
        val viewModel = viewModel()
        viewModel.type(pin = "1234")

        viewModel.onPrimaryAction()

        assertEquals(message(R.string.pin_format_error), viewModel.state.value.feedback)
        assertTrue(vault.calls.none { it.startsWith("signIn") })
    }

    @Test
    fun theRightPinSignsIn() = runTest {
        vault.state = configured()
        val viewModel = viewModel()
        viewModel.type(pin = PIN)

        viewModel.onPrimaryAction()

        assertEquals(LoginEvent.SignedIn, viewModel.events.first())
        assertEquals("signIn:$PIN", vault.calls.last())
    }

    @Test
    fun aWrongPinIsClearedAndTheAttemptsLeftAreShown() {
        vault.state = configured()
        vault.signInResult = SignInResult.InvalidPin(attemptsBeforeLockout = 3)
        val viewModel = viewModel()
        viewModel.type(pin = PIN)

        viewModel.onPrimaryAction()

        assertEquals("", viewModel.state.value.pin)
        assertEquals(message(R.string.invalid_pin_attempts_left, 3), viewModel.state.value.feedback)
    }

    @Test
    fun aLockoutShowsTheSecondsLeftRoundedUp() {
        vault.state = configured()
        vault.signInResult = SignInResult.LockedOut(remainingMillis = 1_001)
        val viewModel = viewModel()
        viewModel.type(pin = PIN)

        viewModel.onPrimaryAction()

        assertEquals("", viewModel.state.value.pin)
        assertEquals(message(R.string.pin_locked_out, 2L), viewModel.state.value.feedback)
        assertEquals(message(R.string.pin_locked_out, 1L), LoginViewModel.lockoutMessage(0))
        assertEquals(message(R.string.pin_locked_out, 30L), LoginViewModel.lockoutMessage(30_000))
    }

    /** Taps on any control, including the biometric and wipe ones that don't check `signInEnabled`. */
    @Test
    fun aSecondTapIsIgnoredWhileTheVaultIsWorking() {
        vault.state = configured()
        val gate = CompletableDeferred<Unit>().also { vault.gate = it }
        val viewModel = viewModel()
        viewModel.type(pin = PIN)

        viewModel.onPrimaryAction()
        viewModel.onPrimaryAction()
        viewModel.onForgotPin()
        viewModel.onRequestWipeData()
        viewModel.onConfirmWipeData()
        viewModel.onBiometricLogin()
        viewModel.onBiometricAuthenticated(Cipher.getInstance("AES/GCM/NoPadding"))

        assertFalse(viewModel.state.value.signInEnabled)
        assertEquals(LoginScreenMode.SIGN_IN, viewModel.state.value.mode)
        assertFalse(viewModel.state.value.showWipeConfirmation)
        gate.complete(Unit)
        assertEquals(listOf("state", "signIn:$PIN"), vault.calls)
        assertTrue(viewModel.state.value.signInEnabled)
    }

    @Test
    fun aNewProfileGetsTheRegionCurrencyAndOnboardingThenAsksForThePin() {
        val viewModel = viewModel()
        viewModel.type(PIN, PIN, QUESTION, ANSWER)

        viewModel.onPrimaryAction()

        assertEquals("setUp:$PIN,$PIN,$QUESTION,$ANSWER", vault.calls.first { it.startsWith("setUp") })
        assertEquals(1, currency.resets)
        assertTrue(onboarding.tourPending.value)
        assertEquals(LoginScreenMode.SIGN_IN, viewModel.state.value.mode)
        assertEquals(listOf("", "", "", ""), viewModel.inputs())
        assertEquals(message(R.string.profile_setup_success), viewModel.state.value.feedback)
    }

    @Test
    fun aRejectedSetupKeepsTheInputsAndSaysWhy() {
        vault.setUpResult = SetupProfileResult.SECURITY_ANSWER_TOO_SHORT
        val viewModel = viewModel()
        viewModel.type(PIN, PIN, QUESTION, "short")

        viewModel.onPrimaryAction()

        assertEquals(message(R.string.security_answer_too_short, SecurityProfileService.MIN_ANSWER_LENGTH), viewModel.state.value.feedback)
        assertEquals(listOf(PIN, PIN, QUESTION, "short"), viewModel.inputs())
        assertEquals(LoginScreenMode.SETUP, viewModel.state.value.mode)
        assertEquals(0, currency.resets)
        assertFalse(onboarding.tourPending.value)
    }

    @Test
    fun forgotPinClearsTheInputsAndOpensRecovery() {
        vault.state = configured()
        val viewModel = viewModel()
        viewModel.type(pin = PIN)

        viewModel.onForgotPin()

        assertEquals(LoginScreenMode.RECOVER_PIN, viewModel.state.value.mode)
        assertEquals(listOf("", "", "", ""), viewModel.inputs())
    }

    @Test
    fun aRecoveredPinReturnsToSignIn() {
        vault.state = configured()
        val viewModel = viewModel().apply { onForgotPin() }
        viewModel.type(pin = NEW_PIN, confirmation = NEW_PIN, answer = ANSWER)

        viewModel.onPrimaryAction()

        assertEquals("recoverPin:$ANSWER,$NEW_PIN,$NEW_PIN", vault.calls.last())
        assertEquals(LoginScreenMode.SIGN_IN, viewModel.state.value.mode)
        assertEquals(listOf("", "", "", ""), viewModel.inputs())
        assertEquals(message(R.string.pin_recovery_success), viewModel.state.value.feedback)
    }

    @Test
    fun aFailedRecoveryStaysAndSaysWhy() {
        vault.state = configured()
        vault.recoveryResult = RecoveryResult.Finished(RecoverPinResult.INVALID_SECURITY_ANSWER)
        val viewModel = viewModel().apply { onForgotPin() }
        viewModel.type(pin = NEW_PIN, confirmation = NEW_PIN, answer = "wrong")

        viewModel.onPrimaryAction()
        assertEquals(LoginScreenMode.RECOVER_PIN, viewModel.state.value.mode)
        assertEquals(message(R.string.invalid_security_answer), viewModel.state.value.feedback)
        assertEquals(listOf(NEW_PIN, NEW_PIN, "", "wrong"), viewModel.inputs())

        vault.recoveryResult = RecoveryResult.Finished(RecoverPinResult.LOCKED_OUT, lockoutRemainingMillis = 5_000)
        viewModel.onPrimaryAction()
        assertEquals(message(R.string.pin_locked_out, 5L), viewModel.state.value.feedback)
    }

    @Test
    fun aV1ProfileThatAcceptsThePinOrAnswerMovesToTheUpgradeStep() {
        vault.state = configured()
        vault.signInResult = SignInResult.UpgradeRequired
        val viewModel = viewModel()
        viewModel.type(pin = PIN)
        viewModel.onPrimaryAction()
        assertEquals(LoginScreenMode.UPGRADE_RECOVERY, viewModel.state.value.mode)
        assertEquals(listOf("", "", "", ""), viewModel.inputs())

        vault.recoveryResult = RecoveryResult.UpgradeRequired
        val recovering = viewModel().apply { onForgotPin() }
        recovering.type(pin = NEW_PIN, confirmation = NEW_PIN, answer = ANSWER)
        recovering.onPrimaryAction()
        assertEquals(LoginScreenMode.UPGRADE_RECOVERY, recovering.state.value.mode)
    }

    @Test
    fun finishingTheUpgradeSignsIn() = runTest {
        vault.state = configured(upgradeStarted = true)
        val viewModel = viewModel()
        viewModel.type(question = QUESTION, answer = ANSWER)

        viewModel.onPrimaryAction()

        assertEquals("completeUpgrade:$QUESTION,$ANSWER", vault.calls.last())
        assertEquals(LoginEvent.SignedIn, viewModel.events.first())
    }

    @Test
    fun aRejectedUpgradeSaysWhy() {
        vault.state = configured(upgradeStarted = true)
        vault.upgradeResult = SetupProfileResult.SECURITY_QUESTION_REQUIRED
        val viewModel = viewModel()

        viewModel.onPrimaryAction()

        assertEquals(message(R.string.security_question_required), viewModel.state.value.feedback)
        assertEquals(LoginScreenMode.UPGRADE_RECOVERY, viewModel.state.value.mode)
    }

    @Test
    fun leavingTheUpgradeStepLocksTheVaultButLeavingRecoveryDoesNot() {
        vault.state = configured()
        val recovering = viewModel().apply { onForgotPin() }
        recovering.onBackToSignIn()
        assertEquals(0, vault.locks)
        assertEquals(LoginScreenMode.SIGN_IN, recovering.state.value.mode)

        vault.state = configured(upgradeStarted = true)
        val upgrading = viewModel()
        upgrading.type(question = QUESTION)
        upgrading.onBackToSignIn()
        assertEquals(1, vault.locks)
        assertEquals(LoginScreenMode.SIGN_IN, upgrading.state.value.mode)
        assertEquals(listOf("", "", "", ""), upgrading.inputs())
    }

    @Test
    fun anUnreadableProfileShowsTheStorageErrorAndRetryReadsItAgain() {
        vault.stateFailure = SecureStorageException("Security profile is corrupt")
        val viewModel = viewModel()
        assertEquals(LoginScreenMode.STORAGE_ERROR, viewModel.state.value.mode)
        assertNull(viewModel.state.value.feedback)
        assertFalse(viewModel.state.value.biometricAvailable)

        vault.stateFailure = null
        vault.state = configured()
        viewModel.onPrimaryAction()
        assertEquals(LoginScreenMode.SIGN_IN, viewModel.state.value.mode)
    }

    @Test
    fun anyOtherFailureShowsTheGenericErrorAndReenablesInput() {
        vault.state = configured()
        vault.signInFailure = IllegalStateException("database closed")
        val viewModel = viewModel()
        viewModel.type(pin = PIN)

        viewModel.onPrimaryAction()

        assertEquals(message(R.string.generic_error), viewModel.state.value.feedback)
        assertEquals(LoginScreenMode.SIGN_IN, viewModel.state.value.mode)
        assertTrue(viewModel.state.value.signInEnabled)
    }

    @Test
    fun theWipeDialogOpensAndCloses() {
        vault.state = configured()
        val viewModel = viewModel()

        viewModel.onRequestWipeData()
        assertTrue(viewModel.state.value.showWipeConfirmation)
        viewModel.onCancelWipeData()
        assertFalse(viewModel.state.value.showWipeConfirmation)
        assertTrue(vault.calls.none { it == "wipe" })
    }

    @Test
    fun aWipeEndsTheSessionAndStartsSetupAgain() {
        vault.state = configured()
        sessionManager.markAuthenticated(now = 0L)
        val viewModel = viewModel()
        viewModel.type(pin = PIN)

        viewModel.onRequestWipeData()
        viewModel.onConfirmWipeData()

        assertTrue(sessionManager.isSessionTimedOut(now = 1L))
        assertFalse(viewModel.state.value.showWipeConfirmation)
        assertEquals(LoginScreenMode.SETUP, viewModel.state.value.mode)
        assertEquals(listOf("", "", "", ""), viewModel.inputs())
        assertEquals(message(R.string.wipe_data_success), viewModel.state.value.feedback)
    }

    @Test
    fun aFailedWipeSaysSoAndStaysOnTheSameStep() {
        vault.state = configured()
        vault.wipeResult = false
        val viewModel = viewModel()
        val stateReads = vault.calls.count { it == "state" }

        viewModel.onConfirmWipeData()

        assertEquals(stateReads, vault.calls.count { it == "state" })
        assertEquals(LoginScreenMode.SIGN_IN, viewModel.state.value.mode)
        assertEquals(message(R.string.wipe_data_error), viewModel.state.value.feedback)
    }

    @Test
    fun biometricSignInShowsThePromptWithTheVaultsCipher() = runTest {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        vault.state = configured(biometricEnrolled = true)
        vault.biometricSignIn = BiometricSignIn.Ready(cipher)
        val viewModel = viewModel()

        viewModel.onBiometricLogin()

        assertSame(cipher, (viewModel.events.first() as LoginEvent.ShowBiometricPrompt).cipher)
        assertTrue(viewModel.state.value.signInEnabled)
    }

    @Test
    fun biometricSignInTheVaultCannotDoAnyMoreIsTurnedOff() {
        vault.state = configured(biometricEnrolled = true)
        val viewModel = viewModel()

        vault.biometricSignIn = BiometricSignIn.NotEnrolled
        viewModel.onBiometricLogin()
        assertFalse(viewModel.state.value.biometricAvailable)
        assertNull(viewModel.state.value.feedback)

        vault.biometricSignIn = BiometricSignIn.Invalidated
        viewModel.onBiometricLogin()
        assertEquals(message(R.string.biometric_sign_in_invalidated), viewModel.state.value.feedback)
    }

    @Test
    fun theAuthorizedCipherSignsIn() = runTest {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        vault.state = configured(biometricEnrolled = true)
        val viewModel = viewModel()

        viewModel.onBiometricAuthenticated(cipher)

        assertSame(cipher, vault.biometricCipher)
        assertEquals(LoginEvent.SignedIn, viewModel.events.first())
    }

    @Test
    fun aCipherThatNoLongerOpensTheVaultTurnsBiometricSignInOff() {
        vault.state = configured(biometricEnrolled = true)
        vault.biometricResult = SignInResult.BiometricFailed
        val viewModel = viewModel()

        viewModel.onBiometricAuthenticated(Cipher.getInstance("AES/GCM/NoPadding"))

        assertFalse(viewModel.state.value.biometricAvailable)
        assertEquals(message(R.string.biometric_sign_in_invalidated), viewModel.state.value.feedback)
    }

    private fun configured(biometricEnrolled: Boolean = false, upgradeStarted: Boolean = false) =
        VaultState.Configured(QUESTION, biometricEnrolled, upgradeStarted)

    /** Answers from its fields and records each call; [gate], when set, holds sign-in until it completes. */
    private class FakeSignInVault : SignInVault {
        var state: VaultState = VaultState.NeedsSetup
        var stateFailure: Throwable? = null
        var signInResult: SignInResult = SignInResult.Unlocked
        var signInFailure: Throwable? = null
        var setUpResult = SetupProfileResult.SUCCESS
        var recoveryResult: RecoveryResult = RecoveryResult.Finished(RecoverPinResult.SUCCESS)
        var upgradeResult = SetupProfileResult.SUCCESS
        var biometricSignIn: BiometricSignIn = BiometricSignIn.NotEnrolled
        var biometricResult: SignInResult = SignInResult.Unlocked
        var biometricCipher: Cipher? = null
        var wipeResult = true
        var gate: CompletableDeferred<Unit>? = null
        var locks = 0
        val calls = mutableListOf<String>()

        override suspend fun state(): VaultState {
            calls += "state"
            stateFailure?.let { throw it }
            return state
        }

        override suspend fun setUp(pin: String, pinConfirmation: String, question: String, answer: String): SetupProfileResult {
            calls += "setUp:$pin,$pinConfirmation,$question,$answer"
            if (setUpResult == SetupProfileResult.SUCCESS) state = VaultState.Configured(question, false, false)
            return setUpResult
        }

        override suspend fun signIn(pin: String): SignInResult {
            calls += "signIn:$pin"
            gate?.await()
            signInFailure?.let { throw it }
            return signInResult
        }

        override suspend fun recoverPin(answer: String, newPin: String, pinConfirmation: String): RecoveryResult {
            calls += "recoverPin:$answer,$newPin,$pinConfirmation"
            return recoveryResult
        }

        override suspend fun completeUpgrade(question: String, answer: String): SetupProfileResult {
            calls += "completeUpgrade:$question,$answer"
            return upgradeResult
        }

        override suspend fun prepareBiometricSignIn(): BiometricSignIn {
            calls += "prepareBiometricSignIn"
            return biometricSignIn
        }

        override suspend fun signInWithBiometric(cipher: Cipher): SignInResult {
            calls += "signInWithBiometric"
            biometricCipher = cipher
            return biometricResult
        }

        override fun lock() {
            locks++
        }

        override suspend fun wipe(): Boolean {
            calls += "wipe"
            if (wipeResult) state = VaultState.NeedsSetup
            return wipeResult
        }
    }

    private class FakeBiometricCapability(var available: Boolean = true) : BiometricCapability {
        override fun canAuthenticate(): Boolean = available
    }

    private class FakeCurrencySettings : CurrencySettings {
        var resets = 0

        override fun resetToRegionDefault() {
            resets++
        }
    }

    private companion object {
        const val PIN = "135790"
        const val NEW_PIN = "246801"
        const val QUESTION = "Where did you first work?"
        const val ANSWER = "Lincoln Elementary"
    }
}
