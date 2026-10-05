# Denarii Dolor — Plan

Active and follow-on work only. Finished work is recorded in `history.md`, decisions and their reasons in `notes.md`, and security findings in `cybersec.md`. Last updated 2026-10-04.

## Requirement Status
| Requirement | Status | Notes |
|---|---|---|
| Inheritance / polymorphism / encapsulation | ✅ Done | `Transaction` → `Expense`/`Income`/`Transfer`, `balanceImpact()`, `accountImpacts()` |
| Search with multi-row results | ✅ Done | Typed multi-row results with count; tap to edit; `%` and `_` match literally |
| Secure DB add / edit / delete | ✅ Done | Full add/edit/delete in UI; SQLCipher database that opens only after sign-in |
| Reports (multi-column, rows, timestamp, title) | ✅ Done | 6 columns, title, period, timestamp, totals; CSV + PDF save/share |
| Validation | ✅ Done | Amount, description (trimmed, at most 200 characters), date, references, budget limit (confirmed, not blocked, when exceeded), duplicate names (category/account) |
| Security | ✅ Done | Database key wrapped by the PIN, the security answer and strong biometrics (`Vault`); elapsed-time lockout; Keystore-encrypted profile; no backups; R8; session timeout; tapjacking guard |
| Scalability | ✅ Done | MVVM, repositories, use cases, Hilt, typed enums, cents, explicit migrations + exported schema, lint/static analysis/coverage in CI |
| GUI | ✅ Done | Bottom nav, FAB, monthly dashboard (hero, tiles, chart, budget meters, alerts), category icons, dark mode, manage screens |

## Phase 7 — Finish the 2026-10-03 remediation

All twelve bugs (BUG-01 – BUG-12) from the 2026-10-03 review are fixed, and every finding in `cybersec.md` except CS-12 has a fix in the code or the workflows. `history.md` (2026-10-03) lists each fix and its tests. What's left needs GitHub, a release tag or a physical device, so it couldn't be finished from the workstation.

1. **Commit and push, then get a green CI run.** Nothing in this round is committed yet. The Compose UI tests can only run on CI: with Espresso 3.6.1 they fail on the local API 37 emulator image (see `maint.md` §6). The first green run also validates:
   - the new instrumented tests: `NavigationTest` (BUG-03), `CrudScreensTest.saveButtonIsDisabledWhileSaving` (BUG-02) and `OverlayGuardTest` (CS-11, which only runs on the API 26 emulator);
   - the API 26 wipe-dialog test. It still failed on `288ff67` and `b36b92e`. The cause was found on 2026-10-03 (the keyboard took the Back press), and the fix is in the working tree;
   - CS-08 (SHA-pinned actions) and the `ubuntu-24.04` runners, before GitHub moves `ubuntu-latest` on 2026-10-19;
   - CS-18, once the removal of `app/release/` is committed.
2. **CS-19:** push a pre-release tag (for example `v1.0.2-rc.1`) and confirm the split CD (`ci-gate` → `build` → `publish`) produces a verified release. Delete the pre-release afterwards if it isn't wanted.
3. **Dependabot alerts (CS-22, CS-23, CS-24).** Done on GitHub: after `b36b92e`, 64 alerts show as fixed, and #60 is dismissed as a tolerable risk (CS-24, now an accepted risk until the toolchain upgrade). CS-22 and CS-23 close with the first fully green CI run.

   Then handle the open Dependabot PRs:
   - **#17** (Actions): close it; the SHA pins replace it.
   - **#12 – #16:** the toolchain upgrade below includes all of them (ktlint-gradle 14.2.0 from #14, with ktlint itself still 1.3.1). Close them once it's merged.
4. **CS-07 / CS-15 biometric checks:** on a device with an enrolled fingerprint, turn on biometric sign-in, then:
   - sign in with the fingerprint;
   - enroll another fingerprint and confirm that biometric sign-in turns off with a message, the PIN still works, and the data is intact.
