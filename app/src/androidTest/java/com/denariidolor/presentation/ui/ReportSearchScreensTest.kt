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

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.denariidolor.data.export.ReportFormat
import com.denariidolor.domain.model.MonthlyReport
import com.denariidolor.domain.model.ReportRow
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.report.ReportText
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.presentation.ui.common.TransactionRow
import com.denariidolor.presentation.ui.common.TransactionRowTagPrefix
import com.denariidolor.presentation.ui.report.ReportScreen
import com.denariidolor.presentation.ui.report.ReportTableTag
import com.denariidolor.presentation.ui.report.ReportUiState
import com.denariidolor.presentation.ui.search.SearchButtonTag
import com.denariidolor.presentation.ui.search.SearchResultCountTag
import com.denariidolor.presentation.ui.search.SearchScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.YearMonth

class ReportSearchScreensTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val period = YearMonth.of(2026, 9)
    private val report = MonthlyReport(
        title = ReportText.TITLE,
        period = period,
        generatedAtEpochMillis = 0L,
        totalIncomeCents = 10_000,
        totalExpenseCents = 4_000,
        netCents = 6_000,
        rows = listOf(ReportRow(1, 0L, TransactionType.EXPENSE, "Dining", "Coffee", 4_000, "Visa"))
    )

    @Test
    fun reportShowsTitleColumnsRowsAndExportActions() {
        val actions = mutableListOf<String>()
        composeRule.setContent {
            DenariiDolorTheme {
                ReportScreen(
                    state = ReportUiState(period = period, report = report),
                    onPreviousMonth = { actions += "prev" },
                    onNextMonth = { actions += "next" },
                    onSave = { actions += "save-${it.extension}" },
                    onShare = { actions += "share-${it.extension}" }
                )
            }
        }

        composeRule.onNodeWithText(ReportText.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(ReportTableTag).assertIsDisplayed()
        composeRule.onNodeWithText("Payment Method").assertExists()
        composeRule.onNodeWithText("Coffee").assertExists()
        composeRule.onNodeWithText("Visa").assertExists()

        composeRule.onNodeWithText("Save PDF").performClick()
        composeRule.onNodeWithText("Share CSV").performClick()
        composeRule.onNodeWithText("‹ Prev").performClick()

        assertEquals(listOf("save-${ReportFormat.PDF.extension}", "share-${ReportFormat.CSV.extension}", "prev"), actions)
    }

    @Test
    fun reportShowsEmptyState() {
        composeRule.setContent {
            DenariiDolorTheme {
                ReportScreen(ReportUiState(period, report.copy(rows = emptyList())), {}, {}, {}, {})
            }
        }

        composeRule.onNodeWithText("No transactions in this month.").assertIsDisplayed()
    }

    @Test
    fun searchShowsResultCountAndOpensResult() {
        var opened: Long? = null
        composeRule.setContent {
            DenariiDolorTheme {
                SearchScreen(
                    categories = emptyList(),
                    results = listOf(TransactionRow(5, "Coffee", TransactionType.EXPENSE, "-$4.00", "Dining", "Cash", "2026-09-20")),
                    onSearch = {},
                    onResultClick = { opened = it }
                )
            }
        }

        composeRule.onNodeWithTag(SearchResultCountTag).assertTextContains("1 result(s)")
        composeRule.onNodeWithTag(TransactionRowTagPrefix + 5).performClick()

        assertEquals(5L, opened)
    }

    @Test
    fun searchSubmitsParsedFilters() {
        var searched = false
        composeRule.setContent {
            DenariiDolorTheme {
                SearchScreen(categories = emptyList(), results = null, onSearch = { searched = it.categoryId == null }, onResultClick = {})
            }
        }

        composeRule.onNodeWithText("All categories").assertExists()
        composeRule.onNodeWithTag(SearchButtonTag).performClick()

        assertEquals(true, searched)
    }
}
