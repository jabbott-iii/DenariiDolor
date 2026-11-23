/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.vault

/**
 * What Settings does with the [Vault] while signed in: turn biometric sign-in on and off (CS-07). An interface, so its
 * ViewModel can be tested without the Keystore. Each call runs off the main thread, as [Vault] documents.
 */
interface SettingsVault {
    suspend fun state(): VaultState

    /** A cipher for `BiometricPrompt` that wraps the database key with a new biometric-bound key. Needs an unlocked vault. */
    suspend fun prepareBiometricEnrollment(): PlatformCipher

    /** Stores the database key wrapped by the cipher `BiometricPrompt` authorized; false if the vault locked meanwhile. */
    suspend fun completeBiometricEnrollment(cipher: PlatformCipher): Boolean

    /** Deletes the biometric wrap and its key; the PIN and the security answer still open the vault. */
    suspend fun disableBiometric()
}
