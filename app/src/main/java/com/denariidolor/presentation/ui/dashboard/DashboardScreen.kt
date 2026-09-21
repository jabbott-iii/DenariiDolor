package com.denariidolor.presentation.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denariidolor.R
import com.denariidolor.presentation.ui.common.ConfirmDialog
import com.denariidolor.presentation.ui.common.UiMessageEffect

internal const val RecentTransactionRowTagPrefix = "recentTransaction_"

@Composable
fun DashboardRoute(
    onEditTransaction: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiMessageEffect(viewModel.messages)
    DashboardScreen(
        state = state,
        onEditTransaction = onEditTransaction,
        onDeleteTransaction = viewModel::deleteTransaction
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<TransactionRow?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp)
    ) {
        item {
            Text(text = stringResource(R.string.dashboard_income, state.summary.income), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.dashboard_expense, state.summary.expense))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.dashboard_net, state.summary.net))
        }
        if (state.balances.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.account_balances)) }
            items(state.balances, key = { "balance_${it.id}" }) { account ->
                Text(text = stringResource(R.string.account_balance_line, account.name, account.balance))
            }
        }
        item { SectionTitle(stringResource(R.string.recent_transactions)) }
        if (state.recent.isEmpty()) {
            item { Text(text = stringResource(R.string.no_transactions)) }
        }
        items(state.recent, key = { it.id }) { row ->
            TransactionRowItem(
                row = row,
                onClick = { onEditTransaction(row.id) },
                onDelete = { pendingDelete = row }
            )
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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun TransactionRowItem(row: TransactionRow, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(RecentTransactionRowTagPrefix + row.id)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = row.description, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${row.dateText} · ${row.categoryName} · ${row.accountLabel}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(text = row.amountText, fontWeight = FontWeight.Bold)
        TextButton(onClick = onDelete) {
            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
        }
    }
}
