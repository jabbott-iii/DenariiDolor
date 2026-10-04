/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui

import com.denariidolor.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionTypeDefaultsTest {
    @Test
    fun `auto category is replaced when prior value was auto generated`() {
        val defaults = applyTransactionTypeDefaults(
            selectedType = "INCOME",
            currentCategoryId = Constants.DEFAULT_EXPENSE_CATEGORY_ID.toString(),
            lastAutoCategoryId = Constants.DEFAULT_EXPENSE_CATEGORY_ID.toString(),
            accountId = ""
        )

        assertEquals(Constants.DEFAULT_INCOME_CATEGORY_ID.toString(), defaults.categoryId)
        assertEquals(Constants.DEFAULT_CASH_ACCOUNT_ID.toString(), defaults.accountId)
        assertEquals("", defaults.transferAccountId)
    }

    @Test
    fun `manual category is preserved while transfer defaults are applied`() {
        val defaults = applyTransactionTypeDefaults(
            selectedType = "TRANSFER",
            currentCategoryId = "99",
            lastAutoCategoryId = Constants.DEFAULT_EXPENSE_CATEGORY_ID.toString(),
            accountId = "7"
        )

        assertEquals("99", defaults.categoryId)
        assertEquals("7", defaults.accountId)
        assertEquals(Constants.DEFAULT_SAVINGS_ACCOUNT_ID.toString(), defaults.transferAccountId)
        assertEquals(Constants.DEFAULT_TRANSFER_CATEGORY_ID.toString(), defaults.lastAutoCategoryId)
    }
}
