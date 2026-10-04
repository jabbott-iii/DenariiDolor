# Denarii Dolor — Plan

Active and follow-on work only. Finished work is recorded in `history.md`, decisions and their reasons in `notes.md`, and security findings in `cybersec.md`. Last updated 2026-10-03.

## Requirement Status
| Requirement | Status | Notes |
|---|---|---|
| Inheritance / polymorphism / encapsulation | ✅ Done | `Transaction` → `Expense`/`Income`/`Transfer`, `balanceImpact()`, `accountImpacts()` |
| Search with multi-row results | ✅ Done | Typed multi-row results with count; tap to edit; `%` and `_` match literally |
| Secure DB add / edit / delete | ✅ Done | Full add/edit/delete in UI; SQLCipher database that opens only after sign-in |
| Reports (multi-column, rows, timestamp, title) | ✅ Done | 6 columns, title, period, timestamp, totals; CSV + PDF save/share |
| Validation | ✅ Done | Amount, description (trimmed, at most 200 characters), date, references, budget limit, duplicate names (category/account) |
| Security | ✅ Done | Database key wrapped by the PIN, the security answer and strong biometrics (`Vault`); elapsed-time lockout; Keystore-encrypted profile; no backups; R8; session timeout; tapjacking guard |
| Scalability | ✅ Done | MVVM, repositories, use cases, Hilt, typed enums, cents, explicit migrations + exported schema, lint/static analysis/coverage in CI |
| GUI | ✅ Done | Bottom nav, FAB, monthly dashboard (hero, tiles, chart, budget meters, alerts), category icons, dark mode, manage screens |

## Phase 7 — Finish the 2026-10-03 remediation

All twelve bugs (BUG-01 – BUG-12) from the 2026-10-03 review are fixed, and every finding in `cybersec.md` except CS-12 has a fix in the code or the workflows. `history.md` (2026-10-03) lists each fix and its tests. What's left needs GitHub, a release tag or a physical device, so it couldn't be finished from the workstation.

1. **Commit and push, then get a green CI run.** Nothing in this round is committed yet. The Compose UI tests can only run on CI: with Espresso 3.6.1 they fail on the local API 37 emulator image (see `maint.md` §6). The first green run also validates:
   - the new instrumented tests: `NavigationTest` (BUG-03), `CrudScreensTest.saveButtonIsDisabledWhileSaving` (BUG-02) and `OverlayGuardTest` (CS-11, which only runs on the API 26 emulator);
   - the API 26 wipe-dialog test fixed in this round, which failed CI on `9f967c6`;
   - CS-08 (SHA-pinned actions) and the `ubuntu-24.04` runners, before GitHub moves `ubuntu-latest` on 2026-10-19;
   - CS-18, once the removal of `app/release/` is committed.
2. **CS-19:** push a pre-release tag (for example `v1.0.2-rc.1`) and confirm the split CD (`ci-gate` → `build` → `publish`) produces a verified release. Delete the pre-release afterwards if it isn't wanted.
3. **CS-22:** after the push, check Security → Dependabot. The alerts for the build-time `netty`, `protobuf-java`, `commons-io` and `logback` versions should close. This session's token can't read them (HTTP 403). Then handle the open Dependabot PRs:
   - **#17** (Actions): close it; the SHA pins replace it.
   - **#14** (ktlint-gradle 14.2.0): CI passes. Merge it together with a newer `ktlint` version, then run `./gradlew ktlintFormat`.
   - **#12, #13, #15, #16:** hold them for the toolchain upgrade below; each fails CI on the current pins.
4. **CS-07 / CS-15 biometric checks:** on a device with an enrolled fingerprint, turn on biometric sign-in, then:
   - sign in with the fingerprint;
   - enroll another fingerprint and confirm that biometric sign-in turns off with a message, the PIN still works, and the data is intact.
5. **CS-21:** run launch → sign-in → timeout → sign-out on API 26 and API 35. This was done on API 37 on 2026-10-03.
6. **CS-12 audit log:** still deferred (decision of Phase 2).

## Follow-on work
- **Next schema migration (v3), BUG-06 follow-up:** reject TRANSFER rows without `transferAccountId` in the database itself. Use a `BEFORE INSERT/UPDATE` trigger created both in the migration and for new installs, because SQLite can't add a CHECK to an existing table. Add a repair step that reports existing malformed rows. Until then, such rows have no balance impact and show as `Account → ?`.
- **Toolchain upgrade:** AGP 9 with Gradle 9 (PR #15), Hilt 2.60+ (#13), Kotlin 2.1+, Room 2.7 with KSP2, SQLCipher 4.1x (compileSdk 37 / Room 3), coroutines 1.11 (#12) and core-ktx 1.19 (#16), as one step (`maint.md` §8). Then:
  - remove the CS-22 build-time constraints (root `buildscript`, the block in `app/build.gradle.kts`, and the `build*` catalog versions) if the new versions are patched;
  - move to an Espresso version that supports API 37, so UI tests run on current emulator images.
- **Drop `security-crypto`** (CS-09) once v1.0.x installs have had time to upgrade. It is used only by `LegacyProfileStorage`.
- **Later:** a chart tooltip/marker; optionally protect the `production` environment with required reviewers (a GitHub setting).
