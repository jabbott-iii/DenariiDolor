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

package com.denariidolor.presentation.ui.auth

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.denariidolor.MainActivity
import com.denariidolor.R
import com.denariidolor.data.export.ReportExporter
import com.denariidolor.data.local.preferences.CurrencyPreferences
import com.denariidolor.data.local.preferences.RecoverPinResult
import com.denariidolor.data.local.preferences.SecureStorageException
import com.denariidolor.data.local.preferences.SecurityProfileService
import com.denariidolor.data.local.preferences.SetupProfileResult
import com.denariidolor.data.local.preferences.ThemePreferences
import com.denariidolor.data.local.vault.BiometricSignIn
import com.denariidolor.data.local.vault.RecoveryResult
import com.denariidolor.data.local.vault.SignInResult
import com.denariidolor.data.local.vault.Vault
import com.denariidolor.data.local.vault.VaultState
import com.denariidolor.presentation.ui.LoginScreen
import com.denariidolor.presentation.ui.LoginScreenMode
import com.denariidolor.presentation.ui.common.setThemedContent
import com.denariidolor.util.SessionManager
import com.denariidolor.util.runSuspendCatching
import dagger.hilt.android.AndroidEntryPoint
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    @Inject
    lateinit var vault: Vault

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var currencyPreferences: CurrencyPreferences

    private var busy = false
    private var signInEnabled by mutableStateOf(false)
    private var biometricAvailable by mutableStateOf(false)
    private var loginMenuMode by mutableStateOf(LoginScreenMode.SIGN_IN)
    private var showWipeConfirmation by mutableStateOf(false)
    private var feedbackMessage by mutableStateOf<String?>(null)
    private var recoveryQuestion by mutableStateOf<String?>(null)

    private var pin by mutableStateOf("")
    private var pinConfirmation by mutableStateOf("")
    private var securityQuestion by mutableStateOf("")
    private var securityAnswer by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.guardAgainstOverlays()
        // Cold start or return after sign-out: no unencrypted share copies should outlive a session.
        ReportExporter.clearShareCache(this)
        setThemedContent(themePreferences) {
            LoginScreen(
                mode = loginMenuMode,
                pin = pin,
                pinConfirmation = pinConfirmation,
                securityQuestion = securityQuestion,
                securityAnswer = securityAnswer,
                recoveryQuestion = recoveryQuestion,
                signInEnabled = signInEnabled,
                biometricAvailable = biometricAvailable,
                showWipeConfirmation = showWipeConfirmation,
                feedbackMessage = feedbackMessage,
                onPinChange = { pin = it },
                onPinConfirmationChange = { pinConfirmation = it },
                onSecurityQuestionChange = { securityQuestion = it },
                onSecurityAnswerChange = { securityAnswer = it },
                onPrimaryAction = ::handlePrimaryAction,
                onForgotPin = ::startPinRecovery,
                onBackToSignIn = ::switchToSignIn,
                onBiometricLogin = ::promptForBiometricSignIn,
                onRequestWipeData = { if (signInEnabled) showWipeConfirmation = true },
                onCancelWipeData = { showWipeConfirmation = false },
                onConfirmWipeData = ::wipeAllUserData
            )
        }
        runVaultAction { showState(vault.state()) }
    }

    /**
     * Runs a vault operation (key derivation, Keystore, disk) off the main thread (BUG-04). Inputs stay disabled until
     * it finishes, so operations never overlap.
     */
    private fun runVaultAction(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        signInEnabled = false
        lifecycleScope.launch {
            val result = runSuspendCatching { action() }
            busy = false
            signInEnabled = true
            result.exceptionOrNull()?.let(::showFailure)
        }
    }

    private fun showFailure(error: Throwable) {
        if (error is SecureStorageException) {
            clearInputs()
            biometricAvailable = false
            feedbackMessage = null
            loginMenuMode = LoginScreenMode.STORAGE_ERROR
        } else {
            feedbackMessage = getString(R.string.generic_error)
        }
    }

    private fun showState(state: VaultState) {
        when (state) {
            VaultState.NeedsSetup -> {
                loginMenuMode = LoginScreenMode.SETUP
                recoveryQuestion = null
                biometricAvailable = false
            }
            is VaultState.Configured -> {
                loginMenuMode = if (state.upgradeStarted) LoginScreenMode.UPGRADE_RECOVERY else LoginScreenMode.SIGN_IN
                recoveryQuestion = state.securityQuestion
                biometricAvailable = state.biometricEnrolled && biometricAuthManager.canAuthenticate(this)
            }
        }
    }

    private fun handlePrimaryAction() {
        if (!signInEnabled) return
        when (loginMenuMode) {
            LoginScreenMode.SIGN_IN -> handlePinLogin()
            LoginScreenMode.SETUP -> handleSetupProfile()
            LoginScreenMode.RECOVER_PIN -> handleRecoverPin()
            LoginScreenMode.UPGRADE_RECOVERY -> handleCompleteUpgrade()
            LoginScreenMode.STORAGE_ERROR -> runVaultAction {
                feedbackMessage = null
                showState(vault.state())
            }
        }
    }

    private fun handlePinLogin() {
        if (!SecurityProfileService.isValidPin(pin)) {
            feedbackMessage = getString(R.string.pin_format_error)
            return
        }
        val enteredPin = pin
        runVaultAction { showSignInResult(vault.signIn(enteredPin)) }
    }

    private fun showSignInResult(result: SignInResult) {
        when (result) {
            SignInResult.Unlocked -> openMain()
            SignInResult.UpgradeRequired -> showUpgradeStep()
            is SignInResult.InvalidPin -> {
                pin = ""
                feedbackMessage = getString(R.string.invalid_pin_attempts_left, result.attemptsBeforeLockout)
            }
            is SignInResult.LockedOut -> {
                pin = ""
                feedbackMessage = lockoutMessage(result.remainingMillis)
            }
            SignInResult.BiometricFailed -> {
                biometricAvailable = false
                feedbackMessage = getString(R.string.biometric_sign_in_invalidated)
            }
        }
    }

    private fun handleSetupProfile() {
        val enteredPin = pin
        val confirmation = pinConfirmation
        val question = securityQuestion
        val answer = securityAnswer
        runVaultAction {
            val result = vault.setUp(enteredPin, confirmation, question, answer)
            if (result == SetupProfileResult.SUCCESS) {
                // A new profile starts with the currency of the device's region; Settings can change it.
                currencyPreferences.resetToRegionDefault()
                clearInputs()
                showState(vault.state())
            }
            feedbackMessage = setupMessage(result)
        }
    }

    private fun startPinRecovery() {
        if (!signInEnabled) return
        clearInputs()
        feedbackMessage = null
        loginMenuMode = LoginScreenMode.RECOVER_PIN
    }

    private fun switchToSignIn() {
        if (!signInEnabled) return
        // Leaving the upgrade step forgets the v1 PIN and database key it was holding.
        if (loginMenuMode == LoginScreenMode.UPGRADE_RECOVERY) vault.lock()
        clearInputs()
        feedbackMessage = null
        loginMenuMode = LoginScreenMode.SIGN_IN
    }

    private fun handleRecoverPin() {
        val answer = securityAnswer
        val newPin = pin
        val confirmation = pinConfirmation
        runVaultAction {
            when (val outcome = vault.recoverPin(answer, newPin, confirmation)) {
                RecoveryResult.UpgradeRequired -> showUpgradeStep()
                is RecoveryResult.Finished -> {
                    if (outcome.result == RecoverPinResult.SUCCESS) {
                        clearInputs()
                        loginMenuMode = LoginScreenMode.SIGN_IN
                    }
                    feedbackMessage = recoveryMessage(outcome)
                }
            }
        }
    }

    private fun handleCompleteUpgrade() {
        val question = securityQuestion
        val answer = securityAnswer
        runVaultAction {
            val result = vault.completeUpgrade(question, answer)
            if (result == SetupProfileResult.SUCCESS) openMain() else feedbackMessage = setupMessage(result)
        }
    }

    private fun showUpgradeStep() {
        clearInputs()
        feedbackMessage = null
        loginMenuMode = LoginScreenMode.UPGRADE_RECOVERY
    }

    private fun wipeAllUserData() {
        showWipeConfirmation = false
        runVaultAction {
            val wiped = vault.wipe()
            sessionManager.invalidate()
            clearInputs()
            if (wiped) showState(vault.state())
            feedbackMessage = getString(if (wiped) R.string.wipe_data_success else R.string.wipe_data_error)
        }
    }

    private fun promptForBiometricSignIn() = runVaultAction {
        when (val prepared = vault.prepareBiometricSignIn()) {
            is BiometricSignIn.Ready -> showBiometricPrompt(prepared.cipher)
            BiometricSignIn.NotEnrolled -> biometricAvailable = false
            BiometricSignIn.Invalidated -> {
                biometricAvailable = false
                feedbackMessage = getString(R.string.biometric_sign_in_invalidated)
            }
        }
    }

    private fun showBiometricPrompt(cipher: Cipher) {
        biometricAuthManager.authenticate(
            activity = this,
            cipher = cipher,
            text = BiometricAuthManager.PromptText(
                title = getString(R.string.biometric_sign_in_title),
                subtitle = getString(R.string.biometric_sign_in_subtitle),
                negativeButton = getString(R.string.use_pin_instead)
            ),
            onAuthenticated = { authorized -> runVaultAction { showSignInResult(vault.signInWithBiometric(authorized)) } },
            onError = { canceled -> if (!canceled) toast(R.string.biometric_sign_in_error) },
            onFailedAttempt = { toast(R.string.biometric_sign_in_failed) }
        )
    }

    private fun setupMessage(result: SetupProfileResult): String = when (result) {
        SetupProfileResult.SUCCESS -> getString(R.string.profile_setup_success)
        SetupProfileResult.ALREADY_CONFIGURED -> getString(R.string.profile_already_configured)
        SetupProfileResult.INVALID_PIN_FORMAT -> getString(R.string.pin_format_error)
        SetupProfileResult.PIN_MISMATCH -> getString(R.string.pin_confirmation_mismatch)
        SetupProfileResult.SECURITY_QUESTION_REQUIRED -> getString(R.string.security_question_required)
        SetupProfileResult.SECURITY_QUESTION_TOO_LONG ->
            getString(R.string.security_question_too_long, SecurityProfileService.MAX_QUESTION_LENGTH)
        SetupProfileResult.SECURITY_ANSWER_REQUIRED -> getString(R.string.security_answer_required)
        SetupProfileResult.SECURITY_ANSWER_TOO_SHORT ->
            getString(R.string.security_answer_too_short, SecurityProfileService.MIN_ANSWER_LENGTH)
        SetupProfileResult.SECURITY_ANSWER_TOO_LONG ->
            getString(R.string.security_answer_too_long, SecurityProfileService.MAX_ANSWER_LENGTH)
        SetupProfileResult.SECURITY_ANSWER_IN_QUESTION -> getString(R.string.security_answer_in_question)
        SetupProfileResult.SECURITY_ANSWER_MATCHES_PIN -> getString(R.string.security_answer_matches_pin)
    }

    private fun recoveryMessage(outcome: RecoveryResult.Finished): String = when (outcome.result) {
        RecoverPinResult.SUCCESS -> getString(R.string.pin_recovery_success)
        RecoverPinResult.PROFILE_NOT_CONFIGURED -> getString(R.string.profile_not_configured)
        RecoverPinResult.SECURITY_ANSWER_REQUIRED -> getString(R.string.security_answer_required)
        RecoverPinResult.INVALID_SECURITY_ANSWER -> getString(R.string.invalid_security_answer)
        RecoverPinResult.INVALID_PIN_FORMAT -> getString(R.string.pin_format_error)
        RecoverPinResult.PIN_MISMATCH -> getString(R.string.pin_confirmation_mismatch)
        RecoverPinResult.LOCKED_OUT -> lockoutMessage(outcome.lockoutRemainingMillis)
    }

    private fun clearInputs() {
        pin = ""
        pinConfirmation = ""
        securityQuestion = ""
        securityAnswer = ""
    }

    private fun toast(@StringRes message: Int) = Toast.makeText(this, getString(message), Toast.LENGTH_SHORT).show()

    private fun lockoutMessage(remainingMillis: Long): String {
        val seconds = ((remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).coerceAtLeast(1)
        return getString(R.string.pin_locked_out, seconds)
    }

    private fun openMain() {
        sessionManager.markAuthenticated()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}

/**
 * Tapjacking (CS-11): another app's overlay must not trick taps onto the PIN or wipe controls. API 31+ hides such overlays
 * while this window is shown; older versions drop touches that pass through one instead.
 */
internal fun Window.guardAgainstOverlays() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setHideOverlayWindows(true)
    } else {
        decorView.filterTouchesWhenObscured = true
    }
}
