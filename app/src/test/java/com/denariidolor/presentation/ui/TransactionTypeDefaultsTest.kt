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
            accountId = "",
            transferAccountId = ""
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
            accountId = "7",
            transferAccountId = "123"
        )

        assertEquals("99", defaults.categoryId)
        assertEquals("7", defaults.accountId)
        assertEquals(Constants.DEFAULT_SAVINGS_ACCOUNT_ID.toString(), defaults.transferAccountId)
        assertEquals(Constants.DEFAULT_TRANSFER_CATEGORY_ID.toString(), defaults.lastAutoCategoryId)
    }
}
