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
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.denariidolor.data.local.db.entity.BudgetEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.presentation.ui.budget.BudgetRow
import com.denariidolor.presentation.ui.budget.ManageBudgetsScreen
import com.denariidolor.presentation.ui.category.CategoryIconOptionTagPrefix
import com.denariidolor.presentation.ui.category.ManageCategoriesScreen
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.presentation.ui.common.PickerOption
import com.denariidolor.presentation.ui.common.TransactionRow
import com.denariidolor.presentation.ui.common.TransactionRowTagPrefix
import com.denariidolor.presentation.ui.dashboard.BudgetAlertBannerTag
import com.denariidolor.presentation.ui.dashboard.BudgetProgress
import com.denariidolor.presentation.ui.dashboard.CategorySpend
import com.denariidolor.presentation.ui.dashboard.DashboardScreen
import com.denariidolor.presentation.ui.dashboard.SpendingChartTag
import com.denariidolor.presentation.ui.dashboard.DashboardUiState
import com.denariidolor.presentation.ui.settings.SettingsScreenState
import com.denariidolor.presentation.ui.transaction.TransactionFormInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CrudScreensTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val row = TransactionRow(
        id = 7,
        description = "Coffee",
        type = TransactionType.EXPENSE,
        amountText = "-$4.50",
        categoryName = "Dining",
        accountLabel = "Cash",
        dateText = "2026-09-20"
    )

    @Test
    fun dashboardShowsEmptyStateWithoutTransactions() {
        composeRule.setContent {
            DenariiDolorTheme { DashboardScreen(DashboardUiState(), onEditTransaction = {}, onDeleteTransaction = {}) }
        }

        composeRule.onNodeWithText("No transactions yet. Tap + to add one.").assertIsDisplayed()
    }

    @Test
    fun dashboardRowClickRequestsEdit() {
        var editedId: Long? = null
        composeRule.setContent {
            DenariiDolorTheme {
                DashboardScreen(DashboardUiState(recent = listOf(row)), onEditTransaction = { editedId = it }, onDeleteTransaction = {})
            }
        }

        composeRule.onNodeWithTag(TransactionRowTagPrefix + 7).performClick()

        assertEquals(7L, editedId)
    }

    @Test
    fun dashboardDeleteRequiresConfirmation() {
        var deletedId: Long? = null
        composeRule.setContent {
            DenariiDolorTheme {
                DashboardScreen(DashboardUiState(recent = listOf(row)), onEditTransaction = {}, onDeleteTransaction = { deletedId = it })
            }
        }

        composeRule.onNodeWithText("Delete").performClick()
        assertNull(deletedId)
        composeRule.onNodeWithText("Delete transaction?").assertIsDisplayed()
        composeRule.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()

        assertEquals(7L, deletedId)
    }

    @Test
    fun editModePrefillsFormAndShowsUpdateLabel() {
        composeRule.setContent {
            DenariiDolorTheme {
                AddTransactionScreen(
                    onSave = { _, _, _, _, _, _, _ -> },
                    onShowMessage = {},
                    categories = listOf(PickerOption(4, "Dining")),
                    accounts = listOf(PickerOption(1, "Cash")),
                    initial = TransactionFormInput(
                        type = "EXPENSE",
                        description = "Coffee",
                        amount = "4.5",
                        categoryId = "4",
                        accountId = "1",
                        transferAccountId = "",
                        dateText = "2026-09-20",
                        dateEpochMillis = 1L
                    )
                )
            }
        }

        composeRule.onNodeWithText("Edit Transaction").assertIsDisplayed()
        composeRule.onNodeWithTag(CategoryPickerTag).assertTextContains("Dining")
        composeRule.onNodeWithTag(AccountPickerTag).assertTextContains("Cash")
        composeRule.onNodeWithTag(SaveTransactionButtonTag).assertTextContains("Update Transaction")
    }

    @Test
    fun editModeKeepsOriginalTimestampWhenDateUnchanged() {
        var savedDate: Long? = null
        composeRule.setContent {
            DenariiDolorTheme {
                AddTransactionScreen(
                    onSave = { _, _, _, _, _, _, date -> savedDate = date },
                    onShowMessage = {},
                    initial = TransactionFormInput("EXPENSE", "Coffee", "4.5", "4", "1", "", "2026-09-20", 123_456L)
                )
            }
        }

        composeRule.onNodeWithTag(SaveTransactionButtonTag).performClick()

        assertEquals(123_456L, savedDate)
    }

    @Test
    fun settingsManageButtonsInvokeCallbacks() {
        val opened = mutableListOf<String>()
        composeRule.setContent {
            DenariiDolorTheme {
                SettingsScreen(
                    state = SettingsScreenState(sessionTimeoutMinutes = 5, pinConfigured = true),
                    onSignOut = {},
                    onResetSecurityProfile = {},
                    onManageCategories = { opened += "categories" },
                    onManageAccounts = { opened += "accounts" },
                    onManageBudgets = { opened += "budgets" }
                )
            }
        }

        composeRule.onNodeWithText("Manage Categories").performClick()
        composeRule.onNodeWithText("Manage Accounts").performClick()
        composeRule.onNodeWithText("Manage Budgets").performClick()

        assertEquals(listOf("categories", "accounts", "budgets"), opened)
    }

    @Test
    fun manageCategoriesAddDialogSubmitsNameAndIcon() {
        var added: Pair<String, String>? = null
        composeRule.setContent {
            DenariiDolorTheme {
                ManageCategoriesScreen(
                    categories = listOf(CategoryEntity(id = 1, name = "General Expense")),
                    onBack = {},
                    onAdd = { name, icon -> added = name to icon },
                    onUpdate = { _, _, _ -> },
                    onDelete = {}
                )
            }
        }

        composeRule.onNodeWithText("Add Category").performClick()
        composeRule.onNodeWithText("Category name").performTextReplacement("Dining")
        composeRule.onNodeWithTag(CategoryIconOptionTagPrefix + "dining").performClick()
        composeRule.onNodeWithTag(CategoryIconOptionTagPrefix + "dining").assertIsSelected()
        composeRule.onNodeWithText("Save").performClick()

        assertEquals("Dining" to "dining", added)
    }

    @Test
    fun dashboardShowsBudgetAlertAndMeterStatus() {
        composeRule.setContent {
            DenariiDolorTheme {
                DashboardScreen(
                    DashboardUiState(
                        spending = listOf(CategorySpend(4, "Dining", "dining", 9_500)),
                        budgets = listOf(BudgetProgress(4, "Dining", "dining", spentCents = 9_500, limitCents = 10_000, warningPercent = 80))
                    ),
                    onEditTransaction = {},
                    onDeleteTransaction = {}
                )
            }
        }

        composeRule.onNodeWithTag(BudgetAlertBannerTag).assertIsDisplayed()
        composeRule.onNodeWithText("1 budget is near or over its limit.").assertIsDisplayed()
        composeRule.onNodeWithTag(SpendingChartTag).assertExists()
        composeRule.onNodeWithText("Near limit · 95% used").assertExists()
    }

    @Test
    fun dashboardHidesBannerWhenBudgetsOnTrack() {
        composeRule.setContent {
            DenariiDolorTheme {
                DashboardScreen(
                    DashboardUiState(budgets = listOf(BudgetProgress(4, "Dining", "dining", spentCents = 1_000, limitCents = 10_000, warningPercent = 80))),
                    onEditTransaction = {},
                    onDeleteTransaction = {}
                )
            }
        }

        composeRule.onNodeWithTag(BudgetAlertBannerTag).assertDoesNotExist()
        composeRule.onNodeWithText("On track · 10% used").assertExists()
    }

    @Test
    fun manageBudgetsHidesRemoveWhenNoBudget() {
        var removed = false
        composeRule.setContent {
            DenariiDolorTheme {
                ManageBudgetsScreen(
                    rows = listOf(
                        BudgetRow(1, "Groceries", BudgetEntity(id = 1, categoryId = 1, monthlyLimitCents = 20_000)),
                        BudgetRow(2, "Dining", null)
                    ),
                    onBack = {},
                    onSetBudget = { _, _, _ -> },
                    onDeleteBudget = { removed = true }
                )
            }
        }

        composeRule.onNodeWithText("No budget set").assertIsDisplayed()
        assertEquals(1, composeRule.onAllNodesWithText("Remove").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Remove").performClick()
        composeRule.onNode(hasText("Remove") and hasAnyAncestor(isDialog())).performClick()

        assertTrue(removed)
    }
}
