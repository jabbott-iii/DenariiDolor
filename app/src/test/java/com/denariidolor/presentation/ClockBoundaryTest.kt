/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Screens and ViewModels read the time only from the injected clock (rule 5), so tests can fix the time and its zone and
 * shared code can bring its own clock. On Android that clock is DeviceClock, the one place that reads the device's clock.
 */
class ClockBoundaryTest {
    @Test
    fun presentationReadsTheTimeOnlyFromTheInjectedClock() {
        val offenders = presentationSources().flatMap { file ->
            val text = file.readText()
            DEVICE_CLOCK_READS.filter { it in text }.map { "${file.name}: $it" }
        }

        assertEquals(emptyList<String>(), offenders)
    }

    /** Guards the test above against passing because it looked in the wrong place. */
    @Test
    fun theScanSeesTheScreensWithDateFields() {
        val names = presentationSources().map { it.name }.toSet()

        assertTrue(names.toString(), names.containsAll(listOf("TransactionFormScreen.kt", "SearchScreen.kt", "BackupViewModel.kt")))
    }

    private fun presentationSources(): List<File> {
        val root = listOf(File("src/main/java"), File("app/src/main/java"))
            .map { File(it, "com/denariidolor/presentation") }
            .first { it.isDirectory }
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private companion object {
        val DEVICE_CLOCK_READS = listOf(
            "Clock.System",
            "System.currentTimeMillis",
            "systemDefaultZone",
            "Instant.now()",
            "LocalDate.now()",
            "LocalDateTime.now()",
            "YearMonth.now()",
            "ZonedDateTime.now()"
        )
    }
}
