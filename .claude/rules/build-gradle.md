---
paths:
  - "**/*.gradle.kts"
  - "gradle/**"
  - "gradle.properties"
  - ".github/**"
  - "app/proguard-rules.pro"
  - "Makefile"
---

# Build, dependencies and CI

## Ground rules

- **Versions:** they live only in `gradle/libs.versions.toml`, and builds reference them through catalog aliases
  (`libs.…`).
  - No inline version strings and no dynamic versions (`+`, `latest.release`).
  - No new repositories. `settings.gradle.kts` uses `FAIL_ON_PROJECT_REPOS` with `google()` and `mavenCentral()` only.
- **Protected files:** don't change the CI/CD workflows, signing, the `Makefile` or the Gradle wrapper unless the task
  requires it. Every change there is security-sensitive.
- **Configuration cache** is on. Build logic must work with it: no `project` access at execution time, and inject services
  such as `FileSystemOperations`, as `CopyLegalDocuments` does. Prefer the lazy APIs (`tasks.register`, `Provider`).
- **R8:**
  - Keep rules go in `app/proguard-rules.pro`; AGP doesn't read `src/main/keepRules/rules.keep`.
  - Keep them narrow. Never use `-dontobfuscate` or a blanket `-keep class **`.
  - Prove them with `./gradlew assembleRelease`.
- **Signing:** release signing comes only from environment variables set in CD. Never commit a keystore,
  `local.properties`, `service-account*.json`, an APK or an AAB; `.gitignore` covers them.
- **Versions and releases:**
  - `versionCode` is `MAJOR*1_000_000 + MINOR*1_000 + PATCH`, derived from the tag (`-PversionCode` / `-PversionName`).
    The local default is in `app/build.gradle.kts`.
  - The maintainer releases with `make release VERSION=vX.Y.Z`, which starts `cd.yml`. Claude never tags or pushes.

## Modules

- `:app` applies `com.android.application` with AGP 9's built-in Kotlin. `:shared` applies
  `org.jetbrains.kotlin.multiplatform` by id, without a version, because the root `buildscript` already puts the catalog's
  Kotlin Gradle plugin on the classpath. It also applies `com.android.kotlin.multiplatform.library`, which
  `settings.gradle.kts` resolves to the AGP artifact.
- `:shared` has only an Android target until the iOS phases. It has no `lintDebug` or `assembleRelease` task, so those CI
  steps cover `:app`; `ktlintCheck` and `detekt` cover both modules, tests included. Its JVM tests run in
  `testAndroidHostTest`, with coverage on. `:app`'s `testDebugUnitTest` depends on that task, and
  `createDebugUnitTestCoverageReport` on `createAndroidHostTestCoverageReport`, so CI and CD run them without naming
  `:shared`. CI's uploaded report artifact still holds only `app/build`.
- `:app`'s `testDebugUnitTest` declares `shared/src`'s common and Android sources as inputs, because
  `SharedCodeBoundaryTest` and `ClockBoundaryTest` read those files. Without that, a change no class reflects, such as an
  unused `java.*` import, would leave the task up to date.
- `:shared` repeats `:app`'s ktlint logback constraint (CS-22) and detekt's Kotlin pin. Keep the two in step.

## Pinned versions (don't bump one on its own)

| Pin | Why |
|---|---|
| Kotlin 2.4.20 and KSP 2.3.12 | AGP 9's built-in Kotlin brings KGP 2.2.10, so the root `buildscript` raises it to the catalog's Kotlin. Don't apply `org.jetbrains.kotlin.android`. Kotlin 2.4.20 is the minimum for CVE-2026-53914 (CS-24). Metro is a Kotlin compiler plugin, so check its compatibility table before moving Kotlin. |
| Metro 1.5.2 | Moves together with Kotlin: each Metro release supports a range of Kotlin versions. |
| Room 2.8.5 | Still opens SQLCipher through `openHelperFactory` (`SupportSQLite`). Room 3 changes the package and the driver API, so it's a separate, planned step. |
| SQLCipher 4.19.1 and `androidx.sqlite` 2.7.1 | SQLCipher 4.18+ needs compileSdk 37. After every upgrade, install the current release build with data, update to the new build, and confirm the database still opens. |
| AppCompat 1.7.1 or later | 1.7.0 crashes `MainActivity` right after sign-in: there is no `NavigationEventDispatcherOwner` for Navigation 2.10's `NavHost`. Never go below 1.7.1. |
| kotlinx-serialization 1.8.1 (constraint) | Room's `MigrationTestHelper` needs it, and AGP 9 doesn't align test dependencies with the app's. Keep it at least at what Room needs. |
| `security-crypto` 1.1.0-alpha06 | Deprecated, and used only for the v1.0.x upgrade path (CS-09). Removing it is the maintainer's decision. |
| detekt 1.23.8, ktlint 1.3.1, Vico 2.0.0 | Held back on purpose. detekt embeds Kotlin 2.0.21, so its configuration is pinned in `app/build.gradle.kts`, and detekt 2 is alpha. A newer ktlint reformats the codebase. Vico 3 rewrites the chart API. |
| AGP 9.4.1, Gradle 9.6.1, JDK 17 | AGP 9.4 needs Gradle 9.6 or later. CI and CD use Temurin 17. |
| compileSdk 37, targetSdk 36, minSdk 26 | targetSdk 36 has been Play's requirement since 2026-08-31. Plan the next raise before Play's next deadline, which has fallen on August 31 each year, and review that API level's behavior changes. |
| `build*` entries | Build-tool-only patches for known advisories (CS-22, CS-23, CS-25); see below. |

How the `build*` entries apply:

- The root `buildscript` constrains the plugin classpath.
- `app/build.gradle.kts` constrains AGP's `_internal-unified-test-platform*` configurations and `ktlint`.
- The `RaiseBuildToolDependencies` component metadata rule raises versions for lint and AGP's test-engine worker. Add a
  module to its map rather than writing another constraint.
- Remove each entry once AGP or ktlint brings a patched version itself. Check with `./gradlew buildEnvironment` and
  `./gradlew :app:dependencies`.

## Adding or upgrading a dependency

Use the `dependency-upgrade` skill, and ask before adding a new library. A library that ships in the APK also needs:

- an entry in `THIRD_PARTY_NOTICES.md`, with its license text in `licenses/` when the license requires it;
- 16 KB page-size alignment, if it has native code. Play requires this for apps targeting Android 15 or later.

## GitHub Actions

- Pin every `uses:` to a full commit SHA with a `# vX.Y.Z` comment (CS-08).
- Every checkout sets `persist-credentials: false`. Every job gets least-privilege `permissions:`, and runners are pinned
  (`ubuntu-24.04`).
- Secrets exist only in CD's `build` job (the `production` environment). The `publish` job can write but holds no secrets
  (CS-19).
- Dependabot opens weekly Gradle and Actions updates. Each must pass CI and the security workflow (CodeQL, dependency review,
  gitleaks).
