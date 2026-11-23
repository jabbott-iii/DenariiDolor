/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.R
import com.denariidolor.data.local.preferences.CurrencySettings
import com.denariidolor.data.local.preferences.OnboardingStore
import com.denariidolor.data.local.preferences.RecoverPinResult
import com.denariidolor.data.local.preferences.SecureStorageException
import com.denariidolor.data.local.preferences.SecurityProfileService
import com.denariidolor.data.local.preferences.SetupProfileResult
import com.denariidolor.data.local.vault.BiometricSignIn
import com.denariidolor.data.local.vault.PlatformCipher
import com.denariidolor.data.local.vault.RecoveryResult
import com.denariidolor.data.local.vault.SignInResult
import com.denariidolor.data.local.vault.SignInVault
import com.denariidolor.data.local.vault.VaultState
import com.denariidolor.presentation.ui.LoginScreenMode
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.util.SessionManager
import com.denariidolor.util.runSuspendCatching
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The sign-in screen. The typed PIN and answer live only here, in memory: they survive rotation, but never go into
 * `SavedStateHandle` or anything else that reaches disk. [signInEnabled] is false while a vault call runs.
 */
data class LoginUiState(
    val mode: LoginScreenMode = LoginScreenMode.SIGN_IN,
    val pin: String = "",
    val pinConfirmation: String = "",
    val securityQuestion: String = "",
    val securityAnswer: String = "",
    val recoveryQuestion: String? = null,
    val signInEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val showWipeConfirmation: Boolean = false,
    val feedback: UiMessage? = null
)

/** The text fields of the sign-in screen. */
enum class LoginField {
    PIN,
    PIN_CONFIRMATION,
    SECURITY_QUESTION,
    SECURITY_ANSWER
}

/** What only the activity can do: open the app once the vault is unlocked, and show the system biometric prompt (CS-07). */
sealed interface LoginEvent {
    data object SignedIn : LoginEvent

    class ShowBiometricPrompt(val cipher: PlatformCipher) : LoginEvent
}

