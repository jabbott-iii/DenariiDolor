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

package com.denariidolor.util

object Constants {
    const val APP_DB_NAME = "denarii_dolor.db"
    const val SESSION_TIMEOUT_MILLIS = 5 * 60 * 1000L
    const val DEFAULT_CASH_ACCOUNT_ID = 1L
    const val DEFAULT_SAVINGS_ACCOUNT_ID = 2L
    const val DEFAULT_EXPENSE_CATEGORY_ID = 1L
    const val DEFAULT_INCOME_CATEGORY_ID = 2L
    const val DEFAULT_TRANSFER_CATEGORY_ID = 3L

    val DEFAULT_ACCOUNTS = listOf(
        SeedAccount(DEFAULT_CASH_ACCOUNT_ID, "Cash", 0L),
        SeedAccount(DEFAULT_SAVINGS_ACCOUNT_ID, "Savings", 0L)
    )

    val DEFAULT_CATEGORIES = listOf(
        SeedCategory(DEFAULT_EXPENSE_CATEGORY_ID, "General Expense", "ic_category_default"),
        SeedCategory(DEFAULT_INCOME_CATEGORY_ID, "General Income", "income"),
        SeedCategory(DEFAULT_TRANSFER_CATEGORY_ID, "Transfer", "transfer")
    )
}

data class SeedAccount(val id: Long, val name: String, val balanceCents: Long)

data class SeedCategory(val id: Long, val name: String, val iconName: String)
