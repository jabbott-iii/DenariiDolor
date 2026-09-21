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

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.denariidolor.presentation.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denariidolor.R
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.presentation.ui.common.PickerOption
import com.denariidolor.presentation.ui.common.ReferencePicker
import com.denariidolor.presentation.ui.common.ScreenHeader
import com.denariidolor.presentation.ui.transaction.TransactionEvent
import com.denariidolor.presentation.ui.transaction.TransactionFormInput
import com.denariidolor.presentation.ui.transaction.TransactionFormState
import com.denariidolor.presentation.ui.transaction.TransactionViewModel
import com.denariidolor.util.Constants
import com.denariidolor.util.DateUtils
import com.denariidolor.util.Money
import java.time.LocalDate

internal const val TransactionTypeFieldTag = "transactionTypeField"
internal const val TransferAccountFieldTag = "transferAccountField"
internal const val CategoryPickerTag = "categoryPicker"
internal const val AccountPickerTag = "accountPicker"
internal const val SaveTransactionButtonTag = "saveTransactionButton"

@Composable
internal fun AddTransactionRoute(
    onFinished: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val categories by viewModel.categoryOptions.collectAsStateWithLifecycle()
    val accounts by viewModel.accountOptions.collectAsStateWithLifecycle()
    var formKey by rememberSaveable { mutableIntStateOf(0) }
    val savedMessage = stringResource(R.string.transaction_saved)
    val errorMessage = stringResource(R.string.generic_error)
    val notFoundMessage = stringResource(R.string.transaction_not_found)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                TransactionEvent.Saved -> {
                    Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
                    if (viewModel.isEditMode) onFinished() else formKey++
                }
                is TransactionEvent.Failed ->
                    Toast.makeText(context, event.message ?: errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    when (val state = formState) {
        TransactionFormState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        TransactionFormState.NotFound -> LaunchedEffect(Unit) {
            Toast.makeText(context, notFoundMessage, Toast.LENGTH_SHORT).show()
            onFinished()
        }
        is TransactionFormState.Ready -> key(formKey) {
            AddTransactionScreen(
                onSave = viewModel::saveTransaction,
                onShowMessage = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                categories = categories,
                accounts = accounts,
                initial = state.initial,
                onBack = if (viewModel.isEditMode) onFinished else null
            )
        }
    }
}

@Composable
fun AddTransactionScreen(
    onSave: (type: String, description: String, amountCents: Long, categoryId: Long, accountId: Long, transferAccountId: Long?, dateEpochMillis: Long) -> Unit,
    onShowMessage: (String) -> Unit,
    categories: List<PickerOption> = emptyList(),
    accounts: List<PickerOption> = emptyList(),
    initial: TransactionFormInput? = null,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val transactionTypes = stringArrayResource(R.array.transaction_types)
    val startType = initial?.type ?: transactionTypes.firstOrNull().orEmpty()
    val startDefaults = remember(startType) {
        applyTransactionTypeDefaults(
            selectedType = startType,
            currentCategoryId = "",
            lastAutoCategoryId = null,
            accountId = "",
            transferAccountId = ""
        )
    }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    var selectedType by rememberSaveable { mutableStateOf(startType) }
    var amount by rememberSaveable { mutableStateOf(initial?.amount.orEmpty()) }
    var categoryId by rememberSaveable { mutableStateOf(initial?.categoryId ?: startDefaults.categoryId) }
    var accountId by rememberSaveable { mutableStateOf(initial?.accountId ?: startDefaults.accountId) }
    var transferAccountId by rememberSaveable { mutableStateOf(initial?.transferAccountId ?: startDefaults.transferAccountId) }
    var lastAutoCategoryId by rememberSaveable { mutableStateOf(startDefaults.lastAutoCategoryId) }
    var dateText by rememberSaveable { mutableStateOf(initial?.dateText.orEmpty()) }
    var dropdownExpanded by rememberSaveable { mutableStateOf(false) }
    val invalidDateMessage = stringResource(R.string.invalid_date_message)
    val referenceRequiredMessage = stringResource(R.string.transaction_reference_required_message)
    val invalidAmountMessage = stringResource(R.string.invalid_amount_message)
    val datePlaceholder = stringResource(R.string.date_hint)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (initial != null) {
            ScreenHeader(title = stringResource(R.string.edit_transaction), onBack = onBack)
        }
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.transaction_description_hint)) },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
        ) {
            OutlinedTextField(
                value = selectedType,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.transaction_type_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .testTag(TransactionTypeFieldTag)
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                transactionTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            dropdownExpanded = false
                            selectedType = type
                            val defaults = applyTransactionTypeDefaults(
                                selectedType = type,
                                currentCategoryId = categoryId,
                                lastAutoCategoryId = lastAutoCategoryId,
                                accountId = accountId,
                                transferAccountId = transferAccountId
                            )
                            categoryId = defaults.categoryId
                            accountId = defaults.accountId
                            transferAccountId = defaults.transferAccountId
                            lastAutoCategoryId = defaults.lastAutoCategoryId
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.transaction_amount_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(modifier = Modifier.height(8.dp))
        ReferencePicker(
            label = stringResource(R.string.transaction_category_label),
            options = categories,
            selectedId = categoryId,
            onSelected = { categoryId = it },
            fieldModifier = Modifier.testTag(CategoryPickerTag)
        )
        Spacer(modifier = Modifier.height(8.dp))
        ReferencePicker(
            label = stringResource(R.string.transaction_account_label),
            options = accounts,
            selectedId = accountId,
            onSelected = { accountId = it },
            fieldModifier = Modifier.testTag(AccountPickerTag)
        )
        if (selectedType == TransactionType.TRANSFER.name) {
            Spacer(modifier = Modifier.height(8.dp))
            ReferencePicker(
                label = stringResource(R.string.transaction_transfer_account_label),
                options = accounts,
                selectedId = transferAccountId,
                onSelected = { transferAccountId = it },
                fieldModifier = Modifier.testTag(TransferAccountFieldTag)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        DateSelectorButton(
            text = dateText.ifBlank { datePlaceholder },
            onClick = { launchDatePicker(context, dateText) { dateText = it } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val amountCents = Money.parseToCents(amount)?.takeIf { it > 0 }
                    ?: return@Button onShowMessage(invalidAmountMessage)
                val parsedCategoryId = categoryId.toLongOrNull()
                    ?: return@Button onShowMessage(referenceRequiredMessage)
                val parsedAccountId = accountId.toLongOrNull()
                    ?: return@Button onShowMessage(referenceRequiredMessage)
                val parsedTransferAccountId = if (selectedType == TransactionType.TRANSFER.name) {
                    transferAccountId.toLongOrNull()
                        ?: return@Button onShowMessage(referenceRequiredMessage)
                } else {
                    null
                }
                val dateEpochMillis = when {
                    dateText.isBlank() -> System.currentTimeMillis()
                    else -> runCatching {
                        initial?.resolveDateEpochMillis(dateText, DateUtils::parseIsoDateToStartOfDayEpochMillis)
                            ?: DateUtils.parseIsoDateToStartOfDayEpochMillis(dateText)
                    }.getOrElse {
                        onShowMessage(invalidDateMessage)
                        return@Button
                    }
                }
                onSave(
                    selectedType,
                    description,
                    amountCents,
                    parsedCategoryId,
                    parsedAccountId,
                    parsedTransferAccountId,
                    dateEpochMillis
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SaveTransactionButtonTag)
        ) {
            Text(stringResource(if (initial == null) R.string.save_transaction else R.string.update_transaction))
        }
    }
}

@Composable
private fun DateSelectorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(onClick = onClick, modifier = modifier) {
        Text(text = text)
    }
}

private fun launchDatePicker(
    context: android.content.Context,
    value: String,
    onDateSelected: (String) -> Unit
) {
    val selectedDate = value.takeIf { it.isNotBlank() }?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    } ?: LocalDate.now()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth).toString())
        },
        selectedDate.year,
        selectedDate.monthValue - 1,
        selectedDate.dayOfMonth
    ).show()
}

