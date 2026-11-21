/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatePromptPolicyTest {
    private fun saveTimes(times: Int, start: RatePromptCounters = RatePromptCounters()): Pair<RatePromptCounters, List<Int>> {
        var counters = start
        val askedAt = mutableListOf<Int>()
        repeat(times) {
            val decision = RatePromptPolicy.afterSave(counters)
            counters = decision.counters
            if (decision.ask) askedAt += counters.saves
        }
        return counters to askedAt
    }

    @Test
    fun asksFirstOnTheTenthSave() {
        val (counters, askedAt) = saveTimes(RatePromptPolicy.FIRST_ASK_AFTER_SAVES)

        assertEquals(listOf(RatePromptPolicy.FIRST_ASK_AFTER_SAVES), askedAt)
        assertEquals(1, counters.timesAsked)
    }

    @Test
    fun asksOnceMoreLaterAndThenNeverAgain() {
        val (counters, askedAt) = saveTimes(100)

        assertEquals(listOf(RatePromptPolicy.FIRST_ASK_AFTER_SAVES, RatePromptPolicy.SECOND_ASK_AFTER_SAVES), askedAt)
        assertEquals(2, counters.timesAsked)
        // Counting stops once no more prompts can follow.
        assertEquals(RatePromptPolicy.SECOND_ASK_AFTER_SAVES, counters.saves)
    }

    @Test
    fun neverAsksAfterTheUserFinished() {
        val (counters, askedAt) = saveTimes(100, RatePromptCounters(saves = 3, finished = true))

        assertTrue(askedAt.isEmpty())
        assertEquals(3, counters.saves)
    }

    @Test
    fun doesNotAskBeforeTheThreshold() {
        val decision = RatePromptPolicy.afterSave(RatePromptCounters(saves = RatePromptPolicy.FIRST_ASK_AFTER_SAVES - 2))

        assertFalse(decision.ask)
        assertEquals(RatePromptPolicy.FIRST_ASK_AFTER_SAVES - 1, decision.counters.saves)
    }
}
