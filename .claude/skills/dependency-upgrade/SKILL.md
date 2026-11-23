---
name: dependency-upgrade
description: Add, upgrade or remove a Rust crate, a Dart package or a Flutter plugin, or change a pinned version (the Rust toolchain, the Flutter release, the Android Gradle plugin, Kotlin, Gradle, Java, the Android SDK levels or a GitHub Action) in Denarii Dolor, without breaking a pin, the offline rule, the notices or Google Play's requirements. Use for any edit to rust/Cargo.toml, a crate's Cargo.toml, pubspec.yaml, pubspec.lock, the Gradle files in android/, rust/rust-toolchain.toml or a pinned action, and for a Dependabot pull request.
---

# Add, upgrade or remove a dependency

Every dependency is code that runs next to users' financial records, so each one has to earn its place
(`docs/02-dependency-policy.md`). Change one dependency, or one group of versions that must move together, per commit.
Review a Dependabot pull request with the same steps.

## 0. Decide whether it should happen

- **A new dependency:** ask first. Answer each question in "The test an exception must pass" in
  `docs/02-dependency-policy.md`: the need, the licenses of everything it pulls in, its maintenance, its size, native code
  or `unsafe`, support for both Android and iOS, and no network access. The maintainer approves or refuses it, and you
  record the decision in that file's register, with the date.
- **An upgrade:** read the register and `docs/decisions.md` for a pin. If the version is pinned, check that the reason
  behind the pin no longer holds, and say so before you change it.
- **A removal:** check that nothing still uses it (`flutter pub deps`, `cargo tree -i <crate>`), and mark its row in the
  register as removed, with the date.

Today's pins:

| What | Where it's set | Why |
|---|---|---|
| Flutter 3.50.1 with Dart 3.14 | `environment` in `pubspec.yaml` | ADR-0009 pins it exactly. A new release needs a new decision record that supersedes ADR-0009 |
| Rust 1.100.0 | `channel` in `rust/rust-toolchain.toml`, and `rust-version` in `rust/Cargo.toml` | The Rust rule's "Toolchain". A bump is a change of its own |
| minSdk 26, targetSdk 36, compileSdk 37 | `android/app/build.gradle.kts` | v1's levels, so installs upgrade in place (ADR-0002). Ask before changing one: a higher minSdk drops users on older phones |
| The NDK | `ndkVersion = flutter.ndkVersion` in `android/app/build.gradle.kts`, which is 28.2.13676358 in Flutter 3.50.1 | The bridge's Rust library builds with the same NDK, through `hook/build.dart` (ADR-0014) |
| `flutter_rust_bridge` 2.13.0 | `=2.13.0` in `[workspace.dependencies]` of `rust/Cargo.toml`, with only the `dart-opaque`, `rust-async` and `thread-pool` features; `flutter_rust_bridge` and `flutter_rust_bridge_hooks` in `pubspec.yaml`; the codegen, installed with `cargo install flutter_rust_bridge_codegen --version 2.13.0 --locked` | ADR-0014 pins all four exactly. `RustLib.init` fails when the codegen and the runtime differ |
| `cargo-expand` 1.0.127 | Installed with `cargo install cargo-expand --version 1.0.127 --locked` | The codegen runs it, and installs the latest one by itself if it's missing (ADR-0014) |
| The Rust targets | `targets` in `rust/rust-toolchain.toml` | The three Android ABIs Flutter builds and the Linux host; the build hook refuses a target that isn't listed (ADR-0014) |
| Android Gradle plugin 9.3.1 and Kotlin Gradle plugin 2.4.20 | `android/settings.gradle.kts` | The versions the app builds with today. `flutter analyze --suggestions` checks that the four Android build tools in these rows work together |
| Gradle 9.5.0 | `android/gradle/wrapper/gradle-wrapper.properties` | The same check |
| Java 17 | `compileOptions` and `kotlin.compilerOptions` in `android/app/build.gradle.kts` | The same check |
| Each GitHub Action | its full commit SHA in `.github/workflows/`, with a `# vX.Y.Z` comment | CS-08 |

