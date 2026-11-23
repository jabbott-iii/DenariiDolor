/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.auth

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denariidolor.MainActivity
import com.denariidolor.R
import com.denariidolor.appGraph
import com.denariidolor.data.export.ReportExporter
import com.denariidolor.data.local.preferences.ThemePreferences
import com.denariidolor.presentation.ui.LoginScreen
import com.denariidolor.presentation.ui.common.setThemedContent
import com.denariidolor.util.SessionManager
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import javax.crypto.Cipher
import kotlinx.coroutines.launch

/**
 * The exported launcher. Its screen state and vault calls live in [LoginViewModel]; the activity keeps what needs a window:
 * the overlay guard (CS-11), clearing share copies (CS-04), the biometric prompt (CS-07) and opening [MainActivity].
 */
class LoginActivity : AppCompatActivity() {
    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var viewModelFactory: MetroViewModelFactory

    private val viewModel: LoginViewModel by viewModels()

    // `by viewModels()` creates LoginViewModel through the app graph.
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory get() = viewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        appGraph.inject(this)
        super.onCreate(savedInstanceState)
        window.guardAgainstOverlays()
        // Cold start or return after sign-out: no unencrypted share copies should outlive a session.
        ReportExporter.clearShareCache(this)
        setThemedContent(themePreferences) {
            val state by viewModel.state.collectAsStateWithLifecycle()
            LoginScreen(
                mode = state.mode,
                pin = state.pin,
                pinConfirmation = state.pinConfirmation,
                securityQuestion = state.securityQuestion,
                securityAnswer = state.securityAnswer,
                recoveryQuestion = state.recoveryQuestion,
                signInEnabled = state.signInEnabled,
                biometricAvailable = state.biometricAvailable,
                showWipeConfirmation = state.showWipeConfirmation,
                feedbackMessage = state.feedback?.resolve(this),
                onPinChange = { viewModel.onInputChange(LoginField.PIN, it) },
                onPinConfirmationChange = { viewModel.onInputChange(LoginField.PIN_CONFIRMATION, it) },
                onSecurityQuestionChange = { viewModel.onInputChange(LoginField.SECURITY_QUESTION, it) },
                onSecurityAnswerChange = { viewModel.onInputChange(LoginField.SECURITY_ANSWER, it) },
                onPrimaryAction = viewModel::onPrimaryAction,
                onForgotPin = viewModel::onForgotPin,
                onBackToSignIn = viewModel::onBackToSignIn,
                onBiometricLogin = viewModel::onBiometricLogin,
                onRequestWipeData = viewModel::onRequestWipeData,
                onCancelWipeData = viewModel::onCancelWipeData,
                onConfirmWipeData = viewModel::onConfirmWipeData
            )
        }
        // Only while started, so the app is never opened from the background.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        LoginEvent.SignedIn -> openMain()
                        is LoginEvent.ShowBiometricPrompt -> showBiometricPrompt(event.cipher)
                    }
                }
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
            onAuthenticated = viewModel::onBiometricAuthenticated,
            onError = { canceled -> if (!canceled) toast(R.string.biometric_sign_in_error) },
            onFailedAttempt = { toast(R.string.biometric_sign_in_failed) }
        )
    }

    private fun toast(@StringRes message: Int) = Toast.makeText(this, getString(message), Toast.LENGTH_SHORT).show()

    private fun openMain() {
        sessionManager.markAuthenticated()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
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
