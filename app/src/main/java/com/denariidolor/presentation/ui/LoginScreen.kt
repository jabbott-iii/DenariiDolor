/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.denariidolor.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.denariidolor.R
import com.denariidolor.data.local.preferences.SecurityProfileService
import kotlinx.coroutines.delay

internal const val LOGIN_BUTTON_TAG = "loginButton"
internal const val BIOMETRIC_BUTTON_TAG = "biometricButton"
internal const val WIPE_CONFIRMATION_FIELD_TAG = "wipeConfirmationField"
internal const val WIPE_CONFIRM_BUTTON_TAG = "wipeConfirmButton"
internal const val WIPE_COUNTDOWN_SECONDS = 10

enum class LoginScreenMode {
    SETUP,
    SIGN_IN,
    RECOVER_PIN,

    /** A v1 profile accepted the user; a new security question and answer finish the upgrade. */
    UPGRADE_RECOVERY,

    /** The encrypted profile can't be read: retry, or wipe and start again. */
    STORAGE_ERROR
}

private val PIN_MODES = setOf(LoginScreenMode.SETUP, LoginScreenMode.SIGN_IN, LoginScreenMode.RECOVER_PIN)
private val NEW_ANSWER_MODES = setOf(LoginScreenMode.SETUP, LoginScreenMode.UPGRADE_RECOVERY)
private val BACK_TO_SIGN_IN_MODES = setOf(LoginScreenMode.RECOVER_PIN, LoginScreenMode.UPGRADE_RECOVERY)

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
    BackHandler(enabled = mode in BACK_TO_SIGN_IN_MODES && signInEnabled, onBack = onBackToSignIn)

    if (showWipeConfirmation) {
        WipeConfirmationDialog(onCancel = onCancelWipeData, onConfirm = onConfirmWipeData)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            if (!signInEnabled) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(16.dp))
            }
            ModeExplanation(mode = mode, recoveryQuestion = recoveryQuestion)
            if (feedbackMessage != null) {
                Text(text = feedbackMessage)
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (mode in PIN_MODES) {
                PinFields(
                    mode = mode,
                    pin = pin,
                    pinConfirmation = pinConfirmation,
                    enabled = signInEnabled,
                    onPinChange = onPinChange,
                    onPinConfirmationChange = onPinConfirmationChange,
                    onDone = onPrimaryAction
                )
            }
            if (mode in NEW_ANSWER_MODES) {
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
            if (mode in NEW_ANSWER_MODES || mode == LoginScreenMode.RECOVER_PIN) {
                Spacer(modifier = Modifier.height(8.dp))
                SecurityAnswerField(
                    value = securityAnswer,
                    onValueChange = onSecurityAnswerChange,
                    enabled = signInEnabled,
                    newAnswer = mode in NEW_ANSWER_MODES
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onPrimaryAction,
                enabled = signInEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(LOGIN_BUTTON_TAG)
            ) {
                Text(primaryActionLabel(mode))
            }
            SecondaryActions(
                mode = mode,
                enabled = signInEnabled,
                biometricAvailable = biometricAvailable,
                onForgotPin = onForgotPin,
                onBackToSignIn = onBackToSignIn,
                onBiometricLogin = onBiometricLogin,
                onRequestWipeData = onRequestWipeData
            )
        }
    }
}

@Composable
private fun primaryActionLabel(mode: LoginScreenMode): String = stringResource(
    when (mode) {
        LoginScreenMode.SIGN_IN -> R.string.sign_in
        LoginScreenMode.SETUP -> R.string.create_security_profile
        LoginScreenMode.RECOVER_PIN -> R.string.reset_pin
        LoginScreenMode.UPGRADE_RECOVERY -> R.string.save_and_continue
        LoginScreenMode.STORAGE_ERROR -> R.string.try_again
    }
)

