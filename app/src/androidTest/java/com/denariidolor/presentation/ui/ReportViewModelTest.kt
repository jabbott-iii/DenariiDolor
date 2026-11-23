/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.data.export.ReportExporter
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.repository.AccountRepositoryImpl
import com.denariidolor.data.repository.CategoryRepositoryImpl
import com.denariidolor.data.repository.TransactionRepositoryImpl
import com.denariidolor.domain.usecase.GenerateReportUseCase
import com.denariidolor.presentation.ui.report.ReportViewModel
import com.denariidolor.util.asZonedClock
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.datetime.YearMonth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a device because the ViewModel's exporter needs a Context. */
@RunWith(AndroidJUnit4::class)
class ReportViewModelTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun theReportOpensOnTheMonthInTheClocksZone() {
        // 12:00 UTC on September 30 is already October 1 in Kiritimati (UTC+14) and still September 30 in Los Angeles.
        val now = Instant.parse("2026-09-30T12:00:00Z")

        assertEquals(YearMonth(2026, 10), reportViewModel(Clock.fixed(now, ZoneId.of("Pacific/Kiritimati"))).state.value.period)
        assertEquals(YearMonth(2026, 9), reportViewModel(Clock.fixed(now, ZoneId.of("America/Los_Angeles"))).state.value.period)
    }

    private fun reportViewModel(clock: Clock): ReportViewModel {
        val generate = GenerateReportUseCase(
            TransactionRepositoryImpl(db, db.transactionDao(), db.accountDao()),
            CategoryRepositoryImpl(db.categoryDao()),
            AccountRepositoryImpl(db.accountDao()),
            clock.asZonedClock()
        )
        return ReportViewModel(generate, ReportExporter(context), clock.asZonedClock())
    }
}
