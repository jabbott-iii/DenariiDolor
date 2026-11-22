/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The stored security profile (CS-15), pinned byte for byte with values computed outside the app (hashlib and OpenSSL), so every
 * profile on a phone today still opens after a change to the crypto underneath it. The device key is [FakeDeviceKeyMixer]'s, so
 * only the format is pinned here; a phone's own key never leaves its Keystore.
 */
class SecurityProfileGoldenTest {
    @Test
    fun aNewProfileIsStoredExactlyAsBefore() {
        val store = InMemorySecurityProfileStore()
        val service =
            SecurityProfileService(
                store,
                FakeDeviceKeyMixer(),
                FakeMonotonicClock(),
                JcaCryptoPrimitives(CountingSecureRandom()),
                iterations = ITERATIONS
            )

        assertEquals(SetupProfileResult.SUCCESS, service.setupProfile(PIN, PIN, QUESTION, ANSWER))
        assertEquals(GOLDEN_PROFILE, store.values)
    }

    @Test
    fun aStoredProfileStillUnlocksWithItsPin() {
        val service = goldenService()

        assertArrayEquals(DATABASE_KEY, (service.unlockWithPin(PIN) as AttemptResult.Accepted).value)
        assertTrue(service.unlockWithPin("135791") is AttemptResult.Rejected)
    }

    /** The answer was stored normalized, so it opens however it is typed now. */
    @Test
    fun aStoredProfileStillRecoversWithItsAnswer() {
        val service = goldenService()

        assertEquals(RecoverPinResult.SUCCESS, service.recoverPin("CAFÉ MÜLLER", NEW_PIN, NEW_PIN))
        assertArrayEquals(DATABASE_KEY, (service.unlockWithPin(NEW_PIN) as AttemptResult.Accepted).value)
    }

    private fun goldenService(): SecurityProfileService {
        val store = InMemorySecurityProfileStore().apply { values.putAll(GOLDEN_PROFILE) }
        return SecurityProfileService(store, FakeDeviceKeyMixer(), FakeMonotonicClock(), JcaCryptoPrimitives())
    }

    private companion object {
        const val PIN = "135790"
        const val NEW_PIN = "246801"
        const val QUESTION = "  Where did you first work?  "

        /** Decomposed é and fullwidth Ｍ: stored as `café müller`. */
        const val ANSWER = "  Cafe\u0301   \uFF2D\u00FCller "
        const val ITERATIONS = 10_000

        /** The first 32 bytes [CountingSecureRandom] hands out; then the PIN's salt and nonce, then the answer's. */
        val DATABASE_KEY = ByteArray(32) { it.toByte() }

        val GOLDEN_PROFILE: Map<String, Any> = mapOf(
            SecurityProfileService.KEY_PROFILE_VERSION to 2,
            SecurityProfileService.KEY_SECURITY_QUESTION to "Where did you first work?",
            "pin_wrap" to "MDEyMzQ1Njc4OTo70uWPgrZgoUIUfXebxIDKe1H5cqj8jvxFL+A2KBDHGDskUSRwqd5TZFu5DhcKJ402",
            "pin_salt" to "ICEiIyQlJicoKSorLC0uLw==",
            "pin_iterations" to ITERATIONS,
            "answer_wrap" to "TE1OT1BRUlNUVVZX8dywEEsjR39KbgztRv23mdc3s28PojP36mQQXe8/a6eidECrd1aHbtlp52VoZHBj",
            "answer_salt" to "PD0+P0BBQkNERUZHSElKSw==",
            "answer_iterations" to ITERATIONS
        )
    }
}
