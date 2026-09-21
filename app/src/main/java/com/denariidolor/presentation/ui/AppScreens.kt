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

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.denariidolor.presentation.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.denariidolor.R
import com.denariidolor.presentation.ui.account.ManageAccountsRoute
import com.denariidolor.presentation.ui.budget.ManageBudgetsRoute
import com.denariidolor.presentation.ui.category.ManageCategoriesRoute
import com.denariidolor.presentation.ui.dashboard.DashboardRoute
import com.denariidolor.presentation.ui.report.ReportRoute
import com.denariidolor.presentation.ui.search.SearchRoute
import com.denariidolor.presentation.ui.settings.SettingsScreenState
import com.denariidolor.presentation.ui.transaction.TransactionViewModel

private enum class MainDestination(
    val route: String,
    val titleRes: Int,
    val iconRes: Int
) {
    Dashboard("dashboard", R.string.dashboard, android.R.drawable.ic_menu_view),
    Search("search", R.string.search, android.R.drawable.ic_menu_search),
    Report("report", R.string.reports, android.R.drawable.ic_menu_info_details),
    Settings("settings", R.string.settings, android.R.drawable.ic_menu_preferences),
    AddTransaction("add_transaction", R.string.add_transaction, android.R.drawable.ic_input_add),
    EditTransaction(
        "edit_transaction/{${TransactionViewModel.ARG_TRANSACTION_ID}}",
        R.string.edit_transaction,
        android.R.drawable.ic_menu_edit
    ),
    ManageCategories("manage_categories", R.string.manage_categories, android.R.drawable.ic_menu_sort_by_size),
    ManageAccounts("manage_accounts", R.string.manage_accounts, android.R.drawable.ic_menu_agenda),
    ManageBudgets("manage_budgets", R.string.manage_budgets, android.R.drawable.ic_menu_manage)
}

private fun editTransactionRoute(id: Long) = "edit_transaction/$id"

private val bottomDestinations = listOf(
    MainDestination.Dashboard,
    MainDestination.Search,
    MainDestination.Report,
    MainDestination.Settings
)

@Composable
fun MainActivityContent(
    settingsState: SettingsScreenState,
    onSignOut: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(destination.iconRes),
                                contentDescription = stringResource(destination.titleRes)
                            )
                        },
                        label = { Text(stringResource(destination.titleRes)) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (bottomDestinations.any { it.route == currentRoute }) {
                FloatingActionButton(
                    onClick = { navController.navigate(MainDestination.AddTransaction.route) { launchSingleTop = true } }
                ) {
                    Icon(
                        painter = painterResource(MainDestination.AddTransaction.iconRes),
                        contentDescription = stringResource(MainDestination.AddTransaction.titleRes)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainDestination.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(MainDestination.Dashboard.route) {
                DashboardRoute(onEditTransaction = { id -> navController.navigate(editTransactionRoute(id)) })
            }
            composable(MainDestination.Search.route) {
                SearchRoute(onEditTransaction = { id -> navController.navigate(editTransactionRoute(id)) })
            }
            composable(MainDestination.Report.route) { ReportRoute() }
            composable(MainDestination.Settings.route) {
                SettingsScreen(
                    state = settingsState,
                    onSignOut = onSignOut,
                    onDarkModeChange = onDarkModeChange,
                    onManageCategories = { navController.navigate(MainDestination.ManageCategories.route) },
                    onManageAccounts = { navController.navigate(MainDestination.ManageAccounts.route) },
                    onManageBudgets = { navController.navigate(MainDestination.ManageBudgets.route) }
                )
            }
            composable(MainDestination.AddTransaction.route) {
                AddTransactionRoute(onFinished = { navController.popBackStack() })
            }
            composable(
                route = MainDestination.EditTransaction.route,
                arguments = listOf(navArgument(TransactionViewModel.ARG_TRANSACTION_ID) { type = NavType.LongType })
            ) {
                AddTransactionRoute(onFinished = { navController.popBackStack() })
            }
            composable(MainDestination.ManageCategories.route) {
                ManageCategoriesRoute(onBack = { navController.popBackStack() })
            }
            composable(MainDestination.ManageAccounts.route) {
                ManageAccountsRoute(onBack = { navController.popBackStack() })
            }
            composable(MainDestination.ManageBudgets.route) {
                ManageBudgetsRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}
