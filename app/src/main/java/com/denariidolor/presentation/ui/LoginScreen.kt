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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

internal const val LoginButtonTag = "loginButton"
internal const val BiometricButtonTag = "biometricButton"

enum class LoginScreenMode {
    SETUP,
    SIGN_IN,
    RECOVER_PIN
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
