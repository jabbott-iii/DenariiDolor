# Subsystem: `android-v1-formats`

This spec is the compatibility contract between the Kotlin app that ships today (v1.x, commit `6d57526`) and the Flutter and
Rust rewrite. On 2026-11-23 the user chose to upgrade existing Play installs in place: the new app ships under the same
application ID and signing key, and on its first start it must open the data the Kotlin app left on the phone, with no
export or re-entry. Everything below describes bytes, names, and rules that already exist on users' phones. The rewrite may
add to them, but it must keep reading every one of them. The security findings that shaped these formats keep their IDs:
CS-15 (key management), CS-07 (biometric sign-in), CS-09 (no `EncryptedSharedPreferences`), CS-13 (lockout on a monotonic
clock), CS-14 (recovery rules), CS-17 (wipe as a cryptographic erase), CS-20 (CSV formula guard), CS-28 (encrypted backups),
and CS-04 (private share folder).

## How to read this spec

- **Citations.** Every fact cites the Kotlin source as `path:line` at commit `6d57526`, because that source leaves the working
  tree with the rewrite. Two prefixes shorten the paths:
  - `app:` means `app/src/main/java/com/denariidolor/`;
  - `shared:` means `shared/src/commonMain/kotlin/com/denariidolor/`.

  Other paths start at the repository root. Read any cited file with `git show 6d57526:<full path>`.
