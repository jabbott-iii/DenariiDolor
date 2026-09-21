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
