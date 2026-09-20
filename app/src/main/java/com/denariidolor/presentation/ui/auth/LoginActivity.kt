package com.denariidolor.presentation.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.denariidolor.MainActivity
import com.denariidolor.R
import com.denariidolor.data.local.db.DefaultDataInitializer
import com.denariidolor.data.local.preferences.EncryptedPreferencesManager
import com.denariidolor.presentation.ui.LoginScreen
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    @Inject
    lateinit var encryptedPreferencesManager: EncryptedPreferencesManager

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var defaultDataInitializer: DefaultDataInitializer

    private var signInEnabled by mutableStateOf(false)
    private var biometricAvailable by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var pin by rememberSaveable { mutableStateOf("") }
            DenariiDolorTheme {
                LoginScreen(
                    pin = pin,
                    signInEnabled = signInEnabled,
                    biometricAvailable = biometricAvailable,
                    onPinChange = { pin = it },
                    onLogin = { handlePinLogin(pin) },
                    onBiometricLogin = ::promptForBiometricSignIn
                )
            }
        }
        setSignInEnabled(false)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                defaultDataInitializer.seedDefaults()
            }
            setSignInEnabled(true)
            biometricAvailable = encryptedPreferencesManager.getPin() != null && biometricAuthManager.canAuthenticate(this@LoginActivity)
        }
    }

    private fun handlePinLogin(pin: String) {
        if (pin.length < 4) {
            Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
            return
        }

        val stored = encryptedPreferencesManager.getPin()
        if (stored == null) {
            encryptedPreferencesManager.savePin(pin)
        } else if (stored != pin) {
            Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show()
            return
        }

        openMain()
    }

    private fun setSignInEnabled(enabled: Boolean) {
        signInEnabled = enabled
    }

    private fun promptForBiometricSignIn() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    openMain()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        Toast.makeText(
                            this@LoginActivity,
                            getString(R.string.biometric_sign_in_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@LoginActivity, getString(R.string.biometric_sign_in_failed), Toast.LENGTH_SHORT).show()
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_sign_in_title))
                .setSubtitle(getString(R.string.biometric_sign_in_subtitle))
                .setNegativeButtonText(getString(R.string.use_pin_instead))
                .build()
        )
    }

    private fun openMain() {
        sessionManager.markAuthenticated()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
