---
paths:
  - "**/*.kt"
  - "**/*.kts"
  - "gradle/libs.versions.toml"
  - "gradle.properties"
  - "gradle/wrapper/gradle-wrapper.properties"
  - "build-logic/**"
  - ".editorconfig"
  - "**/detekt*.yml"
---

# Kotlin standards

These rules cover the Kotlin in this repository: source sets, tests, build scripts, and the
Gradle files around them. They describe how a senior engineer writes Kotlin, and how to work with
the user as a pair: you write the code, and they review it and make the calls that are theirs.
KDoc, error messages, and other text follow `writing.md` in this folder. Android UI and Compose
code follow this rule too, but it covers only what they share with all Kotlin.

Apply them to code you write or edit. Don't rewrite existing code only to make it conform; fix a
passage when you are already changing it, and mention anything else you notice.

## Project details

A repository fills in this section when it adopts the rule, and the rest of the rule refers to
each value by its bold name. If a value still reads `[FILL IN: …]`, the rule isn't set up here
yet. Work out that value from the repository itself, and tell the user which values are missing
so they can fill them in.

- **Modules:** one Gradle module, `:app` in `android/app/`, Flutter's Android host app. Its
  Kotlin is in `android/app/src/main/kotlin/com/denariidolor/`: `MainActivity` today, and the
  Keystore and `BiometricPrompt` code from M3.2. Flutter writes
  `android/app/src/main/java/io/flutter/plugins/GeneratedPluginRegistrant.java`, which is never
  edited by hand. The rules and the data live in Rust (ADR-0004, Proposed: Rust owns the rules
  and the data)
- **Platform:** Android only, from API 26 (the **Platforms** in `flutter.md`). Kotlin holds only
  what needs the Android runtime: the Android Keystore and `BiometricPrompt` (ADR-0007,
  accepted in M3.1: hardware-key operations stay in platform code, and Rust calls them directly
  through JNI). Logic that doesn't need them belongs
  in Rust (`CLAUDE.md`, "Architecture", rule 6), so no module here is pure Kotlin
- **Toolchain:** Kotlin 2.4.20 (the Kotlin Android Gradle plugin) and the Android Gradle plugin
  9.3.1, pinned in `android/settings.gradle.kts`; Gradle 9.5.0, pinned by the wrapper in
  `android/gradle/wrapper/gradle-wrapper.properties`; and Java 17 for `sourceCompatibility`,
  `targetCompatibility`, and `jvmTarget` in `android/app/build.gradle.kts`. The Flutter Gradle
  plugin comes from the pinned Flutter SDK (the **Toolchain** in `flutter.md`). Change any of
  them with the `dependency-upgrade` skill
- **Check commands:** none of Kotlin's own yet: no ktlint, detekt, Android lint, or Gradle test
  task runs. `flutter build appbundle --debug` from the repository root, which CI runs (M1.3),
  compiles the Kotlin; read its Kotlin warnings. Adding a Kotlin check is the maintainer's
  decision, under `docs/02-dependency-policy.md`
- **Style and lint configuration:** the `[*.{kt,kts}]` section of `.editorconfig`, kept from v1:
  ktlint's `android_studio` code style, 4-space indents, lines of at most 140 characters, and no
  wildcard imports. Nothing runs ktlint since v1's Gradle build left the working tree
  (ADR-0003), so format by hand to that style
- **Dependency policy:** ask first, and record each decision in `docs/02-dependency-policy.md`. The
  app's Gradle build has no library of its own yet; the Flutter Gradle plugin adds the
  embedding's. No mocking library: tests use hand-written fakes, as in `flutter.md`
- **Errors:** not settled yet: M3.2 settles it for the calls that come from Rust (ADR-0007). v1
  caught every Keystore failure where it was thrown and rethrew it as `SecureStorageException`
  (`KeystoreSecrets.kt` at 6d57526), and users saw only a localized message for its code. Until
  then, catch Keystore exceptions in Kotlin and hand the caller a value or a code, and show no
  exception text to users
