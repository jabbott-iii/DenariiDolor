# Vault fixtures

Files that v1, the Kotlin app at commit `6d57526`, wrote with its own code on Android. The tests in
`../f7.rs` read them, and they never change. Never edit or regenerate one in place; a new fixture
gets a new name.

## Fixture F7

Fixture F7 of `docs/specs/android-v1-formats.md`: what v1's `SecurityProfileService.normalizeAnswer`
made of answers on the phones' own ICU, on API 26 and on API 36. It checks invariant 5 of that
spec, that answers normalize exactly as v1 normalized them, and settles open questions 2 (`\s` on
Android) and 3 (Unicode versions). M3.3 made it on 2026-11-25.

| File | What it holds | Size | SHA-256 |
| :- | :- | -: | :- |
| `v1-f7-api26-code-points.tsv` | Each of the 6,023 code points that `normalizeAnswer` changed on its own on API 26, one to a line: the code point in hexadecimal, a tab, and what it became, as hexadecimal code points separated by spaces, or nothing when it became empty | 71,680 bytes | `498b44c53159e51830b44cdfcc72ddc69713ae5575afa085275c9e190e09d5ec` |
| `v1-f7-api36-code-points.tsv` | The same for API 36: 6,349 code points | 75,295 bytes | `da545549041091638c65b4e5dcae325d2458ec0f07235c35793798867efa7cf9` |
| `v1-f7-api26-unassigned.txt` | The 638 runs of code points that API 26's Unicode data doesn't assign, as `start..end` in hexadecimal | 7,687 bytes | `b52f449fbc38cc70a0292f29120a3752846afd38351836ffd448b3ed07c0e46e` |
| `v1-f7-api36-unassigned.txt` | The same for API 36: 731 runs | 8,948 bytes | `ea8b57df6f4ecefeccaee9c665a3b384d3a275468367809d379e98721a6817d3` |
| `v1-f7-strings.tsv` | 2,080 strings and what `normalizeAnswer` made of each on API 36, as code points: 80 chosen ones, then 2,000 drawn at random. API 26 wrote the same lines under its own header | 117,899 bytes | `50ea75d1644c6d06f4166228083baee0bc781b6dfe48797ffb2f2838a5fbd93c` |

Every file starts with one header line, `#` followed by the device's build fingerprint, its ICU
version, and its Unicode version: ICU 58.2 and Unicode 9.0 on API 26, and ICU 76.1 and Unicode 16.0
on API 36. Every code point from U+0000 to U+10FFFF but the surrogates was normalized alone; one
that isn't listed came out as it went in.

### The strings

The 80 chosen strings are the characters that the format spec names for F7 (no-break space,
U+3000, U+2028, U+2029, U+1680, U+000B, tabs, Turkish dotted and dotless I, a word-final Greek
sigma, fullwidth letters, and combining accents), v1's own test cases, and the edges near them.
The 2,000 drawn ones are 0 to 12 characters that `java.util.Random(20261125)` drew from 51
characters whose neighbors change what they become: spaces and separators, Latin, Greek, and
Turkish case pairs, `Σ`, `σ`, and `ς`, an apostrophe, a soft hyphen, combining marks, Hangul jamo,
a titlecase digraph, and three characters outside the Basic Multilingual Plane. The specification
of `java.util.Random` fixes its algorithm, so both levels drew the same strings.

### What it showed

Two things that v1 did on Android differed from what the port first did, and both are now the
port's behavior:

- **Kotlin's `trim()` removed U+180E**, the Mongolian vowel separator, on both levels. Android's
  `Character.isWhitespace` still counts it, though Unicode 6.3 stopped calling it a space and a
  desktop Java's doesn't. `denarii_core::is_whitespace` counts it now, for every trim in the port.
- **The final sigma followed Android's rule.** Without an `İ` or a character outside the Basic
  Multilingual Plane in the string, Android's `String.toLowerCase` makes `Σ` final only when the
  character just before it is in the general categories Lu, Ll, or Lt and the one just after
  isn't. Unicode's rule, which Rust's `str::to_lowercase` follows, skips apostrophes, soft hyphens,
  and other case-ignorable characters, so `Α'Σ` became `α'σ` on Android and `α'ς` in the first port.
  With an `İ` or a supplementary character, Android hands the string to ICU, which follows
  Unicode's rule, so `Α'Σ 𐐀` became `α'ς 𐐨`. Six of the drawn strings showed the difference.

