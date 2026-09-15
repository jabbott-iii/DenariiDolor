package com.denariidolor.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private var lastActiveAt: Long = System.currentTimeMillis()

    fun touch(now: Long = System.currentTimeMillis()) {
        lastActiveAt = now
    }

    fun isSessionTimedOut(now: Long = System.currentTimeMillis()): Boolean {
        return now - lastActiveAt > Constants.SESSION_TIMEOUT_MILLIS
    }
}
