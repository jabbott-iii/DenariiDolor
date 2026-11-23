/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui

import com.denariidolor.R
import com.denariidolor.presentation.ui.common.PluralKey
import com.denariidolor.presentation.ui.common.StringKey
import com.denariidolor.presentation.ui.common.resId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Shared code names text by key; Android reads the resource of the same name (StringKey.BACKUP_SAVED is R.string.backup_saved).
 * The map between them is written by hand, so this catches a key wired to the wrong string or left out.
 */
class StringKeyResourcesTest {
    @Test
    fun everyStringKeyReadsTheStringOfItsName() {
        val miswired = StringKey.entries.filterNot { wired(R.string::class.java, it.name) { it.resId } }

        assertEquals(emptyList<StringKey>(), miswired)
    }

    @Test
    fun everyPluralKeyReadsThePluralOfItsName() {
        val miswired = PluralKey.entries.filterNot { wired(R.plurals::class.java, it.name) { it.resId } }

        assertEquals(emptyList<PluralKey>(), miswired)
    }

    /** Whether [resId] gives the resource that R's [type] holds under [key]'s name; a name with no resource is never wired. */
    private fun wired(type: Class<*>, key: String, resId: () -> Int): Boolean {
        val expected = runCatching { type.getField(key.lowercase()).getInt(null) }.getOrNull() ?: return false
        return runCatching(resId).getOrNull() == expected
    }
}
