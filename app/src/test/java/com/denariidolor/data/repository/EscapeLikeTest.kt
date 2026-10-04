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