@Composable
private fun ModeExplanation(mode: LoginScreenMode, recoveryQuestion: String?) {
    val text = when (mode) {
        LoginScreenMode.RECOVER_PIN -> recoveryQuestion ?: stringResource(R.string.security_question_unavailable)
        LoginScreenMode.UPGRADE_RECOVERY -> stringResource(R.string.upgrade_recovery_intro)
        LoginScreenMode.STORAGE_ERROR -> stringResource(R.string.storage_error_message)
        LoginScreenMode.SETUP, LoginScreenMode.SIGN_IN -> null
    }
    if (text != null) {
        Text(text = text)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PinFields(
    mode: LoginScreenMode,
    pin: String,
    pinConfirmation: String,
    enabled: Boolean,
    onPinChange: (String) -> Unit,
    onPinConfirmationChange: (String) -> Unit,
    onDone: () -> Unit
) {
    var pinVisible by rememberSaveable { mutableStateOf(false) }
    val visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation()
    val visibilityToggle: @Composable () -> Unit = {
        TextButton(onClick = { pinVisible = !pinVisible }) {
            Text(stringResource(if (pinVisible) R.string.hide_pin else R.string.show_pin))
        }
    }
    OutlinedTextField(
        value = pin,
        onValueChange = onPinChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(if (mode == LoginScreenMode.RECOVER_PIN) R.string.new_pin_hint else R.string.pin_hint)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        visualTransformation = visualTransformation,
        trailingIcon = visibilityToggle,
        keyboardActions = KeyboardActions(onDone = { if (enabled) onDone() })
    )
    if (mode != LoginScreenMode.SIGN_IN) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pinConfirmation,
            onValueChange = onPinConfirmationChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.confirm_pin_hint)) },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = visualTransformation,
            trailingIcon = visibilityToggle
        )
    }
}

@Composable
private fun SecurityAnswerField(value: String, onValueChange: (String) -> Unit, enabled: Boolean, newAnswer: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(if (newAnswer) R.string.security_answer_hint else R.string.security_answer_verify_hint)) },
        supportingText = if (newAnswer) {
            { Text(stringResource(R.string.security_answer_rules, SecurityProfileService.MIN_ANSWER_LENGTH)) }
        } else {
            null
        },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    )
}

@Composable
private fun SecondaryActions(
    mode: LoginScreenMode,
    enabled: Boolean,
    biometricAvailable: Boolean,
    onForgotPin: () -> Unit,
    onBackToSignIn: () -> Unit,
    onBiometricLogin: () -> Unit,
    onRequestWipeData: () -> Unit
) {
    val biometricContentDescription = stringResource(R.string.biometric_sign_in_accessibility_label)
    if (mode == LoginScreenMode.SIGN_IN) {
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onForgotPin, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.forgot_pin))
        }
        if (biometricAvailable) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onBiometricLogin,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = biometricContentDescription }
                    .testTag(BIOMETRIC_BUTTON_TAG)
            ) {
                Text(stringResource(R.string.sign_in_with_biometrics))
            }
        }
    }
    if (mode == LoginScreenMode.SIGN_IN || mode == LoginScreenMode.STORAGE_ERROR) {
        Spacer(modifier = Modifier.height(8.dp))
        WipeDataButton(enabled = enabled, onClick = onRequestWipeData)
    }
    if (mode in BACK_TO_SIGN_IN_MODES) {
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onBackToSignIn, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back_to_sign_in))
        }
    }
}

@Composable
private fun WipeDataButton(enabled: Boolean, onClick: () -> Unit) {
    val wipeActionContentDescription = stringResource(R.string.wipe_all_data_destructive_label)
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = wipeActionContentDescription }
    ) {
        Text(text = stringResource(R.string.wipe_all_data), color = MaterialTheme.colorScheme.error)
    }
}

/** Wiping needs no PIN (it is the way out of a forgotten PIN and answer), so it asks for a typed word and a short wait (CS-16). */
@Composable
private fun WipeConfirmationDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    val confirmationWord = stringResource(R.string.wipe_data_confirmation_word)
    val confirmContentDescription = stringResource(R.string.wipe_data_confirm_destructive_label)
    var typed by rememberSaveable { mutableStateOf("") }
    var secondsLeft by rememberSaveable { mutableIntStateOf(WIPE_COUNTDOWN_SECONDS) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1_000L)
            secondsLeft--
        }
    }
    val confirmed = secondsLeft == 0 && typed.trim().equals(confirmationWord, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.wipe_data_title)) },
        text = {
            Column {
                Text(stringResource(R.string.wipe_data_warning))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    label = { Text(stringResource(R.string.wipe_data_type_to_confirm, confirmationWord)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(WIPE_CONFIRMATION_FIELD_TAG)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = confirmed,
                modifier = Modifier
                    .semantics { contentDescription = confirmContentDescription }
                    .testTag(WIPE_CONFIRM_BUTTON_TAG)
            ) {
                Text(
                    if (secondsLeft > 0) {
                        stringResource(R.string.wipe_data_confirm_countdown, secondsLeft)
                    } else {
                        stringResource(R.string.wipe_data_confirm)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.wipe_data_cancel))
            }
        }
    )
}
