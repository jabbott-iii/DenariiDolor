package com.denariidolor.presentation.ui.report

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denariidolor.R
import com.denariidolor.data.export.ReportFormat
import com.denariidolor.domain.model.ReportRow
import com.denariidolor.domain.report.ReportText
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.util.DateUtils
import com.denariidolor.util.formatSignedAmount

const val ReportTableTag = "reportTable"

private data class ReportColumn(val titleRes: Int, val width: Dp, val alignEnd: Boolean = false)

private val reportColumns = listOf(
    ReportColumn(R.string.report_col_date, 96.dp),
    ReportColumn(R.string.report_col_type, 80.dp),
    ReportColumn(R.string.report_col_category, 120.dp),
    ReportColumn(R.string.report_col_description, 180.dp),
    ReportColumn(R.string.report_col_amount, 96.dp, alignEnd = true),
    ReportColumn(R.string.report_col_payment, 160.dp)
)

@Composable
fun ReportRoute(viewModel: ReportViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val shareTitle = stringResource(R.string.share_report)
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ReportFormat.CSV.mimeType)) { uri ->
        uri?.let { viewModel.save(it, ReportFormat.CSV) }
    }
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ReportFormat.PDF.mimeType)) { uri ->
        uri?.let { viewModel.save(it, ReportFormat.PDF) }
    }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ReportEvent.Message -> {
                    val text = when (val message = event.message) {
                        is UiMessage.Resource -> context.getString(message.resId)
                        is UiMessage.Text -> message.text
                    }
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                }
                is ReportEvent.Share -> {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        clipData = ClipData.newRawUri("", event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, shareTitle))
                }
            }
        }
    }

    ReportScreen(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onSave = { format ->
            val fileName = ReportText.fileName(state.period, format.extension)
            when (format) {
                ReportFormat.CSV -> saveCsv.launch(fileName)
                ReportFormat.PDF -> savePdf.launch(fileName)
            }
        },
        onShare = viewModel::share
    )
}

@Composable
fun ReportScreen(
    state: ReportUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSave: (ReportFormat) -> Unit,
    onShare: (ReportFormat) -> Unit
) {
    val report = state.report
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPreviousMonth) { Text(stringResource(R.string.previous_month)) }
            Text(
                text = ReportText.periodLabel(state.period),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = onNextMonth) { Text(stringResource(R.string.next_month)) }
        }
        if (report == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.failed) Text(stringResource(R.string.report_load_failed)) else CircularProgressIndicator()
            }
            return@Column
        }
        Text(text = report.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = stringResource(R.string.report_generated, ReportText.generatedLabel(report.generatedAtEpochMillis)),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = stringResource(R.string.report_totals, report.totalIncome, report.totalExpense, report.net))
        Spacer(modifier = Modifier.height(8.dp))
        ExportButtons(enabled = !state.loading, onSave = onSave, onShare = onShare)
        Spacer(modifier = Modifier.height(8.dp))
        if (report.rows.isEmpty()) {
            Text(text = stringResource(R.string.report_empty))
        } else {
            ReportTable(rows = report.rows, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExportButtons(enabled: Boolean, onSave: (ReportFormat) -> Unit, onShare: (ReportFormat) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onSave(ReportFormat.CSV) }, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.save_csv))
            }
            OutlinedButton(onClick = { onSave(ReportFormat.PDF) }, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.save_pdf))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onShare(ReportFormat.CSV) }, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.share_csv))
            }
            OutlinedButton(onClick = { onShare(ReportFormat.PDF) }, enabled = enabled, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.share_pdf))
            }
        }
    }
}

@Composable
private fun ReportTable(rows: List<ReportRow>, modifier: Modifier = Modifier) {
    val tableWidth = reportColumns.fold(0.dp) { total, column -> total + column.width }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag(ReportTableTag)
    ) {
        Column(
            modifier = Modifier
                .width(tableWidth)
                .fillMaxHeight()
        ) {
            TableRow(cells = reportColumns.map { stringResource(it.titleRes) }, header = true)
            HorizontalDivider()
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rows, key = { it.transactionId }) { row ->
                    TableRow(
                        cells = listOf(
                            DateUtils.formatLocalDate(row.dateEpochMillis),
                            row.type,
                            row.categoryName,
                            row.description,
                            formatSignedAmount(row.type, row.amount),
                            row.paymentMethod
                        ),
                        header = false
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        cells.forEachIndexed { index, text ->
            val column = reportColumns[index]
            Text(
                text = text,
                modifier = Modifier
                    .width(column.width)
                    .padding(horizontal = 4.dp),
                textAlign = if (column.alignEnd) TextAlign.End else TextAlign.Start,
                fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
