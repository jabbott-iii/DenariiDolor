/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
private const val DERIVED_KEY_BITS = 256

/** PBKDF2-HMAC-SHA256 (NIST SP 800-132) with a 256-bit output, shared by the current and the v1 profile formats. */
internal fun pbkdf2(secret: String, salt: ByteArray, iterations: Int): ByteArray {
    val spec = PBEKeySpec(secret.toCharArray(), salt, iterations, DERIVED_KEY_BITS)
    return try {
        SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
    } finally {
        spec.clearPassword()
    }
}
