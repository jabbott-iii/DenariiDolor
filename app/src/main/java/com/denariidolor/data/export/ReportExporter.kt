/*
 * Copyright 2026 Joseph Anthony Abbott III
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.denariidolor.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.report.ReportCsvFormatter
import com.denariidolor.domain.report.ReportText
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class ReportFormat(val mimeType: String, val extension: String) {
    CSV("text/csv", "csv"),
    PDF("application/pdf", "pdf")
}

@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun writeTo(uri: Uri, report: MonthlyReport, format: ReportFormat) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Unable to open export destination")
        stream.use { write(it, report, format) }
    }

    /** Writes to a private cache folder (cleared on every call) and returns a read-only FileProvider URI. */
    suspend fun createShareUri(report: MonthlyReport, format: ReportFormat): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, SHARE_DIR).apply {
            deleteRecursively()
            check(mkdirs()) { "Unable to prepare export folder" }
        }
        val file = File(directory, ReportText.fileName(report.period, format.extension))
        file.outputStream().use { write(it, report, format) }
        FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
    }

    private fun write(out: OutputStream, report: MonthlyReport, format: ReportFormat) {
        when (format) {
            ReportFormat.CSV -> out.write(ReportCsvFormatter.format(report).toByteArray(Charsets.UTF_8))
            ReportFormat.PDF -> ReportPdfRenderer.render(report, out)
        }
    }

    companion object {
        const val SHARE_DIR = "reports"
        const val AUTHORITY_SUFFIX = ".fileprovider"
    }
}
