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
