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
