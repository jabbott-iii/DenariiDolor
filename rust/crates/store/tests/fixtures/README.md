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

## `v1-f3-ledger.db`: fixture F3

Fixture F3 of `docs/specs/android-v1-formats.md`: a version 2 database that v1's own code filled
with every transaction type. `DatabaseHolder.open` created it, `DefaultDataInitializer` seeded
the defaults, and v1's use cases and repositories wrote everything else, so each stored balance
is what v1's balance updates left. The one exception is the malformed transfer, which no use case
writes: it went through `TransactionDao.insert`, as v1's own
`TransactionRepositoryImplTest.malformedTransferCanBeDeletedWithoutTouchingBalances` writes one,
with no balance change (BUG-06). M2.3 made it.

| What | Value |
| :- | :- |
| Key | The test bytes `0x00` to `0x1f`, as for the file above |
| Schema version | `PRAGMA user_version` is 2, with version 2's identity hash |
| SQLCipher | `PRAGMA cipher_version` reported `4.19.0 community`, and `cipher_page_size` 4,096 |
| Journal mode | `wal`. Closing folded the log into the file, so no `-wal` or `-shm` file belongs with it |
| Size | 53,248 bytes (13 pages of 4,096 bytes) |
| SHA-256 | `e207ed786cef6ebe10d791d87bbaa70847dfa4e465374a3065b815df4e0cc77a` |
| Made | 2026-11-23, on `Denarii_API_36`, with the same build fingerprint as the file above and the phone's language set to `en-US` |

Accounts, with the opening balance each was added with, which v1 stores nowhere but in the
balance itself:

| ID | Name | Opening balance (cents) | Stored balance (cents) |
| :- | :- | -: | -: |
| 1 | Cash (a default) | 0 | -2,300 |
| 2 | Savings (a default) | 0 | 50,000 |
| 3 | Carte bancaire 💳 | 125,000 | 269,655 |
| 4 | Carte de crédit | -30,000 | -44,000 |

Account 5, "Compte fermé", was added and deleted, so `sqlite_sequence` holds 5 for `accounts`.

Categories 1–3 are the defaults in English: General Expense (`ic_category_default`), General
Income (`income`), and Transfer (`transfer`). The use cases added 4, "Épicerie 🛒"
(`groceries`); 5, "Restaurants" (`dining`); and 6, "Salaire" (`income`). Two budgets: 40,000 cents
a month for category 4 with a 75 percent warning, and 15,000 cents for category 5 with the default
80 percent.

Every date is midnight UTC. The budget check ran on a clock fixed at 2026-11-23T12:00:00Z in UTC.
`createdAtEpochMillis` is the emulator's clock when each row was added.

| ID | Type | Cents | Accounts | Category | Date | What it covers |
| -: | :- | -: | :- | -: | :- | :- |
| 1 | `INCOME` | 250,000 | into 3 | 6 | 2026-11-01 | "Salaire de novembre" |
| 2 | `EXPENSE` | 35,000 | from 3 | 4 | 2026-10-28 | "Épicerie d'octobre", in the month before the others, so November's budget doesn't count it |
| 3 | `EXPENSE` | 450 | from 1 | 5 | 2026-11-03 | Saved as "Café ☕" with two spaces before and after it, and stored trimmed (BUG-08) |
| 4 | `EXPENSE` | 12,345 | from 3 | 4 | 2026-11-05 | "Marché 🥖" |
| 5 | `EXPENSE` | 30,000 | from 4 | 4 | 2026-11-12 | "Grande course". It takes November's groceries to 42,345 cents, past the 40,000-cent budget: the first attempt failed with `BUDGET_EXCEEDED`, and the second passed `allowOverBudget` |
| 6 | `TRANSFER` | 50,000 | 3 to 2 | 3 | 2026-11-02 | "Épargne mensuelle" |
| 7 | `TRANSFER` | 20,000 | 3 to 4 | 3 | 2026-11-15 | "Rembourser la carte" |
| 8 | `EXPENSE` | 1,850 | from 1 | 1 | 2026-11-18 | "午餐 🍜" |
| 9 | `INCOME` | 8,000 | into 4 | 2 | 2026-11-19 | Added as a 7,500-cent expense from account 1 in category 1, "Cadeau", then edited: a new type, amount, category, account, and the Arabic description "هدية" |
| 10 | `TRANSFER` | 12,000 | 4 to 3 | 3 | 2026-11-20 | "Mouvement", added as 10,000 cents from 1 to 2, then edited to move between two other accounts |
| 11 | | | | | 2026-11-21 | Added as a 999-cent expense from account 2, then deleted |
| 12 | `TRANSFER` | 4,200 | from 1, to none | 3 | 2026-11-22 | "Virement sans destination": `transferAccountId` is `NULL` (BUG-06) |

