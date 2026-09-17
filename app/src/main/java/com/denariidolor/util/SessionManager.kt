package com.denariidolor.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private var lastActiveAt: Long = System.currentTimeMillis()
    private var authenticated: Boolean = false

    fun touch(now: Long = System.currentTimeMillis()) {
        authenticated = true
        lastActiveAt = now
    }

    fun isSessionTimedOut(now: Long = System.currentTimeMillis()): Boolean {
        return !authenticated || now - lastActiveAt > Constants.SESSION_TIMEOUT_MILLIS
    }

    fun invalidate() {
        authenticated = false
        lastActiveAt = 0L
    }
}
