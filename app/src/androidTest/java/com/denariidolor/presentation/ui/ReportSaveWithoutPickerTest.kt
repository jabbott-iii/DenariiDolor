/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui

import android.content.ActivityNotFoundException
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.R
import com.denariidolor.data.export.ReportExporter
import com.denariidolor.data.export.ReportFormat
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.repository.AccountRepositoryImpl
import com.denariidolor.data.repository.CategoryRepositoryImpl
import com.denariidolor.data.repository.TransactionRepositoryImpl
import com.denariidolor.domain.report.ReportText
import com.denariidolor.domain.usecase.GenerateReportUseCase
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.presentation.ui.report.ReportRoute
import com.denariidolor.presentation.ui.report.ReportViewModel
import com.denariidolor.util.asZonedClock
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.datetime.YearMonth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BUG-13: on a device with no document picker (some managed or TV builds), Save CSV and Save PDF crashed the app. They now
 * say the report couldn't be exported, as the backup screen already did for backups.
 */
@RunWith(AndroidJUnit4::class)
class ReportSaveWithoutPickerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = Clock.fixed(Instant.parse("2026-09-20T12:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun savingAReportWithoutADocumentPickerDoesNotCrash() {
        val device = NoDocumentPicker()
        val viewModel = reportViewModel()
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides device) {
                DenariiDolorTheme { ReportRoute(viewModel) }
            }
        }
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.report_empty)).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText(text(R.string.save_csv)).performClick()
        composeRule.onNodeWithText(text(R.string.save_pdf)).performClick()
        composeRule.waitForIdle()

        val period = YearMonth(2026, 9)
        assertEquals(
            listOf(ReportText.fileName(period, ReportFormat.CSV.extension), ReportText.fileName(period, ReportFormat.PDF.extension)),
            device.launched
        )
        composeRule.onNodeWithText(text(R.string.save_csv)).assertIsDisplayed().assertIsEnabled()
    }

    private fun reportViewModel(): ReportViewModel {
        val generate = GenerateReportUseCase(
            TransactionRepositoryImpl(db, db.transactionDao(), db.accountDao()),
            CategoryRepositoryImpl(db.categoryDao()),
            AccountRepositoryImpl(db.accountDao()),
            clock.asZonedClock()
        )
        return ReportViewModel(generate, ReportExporter(context), clock)
    }

    private fun text(resId: Int): String = composeRule.activity.getString(resId)

    /** A device where no app can take a document request: each launch fails the way Android's does. */
    private class NoDocumentPicker : ActivityResultRegistryOwner {
        val launched = mutableListOf<Any?>()

        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                launched += input
                throw ActivityNotFoundException("No document picker")
            }
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
