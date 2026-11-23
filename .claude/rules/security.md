---
paths:
  - "app/src/main/java/com/denariidolor/presentation/ui/auth/**"
  - "app/src/main/java/com/denariidolor/presentation/ui/LoginScreen.kt"
  - "app/src/main/java/com/denariidolor/presentation/ui/backup/**"
  - "app/src/main/java/com/denariidolor/data/local/**"
  - "app/src/main/java/com/denariidolor/data/backup/**"
  - "app/src/main/java/com/denariidolor/data/export/**"
  - "shared/src/*/kotlin/com/denariidolor/data/local/**"
  - "shared/src/*/kotlin/com/denariidolor/data/backup/**"
  - "shared/src/*/kotlin/com/denariidolor/domain/report/ReportCsvFormatter.kt"
  - "app/src/main/java/com/denariidolor/util/SessionManager.kt"
  - "app/src/main/java/com/denariidolor/MainActivity.kt"
  - "app/src/main/java/com/denariidolor/di/**"
  - "app/src/main/AndroidManifest.xml"
  - "app/src/main/res/xml/**"
  - "app/proguard-rules.pro"
  - ".github/**"
  - "gradle/**"
  - "settings.gradle.kts"
  - "build.gradle.kts"
  - "app/build.gradle.kts"
  - "shared/build.gradle.kts"
---

# Security-sensitive code

This file guards users' financial data. Name every change to it in your summary, so the maintainer reviews it. If a
request would weaken a control below, don't make that part of the change: explain the conflict and propose an alternative
that keeps the control.

## Threat model

The app is offline and single-user.

- **Assets:** the SQLCipher database and its key, the PIN, the security answer, backups and exports.
- **Attackers:**
  - someone holding the phone, locked or unlocked;
  - other apps on the device (overlays, task hijacking, crafted intents);
  - whoever gets a copied backup file;
  - the supply chain (Gradle plugins, GitHub Actions).
- **Out of scope:** network attackers, because there is no network. Attackers with root get best effort only (CS-27, CS-A2).

## Key management (CS-15)

- **The database key** is 32 random bytes, passed to SQLCipher as a raw key and never stored in the clear. It is wrapped with
  AES-256-GCM under key-encryption keys:
  - one from the PIN and one from the normalized security answer. Each is PBKDF2-HMAC-SHA256 with 210,000 iterations,
    followed by an HMAC with a non-exportable Keystore device key, so guesses can only be checked on the device;
  - optionally, one under a Keystore key that requires `BIOMETRIC_STRONG` for every use, through a `CryptoObject` (CS-07).
- **The rest of the profile** is one AES-GCM blob under a Keystore storage key (`KeystoreProfileStore`, prefs file
  `vault_profile`).
- **Locking:** `Vault.lock()` closes the database and zeroes the passphrase, which SQLCipher holds by reference.
- **Wipe** is a cryptographic erase (CS-17). It deletes the database files (with `-wal`, `-shm` and the journal), the
  profile, the v1.0.x files and every vault key.
- **Legacy upgrade:** v1.0.x installs (`secure_prefs`, `db_key_prefs`) are upgraded by `LegacyProfileStorage`, the only
  user of `security-crypto`.
- **Primitives:** `SecurityProfileService` and `BackupFile` get PBKDF2-HMAC-SHA256, AES-256-GCM, secure random bytes and
  NFC/NFKC from `CryptoPrimitives`, implemented on Android by `JcaCryptoPrimitives`. Keystore keys stay in `KeystoreSecrets`.
  `CryptoKnownAnswerTest`, `CryptoPrimitivesDeviceTest` and `SecurityProfileGoldenTest` pin the exact bytes, so profiles and
  backups already on phones keep opening; never change their expected values to make a change pass.

Never do any of these:

- lower iteration counts or key sizes;
- switch away from GCM, reuse an IV, or roll your own crypto;
- store a key, PIN or answer in prefs, logs, exceptions or `toString()`;
- keep a secret in a `String` longer than necessary, or skip `fill(0)`;
- add a debug unlock or a test-only bypass to main sources;
- make biometric sign-in event-based, that is, without a `CryptoObject`.

## Controls that must stay

