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

package com.denariidolor.presentation.ui.search

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denariidolor.R
import com.denariidolor.domain.model.SearchFilters
import com.denariidolor.presentation.ui.common.PickerOption
import com.denariidolor.presentation.ui.common.ReferencePicker
import com.denariidolor.presentation.ui.common.TransactionRow
import com.denariidolor.presentation.ui.common.TransactionRowItem
import java.time.LocalDate

const val SEARCH_BUTTON_TAG = "searchButton"
const val SEARCH_RESULT_COUNT_TAG = "searchResultCount"
private const val ALL_CATEGORIES_ID = 0L

@Composable
fun SearchRoute(onEditTransaction: (Long) -> Unit, viewModel: SearchViewModel = hiltViewModel()) {
    val results by viewModel.results.collectAsStateWithLifecycle()
    val categories by viewModel.categoryOptions.collectAsStateWithLifecycle()
    SearchScreen(
        categories = categories,
        results = results,
        onSearch = viewModel::search,
        onResultClick = onEditTransaction
    )
}

@Composable
fun SearchScreen(
    categories: List<PickerOption>,
    results: List<TransactionRow>?,
    onSearch: (SearchFilters) -> Unit,
    onResultClick: (Long) -> Unit
) {
    val context = LocalContext.current
    var description by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var minAmount by rememberSaveable { mutableStateOf("") }
    var maxAmount by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val invalidFiltersMessage = stringResource(R.string.invalid_search_filters_message)
    val allCategoriesLabel = stringResource(R.string.search_all_categories)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_description_hint)) },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        ReferencePicker(
            label = stringResource(R.string.transaction_category_label),
            options = listOf(PickerOption(ALL_CATEGORIES_ID, allCategoriesLabel)) + categories,
            selectedId = categoryId.ifEmpty { ALL_CATEGORIES_ID.toString() },
            onSelected = { categoryId = if (it == ALL_CATEGORIES_ID.toString()) "" else it }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = minAmount,
                onValueChange = { minAmount = it },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.search_min_amount_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = maxAmount,
                onValueChange = { maxAmount = it },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.search_max_amount_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { pickDate(context, startDate) { startDate = it } }, modifier = Modifier.weight(1f)) {
                Text(startDate.ifBlank { stringResource(R.string.search_start_date_hint) })
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = { pickDate(context, endDate) { endDate = it } }, modifier = Modifier.weight(1f)) {
                Text(endDate.ifBlank { stringResource(R.string.search_end_date_hint) })
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    description = ""
                    categoryId = ""
                    minAmount = ""
                    maxAmount = ""
                    startDate = ""
                    endDate = ""
                    errorMessage = null
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.search_clear))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    runCatching {
                        SearchFilterParser.parse(description, categoryId, minAmount, maxAmount, startDate, endDate)
                    }.onSuccess {
                        errorMessage = null
                        onSearch(it)
                    }.onFailure {
                        errorMessage = invalidFiltersMessage
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag(SEARCH_BUTTON_TAG)
            ) {
                Text(stringResource(R.string.search))
            }
        }
        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        results?.let { rows ->
            Text(
                text = if (rows.isEmpty()) {
                    stringResource(R.string.search_no_results)
                } else {
                    stringResource(R.string.search_result_count, rows.size)
                },
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.testTag(SEARCH_RESULT_COUNT_TAG)
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rows, key = { it.id }) { row ->
                    TransactionRowItem(row = row, onClick = { onResultClick(row.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun pickDate(context: Context, current: String, onPicked: (String) -> Unit) {
    val initial = current.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
    DatePickerDialog(
        context,
        { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day).toString()) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth
    ).show()
}
