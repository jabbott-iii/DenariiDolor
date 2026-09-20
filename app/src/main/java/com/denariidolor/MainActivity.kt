package com.denariidolor

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.denariidolor.data.local.preferences.EncryptedPreferencesManager
import com.denariidolor.presentation.ui.MainActivityContent
import com.denariidolor.presentation.ui.auth.LoginActivity
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.presentation.ui.settings.SettingsScreenState
import com.denariidolor.util.Constants
import com.denariidolor.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var encryptedPreferencesManager: EncryptedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DenariiDolorTheme {
                MainActivityContent(
                    settingsState = SettingsScreenState(
                        sessionTimeoutMinutes = Constants.SESSION_TIMEOUT_MILLIS / 60_000,
                        pinConfigured = encryptedPreferencesManager.isProfileConfigured()
                    ),
                    onSettingsAction = ::redirectToLogin
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(30_000.milliseconds)
                    if (sessionManager.isSessionTimedOut()) {
                        redirectToLogin(resetSignInPin = false)
                        break
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (sessionManager.isSessionTimedOut()) {
            redirectToLogin(resetSignInPin = false)
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (sessionManager.isSessionTimedOut()) {
            redirectToLogin(resetSignInPin = false)
        } else {
            sessionManager.touch()
        }
    }

    private fun redirectToLogin(resetSignInPin: Boolean) {
        if (resetSignInPin) {
            encryptedPreferencesManager.clearPinForSignInReset()
        }
        sessionManager.invalidate()
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}
