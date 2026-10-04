/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class EscapeLikeTest {
    @Test
    fun wildcardsAndTheEscapeCharacterMatchLiterally() {
        assertEquals("50\\%", escapeLike("50%"))
        assertEquals("\\_", escapeLike("_"))
        assertEquals("C:\\\\tmp", escapeLike("C:\\tmp"))
        assertEquals("Lunch", escapeLike("Lunch"))
    }
}
