package com.denariidolor.domain.usecase

import com.denariidolor.domain.auth.PinSecurityStore

sealed interface InitialLoginResult {
    data object Success : InitialLoginResult
    data class Error(val message: String) : InitialLoginResult
}

class InitialLoginUseCase(
    private val pinSecurityStore: PinSecurityStore,
    private val wipeAllData: suspend () -> Unit
) {
    fun isFirstTimeSetup(): Boolean = !pinSecurityStore.isPinConfigured()

    fun securityQuestion(): String? = pinSecurityStore.getSecurityQuestion()

    fun setup(pin: String, confirmPin: String, securityQuestion: String, securityAnswer: String): InitialLoginResult {
        if (!isFirstTimeSetup()) return InitialLoginResult.Error("PIN setup has already been completed")
        val pinValidation = validatePin(pin, confirmPin)
        if (pinValidation != null) return pinValidation
        if (securityQuestion.isBlank()) return InitialLoginResult.Error("Security question is required")
        if (securityAnswer.isBlank()) return InitialLoginResult.Error("Security answer is required")

        return if (pinSecurityStore.createPinSecurity(pin, securityQuestion.trim(), securityAnswer.trim())) {
            InitialLoginResult.Success
        } else {
            InitialLoginResult.Error("PIN setup has already been completed")
        }
    }

    fun authenticate(pin: String): InitialLoginResult {
        if (isFirstTimeSetup()) return InitialLoginResult.Error("Set up your PIN before signing in")
        if (pin.length < MIN_PIN_LENGTH || !pin.all(Char::isDigit)) {
            return InitialLoginResult.Error("PIN must be at least 4 digits")
        }
        return if (pinSecurityStore.verifyPin(pin)) {
            InitialLoginResult.Success
        } else {
            InitialLoginResult.Error("Invalid PIN")
        }
    }

    fun recoverPin(securityAnswer: String, newPin: String, confirmNewPin: String): InitialLoginResult {
        if (isFirstTimeSetup()) return InitialLoginResult.Error("Set up your PIN before recovery")
        if (securityAnswer.isBlank()) return InitialLoginResult.Error("Security answer is required")
        val pinValidation = validatePin(newPin, confirmNewPin)
        if (pinValidation != null) return pinValidation
        if (!pinSecurityStore.verifySecurityAnswer(securityAnswer.trim())) {
            return InitialLoginResult.Error("Incorrect security answer")
        }
        return if (pinSecurityStore.resetPin(newPin)) {
            InitialLoginResult.Success
        } else {
            InitialLoginResult.Error("Unable to reset PIN")
        }
    }

    suspend fun wipeData(confirm: Boolean): InitialLoginResult {
        if (!confirm) return InitialLoginResult.Error("Data wipe cancelled")
        wipeAllData()
        return InitialLoginResult.Success
    }

    private fun validatePin(pin: String, confirmPin: String): InitialLoginResult.Error? {
        if (pin.length < MIN_PIN_LENGTH || !pin.all(Char::isDigit)) {
            return InitialLoginResult.Error("PIN must be at least 4 digits")
        }
        if (pin != confirmPin) return InitialLoginResult.Error("PINs do not match")
        return null
    }

    companion object {
        private const val MIN_PIN_LENGTH = 4
    }
}
