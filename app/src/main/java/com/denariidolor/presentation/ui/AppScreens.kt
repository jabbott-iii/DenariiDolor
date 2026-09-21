@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.denariidolor.presentation.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.denariidolor.presentation.ui.common.PickerOption
import com.denariidolor.presentation.ui.common.ReferencePicker
import com.denariidolor.presentation.ui.common.ScreenHeader
import com.denariidolor.presentation.ui.dashboard.DashboardRoute
import com.denariidolor.presentation.ui.report.ReportViewModel
import com.denariidolor.presentation.ui.search.SearchFilterParser
import com.denariidolor.presentation.ui.search.SearchViewModel
import com.denariidolor.presentation.ui.settings.SettingsScreenState
import com.denariidolor.presentation.ui.transaction.TransactionEvent
import com.denariidolor.presentation.ui.transaction.TransactionFormInput
import com.denariidolor.presentation.ui.transaction.TransactionFormState
import com.denariidolor.presentation.ui.transaction.TransactionViewModel
import com.denariidolor.util.Constants
import com.denariidolor.util.DateUtils
import java.time.LocalDate

internal const val LoginButtonTag = "loginButton"
internal const val BiometricButtonTag = "biometricButton"
internal const val TransactionTypeFieldTag = "transactionTypeField"
internal const val TransferAccountFieldTag = "transferAccountField"
internal const val CategoryPickerTag = "categoryPicker"
internal const val AccountPickerTag = "accountPicker"
internal const val SaveTransactionButtonTag = "saveTransactionButton"

enum class LoginScreenMode {
    SETUP,
    SIGN_IN,
    RECOVER_PIN
}

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
    onSettingsAction: (startPinRecovery: Boolean) -> Unit
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
        ) {
            composable(MainDestination.Dashboard.route) {
                DashboardRoute(onEditTransaction = { id -> navController.navigate(editTransactionRoute(id)) })
            }
            composable(MainDestination.Search.route) { SearchRoute() }
            composable(MainDestination.Report.route) { ReportRoute() }
            composable(MainDestination.Settings.route) {
                SettingsScreen(
                    state = settingsState,
                    onSignOut = { onSettingsAction(false) },
                    onResetSecurityProfile = { onSettingsAction(true) },
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

@Composable
fun LoginScreen(
    mode: LoginScreenMode,
    pin: String,
    pinConfirmation: String,
    securityQuestion: String,
    securityAnswer: String,
    recoveryQuestion: String?,
    signInEnabled: Boolean,
    biometricAvailable: Boolean,
    showWipeConfirmation: Boolean,
    feedbackMessage: String?,
    onPinChange: (String) -> Unit,
    onPinConfirmationChange: (String) -> Unit,
    onSecurityQuestionChange: (String) -> Unit,
    onSecurityAnswerChange: (String) -> Unit,
    onPrimaryAction: () -> Unit,
    onForgotPin: () -> Unit,
    onBackToSignIn: () -> Unit,
    onBiometricLogin: () -> Unit,
    onRequestWipeData: () -> Unit,
    onCancelWipeData: () -> Unit,
    onConfirmWipeData: () -> Unit
) {
    val biometricContentDescription = stringResource(R.string.biometric_sign_in_accessibility_label)
    val wipeActionContentDescription = stringResource(R.string.wipe_all_data_destructive_label)
    val wipeConfirmContentDescription = stringResource(R.string.wipe_data_confirm_destructive_label)
    var pinVisible by rememberSaveable { mutableStateOf(false) }
    val pinVisualTransformation =
        if (pinVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        }
    val primaryButtonLabel =
        when (mode) {
            LoginScreenMode.SIGN_IN -> stringResource(R.string.sign_in)
            LoginScreenMode.SETUP -> stringResource(R.string.create_security_profile)
            LoginScreenMode.RECOVER_PIN -> stringResource(R.string.reset_pin)
        }
    val pinLabel =
        when (mode) {
            LoginScreenMode.RECOVER_PIN -> stringResource(R.string.new_pin_hint)
            else -> stringResource(R.string.pin_hint)
        }

    if (showWipeConfirmation) {
        AlertDialog(
            onDismissRequest = onCancelWipeData,
            title = { Text(stringResource(R.string.wipe_data_title)) },
            text = { Text(stringResource(R.string.wipe_data_warning)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmWipeData,
                    modifier = Modifier.semantics {
                        contentDescription = wipeConfirmContentDescription
                    }
                ) {
                    Text(stringResource(R.string.wipe_data_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelWipeData) {
                    Text(stringResource(R.string.wipe_data_cancel))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            if (!signInEnabled) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (feedbackMessage != null) {
                Text(text = feedbackMessage)
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (mode == LoginScreenMode.RECOVER_PIN) {
                Text(text = recoveryQuestion ?: stringResource(R.string.security_question_unavailable))
                Spacer(modifier = Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = pin,
                onValueChange = onPinChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(pinLabel) },
                enabled = signInEnabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                visualTransformation = pinVisualTransformation,
                trailingIcon = {
                    TextButton(onClick = { pinVisible = !pinVisible }) {
                        Text(
                            text =
                                stringResource(
                                    if (pinVisible) {
                                        R.string.hide_pin
                                    } else {
                                        R.string.show_pin
                                    }
                                )
                        )
                    }
                },
                keyboardActions = KeyboardActions(onDone = { if (signInEnabled) onPrimaryAction() })
            )
            if (mode != LoginScreenMode.SIGN_IN) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pinConfirmation,
                    onValueChange = onPinConfirmationChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.confirm_pin_hint)) },
                    enabled = signInEnabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = pinVisualTransformation,
                    trailingIcon = {
                        TextButton(onClick = { pinVisible = !pinVisible }) {
                            Text(
                                text =
                                    stringResource(
                                        if (pinVisible) {
                                            R.string.hide_pin
                                        } else {
                                            R.string.show_pin
                                        }
                                    )
                            )
                        }
                    }
                )
            }
            if (mode == LoginScreenMode.SETUP) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = securityQuestion,
                    onValueChange = onSecurityQuestionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.security_question_hint)) },
                    enabled = signInEnabled,
                    singleLine = true
                )
            }
            if (mode == LoginScreenMode.SETUP || mode == LoginScreenMode.RECOVER_PIN) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = securityAnswer,
                    onValueChange = onSecurityAnswerChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            stringResource(
                                if (mode == LoginScreenMode.SETUP) {
                                    R.string.security_answer_hint
                                } else {
                                    R.string.security_answer_verify_hint
                                }
                            )
                        )
                    },
                    enabled = signInEnabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onPrimaryAction,
                enabled = signInEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(LoginButtonTag)
            ) {
                Text(primaryButtonLabel)
            }
            if (mode == LoginScreenMode.SIGN_IN) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onForgotPin,
                    enabled = signInEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.forgot_pin))
                }
                if (biometricAvailable) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onBiometricLogin,
                        enabled = signInEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = biometricContentDescription }
                            .testTag(BiometricButtonTag)
                    ) {
                        Text(stringResource(R.string.sign_in_with_biometrics))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onRequestWipeData,
                    enabled = signInEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = wipeActionContentDescription
                        }
                ) {
                    Text(
                        text = stringResource(R.string.wipe_all_data),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (mode == LoginScreenMode.RECOVER_PIN) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onBackToSignIn,
                    enabled = signInEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.back_to_sign_in))
                }
            }
        }
    }
}

