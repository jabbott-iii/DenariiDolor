/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.backup

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.denariidolor.R
import com.denariidolor.data.backup.BackupError
import com.denariidolor.data.backup.BackupFile
import com.denariidolor.presentation.ui.common.ScreenHeader
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal const val BACKUP_CREATE_BUTTON_TAG = "backupCreate"
internal const val BACKUP_RESTORE_BUTTON_TAG = "backupRestore"
internal const val BACKUP_PASSPHRASE_FIELD_TAG = "backupPassphrase"
internal const val BACKUP_PASSPHRASE_CONFIRMATION_FIELD_TAG = "backupPassphraseConfirmation"
internal const val BACKUP_PASSPHRASE_CONTINUE_TAG = "backupPassphraseContinue"
internal const val RESTORE_CONFIRMATION_FIELD_TAG = "restoreConfirmation"
internal const val RESTORE_CONFIRM_BUTTON_TAG = "restoreConfirm"

@Composable
fun BackupRoute(onBack: () -> Unit, onRestored: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooseDestination = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupFile.MIME_TYPE)) {
        viewModel.onDestinationChosen(it)
    }
    // Backups made elsewhere may carry any type, depending on where they were stored.
    val chooseBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { viewModel.onBackupFileChosen(it) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BackupEvent.ChooseDestination -> chooseDestination.launchOrExplain(event.fileName, context, R.string.backup_save_failed)
                is BackupEvent.Message -> toast(context, event.message.resolve(context))
                BackupEvent.Restored -> {
                    toast(context, context.getString(R.string.backup_restored))
                    onRestored()
                }
            }
        }
    }

    BackupScreen(
        state = state,
        onBack = onBack,
        onCreate = viewModel::startBackup,
        onRestore = { chooseBackup.launchOrExplain(arrayOf("*/*"), context, R.string.backup_error_read) },
        onSubmitNewPassphrase = viewModel::submitNewPassphrase,
        onSubmitRestorePassphrase = viewModel::submitRestorePassphrase,
        onConfirmRestore = viewModel::confirmRestore,
        onDismissDialog = viewModel::dismissDialog
    )
}

// A device without a document picker (some managed or TV builds) can't open one; say so instead of crashing.
private fun <I> ActivityResultLauncher<I>.launchOrExplain(input: I, context: Context, @StringRes failure: Int) {
    try {
        launch(input)
    } catch (_: ActivityNotFoundException) {
        toast(context, context.getString(failure))
    }
}

private fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

@Composable
fun BackupScreen(
    state: BackupUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onRestore: () -> Unit,
    onSubmitNewPassphrase: (passphrase: String, confirmation: String) -> Unit,
    onSubmitRestorePassphrase: (String) -> Unit,
    onConfirmRestore: () -> Unit,
    onDismissDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(title = stringResource(R.string.settings_backup_restore), onBack = onBack)
        Text(stringResource(R.string.backup_explanation))
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.backup_passphrase_warning), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreate,
            enabled = !state.working,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(BACKUP_CREATE_BUTTON_TAG)
        ) {
            Text(stringResource(R.string.backup_create))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onRestore,
            enabled = !state.working,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(BACKUP_RESTORE_BUTTON_TAG)
        ) {
            Text(stringResource(R.string.backup_restore))
        }
        if (state.working) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.backup_working))
            }
        }
    }

    when (val dialog = state.dialog) {
        is BackupDialog.NewPassphrase -> PassphraseDialog(
            title = stringResource(R.string.backup_create_title),
            needsConfirmation = true,
            error = dialog.error,
            working = state.working,
            onSubmit = onSubmitNewPassphrase,
            onDismiss = onDismissDialog
        )
        is BackupDialog.RestorePassphrase -> PassphraseDialog(
            title = stringResource(R.string.backup_restore_title),
            needsConfirmation = false,
            error = dialog.error,
            working = state.working,
            onSubmit = { passphrase, _ -> onSubmitRestorePassphrase(passphrase) },
            onDismiss = onDismissDialog
        )
        is BackupDialog.ConfirmRestore -> RestoreConfirmationDialog(
            preview = dialog.preview,
            working = state.working,
            onConfirm = onConfirmRestore,
            onDismiss = onDismissDialog
        )
        null -> Unit
    }
}