The rest matched the first port: `\s` matched Unicode's `White_Space`, U+0085 included, and every
character that each level's Unicode data assigns normalized alone as the port normalizes it.
Characters a level didn't assign came out unchanged there. The port's newer Unicode (18.0 for NFKC,
17.0 for lowercase) changes 443 of those on API 26, from scripts and letters that Unicode 10.0 to
18.0 added, such as Georgian Mtavruli capitals (U+1C90 to U+1CBF) and the era sign ㋿ (U+32FF),
and 117 on API 36, from Unicode 17.0 and 18.0. An answer that v1 stored with one of them on such a
phone doesn't open in the port, but the phone's own fonts and keyboards didn't know them either.

### How it was made

The writer was a throwaway instrumented test in a scratch copy of v1, so nothing was added to v1's
tree or to the app.

1. Copy v1 out of Git, and point it at the Android SDK:

   ```bash
   mkdir v1 && git archive 6d57526 | tar -x -C v1
   echo "sdk.dir=$HOME/Android/Sdk" > v1/local.properties
   ```

2. Save the test below as
   `v1/app/src/androidTest/java/com/denariidolor/data/AnswerNormalizationOnDeviceTest.kt`, and
   build v1's debug app and its test app. With the Gradle cache from an earlier v1 build, this
   works offline with v1's Gradle 9.6.1 wrapper:

   ```bash
   cd v1 && ./gradlew --offline :app:assembleDebug :app:assembleDebugAndroidTest
   ```

3. Start an emulator that keeps nothing, check that no `com.denariidolor` app is installed
   (uninstalling one deletes its data), then install both apps, run the test, copy its three files
   out, and uninstall both apps:

   ```bash
   emulator -avd Denarii_API_26 -no-window -read-only -no-snapshot -no-audio
   adb shell pm list packages com.denariidolor
   adb install -t app/build/outputs/apk/debug/app-debug.apk
   adb install -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
   adb shell am instrument -w -e class com.denariidolor.data.AnswerNormalizationOnDeviceTest \
     com.denariidolor.test/androidx.test.runner.AndroidJUnitRunner
   adb exec-out run-as com.denariidolor cat files/v1-f7-code-points.tsv > v1-f7-api26-code-points.tsv
   adb exec-out run-as com.denariidolor cat files/v1-f7-unassigned.txt > v1-f7-api26-unassigned.txt
   adb exec-out run-as com.denariidolor cat files/v1-f7-strings.tsv > v1-f7-strings.tsv
   adb uninstall com.denariidolor.test
   adb uninstall com.denariidolor
   ```

   It ran on `Denarii_API_26`
   (`Android/sdk_gphone_x86_64/generic_x86_64:8.0.0/OSR1.180418.026/6741039:userdebug/dev-keys`)
   in 9.4 s, and on `Denarii_API_36`
   (`google/sdk_gphone64_x86_64/emu64xa:16/BE2A.250530.026.F3/13894323:userdebug/dev-keys`) in
   22.5 s, with the files named for each level. The strings file kept is API 36's; API 26's had
   SHA-256 `8fad3b03cf2bc4ed6123220e1324c746662f68479825620e18726ba0bdec3210`, and differed only
   in its header.

