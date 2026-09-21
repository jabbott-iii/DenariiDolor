/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            pin = "123456",
            pinConfirmation = "123456",
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
        service.setupProfile("123456", "123456", "Question?", "answer")

        val result = service.setupProfile("123456", "123456", "Question?", "answer")

        assertEquals(SetupProfileResult.ALREADY_CONFIGURED, result)
    }

    @Test
    fun pinVerificationSucceedsAndFails() {
        val service = SecurityProfileService(InMemorySecurityProfileStore())
        service.setupProfile("123456", "123456", "Question?", "answer")

        assertTrue(service.verifyPin("123456"))
        assertFalse(service.verifyPin("555555"))
    }

    @Test
    fun recoveryRejectsIncorrectAnswerWithoutChangingPin() {
        val service = SecurityProfileService(InMemorySecurityProfileStore())
        service.setupProfile("123456", "123456", "Question?", "answer")

        val result = service.recoverPin(
            securityAnswer = "wrong",
            newPin = "567890",
            pinConfirmation = "567890"
        )

        assertEquals(RecoverPinResult.INVALID_SECURITY_ANSWER, result)
        assertTrue(service.verifyPin("123456"))
        assertFalse(service.verifyPin("567890"))
    }

    @Test
    fun recoveryWithCorrectAnswerReplacesPin() {
        val service = SecurityProfileService(InMemorySecurityProfileStore())
        service.setupProfile("123456", "123456", "Question?", "answer")

        val result = service.recoverPin(
            securityAnswer = "answer",
            newPin = "567890",
            pinConfirmation = "567890"
        )

        assertEquals(RecoverPinResult.SUCCESS, result)
        assertFalse(service.verifyPin("123456"))
        assertTrue(service.verifyPin("567890"))
    }

    @Test
    fun wipeClearsConfiguredState() {
        val service = SecurityProfileService(InMemorySecurityProfileStore())
        service.setupProfile("123456", "123456", "Question?", "answer")

        service.wipeAll()

        assertFalse(service.isProfileConfigured())
        assertFalse(service.verifyPin("123456"))
        assertEquals(ProfileMode.FIRST_TIME_SETUP, service.getProfileState().mode)
    }

    @Test
    fun cancelingWipeKeepsExistingCredentialsUntouched() {
        val service = SecurityProfileService(InMemorySecurityProfileStore())
        service.setupProfile("123456", "123456", "Question?", "answer")

        // Simulates user canceling the destructive dialog by not invoking wipeAll().
        assertTrue(service.isProfileConfigured())
        assertTrue(service.verifyPin("123456"))
    }

    @Test
    fun legacyPinRequiresMatchingPinToMigrateAndClearsLegacyAfterSuccess() {
        val store = InMemorySecurityProfileStore().apply {
            edit { putString(SecurityProfileService.KEY_LEGACY_PIN, "246801") }
        }
        val service = SecurityProfileService(store)

        val mismatch = service.setupProfile(
            pin = "123456",
            pinConfirmation = "123456",
            securityQuestion = "Question?",
            securityAnswer = "answer"
        )
        assertEquals(SetupProfileResult.LEGACY_PIN_MISMATCH, mismatch)
        assertNotNull(store.getString(SecurityProfileService.KEY_LEGACY_PIN))

        val success = service.setupProfile(
            pin = "246801",
            pinConfirmation = "246801",
            securityQuestion = "Question?",
            securityAnswer = "answer"
        )

        assertEquals(SetupProfileResult.SUCCESS, success)
        assertNull(store.getString(SecurityProfileService.KEY_LEGACY_PIN))
        assertTrue(service.verifyPin("246801"))
    }

    @Test
    fun pinMustBeSixToTwelveDigits() {
        val service = SecurityProfileService(InMemorySecurityProfileStore())

        assertEquals(SetupProfileResult.INVALID_PIN_FORMAT, service.setupProfile("12345", "12345", "Question?", "answer"))
        assertEquals(SetupProfileResult.INVALID_PIN_FORMAT, service.setupProfile("1234567890123", "1234567890123", "Question?", "answer"))
        assertEquals(SetupProfileResult.INVALID_PIN_FORMAT, service.setupProfile("12345a", "12345a", "Question?", "answer"))
        assertEquals(SetupProfileResult.SUCCESS, service.setupProfile("123456789012", "123456789012", "Question?", "answer"))

        assertEquals(RecoverPinResult.INVALID_PIN_FORMAT, service.recoverPin("answer", "12345", "12345"))
        assertTrue(service.verifyPin("123456789012"))
    }

    private var now = 1_000_000L

    private fun lockableService(): SecurityProfileService = SecurityProfileService(InMemorySecurityProfileStore(), clock = { now }).apply {
        setupProfile("123456", "123456", "Question?", "answer")
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
            assertEquals(PinAttemptResult.Invalid(attemptsBeforeLockout = 4 - attempt), service.attemptPin("000000"))
        }
        assertEquals(PinAttemptResult.LockedOut(30_000L), service.attemptPin("000000"))

        now += 10_000L
        assertEquals(PinAttemptResult.LockedOut(20_000L), service.attemptPin("123456"))
    }

    @Test
    fun lockoutExpiresAndNextFailureDoubles() {
        val service = lockableService()
        repeat(5) { service.attemptPin("000000") }

        now += 30_000L
        assertEquals(PinAttemptResult.LockedOut(60_000L), service.attemptPin("000000"))
    }

    @Test
    fun successResetsFailureCounter() {
        val service = lockableService()
        repeat(4) { service.attemptPin("000000") }

        assertEquals(PinAttemptResult.Success, service.attemptPin("123456"))
        assertEquals(PinAttemptResult.Invalid(attemptsBeforeLockout = 4), service.attemptPin("000000"))
    }

    @Test
    fun biometricSuccessResetsLockout() {
        val service = lockableService()
        repeat(5) { service.attemptPin("000000") }

        service.recordSuccessfulAuthentication()

        assertEquals(0L, service.lockoutRemainingMillis())
        assertEquals(PinAttemptResult.Success, service.attemptPin("123456"))
    }

    @Test
    fun clockRollbackDoesNotShortenLockout() {
        val service = lockableService()
        repeat(5) { service.attemptPin("000000") }

        now -= 3_600_000L

        assertEquals(30_000L, service.lockoutRemainingMillis())
    }

    @Test
    fun wrongSecurityAnswersShareTheLockout() {
        val service = lockableService()
        repeat(4) { assertEquals(RecoverPinResult.INVALID_SECURITY_ANSWER, service.recoverPin("wrong", "567890", "567890")) }

        assertEquals(RecoverPinResult.LOCKED_OUT, service.recoverPin("wrong", "567890", "567890"))
        assertEquals(RecoverPinResult.LOCKED_OUT, service.recoverPin("answer", "567890", "567890"))
        assertTrue(service.attemptPin("123456") is PinAttemptResult.LockedOut)
    }

    @Test
    fun successfulRecoveryClearsLockout() {
        val service = lockableService()
        repeat(4) { service.attemptPin("000000") }

        assertEquals(RecoverPinResult.SUCCESS, service.recoverPin("answer", "567890", "567890"))
        assertEquals(PinAttemptResult.Invalid(attemptsBeforeLockout = 4), service.attemptPin("000000"))
    }

    @Test
    fun wipeClearsLockout() {
        val service = lockableService()
        repeat(5) { service.attemptPin("000000") }

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