5. **CS-21:** run launch → sign-in → timeout → sign-out on API 26 and API 35. This was done on API 37 on 2026-10-03.
6. **CS-12 audit log:** still deferred (decision of Phase 2).

## Follow-on work
- **Next schema migration (v3), BUG-06 follow-up:** reject TRANSFER rows without `transferAccountId` in the database itself. Use a `BEFORE INSERT/UPDATE` trigger created both in the migration and for new installs, because SQLite can't add a CHECK to an existing table. Add a repair step that reports existing malformed rows. Until then, such rows have no balance impact and show as `Account → ?`.
- **Toolchain upgrade:** committed as `97d6cbe` and `2704d72`. CI run 37183326846 on `2704d72` is fully green, including the instrumented tests on API 26 and 36. Still to do:
  - Install the current release build with some data, update to the new build, and confirm that the database still opens with its data (SQLCipher 4.6.1 → 4.19.1, Room 2.6.1 → 2.8.5).
  - Commit the regenerated Room schema JSON if a local build rewrites `app/schemas/`.
  - CS-25: push the component metadata rule (working tree), then confirm the six Dependabot alerts close. If any remain, the configuration that resolves them is outside the app module.
  - Read the dependency graph and remove the CS-22 and CS-23 constraints that AGP 9.4.1 no longer needs. Netty is done.
  - Close Dependabot PRs #12 – #17.
  - Optionally refresh the wrapper JAR and scripts with `./gradlew wrapper --gradle-version 9.6.1`.
- **Drop `security-crypto`** (CS-09) once v1.0.x installs have had time to upgrade. It is used only by `LegacyProfileStorage`.
- **Language packs (2026-10-04):**
  - Have a native speaker review each pack; each translator's open questions are in `history.md` (2026-10-04).
  - Optionally let users pick the app's language in system settings on Android 13+ (`localeConfig`), independent of the phone's language.
  - Look at a PDF on a device in each language. Columns now fit their content and text wraps instead of being cut (2026-10-04), but the table still runs left to right in Arabic.
  - Arabic with account names in a left-to-right script: the transfer arrow can point the wrong way, since it follows the app's language, not the names.
- **Proprietary license (2026-10-04), before selling:**
  - Have an attorney review `LICENSE`, especially for sales outside the United States and on app stores.
  - Make the GitHub repository private, and stop CD from attaching signed APKs to public GitHub Releases. Anyone can download those today.
  - Releases v1.0.0 and v1.0.1, and every commit published before this change, remain available under Apache 2.0 to anyone who has a copy. That can't be revoked.
  - Generate the complete third-party list from the release build's dependency graph and reconcile `THIRD_PARTY_NOTICES.md` with it. The current list was written from the declared dependencies. Check two points from the 2026-10-04 review: whether Tink (through `security-crypto`) brings protobuf-javalite (BSD 3-Clause, which needs its own notice), and whether the crypto library inside SQLCipher for Android 4.19 is still LibTomCrypt. The app now shows this file (Settings → Open-source licenses), so a correction there reaches users with the next build.
  - Confirm that no employer agreement or school policy claims rights in the app.
- **Play Store readiness (2026-10-04):**
  - Publish `PRIVACY.md` at a public URL that stays up after the repository goes private (for example GitHub Pages from a separate public repository, or your own site), and enter it in Play Console. The app already shows the same text in Settings.
  - Data safety form: no data collected or shared. Backups and reports are made by the user on the device and saved where the user chooses; the app never receives them.
  - Have the attorney review `PRIVACY.md` together with `LICENSE`.
  - CS-28 device check: back up, wipe, set up a new PIN, restore, and confirm every record, balance and the currency came back. Then try a wrong passphrase, a photo and a truncated file. Also time a backup on the API 26 emulator; 600 000 PBKDF2 iterations should take a few seconds at most.
  - Have native speakers review the 31 backup, privacy and license strings added on 2026-10-04 in each language pack.
  - CS-A6: revisit a passphrase strength check together with CS-27 option B.
- **Later:** a chart tooltip/marker; optionally protect the `production` environment with required reviewers (a GitHub setting).
