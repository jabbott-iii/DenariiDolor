/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.denariidolor.App
import com.denariidolor.presentation.ui.account.AccountViewModel
import com.denariidolor.presentation.ui.budget.BudgetViewModel
import com.denariidolor.presentation.ui.category.CategoryViewModel
import com.denariidolor.presentation.ui.dashboard.DashboardViewModel
import com.denariidolor.presentation.ui.report.ReportViewModel
import com.denariidolor.presentation.ui.search.SearchViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Rule 4 through the app's real dependency graph: while the vault is locked, the graph and its ViewModel factory still build,
 * so sign-in works, but nothing that needs the database can be created. Tests never unlock the app's own vault (they use
 * test-only names), so it is locked here.
 */
@RunWith(AndroidJUnit4::class)
class LockedVaultGraphTest {
    private val graph = ApplicationProvider.getApplicationContext<App>().graph

    @Test
    fun theViewModelFactoryBuildsWithoutTheDatabase() {
        graph.metroViewModelFactory
    }

    @Test
    fun screensThatReadTheDatabaseCannotBeCreatedWhileLocked() {
        val refusals = listOf(
            refusal<DashboardViewModel>(),
            refusal<SearchViewModel>(),
            refusal<ReportViewModel>(),
            refusal<CategoryViewModel>(),
            refusal<AccountViewModel>(),
            refusal<BudgetViewModel>()
        )

        assertEquals(List(refusals.size) { "The database is locked" }, refusals)
    }

    /** Why the app's ViewModel factory refused to create [T]. */
    private inline fun <reified T : ViewModel> refusal(): String? = assertThrows(IllegalStateException::class.java) {
        graph.metroViewModelFactory.create(T::class, CreationExtras.Empty)
    }.message
}
