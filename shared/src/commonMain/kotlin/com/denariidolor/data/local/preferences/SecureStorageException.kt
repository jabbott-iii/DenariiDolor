/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

/**
 * The encrypted profile, or a Keystore key it depends on, can't be read (BUG-05). Sign-in can't continue; the user can
 * retry, or wipe the app to start again.
 */
class SecureStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