## `v1-f4-version-1.db` and `v1-f4-migrated-by-v1.db`: fixture F4

Fixture F4 of the format spec: a version 1 database, with money in `REAL` dollars that round in
different directions, and the same file after v1 migrated it. Room's `MigrationTestHelper` made
the version 1 file from v1's exported version 1 schema, as v1's `MigrationTest` does, and SQL
statements in the writer added the rows. The writer closed it and copied it to
`v1-f4-version-1.db`. Then `DatabaseHolder.open` opened the original, which ran v1's
`MIGRATION_1_2` as the app did on a phone, and the writer closed it and copied it to
`v1-f4-migrated-by-v1.db`. M2.3 made both.

| What | `v1-f4-version-1.db` | `v1-f4-migrated-by-v1.db` |
| :- | :- | :- |
| Key | The test bytes `0x00` to `0x1f` | The same |
| Schema version | 1, with version 1's identity hash `15f1c4acb81d97b124713b69e7ec869d` | 2, with version 2's identity hash |
| Journal mode | `wal`: the writer gave the helper SQLCipher's factory with write-ahead logging on, as Room put every file it opened on a phone. `MigrationTest` itself uses SQLite's default | `wal` |
| SQLCipher | `4.19.0 community`, page size 4,096 | The same |
| Size | 53,248 bytes (13 pages) | 57,344 bytes (14 pages) |
| SHA-256 | `c22a0bd12e365522710a1c00d1e7ae8a52a893353d08a311517eb3711587fd2c` | `868ae74bca433f70cd89cac868a2b94e4486fa67205950732792fddba9158c05` |
| Made | 2026-11-23, on `Denarii_API_36` | The same |

The rows at version 1, and the cents v1's migration turned them into:

| Table | ID | Dollars at version 1 | Cents after v1's migration |
| :- | -: | -: | -: |
| `accounts` | 1, "Cash" | 0.005 | 1 |
| `accounts` | 2, "Savings" | 1.005 | 100 |
| `accounts` | 3, "Carte 💳" | 19.995 | 2,000 |
| `accounts` | 4, "Crédit" | -12.345 | -1,235 |
| `budgets` | 1, category 4 at 75 percent | 250.5 | 25,050 |
| `budgets` | 2, category 1 at 80 percent | 99.995 | 10,000 |
| `transactions` | 1, `EXPENSE` "Lunch 🍜" | 19.99 | 1,999 |
| `transactions` | 2, `TRANSFER` "Pay card", 1 to 2 | 0.29 | 29 |
| `transactions` | 3, `INCOME` "Salaire" | 2500.0 | 250,000 |
| `transactions` | 4, `EXPENSE` "Arrondi" | 2.675 | 268 |
| `transactions` | 5, `TRANSFER` "Virement sans destination", with no destination (BUG-06) | 1.005 | 100 |
| `transactions` | 6, `EXPENSE` "Gros montant" | 12345678.915 | 1,234,567,892 |

Categories 1–4 are "General Expense", "General Income", "Transfer", and "Épicerie 🛒". Account
5, "Compte fermé", was added and deleted at version 1, so version 1's counter for `accounts` is
5, and the migration reset it to 4, the highest ID it copied. The account balances don't add up
from the transactions; F4 checks the migration only.

After the writer ran, an empty `v1-f4-work.db.lck` was left beside the file it migrated, and
`Context.deleteDatabase` didn't remove it. F3's file, which only `DatabaseHolder.open` created,
had none. So a phone whose database Room migrated may hold an empty `denarii_dolor.db.lck` too;
no phone has been checked.

## How F3 and F4 were made

The writer was a throwaway instrumented test in a scratch copy of v1, made as steps 1–5 of the
file above describe, with these changes:

- Save the test below as
  `v1/app/src/androidTest/java/com/denariidolor/data/LedgerFixtureWriterTest.kt` in step 2.
- In step 4, run it with `-e class com.denariidolor.data.LedgerFixtureWriterTest`. It prints
  each file's settings and balances to logcat after `FIXTURE`.
- In step 5, copy `databases/v1-f3-ledger.db`, `databases/v1-f4-version-1.db`, and
  `databases/v1-f4-migrated-by-v1.db`.

