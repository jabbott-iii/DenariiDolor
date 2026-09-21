package com.denariidolor.presentation.ui.budget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denariidolor.R
import com.denariidolor.presentation.ui.common.ConfirmDialog
import com.denariidolor.presentation.ui.common.FormDialog
import com.denariidolor.presentation.ui.common.FormField
import com.denariidolor.presentation.ui.common.ManagedItemRow
import com.denariidolor.presentation.ui.common.ScreenHeader
import com.denariidolor.presentation.ui.common.UiMessageEffect

private const val DEFAULT_WARNING_PERCENT = "80"

@Composable
fun ManageBudgetsRoute(onBack: () -> Unit, viewModel: BudgetViewModel = hiltViewModel()) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    UiMessageEffect(viewModel.messages)
    ManageBudgetsScreen(
        rows = rows,
        onBack = onBack,
        onSetBudget = viewModel::setBudget,
        onDeleteBudget = viewModel::deleteBudget
    )
}

@Composable
fun ManageBudgetsScreen(
    rows: List<BudgetRow>,
    onBack: () -> Unit,
    onSetBudget: (categoryId: Long, limit: String, warningPercent: String) -> Unit,
    onDeleteBudget: (Long) -> Unit
) {
    var editing by remember { mutableStateOf<BudgetRow?>(null) }
    var deleting by remember { mutableStateOf<BudgetRow?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(title = stringResource(R.string.manage_budgets), onBack = onBack)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(rows, key = { it.categoryId }) { row ->
                ManagedItemRow(
                    title = row.categoryName,
                    subtitle = row.budget?.let {
                        stringResource(R.string.budget_summary, it.monthlyLimit, it.warningThresholdPercent)
                    } ?: stringResource(R.string.no_budget),
                    onEdit = { editing = row },
                    onDelete = if (row.budget != null) ({ deleting = row }) else null,
                    editLabel = stringResource(R.string.set_budget),
                    deleteLabel = stringResource(R.string.remove)
                )
                HorizontalDivider()
            }
        }
    }

    editing?.let { row ->
        FormDialog(
            title = stringResource(R.string.budget_for_category, row.categoryName),
            fields = listOf(
                FormField(
                    stringResource(R.string.budget_limit),
                    row.budget?.monthlyLimit?.toBigDecimal()?.stripTrailingZeros()?.toPlainString().orEmpty(),
                    KeyboardType.Decimal
                ),
                FormField(
                    stringResource(R.string.budget_warning_percent),
                    row.budget?.warningThresholdPercent?.toString() ?: DEFAULT_WARNING_PERCENT,
                    KeyboardType.Number
                )
            ),
            onConfirm = { values ->
                editing = null
                onSetBudget(row.categoryId, values[0], values[1])
            },
            onDismiss = { editing = null }
        )
    }
    deleting?.let { row ->
        ConfirmDialog(
            title = stringResource(R.string.delete_item_title, stringResource(R.string.budget_for_category, row.categoryName)),
            message = stringResource(R.string.delete_item_message),
            confirmLabel = stringResource(R.string.remove),
            onConfirm = {
                deleting = null
                onDeleteBudget(row.categoryId)
            },
            onDismiss = { deleting = null }
        )
    }
}