Change these together, and nothing else in the same commit:

- the Flutter release and the Dart SDK constraint. Check the `environment` constraint of every package from the Flutter
  authors as well; `material_ui` 1.11.1, for example, needs Flutter 3.47 or later;
- the Android Gradle plugin, the Kotlin Gradle plugin, Gradle, and Java;
- `flutter_rust_bridge`'s Rust crate, its Dart package, `flutter_rust_bridge_hooks`, and its codegen, which must match
  (ADR-0014: the bridge is pinned exactly). Regenerate the bindings with the new codegen
  (`flutter_rust_bridge_codegen generate`) and commit them in the same change (ADR-0012), and check the features the new
  generated code needs, because ADR-0014 allows only the ones it can't compile without;
- from M2.1, `rusqlite` and the SQLCipher build it bundles (ADR-0006, Proposed: the database stays SQLCipher, opened from
  Rust with `rusqlite`).

## 1. Read the release notes

For every version you skip, read the changelog and the release notes. Look for:

- the minimum Rust version (`rust-version`), Dart SDK, Flutter release, Android Gradle plugin, Gradle, Kotlin, Java and
  Android API level;
- breaking changes, and deprecations that the analyzer or Clippy will report;
- a changed license;
- new native code, new `unsafe`, or a new plugin pulled in;
- a new permission in a plugin's Android manifest, or a new entry in its iOS privacy manifest;
- any network access, which ADR-0008 (the app stays offline) refuses outright;
- for native code on Android, support for 16 KB memory pages, which Google Play requires.

## 2. Record the dependency graph before the change

Run these from the repository root before you edit anything. `build/` is git-ignored.

```bash
export PATH="$HOME/flutter/bin:$PATH"
(cd rust && cargo tree --workspace --locked --target all) > build/cargo-tree-before.txt
flutter pub deps --style=compact --no-dev > build/pub-deps-before.txt
```

`--no-dev` lists only what ships in the app. `flutter pub outdated` lists the newer versions of each Dart package.

For a change to `android/` or to a plugin, also record the Android libraries. `android/gradlew` exists once a Flutter
build has run, such as the release build in the `ci-gates` skill:

```bash
(cd android && ./gradlew -q :app:dependencies --configuration releaseRuntimeClasspath) > build/gradle-deps-before.txt
```

## 3. Make the change in one place

- **A Rust crate:** put its version in `[workspace.dependencies]` in `rust/Cargo.toml`, and add `name = { workspace = true }`
  to the `Cargo.toml` of each crate that uses it. `denarii-core` uses `std` only and takes no dependency. Then update the
  lock file for that crate alone, from `rust/`: `cargo update -p <crate>`. Check that the crate's `rust-version` is 1.100
  or lower.
- **A Dart package or plugin:** edit `pubspec.yaml`, then run `flutter pub get`. For an upgrade inside the existing
  constraint, `flutter pub upgrade <package>` changes `pubspec.lock` for that package and what it needs. Commit
  `pubspec.lock`.
- **The Rust toolchain:** change `channel` in `rust/rust-toolchain.toml` and `rust-version` in `rust/Cargo.toml`, then
  run `rustup toolchain install` from `rust/`.
- **The Flutter release:** only after the maintainer accepts the decision record that supersedes ADR-0009. Change
  `environment` in `pubspec.yaml`, then run `flutter pub get` with the new release on `PATH`.
- **The Android build tools or SDK levels:** edit the files in the table above, then run `flutter analyze --suggestions`
  and expect "compatible java/gradle/agp/kgp".
- **A GitHub Action:** ask first, because `.github/` is security-sensitive (`.claude/rules/security.md`). Pin the new
  release's full commit SHA with its `# vX.Y.Z` comment, and keep `persist-credentials: false` on every checkout (CS-03,
  CS-08).

