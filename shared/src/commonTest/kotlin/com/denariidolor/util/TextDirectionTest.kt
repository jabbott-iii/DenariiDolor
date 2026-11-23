/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextDirectionTest {
    @Test
    fun knowsRightToLeftLanguages() {
        assertTrue(isRightToLeft(localeOf("ar-EG")))
        assertTrue(isRightToLeft(localeOf("he")))
        assertFalse(isRightToLeft(localeOf("en-US")))
        assertFalse(isRightToLeft(localeOf("hi-IN")))
    }

    @Test
    fun transferArrowPointsTowardTheDestination() {
        assertEquals("Cash → Savings", transferRoute("Cash", "Savings", localeOf("en-US")))
        assertEquals("النقد ← المدخرات", transferRoute("النقد", "المدخرات", localeOf("ar")))
    }
}
