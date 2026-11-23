---
name: dependency-upgrade
description: Add, upgrade or remove a library, Gradle plugin, SDK level or toolchain version in Denarii Dolor without breaking pinned compatibility (Kotlin/KSP/Metro, Room/SQLCipher, AppCompat/Navigation, detekt/ktlint), the build-tool security constraints, the legal notices or Play requirements. Use for any edit to gradle/libs.versions.toml, plugin versions, compileSdk/targetSdk, the Gradle wrapper, or a Dependabot PR.
---

# Upgrade or add a dependency

## 0. Decide whether it should happen

- **A new library:** ask first. Explain why the platform or an existing dependency can't do the job, and give the library's
  license, its size, any native code, and whether it needs a permission or network access. Network access is never
  allowed.
- **An upgrade:** read the pin table in `.claude/rules/build-gradle.md`. If the library is pinned or held back (Kotlin and
  KSP, Metro, Room, SQLCipher, AppCompat, kotlinx-serialization, `security-crypto`, detekt, ktlint), check that the
  constraint behind the pin no longer applies, and say so before you change it.
- Change one coupled group at a time:
  - Kotlin, KSP and Metro together;
  - Room, `androidx.sqlite` and SQLCipher together;
  - the Compose BOM alone;
  - AGP, the Gradle wrapper and the JDK together.

## 1. Read the release notes

For every version you skip, read the release notes and changelog. Look for:

- the minimum compileSdk, minSdk, AGP, Gradle and Kotlin versions;
- breaking API changes and deprecations that the build treats as errors;
- changes to the transitive Kotlin metadata or `kotlinx-serialization` versions;
- for anything with native code, 16 KB page-size support.

## 2. Edit the catalog

Change `gradle/libs.versions.toml` only, keeping aliases and comments. Update or remove any comment that explains a pin,
both in the catalog and in `app/build.gradle.kts`.

## 3. Build and inspect the graph

```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath > build/deps-after.txt
./gradlew buildEnvironment > build/buildenv-after.txt
```

`build/` is git-ignored. Run the same two commands before you edit the catalog (writing `deps-before.txt` and
`buildenv-before.txt`), compare the files with `diff`, and look for:

- unexpected new transitive libraries in the APK (each needs a notice);
- version downgrades;
- whether the CS-22, CS-23 and CS-25 `build*` constraints are still needed. Remove each one that AGP or ktlint now patches
  itself, and record that in the comments.

## 4. Run the gates

Run the `ci-gates` skill, including `assembleRelease`, because R8 and resource shrinking catch missing keep rules. Add
narrow keep rules to `app/proguard-rules.pro` only when the library's documentation requires them.

## 5. Checks for each area

- **AndroidX, Activity, AppCompat, Navigation, Lifecycle or Compose:** sign in on an emulator and open the Dashboard,
  Search, Reports and Settings. Press Back and sign out. Screen tests run in `ComponentActivity`, so they can't catch
  AppCompat view-tree-owner crashes (the 2026-10-11 incident).
- **Room, `androidx.sqlite` or SQLCipher:** run `MigrationTest`, `EncryptedDatabaseTest` and `VaultTest`. Ask the maintainer
  to install the current release build with data, update to the new build, and confirm the database opens with its data
  intact.
- **Kotlin, KSP or Metro:** a clean build (`./gradlew clean assembleDebug`) must compile the Metro graph. Check that Metro's
  compatibility table lists the new Kotlin version, and run `LockedVaultGraphTest`.
- **detekt or ktlint:** a new ktlint reformats the codebase. Upgrading it is its own change, with the `ktlintFormat` output
  reviewed separately.
- **compileSdk or targetSdk:**
  - Read that API level's behavior changes (edge-to-edge, predictive back, foreground services, intents, storage) and test
    on an emulator at the new level.
  - Add that level to the CI emulator matrix in `ci.yml`. Ask first, because workflows are protected.
  - Play requires the new targetSdk by its yearly deadline.
- **GitHub Actions:** pin each action to the new release's full commit SHA with a `# vX.Y.Z` comment, and keep
  `persist-credentials: false`.

## 6. Notices and summary

- Update `THIRD_PARTY_NOTICES.md`, and `licenses/` when needed, for every library added to or removed from the APK. A
  version bump alone usually doesn't change them unless the license changed.
- In your summary, list:
  - each version change (from → to) and why;
  - the release-note items that matter;
  - the graph changes and the constraints removed;
  - the commands you ran and their results;
  - the manual checks that remain (emulator sign-in, update from an installed release).
