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

package com.denariidolor.presentation.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denariidolor.R
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.util.DateUtils
import com.denariidolor.util.formatSignedAmount
import java.time.ZoneId

const val TRANSACTION_ROW_TAG_PREFIX = "transactionRow_"

data class TransactionRow(
    val id: Long,
    val description: String,
    val type: TransactionType,
    val amountText: String,
    val categoryName: String,
    val accountLabel: String,
    val dateText: String,
    val categoryIcon: String = CategoryIcons.DEFAULT_KEY
)

fun buildTransactionRows(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    limit: Int = Int.MAX_VALUE,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<TransactionRow> {
    val categoriesById = categories.associateBy { it.id }
    val accountNames = accounts.associate { it.id to it.name }
    fun accountName(id: Long) = accountNames[id] ?: "#$id"

    return transactions
        .sortedWith(compareByDescending<TransactionEntity> { it.dateEpochMillis }.thenByDescending { it.id })
        .take(limit.coerceAtLeast(0))
        .map { transaction ->
            val source = accountName(transaction.accountId)
            TransactionRow(
                id = transaction.id,
                description = transaction.description,
                type = transaction.type,
                amountText = formatSignedAmount(transaction.type, transaction.amountCents),
                categoryName = categoriesById[transaction.categoryId]?.name ?: "#${transaction.categoryId}",
                accountLabel = transaction.transferAccountId?.let { "$source → ${accountName(it)}" } ?: source,
                dateText = DateUtils.formatLocalDate(transaction.dateEpochMillis, zoneId),
                categoryIcon = categoriesById[transaction.categoryId]?.iconName ?: CategoryIcons.DEFAULT_KEY
            )
        }
}

@Composable
fun TransactionRowItem(row: TransactionRow, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(TRANSACTION_ROW_TAG_PREFIX + row.id)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBadge(iconKey = row.categoryIcon)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = row.description, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${row.dateText} · ${row.categoryName} · ${row.accountLabel}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(text = row.amountText, fontWeight = FontWeight.Bold)
        if (onDelete != null) {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
