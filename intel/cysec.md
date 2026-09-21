# Denarii Dolor — Cyber Security Log (`cysec.md`)

This is a living record of the app's security issues: what was **identified**, how it is **written up** (risk, location, evidence), and how it was **fixed** or why it remains open. Update it with every change that touches authentication, storage, exports, the build or CI, and after every review. Keep the finding IDs stable and never reuse one.

- **Last review:** 2026-09-21. This was a manual review of the manifest, the auth and session code, storage, exports, the build, and the three GitHub workflows.
- **Standards used:** OWASP MASVS (Storage, Crypto, Auth, Platform, Code, Resilience), NIST SP 800-63B, and NIST SP 800-132.
- **Severity:** **High** means directly exploitable or exposes financial data without device access. **Medium** needs physical access, a compromised dependency or a rooted device. **Low** is defence in depth.

---

## 1. Open findings

| ID | Severity | Area | Finding | Location | Recommended fix | Status |
|---|---|---|---|---|---|---|
| CS-07 | Medium | Auth (MASVS-AUTH) | Biometric sign-in is event-based: `onAuthenticationSucceeded` opens the app without a `CryptoObject`. On a rooted or hooked device the callback can be forged. | `LoginActivity.promptForBiometricSignIn` | Bind biometrics to a Keystore key created with `setUserAuthenticationRequired(true)`, and unlock it through `BiometricPrompt.CryptoObject`. For example, the key could wrap the database key. | Open (deferred in Phase 2) |
| CS-08 | Medium | Supply chain (MASVS-CODE) | Workflow actions are pinned to **mutable tags** (`@v6`, `@v2`, …). That includes third-party actions (`softprops/action-gh-release`, `reactivecircus/android-emulator-runner`, `gitleaks/gitleaks-action`) that run next to signing secrets or with write permission. | `.github/workflows/*.yml` | Pin every `uses:` to a full commit SHA with a `# vX.Y.Z` comment. Dependabot (added under CS-05) will then raise update PRs for the SHAs. | Open. The SHAs must be looked up on GitHub, which this environment can't reach. |
| CS-09 | Low | Crypto (MASVS-CRYPTO) | `androidx.security:security-crypto` (`EncryptedSharedPreferences`, `MasterKey`) is **deprecated** as of 1.1.0. The app uses 1.1.0-alpha06. It still works, but it will get no further fixes. | `EncryptedPreferencesManager`, `DatabaseKeyProvider` | Migrate to a small wrapper that encrypts values with an AES-GCM key held directly in the Android Keystore. Include a one-time migration of the existing values. | Open (planned) |
| CS-11 | Low | Platform (MASVS-PLATFORM) | Tapjacking: nothing hides other apps' overlays while the PIN and wipe-data controls are on screen. Android 12+ already blocks untrusted touches that pass through an overlay; Android 8–11 does not. | `LoginActivity`, `LoginScreen` | On API 31+, call `window.setHideOverlayWindows(true)`, which needs the `HIDE_OVERLAY_WINDOWS` permission. On older versions, ignore touches when the window is obscured. | Open |
| CS-12 | Low | Code (MASVS-CODE) | There is no audit log of security events (sign-ins, lockouts, PIN resets, data wipes). The course's recommended stack lists "audit logs". | — | Add an append-only `audit_events` table in the encrypted database, and a read-only list in Settings. | Open (deferred in Phase 2) |

### Accepted risks

| ID | Risk | Why it's accepted |
|---|---|---|
| CS-A1 | Exported CSV and PDF files are **not encrypted** once they leave the app. | Exporting is an explicit user action, and the user guide and README warn about it. Share copies are now deleted at the end of the session (CS-04). |
| CS-A2 | No root, emulator or tamper detection. | Such checks are easy to bypass and cause false positives. The data is already encrypted at rest, and CS-07 is the stronger fix. |
| CS-A4 | Screens can be captured: screenshots, screen recording and the Recents thumbnail (no `FLAG_SECURE`; this was CS-06). | You decided against it twice (Phase 2 and 2026-09-21). Denarii Dolor is a personal productivity app, not a banking app, and users expect to be able to screenshot their own budgets. |
| CS-A3 | Biometric sign-in stays available during a PIN lockout. | Deliberate, and recorded in `notes.md`: the OS enforces its own biometric lockout, and a successful biometric sign-in resets the PIN counter. |

## 2. Fixed on 2026-09-21

