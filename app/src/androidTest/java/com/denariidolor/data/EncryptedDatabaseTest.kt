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

package com.denariidolor.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.security.DatabaseKeys
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "encrypted_test.db"

    @Before
    fun setUp() {
        System.loadLibrary("sqlcipher")
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun open(hexKey: String) = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .openHelperFactory(SupportOpenHelperFactory(DatabaseKeys.toRawKeyPassphrase(hexKey)))
        .build()

    @Test
    fun databaseFileIsEncryptedAndReadableWithSameKey() = runBlocking {
        val key = DatabaseKeys.generateHexKey(SecureRandom())
        open(key).apply {
            accountDao().insert(AccountEntity(name = "Cash", balanceCents = 1_250))
            close()
        }

        assertFalse(DatabaseKeys.isPlaintextDatabase(context.getDatabasePath(dbName)))
        open(key).apply {
            assertEquals(1, accountDao().count())
            close()
        }
    }

    @Test
    fun wrongKeyCannotReadDatabase() = runBlocking {
        open(DatabaseKeys.generateHexKey(SecureRandom())).apply {
            accountDao().insert(AccountEntity(name = "Cash", balanceCents = 0))
            close()
        }

        val wrong = open(DatabaseKeys.generateHexKey(SecureRandom()))
        val result = runCatching { wrong.accountDao().count() }
        wrong.close()

        assertTrue(result.isFailure)
    }
}
