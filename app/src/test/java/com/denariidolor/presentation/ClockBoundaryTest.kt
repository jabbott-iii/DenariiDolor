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

    /** ViewModels take ZonedClock and kotlinx-datetime types, so they can move to :shared with the code they call. */
    @Test
    fun viewModelsUseNoJavaTime() {
        val viewModels = presentationSources().filter { it.name.endsWith("ViewModel.kt") }
        val offenders = viewModels.filter { JAVA_TIME.containsMatchIn(it.readText()) }.map { it.name }

        assertTrue(viewModels.toString(), viewModels.map { it.name }.containsAll(listOf("DashboardViewModel.kt", "BackupViewModel.kt")))
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

    /** Both modules: :app, and :shared's common and Android code. */
    private fun appSources(): List<File> = sourcesUnder("com/denariidolor") +
        SHARED_SOURCE_SETS.flatMap { sourceSet -> kotlinFilesIn(File(projectDir().parentFile, "shared/src/$sourceSet/kotlin")) }

    private fun sourcesUnder(packagePath: String): List<File> = kotlinFilesIn(File(projectDir(), "src/main/java/$packagePath"))

    /** The :app directory, wherever the test runs from. */
    private fun projectDir(): File = listOf(File("."), File("app")).map { it.absoluteFile.normalize() }
        .first { File(it, "src/main/java").isDirectory }

    private fun kotlinFilesIn(root: File): List<File> {
        check(root.isDirectory) { "No sources at $root" }
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private companion object {
        const val DEVICE_CLOCK = "DeviceClock.kt"

        val SHARED_SOURCE_SETS = listOf("commonMain", "androidMain")

        val JAVA_TIME = Regex("\\bjava\\.time\\.")

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