- **Concurrency:** none: no coroutines, and no thread of Kotlin's own. Each call runs on its
  caller's thread, never Android's main thread, because Keystore operations block. Code that
  creates a Keystore key is `@Synchronized`, as v1's `SecurityProfileService` was, so two
  callers can't both create it
- **Time:** Kotlin reads no wall clock. The lockout and the session timeout run on
  `SystemClock.elapsedRealtime()`, the lockout also on `Settings.Global.BOOT_COUNT` (CS-01,
  CS-13); M3.3 and M3.7 decide which layer reads them
- **Logging:** none. Shipped code has no `Log`, no `println`, and no logging library (`CLAUDE.md`,
  "Security posture")
- **Languages:** Kotlin shows no text except the `BiometricPrompt` dialog (M3.6). Users see the
  app's text in seven languages from the ARB files in `lib/l10n/` (the **Localization** in
  `flutter.md`), and Kotlin returns codes, never sentences. A native speaker reviews every
  string you translate
- **Untrusted input:** the profile blob in `shared_prefs/vault_profile.xml` and the two v1.0.x
  files (M3.4), which may be damaged, and every value that Rust or Dart passes in. Their formats
  and limits are in sections 2–4 of `docs/specs/android-v1-formats.md`
- **Public API:** none: only the app's own Rust and Dart code call its Kotlin
- **Published modules:** none
- **Design docs:** `docs/01-architecture.md` and ADR-0007 in `docs/decisions.md`; what v1 left
  on phones is in `docs/specs/android-v1-formats.md`
- **Test layout:** no Kotlin tests yet. M3.2 adds instrumented tests in
  `android/app/src/androidTest/` with test-only Keystore aliases, and choosing their test
  libraries is the maintainer's decision. Flows that go through Dart run in `integration_test/`
  on an emulator (the **Test layout** in `flutter.md`). A test never touches the app's real
  `denarii_vault_` aliases, profile, or database (`security.md`)

## Working as a pair

- **Read before you write.** Read the code around a change and the tests that cover it, and
  match the patterns already there unless they are the problem. Check the **Modules** to find
  which module a new piece of code belongs in.
- **Say the plan first.** For a change bigger than a few lines, say in two or three sentences
  what will change, where, and how you will check it. Then go ahead, unless the plan includes one
  of the decisions in the next point.
- **Ask before decisions that are the user's.** Stop and ask before you:
  - add a library or a Gradle plugin;
  - change a public API, a serialized format, or a database schema;
  - opt in to an experimental API (`@OptIn`) or add a `-X` compiler flag;
  - change the Kotlin, JDK, Gradle, or AGP version;
  - add a platform import to a module that the **Platform** keeps pure;
  - weaken a lint, a detekt rule, a test, or a check;
  - or build something different from what was asked.

  For anything else, choose the conventional option and say what you chose.
- **Push back with reasons.** If a requested approach is unsound, slow, or fights the codebase,
  say so before you implement it, with the reason and a concrete alternative. Once the user
  decides, follow their decision.
- **Work in small steps.** Compile the module after each step; that is much faster than a full
  build. Run all the **Check commands** before you report done.
- **Fix the cause, not the symptom.** For a bug, first write a test that fails, then fix the bug
  and show the test passing. Don't add a `?.` or `?: return` that quietly skips a null that
  should never be there; find out why it is null.
- **Don't silence the compiler.** Never add `!!`, an unchecked cast, a `@Suppress`, a
  `lateinit`, or `runBlocking` only to make an error or a warning go away. Work out what the
  compiler is saying, then fix the types or the design.
- **Explain the Kotlin that isn't obvious.** In your reply, say why you chose a coroutine scope
  or dispatcher, a Flow operator, `inline` with `reified`, or variance (`out`, `in`). Write for
  the audience in `writing.md`, and cover only what this change needs.
- **Keep the change focused.** No unrelated refactors, renames, or reformatting. When the
  formatter changes files you didn't otherwise touch, revert those changes. Mention problems you
  notice outside the task instead of fixing them.
