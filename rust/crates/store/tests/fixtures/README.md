# Store fixtures

Files that v1, the Kotlin app at commit `6d57526`, wrote on an Android emulator. The tests open
copies of them, so the files here never change. Never edit or regenerate one in place; a new
fixture gets a new name.

## `v1-sqlcipher-android-4.19.1.db`

A database written by v1's own storage code: Room 2.8.5 over `sqlcipher-android` 4.19.1, opened
with v1's raw-key passphrase (`x'<64 lowercase hex digits>'`) and v1's migration, as
`DatabaseHolder.open` opens one. It holds v1's schema version 2, with two accounts and nothing
else. M2.1 made it, to check that the SQLCipher build in `rusqlite` opens a file that v1's library
wrote (ADR-0017).

| What | Value |
| :- | :- |
| Key | The test bytes `0x00` to `0x1f`, in order. This is a test value, not a key from any install |
| Rows | `accounts`: `(1, 'Cash', 1250)` and `(2, 'Épargne 💰', -4200)`, the second name with a non-ASCII letter and an emoji |
| Schema version | `PRAGMA user_version` is 2 |
| SQLCipher | `PRAGMA cipher_version` reported `4.19.0 community`: `sqlcipher-android` 4.19.1 ships SQLCipher 4.19.0 |
| Journal mode | `wal`, Room's default. Closing the database folded the write-ahead log into the file, so no `-wal` or `-shm` file belongs with it |
| Size | 53,248 bytes (13 pages of 4,096 bytes) |
| SHA-256 | `b55995c383de478bd23629fcdce461651c6db2e88460b21d3d5582b0618f5128` |
| Made | 2026-11-23, on `Denarii_API_36` (`google/sdk_gphone64_x86_64/emu64xa:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys`) |

### How it was made

The writer was a throwaway instrumented test in a scratch copy of v1, so nothing was added to v1's
tree or to the app. To make the file again:

1. Copy v1 out of Git, and point it at the Android SDK:

   ```bash
   mkdir v1 && git archive 6d57526 | tar -x -C v1
   echo "sdk.dir=$HOME/Android/Sdk" > v1/local.properties
   ```

2. Save the test below as
   `v1/app/src/androidTest/java/com/denariidolor/data/StoreFixtureWriterTest.kt`.
3. Build v1's debug app and its test app. With the Gradle cache from an earlier v1 build, this
   works offline:

   ```bash
   cd v1 && ./gradlew --offline :app:assembleDebug :app:assembleDebugAndroidTest
   ```

4. On an emulator with no `com.denariidolor` app installed (uninstalling one deletes its data),
   install both apps and run the test:

   ```bash
   adb install -t app/build/outputs/apk/debug/app-debug.apk
   adb install -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
   adb shell am instrument -w -e class com.denariidolor.data.StoreFixtureWriterTest \
     com.denariidolor.test/androidx.test.runner.AndroidJUnitRunner
   ```

   The test prints the cipher version, the journal mode, and the schema version to logcat, after
   `FIXTURE`.
5. Copy the file out, then uninstall both apps:

   ```bash
   adb exec-out run-as com.denariidolor cat databases/v1-sqlcipher-android-4.19.1.db > v1-sqlcipher-android-4.19.1.db
   adb uninstall com.denariidolor.test
   adb uninstall com.denariidolor
   ```

SQLCipher picks a random salt for each new file, so a file made again has other bytes and another
SHA-256, but the same rows and settings.

```kotlin
package com.denariidolor.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.MIGRATION_1_2
import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.security.DatabaseKeys
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Writes the M2.1 fixture for `rust/crates/store`: a database opened the way `DatabaseHolder.open` opens one (Room 2.8.5
 * over sqlcipher-android 4.19.1, v1's raw-key passphrase, and its migration), with two accounts. The key is a test value.
 * The test leaves the file in the app's `databases/` folder, for `adb exec-out run-as` to copy.
 */
@RunWith(AndroidJUnit4::class)
class StoreFixtureWriterTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun writeFixture() = runBlocking<Unit> {
        System.loadLibrary("sqlcipher")
        context.deleteDatabase(DB_NAME)
        val passphrase = DatabaseKeys.toRawKeyPassphrase(DatabaseKeys.toHex(FIXTURE_KEY))
        val database = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .addMigrations(MIGRATION_1_2)
            .build()
        val db = database.openHelper.writableDatabase
        database.accountDao().insert(AccountEntity(name = "Cash", balanceCents = 1_250))
        database.accountDao().insert(AccountEntity(name = "Épargne 💰", balanceCents = -4_200))
        db.query("PRAGMA cipher_version").use { it.moveToFirst(); println("FIXTURE cipher_version=${it.getString(0)}") }
        db.query("PRAGMA journal_mode").use { it.moveToFirst(); println("FIXTURE journal_mode=${it.getString(0)}") }
        db.query("PRAGMA user_version").use { it.moveToFirst(); println("FIXTURE user_version=${it.getInt(0)}") }
        database.close()
        passphrase.fill(0)
    }

    private companion object {
        const val DB_NAME = "v1-sqlcipher-android-4.19.1.db"

        // Test value: the bytes 0x00 to 0x1f.
        val FIXTURE_KEY = ByteArray(32) { it.toByte() }
    }
}
