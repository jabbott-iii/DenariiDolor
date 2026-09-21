package com.denariidolor.presentation.ui.category

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denariidolor.R
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.presentation.ui.common.ConfirmDialog
import com.denariidolor.presentation.ui.common.FormDialog
import com.denariidolor.presentation.ui.common.FormField
import com.denariidolor.presentation.ui.common.ManagedItemRow
import com.denariidolor.presentation.ui.common.ScreenHeader
import com.denariidolor.presentation.ui.common.UiMessageEffect

@Composable
fun ManageCategoriesRoute(onBack: () -> Unit, viewModel: CategoryViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    UiMessageEffect(viewModel.messages)
    ManageCategoriesScreen(
        categories = categories,
        onBack = onBack,
        onAdd = viewModel::add,
        onRename = viewModel::rename,
        onDelete = viewModel::delete
    )
}

@Composable
fun ManageCategoriesScreen(
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleting by remember { mutableStateOf<CategoryEntity?>(null) }
    val nameLabel = stringResource(R.string.category_name)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(title = stringResource(R.string.manage_categories), onBack = onBack)
        Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.add_category))
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(categories, key = { it.id }) { category ->
                ManagedItemRow(
                    title = category.name,
                    subtitle = null,
                    onEdit = { renaming = category },
                    onDelete = { deleting = category }
                )
                HorizontalDivider()
            }
        }
    }

    if (adding) {
        FormDialog(
            title = stringResource(R.string.add_category),
            fields = listOf(FormField(nameLabel)),
            onConfirm = { values ->
                adding = false
                onAdd(values[0])
            },
            onDismiss = { adding = false }
        )
    }
    renaming?.let { category ->
        FormDialog(
            title = stringResource(R.string.rename_category),
            fields = listOf(FormField(nameLabel, category.name)),
            onConfirm = { values ->
                renaming = null
                onRename(category.id, values[0])
            },
            onDismiss = { renaming = null }
        )
    }
    deleting?.let { category ->
        ConfirmDialog(
            title = stringResource(R.string.delete_item_title, category.name),
            message = stringResource(R.string.delete_item_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                deleting = null
                onDelete(category.id)
            },
            onDismiss = { deleting = null }
        )
    }
}
