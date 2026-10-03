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