internal data class TransactionTypeDefaults(
    val categoryId: String,
    val lastAutoCategoryId: String,
    val accountId: String,
    val transferAccountId: String
)

internal fun applyTransactionTypeDefaults(
    selectedType: String,
    currentCategoryId: String,
    lastAutoCategoryId: String?,
    accountId: String,
    transferAccountId: String
): TransactionTypeDefaults {
    val nextDefaultCategoryId = defaultCategoryId(selectedType).toString()
    return TransactionTypeDefaults(
        categoryId = if (currentCategoryId.isBlank() || currentCategoryId == lastAutoCategoryId) {
            nextDefaultCategoryId
        } else {
            currentCategoryId
        },
        lastAutoCategoryId = nextDefaultCategoryId,
        accountId = accountId.ifBlank {
            Constants.DEFAULT_CASH_ACCOUNT_ID.toString()
        },
        transferAccountId = if (selectedType == TransactionType.TRANSFER.name) {
            Constants.DEFAULT_SAVINGS_ACCOUNT_ID.toString()
        } else {
            ""
        }
    )
}

internal fun defaultCategoryId(type: String): Long {
    return when (type) {
        TransactionType.INCOME.name -> Constants.DEFAULT_INCOME_CATEGORY_ID
        TransactionType.TRANSFER.name -> Constants.DEFAULT_TRANSFER_CATEGORY_ID
        else -> Constants.DEFAULT_EXPENSE_CATEGORY_ID
    }
}
