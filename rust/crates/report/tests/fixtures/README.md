# Report fixtures

Files that v1, the Kotlin app at commit `6d57526`, wrote with its own code. The tests in `../f8.rs`
read them, and they never change. Never edit or regenerate one in place; a new fixture gets a new
name.

## Fixture F8

Fixture F8 of `docs/specs/android-v1-formats.md`: CSV exports of one month with transfers,
formula-like text, and a right-to-left language, and the cells that v1's formula guard (CS-20)
escaped, on a desktop Java and on Android. M2.7 made it on 2026-11-24.

| File | What it holds | Size | SHA-256 |
| :- | :- | -: | :- |
| `v1-f8-report-ar.csv` | The month's CSV export with the language set to Arabic, as `ReportExporter` wrote it: UTF-8 with no byte order mark, and CRLF line endings | 945 bytes | `855248e2aa7a72ea141c6831919091f47f976dc61b2e53fea3155d848b88b6d5` |
| `v1-f8-report-en.csv` | The same month in US English | 925 bytes | `34b420ef52e434944cd5dcf022656575c2ca21f0763b82b2689b0251e933be95` |
| `v1-f8-day-starts.txt` | The first moment of each day of October 2026 in `Asia/Beirut`, then of November 1, in milliseconds since 1970, one to a line | 448 bytes | `238e3de243a21fdb1b84f58acbc6bbfa3294a47aa0a0744c3d10245aebb82f53` |
| `v1-f8-escape.tsv` | 2,121 cells and what `ReportCsvFormatter.escape` made of each on a desktop Java, one to a line: the cell's UTF-8 bytes in hexadecimal, a tab, and the result's | 63,852 bytes | `f5fe3b1f5295b97945cc91c9e786c8f7578948faf15aadf58e6a25ea136d56ea` |
| `v1-f8-escape-android.tsv` | The same cells, escaped on Android. API 26 and API 36 wrote the same bytes | 63,862 bytes | `54417f5f4af18c10c36f2bbe16cbae5186f718fc75553a9e97d12c6243242deb` |
| `v1-f8-inner-space-android.txt` | Each character `c` of the Basic Multilingual Plane, but the surrogates, for which `escape("a;" + c + "=1")` put an apostrophe after `c` on Android, as four hexadecimal digits. API 26 and API 36 wrote the same bytes | 130 bytes | `d6ac3baefa94251a5e04aa0c9082daf1b08e35f14a2ceba09a6822c5e3fe7e38` |

The CSV files must keep their bytes, line endings included, so `.gitattributes` at the repository
root marks this folder `-text`, and Git never converts them.

### The month

October 2026 in `Asia/Beirut` runs from 2026-09-30T21:00:00Z, midnight Eastern European Summer
Time (EEST), to 2026-10-31T22:00:00Z, midnight Eastern European Time (EET). The clocks go back
from 00:00 EEST on October 25 to 23:00 EET on October 24, so October 24 lasts 25 hours, and 23:30
happens twice. The report's clock was fixed at 2026-11-02T08:15:00Z, which is 10:15 EET.

Categories: 1, `مصروفات عامة` (general expenses); 2, `دخل عام` (general income); 3, `تحويل`
(transfer); and 4, `@Groceries`, a name that starts like a formula. Accounts: 1, `نقدًا` (cash);
2, `Savings, "main"`, with a comma and quotes; and 3, `-Card`, which starts like a formula.

