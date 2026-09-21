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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denariidolor.R
import com.denariidolor.presentation.ui.settings.SettingsScreenState

internal const val DarkModeSwitchTag = "darkModeSwitch"

@Composable
fun SettingsScreen(
    state: SettingsScreenState,
    onSignOut: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit = {},
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
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = state.darkMode, role = Role.Switch, onValueChange = onDarkModeChange)
                .padding(vertical = 8.dp)
                .testTag(DarkModeSwitchTag),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.settings_dark_mode), modifier = Modifier.weight(1f))
            Switch(checked = state.darkMode, onCheckedChange = null)
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
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sign_out))
        }
    }
}
