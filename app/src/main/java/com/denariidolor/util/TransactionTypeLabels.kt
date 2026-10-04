/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import androidx.annotation.StringRes
import com.denariidolor.R
import com.denariidolor.domain.model.TransactionType

/** The user-facing name of a transaction type; [TransactionType.name] is what's stored, exported to CSV and passed around. */
@StringRes
fun TransactionType.labelRes(): Int = when (this) {
    TransactionType.EXPENSE -> R.string.transaction_type_expense
    TransactionType.INCOME -> R.string.transaction_type_income
    TransactionType.TRANSFER -> R.string.transaction_type_transfer
}
