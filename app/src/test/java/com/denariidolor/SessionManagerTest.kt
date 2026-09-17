package com.denariidolor

import com.denariidolor.util.Constants
import com.denariidolor.util.SessionManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {
    @Test
    fun invalidateMarksSessionAsTimedOut() {
        val sessionManager = SessionManager()
        sessionManager.markAuthenticated(now = 1_000L)

        assertFalse(sessionManager.isSessionTimedOut(now = 1_000L + Constants.SESSION_TIMEOUT_MILLIS - 1))

        sessionManager.invalidate()

        assertTrue(sessionManager.isSessionTimedOut(now = 1_000L))
    }

    @Test
    fun touchDoesNotAuthenticateByItself() {
        val sessionManager = SessionManager()

        sessionManager.touch(now = 1_000L)

        assertTrue(sessionManager.isSessionTimedOut(now = 1_000L))
    }
}
