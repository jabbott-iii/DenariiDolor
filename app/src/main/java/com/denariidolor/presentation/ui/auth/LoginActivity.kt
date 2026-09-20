package com.denariidolor.presentation.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.denariidolor.MainActivity
import com.denariidolor.R
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.DefaultDataInitializer
import com.denariidolor.data.local.preferences.EncryptedPreferencesManager
import com.denariidolor.data.local.preferences.ProfileMode
import com.denariidolor.data.local.preferences.RecoverPinResult
import com.denariidolor.data.local.preferences.SetupProfileResult
import com.denariidolor.presentation.ui.LoginScreen
import com.denariidolor.presentation.ui.LoginScreenMode
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
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
    private var loginMenuMode by mutableStateOf(LoginScreenMode.SETUP)
    private var showWipeConfirmation by mutableStateOf(false)
    private var actionInProgress by mutableStateOf(true)
    private var feedbackMessage by mutableStateOf<String?>(null)
    private var recoveryQuestion by mutableStateOf<String?>(null)

    private var pin by mutableStateOf("")
    private var pinConfirmation by mutableStateOf("")
    private var securityQuestion by mutableStateOf("")
    private var securityAnswer by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DenariiDolorTheme {
                LoginScreen(
                    mode = loginMenuMode,
                    pin = pin,
                    pinConfirmation = pinConfirmation,
                    securityQuestion = securityQuestion,
                    securityAnswer = securityAnswer,
                    recoveryQuestion = recoveryQuestion,
                    signInEnabled = signInEnabled,
                    biometricAvailable = biometricAvailable,
                    showWipeConfirmation = showWipeConfirmation,
                    feedbackMessage = feedbackMessage,
                    onPinChange = { pin = it },
                    onPinConfirmationChange = { pinConfirmation = it },
                    onSecurityQuestionChange = { securityQuestion = it },
                    onSecurityAnswerChange = { securityAnswer = it },
                    onPrimaryAction = ::handlePrimaryAction,
                    onForgotPin = ::startPinRecovery,
                    onBackToSignIn = ::switchToSignIn,
                    onBiometricLogin = ::promptForBiometricSignIn,
                    onRequestWipeData = {
                        if (signInEnabled && !actionInProgress) {
                            showWipeConfirmation = true
                        }
                    },
                    onCancelWipeData = { showWipeConfirmation = false },
                    onConfirmWipeData = ::wipeAllUserData
                )
            }
        }

        initializeScreen()
    }

    private fun initializeScreen() {
        signInEnabled = false
        actionInProgress = true
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                defaultDataInitializer.seedDefaults()
            }
            refreshLoginState()
            feedbackMessage = null
            actionInProgress = false
            signInEnabled = true
        }
    }

    private fun refreshLoginState() {
        val profileState = encryptedPreferencesManager.getProfileState()
        if (profileState.mode == ProfileMode.SIGN_IN && loginMenuMode != LoginScreenMode.RECOVER_PIN) {
            loginMenuMode = LoginScreenMode.SIGN_IN
        }
        if (profileState.mode == ProfileMode.FIRST_TIME_SETUP) {
            loginMenuMode = LoginScreenMode.SETUP
        }
        recoveryQuestion = profileState.securityQuestion
        biometricAvailable =
            profileState.mode == ProfileMode.SIGN_IN && biometricAuthManager.canAuthenticate(this)
    }

    private fun handlePrimaryAction() {
        if (!signInEnabled || actionInProgress) {
            return
        }

        when (loginMenuMode) {
            LoginScreenMode.SIGN_IN -> handlePinLogin()
            LoginScreenMode.SETUP -> handleSetupProfile()
            LoginScreenMode.RECOVER_PIN -> handleRecoverPin()
        }
    }

    private fun handlePinLogin() {
        if (!PIN_REGEX.matches(pin)) {
            feedbackMessage = getString(R.string.pin_format_error)
            return
        }

        if (!encryptedPreferencesManager.verifyPin(pin)) {
            feedbackMessage = getString(R.string.invalid_pin)
            return
        }

        feedbackMessage = null
        openMain()
    }

    private fun handleSetupProfile() {
        val result = encryptedPreferencesManager.setupProfile(
            pin = pin,
            pinConfirmation = pinConfirmation,
            securityQuestion = securityQuestion,
            securityAnswer = securityAnswer
        )

        feedbackMessage =
            when (result) {
                SetupProfileResult.SUCCESS -> {
                    clearInputs()
                    refreshLoginState()
                    getString(R.string.profile_setup_success)
                }
                SetupProfileResult.ALREADY_CONFIGURED -> getString(R.string.profile_already_configured)
                SetupProfileResult.INVALID_PIN_FORMAT -> getString(R.string.pin_format_error)
                SetupProfileResult.PIN_MISMATCH -> getString(R.string.pin_confirmation_mismatch)
                SetupProfileResult.SECURITY_QUESTION_REQUIRED -> getString(R.string.security_question_required)
                SetupProfileResult.SECURITY_ANSWER_REQUIRED -> getString(R.string.security_answer_required)
                SetupProfileResult.LEGACY_PIN_MISMATCH -> getString(R.string.legacy_pin_migration_mismatch)
            }
    }

    private fun startPinRecovery() {
        if (actionInProgress || !encryptedPreferencesManager.isProfileConfigured()) {
            return
        }

        loginMenuMode = LoginScreenMode.RECOVER_PIN
        pin = ""
        pinConfirmation = ""
        securityAnswer = ""
        securityQuestion = ""
        recoveryQuestion = encryptedPreferencesManager.getSecurityQuestion()
        feedbackMessage = null
    }

    private fun switchToSignIn() {
        if (actionInProgress) {
            return
        }
        clearInputs()
        refreshLoginState()
        feedbackMessage = null
    }

    private fun handleRecoverPin() {
        val result = encryptedPreferencesManager.recoverPin(
            securityAnswer = securityAnswer,
            newPin = pin,
            pinConfirmation = pinConfirmation
        )

        feedbackMessage =
            when (result) {
                RecoverPinResult.SUCCESS -> {
                    clearInputs()
                    loginMenuMode = LoginScreenMode.SIGN_IN
                    refreshLoginState()
                    getString(R.string.pin_recovery_success)
                }
                RecoverPinResult.PROFILE_NOT_CONFIGURED -> getString(R.string.profile_not_configured)
                RecoverPinResult.SECURITY_ANSWER_REQUIRED -> getString(R.string.security_answer_required)
                RecoverPinResult.INVALID_SECURITY_ANSWER -> getString(R.string.invalid_security_answer)
                RecoverPinResult.INVALID_PIN_FORMAT -> getString(R.string.pin_format_error)
                RecoverPinResult.PIN_MISMATCH -> getString(R.string.pin_confirmation_mismatch)
            }
    }

    private fun wipeAllUserData() {
        if (actionInProgress) {
            return
        }
        showWipeConfirmation = false
        actionInProgress = true
        signInEnabled = false

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                appDatabase.clearAllTables()
                encryptedPreferencesManager.clearAllSecurityData()
                clearAppOwnedFiles(filesDir)
                clearAppOwnedFiles(cacheDir)
            }
            sessionManager.invalidate()
            clearInputs()
            securityQuestion = ""
            securityAnswer = ""
            recoveryQuestion = null
            feedbackMessage = getString(R.string.wipe_data_success)
            withContext(Dispatchers.IO) {
                defaultDataInitializer.seedDefaults()
            }
            refreshLoginState()
            actionInProgress = false
            signInEnabled = true
        }
    }

    private fun clearAppOwnedFiles(directory: File?) {
        directory?.listFiles()?.forEach { file ->
            file.deleteRecursively()
        }
    }

    private fun clearInputs() {
        pin = ""
        pinConfirmation = ""
        securityQuestion = ""
        securityAnswer = ""
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

    companion object {
        private val PIN_REGEX = Regex("^[0-9]{4,12}$")
    }
}