| ID | Control |
|---|---|
| CS-01 | The 5-minute session timeout runs on `SystemClock.elapsedRealtime()`, never the wall clock |
| CS-02 | `MainActivity.onCreate` checks the session and redirects before it builds any UI |
| CS-04 | Share copies in `cache/reports/` are cleared on sign-out, on timeout and at every `LoginActivity` start |
| CS-07 | Biometric sign-in decrypts a key wrap through a `CryptoObject`. Enrolling a new biometric invalidates it, and the PIN still works |
| CS-10 | The PIN is 6–12 digits (`^[0-9]{6,12}$`) for setup, reset **and** sign-in |
| CS-11 | Overlay guard on `LoginActivity`: `setHideOverlayWindows(true)` on API 31+, `filterTouchesWhenObscured` below |
| CS-13 | The lockout escalates, is shared by PIN and answer attempts, and runs on `elapsedRealtime` plus `Settings.Global.BOOT_COUNT`. A reboot restarts the full lockout. No wall-clock input |
| CS-14 | The security answer needs at least 6 normalized characters, and the comparison is normalized |
| CS-16 | Wipe needs the typed word `WIPE` and a 10-second countdown |
| CS-17 | Wipe is a cryptographic erase |
| CS-20 | The CSV formula guard also checks after leading whitespace, `;` and tabs |
| CS-21 | Both activities set `android:taskAffinity=""` |
| CS-28 | Backups: 12–256-character passphrase, encrypted, versioned, validated, atomic restore |
| CS-03, CS-08, CS-19 | Workflows: `persist-credentials: false`; actions pinned to commit SHAs; CD split into `ci-gate`, `build` (secrets, read-only) and `publish` (write, no secrets) |
| Platform | `allowBackup="false"`, and the backup and data-extraction rules exclude everything; only `LoginActivity` is exported; the FileProvider isn't exported and has minimal paths; no `INTERNET` permission |

## Accepted risks

These were decided by the maintainer. Don't "fix" them unasked.

- **CS-A1:** exported CSV and PDF files aren't encrypted once they leave the app.
- **CS-A2:** there is no root, emulator or tamper detection.
- **CS-A3:** biometric sign-in stays available during a PIN lockout. The OS has its own biometric lockout.
- **CS-A4:** there is no `FLAG_SECURE`. It was declined twice.
- **CS-A6:** the strength of a backup passphrase isn't measured.

## Open findings

These need design decisions. Work on them only when asked.

- **CS-12:** there is no audit log of security events.
- **CS-26:** the vault stays unlocked in the background until the timeout.
- **CS-27:** on a rooted device, the PIN and answer can be guessed on the device itself.

## Android platform rules

- A new component is `android:exported="false"` unless it must be reachable from outside. An intent filter means it's
  exported, so justify it.
- Validate every incoming intent extra and URI. Use explicit intents internally and `PendingIntent.FLAG_IMMUTABLE`.
- Add no new permission without asking. Each one changes `PRIVACY.md` and the Play Data safety form.
- Don't add `WebView`, dynamic code loading, reflection driven by input, world-readable modes or external-storage paths.
  Never copy secrets to the clipboard.
- File I/O goes only through Storage Access Framework URIs and app-private directories. Bound the size of anything read from
  a user-picked file and check its format before parsing it (see `BackupFile`).
- Keep the rules in `proguard-rules.pro` narrow. The R8 mapping file is private; it is uploaded only as a CD artifact.

## When you change this area

- **Tests:**
  - Add JVM tests for pure logic (`SecurityProfileServiceTest`, `LegacySecurityProfileTest`, `DatabaseKeysTest`). A change to
    the primitives or the stored profile must keep `CryptoKnownAnswerTest` and `SecurityProfileGoldenTest` passing unchanged.
  - Add instrumented tests for Keystore and SQLCipher behavior. `VaultTest` uses test-only `VaultConfig` names; never touch
    the app's real profile, database or key aliases from a test.
  - Biometric flows need a manual check on a device with an enrolled fingerprint. Say so in your summary.
- **Summary:** state which control is affected, why it still holds, and any new risk.
- **New findings** take the next free ID. The highest used are CS-28 and CS-A6. The full history is in
  `git show aba0b55:intel/cybersec.md`.
