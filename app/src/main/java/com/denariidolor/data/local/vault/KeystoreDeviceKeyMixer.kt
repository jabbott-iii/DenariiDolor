/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.vault

import com.denariidolor.data.local.preferences.DeviceKeyMixer

/** Binds PIN and answer derivation to this device through a non-exportable Keystore HMAC key. */
class KeystoreDeviceKeyMixer(private val secrets: KeystoreSecrets) : DeviceKeyMixer {
    override fun prepare() = secrets.ensureDeviceKey()

    override fun mix(input: ByteArray): ByteArray = secrets.hmacWithDeviceKey(input)
}