- **Review your own diff** before you report, as a reviewer would. Look for `!!`, `GlobalScope`,
  `runBlocking`, a swallowed `CancellationException`, a hard-coded dispatcher or clock,
  `println`, `TODO()`, and missing docs or tests.
- **Report honestly.** Say what changed and why, each command you ran with its real result, what
  you didn't run and why, your assumptions, and the risks that remain. Never say a check passed
  unless you ran it in this session and saw it pass.

## Design with types

- Model closed sets with an `enum class` or a `sealed interface`, and match them with a `when`
  that has no `else` branch. Then adding a case makes the compiler list every place that must
  handle it.
- Make invalid states unrepresentable. Use a sealed type instead of several nullable fields that
  are only valid together.
- Wrap IDs, units, and validated values in value classes, such as
  `@JvmInline value class UserId(val value: String)`, so the compiler rejects a `UserId` passed
  where an `OrderId` belongs.
- Parse at the boundary. Turn raw input into validated types once, where it enters the program,
  so code inside never checks it again.
- Prefer `val` to `var`, and read-only `List`, `Set`, and `Map` to their mutable versions in
  signatures. A read-only type isn't immutable (the same object may be a `MutableList` behind
  it), so return a copy when you expose a collection you keep changing.
- Use a `data class` with `copy()` for values and state.
- Replace `Boolean` parameters with enums, and use named arguments where a literal's meaning
  isn't clear at the call site: `retry(attempts = 3)`.
- Use default arguments instead of overloads. Add `@JvmOverloads` when Java code calls the
  function.
- Classes are final by default; keep them that way. Mark a class `open` only when it is designed
  for subclassing, and prefer composition and interface delegation (`by`) to inheritance.
- Give public and internal declarations explicit return types, and use the smallest visibility
  that works: `private`, then `internal`, then public.
- Use an extension function for a helper on a type you don't own. Put the behavior of your own
  types inside them.
- Use scope functions (`let`, `run`, `apply`, `also`, `with`) only where they read more clearly
  than a local variable, and never nest them. `if (x != null)` is often clearer than `x?.let`.
- Organize code by what it does (`parser`, `sync`), not by kind (`utils`, `helpers`), and keep
  functions short enough that none needs comments to separate its parts.

## Null safety

- Don't use `!!`. Use `?:`, an early return, or `checkNotNull(x) { "why" }` and
  `requireNotNull(x) { "why" }`, which fail with a message that says what went wrong.
- Use `lateinit` only for fields a framework injects. Use `by lazy` for a value that is
  expensive to create; it synchronizes by default, so use `LazyThreadSafetyMode.NONE` only when
  one thread is certain.
- Java code returns platform types, whose nullability Kotlin doesn't know. Give them an explicit
  Kotlin type where they enter, or annotate the Java side (JSpecify or `androidx.annotation`),
  so a null can't travel far before it fails.
- Use `as?` instead of `as` when a value might have another type, and never make an unchecked
  cast on data from outside the program.
- `map[key]` returns a nullable value. Use `getValue(key)` only where a missing key is a bug.

## Errors and exceptions

- Follow the **Errors** pattern.
- Return a sealed result type for failures a caller should handle (a missing file, bad input, a
  network error). Throw only for programmer errors, with `require` for bad arguments, `check`
  for bad state, and `error("…")` for code that should be unreachable.
- Kotlin has no checked exceptions, so document every exception a function can throw with KDoc's
  `@throws`, and add `@Throws` when Java code calls it.
- Catch `Exception` or `Throwable` only at a boundary that reports the failure, such as a request
  handler or a worker's entry point. Never swallow an exception silently.
- `runCatching` catches every `Throwable`, including `CancellationException` and errors such as
  `OutOfMemoryError`. Don't wrap suspend calls in it, or in `catch (e: Exception)`, unless you
  rethrow `CancellationException`.
- Close every `Closeable` with `use { }`, which also closes it when an exception is thrown.
- Say in each error message what failed, with the identifying context (the path, the ID), and
  never a secret.

## Logging and output

- Follow the **Logging** setup. Production code never calls `println`; it logs through the
  facade, or returns an error.
