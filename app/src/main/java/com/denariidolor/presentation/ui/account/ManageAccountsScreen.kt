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

package com.denariidolor.presentation.ui.account

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denariidolor.R
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.presentation.ui.common.ConfirmDialog
import com.denariidolor.presentation.ui.common.FormDialog
import com.denariidolor.presentation.ui.common.FormField
import com.denariidolor.presentation.ui.common.ManagedItemRow
import com.denariidolor.presentation.ui.common.ScreenHeader
import com.denariidolor.presentation.ui.common.UiMessageEffect
import com.denariidolor.util.formatMoney

@Composable
fun ManageAccountsRoute(onBack: () -> Unit, viewModel: AccountViewModel = hiltViewModel()) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    UiMessageEffect(viewModel.messages)
    ManageAccountsScreen(
        accounts = accounts,
        onBack = onBack,
        onAdd = viewModel::add,
        onRename = viewModel::rename,
        onDelete = viewModel::delete
    )
}

@Composable
fun ManageAccountsScreen(
    accounts: List<AccountEntity>,
    onBack: () -> Unit,
    onAdd: (name: String, openingBalance: String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<AccountEntity?>(null) }
    var deleting by remember { mutableStateOf<AccountEntity?>(null) }
    val nameLabel = stringResource(R.string.account_name)
    val openingBalanceLabel = stringResource(R.string.opening_balance)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(title = stringResource(R.string.manage_accounts), onBack = onBack)
        Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.add_account))
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(accounts, key = { it.id }) { account ->
                ManagedItemRow(
                    title = account.name,
                    subtitle = stringResource(R.string.account_balance, formatMoney(account.balanceCents)),
                    onEdit = { renaming = account },
                    onDelete = { deleting = account }
                )
                HorizontalDivider()
            }
        }
    }

    if (adding) {
        FormDialog(
            title = stringResource(R.string.add_account),
            fields = listOf(
                FormField(nameLabel),
                FormField(openingBalanceLabel, "0", KeyboardType.Decimal)
            ),
            onConfirm = { values ->
                adding = false
                onAdd(values[0], values[1])
            },
            onDismiss = { adding = false }
        )
    }
    renaming?.let { account ->
        FormDialog(
            title = stringResource(R.string.rename_account),
            fields = listOf(FormField(nameLabel, account.name)),
            onConfirm = { values ->
                renaming = null
                onRename(account.id, values[0])
            },
            onDismiss = { renaming = null }
        )
    }
    deleting?.let { account ->
        ConfirmDialog(
            title = stringResource(R.string.delete_item_title, account.name),
            message = stringResource(R.string.delete_item_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                deleting = null
                onDelete(account.id)
            },
            onDismiss = { deleting = null }
        )
    }
}