Update every comment that explains a pin, in the same change. For a new toolchain or Flutter version, find each place
that states the old one and update it:

```bash
git grep -n -e '1\.100' -e '3\.50\.1'
```

## 4. Compare the graph after the change

Run the commands from step 2 again, writing `*-after.txt` files, and compare each pair:

```bash
diff build/cargo-tree-before.txt build/cargo-tree-after.txt
diff build/pub-deps-before.txt build/pub-deps-after.txt
```

Look for:

- new crates or packages that ship in the app: each one passes the license check in step 0 and gets a notice (step 6);
- downgrades;
- a library at two versions: `cargo tree --workspace --locked --target all --duplicates` lists the Rust ones.

## 5. Run the gates and the checks for the area

Run the `ci-gates` skill. Any change to `android/`, `pubspec.yaml`, or `pubspec.lock` also needs its release build and
its permission check, which catch a permission that a plugin's manifest adds.

Then run the checks for what changed:

- **Flutter, Dart, or a package from the Flutter authors:** start the app from a fresh run on the API 26 and API 36
  emulators, one at a time, and look at the placeholder screen:

  ```bash
  export PATH="$HOME/flutter/bin:$PATH"
  flutter emulators --launch Denarii_API_26
  flutter devices
  flutter run --no-resident -d <the emulator's ID from flutter devices>
  ```

  `--no-resident` installs and starts the app, then returns. Close the emulator before you launch the next one:
  `"$(sed -n 's/^sdk.dir=//p' local.properties)/platform-tools/adb" emu kill`.

  For a new Flutter release, also check that the pin still rejects other releases, as M1.1 did: set `flutter:` in
  `pubspec.yaml` to another patch release, expect `flutter pub get` to fail, and change it back.
- **The Android Gradle plugin, Kotlin, Gradle, or Java:** `flutter analyze --suggestions` reports them compatible, the
  release build passes, and the Gradle graph from steps 2 and 4 shows no unexpected change.
- **compileSdk or targetSdk:** read that API level's behavior changes (edge to edge, predictive back, intents, storage,
  and foreground services) and run the app on an emulator at the new level. The `Medium_Phone_API_37.0` emulator runs API
  37. Google Play requires a new targetSdk each year by its deadline.
- **The Rust toolchain:** all four Rust checks pass. Fix new lints in the code, or with an `#[expect(…, reason = "…")]` on
  the smallest item; never weaken `[workspace.lints]` or `rust/clippy.toml`.
- **A crate or package that touches stored data or keys:** the tests against v1's fixtures and golden values, which
  M2.3, M3.3 and M5.1 add, pass unchanged, because users' databases, profiles and backups depend on the exact bytes.

## 6. Notices, privacy, Dependabot, and the summary

- **Notices:** every crate or package that ships in the app goes in `THIRD_PARTY_NOTICES.md`, with its license text in
  `licenses/` when the license requires it (`.claude/rules/legal-docs.md`). Build tools and dev dependencies don't. That
  file still lists v1's libraries until M6.2 rewrites it, so until then add a `Note (from <task>)` under M6.2 in
  `docs/roadmap.md` that names each new package and its license, as M1.1 did. A version bump alone changes the notices
  only when the license changed.
- **Privacy:** if the dependency changes what the app stores, exports or shares, or the permissions it asks for, update
  `PRIVACY.md` and tell the maintainer that the Play Console Data safety form may need updating.
- **Dependabot:** check that `.github/dependabot.yml` covers the ecosystem. Today it covers GitHub Actions and Cargo; the
  `pub` ecosystem joins in M1.3.
- **Summary:** list
  - each version change (from → to) and why;
  - the release-note items that matter;
  - the changes in the dependency graph;
  - the commands you ran and their results;
  - the checks that remain for a person: a look on the emulators, and an update from v1 on a phone.