| ID | Type | Cents | From | To | Category | Moment (UTC) | Date in Beirut | Description | What it covers |
| -: | :- | -: | -: | -: | -: | :- | :- | :- | :- |
| 1 | `INCOME` | 250,000 | 1 | | 2 | 2026-09-30T21:00:00Z | 2026-10-01 | `راتب` (salary) | The month's first moment |
| 2 | `EXPENSE` | 1,250 | 1 | | 4 | 2026-10-03T09:00:00Z | 2026-10-03 | `=HYPERLINK("http://x")` | A formula, in a category whose name starts with `@` |
| 3 | `TRANSFER` | 30,000 | 1 | 2 | 3 | 2026-10-05T06:30:00Z | 2026-10-05 | `ادخار` (saving) | A transfer, whose arrow follows the language |
| 4 | `EXPENSE` | 4,599 | 3 | | 1 | 2026-10-24T20:30:00Z | 2026-10-24 | `Lunch, "team"; @cmd` | The first 23:30, and a formula after a `;` |
| 5 | `EXPENSE` | 99 | 1 | | 99 | 2026-10-24T21:30:00Z | 2026-10-24 | ` +1 refund`, a tab, `tab-=2` | The second 23:30, a category with no row, and a formula after a space |
| 6 | `EXPENSE` | 375 | 7 | | 1 | 2026-10-24T22:00:00Z | 2026-10-25 | `Coffee` | Midnight after the 25-hour day, and an account with no row |
| 7 | `TRANSFER` | 10,000 | 2 | none | 3 | 2026-10-10T09:00:00Z | 2026-10-10 | `تحويل`, a line feed, `ناقص` (incomplete transfer) | A malformed transfer (BUG-06), with a line feed in its description |
| 8 | `EXPENSE` | 500 | 1 | 2 | 1 | 2026-10-12T09:00:00Z | 2026-10-12 | `Odd row` | An expense whose `transferAccountId` is set, which v1's report showed as a transfer route |
| 10 | `INCOME` | 500 | 1 | | 2 | 2026-10-31T21:59:59.999Z | 2026-10-31 | `-5` | The month's last moment, and a description that reads as a number |
| 9 | `EXPENSE` | 200 | 1 | | 1 | 2026-10-31T21:59:59.999Z | 2026-10-31 | `Refund;-5` | The same moment as 10 but given after it, and a number after a `;` |
| 11 | `EXPENSE` | 7,000 | 1 | | 1 | 2026-09-30T20:59:59.999Z | 2026-09-30 | `September` | The moment before the month, which the report leaves out |
| 12 | `EXPENSE` | 8,000 | 1 | | 1 | 2026-10-31T22:00:00Z | 2026-11-01 | `November` | The next month's first moment, which the report leaves out |

v1's report totals 2,505.00 of income, 70.23 of expenses, and a net of 2,434.77. The malformed
transfer counts in none of them.

v1 wrote these labels: in Arabic, the title `تقرير الإنفاق الشهري` (`report_title` in v1's
`values-ar/strings.xml`), the period `أكتوبر 2026`, and `2026-11-02 10:15 EET`; in English,
`Monthly Spending Report`, `October 2026`, and the same time. The period and the zone's name came
from the desktop Java's own locale data, not a phone's. The Rust port takes the labels from Dart,
so these files check only how the CSV carries them.

### The cells

The 2,121 cells are 121 chosen ones, and 2,000 strings of 0 to 12 characters that
`java.util.Random(20261124)` drew from this alphabet: `=`, `+`, `-`, `@`, `;`, tab, carriage
return, line feed, space, vertical tab, no-break space, `"`, `,`, `1`, `.`, `e`, `x`, `N`, `a`,
`I`, `p`, `0`, `f`, `D`, U+0001, and `_`. The specification of `java.util.Random` fixes its
algorithm, so the desktop Java and Android drew the same cells.

On Android, 5 of the 2,121 came out differently from the desktop Java, each where a no-break space
follows a `;` or a tab: Android guarded `a;` followed by a no-break space and `=1`, and the
desktop Java didn't. Android's `java.util.regex` runs on ICU, whose `\s` is Unicode's
`White_Space` property; a desktop Java's `\s` is six ASCII characters. The inner-space file lists
the 25 `White_Space` characters of the Basic Multilingual Plane and the `;` itself, because
`a;;=1` also gets its second part guarded. v1's unit tests ran on the desktop Java, so they never
saw the difference.

### How it was made

The writers were a throwaway unit test and a throwaway instrumented test in a scratch copy of v1,
so nothing was added to v1's tree or to the app.

1. Copy v1 out of Git, and point it at the Android SDK:

   ```bash
   mkdir v1 && git archive 6d57526 | tar -x -C v1
   echo "sdk.dir=$HOME/Android/Sdk" > v1/local.properties
   ```

2. Save the first test below as `v1/app/src/test/java/com/denariidolor/ReportFixtureWriterTest.kt`,
   and run it. With the Gradle cache from an earlier v1 build, this works offline:

   ```bash
   cd v1 && ./gradlew --offline :app:testDebugUnitTest --tests com.denariidolor.ReportFixtureWriterTest
   ```

   It writes the CSV files, the day starts, and `v1-f8-escape.tsv` to `v1/app/build/f8/`. It ran on
   Temurin 17.0.20.1, the Java 17 that v1's `jvmToolchain(17)` asks for, with Kotlin 2.4.20 and
   kotlinx-datetime 0.8.0.

3. Save the second test as
   `v1/app/src/androidTest/java/com/denariidolor/data/ReportEscapeOnDeviceTest.kt`, and build
   v1's debug app and its test app:

   ```bash
   ./gradlew --offline :app:assembleDebug :app:assembleDebugAndroidTest
   ```

