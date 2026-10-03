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
