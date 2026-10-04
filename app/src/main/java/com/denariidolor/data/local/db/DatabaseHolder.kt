/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data.local.db

import android.content.Context
import androidx.room.Room
import com.denariidolor.data.local.db.security.DatabaseKeys
import com.denariidolor.data.local.vault.VaultConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Owns the SQLCipher [AppDatabase] while the vault is unlocked (CS-15). It opens only with a key unwrapped at sign-in
 * and closes on sign-out or timeout. Closing zeroes the passphrase, which SQLCipher holds by reference, so a stray
 * query fails instead of silently reopening the database.
 */
@Singleton
class DatabaseHolder @Inject constructor(@ApplicationContext context: Context, private val config: VaultConfig) {
    private val appContext = context.applicationContext

    // One thread opens, closes and deletes, so those steps can never overlap.
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "database-holder") }

    @Volatile
    private var current: OpenDatabase? = null

    private class OpenDatabase(val database: AppDatabase, val passphrase: ByteArray) {
        fun close() {
            database.close()
            passphrase.fill(0)
        }
    }

    val isOpen: Boolean get() = current != null

    /** The unlocked database. Throws while the vault is locked, so nothing can read data before sign-in. */
    val database: AppDatabase get() = checkNotNull(current) { "The database is locked" }.database

    /** Opens the database with [databaseKey], creating or migrating it as needed. Blocks; call it off the main thread. */
    fun open(databaseKey: ByteArray) = onWorker {
        System.loadLibrary("sqlcipher")
        val passphrase = DatabaseKeys.toRawKeyPassphrase(DatabaseKeys.toHex(databaseKey))
        val database = Room.databaseBuilder(appContext, AppDatabase::class.java, config.databaseName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .addMigrations(MIGRATION_1_2)
            .build()
        val opened = OpenDatabase(database, passphrase)
        var ready = false
        try {
            database.openHelper.writableDatabase
            ready = true
        } finally {
            if (!ready) opened.close()
        }
        swap(opened)?.close()
    }

    /** Locks at once; the close itself runs on the worker so it never blocks the caller. */
    fun close() {
        val closing = swap(null) ?: return
        worker.execute(closing::close)
    }

    /** Closes the database and deletes its files: on wipe, and for an orphan that no profile can decrypt any more. */
    fun deleteDatabaseFiles(): Boolean {
        close()
        return onWorker {
            appContext.deleteDatabase(config.databaseName)
            !appContext.getDatabasePath(config.databaseName).exists()
        }
    }

    /** True for a database left unencrypted by a build from before SQLCipher. */
    fun isPlaintext(): Boolean = DatabaseKeys.isPlaintextDatabase(appContext.getDatabasePath(config.databaseName))

    @Synchronized
    private fun swap(next: OpenDatabase?): OpenDatabase? = current.also { current = next }

    private fun <T> onWorker(task: () -> T): T = try {
        worker.submit(Callable(task)).get()
    } catch (e: ExecutionException) {
        throw e.cause ?: e
    }
}
