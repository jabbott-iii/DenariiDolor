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
 * Code reads the time and its zone only from the injected clock (rule 5), so tests can fix both and shared code can bring its
 * own clock. On Android that clock is DeviceClock, the one place that reads the device's clock or time zone.
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

    @Test
    fun onlyDeviceClockReadsTheSystemTimeZone() {
        val offenders = appSources()
            .filterNot { it.name == DEVICE_CLOCK }
            .flatMap { file ->
                val text = file.readText()
                SYSTEM_ZONE_READS.filter { it in text }.map { "${file.name}: $it" }
            }

        assertEquals(emptyList<String>(), offenders)
    }

    /** Guards the tests above against passing because they looked in the wrong place. */
    @Test
    fun theScanSeesTheScreensWithDateFieldsAndTheDateFormatters() {
        val presentation = presentationSources().map { it.name }.toSet()
        val app = appSources().map { it.name }.toSet()

        val screens = listOf("TransactionFormScreen.kt", "SearchScreen.kt", "BackupViewModel.kt")
        val formatters = listOf(DEVICE_CLOCK, "DateUtils.kt", "ReportCsvFormatter.kt", "ReportPdfRenderer.kt")

        assertTrue(presentation.toString(), presentation.containsAll(screens))
        assertTrue(app.toString(), app.containsAll(formatters))
    }

    private fun presentationSources(): List<File> = sourcesUnder("com/denariidolor/presentation")

    private fun appSources(): List<File> = sourcesUnder("com/denariidolor")

    private fun sourcesUnder(packagePath: String): List<File> {
        val root = listOf(File("src/main/java"), File("app/src/main/java"))
            .map { File(it, packagePath) }
            .first { it.isDirectory }
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private companion object {
        const val DEVICE_CLOCK = "DeviceClock.kt"

        val SYSTEM_ZONE_READS = listOf("currentSystemDefault", "ZoneId.systemDefault", "TimeZone.getDefault", "systemDefaultZone")

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
