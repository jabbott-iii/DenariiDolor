/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui

import com.denariidolor.domain.model.TransactionType
import com.denariidolor.presentation.ui.common.TransactionRow
import com.denariidolor.presentation.ui.dashboard.AccountBalance
import com.denariidolor.presentation.ui.dashboard.BudgetProgress
import com.denariidolor.presentation.ui.dashboard.DashboardUiState
import com.denariidolor.presentation.ui.dashboard.GettingStartedStep
import com.denariidolor.presentation.ui.dashboard.gettingStarted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GettingStartedTest {
    private val defaults = listOf(AccountBalance(1, "Cash", 0L), AccountBalance(2, "Savings", 0L))
    private val row = TransactionRow(7, "Coffee", TransactionType.EXPENSE, "-4.50", "Dining", "Cash", "2026-09-20")
    private val budget = BudgetProgress(4, "Groceries", "groceries", 0L, 10_000L, 80)

    @Test
    fun newProfileHasEveryStepLeft() {
        val checklist = gettingStarted(DashboardUiState(balances = defaults))

        assertEquals(emptySet<GettingStartedStep>(), checklist?.done)
        assertEquals(3, checklist?.total)
    }

    @Test
    fun stepsTickThemselvesFromTheData() {
        val checklist = gettingStarted(
            DashboardUiState(balances = defaults + AccountBalance(3, "Visa", 0L), recent = listOf(row))
        )

        assertEquals(setOf(GettingStartedStep.ADD_ACCOUNT, GettingStartedStep.ADD_TRANSACTION), checklist?.done)
        assertEquals(2, checklist?.completed)
    }

    @Test
    fun goneOnceEveryStepIsDone() {
        val state = DashboardUiState(balances = defaults + AccountBalance(3, "Visa", 0L), recent = listOf(row), budgets = listOf(budget))

        assertNull(gettingStarted(state))
    }

    @Test
    fun hiddenWhenTheDataFailedToLoad() {
        assertNull(gettingStarted(DashboardUiState(failed = true)))
    }
}
