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
        SeedAccount(DEFAULT_CASH_ACCOUNT_ID, "Cash", 0.0),
        SeedAccount(DEFAULT_SAVINGS_ACCOUNT_ID, "Savings", 0.0)
    )

    val DEFAULT_CATEGORIES = listOf(
        SeedCategory(DEFAULT_EXPENSE_CATEGORY_ID, "General Expense", "ic_category_default"),
        SeedCategory(DEFAULT_INCOME_CATEGORY_ID, "General Income", "income"),
        SeedCategory(DEFAULT_TRANSFER_CATEGORY_ID, "Transfer", "transfer")
    )
}

data class SeedAccount(
    val id: Long,
    val name: String,
    val balance: Double
)

data class SeedCategory(
    val id: Long,
    val name: String,
    val iconName: String
)
