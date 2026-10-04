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

package com.denariidolor.util

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextDirectionTest {
    @Test
    fun knowsRightToLeftLanguages() {
        assertTrue(isRightToLeft(Locale("ar", "EG")))
        assertTrue(isRightToLeft(Locale("he")))
        assertFalse(isRightToLeft(Locale.US))
        assertFalse(isRightToLeft(Locale("hi", "IN")))
    }

    @Test
    fun transferArrowPointsTowardTheDestination() {
        assertEquals("Cash → Savings", transferRoute("Cash", "Savings", Locale.US))
        assertEquals("النقد ← المدخرات", transferRoute("النقد", "المدخرات", Locale("ar")))
    }
}
