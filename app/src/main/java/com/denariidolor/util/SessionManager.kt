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

package com.denariidolor.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private var lastActiveAt: Long = System.currentTimeMillis()
    private var authenticated: Boolean = false

    fun touch(now: Long = System.currentTimeMillis()) {
        if (authenticated) {
            lastActiveAt = now
        }
    }

    fun markAuthenticated(now: Long = System.currentTimeMillis()) {
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
