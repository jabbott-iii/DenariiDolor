@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.denariidolor.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denariidolor.R
import com.denariidolor.presentation.ui.settings.SettingsScreenState

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
