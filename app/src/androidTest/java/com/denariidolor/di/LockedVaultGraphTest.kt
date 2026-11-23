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
import com.denariidolor.presentation.ui.transaction.TransactionViewModel
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/** Creates one screen's ViewModel through a factory. */
private typealias Screen = (MetroViewModelFactory) -> ViewModel

/**
 * Rule 4 through the app's real dependency graphs. The app graph, which sign-in uses, has no screen that reads the
 * database at all; those live in the session graph that MainActivity creates after sign-in. Even a session graph created
 * while the vault is locked can't hand them out. Tests never unlock the app's own vault (they use test-only names), so it
 * is locked here.
 */
@RunWith(AndroidJUnit4::class)
class LockedVaultGraphTest {
    private val graph = ApplicationProvider.getApplicationContext<App>().graph

    @Test
    fun theSignInGraphHasNoScreenThatReadsTheDatabase() {
        val screens = DATABASE_SCREENS + { factory -> factory.create(TransactionViewModel::class, CreationExtras.Empty) }

        val refusals = refusals<IllegalArgumentException>(graph.metroViewModelFactory, screens)

        assertEquals(screens.size, refusals.count { it.orEmpty().startsWith("Unknown model class") })
    }

    @Test
    fun aSessionGraphBuildsWhileLockedButCannotOpenTheDatabase() {
        val session = graph.createSessionGraph()

        val refusals = refusals<IllegalStateException>(session.viewModelFactory, DATABASE_SCREENS)

        assertEquals(List(DATABASE_SCREENS.size) { "The database is locked" }, refusals)
    }

    /** Why [factory] refused to create each of [screens]. */
    private inline fun <reified E : Throwable> refusals(factory: MetroViewModelFactory, screens: List<Screen>): List<String?> =
        screens.map { screen -> assertThrows(E::class.java) { screen(factory) }.message }

    private companion object {
        // TransactionViewModel needs a real SavedStateHandle before it reaches the database, so only the sign-in check has it.
        val DATABASE_SCREENS: List<Screen> = listOf(
            { it.create(DashboardViewModel::class, CreationExtras.Empty) },
            { it.create(SearchViewModel::class, CreationExtras.Empty) },
            { it.create(ReportViewModel::class, CreationExtras.Empty) },
            { it.create(CategoryViewModel::class, CreationExtras.Empty) },
            { it.create(AccountViewModel::class, CreationExtras.Empty) },
            { it.create(BudgetViewModel::class, CreationExtras.Empty) }
        )
    }
}
