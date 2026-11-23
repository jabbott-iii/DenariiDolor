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
 * ViewModels move to :shared once Room does, so they and the interfaces they call must not need Java or Android. A picked
 * file and the biometric cipher reach them as PlatformUri and PlatformCipher, which on Android are android.net.Uri and
 * javax.crypto.Cipher; the Android code behind those interfaces keeps the Android names. androidx.lifecycle is
 * multiplatform, and string resources are a later step, so neither is checked here.
 */
class ViewModelBoundaryTest {
    @Test
    fun viewModelsAndTheInterfacesTheyCallUseNoJavaOrAndroidTypes() {
        val offenders = portableCode().flatMap { (name, code) ->
            PLATFORM_ONLY.filter { it.containsMatchIn(code) }.map { "$name: ${it.pattern}" }
        }

        assertEquals(emptyList<String>(), offenders)
    }

    /** Guards the test above against passing because it looked in the wrong place. */
    @Test
    fun theScanSeesTheViewModelsAndTheirInterfaces() {
        val names = portableCode().map { it.first }

        val expected = listOf("LoginViewModel.kt", "SettingsViewModel.kt", "BackupViewModel.kt", "ReportViewModel.kt") +
            INTERFACE_FILES.map { File(it).name } + BACKUP_SERVICE
        assertTrue(names.toString(), names.containsAll(expected))
    }

    /** Each ViewModel, each interface file, and the BackupService interface, which shares its file with the implementation. */
    private fun portableCode(): List<Pair<String, String>> {
        val viewModels = sources("com/denariidolor/presentation").filter { it.name.endsWith("ViewModel.kt") }
        val interfaces = INTERFACE_FILES.map { File(sourceRoot(), it) }
        val backup = File(sourceRoot(), "com/denariidolor/data/backup/BackupManager.kt").readText()
        val backupService = backup.substringAfter("interface BackupService {").substringBefore("\n}\n")
        return (viewModels + interfaces).map { it.name to withoutComments(it.readText()) } +
            (BACKUP_SERVICE to withoutComments(backupService))
    }

    private fun sources(packagePath: String): List<File> =
        File(sourceRoot(), packagePath).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }

    /** KDoc may name an Android type to explain a choice; only code counts. */
    private fun withoutComments(source: String): String =
        source.replace(BLOCK_COMMENT, "").lines().joinToString("\n") { it.substringBefore("//") }

    private companion object {
        const val BACKUP_SERVICE = "BackupService"

        val INTERFACE_FILES = listOf(
            "com/denariidolor/presentation/platform/PlatformActions.kt",
            "com/denariidolor/data/local/vault/SignInVault.kt",
            "com/denariidolor/data/local/vault/SettingsVault.kt",
            "com/denariidolor/data/local/vault/VaultResults.kt"
        )

        val BLOCK_COMMENT = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)

        val PLATFORM_ONLY = listOf(
            Regex("^import (java|javax|android)\\.", RegexOption.MULTILINE),
            Regex("\\b(java|javax|android)\\.[a-z]+\\."),
            // Same-package or already-imported uses that no import line shows.
            Regex("\\b(Uri|Cipher)\\b")
        )
    }
}