4. Start an emulator that keeps nothing, check that no `com.denariidolor` app is installed
   (uninstalling one deletes its data), then install both apps, run the test, copy its two files
   out, and uninstall both apps:

   ```bash
   emulator -avd Denarii_API_36 -no-window -read-only -no-snapshot -no-audio
   adb shell pm list packages com.denariidolor
   adb install -t app/build/outputs/apk/debug/app-debug.apk
   adb install -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
   adb shell am instrument -w -e class com.denariidolor.data.ReportEscapeOnDeviceTest \
     com.denariidolor.test/androidx.test.runner.AndroidJUnitRunner
   adb exec-out run-as com.denariidolor cat files/v1-f8-escape-android.tsv > v1-f8-escape-android.tsv
   adb exec-out run-as com.denariidolor cat files/v1-f8-inner-space-android.txt > v1-f8-inner-space-android.txt
   adb uninstall com.denariidolor.test
   adb uninstall com.denariidolor
   ```

   It ran on `Denarii_API_36`
   (`google/sdk_gphone64_x86_64/emu64xa:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys`) and on
   `Denarii_API_26` (`Android/sdk_gphone_x86_64/generic_x86_64:8.0.0/OSR1.180418.026/6741039:userdebug/dev-keys`),
   and both wrote the same bytes.

The sources below write the cells' no-break space, figure space, and ideographic space as
`\u00A0`, `\u2007`, and `\u3000`; the files the tests ran held the characters themselves, which
Kotlin reads as the same strings.