- Never log secrets, tokens, or personal data. Pass messages as lambdas (`log.debug { "…" }`)
  where the facade supports it, so a disabled level costs nothing.

## Coroutines and Flow

- Follow the **Concurrency** model, and inject dispatchers instead of naming `Dispatchers.IO` or
  `Dispatchers.Default` in a class, so tests can replace them.
- Use structured concurrency. Launch coroutines in a scope that something owns and cancels, such
  as `viewModelScope`, a request's scope, or the caller's `coroutineScope`. Never use
  `GlobalScope`.
- Never call `runBlocking` in production code. It blocks a thread, and on Android's main thread
  it freezes the UI.
- Make suspend functions main-safe: a function that blocks moves that work to another dispatcher
  with `withContext`, so no caller has to.
- Cancellation is cooperative:
  - Long loops call `ensureActive()` or `yield()` between chunks of work.
  - A blocking call doesn't stop when its coroutine is canceled. Wrap interruptible blocking
    calls in `runInterruptible`.
  - Never swallow `CancellationException`. `withTimeout` throws `TimeoutCancellationException`,
    which is one, so use `withTimeoutOrNull` when a timeout is a normal outcome.
- Use `coroutineScope` when one child's failure should cancel the others, and `supervisorScope`
  when children fail independently. Start `async` only for work you `await`.
- Protect shared mutable state by confining it to one coroutine, or with a `Mutex` from
  `kotlinx.coroutines.sync`. A `synchronized` block can't contain a suspension point.
- Flow:
  - Change the dispatcher of a flow's upstream with `flowOn`, never with `withContext` inside
    `flow { }`; emitting from another context throws an `IllegalStateException`.
  - Expose state as a `StateFlow` from a private `MutableStateFlow`, with `asStateFlow()`.
  - Send one-time events through a `Channel` exposed with `receiveAsFlow()`. A replaying
    `SharedFlow` fires an event again, and a `StateFlow` drops one that equals the last.
  - Derive state from changing inputs with `combine` or `flatMapLatest`, instead of starting a
    new collector for each change.
  - On Android, collect flows only while the UI is visible, with `repeatOnLifecycle` or
    `collectAsStateWithLifecycle()`.

## Time, numbers, and money

- Get the time from the source the **Time** value names, never from
  `System.currentTimeMillis()` or `Instant.now()` in logic, so tests can fix it.
- Pass durations as `kotlin.time.Duration`, not as a `Long` whose unit only a comment states.
- Store instants in UTC, pass the time zone explicitly to every function that formats or parses
  a date, and format only at the edge of the program.
- Never use `Float` or `Double` for money. Use a `Long` count of the smallest unit (cents), or
  `BigDecimal` on the JVM.
- Integer arithmetic overflows silently: `Int.MAX_VALUE + 1` is `Int.MIN_VALUE`, with no
  exception. Where input decides the values, use `Math.addExact` and `Math.multiplyExact` on the
  JVM, which throw on overflow, or check the bounds first.
- Conversions lose data silently: `Long.toInt()` keeps only the low 32 bits, and
  `Double.toInt()` clamps out-of-range values and turns `NaN` into 0.

## Text for users and languages

This section covers text that Kotlin code shows people outside Android's resources: a server's
responses, a desktop app, or a command-line tool. Android apps follow `android.md`, and code
shared with iOS follows `kotlin-multiplatform.md`. With "English only" as the **Languages**
value, the first three points still apply, so the program can be translated later.

- Keep user-facing text in the **Languages** location, and refer to it by key. Log and exception
  messages are for developers: keep them in English, and out of the translations.
- Translate at the edge. Domain code returns result types and error codes, as the **Errors**
  pattern says, and the layer that talks to the user turns them into text in the user's
  language. A server takes that language from the request (`Accept-Language`, or the user's
  setting), never from `Locale.getDefault()`, which is the JVM's.
- Pass a `Locale` to everything that formats. `"%.2f".format(x)` uses the JVM's default locale,
  so it writes `3,14` on a French machine. Use `Locale.ROOT` for text that programs read, and the
  user's locale for text people read. Make a locale from its tag with
  `Locale.forLanguageTag("zh-Hans")`.