@Composable
private fun SearchRoute(viewModel: SearchViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val results by viewModel.results.collectAsStateWithLifecycle()
    var description by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var minAmount by rememberSaveable { mutableStateOf("") }
    var maxAmount by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }
    val invalidFiltersMessage = stringResource(R.string.invalid_search_filters_message)
    val startDatePlaceholder = stringResource(R.string.search_start_date_hint)
    val endDatePlaceholder = stringResource(R.string.search_end_date_hint)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_description_hint)) },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = categoryId,
            onValueChange = { categoryId = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search_category_id_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = minAmount,
                onValueChange = { minAmount = it },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.search_min_amount_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = maxAmount,
                onValueChange = { maxAmount = it },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.search_max_amount_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DateSelectorButton(
                text = startDate.ifBlank { startDatePlaceholder },
                onClick = {
                    launchDatePicker(context, startDate) { startDate = it }
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            DateSelectorButton(
                text = endDate.ifBlank { endDatePlaceholder },
                onClick = {
                    launchDatePicker(context, endDate) { endDate = it }
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val filters = runCatching {
                    SearchFilterParser.parse(
                        description = description,
                        categoryId = categoryId,
                        minAmount = minAmount,
                        maxAmount = maxAmount,
                        startDate = startDate,
                        endDate = endDate
                    )
                }.getOrElse {
                    Toast.makeText(context, invalidFiltersMessage, Toast.LENGTH_SHORT).show()
                    return@Button
                }
                viewModel.search(filters)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.search))
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(results) { result ->
                Text(
                    text = result,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ReportRoute(viewModel: ReportViewModel = hiltViewModel()) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        val now = LocalDate.now()
        viewModel.load(now.year, now.monthValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        report?.let {
            Text(
                text = stringResource(
                    R.string.report_summary,
                    it.monthLabel,
                    it.totalIncome,
                    it.totalExpense,
                    it.net
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            SelectionContainer {
                Text(text = it.csv)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: SettingsScreenState,
    onSignOut: () -> Unit,
    onResetSecurityProfile: () -> Unit,
    onManageCategories: () -> Unit = {},
    onManageAccounts: () -> Unit = {},
    onManageBudgets: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_session_timeout, state.sessionTimeoutMinutes),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                if (state.pinConfigured) {
                    R.string.settings_pin_configured
                } else {
                    R.string.settings_pin_not_configured
                }
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onManageCategories, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.manage_categories))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onManageAccounts, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.manage_accounts))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onManageBudgets, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.manage_budgets))
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sign_out))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onResetSecurityProfile, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_recover_pin))
        }
    }
}

