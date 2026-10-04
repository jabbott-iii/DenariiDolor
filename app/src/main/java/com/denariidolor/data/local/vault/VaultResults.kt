/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.vault

import com.denariidolor.data.local.preferences.RecoverPinResult
import javax.crypto.Cipher

sealed interface VaultState {
    data object NeedsSetup : VaultState

    /**
     * A profile exists. [upgradeStarted] means a v1 profile already accepted the user in this session, and only its new
     * security question is missing ([Vault.completeUpgrade]).
     */
    data class Configured(val securityQuestion: String?, val biometricEnrolled: Boolean, val upgradeStarted: Boolean) : VaultState
}

sealed interface SignInResult {
    data object Unlocked : SignInResult

    /** A v1 profile accepted the PIN; a new security question and answer finish the upgrade. */
    data object UpgradeRequired : SignInResult

    data class InvalidPin(val attemptsBeforeLockout: Int) : SignInResult

    data class LockedOut(val remainingMillis: Long) : SignInResult

    /** The biometric key no longer opens the vault, so biometric sign-in was turned off. */
    data object BiometricFailed : SignInResult
}

sealed interface RecoveryResult {
    data class Finished(val result: RecoverPinResult, val lockoutRemainingMillis: Long = 0L) : RecoveryResult

    /** A v1 profile accepted the answer; a new security question and answer finish the upgrade. */
    data object UpgradeRequired : RecoveryResult
}

sealed interface BiometricSignIn {
    class Ready(val cipher: Cipher) : BiometricSignIn

    data object NotEnrolled : BiometricSignIn

    /** The device's enrolled biometrics changed, which invalidated the key, so biometric sign-in was turned off. */
    data object Invalidated : BiometricSignIn
}
