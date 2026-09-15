package com.denariidolor.presentation.ui.auth

import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject

class BiometricAuthManager @Inject constructor() {
    fun canAuthenticate(activity: FragmentActivity): Boolean {
        val biometricManager = BiometricManager.from(activity)
        val result = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }
}
