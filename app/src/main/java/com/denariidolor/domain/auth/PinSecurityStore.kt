package com.denariidolor.domain.auth

interface PinSecurityStore {
    fun isPinConfigured(): Boolean
    fun createPinSecurity(pin: String, securityQuestion: String, securityAnswer: String): Boolean
    fun verifyPin(pin: String): Boolean
    fun getSecurityQuestion(): String?
    fun verifySecurityAnswer(securityAnswer: String): Boolean
    fun resetPin(pin: String): Boolean
    fun clearPin()
}