- A `ResourceBundle` reads `messages_es.properties` and the bundle's other files, and looks up a
  key it lacks in the parent files (`messages_es_MX`, then `messages_es`, then `messages`). For a
  language with no file, it first tries the JVM's default locale: on a French server, an
  Indonesian user gets French. Load bundles with
  `ResourceBundle.Control.getNoFallbackControl(Control.FORMAT_PROPERTIES)`, which falls back to
  `messages.properties` instead. Java 9 and later read these files as UTF-8 (JDK Enhancement
  Proposal (JEP) 226); older releases need `\uXXXX` escapes.
- `java.text.MessageFormat` treats a single quote as the start of quoted text, so
  `l'application {0}` comes out as `lapplication {0}`. Pass every message through it, arguments
  or not, so translators follow one rule: write `''` for each apostrophe.
- Never build a sentence from pieces, and never choose a word form with `n == 1`. The JDK has no
  public API for the Unicode Common Locale Data Repository (CLDR) plural rules, and
  `ChoiceFormat` compares a number with ranges, so it can't express a rule that depends on the
  last digits, such as Arabic's `few` (3–10, 103–110, and so on). Use ICU4J's `MessageFormat`,
  with `{0, plural, one {# file} other {# files}}`, if the **Dependency policy** allows it.
- The JDK's `DecimalFormat` uses one grouping size, so `NumberFormat.getInstance` writes
  `1,234,567.89` for Hindi, where CLDR gives `12,34,567.89`. Use ICU4J's `NumberFormat` where
  Indian grouping matters. Format dates with
  `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)`.
- Sort text that people read with `Collator.getInstance(locale)`. `sorted()` compares UTF-16
  code units, so it puts every capital letter before every lowercase one, and accented letters
  after `z`: `"Zoé"` comes before `"zapato"`, and both before `"ábaco"`.
- Treat translations you write as drafts, and list their keys in your report for a native
  speaker to review.

## Untrusted input

Treat every byte from outside the program as possibly malformed or hostile, especially in the
**Untrusted input** modules.

- Parsers return a typed rejection for malformed input and never throw for it. Catch a
  library's exceptions where its parser is called, and map them to that rejection.
- Check every claimed size against a limit and against the input that remains before you
  allocate. Read large input in chunks; never call `readBytes()` on input of unknown size.
- Bound recursion with an explicit depth limit, and bound loops that follow links inside the
  data, so a cycle in damaged input is an error, not a hang.
- Decode with an explicitly configured parser (for example a kotlinx.serialization `Json` whose
  `ignoreUnknownKeys` you set on purpose), then validate the result. Never use Java
  serialization (`ObjectInputStream`) on outside data: it can run code while it reads.
- Treat paths from input as hostile. Resolve them against the target directory, `normalize()`
  the result, and reject it unless it still starts with that directory. An archive entry named
  `../../x` otherwise writes outside it.
- Keep secrets in a `ByteArray` or `CharArray`, never a `String`, which can't be cleared. Zero
  the array with `fill(0)` in a `finally` block, and compare secrets with
  `MessageDigest.isEqual`, which takes the same time whether or not they match.

## Platforms and Java interop

- Keep the modules the **Platform** marks as pure free of platform imports. Put platform calls
  behind an interface, with the implementation in a platform module.
- In Kotlin Multiplatform code, keep `expect` and `actual` declarations few and small, and prefer
  interfaces that the platform side implements.
- When Java code calls Kotlin, add `@JvmStatic`, `@JvmOverloads`, `@JvmName`, `@JvmField`, and
  `@Throws` where they make the Kotlin API read naturally from Java.

## Public APIs

- Turn on explicit API mode (`kotlin { explicitApi() }`) in the **Public API** modules. It
  requires an explicit visibility and return type on every public declaration, so nothing
  becomes public by accident.
- Keep every API small: prefer `internal`, and mark an `internal` declaration that a public
  `inline` function uses with `@PublishedApi`.
