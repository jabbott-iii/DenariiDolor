/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.denariidolor.data.backup.BackupError
import com.denariidolor.data.legal.LegalBlock
import com.denariidolor.data.legal.LegalDocument
import com.denariidolor.presentation.ui.backup.BACKUP_PASSPHRASE_CONFIRMATION_FIELD_TAG
import com.denariidolor.presentation.ui.backup.BACKUP_PASSPHRASE_CONTINUE_TAG
import com.denariidolor.presentation.ui.backup.BACKUP_PASSPHRASE_FIELD_TAG
import com.denariidolor.presentation.ui.backup.BackupDialog
import com.denariidolor.presentation.ui.backup.BackupPreview
import com.denariidolor.presentation.ui.backup.BackupScreen
import com.denariidolor.presentation.ui.backup.BackupUiState
import com.denariidolor.presentation.ui.backup.RESTORE_CONFIRMATION_FIELD_TAG
import com.denariidolor.presentation.ui.backup.RESTORE_CONFIRM_BUTTON_TAG
import com.denariidolor.presentation.ui.common.DenariiDolorTheme
import com.denariidolor.presentation.ui.legal.LegalDocumentScreen
import com.denariidolor.presentation.ui.legal.LegalUiState
import com.denariidolor.presentation.ui.settings.SettingsScreenState
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BackupAndLegalScreensTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun showBackup(state: BackupUiState, events: MutableList<String> = mutableListOf()) = composeRule.setContent {
        DenariiDolorTheme {
            BackupScreen(
                state = state,
                onBack = { events += "back" },
                onCreate = { events += "create" },
                onRestore = { events += "restore" },
                onSubmitNewPassphrase = { passphrase, confirmation -> events += "new:$passphrase|$confirmation" },
                onSubmitRestorePassphrase = { events += "open:$it" },
                onConfirmRestore = { events += "confirm" },
                onDismissDialog = { events += "dismiss" }
            )
        }
    }

    @Test
    fun settingsOpensBackupPrivacyPolicyAndLicenses() {
        val opened = mutableListOf<String>()
        composeRule.setContent {
            DenariiDolorTheme {
                SettingsScreen(
                    state = SettingsScreenState(sessionTimeoutMinutes = 5, pinConfigured = true, darkMode = false),
                    onSignOut = {},
                    onBackup = { opened += "backup" },
                    onPrivacyPolicy = { opened += "privacy" },
                    onLicenses = { opened += "licenses" }
                )
            }
        }

        composeRule.onNodeWithText("Backup and restore").performScrollTo().performClick()
        composeRule.onNodeWithText("Privacy policy").performScrollTo().performClick()
        composeRule.onNodeWithText("Open-source licenses").performScrollTo().performClick()

        assertEquals(listOf("backup", "privacy", "licenses"), opened)
    }

    @Test
    fun aNewPassphraseNeedsBothFieldsBeforeItCanBeSubmitted() {
        val events = mutableListOf<String>()
        showBackup(BackupUiState(dialog = BackupDialog.NewPassphrase()), events)

        composeRule.onNodeWithTag(BACKUP_PASSPHRASE_CONTINUE_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(BACKUP_PASSPHRASE_FIELD_TAG).performTextInput("correct horse battery")
        composeRule.onNodeWithTag(BACKUP_PASSPHRASE_CONTINUE_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(BACKUP_PASSPHRASE_CONFIRMATION_FIELD_TAG).performTextInput("correct horse battery")
        composeRule.onNodeWithTag(BACKUP_PASSPHRASE_CONTINUE_TAG).assertIsEnabled().performClick()

        composeRule.runOnIdle { assertEquals(listOf("new:correct horse battery|correct horse battery"), events) }
    }

    @Test
    fun passphraseProblemsAreExplained() {
        showBackup(BackupUiState(dialog = BackupDialog.NewPassphrase(BackupError.PASSPHRASE_LENGTH)))

        composeRule.onNodeWithText("The passphrase must be 12 to 256 characters.").assertIsDisplayed()
    }

    @Test
    fun aRestorePassphraseErrorIsExplained() {
        showBackup(BackupUiState(dialog = BackupDialog.RestorePassphrase(android.net.Uri.EMPTY, BackupError.WRONG_PASSPHRASE)))

        composeRule.onNodeWithText("Wrong passphrase, or the file is damaged.").assertIsDisplayed()
        composeRule.onNodeWithTag(BACKUP_PASSPHRASE_CONFIRMATION_FIELD_TAG).assertDoesNotExist()
    }

    @Test
    fun restoringShowsTheBackupsContentsAndNeedsTheTypedWord() {
        val events = mutableListOf<String>()
        val preview = BackupPreview(
            createdAt = ZonedDateTime.of(2026, 10, 4, 14, 30, 0, 0, ZoneOffset.UTC),
            accounts = 3,
            categories = 12,
            budgets = 4,
            transactions = 1_234
        )
        showBackup(BackupUiState(dialog = BackupDialog.ConfirmRestore(preview)), events)

        composeRule.onNodeWithText("Transactions: 1234", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(RESTORE_CONFIRM_BUTTON_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(RESTORE_CONFIRMATION_FIELD_TAG).performTextInput("restore")
        composeRule.onNodeWithTag(RESTORE_CONFIRM_BUTTON_TAG).assertIsEnabled().performClick()

        composeRule.runOnIdle { assertEquals(listOf("confirm"), events) }
    }

    @Test
    fun nothingCanStartWhileWorking() {
        showBackup(BackupUiState(working = true))

        composeRule.onNodeWithText("Create backup").assertIsNotEnabled()
        composeRule.onNodeWithText("Restore from backup").assertIsNotEnabled()
        composeRule.onNodeWithText("Working…").assertIsDisplayed()
    }

    @Test
    fun aLegalDocumentShowsItsBlocks() {
        val blocks = listOf(
            LegalBlock.Heading("Third-party notices", 1),
            LegalBlock.Paragraph("Each one is licensed under its own terms."),
            LegalBlock.Row(listOf("Room", "The Android Open Source Project", "Apache License 2.0")),
            LegalBlock.Bullet("Apache License 2.0: licenses/Apache-2.0.txt")
        )
        composeRule.setContent {
            DenariiDolorTheme {
                LegalDocumentScreen(document = LegalDocument.OPEN_SOURCE_LICENSES, state = LegalUiState.Loaded(blocks), onBack = {})
            }
        }

        composeRule.onNodeWithText("Open-source licenses").assertIsDisplayed()
        composeRule.onNodeWithText("Third-party notices").assertIsDisplayed()
        composeRule.onNodeWithText("The Android Open Source Project · Apache License 2.0").assertIsDisplayed()
    }
}
