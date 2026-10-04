/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher
import javax.inject.Inject

class BiometricAuthManager @Inject constructor() {
    data class PromptText(val title: String, val subtitle: String, val negativeButton: String)

    fun canAuthenticate(activity: FragmentActivity): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows the system prompt bound to [cipher], so success unlocks a Keystore key instead of only firing a callback
     * (CS-07). [onAuthenticated] receives the cipher the prompt authorized; [onError] reports whether the user canceled.
     */
    fun authenticate(
        activity: FragmentActivity,
        cipher: Cipher,
        text: PromptText,
        onAuthenticated: (Cipher) -> Unit,
        onError: (canceled: Boolean) -> Unit,
        onFailedAttempt: () -> Unit = {}
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val authorized = result.cryptoObject?.cipher
                if (authorized == null) onError(false) else onAuthenticated(authorized)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errorCode in CANCEL_CODES)

            override fun onAuthenticationFailed() = onFailedAttempt()
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(text.title)
            .setSubtitle(text.subtitle)
            .setNegativeButtonText(text.negativeButton)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            .authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    companion object {
        const val ALLOWED_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG
        private val CANCEL_CODES = setOf(
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_CANCELED
        )
    }
}
