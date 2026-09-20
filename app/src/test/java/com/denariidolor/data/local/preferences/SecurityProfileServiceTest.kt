package com.denariidolor.data.local.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityProfileServiceTest {

    @Test
    fun setupProfileSucceedsOnceAndConfiguresSignIn() {
        val service = SecurityProfileService(InMemorySecurityProfileStore())

        val result = service.setupProfile(
            pin = "1234",
            pinConfirmation = "1234",
            securityQuestion = "Favorite color?",
            securityAnswer = "blue"
        )

        assertEquals(SetupProfileResult.SUCCESS, result)
        assertTrue(service.isProfileConfigured())
        assertEquals(ProfileMode.SIGN_IN, service.getProfileState().mode)
    }

    @Test
    fun repeatedSetupIsRejected() {
        val service = SecurityProfileService(InMemorySecurityProfileStore())
        service.setupProfile("1234", "1234", "Question?", "answer")

        val result = service.setupProfile("1234", "1234", "Question?", "answer")

        assertEquals(SetupProfileResult.ALREADY_CONFIGURED, result)
    }

    @Test
    fun pinVerificationSucceedsAndFails() {
        val service = SecurityProfileService(InMemorySecurityProfileStore())
        service.setupProfile("1234", "1234", "Question?", "answer")

        assertTrue(service.verifyPin("1234"))
        assertFalse(service.verifyPin("5555"))
    }

    @Test
    fun recoveryRejectsIncorrectAnswerWithoutChangingPin() {
        val service = SecurityProfileService(InMemorySecurityProfileStore())
        service.setupProfile("1234", "1234", "Question?", "answer")

        val result = service.recoverPin(
            securityAnswer = "wrong",
            newPin = "5678",
            pinConfirmation = "5678"
        )

        assertEquals(RecoverPinResult.INVALID_SECURITY_ANSWER, result)
        assertTrue(service.verifyPin("1234"))
        assertFalse(service.verifyPin("5678"))
    }

    @Test
    fun recoveryWithCorrectAnswerReplacesPin() {
        val service = SecurityProfileService(InMemorySecurityProfileStore())
        service.setupProfile("1234", "1234", "Question?", "answer")

        val result = service.recoverPin(
            securityAnswer = "answer",
            newPin = "5678",
            pinConfirmation = "5678"
        )

        assertEquals(RecoverPinResult.SUCCESS, result)
        assertFalse(service.verifyPin("1234"))
        assertTrue(service.verifyPin("5678"))
    }

    @Test
    fun wipeClearsConfiguredState() {
        val service = SecurityProfileService(InMemorySecurityProfileStore())
        service.setupProfile("1234", "1234", "Question?", "answer")

        service.wipeAll()

        assertFalse(service.isProfileConfigured())
        assertFalse(service.verifyPin("1234"))
        assertEquals(ProfileMode.FIRST_TIME_SETUP, service.getProfileState().mode)
    }

    @Test
    fun cancelingWipeKeepsExistingCredentialsUntouched() {
        val service = SecurityProfileService(InMemorySecurityProfileStore())
        service.setupProfile("1234", "1234", "Question?", "answer")

        // Simulates user canceling the destructive dialog by not invoking wipeAll().
        assertTrue(service.isProfileConfigured())
        assertTrue(service.verifyPin("1234"))
    }

    @Test
    fun legacyPinRequiresMatchingPinToMigrateAndClearsLegacyAfterSuccess() {
        val store = InMemorySecurityProfileStore().apply {
            putString(SecurityProfileService.KEY_LEGACY_PIN, "2468")
        }
        val service = SecurityProfileService(store)

        val mismatch = service.setupProfile(
            pin = "1234",
            pinConfirmation = "1234",
            securityQuestion = "Question?",
            securityAnswer = "answer"
        )
        assertEquals(SetupProfileResult.LEGACY_PIN_MISMATCH, mismatch)
        assertNotNull(store.getString(SecurityProfileService.KEY_LEGACY_PIN))

        val success = service.setupProfile(
            pin = "2468",
            pinConfirmation = "2468",
            securityQuestion = "Question?",
            securityAnswer = "answer"
        )

        assertEquals(SetupProfileResult.SUCCESS, success)
        assertNull(store.getString(SecurityProfileService.KEY_LEGACY_PIN))
        assertTrue(service.verifyPin("2468"))
    }

    private class InMemorySecurityProfileStore : SecurityProfileStore {
        private val stringValues = mutableMapOf<String, String>()
        private val intValues = mutableMapOf<String, Int>()
        private val booleanValues = mutableMapOf<String, Boolean>()

        override fun getString(key: String): String? = stringValues[key]

        override fun getInt(key: String, defaultValue: Int): Int = intValues[key] ?: defaultValue

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = booleanValues[key] ?: defaultValue

        override fun putString(key: String, value: String) {
            stringValues[key] = value
        }

        override fun putInt(key: String, value: Int) {
            intValues[key] = value
        }

        override fun putBoolean(key: String, value: Boolean) {
            booleanValues[key] = value
        }

        override fun remove(key: String) {
            stringValues.remove(key)
            intValues.remove(key)
            booleanValues.remove(key)
        }

        override fun clear() {
            stringValues.clear()
            intValues.clear()
            booleanValues.clear()
        }
    }
}
