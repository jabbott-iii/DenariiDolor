/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.backup

import android.content.Context
import android.net.Uri
import com.denariidolor.data.local.db.DatabaseHolder
import com.denariidolor.data.local.preferences.CurrencyPreferences
import com.denariidolor.util.Constants
import com.denariidolor.util.Currencies
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Encrypted backup and restore through files the user picks with the system file picker (CS-28). The app needs no storage
 * permission and no network; where a file goes is the user's choice. Everything here needs an unlocked vault.
 */
interface BackupService {
    /** Every table and the currency setting, encrypted under [passphrase]: the bytes of a backup file, ready to be saved. */
    suspend fun create(passphrase: String): ByteArray

    /** Writes a file made by [create] to [uri]. */
    suspend fun save(uri: Uri, file: ByteArray)

    /** Decrypts and checks the backup at [uri] without changing anything. Throws [BackupException] for a file it can't use. */
    suspend fun open(uri: Uri, passphrase: String): BackupSnapshot

    /** Replaces all data with [snapshot] in one database transaction, then applies its currency setting. */
    suspend fun restore(snapshot: BackupSnapshot)
}

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext context: Context,
    private val databaseHolder: DatabaseHolder,
    private val currencyPreferences: CurrencyPreferences,
    private val clock: Clock
) : BackupService {
    private val contentResolver = context.applicationContext.contentResolver

    // Checked before anything is saved, so a backup that restore would refuse is never written.
    override suspend fun create(passphrase: String): ByteArray = withContext(Dispatchers.IO) {
        val snapshot = BackupStore(databaseHolder.database).read(clock.millis(), currencyPreferences.currency.value.currencyCode)
        if (!snapshot.isConsistent(DEFAULT_ACCOUNT_IDS, DEFAULT_CATEGORY_IDS)) throw BackupException(BackupError.DAMAGED)
        BackupFile.write(snapshot, passphrase).also { check(it.size <= BackupFile.MAX_FILE_BYTES) { "Too large to restore" } }
    }

    override suspend fun save(uri: Uri, file: ByteArray) {
        withContext(Dispatchers.IO) {
            val output = contentResolver.openOutputStream(uri, "wt") ?: error("Unable to open the backup destination")
            output.use { it.write(file) }
        }
    }

    override suspend fun open(uri: Uri, passphrase: String): BackupSnapshot = withContext(Dispatchers.IO) {
        val snapshot = BackupFile.read(readFile(uri), passphrase)
        if (!snapshot.isConsistent(DEFAULT_ACCOUNT_IDS, DEFAULT_CATEGORY_IDS)) throw BackupException(BackupError.DAMAGED)
        snapshot
    }

    override suspend fun restore(snapshot: BackupSnapshot) {
        withContext(Dispatchers.IO) {
            BackupStore(databaseHolder.database).replaceAll(snapshot)
            Currencies.fromCode(snapshot.currencyCode)?.let(currencyPreferences::setCurrency)
        }
    }

    // A provider error or a revoked grant is READ_FAILED; another kind of file, or one too large to be a backup, NOT_A_BACKUP.
    private fun readFile(uri: Uri): ByteArray = try {
        contentResolver.openInputStream(uri)?.use(BackupFile::readFrom) ?: readFailed()
    } catch (e: IOException) {
        readFailed(e)
    } catch (e: SecurityException) {
        readFailed(e)
    }

    private fun readFailed(cause: Throwable? = null): Nothing = throw BackupException(BackupError.READ_FAILED, cause)

    private companion object {
        val DEFAULT_ACCOUNT_IDS = Constants.DEFAULT_ACCOUNTS.mapTo(HashSet()) { it.id }
        val DEFAULT_CATEGORY_IDS = Constants.DEFAULT_CATEGORIES.mapTo(HashSet()) { it.id }
    }
}
