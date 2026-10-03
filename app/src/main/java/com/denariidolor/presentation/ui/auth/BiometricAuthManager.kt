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
