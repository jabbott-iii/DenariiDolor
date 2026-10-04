/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityProfileServiceTest {
    private val store = InMemorySecurityProfileStore()
    private val clock = FakeMonotonicClock()
    private val mixer = FakeDeviceKeyMixer()
    private val service = SecurityProfileService(store, mixer, clock, iterations = SecurityProfileService.MIN_ITERATIONS)

    private fun configured(): SecurityProfileService = service.apply {
        assertEquals(SetupProfileResult.SUCCESS, setupProfile(PIN, PIN, QUESTION, ANSWER))
    }

    private fun SecurityProfileService.unlockedKey(pin: String): ByteArray? = (unlockWithPin(pin) as? AttemptResult.Accepted)?.value

    private fun failures() = store.getInt(SecurityProfileService.KEY_FAILED_ATTEMPTS, 0)

    @Test
    fun setupProfileSucceedsOnceAndConfiguresSignIn() {
        configured()

        assertEquals(SecurityProfileState(ProfileMode.SIGN_IN, QUESTION), service.getProfileState())
        assertEquals(SetupProfileResult.ALREADY_CONFIGURED, service.setupProfile(PIN, PIN, QUESTION, ANSWER))
    }

    @Test
    fun newProfileStartsInSetupMode() {
        assertEquals(SecurityProfileState(ProfileMode.FIRST_TIME_SETUP, null), service.getProfileState())
    }

    @Test
    fun correctPinAlwaysUnwrapsTheSameDatabaseKey() {
        configured()

        val first = service.unlockedKey(PIN)
        val second = service.unlockedKey(PIN)

        assertEquals(SecurityProfileService.DATABASE_KEY_BYTES, first?.size)
        assertArrayEquals(first, second)
        assertNull(service.unlockedKey("654321"))
    }

    @Test
    fun storedProfileContainsNoPlainSecretsOrDatabaseKey() {
        configured()
        val key = requireNotNull(service.unlockedKey(PIN))
        val encodedKey = Base64.getEncoder().encodeToString(key)

        val stored = store.values.values.filterIsInstance<String>()

        assertFalse(stored.any { it.contains(PIN) || it.contains(SecurityProfileService.normalizeAnswer(ANSWER)) })
        assertFalse(stored.any { it.contains(encodedKey) })
    }

    @Test
    fun keyCannotBeUnwrappedWithAnotherDeviceKey() {
        configured()
        val otherDevice = SecurityProfileService(store, FakeDeviceKeyMixer(ByteArray(32) { 2 }), clock)

        assertTrue(otherDevice.unlockWithPin(PIN) is AttemptResult.Rejected)
    }

    @Test
    fun missingDeviceKeyIsAStorageFailureNotAWrongPin() {
        configured()
        mixer.available = false

        assertThrows(SecureStorageException::class.java) { service.unlockWithPin(PIN) }
        assertEquals(0, failures())
    }

    @Test
    fun pinMustBeSixToTwelveDigits() {
        assertEquals(SetupProfileResult.INVALID_PIN_FORMAT, service.setupProfile("12345", "12345", QUESTION, ANSWER))
        assertEquals(SetupProfileResult.INVALID_PIN_FORMAT, service.setupProfile("1234567890123", "1234567890123", QUESTION, ANSWER))
        assertEquals(SetupProfileResult.INVALID_PIN_FORMAT, service.setupProfile("12345a", "12345a", QUESTION, ANSWER))
        assertEquals(SetupProfileResult.PIN_MISMATCH, service.setupProfile(PIN, "123457", QUESTION, ANSWER))
        assertEquals(SetupProfileResult.SUCCESS, service.setupProfile("123456789012", "123456789012", QUESTION, ANSWER))

        assertEquals(RecoverPinResult.INVALID_PIN_FORMAT, service.recoverPin(ANSWER, "12345", "12345"))
        assertTrue(service.unlockWithPin("123456789012") is AttemptResult.Accepted)
    }

    @Test
    fun recoveryAnswersMustBeHardToGuess() {
        fun check(question: String, answer: String) = SecurityProfileService.validateSetup(PIN, PIN, question, answer)

        assertEquals(SetupProfileResult.SECURITY_QUESTION_REQUIRED, check("   ", ANSWER))
        assertEquals(SetupProfileResult.SECURITY_QUESTION_TOO_LONG, check("q".repeat(201), ANSWER))
        assertEquals(SetupProfileResult.SECURITY_ANSWER_REQUIRED, check(QUESTION, "   "))
        assertEquals(SetupProfileResult.SECURITY_ANSWER_TOO_SHORT, check(QUESTION, "Rex"))
        assertEquals(SetupProfileResult.SECURITY_ANSWER_TOO_SHORT, check(QUESTION, "  a   b  c "))
        assertEquals(SetupProfileResult.SECURITY_ANSWER_TOO_LONG, check(QUESTION, "a".repeat(101)))
        assertEquals(SetupProfileResult.SECURITY_ANSWER_IN_QUESTION, check("Is it Rex the dog?", "REX the   dog"))
        assertEquals(SetupProfileResult.SECURITY_ANSWER_MATCHES_PIN, check(QUESTION, PIN))
        assertEquals(SetupProfileResult.SUCCESS, check(QUESTION, "abcdef"))
        assertEquals(SetupProfileResult.SUCCESS, check(QUESTION, "🐶".repeat(6)))
        assertEquals(SetupProfileResult.SECURITY_ANSWER_TOO_SHORT, service.setupProfile(PIN, PIN, QUESTION, "short"))
        assertEquals(ProfileMode.FIRST_TIME_SETUP, service.getProfileState().mode)
    }

    @Test
    fun answersIgnoreCaseSpacingAndUnicodeForm() {
        configured()

        assertEquals(RecoverPinResult.SUCCESS, service.recoverPin("  lincoln   ELEMENTARY ", "567890", "567890"))
        assertEquals(RecoverPinResult.SUCCESS, service.recoverPin("Ｌｉｎｃｏｌｎ Elementary", "567891", "567891"))
        assertEquals("lincoln elementary", SecurityProfileService.normalizeAnswer("ＬＩＮＣＯＬＮ Elementary"))
    }

    @Test
    fun recoveryRejectsIncorrectAnswerWithoutChangingPin() {
        configured()

        assertEquals(RecoverPinResult.INVALID_SECURITY_ANSWER, service.recoverPin("wrong answer", "567890", "567890"))
        assertTrue(service.unlockWithPin(PIN) is AttemptResult.Accepted)
        assertTrue(service.unlockWithPin("567890") is AttemptResult.Rejected)
    }

    @Test
    fun recoveryReplacesPinAndKeepsTheDatabaseKey() {
        configured()
        val original = service.unlockedKey(PIN)

        assertEquals(RecoverPinResult.SUCCESS, service.recoverPin(ANSWER, "567890", "567890"))

        assertNull(service.unlockedKey(PIN))
        assertArrayEquals(original, service.unlockedKey("567890"))
    }

    @Test
    fun migratedProfileWrapsTheExistingKey() {
        val legacyKey = ByteArray(SecurityProfileService.DATABASE_KEY_BYTES) { it.toByte() }

        assertEquals(SetupProfileResult.SECURITY_ANSWER_TOO_SHORT, service.migrateProfile(PIN, QUESTION, "blue", legacyKey))
        assertEquals(ProfileMode.FIRST_TIME_SETUP, service.getProfileState().mode)
        assertEquals(SetupProfileResult.SUCCESS, service.migrateProfile(PIN, QUESTION, ANSWER, legacyKey))

        assertArrayEquals(legacyKey, service.unlockedKey(PIN))
    }

    @Test
    fun biometricWrapIsStoredAndCleared() {
        configured()
        val wrapped = byteArrayOf(1, 2, 3)

        service.setBiometricWrap(wrapped)
        assertArrayEquals(wrapped, service.biometricWrap())

        service.setBiometricWrap(null)
        assertNull(service.biometricWrap())
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
        configured()

        repeat(4) { attempt ->
            assertEquals(AttemptResult.Rejected(attemptsBeforeLockout = 4 - attempt), service.unlockWithPin("000000"))
        }
        assertEquals(AttemptResult.LockedOut(30_000L), service.unlockWithPin("000000"))

        clock.elapsed += 10_000L
        assertEquals(AttemptResult.LockedOut(20_000L), service.unlockWithPin(PIN))
    }

    @Test
    fun lockoutEndsOnlyWithElapsedTime() {
        configured()
        repeat(5) { service.unlockWithPin("000000") }

        // However the device's date is changed, elapsed time doesn't move, so no PIN is checked (CS-13).
        repeat(25) { assertTrue(service.unlockWithPin(PIN) is AttemptResult.LockedOut) }
        assertEquals(5, failures())

        clock.elapsed += 29_999L
        assertEquals(1L, service.lockoutRemainingMillis())
        clock.elapsed += 1L
        assertTrue(service.unlockWithPin(PIN) is AttemptResult.Accepted)
    }

    @Test
    fun lockoutExpiresAndNextFailureDoubles() {
        configured()
        repeat(5) { service.unlockWithPin("000000") }

        clock.elapsed += 30_000L
        assertEquals(AttemptResult.LockedOut(60_000L), service.unlockWithPin("000000"))
    }

    @Test
    fun rebootRestartsTheLockout() {
        configured()
        repeat(5) { service.unlockWithPin("000000") }
        clock.elapsed += 20_000L

        clock.elapsed = 5_000L
        clock.boot = 8

        assertEquals(30_000L, service.lockoutRemainingMillis())
        clock.elapsed += 30_000L
        assertEquals(0L, service.lockoutRemainingMillis())
    }

    @Test
    fun rebootIsDetectedWithoutABootCount() {
        clock.boot = null
        configured()
        repeat(5) { service.unlockWithPin("000000") }

        clock.elapsed = 1_000L

        assertEquals(30_000L, service.lockoutRemainingMillis())
    }

    @Test
    fun bootCountChangeRestartsTheLockoutEvenAfterLongUptime() {
        configured()
        repeat(5) { service.unlockWithPin("000000") }

        clock.elapsed += 25_000L
        clock.boot = 8

        assertEquals(30_000L, service.lockoutRemainingMillis())
    }

    @Test
    fun servedLockoutIsNotRestartedByAReboot() {
        configured()
        repeat(5) { service.unlockWithPin("000000") }
        clock.elapsed += 30_000L
        assertEquals(0L, service.lockoutRemainingMillis())

        clock.elapsed = 1_000L
        clock.boot = 8

        assertEquals(0L, service.lockoutRemainingMillis())
        assertEquals(AttemptResult.LockedOut(60_000L), service.unlockWithPin("000000"))
    }

    @Test
    fun successResetsFailureCounter() {
        configured()
        repeat(4) { service.unlockWithPin("000000") }

        assertTrue(service.unlockWithPin(PIN) is AttemptResult.Accepted)
        assertEquals(AttemptResult.Rejected(attemptsBeforeLockout = 4), service.unlockWithPin("000000"))
    }

    @Test
    fun biometricSuccessResetsLockout() {
        configured()
        repeat(5) { service.unlockWithPin("000000") }

        service.recordSuccessfulAuthentication()

        assertEquals(0L, service.lockoutRemainingMillis())
        assertTrue(service.unlockWithPin(PIN) is AttemptResult.Accepted)
    }

    @Test
    fun wrongSecurityAnswersShareTheLockout() {
        configured()
        repeat(4) { assertEquals(RecoverPinResult.INVALID_SECURITY_ANSWER, service.recoverPin("wrong answer", "567890", "567890")) }

        assertEquals(RecoverPinResult.LOCKED_OUT, service.recoverPin("wrong answer", "567890", "567890"))
        assertEquals(RecoverPinResult.LOCKED_OUT, service.recoverPin(ANSWER, "567890", "567890"))
        assertTrue(service.unlockWithPin(PIN) is AttemptResult.LockedOut)
    }

    @Test
    fun successfulRecoveryClearsLockout() {
        configured()
        repeat(4) { service.unlockWithPin("000000") }

        assertEquals(RecoverPinResult.SUCCESS, service.recoverPin(ANSWER, "567890", "567890"))
        assertEquals(AttemptResult.Rejected(attemptsBeforeLockout = 4), service.unlockWithPin("000000"))
    }

    @Test
    fun customChecksShareTheLockout() {
        configured()

        repeat(5) { service.attempt<Unit> { null } }

        assertTrue(service.unlockWithPin(PIN) is AttemptResult.LockedOut)
    }

    @Test
    fun concurrentWrongAttemptsAreAllCounted() {
        configured()

        val threads = List(4) { Thread { service.unlockWithPin("000000") } }
        threads.forEach(Thread::start)
        threads.forEach(Thread::join)

        assertEquals(4, failures())
    }

    private companion object {
        const val PIN = "123456"
        const val QUESTION = "Name of my first school?"
        const val ANSWER = "Lincoln Elementary"
    }
}
