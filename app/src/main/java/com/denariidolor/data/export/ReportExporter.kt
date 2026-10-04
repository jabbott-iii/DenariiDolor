/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.denariidolor.R
import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.report.ReportCsvFormatter
import com.denariidolor.domain.report.ReportText
import com.denariidolor.util.labelRes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ReportFormat(val mimeType: String, val extension: String) {
    CSV("text/csv", "csv"),
    PDF("application/pdf", "pdf")
}

@Singleton
class ReportExporter @Inject constructor(@ApplicationContext private val context: Context) {
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

    // The CSV keeps its English column keys and type names so spreadsheets and scripts can rely on them; its title is translated.
    private fun write(out: OutputStream, report: MonthlyReport, format: ReportFormat) {
        val title = context.getString(R.string.report_title)
        when (format) {
            ReportFormat.CSV -> out.write(ReportCsvFormatter.format(report.copy(title = title)).toByteArray(Charsets.UTF_8))
            ReportFormat.PDF -> ReportPdfRenderer.render(report, out, labels = pdfLabels(title))
        }
    }

    private fun pdfLabels(title: String) = ReportPdfLabels(
        title = title,
        columns = listOf(
            R.string.report_col_date,
            R.string.report_col_type,
            R.string.report_col_category,
            R.string.report_col_description,
            R.string.report_col_amount,
            R.string.report_col_payment
        ).map { context.getString(it) },
        type = { context.getString(it.labelRes()) },
        empty = context.getString(R.string.report_empty),
        generated = { context.getString(R.string.report_generated, it) },
        totals = { income, expense, net -> context.getString(R.string.report_totals, income, expense, net) },
        page = { context.getString(R.string.report_page, it) }
    )

    companion object {
        const val SHARE_DIR = "reports"
        const val AUTHORITY_SUFFIX = ".fileprovider"

        /** Deletes unencrypted report copies left for sharing; called when the session ends. */
        fun clearShareCache(context: Context): Boolean = File(context.cacheDir, SHARE_DIR).deleteRecursively()
    }
}
