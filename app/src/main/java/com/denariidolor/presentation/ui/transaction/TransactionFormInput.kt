package com.denariidolor.presentation.ui.transaction

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.util.DateUtils
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
            type = entity.type,
            description = entity.description,
            amount = entity.amount.toBigDecimal().stripTrailingZeros().toPlainString(),
            categoryId = entity.categoryId.toString(),
            accountId = entity.accountId.toString(),
            transferAccountId = entity.transferAccountId?.toString().orEmpty(),
            dateText = DateUtils.formatLocalDate(entity.dateEpochMillis, zoneId),
            dateEpochMillis = entity.dateEpochMillis
        )
    }
}
