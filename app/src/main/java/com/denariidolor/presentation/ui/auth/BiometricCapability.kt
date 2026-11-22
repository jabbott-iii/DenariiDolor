/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.auth

import android.content.Context
import androidx.biometric.BiometricManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Whether the device can sign in with a strong biometric now; an interface, so the sign-in ViewModel can be tested. */
fun interface BiometricCapability {
    fun canAuthenticate(): Boolean
}

/** Asks `BiometricManager` about [BiometricAuthManager.ALLOWED_AUTHENTICATORS], on the application context so a ViewModel can hold it. */
class DeviceBiometricCapability @Inject constructor(@ApplicationContext private val context: Context) : BiometricCapability {
    override fun canAuthenticate(): Boolean =
        BiometricManager.from(context).canAuthenticate(BiometricAuthManager.ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
}