```kotlin
/*
 * Copyright (c) 2026 Joseph Anthony Abbott III. All rights reserved.
 * Proprietary and confidential. Use is governed by the LICENSE file; copying, modifying or distributing this file without
 * written permission is prohibited.
 */

package com.denariidolor.data

import android.icu.lang.UCharacter
import android.icu.util.VersionInfo
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.denariidolor.data.local.preferences.JcaCryptoPrimitives
import com.denariidolor.data.local.preferences.SecurityProfileService
import java.io.File
import java.util.Locale
import java.util.Random
import org.junit.Test
import org.junit.runner.RunWith

// Writes fixture F7 for the Rust port (M3.3): v1's own SecurityProfileService.normalizeAnswer, on this device's ICU, for
// every code point alone, for chosen strings, and for strings drawn at random from characters whose context matters.
@RunWith(AndroidJUnit4::class)
class AnswerNormalizationOnDeviceTest {
    private val crypto = JcaCryptoPrimitives()
    private val files = InstrumentationRegistry.getInstrumentation().targetContext.filesDir

    private fun normalize(text: String) = SecurityProfileService.normalizeAnswer(text, crypto)

    private fun hex(text: String): String =
        text.codePoints().toArray().joinToString(" ") { String.format(Locale.ROOT, "%04X", it) }

    private fun header() = "# ${Build.FINGERPRINT} ICU ${VersionInfo.ICU_VERSION} Unicode ${UCharacter.getUnicodeVersion()}\n"

    private fun codePoints() = (0..0x10FFFF).filter { it !in 0xD800..0xDFFF }

    /** Each code point whose normalized form isn't itself: the code point, a tab, and what it became. */
    @Test
    fun recordsEveryCodePointThatChanges() {
        File(files, "v1-f7-code-points.tsv").bufferedWriter().use { out ->
            out.write(header())
            for (codePoint in codePoints()) {
                val text = String(Character.toChars(codePoint))
                val normalized = normalize(text)
                if (normalized != text) out.write("${hex(text)}\t${hex(normalized)}\n")
            }
        }
    }

    /** The runs of code points that this device's Unicode data doesn't assign, which normalization leaves alone. */
    @Test
    fun recordsTheUnassignedCodePoints() {
        File(files, "v1-f7-unassigned.txt").bufferedWriter().use { out ->
            out.write(header())
            var start = -1
            for (codePoint in 0..0x110000) {
                val unassigned = codePoint <= 0x10FFFF && Character.getType(codePoint) == Character.UNASSIGNED.toInt()
                if (unassigned && start < 0) start = codePoint
                if (!unassigned && start >= 0) {
                    out.write(String.format(Locale.ROOT, "%04X..%04X\n", start, codePoint - 1))
                    start = -1
                }
            }
        }
    }

    /** Each string and its normalized form, as code points. */
    @Test
    fun recordsStrings() {
        val random = Random(SEED)
        val drawn = List(DRAWN) {
            buildString { repeat(random.nextInt(MAX_DRAWN_LENGTH + 1)) { append(ALPHABET[random.nextInt(ALPHABET.size)]) } }
        }
        File(files, "v1-f7-strings.tsv").bufferedWriter().use { out ->
            out.write(header())
            for (text in CHOSEN + drawn) out.write("${hex(text)}\t${hex(normalize(text))}\n")
        }
    }

    private companion object {
        const val SEED = 20261125L
        const val DRAWN = 2_000
        const val MAX_DRAWN_LENGTH = 12

        /** Characters whose neighbors change what they become: spaces, case pairs, combining marks, and jamo. */
        val ALPHABET = listOf(
            " ", "\t", "\n", "\u000B", "\u000C", "\r", "\u001C", "\u001F", "\u0085", " ", " ", " ",
            " ", " ", " ", "　", "​", "﻿", "a", "A", "I", "i", "İ", "ı", "Σ",
            "σ", "ς", "Α", "α", "'", ".", "­", "́", "̇", "̣", "ͅ", "e",
            "é", "ﬁ", "Ａ", "ᄀ", "ᅡ", "ᆨ", "ǅ", "ẞ", "Å", "1", "١",
            "𐐀", "𝐀", "😀"
        )

        /** Fixture F7's list in docs/specs/android-v1-formats.md, v1's own test cases, and the edges near them. */
        val CHOSEN = listOf(
            "", " ", "\t\n", "　", " ",
            " rex dog ", "rex　dog", "rex dog", "rex dog", "rex dog", "rex\u000Bdog",
            "\trex\t\tdog\t", "rex \t\u000B dog", "rex dog", "rex dog", "rex dog", "rex  dog",
            "rex᠎dog", "\u0085rex\u0085", "\u001Frex\u001C", "rex\u001Edog", "​rex​", "﻿rex",
            "İSTANBUL", "ISPARTA", "ıspanak", "Iİıi", "DİYARBAKIR", "İ", "İ̇",
            "ΟΔΟΣ", "ΟΔΟΣ ΣΟΦΙΑΣ", "Σ",
            "ΑΣ", "ΑΣ.", "ΑΣ'", "Α'Σ", "ΑΣΑ", "ΑΣ1",
            "ΆΣ", "ΑΣ́", "ΑΣͅ", "ᾼΣ", "Α­Σ",
            "ΑΣ 𐐀", "Α'Σ 𐐀", "ΣΑ", "1Σ",
            "ＬＩＮＣＯＬＮ Elementary", "Ｒｅｘ", "Lincoln Elementary",
            "  lincoln   ELEMENTARY ", "Café", "cafẹ́", "ạ́", "ạ́", "é́",
            "각", "Å", "Ω", "ẞ", "ΐ", "ᾈ", "ǅ", "Ǆ",
            "ﬁrst ﬂat №⁵", "①②③ Ⅻ", "ﻻ ﷲ",
            "मेरा   पहला", "ｶﾞｯｺｳ",
            "  Café   Ｍüller ", "CAFÉ MÜLLER", "𝐀𝐁𝐂 school",
            "𐐀𐐁", "😀 rex", "我的 第一所学校",
            "مدرستي  الأولى", "㍿", "ﷺ", "Ⅻ"
        )
    }
}
```