@Composable
private fun AddTransactionRoute(
    onFinished: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val categories by viewModel.categoryOptions.collectAsStateWithLifecycle()
    val accounts by viewModel.accountOptions.collectAsStateWithLifecycle()
    var formKey by rememberSaveable { mutableIntStateOf(0) }
    val savedMessage = stringResource(R.string.transaction_saved)
    val errorMessage = stringResource(R.string.generic_error)
    val notFoundMessage = stringResource(R.string.transaction_not_found)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                TransactionEvent.Saved -> {
                    Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
                    if (viewModel.isEditMode) onFinished() else formKey++
                }
                is TransactionEvent.Failed ->
                    Toast.makeText(context, event.message ?: errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    when (val state = formState) {
        TransactionFormState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        TransactionFormState.NotFound -> LaunchedEffect(Unit) {
            Toast.makeText(context, notFoundMessage, Toast.LENGTH_SHORT).show()
            onFinished()
        }
        is TransactionFormState.Ready -> key(formKey) {
            AddTransactionScreen(
                onSave = viewModel::saveTransaction,
                onShowMessage = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                categories = categories,
                accounts = accounts,
                initial = state.initial,
                onBack = if (viewModel.isEditMode) onFinished else null
            )
        }
    }
}

@Composable
fun AddTransactionScreen(
    onSave: (String, String, Double, Long, Long, Long?, Long) -> Unit,
    onShowMessage: (String) -> Unit,
    categories: List<PickerOption> = emptyList(),
    accounts: List<PickerOption> = emptyList(),
    initial: TransactionFormInput? = null,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val transactionTypes = stringArrayResource(R.array.transaction_types)
    val startType = initial?.type ?: transactionTypes.firstOrNull().orEmpty()
    val startDefaults = remember(startType) {
        applyTransactionTypeDefaults(
            selectedType = startType,
            currentCategoryId = "",
            lastAutoCategoryId = null,
            accountId = "",
            transferAccountId = ""
        )
    }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    var selectedType by rememberSaveable { mutableStateOf(startType) }
    var amount by rememberSaveable { mutableStateOf(initial?.amount.orEmpty()) }
    var categoryId by rememberSaveable { mutableStateOf(initial?.categoryId ?: startDefaults.categoryId) }
    var accountId by rememberSaveable { mutableStateOf(initial?.accountId ?: startDefaults.accountId) }
    var transferAccountId by rememberSaveable { mutableStateOf(initial?.transferAccountId ?: startDefaults.transferAccountId) }
    var lastAutoCategoryId by rememberSaveable { mutableStateOf(startDefaults.lastAutoCategoryId) }
    var dateText by rememberSaveable { mutableStateOf(initial?.dateText.orEmpty()) }
    var dropdownExpanded by rememberSaveable { mutableStateOf(false) }
    val invalidDateMessage = stringResource(R.string.invalid_date_message)
    val referenceRequiredMessage = stringResource(R.string.transaction_reference_required_message)
    val datePlaceholder = stringResource(R.string.date_hint)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (initial != null) {
            ScreenHeader(title = stringResource(R.string.edit_transaction), onBack = onBack)
        }
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.transaction_description_hint)) },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
        ) {
            OutlinedTextField(
                value = selectedType,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.transaction_type_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag(TransactionTypeFieldTag)
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                transactionTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            dropdownExpanded = false
                            selectedType = type
                            val defaults = applyTransactionTypeDefaults(
                                selectedType = type,
                                currentCategoryId = categoryId,
                                lastAutoCategoryId = lastAutoCategoryId,
                                accountId = accountId,
                                transferAccountId = transferAccountId
                            )
                            categoryId = defaults.categoryId
                            accountId = defaults.accountId
                            transferAccountId = defaults.transferAccountId
                            lastAutoCategoryId = defaults.lastAutoCategoryId
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.transaction_amount_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(modifier = Modifier.height(8.dp))
        ReferencePicker(
            label = stringResource(R.string.transaction_category_label),
            options = categories,
            selectedId = categoryId,
            onSelected = { categoryId = it },
            fieldModifier = Modifier.testTag(CategoryPickerTag)
        )
        Spacer(modifier = Modifier.height(8.dp))
        ReferencePicker(
            label = stringResource(R.string.transaction_account_label),
            options = accounts,
            selectedId = accountId,
            onSelected = { accountId = it },
            fieldModifier = Modifier.testTag(AccountPickerTag)
        )
        if (selectedType == "TRANSFER") {
            Spacer(modifier = Modifier.height(8.dp))
            ReferencePicker(
                label = stringResource(R.string.transaction_transfer_account_label),
                options = accounts,
                selectedId = transferAccountId,
                onSelected = { transferAccountId = it },
                fieldModifier = Modifier.testTag(TransferAccountFieldTag)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        DateSelectorButton(
            text = dateText.ifBlank { datePlaceholder },
            onClick = { launchDatePicker(context, dateText) { dateText = it } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val parsedCategoryId = categoryId.toLongOrNull()
                    ?: return@Button onShowMessage(referenceRequiredMessage)
                val parsedAccountId = accountId.toLongOrNull()
                    ?: return@Button onShowMessage(referenceRequiredMessage)
                val parsedTransferAccountId = if (selectedType == "TRANSFER") {
                    transferAccountId.toLongOrNull()
                        ?: return@Button onShowMessage(referenceRequiredMessage)
                } else {
                    null
                }
                val dateEpochMillis = when {
                    dateText.isBlank() -> System.currentTimeMillis()
                    else -> runCatching {
                        initial?.resolveDateEpochMillis(dateText, DateUtils::parseIsoDateToStartOfDayEpochMillis)
                            ?: DateUtils.parseIsoDateToStartOfDayEpochMillis(dateText)
                    }.getOrElse {
                        onShowMessage(invalidDateMessage)
                        return@Button
                    }
                }
                onSave(
                    selectedType,
                    description,
                    amount.toDoubleOrNull() ?: 0.0,
                    parsedCategoryId,
                    parsedAccountId,
                    parsedTransferAccountId,
                    dateEpochMillis
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SaveTransactionButtonTag)
        ) {
            Text(stringResource(if (initial == null) R.string.save_transaction else R.string.update_transaction))
        }
    }
}

@Composable
private fun DateSelectorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(onClick = onClick, modifier = modifier) {
        Text(text = text)
    }
}

private fun launchDatePicker(
    context: android.content.Context,
    value: String,
    onDateSelected: (String) -> Unit
) {
    val selectedDate = value.takeIf { it.isNotBlank() }?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    } ?: LocalDate.now()
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth).toString())
        },
        selectedDate.year,
        selectedDate.monthValue - 1,
        selectedDate.dayOfMonth
    ).show()
}