- In those modules, each of these breaks callers that were compiled against the old version:
  - removing or renaming a public declaration, or changing its signature;
  - adding a parameter, even one with a default value;
  - adding a property to a public `data class`, which changes its `copy` and `componentN`
    functions;
  - adding a subclass to a public sealed type, or an entry to a public enum, which breaks
    callers' exhaustive `when`;
  - making an open class final, or changing the body of a public `inline` function, which was
    copied into each caller.
- Avoid `data class` in an API that must evolve, for that reason. Ask before you make any of
  these changes, and mark them as `commits.md` requires.

## Documentation

Three kinds of docs describe a module, and each has its own reader:

- KDoc comments (`/** … */`) on declarations build each declaration's page in the generated API
  reference (Dokka).
- The module and package docs are the reference's front pages.
- A published module's `README.md` is its page in the repository and, often, the first thing a
  new user reads.

Keep all three true. A change to behavior, a signature, an error, or a platform updates every
one of them that describes it, in the same change.

### KDoc

- Give every public declaration in the **Public API** modules a KDoc comment. Its first
  paragraph is the summary that Dokka shows in lists, so make it one sentence that stands alone.
- Add each block tag that applies: `@param` for each parameter, `@return`, `@throws` for each
  exception, `@property` for primary-constructor properties, and `@see`.
- Show examples with `@sample`, which displays the body of a function from a samples source set.
  Keep that source set in the build, so a sample that stops compiling fails it.
- State only what the code does. A claim about cost, ordering, thread safety, or a limit must
  match the code, so check it there or in a test before you write it. Describe planned behavior
  as planned, never in the present tense.
- Explain a term once, on the declaration that defines it or in the package docs, and link to it
  from everywhere else with `[Parser]` or `[Parser.parse]`. Turn on Dokka's `failOnWarning`, so
  an unresolved link fails the build.
- Write code comments (`//`) that explain why, not what, and keep them true when the code
  changes. Delete commented-out code instead of leaving it.

### Module and package docs

Write each module's docs in a Markdown file that Dokka reads through `includes`: a
`# Module <name>` section, then a `# Package <name>` section for each package. Put the module
section in this order:

1. What the module is, in one or two sentences.
2. Where it fits: what it depends on, what uses it, and how a user reaches it. Link to its
   write-up in the **Design docs**, if there is one.
3. The main declarations, grouped by purpose, each named with a link.
4. What a caller must know to use it well: its threading rules, errors, limits, and platforms.

### Module READMEs

Give each of the **Published modules** a `README.md` of its own, in this shape:

1. `# <artifact-name>`, then one short paragraph on what the module is and does.
2. How it fits: the project it belongs to, and how users reach it, with a link to the project.
3. `## Status`: the version and how stable the API is. Then, kept apart, what is built today,
   what is left out by design, and what is planned.
4. `## Usage`: the Gradle dependency line (in a block tagged `kotlin`), then examples, then what
   a user must know to avoid errors.
5. `## Platforms`, only when behavior differs between targets.
6. `## Documentation`: links to the API reference, the module's write-up in the **Design docs**,
   and the project.
7. `## License`: the license, with links to the license files.

- Kotlin has no doc tests. Keep every README example in a test or a sample that the build
  compiles, and copy it from there, so you know it works. A tool such as kotlinx-knit can check
  the copies.
- Use absolute URLs for links that leave the module's folder; sites that republish the README
  resolve relative links differently, or not at all.
- Reread the README whenever the module's behavior or API changes.

### Publication metadata

For each of the **Published modules**, fill in the POM fields that Maven Central requires
(`name`, `description`, `url`, `licenses`, `developers`, and `scm`), and publish a sources JAR
and a Javadoc JAR (generated by Dokka) beside the main one. Inherit the shared fields from one
convention plugin rather than repeating them in each module.

## Tests

- Follow the **Test layout**. Give every behavior change a test, and start every bug fix with a
  test that fails before the fix.
- Never delete a test, mark it `@Ignore`, or loosen an assertion to get a build passing. Fix the
  cause, or report it.
