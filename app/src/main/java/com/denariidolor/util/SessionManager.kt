/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import android.os.SystemClock
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Tracks the signed-in session. Times come from [SystemClock.elapsedRealtime] (monotonic, includes deep sleep),
 * so changing the device's wall clock cannot extend or reset a session.
 */
@SingleIn(AppScope::class)
@Inject
class SessionManager {
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
