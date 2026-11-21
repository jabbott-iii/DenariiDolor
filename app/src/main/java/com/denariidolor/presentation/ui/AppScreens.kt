/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.denariidolor.presentation.ui

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.denariidolor.R
import com.denariidolor.data.legal.LegalDocument
import com.denariidolor.presentation.ui.account.ManageAccountsRoute
import com.denariidolor.presentation.ui.backup.BackupRoute
import com.denariidolor.presentation.ui.budget.ManageBudgetsRoute
import com.denariidolor.presentation.ui.category.ManageCategoriesRoute
import com.denariidolor.presentation.ui.common.rememberSupportActions
import com.denariidolor.presentation.ui.dashboard.DashboardRoute
import com.denariidolor.presentation.ui.dashboard.GettingStartedStep
import com.denariidolor.presentation.ui.guide.GuidanceViewModel
import com.denariidolor.presentation.ui.guide.RatePromptDialog
import com.denariidolor.presentation.ui.guide.WelcomeTour
import com.denariidolor.presentation.ui.help.HelpRoute
import com.denariidolor.presentation.ui.legal.LegalDocumentRoute
import com.denariidolor.presentation.ui.legal.LegalDocumentViewModel
import com.denariidolor.presentation.ui.report.ReportRoute
import com.denariidolor.presentation.ui.search.SearchRoute
import com.denariidolor.presentation.ui.settings.SettingsScreenState
import com.denariidolor.presentation.ui.transaction.TransactionViewModel

private enum class MainDestination(val route: String, val titleRes: Int, val iconRes: Int) {
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
    ManageBudgets("manage_budgets", R.string.manage_budgets, android.R.drawable.ic_menu_manage),
    Backup("backup", R.string.settings_backup_restore, android.R.drawable.ic_menu_save),
    Legal("legal/{${LegalDocumentViewModel.ARG_DOCUMENT}}", R.string.settings_privacy_policy, android.R.drawable.ic_menu_info_details),
    Help("help", R.string.help_title, android.R.drawable.ic_menu_help)
}

private fun editTransactionRoute(id: Long) = "edit_transaction/$id"

private fun legalRoute(document: LegalDocument) = "legal/${document.name}"

/**
 * Pops the current destination at most once. Later calls, such as a double tap on Back or a second `Saved` event, arrive after
 * the destination has left the resumed state and are dropped, so they can't pop the start destination and blank the NavHost (BUG-03).
 */
@Composable
internal fun NavController.popBackOnce(): () -> Unit = dropUnlessResumed { popBackStack() }

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
    onDarkModeChange: (Boolean) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onCurrencyChange: (String) -> Unit = {},
    onRestored: () -> Unit = {}
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val guidance: GuidanceViewModel = hiltViewModel()
    val tourPending by guidance.tourPending.collectAsStateWithLifecycle()
    val ratePromptVisible by guidance.ratePromptVisible.collectAsStateWithLifecycle()
    val support = rememberSupportActions()
    val rateApp = {
        guidance.stopAskingToRate()
        support.rateApp()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            // While the tour covers the app, screen readers must not reach the screens underneath it.
            modifier = if (tourPending) Modifier.clearAndSetSemantics { } else Modifier,
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
                    DashboardRoute(
                        onEditTransaction = { id -> navController.navigate(editTransactionRoute(id)) },
                        onGettingStartedStep = navController::openGettingStartedStep
                    )
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
                        onBiometricChange = onBiometricChange,
                        onCurrencyChange = onCurrencyChange,
                        onManageCategories = { navController.navigate(MainDestination.ManageCategories.route) },
                        onManageAccounts = { navController.navigate(MainDestination.ManageAccounts.route) },
                        onManageBudgets = { navController.navigate(MainDestination.ManageBudgets.route) },
                        onBackup = { navController.navigate(MainDestination.Backup.route) },
                        onPrivacyPolicy = { navController.navigate(legalRoute(LegalDocument.PRIVACY_POLICY)) },
                        onLicenses = { navController.navigate(legalRoute(LegalDocument.OPEN_SOURCE_LICENSES)) },
                        onHelp = { navController.navigate(MainDestination.Help.route) },
                        onShowTour = guidance::showTourAgain,
                        onSendFeedback = support.sendFeedback,
                        onRateApp = rateApp,
                        onShareApp = support.shareApp
                    )
                }
                composable(MainDestination.AddTransaction.route) {
                    AddTransactionRoute(onFinished = navController.popBackOnce())
                }
                composable(
                    route = MainDestination.EditTransaction.route,
                    arguments = listOf(navArgument(TransactionViewModel.ARG_TRANSACTION_ID) { type = NavType.LongType })
                ) {
                    AddTransactionRoute(onFinished = navController.popBackOnce())
                }
                composable(MainDestination.ManageCategories.route) {
                    ManageCategoriesRoute(onBack = navController.popBackOnce())
                }
                composable(MainDestination.ManageAccounts.route) {
                    ManageAccountsRoute(onBack = navController.popBackOnce())
                }
                composable(MainDestination.ManageBudgets.route) {
                    ManageBudgetsRoute(onBack = navController.popBackOnce())
                }
                composable(MainDestination.Backup.route) {
                    BackupRoute(onBack = navController.popBackOnce(), onRestored = onRestored)
                }
                composable(
                    route = MainDestination.Legal.route,
                    arguments = listOf(navArgument(LegalDocumentViewModel.ARG_DOCUMENT) { type = NavType.StringType })
                ) {
                    LegalDocumentRoute(onBack = navController.popBackOnce())
                }
                composable(MainDestination.Help.route) {
                    HelpRoute(onBack = navController.popBackOnce(), onSendFeedback = support.sendFeedback)
                }
            }
        }
        if (tourPending) {
            WelcomeTour(onFinish = guidance::finishTour)
        } else if (ratePromptVisible) {
            RatePromptDialog(onRate = rateApp, onLater = guidance::postponeRating, onNever = guidance::stopAskingToRate)
        }
    }
}

private fun NavController.openGettingStartedStep(step: GettingStartedStep) {
    val route = when (step) {
        GettingStartedStep.ADD_ACCOUNT -> MainDestination.ManageAccounts.route
        GettingStartedStep.ADD_TRANSACTION -> MainDestination.AddTransaction.route
        GettingStartedStep.SET_BUDGET -> MainDestination.ManageBudgets.route
    }
    navigate(route) { launchSingleTop = true }
}
