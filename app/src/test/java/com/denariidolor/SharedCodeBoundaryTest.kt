/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The :shared module's common code must stay portable for the iOS phases. While Android is its only target, the compiler
 * builds commonMain as Android code and accepts Java and Android APIs there, so this test refuses them instead. Nothing in
 * commonMain is injected yet, so @Inject is refused too.
 */
class SharedCodeBoundaryTest {
    @Test
    fun commonCodeUsesNoJavaAndroidOrInjectionApis() {
        val offenders = commonSources().flatMap { file ->
            val code = withoutComments(file.readText())
            PLATFORM_ONLY.filter { it.containsMatchIn(code) }.map { "${file.name}: ${it.pattern}" }
        }

        assertEquals(emptyList<String>(), offenders)
    }

    /** Guards the test above against passing because it looked in the wrong place. */
    @Test
    fun theScanSeesTheSharedCode() {
        val names = commonSources().map { it.name }.toSet()

        assertTrue(names.toString(), names.containsAll(listOf("Ledger.kt", "BackupSnapshot.kt", "ZonedClock.kt")))
    }

    private fun commonSources(): List<File> {
        val root = listOf(File("../shared/src/commonMain/kotlin"), File("shared/src/commonMain/kotlin")).first { it.isDirectory }
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    /** KDoc may name a Java type to explain a choice; only code counts. */
    private fun withoutComments(source: String): String =
        source.replace(BLOCK_COMMENT, "").lines().joinToString("\n") { it.substringBefore("//") }

    private companion object {
        val BLOCK_COMMENT = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)

        val PLATFORM_ONLY = listOf(
            Regex("^import (java|javax|android|androidx|dagger)\\.", RegexOption.MULTILINE),
            Regex("\\b(java|javax|android)\\.[a-z]+\\."),
            Regex("::class\\.java\\b"),
            Regex("\\bString\\.format\\("),
            Regex("\\bCharsets\\."),
            Regex("\\bSystem\\."),
            Regex("\\bsynchronized\\("),
            Regex("@Inject\\b")
        )
    }
}