internal data class TransactionTypeDefaults(
    val categoryId: String,
    val lastAutoCategoryId: String,
    val accountId: String,
    val transferAccountId: String
)

internal fun applyTransactionTypeDefaults(
    selectedType: String,
    currentCategoryId: String,
    lastAutoCategoryId: String?,
    accountId: String,
    transferAccountId: String
): TransactionTypeDefaults {
    val nextDefaultCategoryId = defaultCategoryId(selectedType).toString()
    return TransactionTypeDefaults(
        categoryId = if (currentCategoryId.isBlank() || currentCategoryId == lastAutoCategoryId) {
            nextDefaultCategoryId
        } else {
            currentCategoryId
        },
        lastAutoCategoryId = nextDefaultCategoryId,
        accountId = accountId.ifBlank {
            Constants.DEFAULT_CASH_ACCOUNT_ID.toString()
        },
        transferAccountId = if (selectedType == "TRANSFER") {
            Constants.DEFAULT_SAVINGS_ACCOUNT_ID.toString()
        } else {
            ""
        }
    )
}

internal fun defaultCategoryId(type: String): Long {
    return when (type) {
        "INCOME" -> Constants.DEFAULT_INCOME_CATEGORY_ID
        "TRANSFER" -> Constants.DEFAULT_TRANSFER_CATEGORY_ID
        else -> Constants.DEFAULT_EXPENSE_CATEGORY_ID
    }
}
