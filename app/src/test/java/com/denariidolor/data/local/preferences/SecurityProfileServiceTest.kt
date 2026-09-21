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
            edit { putString(SecurityProfileService.KEY_LEGACY_PIN, "2468") }
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

    private var now = 1_000_000L

    private fun lockableService(): SecurityProfileService =
        SecurityProfileService(InMemorySecurityProfileStore(), clock = { now }).apply {
            setupProfile("1234", "1234", "Question?", "answer")
        }

    @Test
    fun lockoutDurationEscalatesAndCaps() {
        assertEquals(0L, SecurityProfileService.lockoutDurationMillis(4))
        assertEquals(30_000L, SecurityProfileService.lockoutDurationMillis(5))
        assertEquals(60_000L, SecurityProfileService.lockoutDurationMillis(6))
        assertEquals(480_000L, SecurityProfileService.lockoutDurationMillis(9))
        assertEquals(900_000L, SecurityProfileService.lockoutDurationMillis(10))
        assertEquals(900_000L, SecurityProfileService.lockoutDurationMillis(1_000))
    }

    @Test
    fun fifthWrongPinLocksOutAndBlocksCorrectPin() {
        val service = lockableService()

        repeat(4) { attempt ->
            assertEquals(PinAttemptResult.Invalid(attemptsBeforeLockout = 4 - attempt), service.attemptPin("0000"))
        }
        assertEquals(PinAttemptResult.LockedOut(30_000L), service.attemptPin("0000"))

        now += 10_000L
        assertEquals(PinAttemptResult.LockedOut(20_000L), service.attemptPin("1234"))
    }

    @Test
    fun lockoutExpiresAndNextFailureDoubles() {
        val service = lockableService()
        repeat(5) { service.attemptPin("0000") }

        now += 30_000L
        assertEquals(PinAttemptResult.LockedOut(60_000L), service.attemptPin("0000"))
    }

    @Test
    fun successResetsFailureCounter() {
        val service = lockableService()
        repeat(4) { service.attemptPin("0000") }

        assertEquals(PinAttemptResult.Success, service.attemptPin("1234"))
        assertEquals(PinAttemptResult.Invalid(attemptsBeforeLockout = 4), service.attemptPin("0000"))
    }

    @Test
    fun biometricSuccessResetsLockout() {
        val service = lockableService()
        repeat(5) { service.attemptPin("0000") }

        service.recordSuccessfulAuthentication()

        assertEquals(0L, service.lockoutRemainingMillis())
        assertEquals(PinAttemptResult.Success, service.attemptPin("1234"))
    }

    @Test
    fun clockRollbackDoesNotShortenLockout() {
        val service = lockableService()
        repeat(5) { service.attemptPin("0000") }

        now -= 3_600_000L

        assertEquals(30_000L, service.lockoutRemainingMillis())
    }

    @Test
    fun wrongSecurityAnswersShareTheLockout() {
        val service = lockableService()
        repeat(4) { assertEquals(RecoverPinResult.INVALID_SECURITY_ANSWER, service.recoverPin("wrong", "5678", "5678")) }

        assertEquals(RecoverPinResult.LOCKED_OUT, service.recoverPin("wrong", "5678", "5678"))
        assertEquals(RecoverPinResult.LOCKED_OUT, service.recoverPin("answer", "5678", "5678"))
        assertTrue(service.attemptPin("1234") is PinAttemptResult.LockedOut)
    }

    @Test
    fun successfulRecoveryClearsLockout() {
        val service = lockableService()
        repeat(4) { service.attemptPin("0000") }

        assertEquals(RecoverPinResult.SUCCESS, service.recoverPin("answer", "5678", "5678"))
        assertEquals(PinAttemptResult.Invalid(attemptsBeforeLockout = 4), service.attemptPin("0000"))
    }

    @Test
    fun wipeClearsLockout() {
        val service = lockableService()
        repeat(5) { service.attemptPin("0000") }

        service.wipeAll()

        assertEquals(0L, service.lockoutRemainingMillis())
    }

    private class InMemorySecurityProfileStore : SecurityProfileStore {
        private val stringValues = mutableMapOf<String, String>()
        private val intValues = mutableMapOf<String, Int>()
        private val longValues = mutableMapOf<String, Long>()
        private val booleanValues = mutableMapOf<String, Boolean>()

        override fun getString(key: String): String? = stringValues[key]

        override fun getInt(key: String, defaultValue: Int): Int = intValues[key] ?: defaultValue

        override fun getLong(key: String, defaultValue: Long): Long = longValues[key] ?: defaultValue

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = booleanValues[key] ?: defaultValue

        override fun edit(block: SecurityProfileStoreEditor.() -> Unit) {
            val editor = object : SecurityProfileStoreEditor {
                override fun putString(key: String, value: String) {
                    stringValues[key] = value
                }

                override fun putInt(key: String, value: Int) {
                    intValues[key] = value
                }

                override fun putLong(key: String, value: Long) {
                    longValues[key] = value
                }

                override fun putBoolean(key: String, value: Boolean) {
                    booleanValues[key] = value
                }

                override fun remove(key: String) {
                    stringValues.remove(key)
                    intValues.remove(key)
                    longValues.remove(key)
                    booleanValues.remove(key)
                }

                override fun clear() {
                    stringValues.clear()
                    intValues.clear()
                    longValues.clear()
                    booleanValues.clear()
                }
            }
            block(editor)
        }
    }
}