/** Setup, sign-in, PIN recovery, the v1 upgrade and wipe, run through the [SignInVault] one call at a time. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class LoginViewModel(
    private val vault: SignInVault,
    private val biometrics: BiometricCapability,
    private val sessionManager: SessionManager,
    private val currencySettings: CurrencySettings,
    private val onboardingStore: OnboardingStore
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    private var busy = false

    init {
        runVaultAction { showState(vault.state()) }
    }

    fun onInputChange(field: LoginField, value: String) = _state.update {
        when (field) {
            LoginField.PIN -> it.copy(pin = value)
            LoginField.PIN_CONFIRMATION -> it.copy(pinConfirmation = value)
            LoginField.SECURITY_QUESTION -> it.copy(securityQuestion = value)
            LoginField.SECURITY_ANSWER -> it.copy(securityAnswer = value)
        }
    }

    fun onPrimaryAction() {
        val current = _state.value
        if (!current.signInEnabled) return
        when (current.mode) {
            LoginScreenMode.SIGN_IN -> signIn(current.pin)
            LoginScreenMode.SETUP -> setUp(current)
            LoginScreenMode.RECOVER_PIN -> recoverPin(current)
            LoginScreenMode.UPGRADE_RECOVERY -> completeUpgrade(current)
            LoginScreenMode.STORAGE_ERROR -> runVaultAction {
                _state.update { it.copy(feedback = null) }
                showState(vault.state())
            }
        }
    }

    fun onForgotPin() {
        if (!_state.value.signInEnabled) return
        _state.update { it.clearedInputs().copy(feedback = null, mode = LoginScreenMode.RECOVER_PIN) }
    }

    fun onBackToSignIn() {
        val current = _state.value
        if (!current.signInEnabled) return
        // Leaving the upgrade step forgets the v1 PIN and database key it was holding.
        if (current.mode == LoginScreenMode.UPGRADE_RECOVERY) vault.lock()
        _state.update { it.clearedInputs().copy(feedback = null, mode = LoginScreenMode.SIGN_IN) }
    }

    fun onBiometricLogin() = runVaultAction {
        when (val prepared = vault.prepareBiometricSignIn()) {
            is BiometricSignIn.Ready -> _events.send(LoginEvent.ShowBiometricPrompt(prepared.cipher))
            BiometricSignIn.NotEnrolled -> _state.update { it.copy(biometricAvailable = false) }
            BiometricSignIn.Invalidated -> _state.update {
                it.copy(biometricAvailable = false, feedback = UiMessage.Resource(R.string.biometric_sign_in_invalidated))
            }
        }
    }

    /** The cipher `BiometricPrompt` authorized: only it can unwrap the database key (CS-07). */
    fun onBiometricAuthenticated(cipher: PlatformCipher) = runVaultAction { showSignInResult(vault.signInWithBiometric(cipher)) }

    fun onRequestWipeData() {
        if (_state.value.signInEnabled) _state.update { it.copy(showWipeConfirmation = true) }
    }

    fun onCancelWipeData() = _state.update { it.copy(showWipeConfirmation = false) }

    fun onConfirmWipeData() {
        _state.update { it.copy(showWipeConfirmation = false) }
        runVaultAction {
            val wiped = vault.wipe()
            sessionManager.invalidate()
            _state.update { it.clearedInputs() }
            if (wiped) showState(vault.state())
            _state.update { it.copy(feedback = UiMessage.Resource(if (wiped) R.string.wipe_data_success else R.string.wipe_data_error)) }
        }
    }

    /**
     * Runs a vault operation (key derivation, Keystore, disk) off the main thread (BUG-04). Inputs stay disabled until it
     * finishes, so operations never overlap.
     */
    private fun runVaultAction(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        _state.update { it.copy(signInEnabled = false) }
        viewModelScope.launch {
            val result = runSuspendCatching { action() }
            busy = false
            _state.update { it.copy(signInEnabled = true) }
            result.exceptionOrNull()?.let(::showFailure)
        }
    }

    private fun showFailure(error: Throwable) {
        if (error is SecureStorageException) {
            _state.update {
                it.clearedInputs().copy(biometricAvailable = false, feedback = null, mode = LoginScreenMode.STORAGE_ERROR)
            }
        } else {
            _state.update { it.copy(feedback = UiMessage.Resource(R.string.generic_error)) }
        }
    }

    private fun showState(state: VaultState) {
        when (state) {
            VaultState.NeedsSetup -> _state.update {
                it.copy(mode = LoginScreenMode.SETUP, recoveryQuestion = null, biometricAvailable = false)
            }
            is VaultState.Configured -> {
                val biometricAvailable = state.biometricEnrolled && biometrics.canAuthenticate()
                _state.update {
                    it.copy(
                        mode = if (state.upgradeStarted) LoginScreenMode.UPGRADE_RECOVERY else LoginScreenMode.SIGN_IN,
                        recoveryQuestion = state.securityQuestion,
                        biometricAvailable = biometricAvailable
                    )
                }
            }
        }
    }

    private fun signIn(pin: String) {
        if (!SecurityProfileService.isValidPin(pin)) {
            _state.update { it.copy(feedback = UiMessage.Resource(R.string.pin_format_error)) }
            return
        }
        runVaultAction { showSignInResult(vault.signIn(pin)) }
    }

    private suspend fun showSignInResult(result: SignInResult) {
        when (result) {
            SignInResult.Unlocked -> _events.send(LoginEvent.SignedIn)
            SignInResult.UpgradeRequired -> showUpgradeStep()
            is SignInResult.InvalidPin -> _state.update {
                it.copy(pin = "", feedback = UiMessage.Resource(R.string.invalid_pin_attempts_left, result.attemptsBeforeLockout))
            }
            is SignInResult.LockedOut -> _state.update { it.copy(pin = "", feedback = lockoutMessage(result.remainingMillis)) }
            SignInResult.BiometricFailed -> _state.update {
                it.copy(biometricAvailable = false, feedback = UiMessage.Resource(R.string.biometric_sign_in_invalidated))
            }
        }
    }

    private fun setUp(input: LoginUiState) = runVaultAction {
        val result = vault.setUp(input.pin, input.pinConfirmation, input.securityQuestion, input.securityAnswer)
        if (result == SetupProfileResult.SUCCESS) {
            // A new profile starts with the currency of the device's region; Settings can change it.
            currencySettings.resetToRegionDefault()
            // A new profile gets the welcome tour and the getting-started checklist once it's signed in.
            onboardingStore.startOnboarding()
            _state.update { it.clearedInputs() }
            showState(vault.state())
        }
        _state.update { it.copy(feedback = setupMessage(result)) }
    }

    private fun recoverPin(input: LoginUiState) = runVaultAction {
        when (val outcome = vault.recoverPin(input.securityAnswer, input.pin, input.pinConfirmation)) {
            RecoveryResult.UpgradeRequired -> showUpgradeStep()
            is RecoveryResult.Finished -> {
                if (outcome.result == RecoverPinResult.SUCCESS) {
                    _state.update { it.clearedInputs().copy(mode = LoginScreenMode.SIGN_IN) }
                }
                _state.update { it.copy(feedback = recoveryMessage(outcome)) }
            }
        }
    }

    private fun completeUpgrade(input: LoginUiState) = runVaultAction {
        val result = vault.completeUpgrade(input.securityQuestion, input.securityAnswer)
        if (result == SetupProfileResult.SUCCESS) {
            _events.send(LoginEvent.SignedIn)
        } else {
            _state.update { it.copy(feedback = setupMessage(result)) }
        }
    }

    private fun showUpgradeStep() = _state.update {
        it.clearedInputs().copy(feedback = null, mode = LoginScreenMode.UPGRADE_RECOVERY)
    }

    private fun LoginUiState.clearedInputs() = copy(pin = "", pinConfirmation = "", securityQuestion = "", securityAnswer = "")

    companion object {
        private const val MILLIS_PER_SECOND = 1_000L

        /** Whole seconds, rounded up, and at least one, so the message never says to wait 0 seconds. */
        fun lockoutMessage(remainingMillis: Long): UiMessage {
            val seconds = ((remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).coerceAtLeast(1)
            return UiMessage.Resource(R.string.pin_locked_out, seconds)
        }

        fun setupMessage(result: SetupProfileResult): UiMessage = when (result) {
            SetupProfileResult.SUCCESS -> UiMessage.Resource(R.string.profile_setup_success)
            SetupProfileResult.ALREADY_CONFIGURED -> UiMessage.Resource(R.string.profile_already_configured)
            SetupProfileResult.INVALID_PIN_FORMAT -> UiMessage.Resource(R.string.pin_format_error)
            SetupProfileResult.PIN_MISMATCH -> UiMessage.Resource(R.string.pin_confirmation_mismatch)
            SetupProfileResult.SECURITY_QUESTION_REQUIRED -> UiMessage.Resource(R.string.security_question_required)
            SetupProfileResult.SECURITY_QUESTION_TOO_LONG ->
                UiMessage.Resource(R.string.security_question_too_long, SecurityProfileService.MAX_QUESTION_LENGTH)
            SetupProfileResult.SECURITY_ANSWER_REQUIRED -> UiMessage.Resource(R.string.security_answer_required)
            SetupProfileResult.SECURITY_ANSWER_TOO_SHORT ->
                UiMessage.Resource(R.string.security_answer_too_short, SecurityProfileService.MIN_ANSWER_LENGTH)
            SetupProfileResult.SECURITY_ANSWER_TOO_LONG ->
                UiMessage.Resource(R.string.security_answer_too_long, SecurityProfileService.MAX_ANSWER_LENGTH)
            SetupProfileResult.SECURITY_ANSWER_IN_QUESTION -> UiMessage.Resource(R.string.security_answer_in_question)
            SetupProfileResult.SECURITY_ANSWER_MATCHES_PIN -> UiMessage.Resource(R.string.security_answer_matches_pin)
        }

        fun recoveryMessage(outcome: RecoveryResult.Finished): UiMessage = when (outcome.result) {
            RecoverPinResult.SUCCESS -> UiMessage.Resource(R.string.pin_recovery_success)
            RecoverPinResult.PROFILE_NOT_CONFIGURED -> UiMessage.Resource(R.string.profile_not_configured)
            RecoverPinResult.SECURITY_ANSWER_REQUIRED -> UiMessage.Resource(R.string.security_answer_required)
            RecoverPinResult.INVALID_SECURITY_ANSWER -> UiMessage.Resource(R.string.invalid_security_answer)
            RecoverPinResult.INVALID_PIN_FORMAT -> UiMessage.Resource(R.string.pin_format_error)
            RecoverPinResult.PIN_MISMATCH -> UiMessage.Resource(R.string.pin_confirmation_mismatch)
            RecoverPinResult.LOCKED_OUT -> lockoutMessage(outcome.lockoutRemainingMillis)
        }
    }
}
