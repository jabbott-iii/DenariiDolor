/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denariidolor.data.export.ReportExporter
import com.denariidolor.data.local.preferences.ThemePreferences
import com.denariidolor.data.local.vault.Vault
import com.denariidolor.presentation.ui.MainActivityContent
import com.denariidolor.presentation.ui.auth.LoginActivity
import com.denariidolor.presentation.ui.common.setThemedContent
import com.denariidolor.util.SessionManager
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var vault: Vault

    @Inject
    lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Filled before anything reads them, including the session check below (CS-02).
        appGraph.inject(this)
        super.onCreate(savedInstanceState)
        // Also covers the task being restored after process death: the session and the open database are both gone.
        if (sessionManager.isSessionTimedOut() || !vault.isUnlocked) {
            redirectToLogin()
            return
        }
        // Only now, with the vault unlocked, can anything reach the database (rule 4). The graph goes away with this activity.
        val session = appGraph.createSessionGraph()
        // Settings, with biometric enrollment (CS-07), runs in SettingsViewModel; the activity keeps the session and restarts.
        setThemedContent(themePreferences) {
            CompositionLocalProvider(LocalMetroViewModelFactory provides session.viewModelFactory) {
                MainActivityContent(onSignOut = ::redirectToLogin, onCurrencyChanged = ::restart, onRestored = ::restart)
            }
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

    /** Starts over on the Dashboard with new ViewModels, still signed in: after a currency change or a restore from backup. */
    private fun restart() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun redirectToLogin() {
        vault.lock()
        sessionManager.invalidate()
        ReportExporter.clearShareCache(this)
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
