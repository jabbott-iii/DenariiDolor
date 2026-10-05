/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.legal

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LegalDocument {
    PRIVACY_POLICY,
    OPEN_SOURCE_LICENSES
}

/**
 * Reads the legal files that the build copies, unchanged, from the repository root into the `legal` assets folder
 * (`copyLegalDocuments` in `app/build.gradle.kts`): `PRIVACY.md`, `THIRD_PARTY_NOTICES.md` and every text in `licenses/`.
 * The app shows them in English, the language they are written in.
 */
@Singleton
class LegalDocuments @Inject constructor(@ApplicationContext context: Context) {
    private val assets = context.applicationContext.assets

    suspend fun load(document: LegalDocument): List<LegalBlock> = withContext(Dispatchers.IO) {
        when (document) {
            LegalDocument.PRIVACY_POLICY -> LegalText.parseMarkdown(read(PRIVACY_POLICY_FILE))
            LegalDocument.OPEN_SOURCE_LICENSES -> LegalText.parseMarkdown(read(NOTICES_FILE)) + licenseTexts()
        }
    }

    // The full license texts, after the notices that list which component each one covers.
    private fun licenseTexts(): List<LegalBlock> = assets.list(DIRECTORY).orEmpty()
        .filter { it.endsWith(LICENSE_TEXT_SUFFIX) }
        .sorted()
        .flatMap { name ->
            listOf(LegalBlock.Heading(name.removeSuffix(LICENSE_TEXT_SUFFIX), level = 2)) + LegalText.parsePlainText(read(name))
        }

    private fun read(name: String): String = assets.open("$DIRECTORY/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }

    companion object {
        const val DIRECTORY = "legal"
        const val PRIVACY_POLICY_FILE = "PRIVACY.md"
        const val NOTICES_FILE = "THIRD_PARTY_NOTICES.md"
        private const val LICENSE_TEXT_SUFFIX = ".txt"
    }
}