M2.3 started the emulator with `emulator -avd Denarii_API_36 -no-window -read-only -no-snapshot`,
so it kept none of the changes. Both apps were uninstalled afterward all the same. SQLCipher
picks a random salt for each new file, so files made again have other bytes and other SHA-256
values, but the same rows and settings, apart from F3's `createdAtEpochMillis`.

```kotlin
package com.denariidolor.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.data.local.db.AppDatabase
import com.denariidolor.data.local.db.DatabaseHolder
import com.denariidolor.data.local.db.DefaultDataInitializer
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.data.local.db.security.DatabaseKeys
import com.denariidolor.data.local.vault.VaultConfig
import com.denariidolor.data.repository.AccountRepositoryImpl
import com.denariidolor.data.repository.BudgetRepositoryImpl
import com.denariidolor.data.repository.CategoryRepositoryImpl
import com.denariidolor.data.repository.TransactionRepositoryImpl
import com.denariidolor.domain.model.Expense
import com.denariidolor.domain.model.Income
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.model.Transfer
import com.denariidolor.domain.usecase.AccountUseCases
import com.denariidolor.domain.usecase.AddTransactionUseCase
import com.denariidolor.domain.usecase.BudgetUseCases
import com.denariidolor.domain.usecase.CategoryUseCases
import com.denariidolor.domain.usecase.DeleteTransactionUseCase
import com.denariidolor.domain.usecase.DomainError
import com.denariidolor.domain.usecase.DomainException
import com.denariidolor.domain.usecase.UpdateTransactionUseCase
import com.denariidolor.domain.usecase.ValidateTransactionUseCase
import com.denariidolor.util.asZonedClock
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Writes fixtures F3 and F4 of `docs/specs/android-v1-formats.md` for `rust/crates/store` (M2.3). F3 is created, and F4's
 * version 1 file migrated, by `DatabaseHolder.open` (Room 2.8.5 over sqlcipher-android 4.19.1, v1's raw-key passphrase, and
 * `MIGRATION_1_2`), and F3's rows go through v1's own seeding, use cases, and repositories. The key is a test value. The test
 * leaves the files in the app's `databases/` folder, for `adb exec-out run-as` to copy.
 */
@RunWith(AndroidJUnit4::class)
class LedgerFixtureWriterTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        // Write-ahead logging, as Room put every file it opened on a phone.
        SupportOpenHelperFactory(DatabaseKeys.toRawKeyPassphrase(DatabaseKeys.toHex(FIXTURE_KEY)), null, true)
    )

    /** F3: a version 2 database with every transaction type, edits, a delete, a budget, and one malformed transfer. */
    @Test
    fun writeF3() = runBlocking<Unit> {
        context.deleteDatabase(F3)
        val holder = DatabaseHolder(context, VaultConfig(databaseName = F3))
        holder.open(FIXTURE_KEY.copyOf())
        DefaultDataInitializer(context, holder).seedDefaults()
        val database = holder.database
        val accountRepository = AccountRepositoryImpl(database.accountDao())
        val categoryRepository = CategoryRepositoryImpl(database.categoryDao())
        val budgetRepository = BudgetRepositoryImpl(database.budgetDao())
        val transactionRepository = TransactionRepositoryImpl(database, database.transactionDao(), database.accountDao())
        val clock = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC).asZonedClock()
        val validate = ValidateTransactionUseCase(categoryRepository, accountRepository, budgetRepository, transactionRepository, clock)
        val accounts = AccountUseCases(accountRepository, transactionRepository)
        val categories = CategoryUseCases(categoryRepository, transactionRepository)
        val budgets = BudgetUseCases(budgetRepository, categoryRepository)
        val add = AddTransactionUseCase(validate, transactionRepository)
        val update = UpdateTransactionUseCase(validate, transactionRepository)
        val delete = DeleteTransactionUseCase(transactionRepository)

        val card = accounts.add("Carte bancaire 💳", 125_000).getOrThrow()
        val credit = accounts.add("Carte de crédit", -30_000).getOrThrow()
        val closed = accounts.add("Compte fermé", 0).getOrThrow()
        accounts.delete(closed).getOrThrow()
        assertEquals(listOf(3L, 4L, 5L), listOf(card, credit, closed))

        val groceries = categories.add("Épicerie 🛒", "groceries").getOrThrow()
        val dining = categories.add("Restaurants", "dining").getOrThrow()
        val salary = categories.add("Salaire", "income").getOrThrow()
        assertEquals(listOf(4L, 5L, 6L), listOf(groceries, dining, salary))
        budgets.set(groceries, 40_000, 75).getOrThrow()
        budgets.set(dining, 15_000).getOrThrow()

        val ids = mutableListOf<Long>()
        ids += add(Income(description = "Salaire de novembre", amountCents = 250_000, categoryId = salary, accountId = card, dateEpochMillis = day("2026-11-01"))).getOrThrow()
        ids += add(Expense(description = "Épicerie d'octobre", amountCents = 35_000, categoryId = groceries, accountId = card, dateEpochMillis = day("2026-10-28"))).getOrThrow()
        // Stored trimmed, as `Transaction.toEntity` trims every description (BUG-08).
        ids += add(Expense(description = "  Café ☕  ", amountCents = 450, categoryId = dining, accountId = CASH, dateEpochMillis = day("2026-11-03"))).getOrThrow()
        ids += add(Expense(description = "Marché 🥖", amountCents = 12_345, categoryId = groceries, accountId = card, dateEpochMillis = day("2026-11-05"))).getOrThrow()
        // 12,345 + 30,000 cents spent in November goes past the 40,000-cent budget, so the user has to confirm it.
        val overBudget = Expense(description = "Grande course", amountCents = 30_000, categoryId = groceries, accountId = credit, dateEpochMillis = day("2026-11-12"))
        val refused = add(overBudget).exceptionOrNull()
        assertEquals(DomainError.BUDGET_EXCEEDED, (refused as DomainException).error)
        ids += add(overBudget, allowOverBudget = true).getOrThrow()
        ids += add(Transfer(description = "Épargne mensuelle", amountCents = 50_000, categoryId = TRANSFER, accountId = card, transferAccountId = SAVINGS, dateEpochMillis = day("2026-11-02"))).getOrThrow()
        ids += add(Transfer(description = "Rembourser la carte", amountCents = 20_000, categoryId = TRANSFER, accountId = card, transferAccountId = credit, dateEpochMillis = day("2026-11-15"))).getOrThrow()
        ids += add(Expense(description = "午餐 🍜", amountCents = 1_850, categoryId = EXPENSE, accountId = CASH, dateEpochMillis = day("2026-11-18"))).getOrThrow()

        // An expense edited into an income: a new type, amount, category, account, and description.
        val gift = add(Expense(description = "Cadeau", amountCents = 7_500, categoryId = EXPENSE, accountId = CASH, dateEpochMillis = day("2026-11-19"))).getOrThrow()
        update(Income(id = gift, description = "هدية", amountCents = 8_000, categoryId = INCOME, accountId = credit, dateEpochMillis = day("2026-11-19"))).getOrThrow()
        ids += gift
        // A transfer edited to move between two other accounts.
        val moved = add(Transfer(description = "Mouvement", amountCents = 10_000, categoryId = TRANSFER, accountId = CASH, transferAccountId = SAVINGS, dateEpochMillis = day("2026-11-20"))).getOrThrow()
        update(Transfer(id = moved, description = "Mouvement", amountCents = 12_000, categoryId = TRANSFER, accountId = credit, transferAccountId = card, dateEpochMillis = day("2026-11-20"))).getOrThrow()
        ids += moved
        // A transaction added, then deleted.
        val removed = add(Expense(description = "Supprimée", amountCents = 999, categoryId = EXPENSE, accountId = SAVINGS, dateEpochMillis = day("2026-11-21"))).getOrThrow()
        delete(removed).getOrThrow()
        // A TRANSFER row with no destination. No use case writes one, so it goes through the DAO, as
        // TransactionRepositoryImplTest.malformedTransferCanBeDeletedWithoutTouchingBalances does, with no balance change (BUG-06).
        val malformed = database.transactionDao().insert(
            TransactionEntity(
                type = TransactionType.TRANSFER,
                description = "Virement sans destination",
                amountCents = 4_200,
                categoryId = TRANSFER,
                accountId = CASH,
                transferAccountId = null,
                dateEpochMillis = day("2026-11-22")
            )
        )
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L), ids)
        assertEquals(listOf(11L, 12L), listOf(removed, malformed))

        val expected = mapOf(CASH to -2_300L, SAVINGS to 50_000L, card to 269_655L, credit to -44_000L)
        expected.forEach { (id, balance) -> assertEquals("account $id", balance, database.accountDao().getById(id)!!.balanceCents) }

        report(database.openHelper.writableDatabase, F3)
        database.close()
        holder.close()
        assertFalse(File(context.getDatabasePath(F3).path + "-wal").exists())
    }

    /**
     * F4: a version 1 database with `REAL` dollars that round, made by Room's migration test helper as `MigrationTest` makes one,
     * and a copy of it migrated by `DatabaseHolder.open`, as the app migrated one on a phone.
     */
    @Test
    fun writeF4() {
        System.loadLibrary("sqlcipher")
        listOf(F4_WORK, F4_VERSION_1, F4_MIGRATED).forEach(context::deleteDatabase)
        helper.createDatabase(F4_WORK, 1).apply {
            execSQL(
                "INSERT INTO categories (id, name, iconName) VALUES " +
                    "(1, 'General Expense', 'ic_category_default'), (2, 'General Income', 'income'), " +
                    "(3, 'Transfer', 'transfer'), (4, 'Épicerie 🛒', 'groceries')"
            )
            execSQL(
                "INSERT INTO accounts (id, name, balance) VALUES " +
                    "(1, 'Cash', 0.005), (2, 'Savings', 1.005), (3, 'Carte 💳', 19.995), (4, 'Crédit', -12.345), " +
                    "(5, 'Compte fermé', 0.0)"
            )
            // A deleted account leaves version 1's counter at 5; the migration resets it to 4.
            execSQL("DELETE FROM accounts WHERE id = 5")
            execSQL(
                "INSERT INTO budgets (id, categoryId, monthlyLimit, warningThresholdPercent) VALUES " +
                    "(1, 4, 250.5, 75), (2, 1, 99.995, 80)"
            )
            execSQL(
                "INSERT INTO transactions (id, type, description, amount, categoryId, accountId, transferAccountId, " +
                    "dateEpochMillis, createdAtEpochMillis) VALUES " +
                    "(1, 'EXPENSE', 'Lunch 🍜', 19.99, 4, 1, NULL, 1793491200000, 1793491200500), " +
                    "(2, 'TRANSFER', 'Pay card', 0.29, 3, 1, 2, 1793577600000, 1793577600500), " +
                    "(3, 'INCOME', 'Salaire', 2500.0, 2, 3, NULL, 1793664000000, 1793664000500), " +
                    "(4, 'EXPENSE', 'Arrondi', 2.675, 1, 4, NULL, 1793750400000, 1793750400500), " +
                    "(5, 'TRANSFER', 'Virement sans destination', 1.005, 3, 1, NULL, 1793836800000, 1793836800500), " +
                    "(6, 'EXPENSE', 'Gros montant', 12345678.915, 1, 3, NULL, 1793923200000, 1793923200500)"
            )
            report(this, "$F4_WORK at version 1", balanceColumn = "balance")
            close()
        }
        val work = context.getDatabasePath(F4_WORK)
        assertFalse(File(work.path + "-wal").exists())
        work.copyTo(context.getDatabasePath(F4_VERSION_1))

        val holder = DatabaseHolder(context, VaultConfig(databaseName = F4_WORK))
        holder.open(FIXTURE_KEY.copyOf())
        val database = holder.database
        report(database.openHelper.writableDatabase, "$F4_WORK migrated")
        database.close()
        holder.close()
        assertFalse(File(work.path + "-wal").exists())
        work.copyTo(context.getDatabasePath(F4_MIGRATED))
        context.deleteDatabase(F4_WORK)
    }

    private fun report(db: SupportSQLiteDatabase, name: String, balanceColumn: String = "balanceCents") {
        for (pragma in listOf("cipher_version", "journal_mode", "user_version", "cipher_page_size")) {
            db.query("PRAGMA $pragma").use { it.moveToFirst(); println("FIXTURE $name $pragma=${it.getString(0)}") }
        }
        db.query("SELECT id, name, $balanceColumn FROM accounts ORDER BY id").use { cursor ->
            while (cursor.moveToNext()) println("FIXTURE $name account ${cursor.getLong(0)} ${cursor.getString(1)} ${cursor.getString(2)}")
        }
    }

    private fun day(date: String): Long = Instant.parse("${date}T00:00:00Z").toEpochMilli()

    private companion object {
        const val F3 = "v1-f3-ledger.db"
        const val F4_WORK = "v1-f4-work.db"
        const val F4_VERSION_1 = "v1-f4-version-1.db"
        const val F4_MIGRATED = "v1-f4-migrated-by-v1.db"

        const val CASH = 1L
        const val SAVINGS = 2L
        const val EXPENSE = 1L
        const val INCOME = 2L
        const val TRANSFER = 3L

        // Test value: the bytes 0x00 to 0x1f.
        val FIXTURE_KEY = ByteArray(32) { it.toByte() }
    }
}
```