- Test one behavior per test, and name each test for the behavior it checks. On the JVM, use a
  backtick sentence: `` `rejects archive whose entries escape the target` ``. Where the platform
  rejects spaces in names, such as Android instrumented tests below API 30, use a camelCase
  sentence instead.
- Test coroutines with `runTest`, which skips delays in virtual time, and inject a test
  dispatcher. On Android, replace `Dispatchers.Main` with a test rule.
- Keep tests deterministic: inject a fixed clock, and don't depend on the network, real sleeps
  (`Thread.sleep`), or state shared between tests.
- Prefer hand-written fakes to mocks. Mock only at boundaries you don't own, and only with a
  library the **Dependency policy** allows.
- Test the edges: empty, one, the maximum, one past the maximum, and malformed. For a parser,
  include truncated and oversized input.
- Assert on result types and error codes, not on message text, so rewording a message doesn't
  break a test.
- Keep Android APIs out of JVM unit tests, which fail on them with "Method … not mocked." Move
  the logic into pure Kotlin and test it there.

## Style, lints, and the toolchain

- Format with the ktlint style in the **Style and lint configuration**, by running its format
  task rather than formatting by hand.
- Follow the Kotlin coding conventions for names: `PascalCase` for types, `camelCase` for
  functions and properties, `UPPER_SNAKE_CASE` for `const val` and other deeply immutable
  top-level values, and a leading underscore for a private backing property (`_state`) behind a
  public read-only one.
- Treat detekt findings as design feedback, and refactor instead of suppressing them. When a
  suppression is right, put it on the smallest declaration with a reason:
  `@Suppress("TooGenericExceptionCaught") // The worker reports every failure.`
- Add no new compiler warnings, and keep `allWarningsAsErrors` wherever it is on.
- Use only language features and APIs that are stable in the pinned Kotlin version.
- Keep the **Toolchain**. Don't change the Kotlin, JDK, Gradle, or AGP version as a side effect
  of another task; a bump is a change of its own. Set the JDK with `jvmToolchain` rather than
  relying on the one installed.
- Don't leave `println`, debugging logs, or `TODO()` behind. `TODO()` compiles, then throws
  `NotImplementedError` when it runs.

## Gradle builds and dependencies

- Follow the **Dependency policy**. Never add a library or a Gradle plugin without asking. When
  you propose one, say what it does that the standard library or existing code can't, and give
  its license, how well it is maintained, what it pulls in, and, on Android, what it adds to the
  app's size.
- Declare every version once, in `gradle/libs.versions.toml`, and refer to it by its alias
  (`libs.…`). Use no inline or dynamic versions (`+`, `latest.release`), and add no repositories.
- Use `implementation` rather than `api` unless a dependency's types appear in the module's
  public API. `api` exposes the dependency to every consumer and makes builds slower.
- Keep build logic compatible with the configuration cache: use the lazy APIs
  (`tasks.register`, `Provider`), and don't touch `project` while a task runs.
- Put shared build logic in convention plugins (for example in `build-logic/`), not in
  `allprojects` or `subprojects` blocks.
- Don't change the Gradle wrapper as part of another task, and keep its `distributionSha256Sum`,
  which makes Gradle check the download.
- On Android, keep R8 keep rules narrow (never a blanket `-keep class **`), and prove them with a
  release build.

## Before finishing

1. Run every **Check commands** command and read its output. Fix every warning, not only the
   errors.
2. If you changed public declarations, KDoc, module docs, or a README:
   1. Generate the API reference with Dokka and `failOnWarning` on, so a broken link fails.
   2. Build the samples and tests that the docs and the README copy their examples from.
   3. Reread the changed prose against the code.
3. If you changed a **Public API** module, run its binary compatibility check, and explain every
   change it reports.
4. If you changed what a published module ships, publish it to the local Maven repository
   (`./gradlew publishToMavenLocal`) and check its POM, sources JAR, and Javadoc JAR.
5. Run Android instrumented tests only when a device or emulator is attached; otherwise say that
   they weren't run.
6. Review your diff as described in "Working as a pair", then report what you ran, what you
   didn't, and what remains.
