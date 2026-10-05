/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.denariidolor.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denariidolor.R
import com.denariidolor.presentation.ui.settings.SettingsScreenState
import java.util.Currency

internal const val DARK_MODE_SWITCH_TAG = "darkModeSwitch"
internal const val BIOMETRIC_SWITCH_TAG = "biometricSwitch"
internal const val CURRENCY_SETTING_TAG = "currencySetting"
internal const val CURRENCY_OPTION_TAG_PREFIX = "currencyOption_"

@Composable
fun SettingsScreen(
    state: SettingsScreenState,
    onSignOut: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit = {},
    onBiometricChange: (Boolean) -> Unit = {},
    onCurrencyChange: (String) -> Unit = {},
    onManageCategories: () -> Unit = {},
    onManageAccounts: () -> Unit = {},
    onManageBudgets: () -> Unit = {},
    onBackup: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onLicenses: () -> Unit = {}
) {
    var showCurrencyDialog by rememberSaveable { mutableStateOf(false) }
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
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = state.darkMode, role = Role.Switch, onValueChange = onDarkModeChange)
                .padding(vertical = 8.dp)
                .testTag(DARK_MODE_SWITCH_TAG),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.settings_dark_mode), modifier = Modifier.weight(1f))
            Switch(checked = state.darkMode, onCheckedChange = null)
        }
        if (state.biometricAvailable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = state.biometricEnabled, role = Role.Switch, onValueChange = onBiometricChange)
                    .padding(vertical = 8.dp)
                    .testTag(BIOMETRIC_SWITCH_TAG),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.settings_biometric_sign_in), modifier = Modifier.weight(1f))
                Switch(checked = state.biometricEnabled, onCheckedChange = null)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = state.currencyOptions.isNotEmpty(), role = Role.Button) { showCurrencyDialog = true }
                .padding(vertical = 8.dp)
                .testTag(CURRENCY_SETTING_TAG),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.settings_currency), modifier = Modifier.weight(1f))
            Text(text = currencyLabel(state.currencyCode), color = MaterialTheme.colorScheme.primary)
        }
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
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_backup_restore))
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onPrivacyPolicy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_privacy_policy))
        }
        TextButton(onClick = onLicenses, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_licenses))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sign_out))
        }
    }
    if (showCurrencyDialog) {
        CurrencyDialog(
            selected = state.currencyCode,
            options = state.currencyOptions,
            onSelect = { code ->
                showCurrencyDialog = false
                onCurrencyChange(code)
            },
            onDismiss = { showCurrencyDialog = false }
        )
    }
}

/** "US Dollar ($)", in the device's language. */
@Composable
private fun currencyLabel(code: String): String {
    val locale = LocalLocale.current.platformLocale
    val currency = Currency.getInstance(code)
    return stringResource(R.string.settings_currency_value, currency.getDisplayName(locale), currency.getSymbol(locale))
}

@Composable
private fun CurrencyDialog(selected: String, options: List<String>, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_currency_title)) },
        text = {
            Column {
                Text(text = stringResource(R.string.settings_currency_note), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(options) { code ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = code == selected, role = Role.RadioButton) { onSelect(code) }
                                .padding(vertical = 4.dp)
                                .testTag(CURRENCY_OPTION_TAG_PREFIX + code),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = code == selected, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(currencyLabel(code))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
