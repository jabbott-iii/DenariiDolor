/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.vault

import com.denariidolor.data.local.preferences.SetupProfileResult

/**
 * What the sign-in screen does with the [Vault]: an interface, so its ViewModel can be tested without the Keystore. Each call
 * runs off the main thread and reports a bad profile as `SecureStorageException`, as [Vault] documents.
 */
interface SignInVault {
    suspend fun state(): VaultState

    suspend fun setUp(pin: String, pinConfirmation: String, question: String, answer: String): SetupProfileResult

    suspend fun signIn(pin: String): SignInResult

    suspend fun recoverPin(answer: String, newPin: String, pinConfirmation: String): RecoveryResult

    suspend fun completeUpgrade(question: String, answer: String): SetupProfileResult

    suspend fun prepareBiometricSignIn(): BiometricSignIn

    suspend fun signInWithBiometric(cipher: PlatformCipher): SignInResult

    fun lock()

    suspend fun wipe(): Boolean
}
