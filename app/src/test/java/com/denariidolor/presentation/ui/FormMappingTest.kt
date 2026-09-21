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

import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.presentation.ui.account.AccountViewModel
import com.denariidolor.presentation.ui.budget.buildBudgetRows
import com.denariidolor.presentation.ui.transaction.TransactionFormInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class FormMappingTest {
    private val entity = TransactionEntity(
        id = 5,
        type = TransactionType.TRANSFER,
        description = "Save",
        amountCents = 25_000,
        categoryId = 3,
        accountId = 1,
        transferAccountId = 2,
        dateEpochMillis = 1_725_192_000_000L
    )

    @Test
    fun formInputFromEntityFormatsFields() {
        val input = TransactionFormInput.from(entity, ZoneId.of("UTC"))

        assertEquals("250", input.amount)
        assertEquals("2", input.transferAccountId)
        assertEquals("2024-09-01", input.dateText)
    }

    @Test
    fun formInputKeepsOriginalTimestampOnlyWhenDateUnchanged() {
        val input = TransactionFormInput.from(entity, ZoneId.of("UTC"))

        assertEquals(entity.dateEpochMillis, input.resolveDateEpochMillis("2024-09-01") { error("should not parse") })
        assertEquals(42L, input.resolveDateEpochMillis("2024-09-02") { 42L })
    }

    @Test
    fun budgetRowsPairCategoriesWithBudgets() {
        val rows = buildBudgetRows(
            listOf(CategoryEntity(id = 1, name = "Food"), CategoryEntity(id = 2, name = "Fun")),
            listOf(BudgetEntity(id = 9, categoryId = 2, monthlyLimitCents = 5_000))
        )

        assertNull(rows[0].budget)
        assertEquals(9L, rows[1].budget?.id)
    }

    @Test
    fun openingBalanceParsing() {
        assertEquals(0L, AccountViewModel.parseOpeningBalance("  "))
        assertEquals(-2_550L, AccountViewModel.parseOpeningBalance("-25.5"))
        assertNull(AccountViewModel.parseOpeningBalance("abc"))
        assertNull(AccountViewModel.parseOpeningBalance("1.005"))
    }
}