```kotlin
/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor

import com.denariidolor.data.local.db.entity.AccountEntity
import com.denariidolor.data.local.db.entity.CategoryEntity
import com.denariidolor.data.local.db.entity.TransactionEntity
import com.denariidolor.domain.model.TransactionType
import com.denariidolor.domain.report.ReportCsvFormatter
import com.denariidolor.domain.usecase.GenerateReportUseCase
import com.denariidolor.testutil.FakeAccountRepository
import com.denariidolor.testutil.FakeCategoryRepository
import com.denariidolor.testutil.FakeTransactionRepository
import com.denariidolor.util.asZonedClock
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.Random
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.YearMonth
import org.junit.Test

// Writes fixture F8 for the Rust port (M2.7): two CSV exports of one month through v1's own GenerateReportUseCase and
// ReportCsvFormatter, the zone's day starts for that month, and ReportCsvFormatter.escape over chosen and random cells.
class ReportFixtureWriterTest {
    private val zone = ZoneId.of("Asia/Beirut")
    private val out = File("build/f8").apply { mkdirs() }

    private fun at(text: String) = Instant.parse(text).toEpochMilli()

    private fun txn(
        id: Long,
        type: TransactionType,
        description: String,
        cents: Long,
        category: Long,
        account: Long,
        date: String,
        transferTo: Long? = null
    ) = TransactionEntity(id, type, description, cents, category, account, transferTo, at(date), createdAtEpochMillis = 0)

    private val categories = listOf(
        CategoryEntity(id = 1, name = "مصروفات عامة"),
        CategoryEntity(id = 2, name = "دخل عام"),
        CategoryEntity(id = 3, name = "تحويل"),
        CategoryEntity(id = 4, name = "@Groceries")
    )

    private val accounts = listOf(
        AccountEntity(id = 1, name = "نقدًا", balanceCents = 0),
        AccountEntity(id = 2, name = "Savings, \"main\"", balanceCents = 0),
        AccountEntity(id = 3, name = "-Card", balanceCents = 0)
    )

    // October 2026 in Asia/Beirut runs from 2026-09-30T21:00Z (00:00 EEST) to 2026-10-31T22:00Z (00:00 EET). Clocks go
    // back from 00:00 EEST on Oct 25 to 23:00 EET on Oct 24, so Oct 24 lasts 25 hours and 23:30 happens twice.
    private val transactions = listOf(
        txn(1, TransactionType.INCOME, "راتب", 250_000, 2, 1, "2026-09-30T21:00:00Z"),
        txn(2, TransactionType.EXPENSE, "=HYPERLINK(\"http://x\")", 1_250, 4, 1, "2026-10-03T09:00:00Z"),
        txn(3, TransactionType.TRANSFER, "ادخار", 30_000, 3, 1, "2026-10-05T06:30:00Z", transferTo = 2),
        txn(4, TransactionType.EXPENSE, "Lunch, \"team\"; @cmd", 4_599, 1, 3, "2026-10-24T20:30:00Z"),
        txn(5, TransactionType.EXPENSE, " +1 refund\ttab-=2", 99, 99, 1, "2026-10-24T21:30:00Z"),
        txn(6, TransactionType.EXPENSE, "Coffee", 375, 1, 7, "2026-10-24T22:00:00Z"),
        txn(7, TransactionType.TRANSFER, "تحويل\nناقص", 10_000, 3, 2, "2026-10-10T09:00:00Z"),
        txn(8, TransactionType.EXPENSE, "Odd row", 500, 1, 1, "2026-10-12T09:00:00Z", transferTo = 2),
        txn(10, TransactionType.INCOME, "-5", 500, 2, 1, "2026-10-31T21:59:59.999Z"),
        txn(9, TransactionType.EXPENSE, "Refund;-5", 200, 1, 1, "2026-10-31T21:59:59.999Z"),
        txn(11, TransactionType.EXPENSE, "September", 7_000, 1, 1, "2026-09-30T20:59:59.999Z"),
        txn(12, TransactionType.EXPENSE, "November", 8_000, 1, 1, "2026-10-31T22:00:00Z")
    )

    private val clock = Clock.fixed(Instant.parse("2026-11-02T08:15:00Z"), zone)

    private fun writeCsv(languageTag: String, title: String, name: String) {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag(languageTag))
            val useCase = GenerateReportUseCase(
                FakeTransactionRepository(transactions),
                FakeCategoryRepository(categories),
                FakeAccountRepository(accounts),
                clock.asZonedClock()
            )
            val report = runBlocking { useCase(YearMonth(2026, 10)) }
            // As ReportExporter.write does: the translated title, and the default locale for everything else.
            File(out, name).writeBytes(ReportCsvFormatter.format(report.copy(title = title)).toByteArray(Charsets.UTF_8))
        } finally {
            Locale.setDefault(saved)
        }
    }

    private fun hex(text: String) = text.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }

    @Test
    fun writeFixtureF8() {
        writeCsv("ar", "تقرير الإنفاق الشهري", "v1-f8-report-ar.csv")
        writeCsv("en-US", "Monthly Spending Report", "v1-f8-report-en.csv")

        val days = (1..31).map { LocalDate.of(2026, 10, it).atStartOfDay(zone).toInstant().toEpochMilli() }
        val next = LocalDate.of(2026, 11, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        File(out, "v1-f8-day-starts.txt").writeText((days + next).joinToString("\n", postfix = "\n"))

        val chosen = listOf(
            "", "plain", "=1", "+1", "-1", "@a", "\t=1", "\r=1", "\t1", "\r1", "\t", "\r", " =1", "  @x", "\u00A0=1",
            "\u2007=1", "\u3000=1", "\u0085=1", "\u001C=1", "\u000B=1", "\u000C=1", "\n=1", "-12.50", "+5", "-5", "1e3",
            "-1e3", "-.5", "-5.", "-Infinity", "-NaN", "+NaN", "-0x1p3", "-1.5f", "-1.5d", "-1.5F", "-1.5D", "-1_000",
            "-١٢", "- 5", "-5 ", " -5", "-5\u0001", "\u0001-5", "-5\u00A0", "\u00A0-5", "-0x1.8p1", "-0X1P3", "-0x1p",
            "-0x.p1", "-0x.8p1", "-0x8.p1", "-1e", "-e5", "-.", "-", "+", "=", "@", "-inf", "-infinity", "-Infinityf",
            "-NaNd", "-1e+5", "-1e-5", "-1E5", "-1e5f", "--5", "+-5", "-0x1p3f", "-0x1P-3d", "-0x1g", "=1.5", "@5",
            "x;=1+1", "x\t=1+1", "Lunch; @cmd; ok", "Refund;-5", "a;b", "a;;=1", "a; \t=1", "a;\t;=1", "a;\u00A0=1",
            "a; -5 ; =x", "a;-5e2", "a;-NaN", "=1;=2", "1;=2", "a\t\t=1", "a;=", "a;-", "a;@", "a;+1;+x", "a;\u000B=1",
            "a;\n=1", "a;\r=1", "a;\u001C=1", "a,b", "a\"b", "a\nb", "a\rb", "a\tb", "💰=1", "=💰", ";=💰", "\"=1\"",
            "\u0000", "a\u0000;=1", "-5;=1", "=1;-5", " -5;=1", "@;-5", "a;-5\u0001", "a; -5", "a;-5 x", "-\u0001",
            "\u0001", "x;\u00A0-5", "Infinity", "-Infinity;-NaN", "a;- 5"
        )
        val alphabet = "=+-@;\t\r\n \u000B\u00A0\",1.exNaIp0fD\u0001_".toCharArray()
        val random = Random(20_261_124L)
        val generated = List(2_000) { String(CharArray(random.nextInt(13)) { alphabet[random.nextInt(alphabet.size)] }) }
        File(out, "v1-f8-escape.tsv").writeText(
            (chosen + generated).joinToString("\n", postfix = "\n") { "${hex(it)}\t${hex(ReportCsvFormatter.escape(it))}" }
        )
    }
}
```

