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

package com.denariidolor.presentation.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denariidolor.R
import com.denariidolor.domain.report.ReportText
import com.denariidolor.presentation.ui.common.CategoryIconBadge
import com.denariidolor.presentation.ui.common.ConfirmDialog
import com.denariidolor.presentation.ui.common.LocalVizColors
import com.denariidolor.presentation.ui.common.TransactionRow
import com.denariidolor.presentation.ui.common.TransactionRowItem
import com.denariidolor.presentation.ui.common.UiMessageEffect
import com.denariidolor.util.formatMoney

const val BUDGET_ALERT_BANNER_TAG = "budgetAlertBanner"
const val BUDGET_METER_TAG_PREFIX = "budgetMeter_"

@Composable
fun DashboardRoute(onEditTransaction: (Long) -> Unit, viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiMessageEffect(viewModel.messages)
    DashboardScreen(
        state = state,
        onEditTransaction = onEditTransaction,
        onDeleteTransaction = viewModel::deleteTransaction
    )
}

@Composable
fun DashboardScreen(state: DashboardUiState, onEditTransaction: (Long) -> Unit, onDeleteTransaction: (Long) -> Unit) {
    var pendingDelete by remember { mutableStateOf<TransactionRow?>(null) }
    val otherLabel = stringResource(R.string.spending_other)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { SummarySection(state) }
        if (state.budgetAlerts > 0) {
            item { BudgetAlertBanner(state.budgetAlerts) }
        }
        item { SectionTitle(stringResource(R.string.spending_by_category)) }
        if (state.spending.isEmpty()) {
            item { MutedText(stringResource(R.string.spending_none)) }
        } else {
            item {
                val bars = state.spending.map { (it.name ?: otherLabel) to it.amountCents }
                SpendingChart(
                    bars = bars,
                    description = bars.joinToString { (name, amount) -> "$name ${formatMoney(amount)}" },
                    modifier = Modifier.testTag(SPENDING_CHART_TAG)
                )
            }
            items(state.spending, key = { "spend_${it.categoryId}" }) { spend ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CategoryIconBadge(iconKey = spend.iconKey, size = 28.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = spend.name ?: otherLabel, modifier = Modifier.weight(1f))
                    Text(text = formatMoney(spend.amountCents))
                }
            }
        }
        if (state.budgets.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.budgets_this_month)) }
            items(state.budgets, key = { "budget_${it.categoryId}" }) { BudgetMeter(it) }
        }
        if (state.balances.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.account_balances)) }
            items(state.balances, key = { "balance_${it.id}" }) { account ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = account.name, modifier = Modifier.weight(1f))
                    Text(text = formatMoney(account.balanceCents))
                }
            }
        }
        item { SectionTitle(stringResource(R.string.recent_transactions)) }
        if (state.recent.isEmpty()) {
            item { MutedText(stringResource(R.string.no_transactions)) }
        }
        items(state.recent, key = { it.id }) { row ->
            TransactionRowItem(row = row, onClick = { onEditTransaction(row.id) }, onDelete = { pendingDelete = row })
            HorizontalDivider()
        }
    }

    pendingDelete?.let { row ->
        ConfirmDialog(
            title = stringResource(R.string.delete_transaction_title),
            message = stringResource(R.string.delete_transaction_message, row.description),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                pendingDelete = null
                onDeleteTransaction(row.id)
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun SummarySection(state: DashboardUiState) {
    Column {
        Text(
            text = state.period?.let { ReportText.periodLabel(it) }.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = stringResource(R.string.net_this_month), style = MaterialTheme.typography.titleSmall)
        Text(
            text = formatMoney(state.summary.netCents),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(stringResource(R.string.income_this_month), formatMoney(state.summary.incomeCents), Modifier.weight(1f))
            StatTile(stringResource(R.string.expense_this_month), formatMoney(state.summary.expenseCents), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BudgetAlertBanner(count: Int) {
    val viz = LocalVizColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag(BUDGET_ALERT_BANNER_TAG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = viz.statusWarning)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = pluralStringResource(R.plurals.budget_alerts, count, count))
        }
    }
}

@Composable
private fun BudgetMeter(progress: BudgetProgress) {
    val viz = LocalVizColors.current
    val (color: Color, icon: ImageVector, labelRes: Int) = when (progress.status) {
        BudgetStatus.OK -> Triple(viz.series1, Icons.Outlined.CheckCircle, R.string.budget_status_ok)
        BudgetStatus.WARNING -> Triple(viz.statusWarning, Icons.Outlined.Warning, R.string.budget_status_warning)
        BudgetStatus.OVER -> Triple(viz.statusCritical, Icons.Outlined.Error, R.string.budget_status_over)
    }
    val statusLabel = stringResource(labelRes)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag(BUDGET_METER_TAG_PREFIX + progress.categoryId)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIconBadge(iconKey = progress.iconKey, size = 28.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = progress.categoryName, modifier = Modifier.weight(1f))
            Text(text = stringResource(R.string.budget_spent_of_limit, formatMoney(progress.spentCents), formatMoney(progress.limitCents)))
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = viz.meterTrack,
            strokeCap = StrokeCap.Round
        )
        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.budget_status_line, statusLabel, (progress.fraction * 100).toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun MutedText(text: String) {
    Text(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
