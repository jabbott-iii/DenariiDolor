/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.viewmodel

import com.denariidolor.domain.usecase.SearchTransactionUseCase
import com.denariidolor.presentation.ui.search.SearchViewModel
import com.denariidolor.testutil.FakeAccountRepository
import com.denariidolor.testutil.FakeCategoryRepository
import com.denariidolor.testutil.FakeTransactionRepository
import com.denariidolor.testutil.MainDispatcherRule
import com.denariidolor.testutil.TestData
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(clock: Clock) = SearchViewModel(
        SearchTransactionUseCase(FakeTransactionRepository()),
        FakeCategoryRepository(TestData.categories),
        FakeAccountRepository(TestData.accounts),
        clock
    )

    @Test
    fun theDatePickersOpenOnTodayInTheInjectedClocksZone() {
        // 23:30 UTC is already the next day at UTC+14 and still the same day at UTC-7.
        val instant = Instant.parse("2026-09-20T23:30:00Z")

        assertEquals(LocalDate(2026, 9, 21), viewModel(Clock.fixed(instant, ZoneId.of("Pacific/Kiritimati"))).today())
        assertEquals(LocalDate(2026, 9, 20), viewModel(Clock.fixed(instant, ZoneId.of("America/Los_Angeles"))).today())
    }
}
