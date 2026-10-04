/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.util

import androidx.annotation.StringRes
import com.denariidolor.R

object Constants {
    const val APP_DB_NAME = "denarii_dolor.db"
    const val SESSION_TIMEOUT_MILLIS = 5 * 60 * 1000L
    const val DEFAULT_CASH_ACCOUNT_ID = 1L
    const val DEFAULT_SAVINGS_ACCOUNT_ID = 2L
    const val DEFAULT_EXPENSE_CATEGORY_ID = 1L
    const val DEFAULT_INCOME_CATEGORY_ID = 2L
    const val DEFAULT_TRANSFER_CATEGORY_ID = 3L

    val DEFAULT_ACCOUNTS = listOf(
        SeedAccount(DEFAULT_CASH_ACCOUNT_ID, R.string.default_account_cash, 0L),
        SeedAccount(DEFAULT_SAVINGS_ACCOUNT_ID, R.string.default_account_savings, 0L)
    )

    val DEFAULT_CATEGORIES = listOf(
        SeedCategory(DEFAULT_EXPENSE_CATEGORY_ID, R.string.default_category_expense, "ic_category_default"),
        SeedCategory(DEFAULT_INCOME_CATEGORY_ID, R.string.default_category_income, "income"),
        SeedCategory(DEFAULT_TRANSFER_CATEGORY_ID, R.string.default_category_transfer, "transfer")
    )
}

/** A default account; its name is resolved in the device's language when it is seeded, and is ordinary data after that. */
data class SeedAccount(val id: Long, @StringRes val nameRes: Int, val balanceCents: Long)

data class SeedCategory(val id: Long, @StringRes val nameRes: Int, val iconName: String)
