/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.platform

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Screens reach Android only through the interfaces in PlatformActions.kt, so they can move to shared code with the
 * Compose Multiplatform plan. AndroidPlatformActions.kt implements those interfaces, and the activities stay Android's own.
 */
class PlatformBoundaryTest {
    @Test
    fun screensCallAndroidOnlyThroughThePlatformLayer() {
        val offenders = presentationSources()
            .filterNot { it.name == ANDROID_IMPLEMENTATION || it.name.endsWith("Activity.kt") }
            .flatMap { file ->
                val text = file.readText()
                PLATFORM_CALLS.filter { it in text }.map { "${file.name}: $it" }
            }

        assertEquals(emptyList<String>(), offenders)
    }

    /** Guards the test above against passing because it looked in the wrong place. */
    @Test
    fun theScanSeesEveryScreen() {
        val names = presentationSources().map { it.name }.toSet()

        assertTrue(names.toString(), names.containsAll(listOf("ReportScreen.kt", "BackupScreen.kt", "LoginActivity.kt")))
    }

    private fun presentationSources(): List<File> {
        val root = listOf(File("src/main/java"), File("app/src/main/java"))
            .map { File(it, "com/denariidolor/presentation") }
            .first { it.isDirectory }
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private companion object {
        const val ANDROID_IMPLEMENTATION = "AndroidPlatformActions.kt"

        val PLATFORM_CALLS = listOf(
            "LocalContext",
            "LocalActivity",
            "android.widget.Toast",
            "android.content.Intent",
            "android.content.ActivityNotFoundException",
            "android.content.ClipData",
            "android.content.pm.PackageManager",
            "android.os.Build",
            "android.app.DatePickerDialog",
            "androidx.activity.compose.rememberLauncherForActivityResult",
            "androidx.activity.result."
        )
    }
}
