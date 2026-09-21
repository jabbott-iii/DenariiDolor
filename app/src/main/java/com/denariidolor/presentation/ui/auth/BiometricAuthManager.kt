package com.denariidolor.presentation.ui.auth

import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject

class BiometricAuthManager @Inject constructor() {
    fun canAuthenticate(activity: FragmentActivity): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    companion object {
        const val ALLOWED_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG
    }
}
