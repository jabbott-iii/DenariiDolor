/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the signed-in session. Times come from [SystemClock.elapsedRealtime] (monotonic, includes deep sleep),
 * so changing the device's wall clock cannot extend or reset a session.
 */
@Singleton
class SessionManager @Inject constructor() {
    private var lastActiveAt: Long = 0L
    private var authenticated: Boolean = false

    fun touch(now: Long = SystemClock.elapsedRealtime()) {
        if (authenticated) {
            lastActiveAt = now
        }
    }

    fun markAuthenticated(now: Long = SystemClock.elapsedRealtime()) {
        authenticated = true
        lastActiveAt = now
    }

    fun isSessionTimedOut(now: Long = SystemClock.elapsedRealtime()): Boolean =
        !authenticated || now - lastActiveAt > Constants.SESSION_TIMEOUT_MILLIS

    fun invalidate() {
        authenticated = false
        lastActiveAt = 0L
    }
}