// The fields use remember, not rememberSaveable: a passphrase must not end up in saved instance state.
@Composable
private fun PassphraseDialog(
    title: String,
    needsConfirmation: Boolean,
    error: BackupError?,
    working: Boolean,
    onSubmit: (passphrase: String, confirmation: String) -> Unit,
    onDismiss: () -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val canSubmit = !working && passphrase.isNotBlank() && (!needsConfirmation || confirmation.isNotEmpty())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PassphraseField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = stringResource(R.string.backup_passphrase),
                    enabled = !working,
                    supportingText = if (needsConfirmation) {
                        stringResource(R.string.backup_passphrase_rules, BackupFile.MIN_PASSPHRASE_LENGTH)
                    } else {
                        null
                    },
                    modifier = Modifier.testTag(BACKUP_PASSPHRASE_FIELD_TAG)
                )
                if (needsConfirmation) {
                    PassphraseField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = stringResource(R.string.backup_passphrase_confirm),
                        enabled = !working,
                        modifier = Modifier.testTag(BACKUP_PASSPHRASE_CONFIRMATION_FIELD_TAG)
                    )
                }
                if (error != null) Text(backupErrorText(error), color = MaterialTheme.colorScheme.error)
                if (working) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(passphrase, confirmation) },
                enabled = canSubmit,
                modifier = Modifier.testTag(BACKUP_PASSPHRASE_CONTINUE_TAG)
            ) {
                Text(stringResource(R.string.backup_continue))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun PassphraseField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = if (supportingText != null) {
            { Text(supportingText) }
        } else {
            null
        },
        enabled = enabled,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = modifier.fillMaxWidth()
    )
}

/** Replacing everything can't be undone, so it shows what the backup holds and asks for a typed word, like wiping does (CS-16). */
@Composable
private fun RestoreConfirmationDialog(preview: BackupPreview, working: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val confirmationWord = stringResource(R.string.backup_restore_confirmation_word)
    var typed by remember { mutableStateOf("") }
    val locale = LocalLocale.current.platformLocale
    val created = remember(preview.createdAt, locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale).format(preview.createdAt)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        R.string.backup_restore_contents,
                        created,
                        preview.accounts,
                        preview.categories,
                        preview.budgets,
                        preview.transactions
                    )
                )
                Text(stringResource(R.string.backup_restore_warning), color = MaterialTheme.colorScheme.error)
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    label = { Text(stringResource(R.string.wipe_data_type_to_confirm, confirmationWord)) },
                    enabled = !working,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(RESTORE_CONFIRMATION_FIELD_TAG)
                )
                if (working) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !working && typed.trim().equals(confirmationWord, ignoreCase = true),
                modifier = Modifier.testTag(RESTORE_CONFIRM_BUTTON_TAG)
            ) {
                Text(stringResource(R.string.backup_restore_action), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun backupErrorText(error: BackupError): String = when (error) {
    BackupError.PASSPHRASE_LENGTH -> stringResource(
        R.string.backup_error_passphrase_length,
        BackupFile.MIN_PASSPHRASE_LENGTH,
        BackupFile.MAX_PASSPHRASE_LENGTH
    )
    BackupError.PASSPHRASE_MISMATCH -> stringResource(R.string.backup_error_passphrase_mismatch)
    BackupError.NOT_A_BACKUP -> stringResource(R.string.backup_error_not_backup)
    BackupError.NEWER_VERSION -> stringResource(R.string.backup_error_newer_version)
    BackupError.WRONG_PASSPHRASE -> stringResource(R.string.backup_error_wrong_passphrase)
    BackupError.DAMAGED -> stringResource(R.string.backup_error_damaged)
    BackupError.READ_FAILED -> stringResource(R.string.backup_error_read)
}
