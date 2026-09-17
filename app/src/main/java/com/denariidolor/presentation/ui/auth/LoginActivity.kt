package com.denariidolor.presentation.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.denariidolor.MainActivity
import com.denariidolor.R
import com.denariidolor.data.local.preferences.EncryptedPreferencesManager
import com.denariidolor.databinding.ActivityLoginBinding
import com.denariidolor.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    @Inject
    lateinit var encryptedPreferencesManager: EncryptedPreferencesManager

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val hasStoredPin = encryptedPreferencesManager.getPin() != null

        binding.btnLogin.setOnClickListener {
            val pin = binding.etPin.text?.toString().orEmpty()
            if (pin.length < 4) {
                Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val stored = encryptedPreferencesManager.getPin()
            if (stored == null) {
                encryptedPreferencesManager.savePin(pin)
            } else if (stored != pin) {
                Toast.makeText(this, "Invalid PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            openMain()
        }

        if (hasStoredPin && biometricAuthManager.canAuthenticate(this)) {
            binding.btnBiometricLogin.setOnClickListener { promptForBiometricSignIn() }
        } else {
            binding.btnBiometricLogin.visibility = android.view.View.GONE
        }
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
