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

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denariidolor.data.local.preferences.EncryptedPreferencesManager
import com.denariidolor.data.local.preferences.ThemePreferences
import com.denariidolor.presentation.ui.MainActivityContent
import com.denariidolor.presentation.ui.auth.LoginActivity
import com.denariidolor.presentation.ui.common.isDarkTheme
import com.denariidolor.presentation.ui.common.setThemedContent
import com.denariidolor.presentation.ui.settings.SettingsScreenState
import com.denariidolor.util.Constants
import com.denariidolor.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var encryptedPreferencesManager: EncryptedPreferencesManager

    @Inject
    lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pinConfigured = encryptedPreferencesManager.isProfileConfigured()
        setThemedContent(themePreferences) {
            MainActivityContent(
                settingsState = SettingsScreenState(
                    sessionTimeoutMinutes = Constants.SESSION_TIMEOUT_MILLIS / 60_000,
                    pinConfigured = pinConfigured,
                    darkMode = themePreferences.isDarkTheme()
                ),
                onSignOut = ::redirectToLogin,
                onDarkModeChange = themePreferences::setDarkMode
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(30_000.milliseconds)
                    if (sessionManager.isSessionTimedOut()) {
                        redirectToLogin()
                        break
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.isSessionTimedOut()) {
            redirectToLogin()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (sessionManager.isSessionTimedOut()) {
            redirectToLogin()
        } else {
            sessionManager.touch()
        }
    }

    private fun redirectToLogin() {
        sessionManager.invalidate()
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
