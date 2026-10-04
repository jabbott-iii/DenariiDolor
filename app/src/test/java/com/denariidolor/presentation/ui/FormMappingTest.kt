/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui

import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.presentation.ui.account.AccountViewModel
import com.denariidolor.presentation.ui.budget.buildBudgetRows
import com.denariidolor.presentation.ui.transaction.TransactionFormInput
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
