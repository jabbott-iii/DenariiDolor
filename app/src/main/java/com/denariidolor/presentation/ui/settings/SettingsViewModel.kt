/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.data.local.preferences.CurrencySettings
import com.denariidolor.data.local.preferences.ThemeSettings
import com.denariidolor.data.local.vault.PlatformCipher
import com.denariidolor.data.local.vault.SettingsVault
import com.denariidolor.data.local.vault.VaultState
import com.denariidolor.di.SessionScope
import com.denariidolor.presentation.platform.BiometricPromptResult
import com.denariidolor.presentation.ui.auth.BiometricCapability
import com.denariidolor.presentation.ui.common.StringKey
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.util.Constants
import com.denariidolor.util.Currencies
import com.denariidolor.util.PlatformCurrency
import com.denariidolor.util.runSuspendCatching
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What Settings shows. The theme follows the device until the user picks one, so [toScreenState] takes the device's choice. */
data class SettingsUiState(
    val darkModeOverride: Boolean? = null,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricChanging: Boolean = false,
    val currencyCode: String = Currencies.code(Currencies.DEFAULT),
    val currencyOptions: List<String> = emptyList()
) {
    fun toScreenState(systemDarkMode: Boolean): SettingsScreenState = SettingsScreenState(
        sessionTimeoutMinutes = Constants.SESSION_TIMEOUT_MILLIS.milliseconds.inWholeMinutes,
        // Settings opens only after sign-in, so a PIN is always set.
        pinConfigured = true,
        darkMode = darkModeOverride ?: systemDarkMode,
        biometricAvailable = biometricAvailable,
        biometricEnabled = biometricEnabled,
        biometricChanging = biometricChanging,
        currencyCode = currencyCode,
        currencyOptions = currencyOptions
    )
}

/** What only the screen can do: show the system biometric prompt (CS-07), and start the app over after a currency change. */
sealed interface SettingsEvent {
    class ShowEnrollmentPrompt(val cipher: PlatformCipher) : SettingsEvent

    data object CurrencyChanged : SettingsEvent
}

/** Dark mode, biometric sign-in and the currency label, which MainActivity used to handle itself. */
@ViewModelKey
@ContributesIntoMap(SessionScope::class)
@Inject
class SettingsViewModel(
    private val vault: SettingsVault,
    biometrics: BiometricCapability,
    private val theme: ThemeSettings,
    private val currencySettings: CurrencySettings
) : ViewModel() {
    private data class BiometricSwitch(val enabled: Boolean = false, val changing: Boolean = false)

    private val biometricHardware = biometrics.canAuthenticate()
    private val biometric = MutableStateFlow(BiometricSwitch())

    val state: StateFlow<SettingsUiState> = combine(theme.darkModeOverride, currencySettings.currency, biometric, ::uiState)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            uiState(theme.darkModeOverride.value, currencySettings.currency.value, biometric.value)
        )

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            val enrolled = biometricEnrolled()
            biometric.update { it.copy(enabled = enrolled) }
        }
    }

    fun onDarkModeChange(enabled: Boolean) = theme.setDarkMode(enabled)

    /**
     * Turning biometric sign-in on wraps the database key with a new biometric-bound Keystore key, which needs the system
     * prompt (CS-07). Taps are ignored from the first one until its vault call and prompt finish, because a second
     * enrollment would replace the key the first prompt is using.
     */
    fun onBiometricChange(enabled: Boolean) {
        if (biometric.value.changing) return
        biometric.update { it.copy(changing = true) }
        viewModelScope.launch {
            if (enabled) {
                val cipher = runSuspendCatching { vault.prepareBiometricEnrollment() }.getOrNull()
                if (cipher != null) {
                    _events.send(SettingsEvent.ShowEnrollmentPrompt(cipher))
                } else {
                    _messages.send(UiMessage.Resource(StringKey.BIOMETRIC_ENABLE_FAILED))
                    finishBiometricChange()
                }
            } else {
                runSuspendCatching { vault.disableBiometric() }
                finishBiometricChange()
            }
        }
    }

    /** How the enrollment prompt ended. Only the cipher it authorized can wrap the database key (CS-07). */
    fun onEnrollmentPromptResult(result: BiometricPromptResult) {
        // Nothing asked for a prompt, so there's nothing to finish.
        if (!biometric.value.changing) return
        viewModelScope.launch {
            when (result) {
                is BiometricPromptResult.Authorized -> {
                    val enrolled = runSuspendCatching { vault.completeBiometricEnrollment(result.cipher) }.getOrDefault(false)
                    _messages.send(UiMessage.Resource(if (enrolled) StringKey.BIOMETRIC_ENABLED else StringKey.BIOMETRIC_ENABLE_FAILED))
                }
                BiometricPromptResult.Failed -> _messages.send(UiMessage.Resource(StringKey.BIOMETRIC_ENABLE_FAILED))
                BiometricPromptResult.Canceled -> Unit
            }
            finishBiometricChange()
        }
    }

    /**
     * Only the symbol changes; amounts aren't converted. Screens already hold amounts formatted with the old symbol, so the
     * app starts over on the Dashboard ([SettingsEvent.CurrencyChanged]), still signed in.
     */
    fun onCurrencyChange(code: String) {
        val currency = Currencies.fromCode(code) ?: return
        if (currency == currencySettings.currency.value) return
        currencySettings.setCurrency(currency)
        viewModelScope.launch { _events.send(SettingsEvent.CurrencyChanged) }
    }

    /** Shows the switch as the vault now has it, and lets it be tapped again. */
    private suspend fun finishBiometricChange() {
        val enrolled = biometricEnrolled()
        biometric.update { BiometricSwitch(enabled = enrolled, changing = false) }
    }

    private suspend fun biometricEnrolled(): Boolean =
        (runSuspendCatching { vault.state() }.getOrNull() as? VaultState.Configured)?.biometricEnrolled == true

    private fun uiState(darkModeOverride: Boolean?, currency: PlatformCurrency, switch: BiometricSwitch) = SettingsUiState(
        darkModeOverride = darkModeOverride,
        // Shown while it's on even if the device can no longer use it, so it can still be turned off.
        biometricAvailable = biometricHardware || switch.enabled,
        biometricEnabled = switch.enabled,
        biometricChanging = switch.changing,
        currencyCode = Currencies.code(currency),
        currencyOptions = Currencies.options(currency).map(Currencies::code)
    )
}
