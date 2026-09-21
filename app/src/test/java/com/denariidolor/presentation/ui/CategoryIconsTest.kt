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

package com.denariidolor.presentation.ui

import com.denariidolor.presentation.ui.common.CategoryIcons
import com.denariidolor.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryIconsTest {
    @Test
    fun keysAreUniqueAndIncludeDefault() {
        val keys = CategoryIcons.options.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
        assertTrue(CategoryIcons.isKnown(CategoryIcons.DEFAULT_KEY))
    }

    @Test
    fun unknownOrNullKeysFallBackToDefault() {
        assertEquals(CategoryIcons.DEFAULT_KEY, CategoryIcons.forKey("nope").key)
        assertEquals(CategoryIcons.DEFAULT_KEY, CategoryIcons.forKey(null).key)
    }

    @Test
    fun seededCategoriesUseKnownIcons() {
        Constants.DEFAULT_CATEGORIES.forEach { assertTrue(it.iconName, CategoryIcons.isKnown(it.iconName)) }
    }
}
