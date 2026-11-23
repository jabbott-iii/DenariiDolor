/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.viewmodel

import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.SearchFilters
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toKotlinTimeZone
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(clock: Clock, transactions: List<TransactionEntity> = emptyList()) = SearchViewModel(
        SearchTransactionUseCase(FakeTransactionRepository(transactions)),
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

    @Test
    fun resultsShowDatesInTheClocksZone() = runTest {
        // 23:30 UTC on Sep 20 is already Sep 21 at UTC+14.
        val stored = TestData.expense(id = 1).copy(dateEpochMillis = Instant.parse("2026-09-20T23:30:00Z").toEpochMilli())
        val kiritimati = ZoneId.of("Pacific/Kiritimati")
        val vm = viewModel(Clock.fixed(Instant.EPOCH, kiritimati), listOf(stored))
        val collector = launch { vm.results.collect {} }

        vm.search(SearchFilters())

        assertEquals("2026-09-21", vm.results.first { it != null }?.single()?.dateText)
        assertEquals(kiritimati.toKotlinTimeZone(), vm.timeZone())
        collector.cancel()
    }
}
