/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
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
