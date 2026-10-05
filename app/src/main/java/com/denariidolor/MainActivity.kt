/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denariidolor.data.export.ReportExporter
import com.denariidolor.data.local.preferences.CurrencyPreferences
import com.denariidolor.data.local.preferences.ThemePreferences
import com.denariidolor.data.local.vault.Vault
import com.denariidolor.data.local.vault.VaultState
import com.denariidolor.presentation.ui.MainActivityContent
import com.denariidolor.presentation.ui.auth.BiometricAuthManager
import com.denariidolor.presentation.ui.auth.LoginActivity
import com.denariidolor.presentation.ui.common.isDarkTheme
import com.denariidolor.presentation.ui.common.setThemedContent
import com.denariidolor.presentation.ui.settings.SettingsScreenState
import com.denariidolor.util.Constants
import com.denariidolor.util.Currencies
import com.denariidolor.util.SessionManager
import com.denariidolor.util.runSuspendCatching
import dagger.hilt.android.AndroidEntryPoint
import javax.crypto.Cipher
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var vault: Vault

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var currencyPreferences: CurrencyPreferences

    private var biometricEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Also covers the task being restored after process death: the session and the open database are both gone.
        if (sessionManager.isSessionTimedOut() || !vault.isUnlocked) {
            redirectToLogin()
            return
        }
        val biometricHardware = biometricAuthManager.canAuthenticate(this)
        val currency = currencyPreferences.currency.value
        lifecycleScope.launch { refreshBiometricEnabled() }
        setThemedContent(themePreferences) {
            MainActivityContent(
                settingsState = SettingsScreenState(
                    sessionTimeoutMinutes = Constants.SESSION_TIMEOUT_MILLIS / 60_000,
                    pinConfigured = true,
                    darkMode = themePreferences.isDarkTheme(),
                    biometricAvailable = biometricHardware || biometricEnabled,
                    biometricEnabled = biometricEnabled,
                    currencyCode = currency.currencyCode,
                    currencyOptions = Currencies.options(currency).map { it.currencyCode }
                ),
                onSignOut = ::redirectToLogin,
                onDarkModeChange = themePreferences::setDarkMode,
                onBiometricChange = ::setBiometricSignIn,
                onCurrencyChange = ::setCurrency,
                onRestored = ::restart
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(30_000.milliseconds)
                    if (sessionManager.isSessionTimedOut()) {
                        redirectToLogin()
                        break
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.isSessionTimedOut()) {
            redirectToLogin()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (sessionManager.isSessionTimedOut()) {
            redirectToLogin()
        } else {
            sessionManager.touch()
        }
    }

    /**
     * Only the symbol changes; amounts aren't converted. Screens already hold amounts formatted with the old symbol, so the
     * activity starts over, with new ViewModels, on the Dashboard. The session stays signed in.
     */
    private fun setCurrency(code: String) {
        val currency = Currencies.fromCode(code) ?: return
        if (currency == currencyPreferences.currency.value) return
        currencyPreferences.setCurrency(currency)
        restart()
    }

    /** Starts over on the Dashboard with new ViewModels, still signed in: after a currency change or a restore from backup. */
    private fun restart() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /** Turning biometric sign-in on wraps the database key with a biometric-bound Keystore key, so it needs a prompt (CS-07). */
    private fun setBiometricSignIn(enabled: Boolean) {
        lifecycleScope.launch {
            if (enabled) {
                runSuspendCatching { vault.prepareBiometricEnrollment() }
                    .onSuccess(::showEnrollmentPrompt)
                    .onFailure { toast(R.string.biometric_enable_failed) }
            } else {
                runSuspendCatching { vault.disableBiometric() }
                refreshBiometricEnabled()
            }
        }
    }

    private fun showEnrollmentPrompt(cipher: Cipher) {
        biometricAuthManager.authenticate(
            activity = this,
            cipher = cipher,
            text = BiometricAuthManager.PromptText(
                title = getString(R.string.biometric_enroll_title),
                subtitle = getString(R.string.biometric_enroll_subtitle),
                negativeButton = getString(R.string.cancel)
            ),
            onAuthenticated = { authorized ->
                lifecycleScope.launch {
                    val enrolled = runSuspendCatching { vault.completeBiometricEnrollment(authorized) }.getOrDefault(false)
                    toast(if (enrolled) R.string.biometric_enabled else R.string.biometric_enable_failed)
                    refreshBiometricEnabled()
                }
            },
            onError = { canceled -> if (!canceled) toast(R.string.biometric_enable_failed) }
        )
    }

    private suspend fun refreshBiometricEnabled() {
        val state = runSuspendCatching { vault.state() }.getOrNull()
        biometricEnabled = (state as? VaultState.Configured)?.biometricEnrolled == true
    }

    private fun toast(@StringRes message: Int) = Toast.makeText(this, getString(message), Toast.LENGTH_SHORT).show()

    private fun redirectToLogin() {
        vault.lock()
        sessionManager.invalidate()
        ReportExporter.clearShareCache(this)
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
