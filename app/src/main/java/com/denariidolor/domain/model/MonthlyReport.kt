/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.domain.model

import java.time.YearMonth

data class MonthlyReport(
    val title: String,
    val period: YearMonth,
    val generatedAtEpochMillis: Long,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long,
    val netCents: Long,
    val rows: List<ReportRow>
)

data class ReportRow(
    val transactionId: Long,
    val dateEpochMillis: Long,
    val type: TransactionType,
    val categoryName: String,
    val description: String,
    val amountCents: Long,
    val paymentMethod: String
)
