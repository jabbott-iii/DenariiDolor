/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation

import android.content.Context
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ViewModels move to :shared once Room does, so they and the interfaces they call must not need Java or Android. A picked
 * file and the biometric cipher reach them as PlatformUri and PlatformCipher, which on Android are android.net.Uri and
 * javax.crypto.Cipher; the Android code behind those interfaces keeps the Android names. Text reaches them as StringKey and
 * PluralKey inside a UiMessage, never as an R resource. androidx.lifecycle is multiplatform, so it isn't checked.
 */
class ViewModelBoundaryTest {
    @Test
    fun viewModelsAndTheInterfacesTheyCallUseNoJavaOrAndroidTypes() {
        val offenders = portableCode().flatMap { (name, code) ->
            PLATFORM_ONLY.filter { it.containsMatchIn(code) }.map { "$name: ${it.pattern}" }
        }

        assertEquals(emptyList<String>(), offenders)
    }

    /** A ViewModel reaches Android services only through interfaces, never through a class that needs a Context. */
    @Test
    fun viewModelsTakeNoClassThatNeedsAContext() {
        val offenders = viewModelClasses().flatMap { viewModel ->
            viewModel.declaredConstructors.flatMap { it.parameterTypes.asList() }.distinct()
                .filter { type -> type.declaredConstructors.any { it.parameterTypes.any(Context::class.java::isAssignableFrom) } }
                .map { "${viewModel.simpleName}: ${it.simpleName}" }
        }

        assertEquals(emptyList<String>(), offenders)
    }

    /** Guards the tests above against passing because they looked in the wrong place. */
    @Test
    fun theScanSeesTheViewModelsAndTheirInterfaces() {
        val names = portableCode().map { it.first }

        val expected = listOf("LoginViewModel.kt", "SettingsViewModel.kt", "BackupViewModel.kt", "ReportViewModel.kt") +
            INTERFACE_FILES.map { File(it).name } + INTERFACES_BESIDE_THEIR_IMPLEMENTATION.map { it.second }
        assertTrue(names.toString(), names.containsAll(expected))
        assertEquals(viewModelFiles().size, viewModelClasses().size)
    }

    /** Each ViewModel, each interface file, and the interfaces that share a file with their implementation. */
    private fun portableCode(): List<Pair<String, String>> {
        val interfaces = INTERFACE_FILES.map { File(sourceRoot(), it) }
        val besideImplementations = INTERFACES_BESIDE_THEIR_IMPLEMENTATION.map { (path, name) ->
            val source = File(sourceRoot(), path).readText()
            check("interface $name {" in source) { "No interface $name in $path" }
            name to withoutComments(source.substringAfter("interface $name {").substringBefore("\n}\n"))
        }
        return (viewModelFiles() + interfaces).map { it.name to withoutComments(it.readText()) } + besideImplementations
    }

    private fun viewModelFiles(): List<File> = sources("com/denariidolor/presentation").filter { it.name.endsWith("ViewModel.kt") }

    private fun viewModelClasses(): List<Class<*>> = viewModelFiles().map { file ->
        val className = file.relativeTo(sourceRoot()).path.removeSuffix(".kt").replace(File.separatorChar, '.')
        Class.forName(className, false, javaClass.classLoader)
    }

    private fun sources(packagePath: String): List<File> =
        File(sourceRoot(), packagePath).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }

    /** KDoc may name an Android type to explain a choice; only code counts. */
    private fun withoutComments(source: String): String =
        source.replace(BLOCK_COMMENT, "").lines().joinToString("\n") { it.substringBefore("//") }

    private companion object {
        val INTERFACES_BESIDE_THEIR_IMPLEMENTATION = listOf(
            "com/denariidolor/data/backup/BackupManager.kt" to "BackupService",
            "com/denariidolor/data/export/ReportExporter.kt" to "ReportExportService",
            "com/denariidolor/data/legal/LegalDocuments.kt" to "LegalDocumentService"
        )

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
            Regex("\\b(Uri|Cipher)\\b"),
            Regex("^import com\\.denariidolor\\.R$", RegexOption.MULTILINE),
            Regex("\\bR\\.(string|plurals)\\.")
        )
    }
}