| ID | Severity | Finding | Fix | Files |
|---|---|---|---|---|
| CS-01 | Medium | The session timeout used the **wall clock** (`System.currentTimeMillis()`). Setting the device clock backwards made `now - lastActiveAt` negative, so the session never timed out. | `SessionManager` now uses `SystemClock.elapsedRealtime()`, a monotonic clock that includes deep sleep. The `now` parameters stay, so the unit tests are unchanged. | `util/SessionManager.kt` |
| CS-02 | Low | `MainActivity` built its UI in `onCreate` before checking the session, and only redirected in `onResume`. When Android restored the task after process death, financial content was composed without a signed-in session. | `onCreate` now redirects to sign-in **before** `setContent` when no valid session exists. | `MainActivity.kt` |
| CS-03 | Medium | Every checkout step left `GITHUB_TOKEN` in `.git/config` (`persist-credentials` defaults to true). In the release and dependency-submission jobs, which have `contents: write`, any Gradle plugin or build script could read it. | `persist-credentials: false` on all 7 checkouts. No step pushes with git. | `.github/workflows/ci.yml`, `cd.yml`, `security.yml` |
| CS-04 | Low | Unencrypted report copies made for **Share** stayed in `cache/reports/` until the next share. | `ReportExporter.clearShareCache()` now runs on sign-out, on session timeout (`MainActivity.redirectToLogin`) and on every `LoginActivity` start, which covers cold starts after process death. | `data/export/ReportExporter.kt`, `MainActivity.kt`, `presentation/ui/auth/LoginActivity.kt` |
| CS-10 | Low | PINs could be 4 digits (10 000 combinations), below the NIST SP 800-63B minimum of 6 for numeric secrets. | New PINs, PIN resets **and sign-in** now require 6–12 digits (`^[0-9]{6,12}$`); the error message reads "PIN must be 6 to 12 digits." As you chose, existing 4–5-digit PINs are **not** kept working: those users sign in through **Forgot PIN?** and set a 6-digit PIN. New unit test `pinMustBeSixToTwelveDigits` covers setup with 5, 12 and 13 digits and a non-digit, plus reset with 5 digits. The existing PIN tests now use 6-digit PINs. | `SecurityProfileService.kt`, `LoginActivity.kt`, `strings.xml`, `SecurityProfileServiceTest.kt`, `ComposeScreensTest.kt`, `README.md` |
| CS-05 | Low | Nothing kept GitHub Actions or Gradle dependencies patched automatically. | Added `.github/dependabot.yml` with weekly updates for `github-actions` (grouped) and `gradle`. Each update PR runs through CI and the security workflow. | `.github/dependabot.yml` |

**Verification:**

- ktlint 1.3.1 and detekt 1.23.8 (with the default rule set, as Gradle runs it): 0 findings.
- All workflow YAML parses.
- **Not yet compiled here.** Run `./gradlew testDebugUnitTest connectedDebugAndroidTest assembleRelease`, or push and let CI run it.
- Manual check: sign in, set the device clock back an hour, and wait 5 minutes. You should be signed out.

## 3. Fixed in earlier phases

| Finding | Fix | Phase |
|---|---|---|
| The Room database was plaintext | SQLCipher 4.6.1 with a random 256-bit key held in EncryptedSharedPreferences (`db_key_prefs`) | 2 |
| App data was included in cloud backup and device transfer | `allowBackup=false` plus backup and data-extraction rules that exclude the database, prefs and files | 2 |
| The PIN was stored in plaintext | PBKDF2-HMAC-SHA256 (210 000 iterations, 16-byte salt), constant-time comparison, and the legacy value migrated on setup | 1a/2 |
| Unlimited PIN and security-answer guessing | Shared escalating lockout: 5 failures → 30 s, doubling up to 15 min, persisted and resistant to clock rollback | 2 |
| Weak biometrics were accepted | `BIOMETRIC_STRONG` only | 2 |
| The release build wasn't obfuscated or shrunk | R8 with keep rules | 2 |
| A destructive migration could silently delete data | `fallbackToDestructiveMigration()` removed; explicit `MIGRATION_1_2` with exported schemas | 2/5 |
| CSV formula injection | Cells starting with `= + - @ \t \r` are prefixed with `'` and quoted | 3 |
| Report sharing | FileProvider, not exported, limited to `cache/reports/`, one-time read grant; saving goes through the system file picker (no storage permission) | 3 |
| Money stored as floating point (integrity) | `Long` cents end to end | 5 |
| Security gates in CI | CodeQL `security-extended`, dependency review (fails on high severity), dependency graph submission, gitleaks; ktlint, detekt and lint are blocking | CI |

## 4. Current controls (quick reference)

- **Network:** no `INTERNET` permission, and no network code.
- **Exported components:** only `LoginActivity`, the launcher. `MainActivity` and the FileProvider are not exported.
- **Storage:**
  - `denarii_dolor.db` is encrypted with SQLCipher.
  - `secure_prefs` and `db_key_prefs` are EncryptedSharedPreferences (AES-256-SIV keys, AES-256-GCM values).
  - `ui_prefs` holds only the theme.
- **Queries:** Room `@Query` with bound parameters only.
- **Input validation:** `Validators`, `ValidateTransactionUseCase` and `Money.parseToCents`. See the unit test plan, TC-01 – TC-20.
- **Logging:** no `Log` calls, no `println`, and no `printStackTrace` in `app/src/main`.
- **Release signing:** keystore and passwords come only from CI secrets; the keystore is deleted when the job ends; signatures are checked with apksigner and jarsigner.

## 5. How to update this file

1. Add a new finding under **Open findings** with the next unused `CS-NN` ID. Give its severity, area, location and recommended fix.
2. When it's fixed, move it to the dated **Fixed** section with the fix, the files and how it was verified. Keep the ID.
3. If a finding is accepted rather than fixed, move it to **Accepted risks** with the reason.
4. Update **Last review** at the top, and add a `history.md` entry that links the IDs.
