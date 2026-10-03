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

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacySecurityProfileTest {
    private val store = InMemorySecurityProfileStore()
    private val profile = LegacySecurityProfile(store)

    /** Writes a profile the way v1.0.x did: PBKDF2 hashes of the PIN and the trimmed answer. */
    private fun writeV1Profile(pin: String, question: String, answer: String) = store.edit {
        putBoolean(LegacySecurityProfile.KEY_PROFILE_CONFIGURED, true)
        putString(LegacySecurityProfile.KEY_SECURITY_QUESTION, question)
        putHash(pin, LegacySecurityProfile.KEY_PIN_HASH, LegacySecurityProfile.KEY_PIN_SALT, LegacySecurityProfile.KEY_PIN_ITERATIONS)
        putHash(
            answer.trim(),
            LegacySecurityProfile.KEY_SECURITY_ANSWER_HASH,
            LegacySecurityProfile.KEY_SECURITY_ANSWER_SALT,
            LegacySecurityProfile.KEY_SECURITY_ANSWER_ITERATIONS
        )
    }

    private fun SecurityProfileStoreEditor.putHash(secret: String, hashKey: String, saltKey: String, iterationsKey: String) {
        val salt = ByteArray(16) { it.toByte() }
        putString(hashKey, Base64.getEncoder().encodeToString(pbkdf2(secret, salt, ITERATIONS)))
        putString(saltKey, Base64.getEncoder().encodeToString(salt))
        putInt(iterationsKey, ITERATIONS)
    }

    @Test
    fun verifiesPinAndTrimmedCaseSensitiveAnswer() {
        writeV1Profile("246801", "Favorite color?", " Blue ")

        assertTrue(profile.isConfigured())
        assertEquals("Favorite color?", profile.securityQuestion())
        assertTrue(profile.verifyPin("246801"))
        assertFalse(profile.verifyPin("246802"))
        assertTrue(profile.verifyAnswer("Blue  "))
        assertFalse(profile.verifyAnswer("blue"))
        assertFalse(profile.verifyAnswer("   "))
    }

    @Test
    fun incompleteProfileIsNotConfigured() {
        writeV1Profile("246801", "Favorite color?", "Blue")
        store.edit { remove(LegacySecurityProfile.KEY_PIN_SALT) }

        assertFalse(profile.isConfigured())
        assertFalse(profile.verifyPin("246801"))
    }

    private companion object {
        const val ITERATIONS = SecurityProfileService.MIN_ITERATIONS
    }
}