- **Sources outside the repository.** A few facts come from the behavior of Android, Room, or SQLCipher rather than from this
  code. Each one says so and names the fixture in [Tests](#tests) that must confirm it.
- **Byte layouts** are big-endian unless a layout says otherwise.

## Why this contract

Each user's records exist only on their phone, encrypted under keys that can't leave it. A rewrite that can't read them
loses the user's data, and nobody can repair it by hand. Three things make in-place reading hard:

1. **Android Keystore keys** (keys held by the phone's secure hardware or system service, which never reveal their bytes to
   the app) protect the profile and take part in every PIN check. They belong to the app's Linux user ID, so only the same
   package, signed with the same key, can use them after an update. The rewrite must keep calling them through Kotlin code in
   `android/`, under the same aliases and with the same parameters.
2. **SQLCipher** (SQLite with page-level AES encryption) holds the ledger. Its file is opened with a raw 32-byte key that the
   profile unwraps. Rust can open the same file only if it uses compatible SQLCipher settings and the same key syntax.
3. **Text rules ran on Android's own libraries.** Security answers were normalized with Android's Unicode and regular
   expression code, and backups store text in Java's "modified UTF-8". The Rust port must reproduce both exactly, or a correct
   answer or a valid backup stops opening.

The alternative the user rejected was a one-time migration through a backup file. It would have needed only the backup
reader, but every user who skipped the export would have lost their data.

## 1. App identity and file locations

| What | Value | Source |
| :- | :- | :- |
| Application ID | `com.denariidolor` | `app/build.gradle.kts:25` |
| SDK levels | `minSdk` 26, `targetSdk` 36, `compileSdk` 37 | `app/build.gradle.kts:22`, `:26`, `:27` |
| Version code | Set by CD from the release tag; the local default is 3. The new app's build number must be higher than the last one uploaded to Play, which the repository doesn't record. | `app/build.gradle.kts:28-30` |
| Launcher activity | `com.denariidolor.presentation.ui.auth.LoginActivity` (exported). Some launchers drop a home-screen icon whose component disappears, so the rewrite keeps this name as an exported `activity-alias` of Flutter's `MainActivity`, which isn't exported (the maintainer's choice on 2026-11-23, built in M1.1). | `app/src/main/AndroidManifest.xml:18-26` |
| Android backup | `android:allowBackup="false"`; both rule files exclude the `database`, `sharedpref`, and `file` domains from cloud backup and device transfer. | `app/src/main/AndroidManifest.xml:10-12`, `app/src/main/res/xml/backup_rules.xml:3-7`, `app/src/main/res/xml/data_extraction_rules.xml:3-14` |
| Share provider | `androidx.core.content.FileProvider` at authority `com.denariidolor.fileprovider`, serving only `cache/reports/` | `app/src/main/AndroidManifest.xml:32-40`, `app/src/main/res/xml/file_paths.xml:3` |

Files in the app's private data directory (`/data/data/com.denariidolor/` for the primary user):

| File | Holds | Written by | Source |
| :- | :- | :- | :- |
| `databases/denarii_dolor.db` | The SQLCipher database | Room through SQLCipher | `app:util/Constants.kt:13`, `app:data/local/vault/VaultConfig.kt:13` |
| `databases/denarii_dolor.db-wal`, `-shm`, `-journal` | SQLite's write-ahead log, its shared-memory index, and the rollback journal. The code sets no journal mode, so Room's default applies: write-ahead logging unless the phone reports itself as low on memory (Room behavior). | SQLite | `app:data/local/db/DatabaseHolder.kt:55-58` |
| `shared_prefs/vault_profile.xml` | The encrypted security profile (section 3) | `KeystoreProfileStore` | `app:data/local/vault/VaultConfig.kt:14` |
| `shared_prefs/secure_prefs.xml` | The v1.0.x profile, if not yet upgraded (section 4) | `EncryptedSharedPreferences` | `app:data/local/vault/VaultConfig.kt:16` |
| `shared_prefs/db_key_prefs.xml` | The v1.0.x database key, if not yet upgraded | `EncryptedSharedPreferences` | `app:data/local/vault/VaultConfig.kt:17` |
| `shared_prefs/ui_prefs.xml` | Currency, theme, onboarding, and rate-prompt settings (table below), in plain text | `SharedPreferences` | `app:data/local/preferences/CurrencyPreferences.kt:51` |
| `cache/reports/` | The last report written for sharing; emptied before each share and when the session ends (CS-04) | `ReportExporter` | `app:data/export/ReportExporter.kt:53-61`, `:94` |

The keys in `ui_prefs`. An install from before a key existed has none of it, and the default column says what that means:

| Key | Type | Meaning | Default when missing | Source |
| :- | :- | :- | :- | :- |
| `currency` | string | ISO 4217 code of the display currency; only codes with 2 decimal places are accepted | `USD` | `app:data/local/preferences/CurrencyPreferences.kt:33`, `:52`; `shared:util/Currencies.kt:12`, `:20`, `:26` |
| `dark_mode` | boolean | The user's theme choice | Missing means "follow the device" | `app:data/local/preferences/ThemePreferences.kt:33`, `:37` |
| `tour_pending` | boolean | The welcome tour still has to run | `false` | `app:data/local/preferences/GuidancePreferences.kt:33`, `:94` |
| `checklist_active` | boolean | The getting-started checklist is showing | `false` | `app:data/local/preferences/GuidancePreferences.kt:36`, `:95` |
| `rate_prompt_saves` | int | Transactions saved, counted for the rate prompt | 0 | `app:data/local/preferences/GuidancePreferences.kt:79`, `:96` |
| `rate_prompt_times_asked` | int | Times the rate prompt showed | 0 | `app:data/local/preferences/GuidancePreferences.kt:80`, `:97` |
| `rate_prompt_finished` | boolean | The user rated or declined for good | `false` | `app:data/local/preferences/GuidancePreferences.kt:81`, `:98` |

A Flutter plugin that keeps its own preferences file or adds a key prefix won't see these values. The rewrite reads
`ui_prefs.xml` by its name, with these keys.

## 2. Android Keystore keys

All vault keys live in the `AndroidKeyStore` provider under the alias prefix `denarii_vault_`
(`app:data/local/vault/VaultConfig.kt:15`, `app:data/local/vault/KeystoreSecrets.kt:30-32`, `:142`). None is created with
StrongBox (a separate secure chip) requested.

| Alias | Algorithm and size | Purpose | User authentication | Created | Source |
| :- | :- | :- | :- | :- | :- |
| `denarii_vault_storage` | AES, 256 bits, GCM mode, no padding | Encrypt and decrypt the profile blob | None | On the first profile write | `app:data/local/vault/KeystoreSecrets.kt:35-36`, `:96-100`, `:138-141` |
| `denarii_vault_device` | HMAC-SHA256, the provider's default size | Sign: mixes every PIN and answer guess with a device-only key | None | Before a new profile is written | `app:data/local/vault/KeystoreSecrets.kt:53-63`, `:116-120` |
| `denarii_vault_biometric` | AES, 256 bits, GCM mode, no padding | Wrap the database key for biometric sign-in (CS-07) | `BIOMETRIC_STRONG` for every use; invalidated when the enrolled biometrics change | Each time biometric sign-in is turned on (the old key is deleted first) | `app:data/local/vault/KeystoreSecrets.kt:66-70`, `:96-114` |
| `_androidx_security_master_key_` | The v1.0.x master key of `androidx.security:security-crypto` (AES-256-GCM key scheme) | Decrypt the two v1.0.x files | None | By v1.0.x | `app:data/local/vault/VaultConfig.kt:18`, `app:data/local/vault/LegacyProfileStorage.kt:56-58` |

How the biometric key requires authentication depends on the API level (`app:data/local/vault/KeystoreSecrets.kt:101-109`):

- API 30 and later: `setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)`, so a strong biometric must authorize each
  use.
- API 26–29: `setUserAuthenticationValidityDurationSeconds(-1)`, the older way to require a biometric for each use.
- Both call `setUserAuthenticationRequired(true)` and `setInvalidatedByBiometricEnrollment(true)`.

AES-GCM (Galois/Counter Mode, an authenticated cipher that detects any change to the data) runs with a 12-byte IV
(initialization vector, the per-message value GCM needs) and a 128-bit tag in every use here
(`app:data/local/vault/KeystoreSecrets.kt:138-139`). Keystore keys generate their own random IV on encryption, so every
Keystore-encrypted value stores the IV in front of the ciphertext (`app:data/local/vault/KeystoreSecrets.kt:41`).

## 3. The security profile and key wrapping (CS-15)

### The database key

The database key is 32 random bytes from `SecureRandom`, created at setup and never stored in the clear
(`app:data/local/preferences/SecurityProfileService.kt:132`, `:338`). Up to three wraps of it are stored, one under the PIN,
one under the security answer, and an optional one under the biometric key.

### The profile blob

`shared_prefs/vault_profile.xml` holds one string entry, `profile` (`app:data/local/vault/KeystoreProfileStore.kt:100`):

```text
profile = Base64( iv[12] || ciphertext || tag[16] )
  key:             Keystore alias denarii_vault_storage (AES-256-GCM)
  associated data: UTF-8 "denarii-dolor:profile:v2"
  plaintext:       UTF-8 JSON object (org.json), described below
```

- Sources: `app:data/local/vault/KeystoreProfileStore.kt:39-40`, `:56-71`, `:101`, `app:data/local/vault/KeystoreSecrets.kt:35-51`.
- Base64 is `java.util.Base64`'s basic alphabet with padding and no line breaks
  (`app:data/local/vault/KeystoreProfileStore.kt:40`, `:59`).
- Associated data (AAD) is data that GCM authenticates but doesn't encrypt; a different value makes decryption fail.
- A missing `profile` entry reads as an empty profile (`app:data/local/vault/KeystoreProfileStore.kt:57`). An entry that
  fails Base64, decryption, or JSON parsing raises `SecureStorageException` (`:58-67`).
- Every edit re-encrypts the whole object with a new IV and saves it with a synchronous `commit()` (`:36-43`).

The JSON keys (`app:data/local/preferences/SecurityProfileService.kt:99-107`, `:346-352`):

| Key | JSON type | Meaning |
| :- | :- | :- |
| `profile_version` | number | Always 2 for this format (`:327`) |
| `security_question` | string | The question, trimmed (`:242`) |
| `pin_wrap`, `answer_wrap` | string, Base64 | `iv[12] || AES-256-GCM(kek, iv, aad = slot context, databaseKey[32])`, so 60 bytes before Base64 (`:253`) |
| `pin_salt`, `answer_salt` | string, Base64 | 16 random bytes (`:249`, `:329`) |
| `pin_iterations`, `answer_iterations` | number | PBKDF2 iterations; written as 210,000 and accepted from 10,000 to 1,000,000 (`:330`, `:336-337`) |
| `biometric_wrap` | string, Base64, optional | `iv[12] || AES-GCM(biometric key, databaseKey[32])` with no associated data; present only while biometric sign-in is on (`:227-233`; `app:data/local/vault/Vault.kt:177`) |
| `failed_attempts` | number | Failed PIN and answer checks since the last success (`:286-291`) |
| `lock_duration_millis` | number | Length of the current lockout (`:293`) |
| `lock_started_at_elapsed_millis` | number | `SystemClock.elapsedRealtime()` when the lockout started (`:294`) |
| `lock_boot_count` | number | `Settings.Global.BOOT_COUNT` then, or −1 if unknown (`:295`, `:332`; `app:data/local/vault/AndroidMonotonicClock.kt:18-24`) |

The profile counts as set up only when `profile_version` is 2, the question isn't blank, and both slots have a wrap, a salt,
and an iteration count within bounds (`app:data/local/preferences/SecurityProfileService.kt:111-117`). Anything less reads
as "no profile", and the next setup deletes the database as an orphan (`app:data/local/vault/Vault.kt:83-86`).

### Key wrapping for the PIN and answer slots

Each slot has a context string, used twice: as the prefix of the device-key input and as the GCM associated data
(`app:data/local/preferences/SecurityProfileService.kt:99-107`):

| Slot | Context (UTF-8) | Secret |
| :- | :- | :- |
| `pin` | `denarii-dolor:pin:v2` | The PIN exactly as typed (6–12 ASCII digits) |
| `answer` | `denarii-dolor:answer:v2` | The normalized answer (below) |

The key-encryption key (KEK, the key that encrypts another key) is derived like this
(`app:data/local/preferences/SecurityProfileService.kt:248-283`):

```text
stretched = PBKDF2-HMAC-SHA256(password = UTF-8(secret), salt[16], iterations, length = 32 bytes)
kek       = HMAC-SHA256(key = Keystore denarii_vault_device, message = UTF-8(context) || stretched)   // 32 bytes
wrap      = iv[12] || AES-256-GCM-Seal(kek, iv, aad = UTF-8(context), plaintext = databaseKey[32])  // ct[32] || tag[16]
```

- PBKDF2 (Password-Based Key Derivation Function 2) repeats a hash to slow down guessing. It runs through the JCA name
  `PBKDF2WithHmacSHA256` with a 256-bit output (`app:data/local/preferences/SecretHashing.kt:12-22`). The password bytes are
  the secret's UTF-8 encoding (`shared:data/local/preferences/CryptoPrimitives.kt:19`), which the non-ASCII known answers in
  `CryptoKnownAnswerTest` confirm.
- HMAC (a keyed hash) with the device key means a guess can only be checked on this phone.
- The salt and the IV are new random bytes for every seal (`:249-250`). The IV for the slots comes from `SecureRandom`, not
  from the Keystore.
- A wrong secret makes GCM authentication fail, which reads as a rejected attempt (`:260-274`;
  `app:data/local/preferences/JcaCryptoPrimitives.kt:25-29`).

### Normalization of the security answer

`normalizeAnswer` (`app:data/local/preferences/SecurityProfileService.kt:334`, `:357-358`) applies these steps in order:

1. Unicode Normalization Form KC (NFKC), through `java.text.Normalizer` (`app:data/local/preferences/JcaCryptoPrimitives.kt:33`).
2. Kotlin's `trim()`, which removes leading and trailing characters for which `Char.isWhitespace()` is true.
3. Replace each run matching the regular expression `\s+` with one U+0020 space.
4. Kotlin's `lowercase()`, which is locale-independent (the root locale).

Steps 3 and 4 ran on Android's runtime, whose `java.util.regex` and case mapping are built on ICU (the International
Components for Unicode library), not on the desktop JDK's code. The JVM unit tests therefore don't pin down the exact set
of characters `\s` matched on phones. See [Open questions](#open-questions-and-contradictions).

The security question is stored with `trim()` only (`:242`). The PIN is never normalized.

### Setup and recovery rules (CS-14)

`validateSetup` (`app:data/local/preferences/SecurityProfileService.kt:361-382`) checks, in this order:

| Order | Rule | Result when broken |
| :- | :- | :- |
| 1 | The PIN matches `^[0-9]{6,12}$` (`:333`) | `INVALID_PIN_FORMAT` |
| 2 | The confirmation equals the PIN | `PIN_MISMATCH` |
| 3 | The trimmed question isn't empty | `SECURITY_QUESTION_REQUIRED` |
| 4 | The trimmed question is at most 200 UTF-16 code units (`:341`) | `SECURITY_QUESTION_TOO_LONG` |
| 5 | The normalized answer isn't empty | `SECURITY_ANSWER_REQUIRED` |
| 6 | The normalized answer is at least 6 code points (`:339`) | `SECURITY_ANSWER_TOO_SHORT` |
| 7 | The normalized answer is at most 100 UTF-16 code units (`:340`) | `SECURITY_ANSWER_TOO_LONG` |
| 8 | The normalized question doesn't contain the normalized answer | `SECURITY_ANSWER_IN_QUESTION` |
| 9 | The normalized answer isn't equal to the PIN | `SECURITY_ANSWER_MATCHES_PIN` |

The minimum counts code points and the maximums count UTF-16 code units, so a port must count each limit the same way.

### Lockout (CS-13)

Failed PIN checks, failed answer checks, and failed v1.0.x checks share one counter
(`app:data/local/preferences/SecurityProfileService.kt:187-197`):

- The first 4 failures only count. A rejected attempt reports `5 − failures` attempts left (`:196`, `:342`).
- From the 5th failure on, the lockout lasts `30 s × 2^(failures − 5)`, capped at 15 minutes (`:343-344`, `:385-389`):

  | Failure | 5th | 6th | 7th | 8th | 9th | 10th and later |
  | :- | :- | :- | :- | :- | :- | :- |
  | Lockout | 30 s | 60 s | 120 s | 240 s | 480 s | 15 min |

- The timer runs on `SystemClock.elapsedRealtime()` (milliseconds since boot, including deep sleep, which changing the date
  can't affect), plus the boot count (`app:data/local/vault/AndroidMonotonicClock.kt:14-24`).
- If the elapsed time is earlier than the stored start, or the boot count changed, the time already served is unknown, so the
  whole lockout starts again from now (`:204-223`).
- Any success resets the counter and the timer (`:191`, `:301-306`). A successful biometric sign-in also resets them, an
  accepted risk (CS-A3) (`:199-201`).
- Every public method of `SecurityProfileService` is `@Synchronized`, so two checks can't run at once to get around the
  lockout (`:89-90`).

### Vault operations

`Vault` (`app:data/local/vault/Vault.kt`) runs every operation off the main thread and reports storage and Keystore failures
as `SecureStorageException` (BUG-05) (`:30-34`, `:287`):

| Operation | What it does | Source |
| :- | :- | :- |
| State | A set-up v2 profile → `Configured` (question, whether `biometric_wrap` exists). Leftover v1.0.x files next to a v2 profile are deleted, because an upgrade was interrupted after it saved. Otherwise a v1.0.x profile → `Configured` with its question. Otherwise `NeedsSetup`. | `:60-75` |
| Setup | Refuses when any profile exists. Deletes orphan database files and v1.0.x files, then writes a new profile around a new random key. The database itself is created at the first sign-in. After setup the sign-in screen resets the currency to the region's default and starts onboarding. | `:77-89`; `app:data/local/preferences/SecurityProfileService.kt:126-139`, `:235-246`; `app:presentation/ui/auth/LoginViewModel.kt:233`, `:235` |
| Sign-in with PIN | Unwraps the PIN slot under the lockout and unlocks. With only a v1.0.x profile, verifies the v1 PIN instead and starts the upgrade (section 4). | `:91-104` |
| Recover PIN | Checks, in order: profile set up, not locked out, new PIN format, confirmation, answer not empty. Then unwraps the answer slot under the lockout and re-wraps the key under the new PIN. The answer slot itself isn't changed. | `:106-117`; `app:data/local/preferences/SecurityProfileService.kt:158-183` |
| Unlock | Deletes a database file that starts with the plaintext SQLite header (`SQLite format 3\0`), left by a build from before SQLCipher. Opens the database with the key, seeds the defaults (section 6), and keeps a copy of the key in memory only while unlocked. | `:256-268`; `app:data/local/db/security/DatabaseKeys.kt:16`, `:38-46` |
| Turn on biometric sign-in | Needs an unlocked vault. Deletes and re-creates the biometric key, has `BiometricPrompt` authorize an encrypt cipher, then stores `cipher.iv || cipher.doFinal(databaseKey)` as `biometric_wrap`. | `:165-185`; `app:data/local/vault/KeystoreSecrets.kt:66-70` |
| Biometric sign-in | Builds a decrypt cipher from the first 12 bytes of `biometric_wrap`. If the key is gone or was invalidated, turns biometric sign-in off (`Invalidated`). After the prompt, decrypts the rest. A failure turns it off (`BiometricFailed`); a success resets the lockout and unlocks. | `:131-162`; `app:data/local/vault/KeystoreSecrets.kt:73-80` |
| Turn off biometric sign-in | Removes `biometric_wrap` and deletes the biometric key | `:187`, `:270-273` |
| Lock | Zeroes the in-memory key and any pending upgrade, then closes the database, which zeroes the SQLCipher passphrase | `:190-198`; `app:data/local/db/DatabaseHolder.kt:39-44`, `:71-74` |
| Wipe (CS-17) | Locks, then runs every step even if one fails: delete the database files, delete `vault_profile`, delete both v1.0.x files and the master key, delete the three vault keys, and empty and re-create the cache folder. Reports success only if every step succeeded. `ui_prefs` is kept. | `:201-219`, `:275-279`; `app:data/local/vault/KeystoreSecrets.kt:85` |

## 4. The v1.0.x profile and its upgrade

Builds before commit `9f967c6` (2026-10-03), which introduced the v2 profile, kept the profile in two `EncryptedSharedPreferences` files, which
`androidx.security:security-crypto` 1.1.0-alpha06 encrypts with the master key `_androidx_security_master_key_`: AES-256-SIV
for the entry names and AES-256-GCM for the values (`app:data/local/vault/LegacyProfileStorage.kt:52-67`;
`gradle/libs.versions.toml:22`). The library stores its own Tink keysets (Tink is Google's cryptography library) inside each
file, which is library behavior rather than code in this repository. The rewrite can read these files only through the same
library, so this path is the one reason the Kotlin layer would keep `security-crypto`.

The entries once decrypted (`app:data/local/preferences/LegacySecurityProfile.kt:44-54`, `app:data/local/vault/LegacyProfileStorage.kt:82`):

| File | Entry | Type | Meaning |
| :- | :- | :- | :- |
| `secure_prefs` | `key_profile_configured` | boolean | Set-up flag |
| `secure_prefs` | `key_pin_hash`, `key_pin_salt` | string, Base64 | `PBKDF2-HMAC-SHA256(UTF-8(pin), salt, iterations, 32 bytes)` and its salt |
| `secure_prefs` | `key_pin_iterations` | int | Accepted from 10,000 to 1,000,000 |
| `secure_prefs` | `key_security_question` | string | The question |
| `secure_prefs` | `key_security_answer_hash`, `key_security_answer_salt`, `key_security_answer_iterations` | as above | The answer, trimmed only, so it is case-sensitive |
| `db_key_prefs` | `db_key_hex` | string | The database key as 64 lowercase hex digits |

The upgrade (`app:data/local/vault/Vault.kt:91-129`, `:221-254`):

1. The v1 profile counts only when the flag is true, the question isn't blank, and both hashes have a salt and an iteration
   count within bounds (`app:data/local/preferences/LegacySecurityProfile.kt:17-20`, `:30-32`).
2. Sign-in compares the PIN's PBKDF2 hash in constant time (`MessageDigest.isEqual`), under the shared lockout stored in the
   new `vault_profile` (`app:data/local/preferences/LegacySecurityProfile.kt:34-40`; `app:data/local/vault/Vault.kt:98-101`).
   Recovery instead checks the new PIN's format and confirmation and that the answer isn't blank, then compares the trimmed
   answer's hash (`app:data/local/vault/Vault.kt:221-242`).
3. On success the vault takes the v1 database key. If that key is missing or malformed, it deletes the database files and
   creates a new random key, as v1 itself did (`app:data/local/vault/Vault.kt:244-254`).
4. The key and the PIN (from sign-in) or the new PIN (from recovery) wait in memory until the user enters a new security
   question and answer (`UpgradeRequired`).
5. `completeUpgrade` validates them with the section 3 rules, writes a v2 profile around the same database key, deletes both
   v1 files and the master key, and unlocks (`app:data/local/vault/Vault.kt:120-129`; `app:data/local/vault/LegacyProfileStorage.kt:43-47`).

Locking before step 5 discards the pending upgrade, and the v1 files stay until a later attempt completes it
(`app:data/local/vault/Vault.kt:190-196`).

## 5. SQLCipher

- **Library:** `net.zetetic:sqlcipher-android` 4.19.1 under Room 2.8.5 (`gradle/libs.versions.toml:21`, `:23`, `:78`).
  It holds SQLCipher 4.19.0: `PRAGMA cipher_version` returns `4.19.0 community` (an API 36 emulator, 2026-11-23).
- **Key syntax:** the ASCII bytes of `x'<64 lowercase hex digits of the database key>'`. This "raw key" form makes SQLCipher
  use the 32 bytes as the encryption key directly, with no PBKDF2 step (`app:data/local/db/security/DatabaseKeys.kt:29`,
  `:35-36`). The byte array goes to `SupportOpenHelperFactory` (`app:data/local/db/DatabaseHolder.kt:53-56`).
- **Cipher settings:** the code runs no `PRAGMA` and sets no `cipher_*` option, so SQLCipher 4's defaults apply. SQLCipher's
  documentation gives them as a 4,096-byte page, HMAC-SHA512 page authentication, PBKDF2-HMAC-SHA512 as the key derivation
  (unused for the encryption key under a raw key), no plaintext header, and a random 16-byte salt at the start of the file.
  These defaults come from SQLCipher, not from this repository; fixture F3 must confirm them. A smaller file that v1's
  code wrote on an API 36 emulator in M2.1 (`rust/crates/store/tests/fixtures/`) already shows the page size, both
  algorithms, and no plaintext header, and that file was in write-ahead-log mode.
- **Open:** one worker thread opens, closes, and deletes, so those steps never overlap. Opening forces the writable database,
  which creates or migrates it, and closes it again if that fails (`app:data/local/db/DatabaseHolder.kt:33-34`, `:52-68`).
- **Close:** closing zeroes the passphrase array, which SQLCipher holds by reference, so a stray query fails instead of
  reopening the database (`app:data/local/db/DatabaseHolder.kt:23-27`, `:39-44`).
- **Delete:** `Context.deleteDatabase`, which also removes the `-wal`, `-shm`, and `-journal` files (Android behavior)
  (`app:data/local/db/DatabaseHolder.kt:77-83`).
- **Locked access:** reading `DatabaseHolder.database` while locked throws (`app:data/local/db/DatabaseHolder.kt:48-49`).

## 6. Database schema (Room version 2)

### Tables

The schema below is what Room exported at version 2 (`app/schemas/com.denariidolor.data.local.db.AppDatabase/2.json:4-5`,
`app:data/local/db/AppDatabase.kt:21-25`). Room keeps the schema version in SQLite's `PRAGMA user_version` (Room behavior).

```sql
CREATE TABLE `transactions` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `type` TEXT NOT NULL,
  `description` TEXT NOT NULL,
  `amountCents` INTEGER NOT NULL,
  `categoryId` INTEGER NOT NULL,
  `accountId` INTEGER NOT NULL,
  `transferAccountId` INTEGER,
  `dateEpochMillis` INTEGER NOT NULL,
  `createdAtEpochMillis` INTEGER NOT NULL,
  FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT ,
  FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT );
CREATE INDEX `index_transactions_categoryId` ON `transactions` (`categoryId`);
CREATE INDEX `index_transactions_accountId` ON `transactions` (`accountId`);
CREATE INDEX `index_transactions_dateEpochMillis` ON `transactions` (`dateEpochMillis`);

CREATE TABLE `categories` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `name` TEXT NOT NULL,
  `iconName` TEXT NOT NULL);
CREATE UNIQUE INDEX `index_categories_name` ON `categories` (`name`);

CREATE TABLE `budgets` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `categoryId` INTEGER NOT NULL,
  `monthlyLimitCents` INTEGER NOT NULL,
  `warningThresholdPercent` INTEGER NOT NULL,
  FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE UNIQUE INDEX `index_budgets_categoryId` ON `budgets` (`categoryId`);

CREATE TABLE `accounts` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `name` TEXT NOT NULL,
  `balanceCents` INTEGER NOT NULL);
CREATE UNIQUE INDEX `index_accounts_name` ON `accounts` (`name`);

CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT);
INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '425274f1a5ac9232a36cc07f4f479e7c');
```

- The export writes each `CREATE` with `IF NOT EXISTS` and the placeholder `${TABLE_NAME}`; the statements above fill in the
  names (`app/schemas/com.denariidolor.data.local.db.AppDatabase/2.json:9`, `:128`, `:170`, `:230`, `:273-274`).
- No column has an SQL `DEFAULT`, there are no triggers, and there are no views. Kotlin defaults such as
  `warningThresholdPercent = 80` and `iconName = "ic_category_default"` apply only to rows the app builds
  (`app:data/local/db/entity/BudgetEntity.kt:30`, `app:data/local/db/entity/CategoryEntity.kt:17`).
- `transferAccountId` has no foreign key, so an account delete can leave a transfer pointing nowhere. Reads map a `TRANSFER`
  row without a destination to "no transaction" with no balance impact (BUG-06)
  (`app:domain/model/TransactionEntityMappings.kt:25-32`).
- `AUTOINCREMENT` means SQLite also keeps a `sqlite_sequence` table, so a deleted row's ID is never handed out again.
- The unique name indexes use SQLite's default binary collation, so they are case-sensitive. The use cases add a
  case-insensitive duplicate check on top (`app:data/local/db/dao/AccountDao.kt:33`).
- Room turns on `PRAGMA foreign_keys = ON` each time it opens a database with foreign keys (Room's generated code, not in this
  repository). The Rust port must turn it on too, or `RESTRICT` and `CASCADE` stop applying.

### Column encodings

| Column | Encoding | Source |
| :- | :- | :- |
| `transactions.type` | The enum name: `EXPENSE`, `INCOME`, or `TRANSFER` | `shared:domain/model/TransactionType.kt:9-13` |
| `transactions.amountCents` | A positive count of cents; the sign comes from the type | `app:presentation/ui/TransactionFormScreen.kt:304` |
| `transactions.description` | Trimmed text | `app:domain/model/TransactionEntityMappings.kt:34-38` |
| `transactions.transferAccountId` | The destination account for `TRANSFER`; `NULL` for the other types | `app:domain/model/TransactionEntityMappings.kt:42` |
| `transactions.dateEpochMillis` | Milliseconds since 1970-01-01 UTC. A date the user picks is stored as the first moment of that day in the phone's time zone at saving; a blank date stores the current moment. | `app:presentation/ui/TransactionFormScreen.kt:316-326`; `shared:util/DateUtils.kt:19-20`, `:37-38`; `app:presentation/ui/transaction/TransactionViewModel.kt:148` |
| `transactions.createdAtEpochMillis` | Milliseconds since 1970-01-01 UTC at insert; kept on update | `app:data/local/db/entity/TransactionEntity.kt:42`; `app:data/repository/TransactionRepository.kt:53` |
| `accounts.balanceCents` | The current balance in cents: the opening balance plus every transaction's impact. The opening balance is stored nowhere else. | `app:domain/usecase/AccountUseCases.kt:19-21`; `app:data/backup/BackupStore.kt:33-35` |
| `budgets.monthlyLimitCents` | Cents | `app:data/local/db/entity/BudgetEntity.kt:29` |
| `budgets.warningThresholdPercent` | A whole percent | `app:data/local/db/entity/BudgetEntity.kt:30` |
| `categories.iconName` | An icon key from the list below | `app:presentation/ui/common/CategoryIcons.kt:54-80` |

Stored balances change only inside the database transaction that writes the transaction
(`app:data/repository/TransactionRepository.kt:45-63`, `:92-96`). Each change runs
`UPDATE accounts SET balanceCents = balanceCents + :deltaCents WHERE id = :id`, and the write fails unless exactly one row
changed (`app:data/local/db/dao/AccountDao.kt:39-40`). The deltas come from `Ledger.balanceDeltas(previous, current)`:

- an expense subtracts its amount from its account;
- an income adds its amount to its account;
- a transfer subtracts from `accountId` and adds to `transferAccountId`.

Sources: `shared:domain/model/Ledger.kt:11-16`, `shared:domain/model/Expense.kt:19`, `shared:domain/model/Income.kt:19`,
`shared:domain/model/Transfer.kt:20-22`.

### Category icon keys

`ic_category_default`, `income`, `transfer`, `groceries`, `dining`, `coffee`, `housing`, `utilities`, `internet`, `phone`,
`car`, `fuel`, `transit`, `travel`, `health`, `fitness`, `shopping`, `clothing`, `entertainment`, `education`, `pets`,
`gifts`, `savings`, `bank` (`app:presentation/ui/common/CategoryIcons.kt:54-80`). An unknown key shows the default icon
(`:87`), so the stored value must be kept even when the rewrite doesn't know it.

### Seeded defaults

After every unlock, inside one transaction, the app inserts the default accounts if `accounts` is empty and the default
categories if `categories` is empty, ignoring conflicts (`app:data/local/db/DefaultDataInitializer.kt:26-55`). Names are
resolved in the phone's language at that moment and are ordinary data afterward (`:19-21`). The use cases refuse to delete
these rows.

| Table | ID | Name (English resource) | Other | Source |
| :- | :- | :- | :- | :- |
| `accounts` | 1 | Cash (`default_account_cash`) | balance 0 | `app:util/Constants.kt:15`, `:25` |
| `accounts` | 2 | Savings (`default_account_savings`) | balance 0 | `app:util/Constants.kt:16`, `:26` |
| `categories` | 1 | General Expense (`default_category_expense`) | icon `ic_category_default` | `app:util/Constants.kt:17`, `:30` |
| `categories` | 2 | General Income (`default_category_income`) | icon `income` | `app:util/Constants.kt:18`, `:31` |
| `categories` | 3 | Transfer (`default_category_transfer`) | icon `transfer` | `app:util/Constants.kt:19`, `:32` |

The English names are in `app/src/main/res/values/strings.xml:268-272`.

### Version 1 and the 1→2 migration

Version 1 (identity hash `15f1c4acb81d97b124713b69e7ec869d`,
`app/schemas/com.denariidolor.data.local.db.AppDatabase/1.json:5`) had the same tables, but stored money as `REAL` dollars in
`accounts.balance`, `budgets.monthlyLimit`, and `transactions.amount`. `MIGRATION_1_2` rebuilds each table under its version
2 definition, copies the rows with `CAST(ROUND(<column> * 100) AS INTEGER)`, drops the old table, renames the new one, and
re-creates its indexes (`app:data/local/db/Migrations.kt:12-70`). Tables are rebuilt because `ALTER TABLE … DROP COLUMN`
needs SQLite 3.35 or later. Room runs migrations before it turns foreign keys on, so the parent tables can be swapped
(`:17-18`). A phone that hasn't opened the app since version 1 may still hold a version 1 database.

## 7. Backup files (CS-28)

### Envelope

A backup is a 40-byte header followed by the AES-256-GCM sealed payload (`app:data/backup/BackupFile.kt:27-41`, `:60-68`):

```text
offset  size  field
0       4     magic: ASCII "DDBK"
4       4     format version: signed 32-bit, big-endian
8       4     PBKDF2 iterations: signed 32-bit, big-endian
12      16    salt
28      12    GCM nonce
40      n     ciphertext of the payload
40+n    16    GCM tag
```

- **Key:** `PBKDF2-HMAC-SHA256(UTF-8(NFC(passphrase)), salt, iterations, 32 bytes)`. NFC is Unicode Normalization Form C, so
  the same passphrase typed with precomposed or combining accents gives the same key (`app:data/backup/BackupFile.kt:150-166`).
- **Associated data:** the whole 40-byte header, so a changed header fails like a wrong passphrase (`:95`, `:127`).
- **This key isn't tied to the device**, because a backup has to open on a new phone (`:38-40`).
- **Values written:** format version 1, 600,000 iterations, and new random bytes for the salt and nonce (`:43`, `:53`, `:86-92`).
- **File name and type:** `denarii-dolor-backup-<YYYY-MM-DD>.ddbackup`, with the date in the phone's time zone, saved as
  `application/octet-stream` where the user picks (`app:presentation/ui/backup/BackupViewModel.kt:94`, `:178`;
  `app:data/backup/BackupFile.kt:44-45`; `app:data/backup/BackupManager.kt:68-73`).

| Limit | Value | Source |
| :- | :- | :- |
| Passphrase length | 12–256 code points after NFC, not blank | `app:data/backup/BackupFile.kt:46-47`, `:72-80` |
| Iterations accepted on read | 100,000–2,000,000 | `app:data/backup/BackupFile.kt:57-58`, `:143` |
| Largest file accepted | 32 MiB (33,554,432 bytes) | `app:data/backup/BackupFile.kt:50`, `:113-114` |

### Format versions

Only one version exists: **`FORMAT_VERSION = 1`** (`app:data/backup/BackupFile.kt:43`), introduced in commit `c6c64ce`
(2026-10-04). Version 1 was first written with `java.io.DataOutputStream` and is now written by `BigEndianWriter`, which must
produce the same bytes (`app:data/backup/BigEndianData.kt:9-10`).

Errors when reading, checked in this order (`app:data/backup/BackupFile.kt:14-23`, `:106-147`):

| Check | Error |
| :- | :- |
| Fewer than 40 bytes, or the magic isn't `DDBK`, or the file is larger than 32 MiB | `NOT_A_BACKUP` |
| Version greater than 1 | `NEWER_VERSION` |
| Version less than 1 | `DAMAGED` |
| Iterations outside 100,000–2,000,000 | `DAMAGED` |
| Fewer than 56 bytes (header plus tag) | `DAMAGED` |
| GCM authentication fails | `WRONG_PASSPHRASE` (a wrong passphrase and a changed file look the same) |
| The payload doesn't decode, or fails the consistency check | `DAMAGED` |
| The file couldn't be read from its provider | `READ_FAILED` (`app:data/backup/BackupManager.kt:88-97`) |

### Payload, format version 1

The decrypted payload is a sequence of values with no padding (`app:data/backup/BackupCodec.kt:9-100`):

```text
i64   createdAtEpochMillis
utf   currencyCode                      ISO 4217
i32   accountCount                      then accountCount times:
        i64 id, utf name, i64 balanceCents
i32   categoryCount                     then categoryCount times:
        i64 id, utf name, utf iconName
i32   budgetCount                       then budgetCount times:
        i64 id, i64 categoryId, i64 monthlyLimitCents, i32 warningThresholdPercent
i32   transactionCount                  then transactionCount times:
        i64 id
        utf type                        EXPENSE | INCOME | TRANSFER
        utf description
        i64 amountCents
        i64 categoryId
        i64 accountId
        u8  hasTransfer                 written as 1 or 0; any nonzero byte reads as true
        i64 transferAccountId           present only when hasTransfer is true
        i64 dateEpochMillis
        i64 createdAtEpochMillis
(end)                                   no bytes may follow
```

- `i32` and `i64` are big-endian two's complement (`app:data/backup/BigEndianData.kt:12`).
- A negative count is `DAMAGED`. The reader reserves no memory from a count, so a damaged count runs into the end of the
  data instead of allocating (`app:data/backup/BackupCodec.kt:93-100`).
- Leftover bytes after the last transaction are `DAMAGED` (`app:data/backup/BackupCodec.kt:57`).
- Rows are written in ID order, read from all four tables in one database transaction (`app:data/backup/BackupStore.kt:21-30`;
  `app:data/local/db/dao/BackupDao.kt:21-31`).

`utf` is Java's modified UTF-8, as `DataOutput.writeUTF` writes it (`app:data/backup/BigEndianData.kt:13-15`, `:56-77`):

```text
u16   byteLength                        at most 65,535; longer text can't be written
...   each UTF-16 code unit on its own:
        U+0001–U+007F   1 byte   0xxxxxxx
        U+0000, U+0080–U+07FF   2 bytes  110xxxxx 10xxxxxx          (U+0000 is C0 80)
        U+0800–U+FFFF   3 bytes  1110xxxx 10xxxxxx 10xxxxxx
      a character outside the Basic Multilingual Plane is two 3-byte surrogates, never one 4-byte sequence
```

The reader accepts exactly what `DataInput.readUTF` accepts and no more: overlong sequences such as `C0 80`, and unpaired
surrogates, read as they do in Java. A bad lead or continuation byte, or a sequence cut off by the end of the text, is
`DAMAGED` (`app:data/backup/BigEndianData.kt:144-176`).

### Consistency check and restore

A snapshot is checked both before a backup is written and after one is read (`app:data/backup/BackupManager.kt:61-66`,
`:75-79`). `isConsistent` passes when all of these hold (`shared:data/backup/BackupSnapshot.kt:42-67`):

1. Every ID is positive.
2. IDs are unique within each table.
3. Account names are unique, and category names are unique (exact comparison).
4. Accounts 1 and 2 and categories 1, 2, and 3 are present.
5. Each budget has a unique category, and that category is in the snapshot.
6. Every transaction has a known type, and its category and account are in the snapshot.

The check doesn't repeat business rules such as description length, because rows from before a rule existed must still
restore. It doesn't check `transferAccountId` either, because the schema has no foreign key there.

Restore replaces all data in one database transaction (`app:data/backup/BackupStore.kt:32-56`):

1. Delete every transaction, then every budget, then every category, then every account (children before parents).
2. Insert the accounts, then the categories, the budgets, and the transactions, each with its original ID and with conflicts
   aborting (`app:data/local/db/dao/BackupDao.kt:45-52`).
3. Copy stored balances as they are; never recompute them, because opening balances live only in `balanceCents`.
4. After the transaction commits, apply the backup's currency if this build supports it (`app:data/backup/BackupManager.kt:81-86`).

If any step fails, the transaction rolls back and the database stays as it was. The security profile, the PIN, the
question, and biometric sign-in are never in a backup (`shared:data/backup/BackupSnapshot.kt:32`).

## 8. Exports

Exports are user-visible formats that people may feed to spreadsheets or scripts, so their shape stays stable.

**File names** are `spending-report-<YYYY-MM>.csv` and `spending-report-<YYYY-MM>.pdf`, with MIME types `text/csv` and
`application/pdf` (`shared:domain/report/ReportText.kt:23`; `app:data/export/ReportExporter.kt:27-30`).

**The rows** are the month's transactions in the phone's time zone, sorted by date and then ID. The month runs from the first
moment of its first day to one millisecond before the next month starts
(`app:domain/usecase/GenerateReportUseCase.kt:32-62`; `shared:util/DateUtils.kt:22-27`).

**CSV** (`shared:domain/report/ReportCsvFormatter.kt:15-67`; `app:data/export/ReportExporter.kt:63-67`):

- UTF-8 with no byte order mark, and CRLF line endings.
- First six key-value rows, then one empty line, then the table:

  ```text
  title,<translated "Monthly Spending Report">
  period,<month and year in the phone's language, pattern "MMMM yyyy">
  generated_at,<yyyy-MM-dd HH:mm z, US English, in the phone's zone>
  total_income,<plain amount>
  total_expense,<plain amount>
  net,<plain amount>

  date,type,category,description,amount,payment_method
  <YYYY-MM-DD>,<EXPENSE|INCOME|TRANSFER>,<category name>,<description>,<plain amount>,<account, or "source → destination">
  ```

- A plain amount is an optional `-`, the whole units, `.`, and exactly two digits, in ASCII digits (`shared:util/Money.kt:148-153`).
- The column keys and type names stay in English in every language; only the title and the period are translated
  (`app:data/export/ReportExporter.kt:63`).
- A transfer's payment method is `source → destination`, or `source ← destination` in a right-to-left language
  (`shared:util/TextDirection.kt:19-20`). A missing category or account name shows as `#<id>`
  (`app:domain/usecase/GenerateReportUseCase.kt:39`, `:47`).
- Quoting follows RFC 4180. Any cell that a spreadsheet would run as a formula, at its start, after leading whitespace, or after
  a `;` or tab inside it, gets an apostrophe in front, unless it reads as a number (CS-20)
  (`shared:domain/report/ReportCsvFormatter.kt:16-21`, `:45-62`).
- The period and generated-at labels come from `shared/src/androidMain/kotlin/com/denariidolor/util/PlatformFormat.android.kt:39-45`.

**PDF:** US Letter pages (612 × 792 points) with 36-point margins, the same six columns under translated headings, translated
type names, and page numbers (`app:data/export/ReportPdfRenderer.kt:48-53`; `app:data/export/ReportExporter.kt:72-87`).

**Sharing** writes the file to `cache/reports/`, emptied first, and shares a read-only `FileProvider` link (CS-04)
(`app:data/export/ReportExporter.kt:52-61`).

## 9. Session timeout and lock

- A signed-in session ends after 5 minutes without user interaction (`app:util/Constants.kt:14`).
- Time comes from `SystemClock.elapsedRealtime()`, so changing the clock can't extend a session. The session state is in
  memory only, so a process restart means signed out (`app:util/SessionManager.kt:14-41`).
- Sign-in marks the session authenticated (`app:presentation/ui/auth/LoginActivity.kt:122`).
- The main screen checks for timeout when it starts (also after the system restores the task), when it resumes, every
  30 seconds while visible, and on every user interaction. An interaction that comes in time extends the session
  (`app:MainActivity.kt:45-48`, `:58-85`).
- On timeout or sign-out the app locks the vault, ends the session, empties `cache/reports/`, and returns to sign-in,
  clearing the task (`app:MainActivity.kt:93-100`).

## Open questions and contradictions

None of these block the format description above, but each one needs a decision or a device check before the Rust port
reads real data.

1. **Short v1.0.x PINs.** Before commit `f8bf31e` (2026-09-21) the PIN rule was `^[0-9]{4,12}$`. `completeUpgrade` checks the
   PIN with today's 6–12 digit rule, so an install upgrading from a build before that commit with a 4- or 5-digit PIN gets
   `INVALID_PIN_FORMAT` and can finish only through PIN recovery (`app:data/local/vault/Vault.kt:120-122`;
   `app:data/local/preferences/SecurityProfileService.kt:143-147`). The repository has no release tags, so it doesn't say
   whether such a build reached Play.
2. **`\s` on Android.** Answer normalization ran Android's ICU-based `java.util.regex`, whose `\s` may match a different set
   (for example, other Unicode separators, or not the vertical tab) than the desktop JDK the unit tests use. Most Unicode
   spaces become U+0020 under NFKC first, but U+2028, U+2029, U+1680, and U+000B need checking. Fixture F7 settles it.
3. **Unicode versions.** NFKC and lowercase mapping came from each phone's ICU version (API 26 is the oldest). A Rust
   normalization library may implement a newer Unicode version. Answers that contain characters whose mapping changed
   between versions could stop matching. Fixture F7 covers the oldest and newest API levels.
4. **Unpaired surrogates.** Kotlin strings, SQLite text written by Android, and modified UTF-8 can all hold a lone UTF-16
   surrogate. A Rust `String` can't. The port needs a stated policy (keep the raw bytes, or replace with U+FFFD and report
   it), with a fixture.
5. **Keeping the v1.0.x path.** Reading `secure_prefs` and `db_key_prefs` needs `androidx.security:security-crypto` in the
   Kotlin layer. Whether any v1.0.x install remains un-upgraded is unknown; that is a decision for the maintainer.
6. **SQLCipher defaults.** The code relies on SQLCipher 4's defaults without naming them. The Rust build must link a
   SQLCipher 4 that keeps those defaults, or set them explicitly; fixture F3 checks either way. `denarii-store` sets them
   explicitly with `PRAGMA cipher_compatibility = 4` (`docs/specs/store.md`).
7. **Profile corruption reads as "no profile".** An out-of-range iteration count or a blank wrap makes the vault offer setup,
   and setup deletes the database (`app:data/local/preferences/SecurityProfileService.kt:111-117`;
   `app:data/local/vault/Vault.kt:83-84`). That is today's behavior; the rewrite must not delete data in more cases than this.

No contradictions between the code and its comments were found. The one inconsistency is in the setup rules: answer length is
counted in code points for the minimum and in UTF-16 code units for the maximum (section 3).

## Invariants

The rewrite must keep each of these. Each names how it can be tested.

1. The Android package is `com.denariidolor`, signed with the existing key, with a version code above the last one on Play.
   Test: the release check in CD compares the signing certificate and version code.
2. The Kotlin layer uses the aliases `denarii_vault_storage`, `denarii_vault_device`, and `denarii_vault_biometric` in
   `AndroidKeyStore`, with the parameters in section 2, and never re-creates the storage or device key while a profile
   exists. Test: F5.
3. `vault_profile.xml`'s `profile` entry decrypts under the storage key with associated data `denarii-dolor:profile:v2`, and
   every JSON key in section 3 keeps its name, type, and meaning. Test: F5.
4. A PIN or answer opens its slot by
   `HMAC-SHA256(device key, context || PBKDF2-HMAC-SHA256(UTF-8(secret), salt, iterations, 32))`, then AES-256-GCM with the
   slot context as associated data. Stored iteration counts from 10,000 to 1,000,000 are honored. Test: F1 for the
   primitives, F5 end to end.
5. Answers normalize exactly as section 3 describes, character for character, on every supported API level. Test: F7.
6. The lockout schedule, its shared counter, the monotonic timer, the boot-count restart, and the reset on success match
   section 3. Test: a unit test over a fake clock, plus F5 for the stored keys.
7. `biometric_wrap` is `iv[12] || ciphertext` with no associated data, and an invalidated key turns biometric sign-in off
   instead of failing sign-in. Test: F5 on a device with an enrolled fingerprint.
8. The database opens with the raw key `x'<64 lowercase hex>'` and SQLCipher 4 settings, with foreign keys on. Test: F3.
9. A version 2 database opens without changing any row, and a version 1 database migrates to version 2 with
   `ROUND(x * 100)` cents. Later schema versions continue from `user_version` 2. Test: F3 and F4.
10. Stored balances change only in the same database transaction as the transaction row, by `Ledger.balanceDeltas`. Test: a
    Rust unit test over the four transaction cases (add, edit, delete, edit that changes the type or accounts).
11. A malformed `TRANSFER` row (no destination) never crashes a read and has no balance impact (BUG-06). Test: F3 includes
    one.
12. Every backup with `FORMAT_VERSION` 1 restores, byte layout and modified UTF-8 included, and a later format still reads
    version 1. Test: F2.
13. Backup errors keep their meaning and order: `NOT_A_BACKUP`, `NEWER_VERSION`, `DAMAGED`, `WRONG_PASSPHRASE`,
    `READ_FAILED`. Test: truncated, oversized, altered-header, wrong-passphrase, and extra-byte cases built from F2.
14. Restore replaces all four tables in one transaction, keeps IDs and stored balances, and leaves the database unchanged
    when it fails. Test: F2 restored over a seeded database, then a failing restore.
15. CSV exports keep their file names, keys, English type names, plain amounts, CRLF, and formula guard (CS-20). Test: F8.
16. Wipe deletes the database files, the profile, both v1.0.x files, the master key, the three vault keys, and the cache, and
    keeps `ui_prefs`. Test: an instrumented test that lists the data directory and Keystore aliases afterward.
17. The session ends after 5 minutes without interaction, timed on the monotonic clock, and ending it locks the vault and
    empties `cache/reports/`. Test: a unit test over a fake monotonic clock.
18. Data in `ui_prefs` keeps its file name and keys, and a missing key keeps its default. Test: F6.

## Tests

The Rust port and the Kotlin layer need these fixtures. Fixtures made on a device must come from a build of commit
`6d57526`, so they capture exactly what users' phones hold. Fixtures hold test values only; no real PIN, passphrase, or key
appears in this document.

| ID | Fixture | How to make it | Checks |
| :- | :- | :- | :- |
| F1 | Crypto known answers: PBKDF2 (including non-ASCII secrets), AES-256-GCM with the PIN slot's associated data, and NFC and NFKC samples | Reuse the vectors in `app/src/test/java/com/denariidolor/data/local/preferences/CryptoKnownAnswerTest.kt:98-145` | Invariant 4 primitives |
| F2 | A format version 1 backup file and its decrypted payload, including an emoji stored as two surrogates | Reuse `app/src/test/java/com/denariidolor/data/backup/BackupGoldenFiles.kt:20-50` and `BackupSamples.kt:13`. Never edit them. | Invariants 12–14 |
| F3 | A version 2 SQLCipher database written by the shipped app, with a known test key, every transaction type, one malformed transfer, a non-ASCII name, an emoji, and a budget | An instrumented test at `6d57526` on an emulator, exporting the file and its key | Invariants 8, 9, 11 |
| F4 | A version 1 database with `REAL` amounts, including values that round, such as 0.005 and 19.995 | Room's migration test helper, as `app/src/androidTest/java/com/denariidolor/data/MigrationTest.kt:27` does | Invariant 9 |
| F5 | An in-place upgrade on a device: install the `6d57526` build, set up a profile, add data, turn on biometric sign-in, fail the PIN a few times, then install the new build over it with the same signing key and sign in with the PIN, the answer, and a fingerprint | A manual or UiAutomator run on API 26 and API 36 emulators | Invariants 2–4, 6, 7 |
| F6 | A `ui_prefs.xml` with every key set, and one with none | Copy from the F5 device | Invariant 18 |
| F7 | Answer normalization vectors computed on the device: NBSP, U+3000, U+2028, U+2029, U+1680, U+000B, tabs, Turkish dotted and dotless I, a word-final Greek sigma, fullwidth letters, and combining accents | An instrumented test at `6d57526` on API 26 and API 36 that records `normalizeAnswer` output | Invariant 5 and open questions 2–3 |
| F8 | A CSV export with a transfer, a formula-like description, and a right-to-left locale | Extend `shared/src/commonTest/kotlin/com/denariidolor/domain/report/ReportFormattingTest.kt:56` to write golden output | Invariant 15 |
| F9 | A v1.0.x install with `secure_prefs` and `db_key_prefs` | A build of commit `9f967c6^` on an emulator, then the F5 steps | Section 4, if the v1.0.x path is kept |
