/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.denariidolor.R
import com.denariidolor.data.backup.BackupError
import com.denariidolor.data.backup.BackupException
import com.denariidolor.data.backup.BackupFile
import com.denariidolor.data.backup.BackupService
import com.denariidolor.data.backup.BackupSnapshot
import com.denariidolor.presentation.ui.common.UiMessage
import com.denariidolor.util.runSuspendCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What a backup holds, shown before it replaces the data on this phone. */
data class BackupPreview(val createdAt: ZonedDateTime, val accounts: Int, val categories: Int, val budgets: Int, val transactions: Int)

sealed interface BackupDialog {
    data class NewPassphrase(val error: BackupError? = null) : BackupDialog
    data class RestorePassphrase(val uri: Uri, val error: BackupError? = null) : BackupDialog
    data class ConfirmRestore(val preview: BackupPreview) : BackupDialog
}

data class BackupUiState(val dialog: BackupDialog? = null, val working: Boolean = false)

sealed interface BackupEvent {
    /** Open the system file picker to create the backup file. */
    data class ChooseDestination(val fileName: String) : BackupEvent
    data class Message(val message: UiMessage) : BackupEvent

    /** The data was replaced; every screen must start over. */
    data object Restored : BackupEvent
}

/**
 * Backup: passphrase → encrypted file in memory → destination → saved. Restore: file → passphrase → preview → typed confirmation
 * → replace. One step runs at a time (BUG-02). The file is encrypted before the picker opens, so no passphrase is held while the
 * user browses; the encrypted file is kept until the destination is chosen, and a decrypted backup until the restore is
 * confirmed or cancelled. None of them goes into saved state.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(private val backupService: BackupService, private val clock: Clock) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    private val _events = Channel<BackupEvent>(Channel.BUFFERED)
    val events: Flow<BackupEvent> = _events.receiveAsFlow()

    private var pendingFile: ByteArray? = null
    private var pendingSnapshot: BackupSnapshot? = null

    fun startBackup() {
        if (!_state.value.working) showDialog(BackupDialog.NewPassphrase())
    }

    fun submitNewPassphrase(passphrase: String, confirmation: String) {
        val dialog = _state.value.dialog as? BackupDialog.NewPassphrase ?: return
        val problem = BackupFile.checkPassphrase(passphrase, confirmation)
        if (problem != null) {
            showDialog(dialog.copy(error = problem))
            return
        }
        work {
            runSuspendCatching { backupService.create(passphrase) }
                .onSuccess { file ->
                    pendingFile = file
                    showDialog(null)
                    _events.send(BackupEvent.ChooseDestination(fileName(LocalDate.now(clock))))
                }
                .onFailure {
                    showDialog(null)
                    _events.send(BackupEvent.Message(UiMessage.Resource(R.string.backup_save_failed)))
                }
        }
    }

    /**
     * The file picker's result; null when it was cancelled. A destination without an encrypted file means the screen was
     * recreated while the picker was open; the picker's empty file is left alone, since it may be one the user chose to replace.
     */
    fun onDestinationChosen(uri: Uri?) {
        val file = pendingFile
        pendingFile = null
        when {
            uri == null -> Unit
            file == null -> viewModelScope.launch { _events.send(BackupEvent.Message(UiMessage.Resource(R.string.backup_save_failed))) }
            else -> work {
                val saved = runSuspendCatching { backupService.save(uri, file) }.isSuccess
                _events.send(BackupEvent.Message(UiMessage.Resource(if (saved) R.string.backup_saved else R.string.backup_save_failed)))
            }
        }
    }

    /** The file picker's result; null when it was cancelled. */
    fun onBackupFileChosen(uri: Uri?) {
        if (uri != null && !_state.value.working) showDialog(BackupDialog.RestorePassphrase(uri))
    }

    fun submitRestorePassphrase(passphrase: String) {
        val dialog = _state.value.dialog as? BackupDialog.RestorePassphrase ?: return
        work {
            runSuspendCatching { backupService.open(dialog.uri, passphrase) }
                .onSuccess { snapshot ->
                    pendingSnapshot = snapshot
                    showDialog(BackupDialog.ConfirmRestore(snapshot.preview()))
                }
                .onFailure { error -> showDialog(dialog.copy(error = openError(error))) }
        }
    }

    fun confirmRestore() {
        if (_state.value.dialog !is BackupDialog.ConfirmRestore) return
        val snapshot = pendingSnapshot ?: return
        work {
            val restored = runSuspendCatching { backupService.restore(snapshot) }.isSuccess
            pendingSnapshot = null
            showDialog(null)
            _events.send(if (restored) BackupEvent.Restored else BackupEvent.Message(UiMessage.Resource(R.string.backup_restore_failed)))
        }
    }

    fun dismissDialog() {
        if (_state.value.working) return
        pendingFile = null
        pendingSnapshot = null
        showDialog(null)
    }

    private fun showDialog(dialog: BackupDialog?) = _state.update { it.copy(dialog = dialog) }

    private fun work(action: suspend () -> Unit) {
        if (_state.value.working) return
        _state.update { it.copy(working = true) }
        viewModelScope.launch {
            try {
                action()
            } finally {
                _state.update { it.copy(working = false) }
            }
        }
    }

    private fun BackupSnapshot.preview() = BackupPreview(
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis).atZone(clock.zone),
        accounts = accounts.size,
        categories = categories.size,
        budgets = budgets.size,
        transactions = transactions.size
    )

    companion object {
        fun fileName(date: LocalDate): String = "denarii-dolor-backup-$date.${BackupFile.FILE_EXTENSION}"

        /** Anything that isn't a [BackupException] means the file couldn't be read: the provider failed or the grant was revoked. */
        fun openError(error: Throwable): BackupError = (error as? BackupException)?.error ?: BackupError.READ_FAILED
    }
}
