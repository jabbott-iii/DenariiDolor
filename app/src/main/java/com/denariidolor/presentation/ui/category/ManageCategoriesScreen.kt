package com.denariidolor.presentation.ui.category

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denariidolor.R
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.presentation.ui.common.CategoryIconBadge
import com.denariidolor.presentation.ui.common.CategoryIcons
import com.denariidolor.presentation.ui.common.ConfirmDialog
import com.denariidolor.presentation.ui.common.ManagedItemRow
import com.denariidolor.presentation.ui.common.ScreenHeader
import com.denariidolor.presentation.ui.common.UiMessageEffect

const val CategoryIconOptionTagPrefix = "categoryIcon_"

@Composable
fun ManageCategoriesRoute(onBack: () -> Unit, viewModel: CategoryViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    UiMessageEffect(viewModel.messages)
    ManageCategoriesScreen(
        categories = categories,
        onBack = onBack,
        onAdd = viewModel::add,
        onUpdate = viewModel::update,
        onDelete = viewModel::delete
    )
}

@Composable
fun ManageCategoriesScreen(
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onAdd: (name: String, iconKey: String) -> Unit,
    onUpdate: (id: Long, name: String, iconKey: String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleting by remember { mutableStateOf<CategoryEntity?>(null) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIconBadge(iconKey = category.iconName)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ManagedItemRow(
                            title = category.name,
                            subtitle = null,
                            onEdit = { editing = category },
                            onDelete = { deleting = category }
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }

    if (adding) {
        CategoryDialog(
            title = stringResource(R.string.add_category),
            initialName = "",
            initialIcon = CategoryIcons.DEFAULT_KEY,
            onConfirm = { name, icon ->
                adding = false
                onAdd(name, icon)
            },
            onDismiss = { adding = false }
        )
    }
    editing?.let { category ->
        CategoryDialog(
            title = stringResource(R.string.edit_category),
            initialName = category.name,
            initialIcon = category.iconName,
            onConfirm = { name, icon ->
                editing = null
                onUpdate(category.id, name, icon)
            },
            onDismiss = { editing = null }
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

@Composable
private fun CategoryDialog(
    title: String,
    initialName: String,
    initialIcon: String,
    onConfirm: (name: String, iconKey: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var icon by rememberSaveable { mutableStateOf(CategoryIcons.forKey(initialIcon).key) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.category_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.category_icon_label, CategoryIcons.forKey(icon).label),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(CategoryIcons.options, key = { it.key }) { option ->
                        val selected = option.key == icon
                        CategoryIconBadge(
                            iconKey = option.key,
                            size = 40.dp,
                            modifier = Modifier
                                .padding(4.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { icon = option.key }
                                .semantics { this.selected = selected }
                                .testTag(CategoryIconOptionTagPrefix + option.key)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, icon) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