```kotlin
/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.domain.report.ReportCsvFormatter
import java.io.File
import java.util.Random
import org.junit.Test
import org.junit.runner.RunWith

// Runs ReportCsvFormatter.escape over F8's cells on a device, whose java.util.regex is ICU's, and leaves the results in
// the app's files folder for `adb exec-out run-as` to copy.
@RunWith(AndroidJUnit4::class)
class ReportEscapeOnDeviceTest {
    private fun hex(text: String) = text.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }

    @Test
    fun writeEscapeVectors() {
        val out = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
        val chosen = listOf(
            "", "plain", "=1", "+1", "-1", "@a", "\t=1", "\r=1", "\t1", "\r1", "\t", "\r", " =1", "  @x", "\u00A0=1",
            "\u2007=1", "\u3000=1", "\u0085=1", "\u001C=1", "\u000B=1", "\u000C=1", "\n=1", "-12.50", "+5", "-5", "1e3",
            "-1e3", "-.5", "-5.", "-Infinity", "-NaN", "+NaN", "-0x1p3", "-1.5f", "-1.5d", "-1.5F", "-1.5D", "-1_000",
            "-١٢", "- 5", "-5 ", " -5", "-5\u0001", "\u0001-5", "-5\u00A0", "\u00A0-5", "-0x1.8p1", "-0X1P3", "-0x1p",
            "-0x.p1", "-0x.8p1", "-0x8.p1", "-1e", "-e5", "-.", "-", "+", "=", "@", "-inf", "-infinity", "-Infinityf",
            "-NaNd", "-1e+5", "-1e-5", "-1E5", "-1e5f", "--5", "+-5", "-0x1p3f", "-0x1P-3d", "-0x1g", "=1.5", "@5",
            "x;=1+1", "x\t=1+1", "Lunch; @cmd; ok", "Refund;-5", "a;b", "a;;=1", "a; \t=1", "a;\t;=1", "a;\u00A0=1",
            "a; -5 ; =x", "a;-5e2", "a;-NaN", "=1;=2", "1;=2", "a\t\t=1", "a;=", "a;-", "a;@", "a;+1;+x", "a;\u000B=1",
            "a;\n=1", "a;\r=1", "a;\u001C=1", "a,b", "a\"b", "a\nb", "a\rb", "a\tb", "💰=1", "=💰", ";=💰", "\"=1\"",
            "\u0000", "a\u0000;=1", "-5;=1", "=1;-5", " -5;=1", "@;-5", "a;-5\u0001", "a; -5", "a;-5 x", "-\u0001",
            "\u0001", "x;\u00A0-5", "Infinity", "-Infinity;-NaN", "a;- 5"
        )
        val alphabet = "=+-@;\t\r\n \u000B\u00A0\",1.exNaIp0fD\u0001_".toCharArray()
        val random = Random(20_261_124L)
        val generated = List(2_000) { String(CharArray(random.nextInt(13)) { alphabet[random.nextInt(alphabet.size)] }) }
        File(out, "v1-f8-escape-android.tsv").writeText(
            (chosen + generated).joinToString("\n", postfix = "\n") { "${hex(it)}\t${hex(ReportCsvFormatter.escape(it))}" }
        )
        println("FIXTURE escape vectors=${chosen.size + generated.size}")
    }

    // Every character of the Basic Multilingual Plane but the surrogates, between `a;` and `=1`: the guard after a `;`
    // fires when its regex's `\s*` takes the character, so the list is the set of characters that `\s` matches here.
    @Test
    fun writeInnerWhitespace() {
        val out = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
        val guarded = (0..0xFFFF).filter { it !in 0xD800..0xDFFF }.filter { code ->
            val c = code.toChar()
            ReportCsvFormatter.escape("a;${c}=1").contains("${c}'=1")
        }
        File(out, "v1-f8-inner-space-android.txt").writeText(guarded.joinToString("\n", postfix = "\n") { "%04x".format(it) })
        println("FIXTURE inner whitespace=${guarded.size}")
    }
}
```
