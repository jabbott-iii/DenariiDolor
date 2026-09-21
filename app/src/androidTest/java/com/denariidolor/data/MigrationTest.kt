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

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.MIGRATION_1_2
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate1To2ConvertsMoneyToCentsAndKeepsData() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO categories (id, name, iconName) VALUES (1, 'Dining', 'dining')")
            execSQL("INSERT INTO accounts (id, name, balance) VALUES (1, 'Cash', 12.34), (2, 'Card', -0.1)")
            execSQL("INSERT INTO budgets (id, categoryId, monthlyLimit, warningThresholdPercent) VALUES (1, 1, 250.5, 75)")
            execSQL(
                "INSERT INTO transactions (id, type, description, amount, categoryId, accountId, transferAccountId, " +
                    "dateEpochMillis, createdAtEpochMillis) VALUES " +
                    "(1, 'EXPENSE', 'Lunch', 19.99, 1, 1, NULL, 100, 200), " +
                    "(2, 'TRANSFER', 'Pay card', 0.29, 1, 1, 2, 300, 400)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2)

        db.query("SELECT id, balanceCents FROM accounts ORDER BY id").use { cursor ->
            cursor.moveToNext(); assertEquals(1_234L, cursor.getLong(1))
            cursor.moveToNext(); assertEquals(-10L, cursor.getLong(1))
        }
        db.query("SELECT monthlyLimitCents, warningThresholdPercent FROM budgets").use { cursor ->
            cursor.moveToNext()
            assertEquals(25_050L, cursor.getLong(0))
            assertEquals(75, cursor.getInt(1))
        }
        db.query("SELECT id, type, amountCents, transferAccountId, createdAtEpochMillis FROM transactions ORDER BY id").use { cursor ->
            cursor.moveToNext()
            assertEquals(1_999L, cursor.getLong(2))
            assertEquals(200L, cursor.getLong(4))
            cursor.moveToNext()
            assertEquals("TRANSFER", cursor.getString(1))
            assertEquals(29L, cursor.getLong(2))
            assertEquals(2L, cursor.getLong(3))
        }
        db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
    }
}
