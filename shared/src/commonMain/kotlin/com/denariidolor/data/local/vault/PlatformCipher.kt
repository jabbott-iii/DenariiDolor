/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.vault

/**
 * What the biometric prompt authorizes (CS-07). The vault prepares it over a key that only a biometric unlocks, the prompt
 * unlocks it, and only then can the vault wrap or unwrap the database key with it. Common code only passes it between the
 * vault and the prompt and never uses it. On Android it is the javax.crypto.Cipher over a Keystore key that requires
 * BIOMETRIC_STRONG (PlatformCipher.android.kt), so Android code keeps passing Cipher; iOS gets its own in Phase 3.
 */
expect open class PlatformCipher
