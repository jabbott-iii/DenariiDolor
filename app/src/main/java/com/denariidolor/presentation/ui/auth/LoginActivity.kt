package com.denariidolor.presentation.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.denariidolor.MainActivity
import com.denariidolor.R
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.DefaultDataInitializer
import com.denariidolor.data.local.preferences.EncryptedPreferencesManager
import com.denariidolor.domain.usecase.InitialLoginResult
import com.denariidolor.domain.usecase.InitialLoginUseCase
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

    @Inject
    lateinit var appDatabase: AppDatabase

    private var signInEnabled by mutableStateOf(false)
    private var biometricAvailable by mutableStateOf(false)
    private var isFirstTimeSetup by mutableStateOf(true)
    private var recoveryQuestion by mutableStateOf<String?>(null)
    private var isSubmitting by mutableStateOf(false)
    private var pinInput by mutableStateOf("")
    private var loginFormStateVersion by mutableStateOf(0)
    private lateinit var initialLoginUseCase: InitialLoginUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialLoginUseCase = InitialLoginUseCase(
            pinSecurityStore = encryptedPreferencesManager,
            wipeAllData = ::wipeAllUserData
        )
        setContent {
            DenariiDolorTheme {
                key(loginFormStateVersion) {
                    LoginScreen(
                        pin = pinInput,
                        signInEnabled = signInEnabled,
                        biometricAvailable = biometricAvailable,
                        isFirstTimeSetup = isFirstTimeSetup,
                        securityQuestionPrompt = recoveryQuestion,
                        isSubmitting = isSubmitting,
                        onPinChange = { pinInput = it },
                        onSignIn = { handleSignIn(pinInput) },
                        onSetup = { confirmPin, securityQuestion, securityAnswer ->
                            handleSetup(pinInput, confirmPin, securityQuestion, securityAnswer)
                        },
                        onBiometricLogin = ::promptForBiometricSignIn,
                        onRecoverPin = { answer, newPin, confirmNewPin ->
                            handlePinRecovery(answer, newPin, confirmNewPin)
                        },
                        onWipeDataConfirmed = ::handleConfirmedDataWipe
                    )
                }
            }
        }
        signInEnabled = false

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                defaultDataInitializer.seedDefaults()
            }
            signInEnabled = true
            refreshLoginState()
        }
    }

    private fun handleSignIn(pin: String) {
        if (!signInEnabled || isSubmitting || isFirstTimeSetup) return
        isSubmitting = true
        when (val result = initialLoginUseCase.authenticate(pin)) {
            InitialLoginResult.Success -> {
                pinInput = ""
                openMain()
            }

            is InitialLoginResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }
        isSubmitting = false
    }

    private fun handleSetup(pin: String, confirmPin: String, securityQuestion: String, securityAnswer: String) {
        if (!signInEnabled || isSubmitting || !isFirstTimeSetup) return
        isSubmitting = true
        when (val result = initialLoginUseCase.setup(pin, confirmPin, securityQuestion, securityAnswer)) {
            InitialLoginResult.Success -> {
                pinInput = ""
                refreshLoginState()
                openMain()
            }

            is InitialLoginResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }
        isSubmitting = false
    }

    private fun handlePinRecovery(answer: String, newPin: String, confirmNewPin: String): Boolean {
        if (!signInEnabled || isSubmitting || isFirstTimeSetup) return false
        isSubmitting = true
        val successful = when (val result = initialLoginUseCase.recoverPin(answer, newPin, confirmNewPin)) {
            InitialLoginResult.Success -> {
                Toast.makeText(this, "PIN reset successful. Sign in with your new PIN.", Toast.LENGTH_SHORT).show()
                pinInput = ""
                true
            }

            is InitialLoginResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                false
            }
        }
        isSubmitting = false
        return successful
    }

    private fun handleConfirmedDataWipe() {
        if (!signInEnabled || isSubmitting) return
        isSubmitting = true
        lifecycleScope.launch {
            try {
                when (val result = initialLoginUseCase.wipeData(confirm = true)) {
                    InitialLoginResult.Success -> {
                        pinInput = ""
                        loginFormStateVersion += 1
                        refreshLoginState()
                        Toast.makeText(this@LoginActivity, "All app data has been deleted.", Toast.LENGTH_SHORT).show()
                    }

                    is InitialLoginResult.Error -> {
                        Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                isSubmitting = false
            }
        }
    }

    private fun refreshLoginState() {
        isFirstTimeSetup = initialLoginUseCase.isFirstTimeSetup()
        recoveryQuestion = initialLoginUseCase.securityQuestion()
        biometricAvailable = !isFirstTimeSetup && biometricAuthManager.canAuthenticate(this)
    }

    private suspend fun wipeAllUserData() {
        withContext(Dispatchers.IO) {
            appDatabase.clearAllTables()
            encryptedPreferencesManager.clearAll()
            clearDirectory(applicationContext.cacheDir)
            defaultDataInitializer.seedDefaults()
            sessionManager.invalidate()
        }
    }

    private fun clearDirectory(directory: java.io.File?) {
        directory?.listFiles()?.forEach { child ->
            child.deleteRecursively()
        }
    }

    private fun promptForBiometricSignIn() {
        if (isSubmitting || isFirstTimeSetup) {
            return
        }
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
