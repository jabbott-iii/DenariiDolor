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

package com.denariidolor.presentation.ui.transaction

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.util.DateUtils
import com.denariidolor.util.Money
import java.time.ZoneId

data class TransactionFormInput(
    val type: String,
    val description: String,
    val amount: String,
    val categoryId: String,
    val accountId: String,
    val transferAccountId: String,
    val dateText: String,
    val dateEpochMillis: Long
) {
    fun resolveDateEpochMillis(currentDateText: String, parse: (String) -> Long): Long =
        if (currentDateText == dateText) dateEpochMillis else parse(currentDateText)

    companion object {
        fun from(entity: TransactionEntity, zoneId: ZoneId = ZoneId.systemDefault()) = TransactionFormInput(
            type = entity.type.name,
            description = entity.description,
            amount = Money.toInput(entity.amountCents),
            categoryId = entity.categoryId.toString(),
            accountId = entity.accountId.toString(),
            transferAccountId = entity.transferAccountId?.toString().orEmpty(),
            dateText = DateUtils.formatLocalDate(entity.dateEpochMillis, zoneId),
            dateEpochMillis = entity.dateEpochMillis
        )
    }
}
