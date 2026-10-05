/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.presentation.ui

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.denariidolor.R
import com.denariidolor.data.backup.BackupError
import com.denariidolor.data.backup.BackupException
import com.denariidolor.data.backup.BackupService
import com.denariidolor.data.backup.BackupSnapshot
import com.denariidolor.presentation.ui.backup.BackupDialog
import com.denariidolor.presentation.ui.backup.BackupEvent
import com.denariidolor.presentation.ui.backup.BackupPreview
import com.denariidolor.presentation.ui.backup.BackupViewModel
import com.denariidolor.presentation.ui.common.UiMessage
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a device because the ViewModel's API takes `android.net.Uri`, which JVM unit tests can't create. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BackupViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val collector = CoroutineScope(dispatcher + Job())
    private val service = FakeBackupService()
    private val events = mutableListOf<BackupEvent>()
    private val uri: Uri = Uri.parse("content://com.denariidolor.test/backup.ddbackup")
    private lateinit var viewModel: BackupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = BackupViewModel(service, Clock.fixed(Instant.parse("2026-10-04T18:30:00Z"), ZoneOffset.UTC))
        collector.launch { viewModel.events.collect { events += it } }
    }

    @After
    fun tearDown() {
        collector.cancel()
        Dispatchers.resetMain()
    }

    private val dialog get() = viewModel.state.value.dialog

    private fun message(resId: Int) = BackupEvent.Message(UiMessage.Resource(resId))

    @Test
    fun aWeakPassphraseIsRejectedBeforeAnythingIsRead() {
        viewModel.startBackup()
        viewModel.submitNewPassphrase("too short", "too short")

        assertEquals(BackupDialog.NewPassphrase(BackupError.PASSPHRASE_LENGTH), dialog)
        assertTrue(service.created.isEmpty())
    }

    @Test
    fun theBackupIsEncryptedBeforeTheDestinationIsChosenAndSavedAfter() {
        viewModel.startBackup()
        viewModel.submitNewPassphrase(PASSPHRASE, PASSPHRASE)

        assertEquals(listOf(PASSPHRASE), service.created)
        assertNull(dialog)
        assertEquals(BackupEvent.ChooseDestination("denarii-dolor-backup-2026-10-04.ddbackup"), events.single())

        viewModel.onDestinationChosen(uri)

        assertEquals(uri, service.saved.single().first)
        assertArrayEquals(FakeBackupService.FILE, service.saved.single().second)
        assertEquals(message(R.string.backup_saved), events.last())
    }

    @Test
    fun aRepeatedContinueEncryptsOnce() {
        viewModel.startBackup()
        viewModel.submitNewPassphrase(PASSPHRASE, PASSPHRASE)
        viewModel.submitNewPassphrase(PASSPHRASE, PASSPHRASE)

        assertEquals(1, service.created.size)
        assertEquals(1, events.count { it is BackupEvent.ChooseDestination })
    }

    @Test
    fun aCancelledPickerDropsTheEncryptedFile() {
        viewModel.startBackup()
        viewModel.submitNewPassphrase(PASSPHRASE, PASSPHRASE)
        viewModel.onDestinationChosen(null)
        // A later result (the screen recreated while a picker was open) has nothing to save, and says so.
        viewModel.onDestinationChosen(uri)

        assertTrue(service.saved.isEmpty())
        assertEquals(message(R.string.backup_save_failed), events.last())
    }

    @Test
    fun aBackupThatCannotBeCreatedIsReported() {
        service.createFailure = BackupException(BackupError.DAMAGED)
        viewModel.startBackup()
        viewModel.submitNewPassphrase(PASSPHRASE, PASSPHRASE)

        assertNull(dialog)
        assertEquals(listOf(message(R.string.backup_save_failed)), events)
    }

    @Test
    fun aWrongPassphraseOrAnUnreadableFileKeepsTheRestoreDialogOpen() {
        viewModel.onBackupFileChosen(uri)
        service.openResult = Result.failure(BackupException(BackupError.WRONG_PASSPHRASE))
        viewModel.submitRestorePassphrase("not the passphrase")
        assertEquals(BackupDialog.RestorePassphrase(uri, BackupError.WRONG_PASSPHRASE), dialog)

        service.openResult = Result.failure(IOException("provider gone"))
        viewModel.submitRestorePassphrase(PASSPHRASE)
        assertEquals(BackupDialog.RestorePassphrase(uri, BackupError.READ_FAILED), dialog)
        assertTrue(service.restored.isEmpty())
    }

    @Test
    fun aRestoreShowsWhatTheBackupHoldsAndReplacesOnlyOnceConfirmed() {
        viewModel.onBackupFileChosen(uri)
        viewModel.submitRestorePassphrase(PASSPHRASE)

        val preview = BackupPreview(Instant.ofEpochMilli(SNAPSHOT.createdAtEpochMillis).atZone(ZoneOffset.UTC), 0, 0, 0, 0)
        assertEquals(BackupDialog.ConfirmRestore(preview), dialog)
        assertTrue(service.restored.isEmpty())

        viewModel.confirmRestore()
        viewModel.confirmRestore()

        assertEquals(listOf(SNAPSHOT), service.restored)
        assertNull(dialog)
        assertEquals(BackupEvent.Restored, events.last())
    }

    @Test
    fun dismissingThePreviewForgetsTheBackup() {
        viewModel.onBackupFileChosen(uri)
        viewModel.submitRestorePassphrase(PASSPHRASE)
        viewModel.dismissDialog()
        viewModel.confirmRestore()

        assertNull(dialog)
        assertTrue(service.restored.isEmpty())
    }

    @Test
    fun aFailedRestoreSaysTheDataDidNotChange() {
        service.restoreFailure = IllegalStateException("The database is locked")
        viewModel.onBackupFileChosen(uri)
        viewModel.submitRestorePassphrase(PASSPHRASE)
        viewModel.confirmRestore()

        assertNull(dialog)
        assertEquals(message(R.string.backup_restore_failed), events.last())
    }

    private class FakeBackupService : BackupService {
        val created = mutableListOf<String>()
        val saved = mutableListOf<Pair<Uri, ByteArray>>()
        val restored = mutableListOf<BackupSnapshot>()
        var createFailure: Exception? = null
        var restoreFailure: Exception? = null
        var openResult: Result<BackupSnapshot> = Result.success(SNAPSHOT)

        override suspend fun create(passphrase: String): ByteArray {
            createFailure?.let { throw it }
            created += passphrase
            return FILE
        }

        override suspend fun save(uri: Uri, file: ByteArray) {
            saved += uri to file
        }

        override suspend fun open(uri: Uri, passphrase: String): BackupSnapshot = openResult.getOrThrow()

        override suspend fun restore(snapshot: BackupSnapshot) {
            restoreFailure?.let { throw it }
            restored += snapshot
        }

        companion object {
            val FILE = byteArrayOf(1, 2, 3)
        }
    }

    private companion object {
        const val PASSPHRASE = "correct horse battery staple"
        val SNAPSHOT = BackupSnapshot(1_791_150_000_000L, "USD", emptyList(), emptyList(), emptyList(), emptyList())
    }
}
